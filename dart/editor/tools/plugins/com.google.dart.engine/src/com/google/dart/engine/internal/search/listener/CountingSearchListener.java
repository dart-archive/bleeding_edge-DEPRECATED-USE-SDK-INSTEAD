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

import com.google.dart.engine.search.SearchListener;
import com.google.dart.engine.search.SearchMatch;

/**
 * Instances of the class {@code CountingSearchListener} listen for search results, passing those
 * results on to a wrapped listener, but ensure that the wrapped search listener receives only one
 * notification that the search is complete.
 * 
 * @coverage dart.engine.search
 */
public class CountingSearchListener implements SearchListener {
  /**
   * The number of times that this listener expects to be told that the search is complete before
   * passing the information along to the wrapped listener.
   */
  private int completionCount;

  /**
   * The listener that will be notified as results are received and when the given number of search
   * complete notifications have been received.
   */
  private SearchListener wrappedListener;

  /**
   * Initialize a newly created search listener to pass search results on to the given listener and
   * to notify the given listener that the search is complete after getting the given number of
   * notifications.
   * 
   * @param completionCount the number of times that this listener expects to be told that the
   *          search is complete
   * @param wrappedListener the listener that will be notified as results are received
   */
  public CountingSearchListener(int completionCount, SearchListener wrappedListener) {
    this.completionCount = completionCount;
    this.wrappedListener = wrappedListener;
    if (completionCount == 0) {
      wrappedListener.searchComplete();
    }
  }

  @Override
  public void matchFound(SearchMatch match) {
    wrappedListener.matchFound(match);
  }

  @Override
  public void searchComplete() {
    completionCount--;
    if (completionCount <= 0) {
      wrappedListener.searchComplete();
    }
  }
}
