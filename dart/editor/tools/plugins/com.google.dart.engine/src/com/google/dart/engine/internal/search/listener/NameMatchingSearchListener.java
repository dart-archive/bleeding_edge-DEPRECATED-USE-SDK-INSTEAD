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
import com.google.dart.engine.search.SearchPattern;

/**
 * Instances of the class <code>NameMatchingSearchListener</code> implement a search listener that
 * delegates to another search listener after removing matches that do not match a given pattern.
 * 
 * @coverage dart.engine.search
 */
public class NameMatchingSearchListener extends WrappedSearchListener {
  /**
   * The pattern used to filter the matches.
   */
  private final SearchPattern pattern;

  /**
   * Initialize a newly created search listener to pass on any matches that match the given pattern
   * to the given listener.
   * 
   * @param pattern the pattern used to filter the matches
   * @param listener the search listener being wrapped
   */
  public NameMatchingSearchListener(SearchPattern pattern, SearchListener listener) {
    super(listener);
    this.pattern = pattern;
  }

  @Override
  public void matchFound(SearchMatch match) {
    if (pattern.matches(match.getElement()) != null) {
      propagateMatch(match);
    }
  }
}
