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
package com.google.dart.engine.internal.search.listener;

import com.google.common.collect.Lists;
import com.google.dart.engine.internal.search.SearchEngineImpl;
import com.google.dart.engine.search.SearchListener;
import com.google.dart.engine.search.SearchMatch;

import java.util.Collections;
import java.util.List;

/**
 * {@link SearchListener} used by {@link SearchEngineImpl} internally to gather asynchronous results
 * and return them synchronously.
 * 
 * @coverage dart.engine.search
 */
public class GatheringSearchListener implements SearchListener {
  /**
   * A list containing the matches that have been found so far.
   */
  private final List<SearchMatch> matches = Lists.newArrayList();

  /**
   * A flag indicating whether the search is complete.
   */
  private volatile boolean isComplete = false;

  /**
   * @return the the matches that have been found.
   */
  public List<SearchMatch> getMatches() {
    Collections.sort(matches, SearchMatch.SORT_BY_ELEMENT_NAME);
    return matches;
  }

  /**
   * Return {@code true} if the search is complete.
   * 
   * @return {@code true} if the search is complete
   */
  public boolean isComplete() {
    return isComplete;
  }

  @Override
  public void matchFound(SearchMatch match) {
    matches.add(match);
  }

  @Override
  public void searchComplete() {
    isComplete = true;
  }
}
