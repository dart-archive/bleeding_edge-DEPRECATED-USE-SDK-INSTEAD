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
 * A WIP scope object.
 * 
 * @see http://code.google.com/chrome/devtools/docs/protocol/tot/debugger.html#type-Scope
 */
public class WebkitScope {

  static WebkitScope[] createFrom(JSONArray arr) throws JSONException {
    if (arr == null) {
      return null;
    }

    WebkitScope[] scopes = new WebkitScope[arr.length()];

    for (int i = 0; i < scopes.length; i++) {
      scopes[i] = createFrom(arr.getJSONObject(i));
    }

    return scopes;
  }

  static WebkitScope createFrom(JSONObject params) throws JSONException {
    WebkitScope scope = new WebkitScope();

    // type ( enumerated string [ "catch" , "closure" , "global" , "local" , "with" ] )
    scope.type = JsonUtils.getString(params, "type");
    scope.object = WebkitRemoteObject.createFrom(params.getJSONObject("object"));

    return scope;
  }

  private String type;

  private WebkitRemoteObject object;

  public WebkitRemoteObject getObject() {
    return object;
  }

  /**
   * Valid values include "local".
   * 
   * @return the scope type
   */
  public String getType() {
    return type;
  }

  public boolean isGlobal() {
    return "global".equals(type);
  }

  @Override
  public String toString() {
    return "[" + type + "," + object + "]";
  }

}
