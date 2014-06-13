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
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultKind;

import java.util.List;

/**
 * A helper to convert {@link SearchMatch} instances into {@link SearchResult}s.
 * 
 * @coverage dart.server.local
 */
@DartOmit
public class SearchResultConverter {
  /**
   * This is used only for testing purposes and allows tests to check the behavior in case an
   * unknown {@link MatchKind}.
   */
  @VisibleForTesting
  public static boolean test_simulateUnknownMatchKind = false;

  private final Function<AnalysisContext, String> contextToIdFunction;

  public SearchResultConverter(Function<AnalysisContext, String> contextToIdFunction) {
    this.contextToIdFunction = contextToIdFunction;
  }

  /**
   * Creates a new {@link SearchResult} from the given {@link SearchMatch}.
   */
  public SearchResult newSearchResult(SearchMatch match, boolean isPotential) {
    MatchKind matchKind = match.getKind();
    SearchResultKind kind = getSearchResultKind(matchKind);
    if (kind == null) {
      return null;
    }
    return newSearchResult(match, kind, isPotential);
  }

  /**
   * Creates a new {@link SearchResult} from the given information.
   */
  public SearchResult newSearchResult(SearchMatch match, SearchResultKind kind, boolean isPotential) {
    Element matchElement = match.getElement();
    SourceRange matchRange = match.getSourceRange();
    return newSearchResult(
        kind,
        isPotential,
        matchElement,
        matchRange.getOffset(),
        matchRange.getLength());
  }

  /**
   * Creates a new {@link SearchResult} from the given information.
   */
  public SearchResult newSearchResult(SearchResultKind kind, boolean isPotential, Element element,
      int offset, int length) {
    return new SearchResultImpl(
        computePath(element),
        element.getSource(),
        kind,
        offset,
        length,
        isPotential);
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
          AnalysisContext context = engineElement.getContext();
          String contextId = contextToIdFunction.apply(context);
          // TODO (jwren) Element API has changed
//          ElementImpl element = ElementImpl.create(contextId, engineElement);
//          path.add(element);
          break;
        default:
          break;
      }
      engineElement = engineElement.getEnclosingElement();
    }
    return path.toArray(new com.google.dart.server.Element[path.size()]);
  }

  /**
   * Returns the {@link SearchResultKind} that corresponds to the {@link MatchKind}, may be
   * {@code null} if unknown.
   */
  private SearchResultKind getSearchResultKind(MatchKind matchKind) {
    if (test_simulateUnknownMatchKind) {
      matchKind = MatchKind.WITH_REFERENCE;
    }
    switch (matchKind) {
      case CLASS_DECLARATION:
        return SearchResultKind.CLASS_DECLARATION;
      case CONSTRUCTOR_REFERENCE:
        return SearchResultKind.CONSTRUCTOR_REFERENCE;
      case FIELD_REFERENCE:
        return SearchResultKind.FIELD_REFERENCE;
      case FIELD_READ:
      case NAME_READ_RESOLVED:
      case NAME_READ_UNRESOLVED:
        return SearchResultKind.FIELD_READ;
      case NAME_READ_WRITE_RESOLVED:
      case NAME_READ_WRITE_UNRESOLVED:
        return SearchResultKind.FIELD_READ_WRITE;
      case FIELD_WRITE:
      case NAME_WRITE_RESOLVED:
      case NAME_WRITE_UNRESOLVED:
        return SearchResultKind.FIELD_WRITE;
      case FUNCTION_DECLARATION:
        return SearchResultKind.FUNCTION_DECLARATION;
      case FUNCTION_EXECUTION:
        return SearchResultKind.FUNCTION_INVOCATION;
      case FUNCTION_REFERENCE:
        return SearchResultKind.FUNCTION_REFERENCE;
      case FUNCTION_TYPE_DECLARATION:
        return SearchResultKind.FUNCTION_TYPE_DECLARATION;
      case METHOD_INVOCATION:
      case NAME_INVOCATION_RESOLVED:
      case NAME_INVOCATION_UNRESOLVED:
        return SearchResultKind.METHOD_INVOCATION;
      case METHOD_REFERENCE:
        return SearchResultKind.METHOD_REFERENCE;
      case TYPE_REFERENCE:
      case FUNCTION_TYPE_REFERENCE:
      case TYPE_PARAMETER_REFERENCE:
        return SearchResultKind.TYPE_REFERENCE;
      case VARIABLE_DECLARATION:
        return SearchResultKind.VARIABLE_DECLARATION;
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
}
