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

package com.google.dart.tools.debug.core.sourcemaps;

import java.util.Comparator;

/**
 * An entry that maps from a line and column range to a SourceMapInfo (an original source location).
 */
class SourceMapInfoEntry {

  private static Comparator<SourceMapInfoEntry> COMPARATOR = new Comparator<SourceMapInfoEntry>() {
    @Override
    public int compare(SourceMapInfoEntry val1, SourceMapInfoEntry val2) {
      return val1.line - val2.line;
    }
  };

  public static SourceMapInfoEntry forLine(int line) {
    return new SourceMapInfoEntry(line, 0, null);
  }

  public static Comparator<SourceMapInfoEntry> lineComparator() {
    return COMPARATOR;
  }

  /**
   * The source line.
   */
  public int line;

  /**
   * The starting source column (inclusive).
   */
  public int column;

  /**
   * The ending source column (non-inclusive).
   */
  public int endColumn = -1;

  /**
   * The associated SourceMapInfo for this range of generated source.
   */
  public SourceMapInfo info;

  SourceMapInfoEntry(int line, int column, SourceMapInfo info) {
    this.line = line;
    this.column = column;
    this.info = info;
  }

  public SourceMapInfo getInfo() {
    return info;
  }

  @Override
  public String toString() {
    return "[" + line + ":" + column + "," + endColumn + "] ==> " + info.toString();
  }

  void setEndColumn(int endColumn) {
    this.endColumn = endColumn;
  }

}
