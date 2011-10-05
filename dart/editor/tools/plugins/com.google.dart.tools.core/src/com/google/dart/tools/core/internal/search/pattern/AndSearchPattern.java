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
package com.google.dart.tools.core.internal.search.pattern;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.search.MatchQuality;
import com.google.dart.tools.core.search.SearchPattern;

/**
 * Instances of the class <code>AndSearchPattern</code> implement a search pattern that matches
 * elements that match all of several other search patterns.
 */
public class AndSearchPattern implements SearchPattern {
  /**
   * The patterns used to determine whether this pattern matches an element.
   */
  private SearchPattern[] patterns;

  /**
   * Initialize a newly created search pattern to match elements that match all of several other
   * search patterns.
   * 
   * @param patterns the patterns used to determine whether this pattern matches an element
   */
  public AndSearchPattern(SearchPattern[] patterns) {
    this.patterns = patterns;
  }

  @Override
  public MatchQuality matches(DartElement element) {
    MatchQuality highestQuality = null;
    for (SearchPattern pattern : patterns) {
      MatchQuality quality = pattern.matches(element);
      if (quality == null) {
        return null;
      }
      if (highestQuality == null) {
        highestQuality = quality;
      } else {
        highestQuality = highestQuality.max(quality);
      }
    }
    return highestQuality;
  }
}
