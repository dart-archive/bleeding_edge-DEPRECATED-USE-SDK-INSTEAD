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

import com.google.dart.tools.core.search.MatchQuality;
import com.google.dart.tools.core.search.SearchFilter;
import com.google.dart.tools.core.search.SearchMatch;

import java.util.EnumSet;

/**
 * Instances of the class <code>MatchQualityFilter</code> implement a search filter that will pass
 * any match whose quality is in a given set of qualities.
 */
public class MatchQualityFilter implements SearchFilter {
  /**
   * The qualities of matches that will be passed by this filter.
   */
  private EnumSet<MatchQuality> passedQualities;

  /**
   * Initialize a newly created search filter to pass any match whose quality is in the given set of
   * qualities.
   * 
   * @param passedQualities the qualities of matches that will be passed by this filter
   */
  public MatchQualityFilter(EnumSet<MatchQuality> passedQualities) {
    this.passedQualities = passedQualities;
  }

  /**
   * Initialize a newly created search filter to pass any match whose quality is the same as the
   * given quality.
   * 
   * @param quality the quality of match that will be passed by this filter
   */
  public MatchQualityFilter(MatchQuality quality) {
    passedQualities = EnumSet.of(quality);
  }

  @Override
  public boolean passes(SearchMatch match) {
    return passedQualities.contains(match.getQuality());
  }
}
