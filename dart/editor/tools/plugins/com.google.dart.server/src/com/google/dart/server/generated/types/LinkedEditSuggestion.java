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
 * A suggestion of a value that could be used to replace all of the linked edit regions in a
 * LinkedEditGroup.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class LinkedEditSuggestion {

  /**
   * An empty array of {@link LinkedEditSuggestion}s.
   */
  public static final LinkedEditSuggestion[] EMPTY_ARRAY = new LinkedEditSuggestion[0];

  /**
   * The value that could be used to replace all of the linked edit regions.
   */
  private final String value;

  /**
   * The kind of value being proposed.
   */
  private final String kind;

  /**
   * Constructor for {@link LinkedEditSuggestion}.
   */
  public LinkedEditSuggestion(String value, String kind) {
    this.value = value;
    this.kind = kind;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LinkedEditSuggestion) {
      LinkedEditSuggestion other = (LinkedEditSuggestion) obj;
      return
        ObjectUtilities.equals(other.value, value) &&
        ObjectUtilities.equals(other.kind, kind);
    }
    return false;
  }

  /**
   * The kind of value being proposed.
   */
  public String getKind() {
    return kind;
  }

  /**
   * The value that could be used to replace all of the linked edit regions.
   */
  public String getValue() {
    return value;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("value", value);
    jsonObject.addProperty("kind", kind);
    return jsonObject;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("value=");
    builder.append(value + ", ");
    builder.append("kind=");
    builder.append(kind);
    builder.append("]");
    return builder.toString();
  }

}
