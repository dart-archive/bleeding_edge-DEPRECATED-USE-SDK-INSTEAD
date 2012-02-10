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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.webkit.WebkitConnection.Callback;
import com.google.dart.tools.debug.core.webkit.WebkitConnection.NotificationHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A WIP debugger domain object.
 */
public class WebkitDebugger extends WebkitDomain {

  public static interface DebuggerListener {
    /**
     * Fired when breakpoint is resolved to an actual script and location.
     * 
     * @param breakpoint
     */
    public void debuggerBreakpointResolved(WebkitBreakpoint breakpoint);

    /**
     * Called when global has been cleared and debugger client should reset its state. Happens upon
     * navigation or reload.
     */
    public void debuggerGlobalObjectCleared();

    /**
     * Fired when the virtual machine stopped on breakpoint or exception or any other stop criteria.
     * 
     * @param reason
     * @param frames
     * @param data
     */
    public void debuggerPaused(PausedReasonType reason, List<WebkitCallFrame> frames, Object data);

    /**
     * Fired when the virtual machine resumed execution.
     */
    public void debuggerResumed();

    /**
     * Fired when virtual machine parses script. This event is also fired for all known and
     * uncollected scripts upon enabling debugger.
     * 
     * @param script
     */
    public void debuggerScriptParsed(WebkitScript script);
  }

  public abstract static class DebuggerListenerAdapter implements DebuggerListener {
    @Override
    public void debuggerBreakpointResolved(WebkitBreakpoint breakpoint) {

    }

    @Override
    public void debuggerGlobalObjectCleared() {

    }

    @Override
    public void debuggerPaused(PausedReasonType reason, List<WebkitCallFrame> frames, Object data) {

    }

    @Override
    public void debuggerResumed() {

    }

    @Override
    public void debuggerScriptParsed(WebkitScript script) {

    }
  }

  public static enum PausedReasonType {
    DOM, EventListener, XHR, exception, other
  }

  public static enum PauseOnExceptionsType {
    all, none, uncaught
  }

  private static final String DEBUGGER_RESUMED = "Debugger.resumed";
  private static final String DEBUGGER_PAUSED = "Debugger.paused";
  private static final String DEBUGGER_GLOBAL_OBJECT_CLEARED = "Debugger.globalObjectCleared";
  private static final String DEBUGGER_BREAKPOINT_RESOLVED = "Debugger.breakpointResolved";
  private static final String DEBUGGER_SCRIPT_PARSED = "Debugger.scriptParsed";

  private List<DebuggerListener> listeners = new ArrayList<DebuggerListener>();

  private Map<String, WebkitScript> scriptMap = new HashMap<String, WebkitScript>();
  private Map<String, WebkitBreakpoint> breakpointMap = new HashMap<String, WebkitBreakpoint>();

  public WebkitDebugger(WebkitConnection connection) {
    super(connection);

    connection.registerNotificationHandler("Debugger.", new NotificationHandler() {
      @Override
      public void handleNotification(String method, JSONObject params) throws JSONException {
        handleDebuggerNotification(method, params);
      }
    });
  }

  public void addDebuggerListener(DebuggerListener listener) {
    listeners.add(listener);
  }

  public void disable() throws IOException {
    sendSimpleCommand("Debugger.disable");
  }

  public void enable() throws IOException {
    sendSimpleCommand("Debugger.enable");
  }

  public Collection<WebkitBreakpoint> getAllBreakpoints() {
    return breakpointMap.values();
  }

  public Collection<WebkitScript> getAllScripts() {
    return scriptMap.values();
  }

  public WebkitScript getScript(String scriptId) {
    return scriptMap.get(scriptId);
  }

  /**
   * Returns source for the script with given id.
   * <p>
   * If successful, the WebkitResult object contains a String for the script's source.
   * 
   * @param scriptId d of the script to get source for
   * @param callback
   * @throws IOException
   */
  public void getScriptSource(String scriptId, final WebkitCallback callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("method", "Debugger.getScriptSource");
      request.put("params", new JSONObject().put("scriptId", scriptId));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetScriptSourceResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void pause() throws IOException {
    sendSimpleCommand("Debugger.pause");
  }

  public void removeBreakpoint(String breakpointId) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Debugger.removeBreakpoint");
      request.put("params", new JSONObject().put("breakpointId", breakpointId));

      connection.sendRequest(request);

      breakpointMap.remove(breakpointId);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void removeDebuggerListener(DebuggerListener listener) {
    listeners.remove(listener);
  }

  public void resume() throws IOException {
    sendSimpleCommand("Debugger.resume");
  }

  /**
   * Sets JavaScript breakpoint at a given location.
   * <p>
   * If successful, the WebkitResult object contains a WebkitBreakpoint.
   * 
   * @param script
   * @param line
   * @param callback
   * @throws IOException
   */
  public void setBreakpoint(WebkitScript script, int line, final WebkitCallback callback)
      throws IOException {
    try {
      JSONObject location = new JSONObject().put("lineNumber", line).put("scriptId",
          script.getScriptId());

      JSONObject request = new JSONObject();

      request.put("method", "Debugger.setBreakpoint");
      request.put("params", new JSONObject().put("location", location));

      if (callback == null) {
        connection.sendRequest(request);
      } else {
        connection.sendRequest(request, new Callback() {
          @Override
          public void handleResult(JSONObject result) throws JSONException {
            callback.handleResult(convertSetBreakpointResult(result));
          }
        });
      }
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Sets JavaScript breakpoint at given location specified either by URL or URL regex. Once this
   * command is issued, all existing parsed scripts will have breakpoints resolved and returned in
   * locations property. Further matching script parsing will result in subsequent
   * breakpointResolved events issued. This logical breakpoint will survive page reloads.
   * <p>
   * If successful and the breakpoint resolved to a loaded script, the WebkitResult object contains
   * a WebkitBreakpoint.
   * 
   * @param url URL of the resources to set breakpoint on (optional)
   * @param urlRegex Regex pattern for the URLs of the resources to set breakpoints on. Either url
   *          or urlRegex must be specified.
   * @param line line number to set breakpoint at
   * @param callback
   */
  public void setBreakpointByUrl(String url, String urlRegex, int line,
      final WebkitCallback callback) throws IOException {
    try {
      JSONObject params = new JSONObject();

      params.put("lineNumber", line);

      if (url != null) {
        params.put("url", url);
      }

      if (urlRegex != null) {
        params.put("urlRegex", urlRegex);
      }

      JSONObject request = new JSONObject();

      request.put("method", "Debugger.setBreakpointByUrl");
      request.put("params", params);

      if (callback == null) {
        connection.sendRequest(request);
      } else {
        connection.sendRequest(request, new Callback() {
          @Override
          public void handleResult(JSONObject result) throws JSONException {
            callback.handleResult(convertSetBreakpointByUrlResult(result));
          }
        });
      }
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Activates / deactivates all breakpoints on the page.
   * 
   * @param active new value for breakpoints active state
   * @throws IOException
   */
  public void setBreakpointsActive(boolean active) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Debugger.setBreakpointsActive");
      request.put("params", new JSONObject().put("active", active));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Legal values are PauseOnExceptionsType.all, PauseOnExceptionsType.none, or
   * PauseOnExceptionsType.uncaught.
   * 
   * @param state
   * @throws IOException
   */
  public void setPauseOnExceptions(PauseOnExceptionsType state) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Debugger.setPauseOnExceptions");
      request.put("params", new JSONObject().put("state", state.toString()));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void stepInto() throws IOException {
    sendSimpleCommand("Debugger.stepInto");
  }

  public void stepOut() throws IOException {
    sendSimpleCommand("Debugger.stepOut");
  }

  public void stepOver() throws IOException {
    sendSimpleCommand("Debugger.stepOver");
  }

  protected void handleDebuggerNotification(String method, JSONObject params) throws JSONException {
    if (method.equals(DEBUGGER_RESUMED)) {
      for (DebuggerListener listener : listeners) {
        listener.debuggerResumed();
      }
    } else if (method.equals(DEBUGGER_GLOBAL_OBJECT_CLEARED)) {
      clearGlobalObjects();

      for (DebuggerListener listener : listeners) {
        listener.debuggerGlobalObjectCleared();
      }
    } else if (method.equals(DEBUGGER_SCRIPT_PARSED)) {
      WebkitScript script = WebkitScript.createFrom(params);

      scriptMap.put(script.getScriptId(), script);

      for (DebuggerListener listener : listeners) {
        listener.debuggerScriptParsed(script);
      }
    } else if (method.equals(DEBUGGER_BREAKPOINT_RESOLVED)) {
      WebkitBreakpoint breakpoint = WebkitBreakpoint.createFrom(params);

      for (DebuggerListener listener : listeners) {
        listener.debuggerBreakpointResolved(breakpoint);
      }
    } else if (method.equals(DEBUGGER_PAUSED)) {
      PausedReasonType reason = PausedReasonType.valueOf(params.getString("reason"));

      List<WebkitCallFrame> frames = WebkitCallFrame.createFrom(params.getJSONArray("callFrames"));

      Object data = null;

      if (params.has("data")) {
        data = params.get("data");
      }

      for (DebuggerListener listener : listeners) {
        listener.debuggerPaused(reason, frames, data);
      }
    } else {
      DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

  private void clearGlobalObjects() {
    breakpointMap.clear();
    scriptMap.clear();
  }

  private WebkitResult convertGetScriptSourceResult(JSONObject object) throws JSONException {
    WebkitResult result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(JsonUtils.getString(object.getJSONObject("result"), "scriptSource"));
    }

    return result;
  }

  private WebkitResult convertSetBreakpointByUrlResult(JSONObject object) throws JSONException {
    // "result":{
    //   "locations":[{"lineNumber":9,"scriptId":"-1","columnNumber":0}],
    //   "breakpointId":"http://0.0.0.0:3030/webapp/webapp.dart:9:0"
    // }

    WebkitResult result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      JSONObject temp = object.getJSONObject("result");

      if (temp.has("locations")) {
        JSONArray arr = temp.getJSONArray("locations");

        if (arr.length() > 0) {
          String breakpointId = JsonUtils.getString(temp, "breakpointId");
          WebkitLocation location = WebkitLocation.createFrom(arr.getJSONObject(0));

          WebkitBreakpoint breakpoint = WebkitBreakpoint.createFrom(breakpointId, location);

          result.setResult(breakpoint);
        }
      }
    }

    return result;
  }

  private WebkitResult convertSetBreakpointResult(JSONObject object) throws JSONException {
    // "result": {
    //   "breakpointId": <BreakpointId>,
    //   "actualLocation": <Location> 
    // }

    WebkitResult result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      WebkitBreakpoint breakpoint = WebkitBreakpoint.createFromActual(object.getJSONObject("result"));

      result.setResult(breakpoint);
    }

    return result;
  }

}
