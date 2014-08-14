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
 * A directive to remove an existing file content overlay. After processing this directive, the
 * file contents will once again be read from the file system.
 *
 * If this directive is used on a file that doesn't currently have a content overlay, it has no
 * effect.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class RemoveContentOverlay {

  /**
   * An empty array of {@link RemoveContentOverlay}s.
   */
  public static final RemoveContentOverlay[] EMPTY_ARRAY = new RemoveContentOverlay[0];

  private final String type;

  /**
   * Constructor for {@link RemoveContentOverlay}.
   */
  public RemoveContentOverlay(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RemoveContentOverlay) {
      RemoveContentOverlay other = (RemoveContentOverlay) obj;
      return
        ObjectUtilities.equals(other.type, type);
    }
    return false;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("type=");
    builder.append(type);
    builder.append("]");
    return builder.toString();
  }

}
