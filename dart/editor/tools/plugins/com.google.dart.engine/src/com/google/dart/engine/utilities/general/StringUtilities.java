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
package com.google.dart.engine.utilities.general;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

/**
 * The class {@code StringUtilities} defines utility methods for strings.
 * 
 * @coverage dart.engine.utilities
 */
public final class StringUtilities {
  /**
   * The empty String {@code ""}.
   */
  public static final String EMPTY = "";

  /**
   * An empty array of strings.
   */
  public static final String[] EMPTY_ARRAY = new String[0];

  /**
   * The {@link Interner} instance to use for {@link #intern(String)}.
   */
  private static final Interner<String> INTERNER = Interners.newWeakInterner();

  /**
   * Returns a canonical representation for the given {@link String}.
   * 
   * @return the given {@link String} or its canonical representation.
   */
  public static String intern(String str) {
    if (str == null) {
      return null;
    }
    str = new String(str);
    return INTERNER.intern(str);
  }

  /**
   * Return {@code true} if the given CharSequence is empty ("") or null.
   * 
   * <pre>
   * StringUtils.isEmpty(null)      = true
   * StringUtils.isEmpty("")        = true
   * StringUtils.isEmpty(" ")       = false
   * StringUtils.isEmpty("bob")     = false
   * StringUtils.isEmpty("  bob  ") = false
   * </pre>
   * 
   * @param cs the CharSequence to check, may be null
   * @return {@code true} if the CharSequence is empty or null
   */
  public static boolean isEmpty(CharSequence cs) {
    return cs == null || cs.length() == 0;
  }

  /**
   * Return the substring before the first occurrence of a separator. The separator is not returned.
   * <p>
   * A {@code null} string input will return {@code null}. An empty ("") string input will return
   * the empty string. A {@code null} separator will return the input string.
   * <p>
   * If nothing is found, the string input is returned.
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
   * @param str the string to get a substring from, may be null
   * @param separator the string to search for, may be null
   * @return the substring before the first occurrence of the separator
   */
  public static String substringBefore(String str, String separator) {
    if (isEmpty(str) || separator == null) {
      return str;
    }
    if (separator.length() == 0) {
      return EMPTY;
    }
    int pos = str.indexOf(separator);
    if (pos < 0) {
      return str;
    }
    return str.substring(0, pos);
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private StringUtilities() {
  }
}
