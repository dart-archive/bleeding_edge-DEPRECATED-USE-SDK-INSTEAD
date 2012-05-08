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
package com.google.dart.tools.search.ui.text;

/**
 * A match filter is used to evaluate the filter state of a match ({@link Match#isFiltered()}.
 * Filters are managed by the ({@link AbstractTextSearchResult}.
 */
public abstract class MatchFilter {

  /**
   * Returns whether the given match is filtered by this filter.
   * 
   * @param match the match to look at
   * @return returns <code>true</code> if the given match should be filtered or <code>false</code>
   *         if not.
   */
  public abstract boolean filters(Match match);

  /**
   * Returns the name of the filter as shown in the match filter selection dialog.
   * 
   * @return the name of the filter as shown in the match filter selection dialog.
   */
  public abstract String getName();

  /**
   * Returns the description of the filter as shown in the match filter selection dialog.
   * 
   * @return the description of the filter as shown in the match filter selection dialog.
   */
  public abstract String getDescription();

  /**
   * Returns the label of the filter as shown by the filter action.
   * 
   * @return the label of the filter as shown by the filter action.
   */
  public abstract String getActionLabel();

  /**
   * Returns an ID of this filter.
   * 
   * @return the id of the filter to be used when persisting this filter.
   */
  public abstract String getID();

}
