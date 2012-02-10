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
      // TODO(devoncarew): encode this better
      remoteObject.value = "" + params.get("value");
    }

    return remoteObject;
  }

  private String className;

  private String description;

  private String objectId;

  private String subtype;

  private String type;

  private Object value;

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

  public String getType() {
    return type;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "[" + type + "," + value + "]";
  }

}
