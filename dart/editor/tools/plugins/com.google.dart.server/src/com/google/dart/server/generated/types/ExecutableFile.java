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
 * A description of an executable file.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class ExecutableFile {

  /**
   * The path of the executable file.
   */
  private final String file;

  /**
   * The offset of the region to be highlighted.
   */
  private final String offset;

  /**
   * Constructor for {@link ExecutableFile}.
   */
  public ExecutableFile(String file, String offset) {
    this.file = file;
    this.offset = offset;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ExecutableFile) {
      ExecutableFile other = (ExecutableFile) obj;
      return
        ObjectUtilities.equals(other.file, file) &&
        ObjectUtilities.equals(other.offset, offset);
    }
    return false;
  }

  /**
   * The path of the executable file.
   */
  public String getFile() {
    return file;
  }

  /**
   * The offset of the region to be highlighted.
   */
  public String getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("file=");
    builder.append(file.toString() + ", ");
    builder.append("offset=");
    builder.append(offset.toString() + ", ");
    builder.append("]");
    return builder.toString();
  }

}
