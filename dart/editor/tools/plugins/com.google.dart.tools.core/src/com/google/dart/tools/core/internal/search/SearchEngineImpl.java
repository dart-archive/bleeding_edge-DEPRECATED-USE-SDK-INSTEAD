/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.internal.search;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Index;
import com.google.dart.tools.core.index.Location;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.RelationshipCallback;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.contributor.IndexConstants;
import com.google.dart.tools.core.internal.index.util.ElementFactory;
import com.google.dart.tools.core.internal.index.util.ResourceFactory;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.internal.search.listener.CountingSearchListener;
import com.google.dart.tools.core.internal.search.listener.FilteredSearchListener;
import com.google.dart.tools.core.internal.search.listener.GatheringSearchListener;
import com.google.dart.tools.core.internal.search.listener.NameMatchingSearchListener;
import com.google.dart.tools.core.internal.search.listener.WrappedSearchListener;
import com.google.dart.tools.core.internal.search.scope.LibrarySearchScope;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.ParentElement;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.MatchKind;
import com.google.dart.tools.core.search.MatchQuality;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchFilter;
import com.google.dart.tools.core.search.SearchListener;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchPattern;
import com.google.dart.tools.core.search.SearchScope;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Instances of the class <code>SearchEngineImpl</code> implement a search engine that uses the new
 * index to obtain results.
 */
public class SearchEngineImpl implements SearchEngine {
  /**
   * Instances of the class <code>ConstructorConverter</code> implement a listener that listens for
   * matches to classes and reports matches to all of the constructors in those classes.
   */
  private static class ConstructorConverter extends WrappedSearchListener {
    public ConstructorConverter(SearchListener listener) {
      super(listener);
    }

    @Override
    public void matchFound(SearchMatch match) {
      DartElement element = match.getElement();
      if (element instanceof Type) {
        Type type = (Type) element;
        try {
          for (Method method : type.getMethods()) {
            if (method.isConstructor()) {
              SearchMatch constructorMatch = new SearchMatch(
                  match.getQuality(),
                  method,
                  method.getNameRange());
              propagateMatch(constructorMatch);
            }
          }
        } catch (DartModelException exception) {
          DartCore.logError(
              "Could not access methods associated with the type " + type.getElementName(),
              exception);
        }
      }
    }
  }

  /**
   * Instances of the class <code>RelationshipCallbackImpl</code> implement a callback that can be
   * used to report results to a search listener.
   */
  private static class RelationshipCallbackImpl implements RelationshipCallback {
    /**
     * The kind of matches that are represented by the results that will be provided to this
     * callback.
     */
    private MatchKind matchKind;

    /**
     * The search listener that should be notified when results are found.
     */
    private SearchListener listener;

    /**
     * Initialize a newly created callback to report matches of the given kind to the given listener
     * when results are found.
     * 
     * @param matchKind the kind of matches that are represented by the results
     * @param listener the search listener that should be notified when results are found
     */
    public RelationshipCallbackImpl(MatchKind matchKind, SearchListener listener) {
      this.matchKind = matchKind;
      this.listener = listener;
    }

    @Override
    public void hasRelationships(Element element, Relationship relationship, Location[] locations) {
      for (Location location : locations) {
        Element targetElement = location.getElement();
        CompilationUnit unit = getCompilationUnit(targetElement.getResource());
        if (unit != null) {
          DartElement dartElement = findElement(unit, targetElement);
          SourceRange range = new SourceRangeImpl(location.getOffset(), location.getLength());
          SearchMatch match = new SearchMatch(MatchQuality.EXACT, matchKind, dartElement, range);
          match.setQualified(relationship == IndexConstants.IS_ACCESSED_BY_QUALIFIED
              || relationship == IndexConstants.IS_MODIFIED_BY_QUALIFIED
              || relationship == IndexConstants.IS_INVOKED_BY_QUALIFIED);
          match.setImportPrefix(location.getImportPrefix());
          listener.matchFound(match);
        }
      }
      listener.searchComplete();
    }

    private DartElement findElement(CompilationUnit unit, Element element) {
      String elementId = element.getElementId();
      if (elementId.equals("#library")) {
        return unit.getLibrary();
      }
      if (elementId.equals(unit.getElementName())) {
        return unit;
      }
      ArrayList<String> elementComponents = getComponents(elementId);
      DartElement dartElement = unit;
      for (String component : elementComponents) {
        dartElement = findElement(dartElement, component);
        if (dartElement == null) {
          return unit;
        }
      }
      return dartElement;
    }

    private DartElement findElement(DartElement parentElement, String component) {
      if (!(parentElement instanceof ParentElement)) {
        DartCore.logError("Cannot find " + component + " as a child of " + parentElement
            + " because it has no children");
        return null;
      } else if (component.length() == 0) {
        DartCore.logError("Cannot find a child of " + parentElement + " without a name");
        return null;
      }
      try {
        if (Character.isDigit(component.charAt(0))) {
          try {
            int target = Integer.parseInt(component);
            int count = 0;
            for (DartElement childElement : ((ParentElement) parentElement).getChildren()) {
              if (childElement.getElementName().isEmpty()) {
                if (count == target) {
                  return childElement;
                }
                count++;
              }
            }
          } catch (NumberFormatException exception) {
            DartCore.logError("Cannot find " + component + " as a child of " + parentElement
                + " because it is an invalid name");
            return null;
          }
        } else {
          for (DartElement childElement : ((ParentElement) parentElement).getChildren()) {
            if (childElement.getElementName().equals(component)) {
              return childElement;
            }
          }
        }
      } catch (DartModelException exception) {
        DartCore.logError("Cannot find children of " + parentElement);
        return null;
      }
      DartCore.logError("Cannot find " + component + " as a child of " + parentElement);
      return null;
    }

    private CompilationUnit getCompilationUnit(Resource resource) {
      String resourceId = resource.getResourceId();
      ArrayList<String> resourceComponents = getComponents(resourceId);
      if (resourceComponents.size() != 2) {
        DartCore.logError("Invalid resource id found " + resourceId);
        return null;
      }
      String libraryUri = resourceComponents.get(0);
      String unitUri = resourceComponents.get(1);
      if (unitUri.startsWith("dart:")) {
        try {
          ExternalCompilationUnitImpl unit = DartModelManager.getInstance().getDartModel().getBundledCompilationUnit(
              new URI(libraryUri));
          if (unit != null) {
            int index = unitUri.lastIndexOf('/');
            return unit.getLibrary().getCompilationUnit(unitUri.substring(index + 1));
          }
        } catch (Exception exception) {
          DartCore.logError("Could not get bundled resource " + resourceId, exception);
        }
        return null;
      }
      IFile[] unitFiles = getFilesForUri(unitUri);
      if (unitFiles == null) {
        return null;
      } else if (unitFiles.length == 0) {
        return searchForUnit(libraryUri, unitUri);
      } else if (unitFiles.length == 1) {
        DartElement unitElement = DartCore.create(unitFiles[0]);
        if (unitElement instanceof CompilationUnit) {
          return (CompilationUnit) unitElement;
        } else if (unitElement instanceof DartLibrary) {
          try {
            return ((DartLibrary) unitElement).getDefiningCompilationUnit();
          } catch (DartModelException exception) {
            DartCore.logError(
                "Could not access defining compilation unit for library " + unitUri,
                exception);
          }
        }
        return null;
      }
      IFile[] libraryFiles = getFilesForUri(libraryUri);
      if (libraryFiles == null) {
        return null;
      } else if (libraryFiles.length == 0) {
        return searchForUnit(libraryUri, unitUri);
      } else if (libraryFiles.length > 1) {
        DartCore.logError("Multiple files linked to URI's " + libraryUri + " and " + unitUri);
        return null;
      }
      DartElement libraryElement = DartCore.create(libraryFiles[0]);
      DartLibrary library = null;
      if (libraryElement instanceof DartLibrary) {
        library = (DartLibrary) libraryElement;
      } else if (libraryElement instanceof CompilationUnit) {
        library = ((CompilationUnit) libraryElement).getLibrary();
      }
      if (library == null) {
        DartCore.logError("Could not find library for URI " + libraryUri);
        return null;
      }
      try {
        return library.getCompilationUnit(new URI(unitUri));
      } catch (URISyntaxException exception) {
        DartCore.logError("Could not find compilation unit for URI " + unitUri + " in library "
            + libraryUri);
      }
      return null;
    }

    private ArrayList<String> getComponents(String identifier) {
      ArrayList<String> components = new ArrayList<String>();
      boolean previousWasSeparator = false;
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < identifier.length(); i++) {
        char currentChar = identifier.charAt(i);
        if (previousWasSeparator) {
          if (currentChar == '^') {
            builder.append(currentChar);
          } else {
            components.add(builder.toString());
            builder.setLength(0);
            builder.append(currentChar);
          }
          previousWasSeparator = false;
        } else {
          if (currentChar == '^') {
            previousWasSeparator = true;
          } else {
            builder.append(currentChar);
          }
        }
      }
      components.add(builder.toString());
      return components;
    }

    private IFile[] getFilesForUri(String uri) {
      try {
        ArrayList<IFile> files = new ArrayList<IFile>();
        for (IResource resource : ResourceUtil.getResources(new URI(uri))) {
          if (resource instanceof IFile) {
            files.add((IFile) resource);
          }
        }
        return files.toArray(new IFile[files.size()]);
      } catch (URISyntaxException exception) {
        DartCore.logError("Invalid URI stored in resource id " + uri, exception);
      }
      return null;
    }

    private CompilationUnit searchForUnit(String libraryUri, String unitUri) {
      try {
        DartLibraryImpl libraryImpl = new DartLibraryImpl(new File(new URI(libraryUri)));
        for (CompilationUnit unit : libraryImpl.getCompilationUnits()) {
          if (((CompilationUnitImpl) unit).getSourceRef().getUri().toString().equals(unitUri)) {
            return unit;
          }
        }
      } catch (Exception exception) {
        DartCore.logInformation("Could not find compilation unit \"" + unitUri + "\" in library \""
            + libraryUri + "\"", exception);
        return null;
      }
      DartCore.logInformation("Could not find compilation unit \"" + unitUri + "\" in library \""
          + libraryUri + "\"");
      return null;
    }
  }

  /**
   * The interface <code>SearchRunner</code> defines the behavior of objects that can be used to
   * perform an asynchronous search.
   */
  private interface SearchRunner {
    /**
     * Perform an asynchronous search, passing the results to the given listener.
     * 
     * @param listener the listener to which search results should be passed
     * @throws SearchException if the results could not be computed
     */
    public void performSearch(SearchListener listener) throws SearchException;
  }

  /**
   * The index used to respond to the search requests.
   */
  private Index index;

  /**
   * Initialize a newly created search engine to use the given index.
   * 
   * @param index the index used to respond to the search requests
   */
  public SearchEngineImpl(Index index) {
    this.index = index;
  }

  @Override
  public List<SearchMatch> searchConstructorDeclarations(final SearchScope scope,
      final SearchPattern pattern, final SearchFilter filter, final IProgressMonitor monitor)
      throws SearchException {
    final Element[] elements = createElements(scope);
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchConstructorDeclarations(elements, pattern, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchConstructorDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    searchConstructorDeclarations(createElements(scope), pattern, filter, listener, monitor);
  }

  @Override
  public List<SearchMatch> searchFieldDeclarations(final SearchScope scope,
      final SearchPattern pattern, final SearchFilter filter, final IProgressMonitor monitor)
      throws SearchException {
    final Element[] elements = createElements(scope);
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchFieldDeclarations(elements, pattern, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchFieldDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    searchFieldDeclarations(createElements(scope), pattern, filter, listener, monitor);
  }

  @Override
  public List<SearchMatch> searchFunctionDeclarations(final SearchScope scope,
      final SearchPattern pattern, final SearchFilter filter, final IProgressMonitor monitor)
      throws SearchException {
    final Element[] elements = createElements(scope);
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchFunctionDeclarations(elements, pattern, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    searchFunctionDeclarations(createElements(scope), pattern, filter, listener, monitor);
  }

  @Override
  public List<SearchMatch> searchImplementors(final Type type, final SearchScope scope,
      final SearchFilter filter, final IProgressMonitor monitor) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchImplementors(type, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchImplementors(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    index.getRelationships(
        createElement(type),
        IndexConstants.IS_IMPLEMENTED_BY,
        new RelationshipCallbackImpl(MatchKind.INTERFACE_IMPLEMENTED, applyFilter(filter, listener)));
  }

  @Override
  public List<SearchMatch> searchMethodDeclarations(final SearchScope scope,
      final SearchPattern pattern, final SearchFilter filter, final IProgressMonitor monitor)
      throws SearchException {
    final Element[] elements = createElements(scope);
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchMethodDeclarations(elements, pattern, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchMethodDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    searchMethodDeclarations(createElements(scope), pattern, filter, listener, monitor);
  }

  @Override
  public List<SearchMatch> searchReferences(final DartFunction function, final SearchScope scope,
      final SearchFilter filter, final IProgressMonitor monitor) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(function, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchReferences(DartFunction function, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(3, applyFilter(filter, listener));
    index.getRelationships(
        createElement(function),
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new RelationshipCallbackImpl(MatchKind.FUNCTION_EXECUTION, filteredListener));
    index.getRelationships(
        createElement(function),
        IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
        new RelationshipCallbackImpl(MatchKind.FUNCTION_EXECUTION, filteredListener));
    index.getRelationships(
        createElement(function),
        IndexConstants.IS_REFERENCED_BY,
        new RelationshipCallbackImpl(MatchKind.FUNCTION_EXECUTION, filteredListener));
  }

  @Override
  public List<SearchMatch> searchReferences(final DartFunctionTypeAlias alias,
      final SearchScope scope, final SearchFilter filter, final IProgressMonitor monitor)
      throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(alias, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchReferences(DartFunctionTypeAlias alias, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    index.getRelationships(
        createElement(alias),
        IndexConstants.IS_REFERENCED_BY,
        new RelationshipCallbackImpl(MatchKind.FUNCTION_TYPE_REFERENCE, applyFilter(
            filter,
            listener)));
  }

  @Override
  public List<SearchMatch> searchReferences(final DartImport imprt, final SearchScope scope,
      final SearchFilter filter, final IProgressMonitor monitor) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(imprt, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchReferences(DartImport imprt, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    index.getRelationships(
        createElement(imprt),
        IndexConstants.IS_REFERENCED_BY,
        new RelationshipCallbackImpl(MatchKind.IMPORT_REFERENCE, applyFilter(filter, listener)));
  }

  @Override
  public List<SearchMatch> searchReferences(final DartVariableDeclaration variable,
      final SearchScope scope, final SearchFilter filter, final IProgressMonitor monitor)
      throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(variable, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchReferences(DartVariableDeclaration variable, SearchScope scope,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(4, applyFilter(filter, listener));
    index.getRelationships(
        createElement(variable),
        IndexConstants.IS_ACCESSED_BY_QUALIFIED,
        new RelationshipCallbackImpl(MatchKind.FIELD_READ, filteredListener));
    index.getRelationships(
        createElement(variable),
        IndexConstants.IS_MODIFIED_BY_QUALIFIED,
        new RelationshipCallbackImpl(MatchKind.FIELD_WRITE, filteredListener));
    index.getRelationships(
        createElement(variable),
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        new RelationshipCallbackImpl(MatchKind.FIELD_READ, filteredListener));
    index.getRelationships(
        createElement(variable),
        IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
        new RelationshipCallbackImpl(MatchKind.FIELD_WRITE, filteredListener));
  }

  @Override
  public List<SearchMatch> searchReferences(final Field field, final SearchScope scope,
      final SearchFilter filter, final IProgressMonitor monitor) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(field, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchReferences(Field field, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    Element fieldElement = createElement(field);
    SearchListener filteredListener = new CountingSearchListener(4, applyFilter(filter, listener));
    index.getRelationships(
        fieldElement,
        IndexConstants.IS_ACCESSED_BY_QUALIFIED,
        new RelationshipCallbackImpl(MatchKind.FIELD_READ, filteredListener));
    index.getRelationships(
        fieldElement,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        new RelationshipCallbackImpl(MatchKind.FIELD_READ, filteredListener));
    index.getRelationships(
        fieldElement,
        IndexConstants.IS_MODIFIED_BY_QUALIFIED,
        new RelationshipCallbackImpl(MatchKind.FIELD_WRITE, filteredListener));
    index.getRelationships(
        fieldElement,
        IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
        new RelationshipCallbackImpl(MatchKind.FIELD_WRITE, filteredListener));
  }

  @Override
  public List<SearchMatch> searchReferences(final IFile file, final SearchScope scope,
      final SearchFilter filter, final IProgressMonitor monitor) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(file, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchReferences(IFile file, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    index.getRelationships(
        createElement(file),
        IndexConstants.IS_REFERENCED_BY,
        new RelationshipCallbackImpl(MatchKind.FILE_REFERENCE, applyFilter(filter, listener)));
  }

  @Override
  public List<SearchMatch> searchReferences(final Method method, final SearchScope scope,
      final SearchFilter filter, final IProgressMonitor monitor) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(method, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchReferences(Method method, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(2, applyFilter(filter, listener));
    index.getRelationships(
        createElement(method),
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        new RelationshipCallbackImpl(MatchKind.METHOD_INVOCATION, filteredListener));
    index.getRelationships(
        createElement(method),
        IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
        new RelationshipCallbackImpl(MatchKind.METHOD_INVOCATION, filteredListener));
  }

  @Override
  public List<SearchMatch> searchReferences(final Type type, final SearchScope scope,
      final SearchFilter filter, final IProgressMonitor monitor) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(type, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchReferences(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    index.getRelationships(
        createElement(type),
        IndexConstants.IS_REFERENCED_BY,
        new RelationshipCallbackImpl(MatchKind.TYPE_REFERENCE, applyFilter(filter, listener)));
  }

  @Override
  public List<SearchMatch> searchSubtypes(final Type type, final SearchScope scope,
      final SearchFilter filter, final IProgressMonitor monitor) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchSubtypes(type, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchSubtypes(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(2, applyFilter(filter, listener));
    index.getRelationships(
        createElement(type),
        IndexConstants.IS_EXTENDED_BY,
        new RelationshipCallbackImpl(MatchKind.TYPE_REFERENCE, filteredListener));
    index.getRelationships(
        createElement(type),
        IndexConstants.IS_IMPLEMENTED_BY,
        new RelationshipCallbackImpl(MatchKind.TYPE_REFERENCE, filteredListener));
  }

  @Override
  public List<SearchMatch> searchSupertypes(final Type type, final SearchScope scope,
      final SearchFilter filter, final IProgressMonitor monitor) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchSupertypes(type, scope, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchSupertypes(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(2, applyFilter(filter, listener));
    index.getRelationships(
        createElement(type),
        IndexConstants.EXTENDS,
        new RelationshipCallbackImpl(MatchKind.TYPE_REFERENCE, filteredListener));
    index.getRelationships(
        createElement(type),
        IndexConstants.IMPLEMENTS,
        new RelationshipCallbackImpl(MatchKind.TYPE_REFERENCE, filteredListener));
  }

  @Override
  public List<SearchMatch> searchTypeDeclarations(SearchScope scope, final SearchPattern pattern,
      final SearchFilter filter, final IProgressMonitor monitor) throws SearchException {
    final Element[] elements = createElements(scope);
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchTypeDeclarations(elements, pattern, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    searchTypeDeclarations(createElements(scope), pattern, filter, listener, monitor);
  }

  @Override
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    searchTypeDeclarations(scope, pattern, null, listener, monitor);
  }

  @Override
  public List<SearchMatch> searchVariableDeclarations(final SearchScope scope,
      final SearchPattern pattern, final SearchFilter filter, final IProgressMonitor monitor)
      throws SearchException {
    final Element[] elements = createElements(scope);
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchVariableDeclarations(elements, pattern, filter, listener, monitor);
      }
    });
  }

  @Override
  public void searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    searchVariableDeclarations(createElements(scope), pattern, filter, listener, monitor);
  }

  /**
   * Apply the given filter to the given listener.
   * 
   * @param filter the filter to be used before passing matches on to the listener, or
   *          <code>null</code> if all matches should be passed on
   * @param listener the listener that will only be given matches that pass the filter
   * @return a search listener that will pass to the given listener any matches that pass the given
   *         filter
   */
  private SearchListener applyFilter(SearchFilter filter, SearchListener listener) {
    if (filter == null) {
      return listener;
    }
    return new FilteredSearchListener(filter, listener);
  }

  /**
   * Apply the given pattern to the given listener.
   * 
   * @param pattern the pattern to be used before passing matches on to the listener, or
   *          <code>null</code> if all matches should be passed on
   * @param listener the listener that will only be given matches that match the pattern
   * @return a search listener that will pass to the given listener any matches that match the given
   *         pattern
   */
  private SearchListener applyPattern(SearchPattern pattern, SearchListener listener) {
    if (pattern == null) {
      return listener;
    }
    return new NameMatchingSearchListener(pattern, listener);
  }

  private Element createElement(DartElement element) throws SearchException {
    if (element == null || element instanceof CompilationUnit) {
      return null;
    } else if (element instanceof DartFunction) {
      return createElement((DartFunction) element);
    } else if (element instanceof DartFunctionTypeAlias) {
      return createElement((DartFunctionTypeAlias) element);
    } else if (element instanceof DartVariableDeclaration) {
      return createElement((DartVariableDeclaration) element);
    } else if (element instanceof Field) {
      return createElement((Field) element);
    } else if (element instanceof Method) {
      return createElement((Method) element);
    } else if (element instanceof Type) {
      return createElement((Type) element);
    } else if (element instanceof DartImport) {
      return createElement((DartImport) element);
    } else {
      return createElement(element.getParent());
    }
  }

  private Element createElement(DartFunction function) throws SearchException {
    String functionName = function.getElementName();
    // TODO(brianwilkerson) Handle unnamed functions
    return new Element(getResource(function.getCompilationUnit()), ElementFactory.composeElementId(
        createElement(function.getParent()),
        functionName));
  }

  private Element createElement(DartFunctionTypeAlias alias) throws SearchException {
    return new Element(
        getResource(alias.getCompilationUnit()),
        ElementFactory.composeElementId(alias.getElementName()));
  }

  private Element createElement(DartImport imprt) throws SearchException {
    CompilationUnit unit = imprt.getCompilationUnit();
    return new Element(getResource(unit), imprt.getPrefix() + ":"
        + imprt.getLibrary().getElementName());
  }

  private Element createElement(DartLibrary library) throws SearchException {
    try {
      Resource libraryResource = getResource(library.getDefiningCompilationUnit());
      return new Element(libraryResource, ElementFactory.LIBRARY_ELEMENT_ID);
    } catch (DartModelException exception) {
      throw new SearchException(exception);
    }
  }

  private Element createElement(DartVariableDeclaration variable) throws SearchException {
    return new Element(
        getResource(variable.getCompilationUnit()),
        ElementFactory.composeElementId(variable.getElementName()));
  }

  private Element createElement(Field field) throws SearchException {
    Type type = field.getDeclaringType();
    if (type == null) {
      return new Element(getResource(field.getCompilationUnit()), field.getElementName());
    }
    return new Element(getResource(field.getCompilationUnit()), type.getElementName()
        + ResourceFactory.SEPARATOR_CHAR + field.getElementName());
  }

  private Element createElement(IFile file) throws SearchException {
    return new Element(getResource(file), "");
  }

  private Element createElement(Method method) throws SearchException {
    Type type = method.getDeclaringType();
    if (type == null) {
      return new Element(getResource(method.getCompilationUnit()), method.getElementName());
    }
    return new Element(getResource(method.getCompilationUnit()), type.getElementName()
        + ResourceFactory.SEPARATOR_CHAR + method.getElementName());
  }

  private Element createElement(Type type) throws SearchException {
    return new Element(getResource(type.getCompilationUnit()), type.getElementName());
  }

  private Element[] createElements(SearchScope scope) throws SearchException {
    // TODO(brianwilkerson) Figure out how to handle scope information in a more generic way
    if (scope instanceof LibrarySearchScope) {
      DartLibrary[] libraries = ((LibrarySearchScope) scope).getLibraries();
      int count = libraries.length;
      Element[] elements = new Element[count];
      for (int i = 0; i < count; i++) {
        elements[i] = createElement(libraries[i]);
      }
      return elements;
    }
    return new Element[] {IndexConstants.UNIVERSE};
  }

  /**
   * Use the given runner to perform the given number of asynchronous searches, then wait until the
   * search has completed and return the results that were produced.
   * 
   * @param runner the runner used to perform an asynchronous search
   * @return the results that were produced
   * @throws SearchException if the results of at least one of the searched could not be computed
   */
  private List<SearchMatch> gatherResults(SearchRunner runner) throws SearchException {
    GatheringSearchListener listener = new GatheringSearchListener();
    runner.performSearch(listener);
    while (!listener.isComplete()) {
      Thread.yield();
    }
    return listener.getMatches();
  }

  private Resource getResource(CompilationUnit compilationUnit) throws SearchException {
    try {
      return ResourceFactory.getResource(compilationUnit);
    } catch (DartModelException exception) {
      throw new SearchException(exception);
    }
  }

  private Resource getResource(IFile file) throws SearchException {
    try {
      return ResourceFactory.getResource(file);
    } catch (DartModelException exception) {
      throw new SearchException(exception);
    }
  }

  private void searchConstructorDeclarations(Element[] elements, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(elements.length, applyFilter(
        filter,
        applyPattern(pattern, new ConstructorConverter(listener))));
    for (Element element : elements) {
      index.getRelationships(element, IndexConstants.DEFINES_CLASS, new RelationshipCallbackImpl(
          MatchKind.NOT_A_REFERENCE,
          filteredListener));
    }
  }

  private void searchFieldDeclarations(Element[] elements, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(elements.length, applyFilter(
        filter,
        applyPattern(pattern, listener)));
    for (Element element : elements) {
      index.getRelationships(element, IndexConstants.DEFINES_FIELD, new RelationshipCallbackImpl(
          MatchKind.NOT_A_REFERENCE,
          filteredListener));
    }
  }

  private void searchFunctionDeclarations(Element[] elements, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(elements.length, applyFilter(
        filter,
        applyPattern(pattern, listener)));
    for (Element element : elements) {
      index.getRelationships(
          element,
          IndexConstants.DEFINES_FUNCTION,
          new RelationshipCallbackImpl(MatchKind.NOT_A_REFERENCE, filteredListener));
    }
  }

  private void searchMethodDeclarations(Element[] elements, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(elements.length, applyFilter(
        filter,
        applyPattern(pattern, listener)));
    for (Element element : elements) {
      index.getRelationships(element, IndexConstants.DEFINES_METHOD, new RelationshipCallbackImpl(
          MatchKind.NOT_A_REFERENCE,
          filteredListener));
    }
  }

  private void searchTypeDeclarations(Element[] elements, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(elements.length * 3, applyFilter(
        filter,
        applyPattern(pattern, listener)));
    for (Element element : elements) {
      index.getRelationships(element, IndexConstants.DEFINES_CLASS, new RelationshipCallbackImpl(
          MatchKind.NOT_A_REFERENCE,
          filteredListener));
      index.getRelationships(
          element,
          IndexConstants.DEFINES_FUNCTION_TYPE,
          new RelationshipCallbackImpl(MatchKind.NOT_A_REFERENCE, filteredListener));
      index.getRelationships(
          element,
          IndexConstants.DEFINES_INTERFACE,
          new RelationshipCallbackImpl(MatchKind.NOT_A_REFERENCE, filteredListener));
    }
  }

  private void searchVariableDeclarations(Element[] elements, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    SearchListener filteredListener = new CountingSearchListener(elements.length, applyFilter(
        filter,
        applyPattern(pattern, listener)));
    for (Element element : elements) {
      index.getRelationships(
          element,
          IndexConstants.DEFINES_FUNCTION,
          new RelationshipCallbackImpl(MatchKind.NOT_A_REFERENCE, filteredListener));
    }
  }
}
