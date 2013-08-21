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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A WIP scope object.
 * 
 * @see http://code.google.com/chrome/devtools/docs/protocol/tot/runtime.html#type-RemoteObject
 */
public class WebkitRemoteObject {

  public static WebkitRemoteObject createFrom(JSONObject params) throws JSONException {
    WebkitRemoteObject remoteObject = new WebkitRemoteObject();

    remoteObject.className = JsonUtils.getString(params, "className");
    remoteObject.description = JsonUtils.getString(params, "description");
    remoteObject.objectId = JsonUtils.getString(params, "objectId");
    remoteObject.subtype = JsonUtils.getString(params, "subtype");
    remoteObject.type = JsonUtils.getString(params, "type");

    if (params.has("value")) {
      Object obj = params.get("value");
      remoteObject.value = String.valueOf(obj);
    }

    return remoteObject;
  }

  public static WebkitRemoteObject createNull() {
    WebkitRemoteObject obj = new WebkitRemoteObject();

    obj.type = "object";
    obj.subtype = "null";
    obj.description = "null";

    return obj;
  }

  String className;

  private String description;

  private String objectId;

  private String subtype;

  private String type;

  private String value;

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WebkitRemoteObject) {
      return ((WebkitRemoteObject) obj).getObjectId().equals(getObjectId());
    }

    return false;
  }

  public String getClassName() {
    if (className != null) {
      return className;
    }

    if (isString()) {
      return "String";
    }

    if (isBoolean()) {
      return "bool";
    }

    if (isNumber()) {
      return "num";
    }

    return null;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Return the length of the list if this object is a list.
   * 
   * @return
   */
  public int getListLength() {
    // TODO(devoncarew): write a test to notify us when this convention changes

    // Since there is no direct way to obtain the length of an array, use the description from
    // Dartium to derive the array length. value.getDescription() == "Array[x]"

    String str = getDescription();

    int startIndex = str.indexOf('[');
    int endIndex = str.indexOf(']', startIndex);

    if (startIndex != -1 && endIndex != -1) {
      String val = str.substring(startIndex + 1, endIndex);

      try {
        return Integer.parseInt(val);
      } catch (NumberFormatException nfe) {

      }
    }

    return 0;
  }

  public String getObjectId() {
    return objectId;
  }

  /**
   * One of "array", "date", "node", "null", "regexp".
   */
  public String getSubtype() {
    return subtype;
  }

  /**
   * One of "boolean", "function", "number", "object", "string", "undefined".
   */
  public String getType() {
    return type;
  }

  public String getValue() {
    // Only numbers are returned with a description, and it's often more useful then the value
    // field (for things like Infinity).
    if (isNumber() && description != null) {
      return description;
    } else {
      return value;
    }
  }

  @Override
  public int hashCode() {
    return objectId.hashCode();
  }

  public boolean hasObjectId() {
    return getObjectId() != null;
  }

  public boolean isBoolean() {
    return "boolean".equals(type);
  }

  public boolean isDartFunction() {
    return "[Dart Function]".equals(className);
  }

  public boolean isFunction() {
    return "function".equals(type);
  }

  public boolean isList() {
    return "array".equals(subtype);
  }

  public boolean isNode() {
    return "node".equals(subtype);
  }

  public boolean isNull() {
    if (isObject() && "null".equals(subtype)) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isNumber() {
    return "number".equals(type);
  }

  public boolean isObject() {
    return "object".equals(type);
  }

  public boolean isPrimitive() {
    return "number".equals(type) || "string".equals(type) || "boolean".equals(type);
  }

  public boolean isString() {
    return "string".equals(type);
  }

  public boolean isSyntaxError() {
    return isObject() && "SyntaxError".equals(className);
  }

  public boolean isUndefined() {
    return "undefined".equals(type);
  }

  @Override
  public String toString() {
    if (value == null) {
      return "[" + type + "," + description + "]";
    } else {
      return "[" + type + "," + value + "]";
    }
  }

}
