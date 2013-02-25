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
package com.google.dart.indexer.tests.internal;

/**
 * The class <code>StringUtilities</code> implements general String utilities.
 */
public class StringUtilities {

  /**
   * Return <code>true</code> if the given strings contain the same characters when whitespace
   * characters are ignored.
   * 
   * @param first the first string being compared
   * @param second the second string being compared
   * @return <code>true</code> if the given strings contain the same characters when whitespace
   *         characters are ignored
   */
  public static boolean equalIgnoringWhitespace(String first, String second) {
    int firstLength, firstIndex;
    int secondLength, secondIndex;
    char firstChar, secondChar, previousChar, literalChar;
    boolean inLiteral;

    firstLength = first.length();
    secondLength = second.length();
    firstIndex = 0;
    secondIndex = 0;
    previousChar = ' ';
    literalChar = ' ';
    inLiteral = false;
    while (firstIndex < firstLength && secondIndex < secondLength) {
      firstIndex = firstNonWhitespaceOrComment(first, firstIndex, inLiteral);
      secondIndex = firstNonWhitespaceOrComment(second, secondIndex, inLiteral);
      if (firstIndex >= firstLength) {
        return secondIndex >= secondLength;
      } else if (secondIndex >= secondLength) {
        return false;
      }
      firstChar = first.charAt(firstIndex);
      secondChar = second.charAt(secondIndex);
      if (firstChar != secondChar) {
        return false;
      } else if (inLiteral) {
        if (firstChar == literalChar && previousChar != '\\') {
          inLiteral = false;
        }
      } else if (firstChar == '\'' || firstChar == '"') {
        literalChar = firstChar;
        inLiteral = true;
      }
      if (firstChar == '\\' && previousChar == '\\') {
        previousChar = ' ';
      } else {
        previousChar = firstChar;
      }
      firstIndex++;
      secondIndex++;
    }
    return true;
  }

  /**
   * Return the index of the first character in the given string that is at or after the given index
   * and that is neither whitespace nor part of a comment.
   * 
   * @param string the string being searched
   * @param startIndex the index at which the search is to begin
   * @param inLiteral <code>true</code> if we are currently within a string literal
   * @return the index of the first non-whitespace, non-comment character at or after the given
   *         index
   */
  public static int firstNonWhitespaceOrComment(String string, int startIndex, boolean inLiteral) {
    int length, index;

    length = string.length();
    index = startIndex;
    while (index < length) {
      if (!inLiteral && string.startsWith("//", index)) {
        index = index + 2;
        while (index < length && string.charAt(index) != '\r' && string.charAt(index) != '\n') {
          index = index + 1;
        }
      } else if (!inLiteral && string.startsWith("/*", index)) {
        index = string.indexOf("*/", index + 2);
        if (index < 0) {
          index = length;
        } else {
          index = index + 2;
        }
      } else if (!inLiteral && Character.isWhitespace(string.charAt(index))) {
        index = index + 1;
      } else {
        return index;
      }
    }
    return index;
  }
}
