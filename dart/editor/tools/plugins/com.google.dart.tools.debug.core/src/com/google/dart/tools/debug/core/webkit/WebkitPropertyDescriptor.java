/*
 * Copyright (c) 2012, the Dart project authors.
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

package com.google.dart.tools.debug.core.webkit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A WIP property descriptor object.
 * 
 * @see code.google.com/chrome/devtools/docs/protocol/tot/runtime.html#type-PropertyDescriptor
 */
public class WebkitPropertyDescriptor implements Comparable<WebkitPropertyDescriptor> {
  public static final String STATIC_FIELDS_OBJECT = "@staticFields";
  public static final String LIBRARY_OBJECT = "@library";

  public static WebkitPropertyDescriptor createIndexProperty(int index, WebkitRemoteObject value) {
    WebkitPropertyDescriptor descriptor = new WebkitPropertyDescriptor();

    descriptor.name = "[" + index + "]";
    descriptor.value = value;

    return descriptor;
  }

  public static WebkitPropertyDescriptor createObjectDescriptor(WebkitRemoteObject thisObject,
      String name) {
    WebkitPropertyDescriptor descriptor = new WebkitPropertyDescriptor();

    descriptor.name = name;
    descriptor.value = thisObject;

    return descriptor;
  }

  static WebkitPropertyDescriptor[] createFrom(JSONArray arr) throws JSONException {
    WebkitPropertyDescriptor[] results = new WebkitPropertyDescriptor[arr.length()];

    for (int i = 0; i < results.length; i++) {
      results[i] = WebkitPropertyDescriptor.createFrom(arr.getJSONObject(i));
    }

    return results;
  }

  static WebkitPropertyDescriptor createFrom(JSONObject params) throws JSONException {
    WebkitPropertyDescriptor descriptor = new WebkitPropertyDescriptor();

    descriptor.name = JsonUtils.getString(params, "name");
    descriptor.configurable = JsonUtils.getBoolean(params, "configurable");
    descriptor.enumerable = JsonUtils.getBoolean(params, "enumerable");
    descriptor.wasThrown = JsonUtils.getBoolean(params, "wasThrown");
    descriptor.writable = JsonUtils.getBoolean(params, "writable");

    // get ( optional RemoteObject )
    if (params.has("get")) {
      descriptor.getterFunction = WebkitRemoteObject.createFrom(params.getJSONObject("get"));
    }

    // set ( optional RemoteObject )
    if (params.has("set")) {
      descriptor.setterFunction = WebkitRemoteObject.createFrom(params.getJSONObject("set"));
    }

    // value ( optional RemoteObject )
    if (params.has("value")) {
      descriptor.value = WebkitRemoteObject.createFrom(params.getJSONObject("value"));
    }

    // __proto__
    if (descriptor.name.equals("__proto__")) {
      descriptor.enumerable = false;
    }

    if (descriptor.value != null) {
      if (descriptor.value.isDartFunction()) {
        // Convert "[Dart Function]" into something more user friendly.
        descriptor.value.setDescription("Function");
      }

      // [runtimeType, _Type]
      if (descriptor.name.equals("runtimeType") && "_Type".equals(descriptor.value.getClassName())) {
        descriptor.enumerable = false;
      }

      // Patch up the className for the @staticFields property.
      if (STATIC_FIELDS_OBJECT.equals(descriptor.name)) {
        descriptor.value.className = "Type";
      }

      // Patch up the className for the @library property.
      if (LIBRARY_OBJECT.equals(descriptor.name)) {
        descriptor.value.className = "Library";
      }
    }

    return descriptor;
  }

  private WebkitRemoteObject setterFunction;

  private WebkitRemoteObject getterFunction;

  private boolean writable;

  private boolean wasThrown;

  private String name;

  private boolean enumerable;

  private boolean configurable;

  private WebkitRemoteObject value;

  @Override
  public int compareTo(WebkitPropertyDescriptor other) {
    return getName().toUpperCase().compareTo(other.getName().toUpperCase());
  }

  /**
   * A function which serves as a getter for the property, or undefined if there is no getter
   * (accessor descriptors only).
   */
  public WebkitRemoteObject getGetterFunction() {
    return getterFunction;
  }

  public String getName() {
    return name;
  }

  /**
   * A function which serves as a setter for the property, or undefined if there is no setter
   * (accessor descriptors only).
   */
  public WebkitRemoteObject getSetterFunction() {
    return setterFunction;
  }

  /**
   * The (optional) value associated with the property.
   */
  public WebkitRemoteObject getValue() {
    return value;
  }

  /**
   * True if the type of this property descriptor may be changed and if the property may be deleted
   * from the corresponding object.
   */
  public boolean isConfigurable() {
    return configurable;
  }

  /**
   * True if this property shows up during enumeration of the properties on the corresponding
   * object.
   */
  public boolean isEnumerable() {
    return enumerable;
  }

  /**
   * True if the result was thrown during the evaluation.
   */
  public boolean isWasThrown() {
    return wasThrown;
  }

  /**
   * True if the value associated with the property may be changed (data descriptors only).
   */
  public boolean isWritable() {
    return writable;
  }

  @Override
  public String toString() {
    return "[" + name + "," + value + "]";
  }

}
