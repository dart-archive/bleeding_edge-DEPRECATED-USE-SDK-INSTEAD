/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.search;

import com.google.dart.tools.core.search.SearchScope;

/**
 * Describes a search query by giving a textual pattern to search for.
 * <p>
 * This class is not intended to be instantiated or subclassed by clients.
 * </p>
 * 
 * @see QuerySpecification
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class PatternQuerySpecification extends QuerySpecification {

  private final boolean caseSensitive;
  private final String pattern;
  private final int searchFor;

  /**
   * @param pattern The string that the query should search for.
   * @param searchFor What kind of <code>DartElement</code> the query should search for.
   * @param caseSensitive Whether the query should be case sensitive.
   * @param limitTo The kind of occurrence the query should search for.
   * @param scope The scope to search in.
   * @param scopeDescription A human readable description of the search scope.
   */
  public PatternQuerySpecification(String pattern, int searchFor, boolean caseSensitive,
      int limitTo, SearchScope scope, String scopeDescription) {
    super(limitTo, scope, scopeDescription);
    this.pattern = pattern;
    this.searchFor = searchFor;
    this.caseSensitive = caseSensitive;
  }

  /**
   * Returns the search pattern the query should search for.
   * 
   * @return the search pattern
   */
  public String getPattern() {
    return pattern;
  }

  /**
   * Returns what kind of <code>DartElement</code> the query should search for.
   * 
   * @return The kind of <code>DartElement</code> to search for.
   */
  public int getSearchFor() {
    return searchFor;
  }

  /**
   * Whether the query should be case sensitive.
   * 
   * @return Whether the query should be case sensitive.
   */
  public boolean isCaseSensitive() {
    return caseSensitive;
  }
}
