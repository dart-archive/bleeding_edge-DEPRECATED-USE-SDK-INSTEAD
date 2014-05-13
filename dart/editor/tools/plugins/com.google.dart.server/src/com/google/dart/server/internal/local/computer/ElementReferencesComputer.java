/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.computer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassMemberElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.internal.element.ElementLocationImpl;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultKind;
import com.google.dart.server.SearchResultsConsumer;

import java.util.List;
import java.util.Set;

/**
 * A computer for reference {@link SearchResult}s.
 * 
 * @coverage dart.server.local
 */
@DartOmit
public class ElementReferencesComputer {
  /**
   * This is used only for testing purposes and allows tests to check the behavior in case an
   * unknown {@link MatchKind}.
   */
  @VisibleForTesting
  public static boolean test_simulateUknownMatchKind = false;

  /**
   * Returns the {@link SearchResultKind} that corresponds to the {@link MatchKind}, may be
   * {@code null} if unknown.
   */
  private static SearchResultKind getSearchResultKind(MatchKind matchKind) {
    if (test_simulateUknownMatchKind) {
      matchKind = MatchKind.CLASS_DECLARATION;
    }
    switch (matchKind) {
      case CONSTRUCTOR_REFERENCE:
        return SearchResultKind.CONSTRUCTOR_REFERENCE;
      case FIELD_REFERENCE:
        return SearchResultKind.FIELD_REFERENCE;
      case FIELD_READ:
        return SearchResultKind.FIELD_READ;
      case FIELD_WRITE:
        return SearchResultKind.FIELD_WRITE;
      case FUNCTION_EXECUTION:
        return SearchResultKind.FUNCTION_INVOCATION;
      case FUNCTION_REFERENCE:
        return SearchResultKind.FUNCTION_REFERENCE;
      case METHOD_INVOCATION:
        return SearchResultKind.METHOD_INVOCATION;
      case METHOD_REFERENCE:
        return SearchResultKind.METHOD_REFERENCE;
      case TYPE_REFERENCE:
      case FUNCTION_TYPE_REFERENCE:
      case TYPE_PARAMETER_REFERENCE:
        return SearchResultKind.TYPE_REFERENCE;
      case VARIABLE_READ:
        return SearchResultKind.VARIABLE_READ;
      case VARIABLE_READ_WRITE:
        return SearchResultKind.VARIABLE_READ_WRITE;
      case VARIABLE_WRITE:
        return SearchResultKind.VARIABLE_WRITE;
      default:
        return null;
    }
  }

  private final SearchEngine searchEngine;
  private final String contextId;
  private final AnalysisContext context;
  private final com.google.dart.server.Element element;
  private final SearchResultsConsumer consumer;

  public ElementReferencesComputer(SearchEngine searchEngine, AnalysisContext context,
      com.google.dart.server.Element element, SearchResultsConsumer consumer) {
    this.searchEngine = searchEngine;
    this.contextId = element.getContextId();
    this.context = context;
    this.element = element;
    this.consumer = consumer;
  }

  /**
   * Computes {@link SearchResult}s and notifies the {@link SearchResultsConsumer}.
   */
  public void compute() {
    Element element = findEngineElement();
    // tweak element
    if (element instanceof PropertyAccessorElement) {
      element = ((PropertyAccessorElement) element).getVariable();
    }
    // prepare Element(s) to find references to
    Element[] refElements = {};
    if (element != null) {
      if (element instanceof ClassMemberElement) {
        ClassMemberElement member = (ClassMemberElement) element;
        Set<ClassMemberElement> hierarchyMembers = HierarchyUtils.getHierarchyMembers(
            searchEngine,
            member);
        refElements = hierarchyMembers.toArray(new Element[hierarchyMembers.size()]);
      } else {
        refElements = new Element[] {element};
      }
    }
    // process each 'refElement'
    for (Element refElement : refElements) {
      // include variable declaration into search results
      if (isVariableLikeElement(refElement)) {
        SearchResultImpl result = new SearchResultImpl(
            computePath(refElement),
            refElement.getSource(),
            SearchResultKind.VARIABLE_DECLARATION,
            refElement.getNameOffset(),
            refElement.getName().length());
        consumer.computed(new SearchResult[] {result}, false);
      }
      // do search
      List<SearchResult> results = Lists.newArrayList();
      List<SearchMatch> searchMatches = searchEngine.searchReferences(refElement, null, null);
      for (SearchMatch match : searchMatches) {
        SearchResultImpl result = newSearchResult(match);
        if (result == null) {
          continue;
        }
        results.add(result);
      }
      consumer.computed(results.toArray(new SearchResult[results.size()]), false);
    }
  }

  private com.google.dart.server.Element[] computePath(Element engineElement) {
    List<com.google.dart.server.Element> path = Lists.newArrayList();
    while (engineElement != null) {
      switch (engineElement.getKind()) {
        case CLASS:
        case COMPILATION_UNIT:
        case CONSTRUCTOR:
        case FUNCTION:
        case FUNCTION_TYPE_ALIAS:
        case LIBRARY:
        case METHOD:
          ElementImpl element = ElementImpl.create(contextId, engineElement);
          path.add(element);
          break;
        default:
          break;
      }
      engineElement = engineElement.getEnclosingElement();
    }
    return path.toArray(new com.google.dart.server.Element[path.size()]);
  }

  private Element findEngineElement() {
    String elementLocationEncoding = element.getId();
    ElementLocationImpl elementLocation = new ElementLocationImpl(elementLocationEncoding);
    return context.getElement(elementLocation);
  }

  private boolean isVariableLikeElement(Element element) {
    if (element instanceof LocalVariableElement) {
      return true;
    }
    if (element instanceof ParameterElement) {
      return true;
    }
    if (element instanceof PropertyInducingElement) {
      return !((PropertyInducingElement) element).isSynthetic();
    }
    return false;
  }

  private SearchResultImpl newSearchResult(SearchMatch match) {
    MatchKind matchKind = match.getKind();
    SearchResultKind kind = getSearchResultKind(matchKind);
    if (kind == null) {
      return null;
    }
    Element matchElement = match.getElement();
    SourceRange matchRange = match.getSourceRange();
    return new SearchResultImpl(
        computePath(matchElement),
        matchElement.getSource(),
        kind,
        matchRange.getOffset(),
        matchRange.getLength());
  }
}
