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
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultKind;
import com.google.dart.server.SearchResultsConsumer;

import java.util.List;

/**
 * A computer for reference {@link SearchResult}s.
 * 
 * @coverage dart.server.local
 */
@DartOmit
public class DartUnitReferencesComputer {
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

  private SearchEngine searchEngine;
  private final String contextId;
  private final Source source;
  private final CompilationUnit unit;
  private final int offset;
  private final SearchResultsConsumer consumer;

  public DartUnitReferencesComputer(SearchEngine searchEngine, String contextId, Source source,
      CompilationUnit unit, int offset, SearchResultsConsumer consumer) {
    this.searchEngine = searchEngine;
    this.contextId = contextId;
    this.source = source;
    this.unit = unit;
    this.offset = offset;
    this.consumer = consumer;
  }

  /**
   * Computes {@link SearchResult}s and notifies the {@link SearchResultsConsumer}.
   */
  public void compute() {
    AstNode node = new NodeLocator(offset).searchWithin(unit);
    Element element = ElementLocator.locateWithOffset(node, offset);
    // tweak element
    if (element instanceof PropertyAccessorElement) {
      element = ((PropertyAccessorElement) element).getVariable();
    }
    if (element instanceof FieldFormalParameterElement) {
      element = ((FieldFormalParameterElement) element).getField();
    }
    // include variable declaration into search results
    if (isVariableLikeElement(element)) {
      SearchResultImpl result = new SearchResultImpl(
          computePath(element),
          element.getSource(),
          SearchResultKind.VARIABLE_DECLARATION,
          element.getNameOffset(),
          element.getName().length());
      consumer.computedReferences(contextId, source, offset, new SearchResult[] {result}, false);
    }
    // do search
    if (element != null) {
      List<SearchResult> results = Lists.newArrayList();
      List<SearchMatch> searchMatches = searchEngine.searchReferences(element, null, null);
      for (SearchMatch match : searchMatches) {
        SearchResultImpl result = newSearchResult(match);
        if (result == null) {
          continue;
        }
        results.add(result);
      }
      consumer.computedReferences(
          contextId,
          source,
          offset,
          results.toArray(new SearchResult[results.size()]),
          false);
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
