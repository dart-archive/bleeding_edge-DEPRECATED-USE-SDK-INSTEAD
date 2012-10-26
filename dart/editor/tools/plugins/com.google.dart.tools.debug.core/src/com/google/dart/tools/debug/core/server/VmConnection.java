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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.server.VmListener.PausedReason;
import com.google.dart.tools.debug.core.webkit.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * A low level interface to the Dart VM debugger protocol.
 */
public class VmConnection {

  public static enum BreakOnExceptionsType {
    all,
    none,
    unhandled
  }

  public static interface BreakpointResolvedCallback {
    public void handleResolved(VmBreakpoint bp);
  }

  static interface Callback {
    public void handleResult(JSONObject result) throws JSONException;
  }

  private static final String EVENT_ISOLATE = "isolate";
  private static final String EVENT_PAUSED = "paused";
  private static final String EVENT_BREAKPOINTRESOLVED = "breakpointResolved";

  private static Charset UTF8 = Charset.forName("UTF-8");

  /**
   * A set of core libraries - semantically considered part of the core Dart library implementation.
   */
  private static final Set<String> CORE_IMPL_LIBRARIES = new HashSet<String>(
      Arrays.asList(new String[] {"dart:core", "dart:coreimpl", "dart:nativewrappers"}));

  private List<VmListener> listeners = new ArrayList<VmListener>();
  private int port;

  private Map<Integer, Callback> callbackMap = new HashMap<Integer, Callback>();

  private int nextCommandId = 1;

  private Socket socket;
  private OutputStream out;

  private List<VmBreakpoint> breakpoints = Collections.synchronizedList(new ArrayList<VmBreakpoint>());

  private Map<String, String> sourceCache = new HashMap<String, String>();

  protected Map<Integer, BreakpointResolvedCallback> breakpointCallbackMap = new HashMap<Integer, VmConnection.BreakpointResolvedCallback>();

  private Map<Integer, VmIsolate> isolateMap = new HashMap<Integer, VmIsolate>();

  public VmConnection(int port) {
    this.port = port;
  }

  public void addListener(VmListener listener) {
    listeners.add(listener);
  }

  public void close() throws IOException {
    if (socket != null) {
      socket.close();
      socket = null;
    }
  }

  /**
   * Connect to the VM debug server.
   * 
   * @throws IOException
   */
  public void connect() throws IOException {
    socket = new Socket((String) null, port);

    out = socket.getOutputStream();
    final InputStream in = socket.getInputStream();

    // Start a reader thread.
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          processVmEvents(in);
        } catch (EOFException e) {

        } catch (SocketException se) {
          // ignore java.net.SocketException: Connection reset
          final String reset = "Connection reset";

          if (!(se.getMessage() != null && se.getMessage().contains(reset))) {
            DartDebugCorePlugin.logError(se);
          }
        } catch (IOException e) {
          DartDebugCorePlugin.logError(e);
        } finally {
          socket = null;
        }
      }
    }).start();
  }

  /**
   * Enable stepping for all libraries (except for certain core ones).
   * 
   * @throws IOException
   */
  public void enableAllStepping(final VmIsolate isolate) throws IOException {
    getLibraries(isolate, new VmCallback<List<VmLibraryRef>>() {
      @Override
      public void handleResult(VmResult<List<VmLibraryRef>> result) {
        if (!result.isError()) {
          for (VmLibraryRef ref : result.getResult()) {
            try {
              //if (ref.getUrl().startsWith("dart:")) {
              if (!CORE_IMPL_LIBRARIES.contains(ref.getUrl())) {
                setLibraryProperties(isolate, ref.getId(), true);
              }
            } catch (IOException e) {

            }
          }
        }
      }
    });
  }

  public List<VmBreakpoint> getBreakpoints() {
    return breakpoints;
  }

  public String getClassNameSync(VmObject obj) {
    if (obj.getClassId() == -1) {
      return "";
    }

    VmIsolate isolate = obj.getIsolate();

    if (!isolate.hasClassName(obj.getClassId())) {
      populateClassName(isolate, obj.getClassId());
    }

    return isolate.getClassName(obj.getClassId());
  }

  public void getClassProperties(final VmIsolate isolate, final int classId,
      final VmCallback<VmClass> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getClassProperties");
      request.put("params", new JSONObject().put("classId", classId));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          VmResult<VmClass> vmClassResult = convertGetClassPropertiesResult(
              isolate,
              classId,
              result);

          callback.handleResult(vmClassResult);
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void getGlobalVariables(final VmIsolate isolate, final int libraryId,
      final VmCallback<List<VmVariable>> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getGlobalVariables");
      request.put("params", new JSONObject().put("libraryId", libraryId));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          VmResult<List<VmVariable>> retValue = convertGetGlobalVariablesResult(isolate, result);

          callback.handleResult(retValue);
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void getLibraries(VmIsolate isolate, final VmCallback<List<VmLibraryRef>> callback)
      throws IOException {
    sendSimpleCommand("getLibraries", isolate.getId(), new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        callback.handleResult(convertGetLibrariesResult(result));
      }
    });
  }

  public void getLibraryProperties(final VmIsolate isolate, final int libraryId,
      final VmCallback<VmLibrary> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getLibraryProperties");
      request.put("params", new JSONObject().put("libraryId", libraryId));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          VmResult<VmLibrary> retValue = convertGetLibraryPropertiesResult(
              isolate,
              libraryId,
              result);

          callback.handleResult(retValue);
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void getListElements(final VmIsolate isolate, int listObjectId, int index,
      final VmCallback<VmValue> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getListElements");
      request.put("params", new JSONObject().put("objectId", listObjectId).put("index", index));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          VmResult<VmValue> vmObjectResult = convertGetListElementsResult(isolate, result);

          callback.handleResult(vmObjectResult);
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void getObjectProperties(final VmIsolate isolate, final int objectId,
      final VmCallback<VmObject> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getObjectProperties");
      request.put("params", new JSONObject().put("objectId", objectId));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          VmResult<VmObject> vmObjectResult = convertGetObjectPropertiesResult(
              isolate,
              objectId,
              result);

          callback.handleResult(vmObjectResult);
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * This synchronous, potentially long-running call returns the cached source for the given
   * libraryId and source url.
   * 
   * @param isolate
   * @param libraryId
   * @param url
   * @return
   */
  public String getScriptSource(VmIsolate isolate, final int libraryId, String url) {
    final String cacheKey = libraryId + ":" + url;

    if (!sourceCache.containsKey(cacheKey)) {
      final CountDownLatch latch = new CountDownLatch(1);

      try {
        getScriptSourceAsync(isolate, libraryId, url, new VmCallback<String>() {
          @Override
          public void handleResult(VmResult<String> result) {
            if (result.isError()) {
              sourceCache.put(cacheKey, null);
            } else {
              sourceCache.put(cacheKey, result.getResult());
            }

            latch.countDown();
          }
        });
      } catch (IOException e) {
        sourceCache.put(cacheKey, null);
        latch.countDown();
      }

      try {
        latch.await();
      } catch (InterruptedException e) {

      }
    }

    return sourceCache.get(cacheKey);
  }

//  /**
//   * This synchronous, potentially long-running call returns the cached source for the given script
//   * url, if any.
//   * 
//   * @param url
//   * @return
//   */
//  public String getScriptSource(final VmIsolate isolate, final String url) {
//    if (!sourceCache.containsKey(url)) {
//      try {
//        final CountDownLatch latch = new CountDownLatch(1);
//
//        getLibraries(isolate, new VmCallback<List<VmLibraryRef>>() {
//          @Override
//          public void handleResult(VmResult<List<VmLibraryRef>> result) {
//            if (result.isError()) {
//              sourceCache.put(url, null);
//              latch.countDown();
//            } else {
//              for (VmLibraryRef library : result.getResult()) {
//                if (url.equals(library.getUrl())) {
//                  try {
//                    getScriptSourceAsync(isolate, library.getId(), url, new VmCallback<String>() {
//                      @Override
//                      public void handleResult(VmResult<String> result) {
//                        if (result.isError()) {
//                          sourceCache.put(url, null);
//                        } else {
//                          sourceCache.put(url, result.getResult());
//                        }
//
//                        latch.countDown();
//                      }
//                    });
//                  } catch (IOException e) {
//                    sourceCache.put(url, null);
//                    latch.countDown();
//                  }
//
//                  return;
//                }
//              }
//
//              // No matches found.
//              sourceCache.put(url, null);
//              latch.countDown();
//            }
//          }
//        });
//
//        try {
//          latch.await();
//        } catch (InterruptedException e) {
//
//        }
//      } catch (IOException ioe) {
//        sourceCache.put(url, null);
//      }
//    }
//
//    return sourceCache.get(url);
//  }

  public void getScriptSourceAsync(VmIsolate isolate, int libraryId, String url,
      final VmCallback<String> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getScriptSource");
      request.put("params", new JSONObject().put("libraryId", libraryId).put("url", url));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetScriptSourceResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void getScriptURLs(VmIsolate isolate, int libraryId,
      final VmCallback<List<String>> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getScriptURLs");
      request.put("params", new JSONObject().put("libraryId", libraryId));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetScriptURLsResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void interrupt(VmIsolate isolate) throws IOException {
    sendSimpleCommand("interrupt", isolate.getId());
  }

  public boolean isConnected() {
    return socket != null;
  }

  public void removeBreakpoint(VmIsolate isolate, final VmBreakpoint breakpoint) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("command", "removeBreakpoint");
      request.put("params", new JSONObject().put("breakpointId", breakpoint.getBreakpointId()));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject object) throws JSONException {
          // Update the list of breakpoints based on the result code.
          VmResult<?> result = VmResult.createFrom(object);

          if (!result.isError()) {
            breakpoints.remove(breakpoint);
          }
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void removeListener(VmListener listener) {
    listeners.remove(listener);
  }

  /**
   * Remove the single, special system breakpoint at the first line of source in main().
   * 
   * @throws IOException
   */
  public void removeSystemBreakpoint(VmIsolate isolate) throws IOException {
    // TODO(devoncarew): This code will need to be updated if the VM no longer uses a user
    // breakpoint to stop on the first line of main().

    removeBreakpoint(isolate, new VmBreakpoint("", -1, 1));
  }

  public void resume(VmIsolate isolate) throws IOException {
    sendSimpleCommand("resume", isolate.getId(), resumeOnSuccess(isolate));
  }

  public void setBreakpoint(VmIsolate isolate, final String url, final int line,
      final BreakpointResolvedCallback callback) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("command", "setBreakpoint");
      request.put(
          "params",
          new JSONObject().put("url", VmUtils.eclipseUrlToVm(url)).put("line", line));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject object) throws JSONException {
          if (object.has("error")) {
            if (DartDebugCorePlugin.LOGGING) {
              System.out.println("    error setting breakpoint at " + url + ", " + line + ": "
                  + JsonUtils.getString(object, "error"));
            }
          } else {
            int breakpointId = JsonUtils.getInt(object.getJSONObject("result"), "breakpointId");

            VmBreakpoint bp = new VmBreakpoint(url, line, breakpointId);

            breakpoints.add(bp);

            if (callback != null) {
              //callback.handleResolved(bp);

              breakpointCallbackMap.put(breakpointId, callback);
            }
          }
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Set the given library's properties; currently this enables / disables stepping into the
   * library.
   * 
   * @param libraryId
   * @param debuggingEnabled
   * @throws IOException
   */
  public void setLibraryProperties(VmIsolate isolate, int libraryId, boolean debuggingEnabled)
      throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("command", "setLibraryProperties");
      request.put(
          "params",
          new JSONObject().put("libraryId", libraryId).put(
              "debuggingEnabled",
              Boolean.toString(debuggingEnabled)));

      sendRequest(request, isolate.getId(), null);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Set the VM to pause on exceptions.
   * 
   * @param kind
   * @throws IOException
   */
  public void setPauseOnException(VmIsolate isolate, BreakOnExceptionsType kind) throws IOException {
    // pauseOnException
    try {
      JSONObject request = new JSONObject();

      request.put("command", "setPauseOnException");
      request.put("params", new JSONObject().put("exceptions", kind.toString()));

      sendRequest(request, isolate.getId(), null);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void stepInto(VmIsolate isolate) throws IOException {
    sendSimpleCommand("stepInto", isolate.getId(), resumeOnSuccess(isolate));
  }

  public void stepOut(VmIsolate isolate) throws IOException {
    sendSimpleCommand("stepOut", isolate.getId(), resumeOnSuccess(isolate));
  }

  public void stepOver(VmIsolate isolate) throws IOException {
    sendSimpleCommand("stepOver", isolate.getId(), resumeOnSuccess(isolate));
  }

  protected void handleTerminated() {
    // Clean up the callbackMap on termination.
    List<Callback> callbacks = new ArrayList<VmConnection.Callback>(callbackMap.values());

    for (Callback callback : callbacks) {
      try {
        callback.handleResult(VmResult.createJsonErrorResult("connection termination"));
      } catch (JSONException e) {

      }
    }

    callbackMap.clear();
  }

  protected void processJson(JSONObject result) {
    try {
      if (result.has("id")) {
        processResponse(result);
      } else {
        processNotification(result);
      }
    } catch (Throwable exception) {
      DartDebugCorePlugin.logError(exception);
    }
  }

  protected void sendSimpleCommand(String command, int isolateId) throws IOException {
    sendSimpleCommand(command, isolateId, null);
  }

  protected void sendSimpleCommand(String command, int isolateId, Callback callback)
      throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("command", command);

      sendRequest(request, isolateId, callback);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  void sendRequest(JSONObject request, int isolateId, Callback callback) throws IOException {
    int id = 0;

    try {
      if (!request.has("params")) {
        request.put("params", new JSONObject());
      }

      JSONObject params = request.getJSONObject("params");

      if (!params.has("isolateId")) {
        params.put("isolateId", isolateId);
      }
    } catch (JSONException jse) {
      throw new IOException(jse);
    }

    synchronized (this) {
      id = nextCommandId++;

      try {
        request.put("id", id);
      } catch (JSONException ex) {
        throw new IOException(ex);
      }

      if (callback != null) {
        callbackMap.put(id, callback);
      }
    }

    try {
      send(request.toString());
    } catch (IOException ex) {
      if (callback != null) {
        synchronized (this) {
          callbackMap.remove(id);
        }
      }

      throw ex;
    }
  }

  private VmResult<VmClass> convertGetClassPropertiesResult(VmIsolate isolate, int classId,
      JSONObject object) throws JSONException {
    VmResult<VmClass> result = VmResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(VmClass.createFrom(isolate, object.getJSONObject("result")));
      result.getResult().setClassId(classId);
    }

    return result;
  }

  private VmResult<List<VmVariable>> convertGetGlobalVariablesResult(VmIsolate isolate,
      JSONObject object) throws JSONException {
    VmResult<List<VmVariable>> result = VmResult.createFrom(object);

    if (object.has("result")) {
      JSONObject jsonResult = object.getJSONObject("result");

      result.setResult(VmVariable.createFrom(isolate, jsonResult.optJSONArray("globals")));
    }

    return result;
  }

  private VmResult<List<VmLibraryRef>> convertGetLibrariesResult(JSONObject object)
      throws JSONException {
    VmResult<List<VmLibraryRef>> result = VmResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(VmLibraryRef.createFrom(object.getJSONObject("result").optJSONArray(
          "libraries")));
    }

    return result;
  }

  private VmResult<VmLibrary> convertGetLibraryPropertiesResult(VmIsolate isolate, int libraryId,
      JSONObject object) throws JSONException {
    VmResult<VmLibrary> result = VmResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(VmLibrary.createFrom(isolate, libraryId, object.getJSONObject("result")));
    }

    return result;
  }

  private VmResult<VmValue> convertGetListElementsResult(VmIsolate isolate, JSONObject object)
      throws JSONException {
    VmResult<VmValue> result = VmResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(VmValue.createFrom(isolate, object.getJSONObject("result")));
    }

    return result;
  }

  private VmResult<VmObject> convertGetObjectPropertiesResult(VmIsolate isolate, int objectId,
      JSONObject object) throws JSONException {
    VmResult<VmObject> result = VmResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(VmObject.createFrom(isolate, object.getJSONObject("result")));
      result.getResult().setObjectId(objectId);
    }

    return result;
  }

  private VmResult<String> convertGetScriptSourceResult(JSONObject object) throws JSONException {
    VmResult<String> result = VmResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(object.getJSONObject("result").getString("text"));
    }

    return result;
  }

  private VmResult<List<String>> convertGetScriptURLsResult(JSONObject object) throws JSONException {
    VmResult<List<String>> result = VmResult.createFrom(object);

    if (object.has("result")) {
      List<String> libUrls = new ArrayList<String>();

      JSONArray arr = object.getJSONObject("result").getJSONArray("urls");

      for (int i = 0; i < arr.length(); i++) {
        libUrls.add(VmUtils.vmUrlToEclipse(arr.getString(i)));
      }

      result.setResult(libUrls);
    }

    return result;
  }

  private VmIsolate getCreateIsolate(int isolateId) {
    if (isolateId == -1) {
      return null;
    }

    if (isolateMap.get(isolateId) == null) {
      isolateMap.put(isolateId, new VmIsolate(isolateId));
    }

    return isolateMap.get(isolateId);
  }

  private void handleBreakpointResolved(int breakpointId, String url, int line) {
    VmBreakpoint breakpoint = null;

    synchronized (breakpoints) {
      for (VmBreakpoint bp : breakpoints) {
        if (bp.getBreakpointId() == breakpointId) {
          breakpoint = bp;

          break;
        }
      }
    }

    if (breakpoint == null) {
      breakpoint = new VmBreakpoint(url, line, breakpointId);

      breakpoints.add(breakpoint);
    } else {
      breakpoint.updateInfo(url, line);

      BreakpointResolvedCallback callback = breakpointCallbackMap.get(breakpointId);

      if (callback != null) {
        breakpointCallbackMap.remove(breakpointId);

        callback.handleResolved(breakpoint);
      }
    }
  }

  private void notifyDebuggerResumed(VmIsolate isolate) {
    isolate.clearClassNameMap();

    for (VmListener listener : listeners) {
      listener.debuggerResumed(isolate);
    }
  }

  private void populateClassName(final VmIsolate isolate, final int classId) {
    final CountDownLatch latch = new CountDownLatch(1);

    try {
      getClassProperties(isolate, classId, new VmCallback<VmClass>() {
        @Override
        public void handleResult(VmResult<VmClass> result) {
          if (!result.isError()) {
            isolate.setClassName(classId, result.getResult().getName());
          }

          latch.countDown();
        }
      });
    } catch (IOException e1) {
      latch.countDown();
    }

    try {
      latch.await();
    } catch (InterruptedException e) {

    }
  }

  private void processNotification(JSONObject result) throws JSONException {
    if (result.has("event")) {
      String eventName = result.getString("event");
      JSONObject params = result.optJSONObject("params");

      if (eventName.equals(EVENT_PAUSED)) {
        // { "event": "paused", "params": { "callFrames" : [  { "functionName": "main" , "location": { "url": "file:///Users/devoncarew/tools/eclipse_37/eclipse/samples/time/time_server.dart", "lineNumber": 15 }}]}}

        String reason = params.optString("reason", null);
        int isolateId = params.optInt("id", -1);
        VmIsolate isolate = getCreateIsolate(isolateId);

        VmValue exception = VmValue.createFrom(isolate, params.optJSONObject("exception"));
        List<VmCallFrame> frames = VmCallFrame.createFrom(
            isolate,
            params.getJSONArray("callFrames"));

        for (VmListener listener : listeners) {
          listener.debuggerPaused(PausedReason.parse(reason), isolate, frames, exception);
        }
      } else if (eventName.equals(EVENT_BREAKPOINTRESOLVED)) {
        // { "event": "breakpointResolved", "params": {"breakpointId": 2, "url": "file:///Users/devoncarew/tools/eclipse_37/eclipse/samples/time/time_server.dart", "line": 19 }}

        int breakpointId = params.optInt("breakpointId");
        String url = VmUtils.vmUrlToEclipse(params.optString("url"));
        int line = params.optInt("line");

        handleBreakpointResolved(breakpointId, url, line);
      } else if (eventName.equals(EVENT_ISOLATE)) {
        // "{" event ":" isolate "," params ":" "{" reason ":" created "," id ":" Integer "}" "}"
        // "{" event ":" isolate "," params ":" "{" reason ":" shutdown "," id ":" Integer "}" "}"

        String reason = params.optString("reason", null);
        int isolateId = params.optInt("id", -1);

        VmIsolate isolate = getCreateIsolate(isolateId);

        if ("created".equals(reason)) {
          for (VmListener listener : listeners) {
            listener.isolateCreated(isolate);
          }
        } else if ("shutdown".equals(reason)) {
          for (VmListener listener : listeners) {
            listener.isolateShutdown(isolate);
          }

          isolateMap.remove(isolate.getId());
        }
      } else {
        DartDebugCorePlugin.logInfo("no handler for notification: " + eventName);
      }
    } else {
      DartDebugCorePlugin.logInfo("event not understood: " + result);
    }
  }

  private void processResponse(JSONObject result) throws JSONException {
    // Process a command response.
    int id = result.getInt("id");

    Callback callback;

    synchronized (this) {
      callback = callbackMap.remove(id);
    }

    if (callback != null) {
      callback.handleResult(result);
    } else if (result.has("error")) {
      // If we get an error back, and nobody was listening for the result, then log it.
      VmResult<?> vmResult = VmResult.createFrom(result);

      DartDebugCorePlugin.logInfo("Error from command id " + id + ": " + vmResult.getError());
    }
  }

  private void processVmEvents(InputStream in) throws IOException {
    Reader reader = new InputStreamReader(in, UTF8);

    JSONObject obj = readJson(reader);

    while (obj != null) {
      processJson(obj);

      obj = readJson(reader);
    }
  }

  private JSONObject readJson(Reader in) throws IOException {
    StringBuilder builder = new StringBuilder();

    boolean inQuote = false;
    boolean ignoreLast = false;
    int curlyCount = 0;

    int c = in.read();

    while (true) {
      if (c == -1) {
        throw new EOFException();
      }

      builder.append((char) c);

      if (!ignoreLast) {
        if (c == '"') {
          inQuote = !inQuote;
        }
      }

      if (inQuote && c == '\\') {
        ignoreLast = true;
      } else {
        ignoreLast = false;
      }

      if (!inQuote) {
        if (c == '{') {
          curlyCount++;
        } else if (c == '}') {
          curlyCount--;

          if (curlyCount == 0) {
            try {
              String str = builder.toString();

              if (DartDebugCorePlugin.LOGGING) {
                // Print the event / response from the VM.
                System.out.println("<== " + str);
              }

              // The VM doesn't escape newlines in toString values - our JSON parser chokes.
              // TODO(devoncarew): file a bug
              str = str.replace("\n", "\\n");

              return new JSONObject(str);
            } catch (JSONException e) {
              throw new IOException(e);
            }
          }
        }
      }

      c = in.read();
    }
  }

  private Callback resumeOnSuccess(final VmIsolate isolate) {
    return new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        VmResult<String> response = VmResult.createFrom(result);

        if (!response.isError()) {
          notifyDebuggerResumed(isolate);
        }
      }
    };
  }

  private void send(String str) throws IOException {
    if (DartDebugCorePlugin.LOGGING) {
      // Print the command to the VM.
      System.out.println("==> " + str);
    }

    byte[] bytes = str.getBytes(UTF8);

    out.write(bytes);
    out.flush();
  }

}
