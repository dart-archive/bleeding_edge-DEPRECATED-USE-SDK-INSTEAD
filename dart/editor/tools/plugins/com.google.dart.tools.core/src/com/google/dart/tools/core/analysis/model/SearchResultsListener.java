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

package com.google.dart.tools.core.analysis.model;

import com.google.dart.server.generated.types.SearchResult;

import java.util.List;

/**
 * Used by {@link AnalysisServerData} to notify clients that new {@link SearchResult}s are ready.
 * 
 * @coverage dart.tools.core.model
 */
public interface SearchResultsListener {
  /**
   * Called when {@link SearchResult}s for a particular search request are ready.
   */
  void computedSearchResults(List<SearchResult> results, boolean last);
}
