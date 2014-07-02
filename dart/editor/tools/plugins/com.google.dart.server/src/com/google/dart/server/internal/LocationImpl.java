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
 */
package com.google.dart.server.internal;

import com.google.dart.server.Location;
import com.google.dart.server.utilities.general.ObjectUtilities;

/**
 * A concrete implementation of {@link Location}.
 * 
 * @coverage dart.server
 */
public class LocationImpl implements Location {

  private final String file;
  private final int offset;
  private final int length;
  private final int startLine;
  private final int startColumn;

  public LocationImpl(String file, int offset, int length, int startLine, int startColumn) {
    this.file = file;
    this.offset = offset;
    this.length = length;
    this.startLine = startLine;
    this.startColumn = startColumn;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LocationImpl) {
      LocationImpl other = (LocationImpl) obj;
      return ObjectUtilities.equals(other.file, file) && other.offset == offset
          && other.length == length && other.startLine == startLine
          && other.startColumn == startColumn;
    }
    return false;
  }

  @Override
  public String getFile() {
    return file;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public int getStartColumn() {
    return startColumn;
  }

  @Override
  public int getStartLine() {
    return startLine;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[file=");
    builder.append(file);
    builder.append(", offset=");
    builder.append(offset);
    builder.append(", length=");
    builder.append(length);
    builder.append(", startLine=");
    builder.append(startLine);
    builder.append(", startColumn=");
    builder.append(startColumn);
    builder.append("]");
    return builder.toString();
  }

}
