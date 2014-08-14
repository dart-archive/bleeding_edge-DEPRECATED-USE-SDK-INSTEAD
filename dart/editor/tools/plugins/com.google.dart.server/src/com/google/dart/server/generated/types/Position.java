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
 * A position within a file.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class Position {

  /**
   * An empty array of {@link Position}s.
   */
  public static final Position[] EMPTY_ARRAY = new Position[0];

  /**
   * The file containing the position.
   */
  private final String file;

  /**
   * The offset of the position.
   */
  private final int offset;

  /**
   * Constructor for {@link Position}.
   */
  public Position(String file, int offset) {
    this.file = file;
    this.offset = offset;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Position) {
      Position other = (Position) obj;
      return
        ObjectUtilities.equals(other.file, file) &&
        other.offset == offset;
    }
    return false;
  }

  /**
   * The file containing the position.
   */
  public String getFile() {
    return file;
  }

  /**
   * The offset of the position.
   */
  public int getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("file=");
    builder.append(file.toString() + ", ");
    builder.append("offset=");
    builder.append(offset + ", ");
    builder.append("]");
    return builder.toString();
  }

}
