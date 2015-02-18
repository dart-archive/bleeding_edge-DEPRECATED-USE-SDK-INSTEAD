/*
 * Copyright (c) 2015, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.internal.util.StringMatcher;

import org.eclipse.ui.dialogs.SearchPattern;

/**
 * Matches using {@link SearchPattern} or {@link StringMatcher}.
 */
public class DartElementPrefixPatternMatcher {
  static final String STAR = "*";

  private char firstPatternChar;
  private SearchPattern camelPattern;
  private SearchPattern camelPatternUpper;
  private StringMatcher wildPattern;

  public DartElementPrefixPatternMatcher(String pattern) {
    if (!pattern.isEmpty() && !pattern.startsWith(STAR)) {
      camelPattern = new SearchPattern(SearchPattern.RULE_CAMELCASE_MATCH);
      camelPattern.setPattern(pattern);
      firstPatternChar = pattern.charAt(0);
      // RULE_CAMELCASE_MATCH works well only when the first character is upper case.
      // But it is nice to camel-case also method names.
      if (Character.isLowerCase(firstPatternChar)) {
        camelPatternUpper = new SearchPattern(SearchPattern.RULE_CAMELCASE_MATCH);
        String upperPattern = Character.toUpperCase(firstPatternChar) + pattern.substring(1);
        camelPatternUpper.setPattern(upperPattern);
      }
    }
    boolean ignoreCase = pattern.toLowerCase().equals(pattern);
    wildPattern = new StringMatcher(pattern + STAR, ignoreCase, false);
  }

  public boolean match(String text) {
    if (text.isEmpty()) {
      return false;
    }
    char firstChar = text.charAt(0);
    if (firstChar == firstPatternChar) {
      if (camelPattern != null) {
        if (camelPattern.matches(text)) {
          return true;
        }
      }
      if (camelPatternUpper != null && Character.isLowerCase(firstChar)) {
        String upperText = Character.toUpperCase(firstChar) + text.substring(1);
        if (camelPatternUpper.matches(upperText)) {
          return true;
        }
      }
    }
    return wildPattern.match(text);
  }
}
