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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassMemberElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.util.HierarchyUtils;
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
  private final SearchEngine searchEngine;
  private final SearchResultConverter converter;
  private final AnalysisContext context;
  private final com.google.dart.server.Element element;
  private final boolean withPotential;
  private final SearchResultsConsumer consumer;

  public ElementReferencesComputer(SearchEngine searchEngine,
      Function<AnalysisContext, String> contextToIdFunction, AnalysisContext context,
      com.google.dart.server.Element element, boolean withPotential, SearchResultsConsumer consumer) {
    this.searchEngine = searchEngine;
    this.converter = new SearchResultConverter(contextToIdFunction);
    this.withPotential = withPotential;
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
        SearchResult result = converter.newSearchResult(
            SearchResultKind.VARIABLE_DECLARATION,
            false,
            refElement,
            refElement.getNameOffset(),
            refElement.getName().length());
        consumer.computed(new SearchResult[] {result}, false);
      }
      // do search
      List<SearchResult> results = Lists.newArrayList();
      List<SearchMatch> searchMatches = searchEngine.searchReferences(refElement, null, null);
      for (SearchMatch match : searchMatches) {
        SearchResult result = converter.newSearchResult(match, false);
        if (result == null) {
          continue;
        }
        results.add(result);
      }
      consumer.computed(results.toArray(new SearchResult[results.size()]), false);
    }
    // report potential references
    if (withPotential) {
      List<SearchResult> results = Lists.newArrayList();
      List<SearchMatch> matches = searchEngine.searchQualifiedMemberReferences(
          element.getName(),
          null,
          null);
      for (SearchMatch match : matches) {
        MatchKind kind = match.getKind();
        if (kind == MatchKind.NAME_INVOCATION_UNRESOLVED || kind == MatchKind.NAME_READ_UNRESOLVED
            || kind == MatchKind.NAME_READ_WRITE_UNRESOLVED
            || kind == MatchKind.NAME_WRITE_UNRESOLVED) {
          SearchResult result = converter.newSearchResult(match, true);
          if (result != null) {
            results.add(result);
          }
        }
      }
      consumer.computed(results.toArray(new SearchResult[results.size()]), false);
    }
  }

  private Element findEngineElement() {
    // TODO (jwren) Element API has changed
//    String elementLocationEncoding = element.getId();
//    ElementLocationImpl elementLocation = new ElementLocationImpl(elementLocationEncoding);
//    return context.getElement(elementLocation);
    return null;
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
}
