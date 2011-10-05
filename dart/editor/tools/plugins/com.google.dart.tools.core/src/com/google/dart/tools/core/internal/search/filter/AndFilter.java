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
package com.google.dart.tools.core.internal.search.filter;

import com.google.dart.tools.core.search.SearchFilter;
import com.google.dart.tools.core.search.SearchMatch;

/**
 * Instances of the class <code>AndFilter</code> implement a search filter that will pass any match
 * that passes all of a list of filters.
 */
public class AndFilter implements SearchFilter {
  /**
   * The filters that must all pass the match in order for this filter to pass the match.
   */
  private SearchFilter[] filters;

  /**
   * Initialize a newly created search filter to pass any match that passes all of the given
   * filters.
   * 
   * @param filters the filters that must all pass the match in order for this filter to pass the
   *          match
   */
  public AndFilter(SearchFilter... filters) {
    assert filters.length > 1;
    this.filters = filters;
  }

  @Override
  public boolean passes(SearchMatch match) {
    for (SearchFilter filter : filters) {
      if (!filter.passes(match)) {
        return false;
      }
    }
    return true;
  }
}
