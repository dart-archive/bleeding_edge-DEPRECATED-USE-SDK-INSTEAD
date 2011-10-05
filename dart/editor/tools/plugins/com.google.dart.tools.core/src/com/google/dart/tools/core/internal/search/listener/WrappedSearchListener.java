/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.search.listener;

import com.google.dart.tools.core.search.SearchListener;
import com.google.dart.tools.core.search.SearchMatch;

/**
 * Instances of the class <code>ScopedSearchListener</code> implement a search listener that
 * delegates to another search listener after removing matches that are outside a given scope.
 */
public abstract class WrappedSearchListener implements SearchListener {
  /**
   * The listener being wrapped.
   */
  private SearchListener baseListener;

  /**
   * Initialize a newly created search listener to wrap the given listener.
   * 
   * @param listener the search listener being wrapped
   */
  public WrappedSearchListener(SearchListener listener) {
    baseListener = listener;
  }

  /**
   * Pass the given match on to the wrapped listener.
   * 
   * @param match the match to be propagated
   */
  protected void propagateMatch(SearchMatch match) {
    baseListener.matchFound(match);
  }
}
