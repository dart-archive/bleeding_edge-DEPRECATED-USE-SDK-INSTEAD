/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 */
public class PatternConstructor {

  /**
   * Creates a pattern element from the pattern string which is either a reg-ex expression or in our
   * old 'StringMatcher' format.
   * 
   * @param pattern The search pattern
   * @param isCaseSensitive Set to <code>true</code> to create a case insensitive pattern
   * @param isRegexSearch <code>true</code> if the passed string already is a reg-ex pattern
   * @return The created pattern
   * @throws PatternSyntaxException
   */
  public static Pattern createPattern(String pattern, boolean isCaseSensitive, boolean isRegexSearch)
      throws PatternSyntaxException {
    if (!isRegexSearch) {
      pattern = asRegEx(pattern, new StringBuffer()).toString();
    }

    if (!isCaseSensitive) {
      return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
          | Pattern.MULTILINE);
    }

    return Pattern.compile(pattern, Pattern.MULTILINE);
  }

  /**
   * Creates a pattern element from the pattern string which is either a reg-ex expression or in our
   * old 'StringMatcher' format.
   * 
   * @param patterns The search patterns
   * @param isCaseSensitive Set to <code>true</code> to create a case insensitive pattern
   * @param isRegexSearch <code>true</code> if the passed string already is a reg-ex pattern
   * @return The created pattern
   * @throws PatternSyntaxException
   */
  public static Pattern createPattern(String[] patterns, boolean isCaseSensitive,
      boolean isRegexSearch) throws PatternSyntaxException {
    StringBuffer pattern = new StringBuffer();
    for (int i = 0; i < patterns.length; i++) {
      if (i > 0) {
        pattern.append('|');
      }
      if (isRegexSearch) {
        pattern.append(patterns[i]);
      } else {
        asRegEx(patterns[i], pattern);
      }
    }
    return createPattern(pattern.toString(), isCaseSensitive, true);
  }

  /**
   * Translates a StringMatcher pattern (using '*' and '?') to a regex pattern string
   * 
   * @param stringMatcherPattern a pattern using '*' and '?'
   */
  private static StringBuffer asRegEx(String stringMatcherPattern, StringBuffer out) {
    boolean escaped = false;
    boolean quoting = false;

    int i = 0;
    while (i < stringMatcherPattern.length()) {
      char ch = stringMatcherPattern.charAt(i++);

      if (ch == '*' && !escaped) {
        if (quoting) {
          out.append("\\E"); //$NON-NLS-1$
          quoting = false;
        }
        out.append(".*"); //$NON-NLS-1$
        escaped = false;
        continue;
      } else if (ch == '?' && !escaped) {
        if (quoting) {
          out.append("\\E"); //$NON-NLS-1$
          quoting = false;
        }
        out.append("."); //$NON-NLS-1$
        escaped = false;
        continue;
      } else if (ch == '\\' && !escaped) {
        escaped = true;
        continue;

      } else if (ch == '\\' && escaped) {
        escaped = false;
        if (quoting) {
          out.append("\\E"); //$NON-NLS-1$
          quoting = false;
        }
        out.append("\\\\"); //$NON-NLS-1$
        continue;
      }

      if (!quoting) {
        out.append("\\Q"); //$NON-NLS-1$
        quoting = true;
      }
      if (escaped && ch != '*' && ch != '?' && ch != '\\') {
        out.append('\\');
      }
      out.append(ch);
      escaped = ch == '\\';

    }
    if (quoting) {
      out.append("\\E"); //$NON-NLS-1$
    }

    return out;
  }

  private PatternConstructor() {
    // don't instantiate
  }

}
