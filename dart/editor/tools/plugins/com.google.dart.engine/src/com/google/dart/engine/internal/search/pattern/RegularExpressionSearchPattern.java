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

import java.util.regex.Pattern;

/**
 * Instances of the class <code>RegularExpressionSearchPattern</code> implement a search pattern
 * that matches elements whose name matches a given regular expression.
 * 
 * @coverage dart.engine.search
 */
public class RegularExpressionSearchPattern implements SearchPattern {
  /**
   * The regular expression pattern that matching elements must match.
   */
  private Pattern pattern;

  /**
   * Initialize a newly created search pattern to match elements whose names begin with the given
   * prefix.
   * 
   * @param regularExpression the regular expression that matching elements must match
   * @param caseSensitive {@code true} if a case sensitive match is to be performed
   */
  public RegularExpressionSearchPattern(String regularExpression, boolean caseSensitive) {
    pattern = Pattern.compile(regularExpression, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
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
    if (pattern.matcher(name).matches()) {
      return MatchQuality.EXACT;
    }
    return null;
  }
}
