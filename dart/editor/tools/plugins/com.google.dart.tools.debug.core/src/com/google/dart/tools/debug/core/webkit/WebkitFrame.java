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
 */
public class WebkitFrame {

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

  public static List<WebkitFrame> createFrom(JSONArray arr) throws JSONException {
    List<WebkitFrame> frames = new ArrayList<WebkitFrame>();

    for (int i = 0; i < arr.length(); i++) {
      frames.add(createFrom(arr.getJSONObject(i)));
    }

    return frames;
  }

  private static WebkitFrame createFrom(JSONObject object) throws JSONException {
    WebkitFrame frame = new WebkitFrame();

    frame.callFrameId = JsonUtils.getString(object, "callFrameId");
    frame.functionName = JsonUtils.getString(object, "functionName");

    // TODO(devoncarew): fill in the rest of this object

    return frame;
  }

  private String callFrameId;

  private String functionName;

  public String getCallFrameId() {
    return callFrameId;
  }

  public String getFunctionName() {
    return functionName;
  }

  @Override
  public String toString() {
    return "[" + callFrameId + "," + functionName + "]";
  }

}
