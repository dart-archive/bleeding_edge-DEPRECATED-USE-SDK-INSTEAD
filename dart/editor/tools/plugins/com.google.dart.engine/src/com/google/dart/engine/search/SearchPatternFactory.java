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
package com.google.dart.engine.search;

import com.google.dart.engine.internal.search.pattern.AndSearchPattern;
import com.google.dart.engine.internal.search.pattern.CamelCaseSearchPattern;
import com.google.dart.engine.internal.search.pattern.ExactSearchPattern;
import com.google.dart.engine.internal.search.pattern.OrSearchPattern;
import com.google.dart.engine.internal.search.pattern.PrefixSearchPattern;
import com.google.dart.engine.internal.search.pattern.RegularExpressionSearchPattern;
import com.google.dart.engine.internal.search.pattern.WildcardSearchPattern;

/**
 * The class <code>SearchPatternFactory</code> defines utility methods that can be used to create
 * search patterns.
 * 
 * @coverage dart.engine.search
 */
public final class SearchPatternFactory {
  /**
   * Create a pattern that will match any element that is matched by all of the given patterns. If
   * no patterns are given, then the resulting pattern will not match any elements.
   * 
   * @param patterns the patterns that must all be matched in order for the new pattern to be
   *          matched
   * @return the pattern that was created
   */
  public static SearchPattern createAndPattern(SearchPattern... patterns) {
    if (patterns.length == 1) {
      return patterns[0];
    }
    return new AndSearchPattern(patterns);
  }

  /**
   * Create a pattern that will match any element whose name matches a partial identifier where
   * camel case conventions are used to perform what is essentially multiple prefix matches.
   * 
   * @param pattern the pattern that matching elements must match
   * @param samePartCount {@code true} if the pattern and the name being matched must have
   *          exactly the same number of parts (i.e. the same number of uppercase characters)
   * @return the pattern that was created
   */
  public static SearchPattern createCamelCasePattern(String prefix, boolean samePartCount) {
    return new CamelCaseSearchPattern(prefix, samePartCount);
  }

  /**
   * Create a pattern that will match any element whose name matches a specified identifier exactly.
   * 
   * @param identifier the identifier that matching elements must be equal to
   * @param caseSensitive {@code true} if a case sensitive match is to be performed
   * @return the pattern that was created
   */
  public static SearchPattern createExactPattern(String identifier, boolean caseSensitive) {
    return new ExactSearchPattern(identifier, caseSensitive);
  }

  /**
   * Create a pattern that will match any element that is matched by at least one of the given
   * patterns. If no patterns are given, then the resulting pattern will not match any elements.
   * 
   * @param patterns the patterns used to determine whether the new pattern is matched
   * @return the pattern that was created
   */
  public static SearchPattern createOrPattern(SearchPattern... patterns) {
    if (patterns.length == 1) {
      return patterns[0];
    }
    return new OrSearchPattern(patterns);
  }

  /**
   * Create a pattern that will match any element whose name starts with the given prefix.
   * 
   * @param prefix the prefix of names that match the pattern
   * @param caseSensitive {@code true} if a case sensitive match is to be performed
   * @return the pattern that was created
   */
  public static SearchPattern createPrefixPattern(String prefix, boolean caseSensitive) {
    return new PrefixSearchPattern(prefix, caseSensitive);
  }

  /**
   * Create a pattern that will match any element whose name matches a regular expression.
   * 
   * @param regularExpression the regular expression that matching elements must match
   * @param caseSensitive {@code true} if a case sensitive match is to be performed
   * @return the pattern that was created
   */
  public static SearchPattern createRegularExpressionPattern(String regularExpression,
      boolean caseSensitive) {
    return new RegularExpressionSearchPattern(regularExpression, caseSensitive);
  }

  /**
   * Create a pattern that will match any element whose name matches a pattern containing wildcard
   * characters. The wildcard characters that are currently supported are '?' (to match any single
   * character) and '*' (to match zero or more characters).
   * 
   * @param pattern the pattern that matching elements must match
   * @param caseSensitive {@code true} if a case sensitive match is to be performed
   * @return the pattern that was created
   */
  public static SearchPattern createWildcardPattern(String pattern, boolean caseSensitive) {
    return new WildcardSearchPattern(pattern, caseSensitive);
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private SearchPatternFactory() {
    super();
  }
}
