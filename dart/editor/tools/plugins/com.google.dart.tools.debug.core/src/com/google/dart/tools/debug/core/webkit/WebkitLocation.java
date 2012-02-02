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
 * A WIP location object.
 */
public class WebkitLocation {

  static WebkitLocation createFrom(JSONObject object) throws JSONException {
    WebkitLocation location = new WebkitLocation();

    location.scriptId = JsonUtils.getString(object, "scriptId");
    location.lineNumber = JsonUtils.getInt(object, "lineNumber", -1);
    location.columnNumber = JsonUtils.getInt(object, "columnNumber", -1);

    return location;
  }

  private int columnNumber;

  private int lineNumber;

  private String scriptId;

  WebkitLocation() {

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

  @Override
  public String toString() {
    return "[" + scriptId + "," + lineNumber + "," + columnNumber + "]";
  }

}
