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

import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.search.MatchQuality;
import com.google.dart.tools.core.search.SearchPattern;

/**
 * Instances of the class <code>WildcardSearchPattern</code> implement a search pattern that matches
 * elements whose name matches a pattern with wildcard characters. The wildcard characters that are
 * currently supported are '?' (to match any single character) and '*' (to match zero or more
 * characters).
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
   * @param caseSensitive <code>true</code> if a case sensitive match is to be performed
   */
  public WildcardSearchPattern(String pattern, boolean caseSensitive) {
    this.pattern = pattern.toCharArray();
    this.caseSensitive = caseSensitive;
  }

  @Override
  public MatchQuality matches(DartElement element) {
    String name = element.getElementName();
    if (name == null) {
      return null;
    }
    if (CharOperation.match(pattern, name.toCharArray(), caseSensitive)) {
      return MatchQuality.EXACT;
    }
    return null;
  }
}
