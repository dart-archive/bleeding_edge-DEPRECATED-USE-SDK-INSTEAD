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
import com.google.common.collect.Lists;
import com.google.dart.server.utilities.general.JsonUtilities;
import com.google.dart.server.utilities.general.ObjectUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.lang3.StringUtils;

/**
 * An indication of a problem with the execution of the server, typically in response to a request.
 * The error codes that can be returned are documented in the section titled Errors.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class Error {

  public static final Error[] EMPTY_ARRAY = new Error[0];

  public static final List<Error> EMPTY_LIST = Lists.newArrayList();

  /**
   * A code that uniquely identifies the error that occurred.
   */
  private final String code;

  /**
   * A short description of the error.
   */
  private final String message;

  /**
   * Additional data related to the error. This field is omitted if there is no additional data
   * available.
   */
  private final Object data;

  /**
   * Constructor for {@link Error}.
   */
  public Error(String code, String message, Object data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Error) {
      Error other = (Error) obj;
      return
        ObjectUtilities.equals(other.code, code) &&
        ObjectUtilities.equals(other.message, message) &&
        ObjectUtilities.equals(other.data, data);
    }
    return false;
  }

  public static Error fromJson(JsonObject jsonObject) {
    String code = jsonObject.get("code").getAsString();
    String message = jsonObject.get("message").getAsString();
    Object data = jsonObject.get("data") == null ? null : jsonObject.get("data").getAsJsonArray();
    return new Error(code, message, data);
  }

  public static List<Error> fromJsonArray(JsonArray jsonArray) {
    if (jsonArray == null) {
      return EMPTY_LIST;
    }
    ArrayList<Error> list = new ArrayList<Error>(jsonArray.size());
    Iterator<JsonElement> iterator = jsonArray.iterator();
    while (iterator.hasNext()) {
      list.add(fromJson(iterator.next().getAsJsonObject()));
    }
    return list;
  }

  /**
   * A code that uniquely identifies the error that occurred.
   */
  public String getCode() {
    return code;
  }

  /**
   * Additional data related to the error. This field is omitted if there is no additional data
   * available.
   */
  public Object getData() {
    return data;
  }

  /**
   * A short description of the error.
   */
  public String getMessage() {
    return message;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(code);
    builder.append(message);
    builder.append(data);
    return builder.toHashCode();
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("code", code);
    jsonObject.addProperty("message", message);
    return jsonObject;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("code=");
    builder.append(code + ", ");
    builder.append("message=");
    builder.append(message + ", ");
    builder.append("data=");
    builder.append(data);
    builder.append("]");
    return builder.toString();
  }

}
