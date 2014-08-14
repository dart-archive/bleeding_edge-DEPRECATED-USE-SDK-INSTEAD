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
 * A directive to modify an existing file content overlay. One or more ranges of text are deleted
 * from the old file content overlay and replaced with new text.
 *
 * The edits are applied in the order in which they occur in the list. This means that the offset
 * of each edit must be correct under the assumption that all previous edits have been applied.
 *
 * It is an error to use this overlay on a file that does not yet have a file content overlay or
 * that has had its overlay removed via RemoveContentOverlay.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class ChangeContentOverlay {

  /**
   * An empty array of {@link ChangeContentOverlay}s.
   */
  public static final ChangeContentOverlay[] EMPTY_ARRAY = new ChangeContentOverlay[0];

  /**
   */
  private final String type;

  /**
   * The edits to be applied to the file.
   */
  private final List<SourceEdit> edits;

  /**
   * Constructor for {@link ChangeContentOverlay}.
   */
  public ChangeContentOverlay(String type, List<SourceEdit> edits) {
    this.type = type;
    this.edits = edits;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ChangeContentOverlay) {
      ChangeContentOverlay other = (ChangeContentOverlay) obj;
      return
        ObjectUtilities.equals(other.type, type) &&
        ObjectUtilities.equals(other.edits, edits);
    }
    return false;
  }

  /**
   * The edits to be applied to the file.
   */
  public List<SourceEdit> getEdits() {
    return edits;
  }

  /**
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
    builder.append("edits=");
    builder.append(StringUtils.join(edits, ", ") + ", ");
    builder.append("]");
    return builder.toString();
  }

}
