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
package com.google.dart.engine.internal.search.pattern;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.MatchQuality;
import com.google.dart.engine.search.SearchPattern;
import com.google.dart.engine.utilities.general.CharOperation;

/**
 * Instances of the class <code>WildcardSearchPattern</code> implement a search pattern that matches
 * elements whose name matches a pattern with wildcard characters. The wildcard characters that are
 * currently supported are '?' (to match any single character) and '*' (to match zero or more
 * characters).
 * 
 * @coverage dart.engine.search
 */
public class WildcardSearchPattern implements SearchPattern {
  /**
   * The pattern that matching elements must match.
   */
  private char[] pattern;

  /**
   * A flag indicating whether a case sensitive match is to be performed.
   */
  private boolean caseSensitive;

  /**
   * Initialize a newly created search pattern to match elements whose names begin with the given
   * prefix.
   * 
   * @param pattern the pattern that matching elements must match
   * @param caseSensitive {@code true} if a case sensitive match is to be performed
   */
  public WildcardSearchPattern(String pattern, boolean caseSensitive) {
    this.pattern = caseSensitive ? pattern.toCharArray() : pattern.toLowerCase().toCharArray();
    this.caseSensitive = caseSensitive;
  }

  @Override
  public MatchQuality matches(Element element) {
    if (element == null) {
      return null;
    }
    String name = element.getDisplayName();
    if (name == null) {
      return null;
    }
    if (CharOperation.match(pattern, name.toCharArray(), caseSensitive)) {
      return MatchQuality.EXACT;
    }
    return null;
  }
}
