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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.dart.engine.utilities.general.StringUtilities;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.Element;
import com.google.dart.server.ElementKind;
import com.google.dart.server.Location;
import com.google.dart.server.internal.local.computer.ElementImpl;
import com.google.dart.server.internal.shared.LocationImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Iterator;
import java.util.List;

/**
 * Abstract processor class which holds the {@link AnalysisServerListener} for all processors.
 * 
 * @coverage dart.server.remote
 */
public abstract class NotificationProcessor {
  /**
   * Return the {@link ElementKind} code for the given name. If the passed name cannot be found, an
   * {@link IllegalArgumentException} is thrown.
   */
  @VisibleForTesting
  public static ElementKind getElementKind(String kindName) {
    return ElementKind.valueOf(kindName);
  }

  private final AnalysisServerListener listener;

  public NotificationProcessor(AnalysisServerListener listener) {
    this.listener = listener;
  }

  /**
   * Process the given {@link JsonObject} notification and notify {@link #listener}.
   */
  public abstract void process(JsonObject response) throws Exception;

  protected Element constructElement(JsonObject elementObject) {
    ElementKind kind = getElementKind(elementObject.get("kind").getAsString());
    String name = elementObject.get("name").getAsString();
    Location location = constructLocation(elementObject.get("location").getAsJsonObject());
    int flags = elementObject.get("flags").getAsInt();
    String parameters = safelyGetAsString(elementObject, "parameters");
    String returnType = safelyGetAsString(elementObject, "returnType");
    return new ElementImpl(kind, name, location, flags, parameters, returnType);
  }

  /**
   * Given some {@link JsonArray} and of string primitives, return the {@link String} array.
   * 
   * @param strJsonArray some {@link JsonArray} of {@link String}s
   * @return the {@link String} array
   */
  protected String[] constructStringArray(JsonArray strJsonArray) {
    if (strJsonArray == null) {
      return StringUtilities.EMPTY_ARRAY;
    }
    List<String> strings = Lists.newArrayList();
    Iterator<JsonElement> iterator = strJsonArray.iterator();
    while (iterator.hasNext()) {
      strings.add(iterator.next().getAsString());
    }
    return strings.toArray(new String[strings.size()]);
  }

  protected AnalysisServerListener getListener() {
    return listener;
  }

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

  private Location constructLocation(JsonObject locationObject) {
    String file = locationObject.get("file").getAsString();
    int offset = locationObject.get("offset").getAsInt();
    int length = locationObject.get("length").getAsInt();
    return new LocationImpl(file, offset, length);
  }
}
