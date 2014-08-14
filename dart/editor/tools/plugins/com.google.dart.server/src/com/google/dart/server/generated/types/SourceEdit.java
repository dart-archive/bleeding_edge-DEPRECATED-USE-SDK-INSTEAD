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
 * A description of a single change to a single file.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class SourceEdit {

  /**
   * An empty array of {@link SourceEdit}s.
   */
  public static final SourceEdit[] EMPTY_ARRAY = new SourceEdit[0];

  /**
   * The offset of the region to be modified.
   */
  private final int offset;

  /**
   * The length of the region to be modified.
   */
  private final int length;

  /**
   * The code that is to replace the specified region in the original code.
   */
  private final String replacement;

  /**
   * Constructor for {@link SourceEdit}.
   */
  public SourceEdit(int offset, int length, String replacement) {
    this.offset = offset;
    this.length = length;
    this.replacement = replacement;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SourceEdit) {
      SourceEdit other = (SourceEdit) obj;
      return
        other.offset == offset &&
        other.length == length &&
        ObjectUtilities.equals(other.replacement, replacement);
    }
    return false;
  }

  /**
   * The length of the region to be modified.
   */
  public int getLength() {
    return length;
  }

  /**
   * The offset of the region to be modified.
   */
  public int getOffset() {
    return offset;
  }

  /**
   * The code that is to replace the specified region in the original code.
   */
  public String getReplacement() {
    return replacement;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("offset=");
    builder.append(offset + ", ");
    builder.append("length=");
    builder.append(length + ", ");
    builder.append("replacement=");
    builder.append(replacement);
    builder.append("]");
    return builder.toString();
  }

}
