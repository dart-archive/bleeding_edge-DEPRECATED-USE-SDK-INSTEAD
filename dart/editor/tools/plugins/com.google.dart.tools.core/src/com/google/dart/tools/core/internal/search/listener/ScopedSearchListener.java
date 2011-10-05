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
import com.google.dart.tools.core.search.SearchScope;

/**
 * Instances of the class <code>ScopedSearchListener</code> implement a search listener that
 * delegates to another search listener after removing matches that are outside a given scope.
 */
public class ScopedSearchListener extends WrappedSearchListener {
  /**
   * The scope used to filter the matches.
   */
  private SearchScope scope;

  /**
   * Initialize a newly created search listener to pass on any matches that are contained in the
   * given scope to the given listener.
   * 
   * @param scope the scope used to filter the matches
   * @param listener the search listener being wrapped
   */
  public ScopedSearchListener(SearchScope scope, SearchListener listener) {
    super(listener);
    this.scope = scope;
  }

  @Override
  public void matchFound(SearchMatch match) {
    if (scope.encloses(match.getElement())) {
      propagateMatch(match);
    }
  }
}
