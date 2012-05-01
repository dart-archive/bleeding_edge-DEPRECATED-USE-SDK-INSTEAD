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

package com.google.dart.tools.debug.core.server;

import com.google.dart.tools.debug.core.webkit.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A VM location object.
 */
public class VmLocation {

  public static int eclipseToWebkitLine(int line) {
    return line - 1;
  }

  public static int webkitToElipseLine(int line) {
    return line + 1;
  }

  static VmLocation createFrom(JSONObject object) throws JSONException {
    VmLocation location = new VmLocation();

    location.scriptId = JsonUtils.getString(object, "scriptId");
    location.lineNumber = JsonUtils.getInt(object, "lineNumber", -1);
    location.columnNumber = JsonUtils.getInt(object, "columnNumber", -1);

    return location;
  }

  private int columnNumber;

  private int lineNumber;

  private String scriptId;

  VmLocation() {

  }

  public int getColumnNumber() {
    return columnNumber;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getScriptId() {
    return scriptId;
  }

  public JSONObject toJSONObject() throws JSONException {
    JSONObject object = new JSONObject();

    object.put("scriptId", scriptId);
    object.put("lineNumber", lineNumber);
    object.put("columnNumber", columnNumber);

    return object;
  }

  @Override
  public String toString() {
    return "[" + scriptId + "," + lineNumber + "," + columnNumber + "]";
  }

}
