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
 * A description of a region that can be folded.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class FoldingRegion {

  /**
   * An empty array of {@link FoldingRegion}s.
   */
  public static final FoldingRegion[] EMPTY_ARRAY = new FoldingRegion[0];

  /**
   * The kind of the region.
   */
  private final String kind;

  /**
   * The offset of the region to be folded.
   */
  private final int offset;

  /**
   * The length of the region to be folded.
   */
  private final int length;

  /**
   * Constructor for {@link FoldingRegion}.
   */
  public FoldingRegion(String kind, int offset, int length) {
    this.kind = kind;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FoldingRegion) {
      FoldingRegion other = (FoldingRegion) obj;
      return
        ObjectUtilities.equals(other.kind, kind) &&
        other.offset == offset &&
        other.length == length;
    }
    return false;
  }

  /**
   * The kind of the region.
   */
  public String getKind() {
    return kind;
  }

  /**
   * The length of the region to be folded.
   */
  public int getLength() {
    return length;
  }

  /**
   * The offset of the region to be folded.
   */
  public int getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("kind=");
    builder.append(kind + ", ");
    builder.append("offset=");
    builder.append(offset + ", ");
    builder.append("length=");
    builder.append(length);
    builder.append("]");
    return builder.toString();
  }

}
