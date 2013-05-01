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
import com.google.dart.engine.utilities.general.CharOperation;

/**
 * Instances of the class <code>CamelCaseSearchPattern</code> implement a search pattern that
 * matches elements whose name matches a partial identifier where camel case conventions are used to
 * perform what is essentially multiple prefix matches.
 * 
 * @coverage dart.engine.search
 */
public class CamelCaseSearchPattern implements SearchPattern {
  /**
   * The pattern that matching elements must match.
   */
  private final char[] pattern;

  /**
   * A flag indicating whether the pattern and the name being matched must have exactly the same
   * number of parts (i.e. the same number of uppercase characters).
   */
  private final boolean samePartCount;

  /**
   * Initialize a newly created search pattern to match elements whose names match the given
   * camel-case pattern.
   * 
   * @param pattern the pattern that matching elements must match
   * @param samePartCount {@code true} if the pattern and the name being matched must have
   *          exactly the same number of parts (i.e. the same number of uppercase characters)
   */
  public CamelCaseSearchPattern(String pattern, boolean samePartCount) {
    this.pattern = pattern.toCharArray();
    this.samePartCount = samePartCount;
  }

  @Override
  public MatchQuality matches(Element element) {
    String name = element.getDisplayName();
    if (name == null) {
      return null;
    }
    if (CharOperation.camelCaseMatch(pattern, name.toCharArray(), samePartCount)) {
      return MatchQuality.EXACT;
    }
    return null;
  }
}
