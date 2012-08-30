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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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
     * @param exception
     */
    public void debuggerPaused(PausedReasonType reason, List<WebkitCallFrame> frames,
        WebkitRemoteObject exception);

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
    public void debuggerPaused(PausedReasonType reason, List<WebkitCallFrame> frames,
        WebkitRemoteObject exception) {

    }

    @Override
    public void debuggerResumed() {

    }

    @Override
    public void debuggerScriptParsed(WebkitScript script) {

    }
  }

  public static enum PausedReasonType {
    DOM,
    EventListener,
    XHR,
    exception,
    other;

    /**
     * Call valueOf(); Catch exceptions in the cases of invalid enum values and return null in those
     * cases.
     * 
     * @param str
     * @return
     */
    public static PausedReasonType value(String str) {
      try {
        return PausedReasonType.valueOf(str);
      } catch (IllegalArgumentException exception) {
        return null;
      } catch (NullPointerException exception) {
        return null;
      }
    }
  }

  public static enum PauseOnExceptionsType {
    all,
    none,
    uncaught
  }

  private static final String DEBUGGER_RESUMED = "Debugger.resumed";
  private static final String DEBUGGER_PAUSED = "Debugger.paused";
  private static final String DEBUGGER_GLOBAL_OBJECT_CLEARED = "Debugger.globalObjectCleared";
  private static final String DEBUGGER_BREAKPOINT_RESOLVED = "Debugger.breakpointResolved";
  private static final String DEBUGGER_SCRIPT_PARSED = "Debugger.scriptParsed";

  private static final String OBJECT_GROUP_KEY = "objectGroup";

  private List<DebuggerListener> listeners = new ArrayList<DebuggerListener>();

  private Map<String, WebkitScript> scriptMap = new HashMap<String, WebkitScript>();
  private Map<String, WebkitBreakpoint> breakpointMap = new HashMap<String, WebkitBreakpoint>();
  private Map<WebkitRemoteObject, String> classInfoMap = new HashMap<WebkitRemoteObject, String>();

  private int remoteObjectCount;

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

  /**
   * Tells whether setScriptSource is supported.
   * <p>
   * If successful, the WebkitResult object contains a Boolean value.
   * 
   * @throws IOException
   */
  public void canSetScriptSource(final WebkitCallback<Boolean> callback) throws IOException {
    sendSimpleCommand("Debugger.canSetScriptSource", new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        callback.handleResult(convertCanSetScriptSourceResult(result));
      }
    });
  }

  /**
   * Continues execution until specific location is reached.
   * 
   * @param location
   * @throws IOException
   */
  public void continueToLocation(WebkitLocation location) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Debugger.continueToLocation");
      request.put("params", new JSONObject().put("location", location.toJSONObject()));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void disable() throws IOException {
    sendSimpleCommand("Debugger.disable");
  }

  public void enable() throws IOException {
    sendSimpleCommand("Debugger.enable");
  }

  public void evaluateOnCallFrame(String callFrameId, String expression,
      final WebkitCallback<WebkitRemoteObject> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("method", "Debugger.evaluateOnCallFrame");
      request.put(
          "params",
          new JSONObject().put("callFrameId", callFrameId).put("expression", expression).put(
              "objectGroup",
              OBJECT_GROUP_KEY).put("returnByValue", true));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertEvaluateOnCallFrameResult(result));
        }
      });

      remoteObjectCount++;
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public Collection<WebkitBreakpoint> getAllBreakpoints() {
    return breakpointMap.values();
  }

  public Collection<WebkitScript> getAllScripts() {
    return scriptMap.values();
  }

  public String getClassNameSync(WebkitRemoteObject value) {
    WebkitRemoteObject classInfo = value.getClassInfo();

    if (classInfo != null) {
      if (!classInfoMap.containsKey(classInfo)) {
        populateClassInfoMap(classInfo);
      }

      return classInfoMap.get(classInfo);
    } else {
      return value.getClassName();
    }
  }

  public WebkitScript getScript(String scriptId) {
    return scriptMap.get(scriptId);
  }

  public WebkitScript getScriptByUrl(String url) {
    for (WebkitScript script : getAllScripts()) {
      if (url.equals(script.getUrl())) {
        return script;
      }
    }

    return null;
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
  public void getScriptSource(String scriptId, final WebkitCallback<String> callback)
      throws IOException {
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

  /**
   * This is a convenience method which will synchronously populate the given script's source if
   * necessary.
   * 
   * @param script
   * @throws IOException
   */
  public void populateScriptSource(WebkitScript script) throws IOException {
    if (!script.hasScriptSource()) {
      final IOException[] error = new IOException[1];
      final String[] source = new String[1];

      final CountDownLatch latch = new CountDownLatch(1);

      getScriptSource(script.getScriptId(), new WebkitCallback<String>() {
        @Override
        public void handleResult(WebkitResult<String> result) {
          if (result.isError()) {
            error[0] = new IOException("error retrieving script source");
          } else {
            source[0] = result.getResult();
          }

          latch.countDown();
        }
      });

      try {
        latch.await();
      } catch (InterruptedException e) {
        throw new IOException(e);
      }

      if (error[0] != null) {
        throw error[0];
      }

      script.setScriptSource(source[0]);
    }
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
  public void setBreakpoint(WebkitScript script, int line,
      final WebkitCallback<WebkitBreakpoint> callback) throws IOException {
    try {
      JSONObject location = new JSONObject().put("lineNumber", line).put(
          "scriptId",
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
   * @param callback the breakpointId of the created breakpoint
   */
  public void setBreakpointByUrl(String url, String urlRegex, int line,
      final WebkitCallback<String> callback) throws IOException {
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
            List<WebkitBreakpoint> resolvedBreakpoints = new ArrayList<WebkitBreakpoint>();

            callback.handleResult(convertSetBreakpointByUrlResult(result, resolvedBreakpoints));

            // This will resolve immediately if the script is loaded in the browser. Otherwise the 
            // breakpoint info will be sent to us using the breakpoint resolved notification.
            if (resolvedBreakpoints.size() > 0) {
              WebkitBreakpoint bp = resolvedBreakpoints.get(0);

              for (DebuggerListener listener : listeners) {
                listener.debuggerBreakpointResolved(bp);
              }
            }
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

  /**
   * Edits source live.
   * <p>
   * If the VM is paused, and the source change causes changes to the stack, a debugger paused event
   * will be fired with the new stack information.
   * 
   * @throws IOException
   */
  public void setScriptSource(String scriptId, String scriptSource) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Debugger.setScriptSource");
      request.put(
          "params",
          new JSONObject().put("scriptId", scriptId).put("scriptSource", scriptSource));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          WebkitResult<WebkitCallFrame[]> webkitResult = convertSetScriptSourceResult(result);

          if (!webkitResult.isError() && webkitResult.getResult() != null) {
            List<WebkitCallFrame> frames = Arrays.asList(webkitResult.getResult());

            for (DebuggerListener listener : listeners) {
              listener.debuggerPaused(PausedReasonType.other, frames, null);
            }
          }
        }
      });
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

      handleResumed();
    } else if (method.equals(DEBUGGER_GLOBAL_OBJECT_CLEARED)) {
      clearGlobalObjects();

      for (DebuggerListener listener : listeners) {
        listener.debuggerGlobalObjectCleared();
      }
    } else if (method.equals(DEBUGGER_SCRIPT_PARSED)) {
      WebkitScript script = WebkitScript.createFrom(params);

      // We get a blizzard of empty script parsed events from Dartium due to the way they integrated
      // the Dart VM into the Webkit debugger.
      if (script.getUrl().length() > 0) {
        scriptMap.put(script.getScriptId(), script);

        for (DebuggerListener listener : listeners) {
          listener.debuggerScriptParsed(script);
        }
      }
    } else if (method.equals(DEBUGGER_BREAKPOINT_RESOLVED)) {
      WebkitBreakpoint breakpoint = WebkitBreakpoint.createFrom(params);

      for (DebuggerListener listener : listeners) {
        listener.debuggerBreakpointResolved(breakpoint);
      }
    } else if (method.equals(DEBUGGER_PAUSED)) {
      PausedReasonType reason = PausedReasonType.value(params.getString("reason"));

      List<WebkitCallFrame> frames = WebkitCallFrame.createFrom(params.getJSONArray("callFrames"));

      WebkitRemoteObject exception = null;

      // The data field contains exception info.
      if (reason == PausedReasonType.exception && params.has("data")) {
        // {"value":"ssdfsdfd","type":"string"}
        exception = WebkitRemoteObject.createFrom(params.getJSONObject("data"));
      }

      for (DebuggerListener listener : listeners) {
        listener.debuggerPaused(reason, frames, exception);
      }
    } else {
      DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

  private void clearGlobalObjects() {
    breakpointMap.clear();
    scriptMap.clear();
  }

  private void clearRemoteObjects() {
    if (remoteObjectCount > 0) {
      remoteObjectCount = 0;

      try {
        getConnection().getRuntime().releaseObjectGroup(OBJECT_GROUP_KEY);
      } catch (IOException e) {
        // This is a best-effort call.

      }
    }
  }

  private WebkitResult<Boolean> convertCanSetScriptSourceResult(JSONObject object)
      throws JSONException {
    WebkitResult<Boolean> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(Boolean.valueOf(object.getJSONObject("result").getBoolean("result")));
    }

    return result;
  }

  private WebkitResult<WebkitRemoteObject> convertEvaluateOnCallFrameResult(JSONObject object)
      throws JSONException {
    WebkitResult<WebkitRemoteObject> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      object = object.getJSONObject("result");

      WebkitRemoteObject remoteObject = WebkitRemoteObject.createFrom(object.getJSONObject("result"));

      if (object.optBoolean("wasThrown", false)) {
        result.setError(remoteObject);
      } else {
        result.setResult(remoteObject);
      }
    }

    return result;
  }

  private WebkitResult<String> convertGetScriptSourceResult(JSONObject object) throws JSONException {
    WebkitResult<String> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(JsonUtils.getString(object.getJSONObject("result"), "scriptSource"));
    }

    return result;
  }

  private WebkitResult<String> convertSetBreakpointByUrlResult(JSONObject object,
      List<WebkitBreakpoint> resolvedBreakpoints) throws JSONException {
    // "result":{
    //   "locations":[{"lineNumber":9,"scriptId":"-1","columnNumber":0}],
    //   "breakpointId":"http://0.0.0.0:3030/webapp/webapp.dart:9:0"
    // }

    WebkitResult<String> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      JSONObject temp = object.getJSONObject("result");

      String breakpointId = temp.optString("breakpointId");

      result.setResult(breakpointId);

      if (temp.has("locations")) {
        JSONArray arr = temp.getJSONArray("locations");

        if (arr.length() > 0) {
          WebkitLocation location = WebkitLocation.createFrom(arr.getJSONObject(0));

          WebkitBreakpoint breakpoint = WebkitBreakpoint.createFrom(breakpointId, location);

          resolvedBreakpoints.add(breakpoint);
        }
      }
    }

    return result;
  }

  private WebkitResult<WebkitBreakpoint> convertSetBreakpointResult(JSONObject object)
      throws JSONException {
    // "result": {
    //   "breakpointId": <BreakpointId>,
    //   "actualLocation": <Location> 
    // }

    WebkitResult<WebkitBreakpoint> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      WebkitBreakpoint breakpoint = WebkitBreakpoint.createFromActual(object.getJSONObject("result"));

      result.setResult(breakpoint);
    }

    return result;
  }

  private WebkitResult<WebkitCallFrame[]> convertSetScriptSourceResult(JSONObject object)
      throws JSONException {
    WebkitResult<WebkitCallFrame[]> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      JSONObject obj = object.getJSONObject("result");

      if (obj.has("callFrames")) {
        List<WebkitCallFrame> frames = WebkitCallFrame.createFrom(obj.getJSONArray("callFrames"));

        result.setResult(frames.toArray(new WebkitCallFrame[frames.size()]));
      }
    }

    return result;
  }

  private void handleResumed() {
    clearRemoteObjects();
    classInfoMap.clear();
  }

  private void populateClassInfoMap(final WebkitRemoteObject classInfo) {
    final CountDownLatch latch = new CountDownLatch(1);

    try {
      getConnection().getRuntime().getProperties(
          classInfo,
          true,
          new WebkitCallback<WebkitPropertyDescriptor[]>() {
            @Override
            public void handleResult(WebkitResult<WebkitPropertyDescriptor[]> result) {
              // [[library,[string,dart:core]], [class,[string,TypeErrorImplementation]], [__proto__,[object,Object]]]

              if (!result.isError()) {
                for (WebkitPropertyDescriptor descriptor : result.getResult()) {
                  if (descriptor.getName().equals("class")) {
                    String className = descriptor.getValue().getValue();

                    classInfoMap.put(classInfo, className);
                  }
                }
              }

              latch.countDown();
            }
          });
    } catch (IOException ex) {
      latch.countDown();
    }

    try {
      latch.await();
    } catch (InterruptedException e) {

    }
  }

}
