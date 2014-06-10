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

import com.google.dart.tools.debug.core.util.DebuggerUtils;

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

  static List<WebkitCallFrame> createFrom(JSONArray arr) throws JSONException {
    List<WebkitCallFrame> frames = new ArrayList<WebkitCallFrame>();

    for (int i = 0; i < arr.length(); i++) {
      WebkitCallFrame frame = createFrom(arr.getJSONObject(i));

      // If we are on the first frame and there are at least 3 frames:
      if (i == 0 && arr.length() > 2) {
        if (DebuggerUtils.isInternalMethodName(frame.getFunctionName())) {
          // Strip out the first frame if it's _noSuchMethod. There will be another
          // "Object.noSuchMethod" on the stack. This sucks, but it's where we're choosing to put
          // the fix.
          continue;
        }
      }

      frames.add(frame);
    }

    return frames;
  }

  private static WebkitCallFrame createFrom(JSONObject object) throws JSONException {
    WebkitCallFrame frame = new WebkitCallFrame();

    frame.callFrameId = JsonUtils.getString(object, "callFrameId");
    frame.functionName = JsonUtils.getString(object, "functionName");
    frame.location = WebkitLocation.createFrom(object.getJSONObject("location"));
    frame.thisObject = WebkitRemoteObject.createFrom(object.getJSONObject("this"));

    if ("null".equals(frame.thisObject.className)) {
      frame.thisObject = null;
    }

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

  /**
   * Call frame identifier. This identifier is only valid while the virtual machine is paused.
   */
  public String getCallFrameId() {
    return callFrameId;
  }

  /**
   * Name of the Dart function called on this call frame.
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Return the 'isolate' scope - the list of libraries for the current isolate.
   */
  public WebkitRemoteObject getIsolateScope() {
    for (WebkitScope scope : getScopeChain()) {
      if (scope.isIsolate()) {
        return scope.getObject();
      }
    }

    return null;
  }

  /**
   * Return the 'libraries' scope - the list of libraries for the current isolate.
   */
  public WebkitRemoteObject getLibrariesScope() {
    for (WebkitScope scope : getScopeChain()) {
      if (scope.isLibraries()) {
        return scope.getObject();
      }
    }

    return null;
  }

  /**
   * Location in the source code.
   */
  public WebkitLocation getLocation() {
    return location;
  }

  /**
   * Scope chain for this call frame.
   */
  public WebkitScope[] getScopeChain() {
    return scopeChain;
  }

  /**
   * This object for this call frame.
   */
  public WebkitRemoteObject getThisObject() {
    return thisObject;
  }

  public boolean isPrivateMethod() {
    // _bar or foo._bar

    return functionName.startsWith("_") || functionName.contains("._");
  }

  public boolean isStaticMethod() {
    return thisObject == null || thisObject.getObjectId() == null;
  }

  @Override
  public String toString() {
    return "[" + callFrameId + "," + functionName + "]";
  }

}
