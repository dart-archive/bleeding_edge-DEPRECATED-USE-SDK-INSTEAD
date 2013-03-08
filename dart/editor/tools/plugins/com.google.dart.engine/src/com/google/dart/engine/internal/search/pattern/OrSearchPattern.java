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
package com.google.dart.engine.internal.search.pattern;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.MatchQuality;
import com.google.dart.engine.search.SearchPattern;

/**
 * Instances of the class <code>OrSearchPattern</code> implement a search pattern that matches
 * elements that match any one of several other search patterns.
 * 
 * @coverage dart.engine.search
 */
public class OrSearchPattern implements SearchPattern {
  /**
   * The patterns used to determine whether this pattern matches an element.
   */
  private SearchPattern[] patterns;

  /**
   * Initialize a newly created search pattern to match elements that match any one of several other
   * search patterns.
   * 
   * @param patterns the patterns used to determine whether this pattern matches an element
   */
  public OrSearchPattern(SearchPattern... patterns) {
    this.patterns = patterns;
  }

  @Override
  public MatchQuality matches(Element element) {
    // Do we want to return the highest quality of match rather than stopping
    // after the first match? Doing so would be more accurate, but slower.
    for (SearchPattern pattern : patterns) {
      MatchQuality quality = pattern.matches(element);
      if (quality != null) {
        return quality;
      }
    }
    return null;
  }
}
