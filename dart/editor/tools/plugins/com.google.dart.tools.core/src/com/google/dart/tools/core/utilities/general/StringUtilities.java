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
package com.google.dart.tools.core.utilities.general;

import java.util.ArrayList;
import java.util.List;

/**
 * @coverage dart.tools.core.utilities
 */
public final class StringUtilities {

  /**
   * The empty String <code>""</code>.
   */
  public static final String EMPTY = "";

  /**
   * Test to see if this string contains any upper case characters.
   * 
   * @param str the string to test
   * @return <code>true</code> if this string contains any upper case characters, <code>false</code>
   *         otherwise.
   */
  public static boolean containsUpperCase(String str) {
    if (str != null) {
      for (char c : str.toCharArray()) {
        if (Character.isUpperCase(c)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Test to see if this string contains any whitespace characters.
   * 
   * @param str the string to test
   * @return <code>true</code> if this string contains any whitespace characters, <code>false</code>
   *         otherwise.
   */
  public static boolean containsWhitespace(String str) {
    if (str != null) {
      for (char c : str.toCharArray()) {
        if (Character.isWhitespace(c)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * <p>
   * Counts how many times the substring appears in the larger String.
   * </p>
   * <p>
   * A <code>null</code> or empty ("") String input returns <code>0</code>.
   * </p>
   * 
   * <pre>
   * StringUtils.countMatches(null, *)       = 0
   * StringUtils.countMatches("", *)         = 0
   * StringUtils.countMatches("abba", null)  = 0
   * StringUtils.countMatches("abba", "")    = 0
   * StringUtils.countMatches("abba", "a")   = 2
   * StringUtils.countMatches("abba", "ab")  = 1
   * StringUtils.countMatches("abba", "xxx") = 0
   * </pre>
   * 
   * @param str the String to check, may be null
   * @param sub the substring to count, may be null
   * @return the number of occurrences, 0 if either String is <code>null</code>
   */
  public static int countMatches(String str, String sub) {
    if (isEmpty(str) || isEmpty(sub)) {
      return 0;
    }
    int count = 0;
    int idx = 0;
    while ((idx = str.indexOf(sub, idx)) != -1) {
      count++;
      idx += sub.length();
    }
    return count;
  }

  /**
   * This method is equivalent to {@link String#endsWith(String)} with the exception that
   * {@link String#equalsIgnoreCase(String)} is used, instead of {@link String#equals(Object)}.
   * 
   * @see String#endsWith(String)
   * @param str the String to test
   * @param suffix the suffix
   * @return <code>true</code> if the passes string ends with the passed suffix, ignoring cases
   */
  public static boolean endsWithIgnoreCase(String str, String suffix) {
    // if one of the two inputs is null, return false.
    if (str == null || suffix == null) {
      return false;
    }
    int strLength = str.length();
    int suffixLength = suffix.length();
    // cover the trivial case where the suffix has no length
    if (suffixLength == 0) {
      return true;
    }
    // if the string is shorter than the suffix, return false
    if (strLength < suffixLength) {
      return false;
    }
    String strSuffix = str.substring(strLength - suffixLength);
    return strSuffix.equalsIgnoreCase(suffix);
  }

  public static boolean isAlphanumeric(CharSequence cs) {
    if (cs == null || cs.length() == 0) {
      return false;
    }
    int sz = cs.length();
    for (int i = 0; i < sz; i++) {
      if (Character.isLetterOrDigit(cs.charAt(i)) == false) {
        return false;
      }
    }
    return true;
  }

  /**
   * <p>
   * Checks if a String is whitespace, empty ("") or null.
   * </p>
   * 
   * <pre>
   * StringUtils.isBlank(null)      = true
   * StringUtils.isBlank("")        = true
   * StringUtils.isBlank(" ")       = true
   * StringUtils.isBlank("bob")     = false
   * StringUtils.isBlank("  bob  ") = false
   * </pre>
   * 
   * @param str the String to check, may be null
   * @return <code>true</code> if the String is null, empty or whitespace
   */
  public static boolean isBlank(String str) {
    int strLen;
    if (str == null || (strLen = str.length()) == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if (Character.isWhitespace(str.charAt(i)) == false) {
        return false;
      }
    }
    return true;
  }

  /**
   * <p>
   * Checks if a String is empty ("") or null.
   * </p>
   * 
   * <pre>
   * StringUtils.isEmpty(null)      = true
   * StringUtils.isEmpty("")        = true
   * StringUtils.isEmpty(" ")       = false
   * StringUtils.isEmpty("bob")     = false
   * StringUtils.isEmpty("  bob  ") = false
   * </pre>
   * 
   * @param str the String to check, may be null
   * @return <code>true</code> if the String is empty or null
   */
  public static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

  /**
   * <p>
   * Checks if a String is not empty (""), not null and not whitespace only.
   * </p>
   * 
   * <pre>
   * StringUtils.isNotBlank(null)      = false
   * StringUtils.isNotBlank("")        = false
   * StringUtils.isNotBlank(" ")       = false
   * StringUtils.isNotBlank("bob")     = true
   * StringUtils.isNotBlank("  bob  ") = true
   * </pre>
   * 
   * @param str the String to check, may be null
   * @return <code>true</code> if the String is not empty and not null and not whitespace
   */
  public static boolean isNotBlank(String str) {
    return !isBlank(str);
  }

  /**
   * <p>
   * Checks if a String is not empty ("") and not null.
   * </p>
   * 
   * <pre>
   * StringUtils.isNotEmpty(null)      = false
   * StringUtils.isNotEmpty("")        = false
   * StringUtils.isNotEmpty(" ")       = true
   * StringUtils.isNotEmpty("bob")     = true
   * StringUtils.isNotEmpty("  bob  ") = true
   * </pre>
   * 
   * @param str the String to check, may be null
   * @return <code>true</code> if the String is not empty and not null
   */
  public static boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }

  /**
   * Convert an argument string into a list of arguments. This essentially splits on the space char,
   * with handling for quotes and escaped quotes.
   * <p>
   * <ul>
   * <li>foo bar baz ==> [foo][bar][baz]
   * <li>foo=three bar='one two' ==> [foo=three][bar='one two']
   * 
   * @param command
   * @return
   */
  public static String[] parseArgumentString(String command) {
    List<String> args = new ArrayList<String>();

    StringBuilder builder = new StringBuilder();
    boolean inQuote = false;
    boolean prevWasSlash = false;

    command = command.replaceAll("\r\n", " ").replaceAll("\n", " ").replaceAll("\\s+", " ");

    for (final char c : command.toCharArray()) {
      if (!prevWasSlash && (c == '\'' || c == '"')) {
        inQuote = !inQuote;
        prevWasSlash = false;
      }

      if (c == ' ' && !inQuote) {
        args.add(stripQuotes(builder.toString()));
        builder.setLength(0);
      } else {
        builder.append(c);
      }

      prevWasSlash = c == '\\';
    }

    if (builder.length() > 0) {
      args.add(stripQuotes(builder.toString()));
    }

    return args.toArray(new String[args.size()]);
  }

  public static String stripQuotes(String str) {
    if (str.length() > 1) {
      if ((str.startsWith("'") && str.endsWith("'"))
          || (str.startsWith("\"") && str.endsWith("\""))) {
        str = str.substring(1, str.length() - 1);
      }
    }

    return str;
  }

  /**
   * <p>
   * Gets the substring after the first occurrence of a separator. The separator is not returned.
   * </p>
   * <p>
   * A <code>null</code> string input will return <code>null</code>. An empty ("") string input will
   * return the empty string. A <code>null</code> separator will return the empty string if the
   * input string is not <code>null</code>.
   * </p>
   * <p>
   * If nothing is found, the empty string is returned.
   * </p>
   * 
   * <pre>
   * StringUtils.substringAfter(null, *)      = null
   * StringUtils.substringAfter("", *)        = ""
   * StringUtils.substringAfter(*, null)      = ""
   * StringUtils.substringAfter("abc", "a")   = "bc"
   * StringUtils.substringAfter("abcba", "b") = "cba"
   * StringUtils.substringAfter("abc", "c")   = ""
   * StringUtils.substringAfter("abc", "d")   = ""
   * StringUtils.substringAfter("abc", "")    = "abc"
   * </pre>
   * 
   * @param str the String to get a substring from, may be null
   * @param separator the String to search for, may be null
   * @return the substring after the first occurrence of the separator, <code>null</code> if null
   *         String input
   * @since 2.0
   */
  public static String substringAfter(String str, String separator) {
    if (isEmpty(str)) {
      return str;
    }
    if (separator == null) {
      return EMPTY;
    }
    int pos = str.indexOf(separator);
    if (pos == -1) {
      return EMPTY;
    }
    return str.substring(pos + separator.length());
  }

  /**
   * <p>
   * Gets the substring after the last occurrence of a separator. The separator is not returned.
   * </p>
   * <p>
   * A <code>null</code> string input will return <code>null</code>. An empty ("") string input will
   * return the empty string. An empty or <code>null</code> separator will return the empty string
   * if the input string is not <code>null</code>.
   * </p>
   * <p>
   * If nothing is found, the empty string is returned.
   * </p>
   * 
   * <pre>
   * StringUtils.substringAfterLast(null, *)      = null
   * StringUtils.substringAfterLast("", *)        = ""
   * StringUtils.substringAfterLast(*, "")        = ""
   * StringUtils.substringAfterLast(*, null)      = ""
   * StringUtils.substringAfterLast("abc", "a")   = "bc"
   * StringUtils.substringAfterLast("abcba", "b") = "a"
   * StringUtils.substringAfterLast("abc", "c")   = ""
   * StringUtils.substringAfterLast("a", "a")     = ""
   * StringUtils.substringAfterLast("a", "z")     = ""
   * </pre>
   * 
   * @param str the String to get a substring from, may be null
   * @param separator the String to search for, may be null
   * @return the substring after the last occurrence of the separator, <code>null</code> if null
   *         String input
   */
  public static String substringAfterLast(String str, String separator) {
    if (isEmpty(str)) {
      return str;
    }
    if (isEmpty(separator)) {
      return EMPTY;
    }
    int pos = str.lastIndexOf(separator);
    if (pos == -1 || pos == str.length() - separator.length()) {
      return EMPTY;
    }
    return str.substring(pos + separator.length());
  }

  /**
   * <p>
   * Gets the substring before the first occurrence of a separator. The separator is not returned.
   * </p>
   * <p>
   * A <code>null</code> string input will return <code>null</code>. An empty ("") string input will
   * return the empty string. A <code>null</code> separator will return the input string.
   * </p>
   * <p>
   * If nothing is found, the string input is returned.
   * </p>
   * 
   * <pre>
   * StringUtils.substringBefore(null, *)      = null
   * StringUtils.substringBefore("", *)        = ""
   * StringUtils.substringBefore("abc", "a")   = ""
   * StringUtils.substringBefore("abcba", "b") = "a"
   * StringUtils.substringBefore("abc", "c")   = "ab"
   * StringUtils.substringBefore("abc", "d")   = "abc"
   * StringUtils.substringBefore("abc", "")    = ""
   * StringUtils.substringBefore("abc", null)  = "abc"
   * </pre>
   * 
   * @param str the String to get a substring from, may be null
   * @param separator the String to search for, may be null
   * @return the substring before the first occurrence of the separator, <code>null</code> if null
   *         String input
   * @since 2.0
   */
  public static String substringBefore(String str, String separator) {
    if (isEmpty(str) || separator == null) {
      return str;
    }
    if (separator.length() == 0) {
      return EMPTY;
    }
    int pos = str.indexOf(separator);
    if (pos == -1) {
      return str;
    }
    return str.substring(0, pos);
  }

  /**
   * <p>
   * Gets the substring before the last occurrence of a separator. The separator is not returned.
   * </p>
   * <p>
   * A <code>null</code> string input will return <code>null</code>. An empty ("") string input will
   * return the empty string. An empty or <code>null</code> separator will return the input string.
   * </p>
   * <p>
   * If nothing is found, the string input is returned.
   * </p>
   * 
   * <pre>
   * StringUtils.substringBeforeLast(null, *)      = null
   * StringUtils.substringBeforeLast("", *)        = ""
   * StringUtils.substringBeforeLast("abcba", "b") = "abc"
   * StringUtils.substringBeforeLast("abc", "c")   = "ab"
   * StringUtils.substringBeforeLast("a", "a")     = ""
   * StringUtils.substringBeforeLast("a", "z")     = "a"
   * StringUtils.substringBeforeLast("a", null)    = "a"
   * StringUtils.substringBeforeLast("a", "")      = "a"
   * </pre>
   * 
   * @param str the String to get a substring from, may be null
   * @param separator the String to search for, may be null
   * @return the substring before the last occurrence of the separator, <code>null</code> if null
   *         String input
   */
  public static String substringBeforeLast(String str, String separator) {
    if (isEmpty(str) || isEmpty(separator)) {
      return str;
    }
    int pos = str.lastIndexOf(separator);
    if (pos == -1) {
      return str;
    }
    return str.substring(0, pos);
  }

  private StringUtilities() {

  }

}
