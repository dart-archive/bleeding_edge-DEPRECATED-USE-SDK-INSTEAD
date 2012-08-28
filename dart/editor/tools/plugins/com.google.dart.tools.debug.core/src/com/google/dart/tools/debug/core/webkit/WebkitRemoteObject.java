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

//    className ( optional string )

//    description ( optional string )

//    objectId ( optional RemoteObjectId )

//    subtype ( optional enumerated string [ "array" , "date" , "node" , "null" , "regexp" ] )

//    type ( enumerated string [ "boolean" , "function" , "number" , "object" , "string" , "undefined" ] )

//    value ( optional any )
//    Remote object value (in case of primitive values or JSON values if it was requested).

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

  private String className;

  private String description;

  private String objectId;

  private String subtype;

  private String type;

  private String value;

  public String getClassName() {
    return className;
  }

  public String getDescription() {
    return description;
  }

  public String getObjectId() {
    return objectId;
  }

  public String getSubtype() {
    return subtype;
  }

  /**
   * Valid values include "object", "string", and "number".
   * 
   * @return
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

  public boolean hasObjectId() {
    return getObjectId() != null;
  }

  public boolean isFunction() {
    return "function".equals(type);
  }

  public boolean isList() {
    return "array".equals(subtype);
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

  @Override
  public String toString() {
    if (value == null) {
      return "[" + type + "," + description + "]";
    } else {
      return "[" + type + "," + value + "]";
    }
  }

}
