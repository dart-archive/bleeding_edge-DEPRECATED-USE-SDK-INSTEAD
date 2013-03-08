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

import com.google.dart.engine.search.SearchFilter;
import com.google.dart.engine.search.SearchListener;
import com.google.dart.engine.search.SearchMatch;

/**
 * Instances of the class <code>FilteredSearchListener</code> implement a search listener that
 * delegates to another search listener after removing matches that do not pass a given filter.
 * 
 * @coverage dart.engine.search
 */
public class FilteredSearchListener extends WrappedSearchListener {
  /**
   * The filter used to filter the matches.
   */
  private final SearchFilter filter;

  /**
   * Initialize a newly created search listener to pass on any matches that pass the given filter to
   * the given listener.
   * 
   * @param filter the filter used to filter the matches
   * @param listener the search listener being wrapped
   */
  public FilteredSearchListener(SearchFilter filter, SearchListener listener) {
    super(listener);
    this.filter = filter;
  }

  @Override
  public void matchFound(SearchMatch match) {
    if (filter.passes(match)) {
      propagateMatch(match);
    }
  }
}
