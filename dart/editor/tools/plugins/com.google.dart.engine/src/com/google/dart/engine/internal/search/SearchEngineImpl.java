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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.LocationWithData;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.index.RelationshipCallback;
import com.google.dart.engine.internal.element.member.Member;
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
import com.google.dart.engine.search.SearchFilter;
import com.google.dart.engine.search.SearchListener;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.search.SearchPattern;
import com.google.dart.engine.search.SearchScope;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.SourceRange;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Implementation of {@link SearchEngine}.
 * 
 * @coverage dart.engine.search
 */
public class SearchEngineImpl implements SearchEngine {

  /**
   * Instances of the class <code>RelationshipCallbackImpl</code> implement a callback that can be
   * used to report results to a search listener.
   */
  private static class RelationshipCallbackImpl implements RelationshipCallback {
    private final SearchScope scope;
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
     * @param scope the {@link SearchScope} to return matches from, may be {@code null} to return
     *          all matches
     * @param matchKind the kind of matches that are represented by the results
     * @param listener the search listener that should be notified when results are found
     */
    public RelationshipCallbackImpl(SearchScope scope, MatchKind matchKind, SearchListener listener) {
      this.scope = scope;
      this.matchKind = matchKind;
      this.listener = listener;
    }

    @Override
    public void hasRelationships(Element element, Relationship relationship, Location[] locations) {
      for (Location location : locations) {
        Element targetElement = location.getElement();
        // check scope
        if (scope != null && !scope.encloses(targetElement)) {
          continue;
        }
        SourceRange range = new SourceRange(location.getOffset(), location.getLength());
        // TODO(scheglov) IndexConstants.DYNAMIC for MatchQuality.NAME
        MatchQuality quality = MatchQuality.EXACT;
//          MatchQuality quality = element.getResource() != IndexConstants.DYNAMIC
//              ? MatchQuality.EXACT : MatchQuality.NAME;
        SearchMatch match = new SearchMatch(quality, matchKind, targetElement, range);
        match.setQualified(relationship == IndexConstants.IS_REFERENCED_BY_QUALIFIED
            || relationship == IndexConstants.IS_INVOKED_BY_QUALIFIED);
        listener.matchFound(match);
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
     * @param listener the listener to which search results should be passed @ if the results could
     *          not be computed
     */
    public void performSearch(SearchListener listener);
  }

  /**
   * Apply the given filter to the given listener.
   * 
   * @param filter the filter to be used before passing matches on to the listener, or {@code null}
   *          if all matches should be passed on
   * @param listener the listener that will only be given matches that pass the filter
   * @return a search listener that will pass to the given listener any matches that pass the given
   *         filter
   */
  private static SearchListener applyFilter(SearchFilter filter, SearchListener listener) {
    if (filter == null) {
      return listener;
    }
    return new FilteredSearchListener(filter, listener);
  }

  /**
   * Apply the given pattern to the given listener.
   * 
   * @param pattern the pattern to be used before passing matches on to the listener, or
   *          {@code null} if all matches should be passed on
   * @param listener the listener that will only be given matches that match the pattern
   * @return a search listener that will pass to the given listener any matches that match the given
   *         pattern
   */
  private static SearchListener applyPattern(SearchPattern pattern, SearchListener listener) {
    if (pattern == null) {
      return listener;
    }
    return new NameMatchingSearchListener(pattern, listener);
  }

  private static Element[] createElements(SearchScope scope) {
    if (scope instanceof LibrarySearchScope) {
      return ((LibrarySearchScope) scope).getLibraries();
    }
    return new Element[] {IndexConstants.UNIVERSE};
  }

  private static RelationshipCallback newCallback(MatchKind matchKind, SearchScope scope,
      SearchListener listener) {
    return new RelationshipCallbackImpl(scope, matchKind, listener);
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
  public Set<Type> searchAssignedTypes(PropertyInducingElement variable, SearchScope scope) {
    PropertyAccessorElement setter = variable.getSetter();
    int numRequests = (setter != null ? 2 : 0) + 2;
    // find locations
    final List<Location> locations = Lists.newArrayList();
    final CountDownLatch latch = new CountDownLatch(numRequests);
    class Callback implements RelationshipCallback {
      @Override
      public void hasRelationships(Element element, Relationship relationship, Location[] locs) {
        Collections.addAll(locations, locs);
        latch.countDown();
      }
    }
    if (setter != null) {
      index.getRelationships(setter, IndexConstants.IS_REFERENCED_BY_QUALIFIED, new Callback());
      index.getRelationships(setter, IndexConstants.IS_REFERENCED_BY_UNQUALIFIED, new Callback());
    }
    index.getRelationships(variable, IndexConstants.IS_REFERENCED_BY, new Callback());
    index.getRelationships(variable, IndexConstants.IS_DEFINED_BY, new Callback());
    Uninterruptibles.awaitUninterruptibly(latch);
    // get types from locations
    Set<Type> types = Sets.newHashSet();
    for (Location location : locations) {
      // check scope
      if (scope != null) {
        Element targetElement = location.getElement();
        if (!scope.encloses(targetElement)) {
          continue;
        }
      }
      // we need data
      if (!(location instanceof LocationWithData<?>)) {
        continue;
      }
      LocationWithData<?> locationWithData = (LocationWithData<?>) location;
      // add type
      Object data = locationWithData.getData();
      if (data instanceof Type) {
        Type type = (Type) data;
        types.add(type);
      }
    }
    // done
    return types;
  }

  @Override
  public List<SearchMatch> searchDeclarations(final String name, final SearchScope scope,
      final SearchFilter filter) {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) {
        searchDeclarations(name, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchDeclarations(String name, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.IS_DEFINED_BY,
        newCallback(MatchKind.NAME_DECLARATION, scope, listener));
  }

  @Override
  public List<SearchMatch> searchFunctionDeclarations(final SearchScope scope,
      final SearchPattern pattern, final SearchFilter filter) {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) {
        searchFunctionDeclarations(scope, pattern, filter, listener);
      }
    });
  }

  @Override
  public void searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener) {
    assert listener != null;
    Element[] elements = createElements(scope);
    listener = applyPattern(pattern, listener);
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(elements.length, listener);
    for (Element element : elements) {
      index.getRelationships(
          element,
          IndexConstants.DEFINES_FUNCTION,
          newCallback(MatchKind.FUNCTION_DECLARATION, scope, listener));
    }
  }

  @Override
  public List<SearchMatch> searchQualifiedMemberReferences(final String name,
      final SearchScope scope, final SearchFilter filter) {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) {
        searchQualifiedMemberReferences(name, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchQualifiedMemberReferences(String name, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(10, listener);
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED,
        newCallback(MatchKind.NAME_REFERENCE_RESOLVED, scope, listener));
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED,
        newCallback(MatchKind.NAME_REFERENCE_UNRESOLVED, scope, listener));
    // granular resolved operations
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.NAME_IS_INVOKED_BY_RESOLVED,
        newCallback(MatchKind.NAME_INVOCATION_RESOLVED, scope, listener));
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.NAME_IS_READ_BY_RESOLVED,
        newCallback(MatchKind.NAME_READ_RESOLVED, scope, listener));
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.NAME_IS_READ_WRITTEN_BY_RESOLVED,
        newCallback(MatchKind.NAME_READ_WRITE_RESOLVED, scope, listener));
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.NAME_IS_WRITTEN_BY_RESOLVED,
        newCallback(MatchKind.NAME_WRITE_RESOLVED, scope, listener));
    // granular unresolved operations
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.NAME_IS_INVOKED_BY_UNRESOLVED,
        newCallback(MatchKind.NAME_INVOCATION_UNRESOLVED, scope, listener));
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.NAME_IS_READ_BY_UNRESOLVED,
        newCallback(MatchKind.NAME_READ_UNRESOLVED, scope, listener));
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.NAME_IS_READ_WRITTEN_BY_UNRESOLVED,
        newCallback(MatchKind.NAME_READ_WRITE_UNRESOLVED, scope, listener));
    index.getRelationships(
        new NameElementImpl(name),
        IndexConstants.NAME_IS_WRITTEN_BY_UNRESOLVED,
        newCallback(MatchKind.NAME_WRITE_UNRESOLVED, scope, listener));
  }

  @Override
  public List<SearchMatch> searchReferences(final Element element, final SearchScope scope,
      final SearchFilter filter) {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) {
        searchReferences(element, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchReferences(Element element, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    if (element == null) {
      listener.searchComplete();
      return;
    }
    if (element instanceof Member) {
      element = ((Member) element).getBaseElement();
    }
    switch (element.getKind()) {
      case ANGULAR_COMPONENT:
      case ANGULAR_CONTROLLER:
      case ANGULAR_FORMATTER:
      case ANGULAR_PROPERTY:
      case ANGULAR_SCOPE_PROPERTY:
      case ANGULAR_SELECTOR:
        searchReferences((AngularElement) element, scope, filter, listener);
        return;
      case CLASS:
        searchReferences((ClassElement) element, scope, filter, listener);
        return;
      case COMPILATION_UNIT:
        searchReferences((CompilationUnitElement) element, scope, filter, listener);
        return;
      case CONSTRUCTOR:
        searchReferences((ConstructorElement) element, scope, filter, listener);
        return;
      case FIELD:
      case TOP_LEVEL_VARIABLE:
        searchReferences((PropertyInducingElement) element, scope, filter, listener);
        return;
      case FUNCTION:
        searchReferences((FunctionElement) element, scope, filter, listener);
        return;
      case GETTER:
      case SETTER:
        searchReferences((PropertyAccessorElement) element, scope, filter, listener);
        return;
      case IMPORT:
        searchReferences((ImportElement) element, scope, filter, listener);
        return;
      case LIBRARY:
        searchReferences((LibraryElement) element, scope, filter, listener);
        return;
      case LOCAL_VARIABLE:
        searchReferences((LocalVariableElement) element, scope, filter, listener);
        return;
      case METHOD:
        searchReferences((MethodElement) element, scope, filter, listener);
        return;
      case PARAMETER:
        searchReferences((ParameterElement) element, scope, filter, listener);
        return;
      case FUNCTION_TYPE_ALIAS:
        searchReferences((FunctionTypeAliasElement) element, scope, filter, listener);
        return;
      case TYPE_PARAMETER:
        searchReferences((TypeParameterElement) element, scope, filter, listener);
        return;
      default:
        listener.searchComplete();
        return;
    }
  }

  @Override
  public List<SearchMatch> searchSubtypes(final ClassElement type, final SearchScope scope,
      final SearchFilter filter) {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) {
        searchSubtypes(type, scope, filter, listener);
      }
    });
  }

  @Override
  public void searchSubtypes(ClassElement type, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(3, listener);
    index.getRelationships(
        type,
        IndexConstants.IS_EXTENDED_BY,
        newCallback(MatchKind.EXTENDS_REFERENCE, scope, listener));
    index.getRelationships(
        type,
        IndexConstants.IS_MIXED_IN_BY,
        newCallback(MatchKind.WITH_REFERENCE, scope, listener));
    index.getRelationships(
        type,
        IndexConstants.IS_IMPLEMENTED_BY,
        newCallback(MatchKind.IMPLEMENTS_REFERENCE, scope, listener));
  }

  @Override
  public List<SearchMatch> searchTypeDeclarations(final SearchScope scope,
      final SearchPattern pattern, final SearchFilter filter) {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) {
        searchTypeDeclarations(scope, pattern, filter, listener);
      }
    });
  }

  @Override
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    Element[] elements = createElements(scope);
    listener = applyPattern(pattern, listener);
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(elements.length * 3, listener);
    for (Element element : elements) {
      index.getRelationships(
          element,
          IndexConstants.DEFINES_CLASS,
          newCallback(MatchKind.CLASS_DECLARATION, scope, listener));
      index.getRelationships(
          element,
          IndexConstants.DEFINES_CLASS_ALIAS,
          newCallback(MatchKind.CLASS_ALIAS_DECLARATION, scope, listener));
      index.getRelationships(
          element,
          IndexConstants.DEFINES_FUNCTION_TYPE,
          newCallback(MatchKind.FUNCTION_TYPE_DECLARATION, scope, listener));
    }
  }

  @Override
  public List<SearchMatch> searchVariableDeclarations(final SearchScope scope,
      final SearchPattern pattern, final SearchFilter filter) {
    return gatherResults(new SearchRunner() {
      @Override
      public void performSearch(SearchListener listener) {
        searchVariableDeclarations(scope, pattern, filter, listener);
      }
    });
  }

  @Override
  public void searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener) {
    assert listener != null;
    Element[] elements = createElements(scope);
    listener = applyPattern(pattern, listener);
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(elements.length, listener);
    for (Element element : elements) {
      index.getRelationships(
          element,
          IndexConstants.DEFINES_VARIABLE,
          newCallback(MatchKind.VARIABLE_DECLARATION, scope, listener));
    }
  }

  /**
   * Use the given runner to perform the given number of asynchronous searches, then wait until the
   * search has completed and return the results that were produced.
   * 
   * @param runner the runner used to perform an asynchronous search
   * @return the results that were produced @ if the results of at least one of the searched could
   *         not be computed
   */
  private List<SearchMatch> gatherResults(SearchRunner runner) {
    GatheringSearchListener listener = new GatheringSearchListener();
    runner.performSearch(listener);
    while (!listener.isComplete()) {
      Thread.yield();
    }
    return listener.getMatches();
  }

  private void searchReferences(AngularElement element, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(2, listener);
    index.getRelationships(
        element,
        IndexConstants.ANGULAR_REFERENCE,
        newCallback(MatchKind.ANGULAR_REFERENCE, scope, listener));
    index.getRelationships(
        element,
        IndexConstants.ANGULAR_CLOSING_TAG_REFERENCE,
        newCallback(MatchKind.ANGULAR_CLOSING_TAG_REFERENCE, scope, listener));
  }

  private void searchReferences(ClassElement type, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        type,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.TYPE_REFERENCE, scope, listener));
  }

  private void searchReferences(CompilationUnitElement unit, SearchScope scope,
      SearchFilter filter, SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        unit,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.UNIT_REFERENCE, scope, listener));
  }

  private void searchReferences(ConstructorElement constructor, SearchScope scope,
      SearchFilter filter, SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(2, listener);
    index.getRelationships(
        constructor,
        IndexConstants.IS_DEFINED_BY,
        newCallback(MatchKind.CONSTRUCTOR_DECLARATION, scope, listener));
    index.getRelationships(
        constructor,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.CONSTRUCTOR_REFERENCE, scope, listener));
  }

  private void searchReferences(FunctionElement function, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(2, listener);
    index.getRelationships(
        function,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.FUNCTION_REFERENCE, scope, listener));
    index.getRelationships(
        function,
        IndexConstants.IS_INVOKED_BY,
        newCallback(MatchKind.FUNCTION_EXECUTION, scope, listener));
  }

  private void searchReferences(FunctionTypeAliasElement alias, SearchScope scope,
      SearchFilter filter, SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        alias,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.FUNCTION_TYPE_REFERENCE, scope, listener));
  }

  private void searchReferences(ImportElement imp, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        imp,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.IMPORT_REFERENCE, scope, listener));
  }

  private void searchReferences(LibraryElement library, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        library,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.LIBRARY_REFERENCE, scope, listener));
  }

  private void searchReferences(MethodElement method, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    // TODO(scheglov) use "5" when add named matches
    listener = new CountingSearchListener(4, listener);
    // exact matches
    index.getRelationships(
        method,
        IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
        newCallback(MatchKind.METHOD_INVOCATION, scope, listener));
    index.getRelationships(
        method,
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        newCallback(MatchKind.METHOD_INVOCATION, scope, listener));
    index.getRelationships(
        method,
        IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
        newCallback(MatchKind.METHOD_REFERENCE, scope, listener));
    index.getRelationships(
        method,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        newCallback(MatchKind.METHOD_REFERENCE, scope, listener));
    // TODO(scheglov)
    // inexact matches
//    index.getRelationships(
//        new Element(IndexConstants.DYNAMIC, method.getElementName()),
//        IndexConstants.IS_INVOKED_BY_QUALIFIED,
//        newCallback(MatchKind.METHOD_INVOCATION, listener));
  }

  private void searchReferences(ParameterElement parameter, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(5, listener);
    index.getRelationships(
        parameter,
        IndexConstants.IS_READ_BY,
        newCallback(MatchKind.VARIABLE_READ, scope, listener));
    index.getRelationships(
        parameter,
        IndexConstants.IS_READ_WRITTEN_BY,
        newCallback(MatchKind.VARIABLE_READ_WRITE, scope, listener));
    index.getRelationships(
        parameter,
        IndexConstants.IS_WRITTEN_BY,
        newCallback(MatchKind.VARIABLE_WRITE, scope, listener));
    index.getRelationships(
        parameter,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.NAMED_PARAMETER_REFERENCE, scope, listener));
    index.getRelationships(
        parameter,
        IndexConstants.IS_INVOKED_BY,
        newCallback(MatchKind.FUNCTION_EXECUTION, scope, listener));
  }

  private void searchReferences(PropertyAccessorElement accessor, SearchScope scope,
      SearchFilter filter, SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(2, listener);
    index.getRelationships(
        accessor,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        newCallback(MatchKind.PROPERTY_ACCESSOR_REFERENCE, scope, listener));
    index.getRelationships(
        accessor,
        IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
        newCallback(MatchKind.PROPERTY_ACCESSOR_REFERENCE, scope, listener));
  }

  private void searchReferences(PropertyInducingElement field, SearchScope scope,
      SearchFilter filter, SearchListener listener) {
    assert listener != null;
    PropertyAccessorElement getter = field.getGetter();
    PropertyAccessorElement setter = field.getSetter();
    int numRequests = (getter != null ? 4 : 0) + (setter != null ? 2 : 0) + 2;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(numRequests, listener);
    if (getter != null) {
      index.getRelationships(
          getter,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          newCallback(MatchKind.FIELD_READ, scope, listener));
      index.getRelationships(
          getter,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          newCallback(MatchKind.FIELD_READ, scope, listener));
      index.getRelationships(
          getter,
          IndexConstants.IS_INVOKED_BY_QUALIFIED,
          newCallback(MatchKind.FIELD_INVOCATION, scope, listener));
      index.getRelationships(
          getter,
          IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
          newCallback(MatchKind.FIELD_INVOCATION, scope, listener));
    }
    if (setter != null) {
      index.getRelationships(
          setter,
          IndexConstants.IS_REFERENCED_BY_QUALIFIED,
          newCallback(MatchKind.FIELD_WRITE, scope, listener));
      index.getRelationships(
          setter,
          IndexConstants.IS_REFERENCED_BY_UNQUALIFIED,
          newCallback(MatchKind.FIELD_WRITE, scope, listener));
    }
    index.getRelationships(
        field,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.FIELD_REFERENCE, scope, listener));
    index.getRelationships(
        field,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        newCallback(MatchKind.FIELD_REFERENCE, scope, listener));
  }

  private void searchReferences(TypeParameterElement typeParameter, SearchScope scope,
      SearchFilter filter, SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    index.getRelationships(
        typeParameter,
        IndexConstants.IS_REFERENCED_BY,
        newCallback(MatchKind.TYPE_PARAMETER_REFERENCE, scope, listener));
  }

  private void searchReferences(VariableElement variable, SearchScope scope, SearchFilter filter,
      SearchListener listener) {
    assert listener != null;
    listener = applyFilter(filter, listener);
    listener = new CountingSearchListener(4, listener);
    index.getRelationships(
        variable,
        IndexConstants.IS_READ_BY,
        newCallback(MatchKind.VARIABLE_READ, scope, listener));
    index.getRelationships(
        variable,
        IndexConstants.IS_READ_WRITTEN_BY,
        newCallback(MatchKind.VARIABLE_READ_WRITE, scope, listener));
    index.getRelationships(
        variable,
        IndexConstants.IS_WRITTEN_BY,
        newCallback(MatchKind.VARIABLE_WRITE, scope, listener));
    index.getRelationships(
        variable,
        IndexConstants.IS_INVOKED_BY,
        newCallback(MatchKind.FUNCTION_EXECUTION, scope, listener));
  }
}
