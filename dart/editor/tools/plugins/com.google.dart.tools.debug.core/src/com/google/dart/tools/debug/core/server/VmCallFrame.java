/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.core.server;

import com.google.dart.tools.debug.core.webkit.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A VM frame object.
 */
public class VmCallFrame {

  static List<VmCallFrame> createFrom(JSONArray arr) throws JSONException {
    List<VmCallFrame> frames = new ArrayList<VmCallFrame>();

    for (int i = 0; i < arr.length(); i++) {
      frames.add(createFrom(arr.getJSONObject(i)));
    }

    return frames;
  }

  private static VmCallFrame createFrom(JSONObject object) throws JSONException {
    VmCallFrame frame = new VmCallFrame();

    frame.functionName = JsonUtils.getString(object, "functionName");
    frame.location = VmLocation.createFrom(object.getJSONObject("location"));
    frame.locals = VmVariable.createFrom(object.optJSONArray("locals"));

    return frame;
  }

  private String functionName;

  private VmLocation location;

  private List<VmVariable> locals;

  /**
   * Name of the Dart function called on this call frame.
   */
  public String getFunctionName() {
    return functionName;
  }

  public List<VmVariable> getLocals() {
    return locals;
  }

  /**
   * Location in the source code.
   */
  public VmLocation getLocation() {
    return location;
  }

  @Override
  public String toString() {
    return "[" + functionName + "]";
  }

}
