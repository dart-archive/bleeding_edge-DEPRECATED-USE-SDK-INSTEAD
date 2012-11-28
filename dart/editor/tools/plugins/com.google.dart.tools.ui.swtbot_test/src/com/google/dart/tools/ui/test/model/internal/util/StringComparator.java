/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.test.model.internal.util;

/**
 * The String comparison algorithm used in widget text matching. <br>
 * <br>
 * Hearkening back to WindowTester we use a very simple String-matching strategy that takes
 * advantage of Java's regular expression support. String matching can be summarized as follows:
 * 
 * <pre>
 *    1) First test for simple String equality:
 *         s1.equals(s2)
 *    2) Then test for a pattern match (as defined by java.util.regex.Pattern):
 *         s1.matches(s2)
 *
 *    If either is true, we have a match:
 *         s1.equals(s2) || s1.matches(s2);
 * </pre>
 * 
 * A pattern-match is supplemented with a String equality test to guard against (legacy) cases where
 * special wildcard characters are meant to be taken literally in the pattern match. For instance,
 * the String "(A|B)" has very different interpretations if taken literally versus as a pattern. <br>
 * <br>
 * In practice, the use of Strings for matching that match literally (using equals) and not as
 * regular expressions (using match) is uncommon. As a rule it is best to think of all Strings as
 * patterns and so to use proper patterns (escaping special characters) to match in all cases where
 * the String contains characters with special meaning in regular expressions. For instance, in the
 * case where you want to match the String "(A|B)" you are encouraged to prefer the pattern:
 * "\\(A\\|B\\)". <br>
 * <br>
 * Examples:
 * 
 * <pre>
 *    "(Run|Debug)"       matches "(Run|Debug)" (String equality)
 *    "(Run|Debug)"       matches "Run"         (Pattern match)
 *    "(Run|Debug)"       matches "Debug"       (Pattern match)
 *    "\\(Run\\|Debug\\)" matches "(Run|Debug)" (Pattern match)
 *    "Foo(.java)?"       matches "Foo.java"    (Pattern match)
 *    "Foo(.java)?"       matches "Foo"         (Pattern match)
 * </pre>
 * 
 * More complex cases:
 * 
 * <pre>    	 
 *     "(\\d\\s)?Foo[.java]?" matches "13 Foo.java"
 *     "(\\d\\s)?Foo[.java]?" matches "13 Foo"
 *     "(\\d\\s)?Foo[.java]?" matches "Foo"
 * </pre>
 * 
 * For more on regular expressions and pattern-matching in general, see
 * {@link java.util.regex.Pattern}.
 */
public class StringComparator {

  /**
   * Test this string for a match against the given string or pattern.
   * <p>
   * See the general {@link StringComparator} notes for a description of the pattern matching
   * strategy.
   * <p>
   * Note that if either is argument is <code>null</code> the match will return <code>false</code>.
   * 
   * @param string the string in question
   * @param stringOrPattern a string or pattern against which to match
   * @return <code>true</code> if the String matches, <code>false</code> otherwise
   */
  public static boolean matches(String string, String stringOrPattern) {

    //handle null cases:
    if (string == null || stringOrPattern == null) {
      return false;
    }

    //check for equality first THEN for a regexp match
    try {
      return string.equals(stringOrPattern) || string.matches(stringOrPattern);
    } catch (java.util.regex.PatternSyntaxException e) {
      // do nothing
      // System.out.println("String is not a legal regular expression");
      // e.printStackTrace();
    }
    return false;

  }

}
