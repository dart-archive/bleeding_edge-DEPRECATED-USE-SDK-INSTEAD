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
 * A WIP breakpoint object.
 */
public class WebkitBreakpoint {

  public static WebkitBreakpoint createFrom(JSONObject params) throws JSONException {
    WebkitBreakpoint breakpoint = new WebkitBreakpoint();

    breakpoint.breakpointId = JsonUtils.getString(params, "breakpointId");
    breakpoint.location = WebkitLocation.createFrom(params.getJSONObject("location"));

    return breakpoint;
  }

  public static WebkitBreakpoint createFrom(String breakpointId, WebkitLocation location) {
    WebkitBreakpoint breakpoint = new WebkitBreakpoint();

    breakpoint.breakpointId = breakpointId;
    breakpoint.location = location;

    return breakpoint;
  }

  public static WebkitBreakpoint createFromActual(JSONObject params) throws JSONException {
    WebkitBreakpoint breakpoint = new WebkitBreakpoint();

    breakpoint.breakpointId = JsonUtils.getString(params, "breakpointId");
    breakpoint.location = WebkitLocation.createFrom(params.getJSONObject("actualLocation"));

    return breakpoint;
  }

  private WebkitLocation location;

  private String breakpointId;

  WebkitBreakpoint() {

  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof WebkitBreakpoint)) {
      return false;
    }

    WebkitBreakpoint obj = (WebkitBreakpoint) other;

    return breakpointId.equals(obj.getBreakpointId());
  }

  public String getBreakpointId() {
    return breakpointId;
  }

  public WebkitLocation getLocation() {
    return location;
  }

  @Override
  public int hashCode() {
    return breakpointId.hashCode();
  }

  @Override
  public String toString() {
    return "[" + breakpointId + "," + location + "]";
  }

}
