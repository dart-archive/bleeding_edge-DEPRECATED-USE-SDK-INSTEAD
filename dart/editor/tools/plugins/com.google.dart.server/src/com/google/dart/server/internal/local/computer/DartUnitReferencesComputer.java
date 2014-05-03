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
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.server.Outline;
import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultKind;
import com.google.dart.server.SearchResultsConsumer;

import java.util.List;

/**
 * A computer for reference {@link SearchResult}s.
 * 
 * @coverage dart.server.local
 */
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
      case PROPERTY_ACCESSOR_REFERENCE:
        return SearchResultKind.PROPERTY_ACCESSOR_REFERENCE;
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
          true);
    }
    // done
    consumer.computedReferences(contextId, source, offset, SearchResult.EMPTY_ARRAY, true);
  }

  private Outline newOutline(Element element) {
    return newOutline_withChildren(element, Outline.EMPTY_ARRAY);
  }

  private OutlineImpl newOutline_withChildren(Element engineElement, Outline[] children) {
    Element engineEnclosingElement = engineElement.getEnclosingElement();
    // use only these elements as a scope
    switch (engineElement.getKind()) {
      case CLASS:
      case COMPILATION_UNIT:
      case CONSTRUCTOR:
      case FUNCTION:
      case FUNCTION_TYPE_ALIAS:
      case LIBRARY:
      case METHOD:
        break;
      default:
        return newOutline_withChildren(engineEnclosingElement, children);
    }
    // prepare parent
    OutlineImpl parent = null;
    Outline[] parentChildren = null;
    if (engineEnclosingElement != null) {
      parentChildren = new Outline[1];
      parent = newOutline_withChildren(engineEnclosingElement, parentChildren);
    }
    // new outline
    ElementImpl element = ElementImpl.create(engineElement);
    OutlineImpl outline = new OutlineImpl(parent, element, new SourceRegionImpl(0, 0));
    outline.setChildren(children);
    // done
    if (parentChildren != null) {
      parentChildren[0] = outline;
    }
    return outline;
  }

  private SearchResultImpl newSearchResult(SearchMatch match) {
    Element matchElement = match.getElement();
    MatchKind matchKind = match.getKind();
    SourceRange matchRange = match.getSourceRange();
    Outline path = newOutline(matchElement);
    SearchResultKind kind = getSearchResultKind(matchKind);
    if (kind == null) {
      return null;
    }
    return new SearchResultImpl(
        path,
        matchElement.getSource(),
        kind,
        matchRange.getOffset(),
        matchRange.getLength());
  }
}
