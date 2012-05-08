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

import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.SearchResultEvent;

/**
 * An event object describing that the filter state of the given {@link Match matches} has been
 * updated or {@link MatchFilter match filters} have been reconfigured.
 * <p>
 * Clients may instantiate or subclass this class.
 * </p>
 */
public class FilterUpdateEvent extends SearchResultEvent {

  private static final long serialVersionUID = 6009335074727417443L;

  private final Match[] fMatches;
  private final MatchFilter[] fFilters;

  /**
   * Constructs a new {@link FilterUpdateEvent}.
   * 
   * @param searchResult the search result concerned
   * @param matches the matches updated by the filter change
   * @param filters the currently activated filters
   */
  public FilterUpdateEvent(ISearchResult searchResult, Match[] matches, MatchFilter[] filters) {
    super(searchResult);
    fMatches = matches;
    fFilters = filters;
  }

  /**
   * Returns the matches updated by the filter update.
   * 
   * @return the matches updated by the filter update
   */
  public Match[] getUpdatedMatches() {
    return fMatches;
  }

  /**
   * Returns the the filters currently set, or <code>null</code> if filters have been disabled.
   * 
   * @return the filters currently set
   */
  public MatchFilter[] getActiveFilters() {
    return fFilters;
  }
}
