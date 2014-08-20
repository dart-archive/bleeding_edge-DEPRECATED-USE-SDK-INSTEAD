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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
  private final Integer offset;

  /**
   * The length of the region to be modified.
   */
  private final Integer length;

  /**
   * The code that is to replace the specified region in the original code.
   */
  private final String replacement;

  /**
   * An identifier that uniquely identifies this source edit from other edits in the same response.
   * This field is omitted unless a containing structure needs to be able to identify the edit for
   * some reason.
   *
   * For example, some refactoring operations can produce edits that might not be appropriate
   * (referred to as potential edits). Such edits will have an id so that they can be referenced.
   * Edits in the same response that do not need to be referenced will not have an id.
   */
  private final String id;

  /**
   * Constructor for {@link SourceEdit}.
   */
  public SourceEdit(Integer offset, Integer length, String replacement, String id) {
    this.offset = offset;
    this.length = length;
    this.replacement = replacement;
    this.id = id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SourceEdit) {
      SourceEdit other = (SourceEdit) obj;
      return
        other.offset == offset &&
        other.length == length &&
        ObjectUtilities.equals(other.replacement, replacement) &&
        ObjectUtilities.equals(other.id, id);
    }
    return false;
  }

  /**
   * An identifier that uniquely identifies this source edit from other edits in the same response.
   * This field is omitted unless a containing structure needs to be able to identify the edit for
   * some reason.
   *
   * For example, some refactoring operations can produce edits that might not be appropriate
   * (referred to as potential edits). Such edits will have an id so that they can be referenced.
   * Edits in the same response that do not need to be referenced will not have an id.
   */
  public String getId() {
    return id;
  }

  /**
   * The length of the region to be modified.
   */
  public Integer getLength() {
    return length;
  }

  /**
   * The offset of the region to be modified.
   */
  public Integer getOffset() {
    return offset;
  }

  /**
   * The code that is to replace the specified region in the original code.
   */
  public String getReplacement() {
    return replacement;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("offset", offset);
    jsonObject.addProperty("length", length);
    jsonObject.addProperty("replacement", replacement);
    if (id != null) {
      jsonObject.addProperty("id", id);
    }
    return jsonObject;
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
    builder.append(replacement + ", ");
    builder.append("id=");
    builder.append(id);
    builder.append("]");
    return builder.toString();
  }

}
