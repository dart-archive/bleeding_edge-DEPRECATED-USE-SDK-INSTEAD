/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.search;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.index.RelationshipCallback;
import com.google.dart.engine.internal.index.IndexConstants;
import com.google.dart.engine.internal.index.NameElementImpl;
import com.google.dart.engine.internal.search.listener.CountingSearchListener;
import com.google.dart.engine.internal.search.listener.FilteredSearchListener;
import com.google.dart.engine.internal.search.listener.GatheringSearchListener;
import com.google.dart.engine.internal.search.listener.NameMatchingSearchListener;
import com.google.dart.engine.internal.search.scope.LibrarySearchScope;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.MatchQuality;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchException;
import com.google.dart.engine.search.SearchFilter;
import com.google.dart.engine.search.SearchListener;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.search.SearchPattern;
import com.google.dart.engine.search.SearchScope;
import com.google.dart.engine.utilities.source.SourceRange;

import java.util.List;

/**
 * Implementation of {@link SearchEngine}.
 */
public class SearchEngineImpl implements SearchEngine {

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
//        CompilationUnit unit = getCompilationUnit(targetElement.getResource());
//        if (unit != null) {
//        Element dartElement = findElement(unit, targetElement);
        SourceRange range = new SourceRange(location.getOffset(), location.getLength());
        // TODO(scheglov) IndexConstants.DYNAMIC for MatchQuality.NAME
        MatchQuality quality = MatchQuality.EXACT;
//          MatchQuality quality = element.getResource() != IndexConstants.DYNAMIC
//              ? MatchQuality.EXACT : MatchQuality.NAME;
        SearchMatch match = new SearchMatch(quality, matchKind, targetElement, range);
        match.setQualified(relationship == IndexConstants.IS_ACCESSED_BY_QUALIFIED
            || relationship == IndexConstants.IS_MODIFIED_BY_QUALIFIED
            || relationship == IndexConstants.IS_INVOKED_BY_QUALIFIED);
        match.setImportPrefix(location.getImportPrefix());
        listener.matchFound(match);
//        }
      }
      listener.searchComplete();
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

  private static Element[] createElements(SearchScope scope) throws SearchException {
    if (scope instanceof LibrarySearchScope) {
      return ((LibrarySearchScope) scope).getLibraries();
    }
    return new Element[] {IndexConstants.UNIVERSE};
  }

  private static RelationshipCallback newCallback(MatchKind matchKind, SearchListener listener) {
    return new RelationshipCallbackImpl(matchKind, listener);
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

//  @Override
//  public List<SearchMatch> searchImplementors(final Type type, final SearchScope scope,
//      final SearchFilter filter) throws SearchException {
//    return gatherResults(new SearchRunner() {
//      @Override
//      public void performSearch(SearchListener listener) throws SearchException {
//        searchImplementors(type, scope, filter, listener);
//      }
//    });
//  }
//
//  @Override
//  public void searchImplementors(Type type, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException {
//    if (listener == null) {
//      throw new IllegalArgumentException("listener cannot be null");
//    }
//    index.getRelationships(
//        createElement(type),
//        IndexConstants.IS_IMPLEMENTED_BY,
//        new RelationshipCallbackImpl(MatchKind.INTERFACE_IMPLEMENTED, applyFilter(filter, listener)));
//  }
//
////  @Override
////  public List<SearchMatch> searchMethodDeclarations(final SearchScope scope,
////      final SearchPattern pattern, final SearchFilter filter)
////      throws SearchException {
////    final Element[] elements = createElements(scope);
////    return gatherResults(new SearchRunner() {
////      @Override
////      public void performSearch(SearchListener listener) throws SearchException {
////        searchMethodDeclarations(elements, pattern, filter, listener);
////      }
////    });
////  }
////
////  @Override
////  public void searchMethodDeclarations(SearchScope scope, SearchPattern pattern,
////      SearchFilter filter, SearchListener listener)
////      throws SearchException {
////    searchMethodDeclarations(createElements(scope), pattern, filter, listener);
////  }
//
//  @Override
//  public List<SearchMatch> searchReferences(final DartClassTypeAlias type, final SearchScope scope,
//      final SearchFilter filter) throws SearchException {
//    return gatherResults(new SearchRunner() {
//      @Override
//      public void performSearch(SearchListener listener) throws SearchException {
//        searchReferences(type, scope, filter, listener);
//      }
//    });
//  }
//
//  @Override
//  public void searchReferences(DartClassTypeAlias type, SearchScope scope, SearchFilter filter,
//      SearchListener listener) throws SearchException {
//    if (listener == null) {
//      throw new IllegalArgumentException("listener cannot be null");
//    }
//    index.getRelationships(
//        createElement(type),
//        IndexConstants.IS_REFERENCED_BY,
//        new RelationshipCallbackImpl(MatchKind.TYPE_REFERENCE, applyFilter(filter, listener)));
//  }

  @Override
  public List<SearchMatch> searchDeclarations(final String name, final SearchFilter filter)
      throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchDeclarations(name, filter, listener);
      }
    });
  }

  @Override
  public void searchDeclarations(String name, SearchFilter filter, SearchListener listener)
      throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.IS_DEFINED_BY,
        newCallback(MatchKind.NAME_DECLARATION, listener));
  }

  @Override
  public List<SearchMatch> searchFunctionDeclarations(final SearchScope scope,
      final SearchPattern pattern, final SearchFilter filter) throws SearchException {
    final Element[] elements = createElements(scope);
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchFunctionDeclarations(elements, pattern, filter, listener);
      }
    });
  }

  @Override
  public void searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener) throws SearchException {
    searchFunctionDeclarations(createElements(scope), pattern, filter, listener);
  }

  @Override
  public List<SearchMatch> searchReferences(final ClassElement type, final SearchScope scope,
      final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(type, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(ClassElement type, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        type,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.TYPE_REFERENCE, listener));
  }

  @Override
  public List<SearchMatch> searchReferences(final CompilationUnitElement unit,
      final SearchScope scope, final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(unit, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(CompilationUnitElement unit, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        unit,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.UNIT_REFERENCE, listener));
  }

  @Override
  public List<SearchMatch> searchReferences(final Element element, final SearchScope scope,
      final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(element, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(Element element, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    if (element != null) {
      switch (element.getKind()) {
        case CLASS:
          searchReferences((ClassElement) element, scope, filter, listener);
          return;
        case COMPILATION_UNIT:
          searchReferences((CompilationUnitElement) element, scope, filter, listener);
          return;
        case FIELD:
          searchReferences((FieldElement) element, scope, filter, listener);
          return;
        case FUNCTION:
          searchReferences((FunctionElement) element, scope, filter, listener);
          return;
        case IMPORT:
          searchReferences((ImportElement) element, scope, filter, listener);
          return;
        case LIBRARY:
          searchReferences((LibraryElement) element, scope, filter, listener);
          return;
        case METHOD:
          searchReferences((MethodElement) element, scope, filter, listener);
          return;
        case PARAMETER:
          searchReferences((ParameterElement) element, scope, filter, listener);
          return;
        case TYPE_ALIAS:
          searchReferences((TypeAliasElement) element, scope, filter, listener);
          return;
        case VARIABLE:
          searchReferences((VariableElement) element, filter, listener);
          return;
      }
    }
    listener.searchComplete();
  }

  @Override
  public List<SearchMatch> searchReferences(final FieldElement field, final SearchScope scope,
      final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(field, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(FieldElement field, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    // TODO(scheglov) use "6" when add matches by name
    listener = new CountingSearchListener(4, listener);
    // exact matches
    {
      index.getRelationships(
          field,
          IndexConstants.IS_ACCESSED_BY_QUALIFIED,
          newCallback(MatchKind.FIELD_READ, listener));
      index.getRelationships(
          field,
          IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
          newCallback(MatchKind.FIELD_READ, listener));
      index.getRelationships(
          field,
          IndexConstants.IS_MODIFIED_BY_QUALIFIED,
          newCallback(MatchKind.FIELD_WRITE, listener));
      index.getRelationships(
          field,
          IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
          newCallback(MatchKind.FIELD_WRITE, listener));
    }
    // TODO(scheglov)
    // inexact matches by name
//    {
//      Element inexactElement = new Element(IndexConstants.DYNAMIC, field.getElementName());
//      index.getRelationships(
//          inexactElement,
//          IndexConstants.IS_ACCESSED_BY_QUALIFIED,
//          new RelationshipCallbackImpl(MatchKind.FIELD_READ, listener));
//      index.getRelationships(
//          inexactElement,
//          IndexConstants.IS_MODIFIED_BY_QUALIFIED,
//          new RelationshipCallbackImpl(MatchKind.FIELD_WRITE, listener));
//    }
  }

  @Override
  public List<SearchMatch> searchReferences(final FunctionElement function,
      final SearchScope scope, final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(function, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(FunctionElement function, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(4, listener);
    index.getRelationships(
        function,
        IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
        newCallback(MatchKind.FUNCTION_EXECUTION, listener));
    index.getRelationships(
        function,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        newCallback(MatchKind.FUNCTION_EXECUTION, listener));
    index.getRelationships(
        function,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        newCallback(MatchKind.FUNCTION_REFERENCE, listener));
    index.getRelationships(
        function,
        IndexConstants.IS_ACCESSED_BY_QUALIFIED,
        newCallback(MatchKind.FUNCTION_REFERENCE, listener));
  }

  @Override
  public List<SearchMatch> searchReferences(final ImportElement imp, final SearchScope scope,
      final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(imp, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(ImportElement imp, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        imp,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.IMPORT_REFERENCE, listener));
  }

  @Override
  public List<SearchMatch> searchReferences(final LibraryElement library, final SearchScope scope,
      final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(library, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(LibraryElement library, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        library,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.LIBRARY_REFERENCE, listener));
  }

  @Override
  public List<SearchMatch> searchReferences(final MethodElement method, final SearchScope scope,
      final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(method, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(MethodElement method, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    // TODO(scheglov) use "5" when add named matches
    listener = new CountingSearchListener(4, listener);
    // exact matches
    index.getRelationships(
        method,
        IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
        newCallback(MatchKind.METHOD_INVOCATION, listener));
    index.getRelationships(
        method,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        newCallback(MatchKind.METHOD_INVOCATION, listener));
    index.getRelationships(
        method,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        newCallback(MatchKind.METHOD_REFERENCE, listener));
    index.getRelationships(
        method,
        IndexConstants.IS_ACCESSED_BY_QUALIFIED,
        newCallback(MatchKind.METHOD_REFERENCE, listener));
    // TODO(scheglov)
    // inexact matches
//    index.getRelationships(
//        new Element(IndexConstants.DYNAMIC, method.getElementName()),
//        IndexConstants.IS_INVOKED_BY_QUALIFIED,
//        new RelationshipCallbackImpl(MatchKind.METHOD_INVOCATION, listener));
  }

  @Override
  public List<SearchMatch> searchReferences(final ParameterElement parameter,
      final SearchScope scope, final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(parameter, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(ParameterElement parameter, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(2, listener);
    index.getRelationships(
        parameter,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        new RelationshipCallbackImpl(MatchKind.VARIABLE_READ, listener));
    index.getRelationships(
        parameter,
        IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
        new RelationshipCallbackImpl(MatchKind.VARIABLE_WRITE, listener));
  }

  @Override
  public List<SearchMatch> searchReferences(final String name, final SearchScope scope,
      final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(name, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(String name, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.NAME_REFERENCE, listener));
  }

  @Override
  public List<SearchMatch> searchReferences(final TypeAliasElement alias, final SearchScope scope,
      final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(alias, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(TypeAliasElement alias, SearchScope scope, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        alias,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.FUNCTION_TYPE_REFERENCE, listener));
  }

  @Override
  public void searchReferences(VariableElement variable, SearchFilter filter,
      SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(4, listener);
    index.getRelationships(
        variable,
        IndexConstants.IS_ACCESSED_BY_UNQUALIFIED,
        new RelationshipCallbackImpl(MatchKind.VARIABLE_READ, listener));
    index.getRelationships(
        variable,
        IndexConstants.IS_ACCESSED_BY_QUALIFIED,
        new RelationshipCallbackImpl(MatchKind.VARIABLE_READ, listener));
    index.getRelationships(
        variable,
        IndexConstants.IS_MODIFIED_BY_UNQUALIFIED,
        new RelationshipCallbackImpl(MatchKind.VARIABLE_WRITE, listener));
    index.getRelationships(
        variable,
        IndexConstants.IS_MODIFIED_BY_QUALIFIED,
        new RelationshipCallbackImpl(MatchKind.VARIABLE_WRITE, listener));
  }

  @Override
  public List<SearchMatch> searchReferences(final VariableElement variable,
      final SearchScope scope, final SearchFilter filter) throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchReferences(variable, filter, listener);
      }
    });
  }

  @Override
  public List<SearchMatch> searchSubtypes(final ClassElement type, final SearchFilter filter)
      throws SearchException {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchSubtypes(type, filter, listener);
      }
    });
  }

  @Override
  public void searchSubtypes(ClassElement type, SearchFilter filter, SearchListener listener)
      throws SearchException {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(3, listener);
    index.getRelationships(
        type,
        IndexConstants.IS_EXTENDED_BY,
        newCallback(MatchKind.EXTENDS_REFERENCE, listener));
    index.getRelationships(
        type,
        IndexConstants.IS_MIXED_IN_BY,
        newCallback(MatchKind.WITH_REFERENCE, listener));
    index.getRelationships(
        type,
        IndexConstants.IS_IMPLEMENTED_BY,
        newCallback(MatchKind.IMPLEMENTS_REFERENCE, listener));
  }

////  @Override
////  public List<SearchMatch> searchSupertypes(final Type type, final SearchScope scope,
////      final SearchFilter filter) throws SearchException {
////    return gatherResults(new SearchRunner() {
////      @Override
////      public void performSearch(SearchListener listener) throws SearchException {
////        searchSupertypes(type, scope, filter, listener);
////      }
////    });
////  }
////
////  @Override
////  public void searchSupertypes(Type type, SearchScope scope, SearchFilter filter,
////      SearchListener listener) throws SearchException {
////    if (listener == null) {
////      throw new IllegalArgumentException("listener cannot be null");
////    }
////    SearchListener filteredListener = new CountingSearchListener(2, applyFilter(filter, listener));
////    index.getRelationships(
////        createElement(type),
////        IndexConstants.EXTENDS,
////        new RelationshipCallbackImpl(MatchKind.TYPE_REFERENCE, filteredListener));
////    index.getRelationships(
////        createElement(type),
////        IndexConstants.IMPLEMENTS,
////        new RelationshipCallbackImpl(MatchKind.TYPE_REFERENCE, filteredListener));
////  }

  @Override
  public List<SearchMatch> searchTypeDeclarations(SearchScope scope, final SearchPattern pattern,
      final SearchFilter filter) throws SearchException {
    final Element[] elements = createElements(scope);
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchTypeDeclarations(elements, pattern, filter, listener);
      }
    });
  }

  @Override
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
      SearchListener listener) throws SearchException {
    searchTypeDeclarations(createElements(scope), pattern, filter, listener);
  }

  @Override
  public List<SearchMatch> searchVariableDeclarations(final SearchScope scope,
      final SearchPattern pattern, final SearchFilter filter) throws SearchException {
    final Element[] elements = createElements(scope);
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) throws SearchException {
        searchVariableDeclarations(elements, pattern, filter, listener);
      }
    });
  }

  @Override
  public void searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener) throws SearchException {
    searchVariableDeclarations(createElements(scope), pattern, filter, listener);
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

//  private Element createElement(DartClassTypeAlias alias) throws SearchException {
//    return new Element(
//        getResource(alias.getCompilationUnit()),
//        ElementFactory.composeElementId(alias.getElementName()));
//  }
//
//  private Element createElement(Element element) throws SearchException {
//    if (element == null || element instanceof CompilationUnit) {
//      return null;
//    } else if (element instanceof DartFunction) {
//      return createElement((DartFunction) element);
//    } else if (element instanceof DartClassTypeAlias) {
//      return createElement((DartClassTypeAlias) element);
//    } else if (element instanceof DartFunctionTypeAlias) {
//      return createElement((DartFunctionTypeAlias) element);
//    } else if (element instanceof DartVariableDeclaration) {
//      return createElement((DartVariableDeclaration) element);
//    } else if (element instanceof Field) {
//      return createElement((Field) element);
//    } else if (element instanceof Method) {
//      return createElement((Method) element);
//    } else if (element instanceof Type) {
//      return createElement((Type) element);
//    } else if (element instanceof DartImport) {
//      return createElement((DartImport) element);
//    } else {
//      return createElement(element.getParent());
//    }
//  }
//
//  private Element createElement(DartImport imprt) throws SearchException {
//    CompilationUnit unit = imprt.getCompilationUnit();
//    return new Element(getResource(unit), imprt.getPrefix() + ":"
//        + imprt.getLibrary().getElementName());
//  }
//
//  private Element createElement(DartVariableDeclaration variable) throws SearchException {
//    return new Element(
//        getResource(variable.getCompilationUnit()),
//        ElementFactory.composeElementId(variable.getElementName()));
//  }
//
//  private Element createElement(IFile file) throws SearchException {
//    return new Element(getResource(file), "");
//  }

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

//  private Element createMethodParameterElement(DartVariableDeclaration parameter)
//      throws SearchException {
//    DartFunction function = (DartFunction) parameter.getParent();
//    Element functionElement = createElement(function);
//    return new Element(functionElement.getResource(), functionElement.getElementId()
//        + ResourceFactory.SEPARATOR_CHAR + parameter.getElementName());
//  }

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

//  private Resource getResource(IFile file) throws SearchException {
//    try {
//      return ResourceFactory.getResource(file);
//    } catch (DartModelException exception) {
//      throw new SearchException(exception);
//    }
//  }
//
////  private void searchConstructorDeclarations(Element[] elements, SearchPattern pattern,
////      SearchFilter filter, SearchListener listener)
////      throws SearchException {
////    if (listener == null) {
////      throw new IllegalArgumentException("listener cannot be null");
////    }
////    SearchListener filteredListener = new CountingSearchListener(elements.length, applyFilter(
////        filter,
////        applyPattern(pattern, new ConstructorConverter(listener))));
////    for (Element element : elements) {
////      index.getRelationships(element, IndexConstants.DEFINES_CLASS, new RelationshipCallbackImpl(
////          MatchKind.NOT_A_REFERENCE,
////          filteredListener));
////    }
////  }
////
////  private void searchFieldDeclarations(Element[] elements, SearchPattern pattern,
////      SearchFilter filter, SearchListener listener)
////      throws SearchException {
////    if (listener == null) {
////      throw new IllegalArgumentException("listener cannot be null");
////    }
////    SearchListener filteredListener = new CountingSearchListener(elements.length, applyFilter(
////        filter,
////        applyPattern(pattern, listener)));
////    for (Element element : elements) {
////      index.getRelationships(element, IndexConstants.DEFINES_FIELD, new RelationshipCallbackImpl(
////          MatchKind.NOT_A_REFERENCE,
////          filteredListener));
////    }
////  }

  private void searchFunctionDeclarations(Element[] elements, SearchPattern pattern,
      SearchFilter filter, SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyPattern(pattern, listener);
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(elements.length, listener);
    for (Element element : elements) {
      index.getRelationships(
          element,
          IndexConstants.DEFINES_FUNCTION,
          newCallback(MatchKind.NOT_A_REFERENCE, listener));
    }
  }

////  private void searchMethodDeclarations(Element[] elements, SearchPattern pattern,
////      SearchFilter filter, SearchListener listener)
////      throws SearchException {
////    if (listener == null) {
////      throw new IllegalArgumentException("listener cannot be null");
////    }
////    SearchListener filteredListener = new CountingSearchListener(elements.length, applyFilter(
////        filter,
////        applyPattern(pattern, listener)));
////    for (Element element : elements) {
////      index.getRelationships(element, IndexConstants.DEFINES_METHOD, new RelationshipCallbackImpl(
////          MatchKind.NOT_A_REFERENCE,
////          filteredListener));
////    }
////  }

  private void searchTypeDeclarations(Element[] elements, SearchPattern pattern,
      SearchFilter filter, SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyPattern(pattern, listener);
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(elements.length * 3, listener);
    for (Element element : elements) {
      index.getRelationships(
          element,
          IndexConstants.DEFINES_CLASS,
          newCallback(MatchKind.NOT_A_REFERENCE, listener));
      index.getRelationships(
          element,
          IndexConstants.DEFINES_CLASS_ALIAS,
          newCallback(MatchKind.NOT_A_REFERENCE, listener));
      index.getRelationships(
          element,
          IndexConstants.DEFINES_FUNCTION_TYPE,
          newCallback(MatchKind.NOT_A_REFERENCE, listener));
    }
  }

  private void searchVariableDeclarations(Element[] elements, SearchPattern pattern,
      SearchFilter filter, SearchListener listener) throws SearchException {
    assert listener != null;
    listener = applyPattern(pattern, listener);
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(elements.length, listener);
    for (Element element : elements) {
      index.getRelationships(
          element,
          IndexConstants.DEFINES_VARIABLE,
          newCallback(MatchKind.NOT_A_REFERENCE, listener));
    }
  }
}
