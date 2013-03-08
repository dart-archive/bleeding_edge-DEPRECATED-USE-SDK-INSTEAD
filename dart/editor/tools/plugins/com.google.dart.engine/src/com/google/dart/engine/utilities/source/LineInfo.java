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
package com.google.dart.engine.utilities.source;

/**
 * Instances of the class {@code LineInfo} encapsulate information about line and column information
 * within a source file.
 * 
 * @coverage dart.engine.utilities
 */
public class LineInfo {
  /**
   * Instances of the class {@code Location} represent the location of a character as a line and
   * column pair.
   */
  public static class Location {
    /**
     * The one-based index of the line containing the character.
     */
    private int lineNumber;

    /**
     * The one-based index of the column containing the character.
     */
    private int columnNumber;

    /**
     * Initialize a newly created location to represent the location of the character at the given
     * line and column position.
     * 
     * @param lineNumber the one-based index of the line containing the character
     * @param columnNumber the one-based index of the column containing the character
     */
    public Location(int lineNumber, int columnNumber) {
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }

    /**
     * Return the one-based index of the column containing the character.
     * 
     * @return the one-based index of the column containing the character
     */
    public int getColumnNumber() {
      return columnNumber;
    }

    /**
     * Return the one-based index of the line containing the character.
     * 
     * @return the one-based index of the line containing the character
     */
    public int getLineNumber() {
      return lineNumber;
    }
  }

  /**
   * An array containing the offsets of the first character of each line in the source code.
   */
  private int[] lineStarts;

  /**
   * Initialize a newly created set of line information to represent the data encoded in the given
   * array.
   * 
   * @param lineStarts the offsets of the first character of each line in the source code
   */
  public LineInfo(int[] lineStarts) {
    if (lineStarts == null) {
      throw new IllegalArgumentException("lineStarts must be non-null");
    } else if (lineStarts.length < 1) {
      throw new IllegalArgumentException("lineStarts must be non-empty");
    }
    this.lineStarts = lineStarts;
  }

  /**
   * Return the location information for the character at the given offset.
   * 
   * @param offset the offset of the character for which location information is to be returned
   * @return the location information for the character at the given offset
   */
  public Location getLocation(int offset) {
    int lineCount = lineStarts.length;
    for (int i = 1; i < lineCount; i++) {
      if (offset < lineStarts[i]) {
        return new Location(i, offset - lineStarts[i - 1] + 1);
      }
    }
    return new Location(lineCount, offset - lineStarts[lineCount - 1] + 1);
  }
}
