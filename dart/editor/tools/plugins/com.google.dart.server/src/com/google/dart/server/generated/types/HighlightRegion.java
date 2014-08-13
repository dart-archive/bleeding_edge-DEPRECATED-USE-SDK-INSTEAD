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
 * A description of a region that could have special highlighting associated with it.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class HighlightRegion {

  /**
   * The length of the region to be highlighted.
   */
  private final int length;

  /**
   * The offset of the region to be highlighted.
   */
  private final int offset;

  /**
   * The type of highlight associated with the region.
   */
  private final String type;

  /**
   * Constructor for {@link HighlightRegion}.
   */
  public HighlightRegion(String type, int offset, int length) {
    this.type = type;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof HighlightRegion) {
      HighlightRegion other = (HighlightRegion) obj;
      return
        ObjectUtilities.equals(other.type, type) &&
        other.offset == offset &&
        other.length == length;
    }
    return false;
  }

  /**
   * The length of the region to be highlighted.
   */
  public int getLength() {
    return length;
  }

  /**
   * The offset of the region to be highlighted.
   */
  public int getOffset() {
    return offset;
  }

  /**
   * The type of highlight associated with the region.
   */
  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("type=");
    builder.append(type.toString() + ", ");
    builder.append("offset=");
    builder.append(offset + ", ");
    builder.append("length=");
    builder.append(length + ", ");
    builder.append("]");
    return builder.toString();
  }

}
