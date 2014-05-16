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
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultKind;
import com.google.dart.server.SearchResultsConsumer;

import java.util.List;

/**
 * A computer for declarations of class members with the given name.
 * 
 * @coverage dart.server.local
 */
@DartOmit
public class ClassMemberDeclarationsComputer {
  private final SearchEngine searchEngine;
  private final SearchResultConverter converter;
  private final String name;
  private final SearchResultsConsumer consumer;

  public ClassMemberDeclarationsComputer(SearchEngine searchEngine,
      Function<AnalysisContext, String> contextToIdFunction, String name,
      SearchResultsConsumer consumer) {
    this.searchEngine = searchEngine;
    this.converter = new SearchResultConverter(contextToIdFunction);
    this.name = name;
    this.consumer = consumer;
  }

  /**
   * Computes {@link SearchResult}s and notifies the {@link SearchResultsConsumer}.
   */
  public void compute() {
    List<SearchResult> results = Lists.newArrayList();
    List<SearchMatch> matches = searchEngine.searchDeclarations(name, null, null);
    for (SearchMatch match : matches) {
      Element element = match.getElement();
      if (element.getEnclosingElement() instanceof ClassElement) {
        SearchResult result = converter.newSearchResult(
            match,
            SearchResultKind.CLASS_MEMBER_DECLARATION,
            false);
        if (result != null) {
          results.add(result);
        }
      }
    }
    consumer.computed(results.toArray(new SearchResult[results.size()]), false);
  }
}
