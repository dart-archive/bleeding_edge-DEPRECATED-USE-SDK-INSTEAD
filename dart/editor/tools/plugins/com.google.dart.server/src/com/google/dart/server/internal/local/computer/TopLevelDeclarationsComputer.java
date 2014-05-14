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
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.search.SearchPattern;
import com.google.dart.engine.search.SearchPatternFactory;
import com.google.dart.engine.search.SearchScope;
import com.google.dart.engine.search.SearchScopeFactory;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultsConsumer;

import java.util.List;

/**
 * A computer for top-level declaration {@link SearchResult}s.
 * 
 * @coverage dart.server.local
 */
@DartOmit
public class TopLevelDeclarationsComputer {
  private final SearchEngine searchEngine;
  private final SearchResultConverter converter;
  private final String pattern;
  private final SearchResultsConsumer consumer;
  private final AnalysisContext scopeContext;

  public TopLevelDeclarationsComputer(SearchEngine searchEngine,
      Function<AnalysisContext, String> contextToIdFunction, AnalysisContext scopeContext,
      String pattern, SearchResultsConsumer consumer) {
    this.searchEngine = searchEngine;
    this.converter = new SearchResultConverter(contextToIdFunction);
    this.scopeContext = scopeContext;
    this.pattern = pattern;
    this.consumer = consumer;
  }

  public void compute() {
    SearchScope searchScope = createSearchScope();
    List<SearchMatch> searchMatches = Lists.newArrayList();
    SearchPattern searchPattern = SearchPatternFactory.createRegularExpressionPattern(
        pattern,
        false);
    searchMatches.addAll(searchEngine.searchTypeDeclarations(searchScope, searchPattern, null));
    searchMatches.addAll(searchEngine.searchFunctionDeclarations(searchScope, searchPattern, null));
    searchMatches.addAll(searchEngine.searchVariableDeclarations(searchScope, searchPattern, null));
    List<SearchResult> results = Lists.newArrayList();
    for (SearchMatch match : searchMatches) {
      SearchResult result = converter.newSearchResult(match, false);
      if (result != null) {
        results.add(result);
      }
    }
    consumer.computed(results.toArray(new SearchResult[results.size()]), false);
  }

  private SearchScope createSearchScope() {
    // no scope context - use universe
    if (scopeContext == null) {
      return SearchScopeFactory.createUniverseScope();
    }
    // prepare scope libraries
    List<LibraryElement> libraryElements = Lists.newArrayList();
    Source[] librarySources = scopeContext.getLibrarySources();
    for (Source librarySource : librarySources) {
      LibraryElement libraryElement = scopeContext.getLibraryElement(librarySource);
      if (libraryElement != null) {
        libraryElements.add(libraryElement);
      }
    }
    // use libraries scope
    return SearchScopeFactory.createLibraryScope(libraryElements);
  }
}
