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
 * A description of a set of changes to a single file.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class SourceFileEdit {

  /**
   * A list of the edits used to effect the change.
   */
  private final List<SourceEdit> edits;

  /**
   * The file containing the code to be modified.
   */
  private final String file;

  /**
   * Constructor for {@link SourceFileEdit}.
   */
  public SourceFileEdit(String file, List<SourceEdit> edits) {
    this.file = file;
    this.edits = edits;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SourceFileEdit) {
      SourceFileEdit other = (SourceFileEdit) obj;
      return
        ObjectUtilities.equals(other.file, file) &&
        ObjectUtilities.equals(other.edits, edits);
    }
    return false;
  }

  /**
   * A list of the edits used to effect the change.
   */
  public List<SourceEdit> getEdits() {
    return edits;
  }

  /**
   * The file containing the code to be modified.
   */
  public String getFile() {
    return file;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("file=");
    builder.append(file.toString() + ", ");
    builder.append("edits=");
    builder.append(StringUtils.join(edits, ", ") + ", ");
    builder.append("]");
    return builder.toString();
  }

}
