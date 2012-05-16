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

  static VmLocation createFrom(JSONObject object) throws JSONException {
    VmLocation location = new VmLocation();

    location.url = VmUtils.vmUrlToEclipse(JsonUtils.getString(object, "url"));
    location.lineNumber = JsonUtils.getInt(object, "lineNumber", -1);
    // This field is not currently used by the VM.
    location.columnNumber = JsonUtils.getInt(object, "columnNumber", -1);

    return location;
  }

  private int columnNumber;

  private int lineNumber;

  private String url;

  public VmLocation(String url, int lineNumber) {
    this.url = url;
    this.lineNumber = lineNumber;
  }

  VmLocation() {

  }

  public int getColumnNumber() {
    return columnNumber;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getUrl() {
    return url;
  }

  public JSONObject toJSONObject() throws JSONException {
    JSONObject object = new JSONObject();

    object.put("url", url);
    object.put("lineNumber", lineNumber);

    if (columnNumber != -1) {
      object.put("columnNumber", columnNumber);
    }

    return object;
  }

  @Override
  public String toString() {
    return "[" + url + "," + lineNumber + (columnNumber == -1 ? "" : "," + columnNumber) + "]";
  }

}
