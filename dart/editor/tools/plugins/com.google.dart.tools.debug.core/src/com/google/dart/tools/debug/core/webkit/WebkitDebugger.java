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
    public void debuggerPaused(PausedReasonType reason, List<WebkitFrame> frames, Object data);

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
    public void debuggerPaused(PausedReasonType reason, List<WebkitFrame> frames, Object data) {

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
  private static final String DEBUGGER_BREAKPOINT_RESOLVED = "breakpointResolved";
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

  public void setBreakpointByUrl(String url, int line, final WebkitCallback callback) {
    // TODO(devoncarew): implement

    throw new UnsupportedOperationException("setBreakpointByUrl()");
  }

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
   * Legal values are PauseOnExceptionsType.ALL, PauseOnExceptionsType.NONE, or
   * PauseOnExceptionsType.UNCAUGHT.
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

      List<WebkitFrame> frames = WebkitFrame.createFrom(params.getJSONArray("callFrames"));

      Object data = null;

      if (params.has("data")) {
        data = params.get("data");
      }

      for (DebuggerListener listener : listeners) {
        listener.debuggerPaused(reason, frames, data);
      }
    } else {
      DartDebugCorePlugin.logWarning("unhandled notification: " + method);
    }
  }

  private void clearGlobalObjects() {
    breakpointMap.clear();
    scriptMap.clear();
  }

  private WebkitResult convertSetBreakpointResult(JSONObject object) throws JSONException {
    WebkitResult result = new WebkitResult();

    if (object.has("error")) {
      result.setError(object.get("error"));
    }

    if (object.has("result")) {
      // breakpointId
      // actualLocation

      // TODO(devoncarew): test if we get another notification through the debuggerBreakpointResolved event

      JSONObject obj = object.getJSONObject("result");

      result.setResult(obj.get("breakpointId"));
    }

    return result;
  }

}
