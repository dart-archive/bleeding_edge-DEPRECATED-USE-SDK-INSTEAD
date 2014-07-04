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
 */
package com.google.dart.server.internal.remote.processor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Abstract processor class with common behavior for {@link NotificationProcessor} and
 * {@link ResultProcessor}.
 * 
 * @coverage dart.server.remote
 */
public abstract class JsonProcessor {

  /**
   * Safely get some member off of the passed {@link JsonObject} and return the {@code int}. Instead
   * of calling {@link JsonObject#has(String)} before {@link JsonObject#get(String)}, only one call
   * to the {@link JsonObject} is made in order to be faster. The result will be the passed default
   * value if the member is not on the {@link JsonObject}. This is used for optional json
   * parameters.
   * 
   * @param jsonObject the {@link JsonObject}
   * @param memberName the member name
   * @param defaultValue the default value if the member is not in the {@link JsonObject}
   * @return the looked up {@link JsonArray}, or {@code null}
   */
  protected int safelyGetAsInt(JsonObject jsonObject, String memberName, int defaultValue) {
    JsonElement jsonElement = jsonObject.get(memberName);
    if (jsonElement == null) {
      return defaultValue;
    } else {
      return jsonElement.getAsInt();
    }
  }

  /**
   * Safely get some member off of the passed {@link JsonObject} and return the {@link JsonArray}.
   * Instead of calling {@link JsonObject#has(String)} before {@link JsonObject#get(String)}, only
   * one call to the {@link JsonObject} is made in order to be faster. The result will be
   * {@code null} if the member is not on the {@link JsonObject}. This is used for optional json
   * parameters.
   * 
   * @param jsonObject the {@link JsonObject}
   * @param memberName the member name
   * @return the looked up {@link JsonArray}, or {@code null}
   */
  protected JsonArray safelyGetAsJsonArray(JsonObject jsonObject, String memberName) {
    JsonElement jsonElement = jsonObject.get(memberName);
    if (jsonElement == null) {
      return null;
    } else {
      return jsonElement.getAsJsonArray();
    }
  }

  /**
   * Safely get some member off of the passed {@link JsonObject} and return the {@link String}.
   * Instead of calling {@link JsonObject#has(String)} before {@link JsonObject#get(String)}, only
   * one call to the {@link JsonObject} is made in order to be faster. The result will be
   * {@code null} if the member is not on the {@link JsonObject}. This is used for optional json
   * parameters.
   * 
   * @param jsonObject the {@link JsonObject}
   * @param memberName the member name
   * @return the looked up {@link String}, or {@code null}
   */
  protected String safelyGetAsString(JsonObject jsonObject, String memberName) {
    JsonElement jsonElement = jsonObject.get(memberName);
    if (jsonElement == null) {
      return null;
    } else {
      return jsonElement.getAsString();
    }
  }

}
