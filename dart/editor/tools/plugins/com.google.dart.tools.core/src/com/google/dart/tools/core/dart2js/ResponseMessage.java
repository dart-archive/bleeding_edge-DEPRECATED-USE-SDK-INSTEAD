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

package com.google.dart.tools.core.dart2js;

import org.eclipse.core.resources.IMarker;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A JSON server response representing a compiler error or warning.
 */
public class ResponseMessage {
  public static class Location {
    public String path;

    public int start = -1;
    public int end = -1;

    public int line = -1;
    public int column = -1;
  }

  private String severityText;

  private String message;

  private Location location;

  ResponseMessage(JSONObject object) throws JSONException {
    // severity
    String prefix = null;

    if (object.has("prefix")) {
      prefix = object.getString("prefix");
    }

    if (prefix != null) {
      severityText = prefix.trim();

      if (severityText.endsWith(":")) {
        severityText = severityText.substring(0, severityText.length() - 1);
      }
    } else {
      severityText = "";
    }

    // message
    message = object.getString("message");

    // location
    if (object.has("span") && object.get("span") instanceof JSONObject) {
      JSONObject span = (JSONObject) object.get("span");

      location = new Location();

      location.path = span.getString("file");

      location.start = span.getInt("start");
      location.end = span.getInt("end");
      location.line = span.getInt("line");
      location.column = span.getInt("column");

      // The json server has 0-based lines; we use 1-based lines.
      if (location.line != -1) {
        location.line++;
      }
    }
  }

  public Location getLocation() {
    return location;
  }

  public String getMessage() {
    return message;
  }

  /**
   * See {@link IMarker#SEVERITY_ERROR}, {@link IMarker#SEVERITY_WARNING},
   * {@link IMarker#SEVERITY_INFO}.
   * 
   * @return
   */
  public int getSeverity() {
    if (severityText.equals("warning")) {
      return IMarker.SEVERITY_WARNING;
    } else if (severityText.equals("info")) {
      return IMarker.SEVERITY_INFO;
    }

    // "error", "fatal", or something unknown
    return IMarker.SEVERITY_ERROR;
  }

  public String getSeverityText() {
    return severityText;
  }

}
