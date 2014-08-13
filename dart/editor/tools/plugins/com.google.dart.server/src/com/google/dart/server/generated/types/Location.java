/*
 * Copyright (c) 2014, the Dart project authors.
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
 *
 * This file has been automatically generated.  Please do not edit it manually.
 * To regenerate the file, use the script "pkg/analysis_server/spec/generate_files".
 */
package com.google.dart.server.generated.types;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.google.dart.server.utilities.general.ObjectUtilities;
import org.apache.commons.lang3.StringUtils;

/**
 * A location (character range) within a file.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class Location {

  /**
   * The file containing the range.
   */
  private final String file;

  /**
   * The length of the range.
   */
  private final int length;

  /**
   * The offset of the range.
   */
  private final int offset;

  /**
   * The one-based index of the column containing the first character of the range.
   */
  private final int startColumn;

  /**
   * The one-based index of the line containing the first character of the range.
   */
  private final int startLine;

  /**
   * Constructor for {@link Location}.
   */
  public Location(String file, int offset, int length, int startLine, int startColumn) {
    this.file = file;
    this.offset = offset;
    this.length = length;
    this.startLine = startLine;
    this.startColumn = startColumn;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Location) {
      Location other = (Location) obj;
      return
        ObjectUtilities.equals(other.file, file) &&
        other.offset == offset &&
        other.length == length &&
        other.startLine == startLine &&
        other.startColumn == startColumn;
    }
    return false;
  }

  /**
   * The file containing the range.
   */
  public String getFile() {
    return file;
  }

  /**
   * The length of the range.
   */
  public int getLength() {
    return length;
  }

  /**
   * The offset of the range.
   */
  public int getOffset() {
    return offset;
  }

  /**
   * The one-based index of the column containing the first character of the range.
   */
  public int getStartColumn() {
    return startColumn;
  }

  /**
   * The one-based index of the line containing the first character of the range.
   */
  public int getStartLine() {
    return startLine;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("file=");
    builder.append(file.toString() + ", ");
    builder.append("offset=");
    builder.append(offset + ", ");
    builder.append("length=");
    builder.append(length + ", ");
    builder.append("startLine=");
    builder.append(startLine + ", ");
    builder.append("startColumn=");
    builder.append(startColumn + ", ");
    builder.append("]");
    return builder.toString();
  }

}
