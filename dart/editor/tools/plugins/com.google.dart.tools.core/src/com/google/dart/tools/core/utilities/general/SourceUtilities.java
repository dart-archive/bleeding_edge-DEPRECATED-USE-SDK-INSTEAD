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

import com.google.dart.compiler.parser.Token;

import java.util.Arrays;
import java.util.List;

/**
 * The class <code>SourceUtilities</code> defines utility methods for processing strings.
 */
public final class SourceUtilities {
  /**
   * The line separator used on the current platform.
   */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  /**
   * The token representing a #library directive.
   */
  public static String LIBRARY_DIRECTIVE = Token.LIBRARY.getSyntax();

  /**
   * The token representing an #import directive.
   */
  public static String IMPORT_DIRECTIVE = Token.IMPORT.getSyntax();

  /**
   * The token representing a #source directive.
   */
  public static String SOURCE_DIRECTIVE = Token.SOURCE.getSyntax();

  /**
   * The token representing a #resource directive.
   */
  public static String RESOURCE_DIRECTIVE = Token.RESOURCE.getSyntax();

  /**
   * The token representing a #native directive.
   */
  public static String NATIVE_DIRECTIVE = Token.NATIVE.getSyntax();

  /**
   * A list containing all of the directives in the order in which they must appear in a valid
   * source file.
   */
  private static List<String> DIRECTIVES = Arrays.asList(
      LIBRARY_DIRECTIVE,
      IMPORT_DIRECTIVE,
      SOURCE_DIRECTIVE,
      RESOURCE_DIRECTIVE,
      NATIVE_DIRECTIVE);

  /**
   * Return the offset into the given compilation unit contents at which a directive with the given
   * name and a literal value equal to the given relative path should be added. The offset will
   * always be the index of the first character after an end-of-line marker.
   * 
   * @param contents the contents of the compilation unit to which the directive will be added
   * @param directiveName the name of the directive (including the leading pound sign)
   * @param relativePath the path of the file referenced in the literal string in the directive
   * @return the index at which the text for the new directive should be inserted
   */
  public static int findInsertionPointForSource(String contents, String directiveName,
      String relativePath) {
    // TODO(brianwilkerson) Improve the algorithm used to find the location for the directive. If
    // the directives in the unit are in the right order and at the beginning of the file, this
    // should work fine. If not, this can produce additional compilation errors.
    int index = DIRECTIVES.indexOf(directiveName);
    if (index < 0) {
      throw new IllegalArgumentException("Unrecognized directive name: " + directiveName);
    }
    int directiveCount = DIRECTIVES.size();
    for (int i = index; i >= 0; i--) {
      int offset = contents.lastIndexOf(DIRECTIVES.get(i));
      if (offset >= 0) {
        return SourceUtilities.findStartOfNextLine(contents, offset);
      }
    }
    for (int i = index + 1; i < directiveCount; i++) {
      int offset = contents.indexOf(DIRECTIVES.get(i));
      if (offset >= 0) {
        return SourceUtilities.findStartOfLine(contents, offset);
      }
    }
    return 0;
  }

  /**
   * Return the index of the first character in the given string after the first end-of-line marker
   * before the given start index. If there is no end-of-line marker before the given index, return
   * zero (0).
   * 
   * @param string the string being searched
   * @param startIndex the index at which the search is to begin
   * @return the index of the first character in the given string after the first end-of-line marker
   *         before the given start index
   */
  public static int findStartOfLine(String string, int startIndex) {
    int index = startIndex;
    while (index >= 0) {
      if (string.charAt(index) == '\r' || string.charAt(index) == '\n') {
        return index + 1;
      }
      index = index + 1;
    }
    return 0;
  }

  /**
   * Return the index of the first character in the given string after the first end-of-line marker
   * at or after the given index. If there is no end-of-line marker at or after the given index, or
   * if there are no characters after the next end-of-line marker, return the length of the string.
   * 
   * @param string the string being searched
   * @param startIndex the index at which the search is to begin
   * @return the index of the first character in the given string after the first end-of-line marker
   *         at or after the given index
   */
  public static int findStartOfNextLine(String string, int startIndex) {
    int length = string.length();
    int index = startIndex;
    while (index < length) {
      if (string.charAt(index) == '\r') {
        if (index + 1 < length && string.charAt(index + 1) == '\n') {
          return index + 2;
        }
        return index + 1;
      } else if (string.charAt(index) == '\n') {
        return index + 1;
      }
      index = index + 1;
    }
    return length;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private SourceUtilities() {
    super();
  }
}
