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

import java.util.ArrayList;
import java.util.List;

/**
 * A WIP frame object.
 * 
 * @see http://code.google.com/chrome/devtools/docs/protocol/tot/debugger.html#type-CallFrame
 */
public class WebkitCallFrame {

//  callFrameId ( CallFrameId )
//  Call frame identifier. This identifier is only valid while the virtual machine is paused.
//  functionName ( string )
//  Name of the JavaScript function called on this call frame.
//  location ( Location )
//  Location in the source code.
//  scopeChain ( array of Scope )
//  Scope chain for this call frame.
//  this ( Runtime.RemoteObject )
//  this object for this call frame.

  static List<WebkitCallFrame> createFrom(JSONArray arr) throws JSONException {
    List<WebkitCallFrame> frames = new ArrayList<WebkitCallFrame>();

    for (int i = 0; i < arr.length(); i++) {
      frames.add(createFrom(arr.getJSONObject(i)));
    }

    return frames;
  }

  private static WebkitCallFrame createFrom(JSONObject object) throws JSONException {
    WebkitCallFrame frame = new WebkitCallFrame();

    frame.callFrameId = JsonUtils.getString(object, "callFrameId");
    frame.functionName = JsonUtils.getString(object, "functionName");
    frame.location = WebkitLocation.createFrom(object.getJSONObject("location"));
    frame.thisObject = WebkitRemoteObject.createFrom(object.getJSONObject("this"));

    if (object.has("scopeChain")) {
      frame.scopeChain = WebkitScope.createFrom(object.getJSONArray("scopeChain"));
    }

    return frame;
  }

  private String callFrameId;

  private String functionName;

  private WebkitLocation location;

  private WebkitScope[] scopeChain;

  private WebkitRemoteObject thisObject;

  public String getCallFrameId() {
    return callFrameId;
  }

  public String getFunctionName() {
    return functionName;
  }

  public WebkitLocation getLocation() {
    return location;
  }

  public WebkitScope[] getScopeChain() {
    return scopeChain;
  }

  public WebkitRemoteObject getThisObject() {
    return thisObject;
  }

  @Override
  public String toString() {
    return "[" + callFrameId + "," + functionName + "]";
  }

}
