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
 * A description of a set of edits that implement a single conceptual change.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class SourceChange {

  /**
   * An empty array of {@link SourceChange}s.
   */
  public static final SourceChange[] EMPTY_ARRAY = new SourceChange[0];

  /**
   * A human-readable description of the change to be applied.
   */
  private final String message;

  /**
   * A list of the edits used to effect the change, grouped by file.
   */
  private final List<SourceFileEdit> edits;

  /**
   * A list of the linked editing groups used to customize the changes that were made.
   */
  private final List<LinkedEditGroup> linkedEditGroups;

  /**
   * The position that should be selected after the edits have been applied.
   */
  private final Position selection;

  /**
   * Constructor for {@link SourceChange}.
   */
  public SourceChange(String message, List<SourceFileEdit> edits, List<LinkedEditGroup> linkedEditGroups, Position selection) {
    this.message = message;
    this.edits = edits;
    this.linkedEditGroups = linkedEditGroups;
    this.selection = selection;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SourceChange) {
      SourceChange other = (SourceChange) obj;
      return
        ObjectUtilities.equals(other.message, message) &&
        ObjectUtilities.equals(other.edits, edits) &&
        ObjectUtilities.equals(other.linkedEditGroups, linkedEditGroups) &&
        ObjectUtilities.equals(other.selection, selection);
    }
    return false;
  }

  /**
   * A list of the edits used to effect the change, grouped by file.
   */
  public List<SourceFileEdit> getEdits() {
    return edits;
  }

  /**
   * A list of the linked editing groups used to customize the changes that were made.
   */
  public List<LinkedEditGroup> getLinkedEditGroups() {
    return linkedEditGroups;
  }

  /**
   * A human-readable description of the change to be applied.
   */
  public String getMessage() {
    return message;
  }

  /**
   * The position that should be selected after the edits have been applied.
   */
  public Position getSelection() {
    return selection;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("message=");
    builder.append(message + ", ");
    builder.append("edits=");
    builder.append(StringUtils.join(edits, ", ") + ", ");
    builder.append("linkedEditGroups=");
    builder.append(StringUtils.join(linkedEditGroups, ", ") + ", ");
    builder.append("selection=");
    builder.append(selection);
    builder.append("]");
    return builder.toString();
  }

}
