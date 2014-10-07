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
package com.google.dart.tools.ui.omni.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for camel case style matching.
 */
public class CamelUtil {

  /**
   * Returns a lowercase string consisting of all initials of the words in the given String. Words
   * are separated by whitespace and other special characters, or by uppercase letters in a word
   * like CamelCase.
   * 
   * @param s the string
   * @return a lowercase string containing the first character of every word in the given string.
   */
  public static String getCamelCase(String s) {
    StringBuffer result = new StringBuffer();
    if (s.length() > 0) {
      int index = 0;
      while (index != -1) {
        result.append(s.charAt(index));
        index = getNextCamelIndex(s, index + 1);
      }
    }
    return result.toString().toLowerCase();
  }

  /**
   * Return an array with start/end indices for the characters used for camel case matching,
   * ignoring the first (start) many camel case characters. For example,
   * getCamelCaseIndices("some CamelCase", 1, 2) will return {{5,5},{10,10}}.
   * 
   * @param s the source string
   * @param start how many characters of getCamelCase(s) should be ignored
   * @param length for how many characters should indices be returned
   * @return an array of length start
   */
  public static int[][] getCamelCaseIndices(String s, int start, int length) {
    List<int[]> result = new ArrayList<int[]>();
    int index = 0;
    while (start > 0) {
      index = getNextCamelIndex(s, index + 1);
      start--;
    }
    while (length > 0) {
      result.add(new int[] {index, index});
      index = getNextCamelIndex(s, index + 1);
      length--;
    }
    return result.toArray(new int[result.size()][]);
  }

  /**
   * Return a regular expression for the given camel-case string.
   * 
   * <pre>
   *   AnaSer   => Ana[a-z]*?Ser[a-z]*?
   *   AngCEI   => Ang[a-z]*?C[a-z]*?E[a-z]*?I[a-z]*?
   *   arP      => ar[a-z]*?P[a-z]*?
   * </pre>
   */
  public static String getCamelCaseRegExp(String s) {
    StringBuilder buf = new StringBuilder();
    int index = 0;
    while (index < s.length()) {
      char c = s.charAt(index++);
      buf.append("(");
      buf.append(c);
      while (index < s.length()) {
        char next = s.charAt(index);
        if (Character.isDigit(next) || Character.isLowerCase(next)) {
          buf.append(next);
          index++;
        } else {
          break;
        }
      }
      buf.append(")");
      buf.append("[a-z]*?");
    }
    return buf.toString();
  }

  /**
   * Returns the next index to be used for camel case matching.
   * 
   * @param s the string
   * @param index the index
   * @return the next index, or -1 if not found
   */
  public static int getNextCamelIndex(String s, int index) {
    char c;
    while (index < s.length() && !(isSeparatorForCamelCase(c = s.charAt(index)))
        && Character.isLowerCase(c)) {
      index++;
    }
    while (index < s.length() && isSeparatorForCamelCase(c = s.charAt(index))) {
      index++;
    }
    if (index >= s.length()) {
      index = -1;
    }
    return index;
  }

  /**
   * Returns true if the given character is to be considered a separator for camel case matching
   * purposes.
   * 
   * @param c the character
   * @return true if the character is a separator
   */
  public static boolean isSeparatorForCamelCase(char c) {
    return !Character.isLetterOrDigit(c);
  }

}
