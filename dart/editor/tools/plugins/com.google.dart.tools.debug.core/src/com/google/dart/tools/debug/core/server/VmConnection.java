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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
      Arrays.asList(new String[] {"dart:core", "dart:nativewrappers"}));

  private static ExecutorService threadPool = Executors.newCachedThreadPool();

  private List<VmListener> listeners = new ArrayList<VmListener>();

  private String host;
  private int port;

  private Map<Integer, Callback> callbackMap = new HashMap<Integer, Callback>();

  private int nextCommandId = 1;

  private Socket socket;
  private OutputStream out;

  private List<VmBreakpoint> breakpoints = Collections.synchronizedList(new ArrayList<VmBreakpoint>());

  private Map<String, String> sourceCache = new HashMap<String, String>();

  private Map<String, VmLineNumberTable> lineNumberTableCache = new HashMap<String, VmLineNumberTable>();

  protected Map<Integer, BreakpointResolvedCallback> breakpointCallbackMap = new HashMap<Integer, VmConnection.BreakpointResolvedCallback>();

  private Map<Integer, VmIsolate> isolateMap = new HashMap<Integer, VmIsolate>();

  public VmConnection(String host, int port) {
    this.host = host;
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
    socket = new Socket(host, port);

    out = socket.getOutputStream();
    final InputStream in = socket.getInputStream();

    // Start a reader thread.
    new Thread(new Runnable() {
      @Override
      public void run() {
        for (VmListener listener : listeners) {
          listener.connectionOpened(VmConnection.this);
        }

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

        for (VmListener listener : listeners) {
          listener.connectionClosed(VmConnection.this);
        }

        handleTerminated();
      }
    }).start();
  }

  /**
   * Enable stepping for all libraries (except for certain core ones).
   * 
   * @throws IOException
   */
  public void enableAllSteppingSync(final VmIsolate isolate) throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);

    getLibraries(isolate, new VmCallback<List<VmLibraryRef>>() {
      @Override
      public void handleResult(VmResult<List<VmLibraryRef>> result) {
        try {
          if (!result.isError()) {
            for (VmLibraryRef ref : result.getResult()) {
              try {
                if (!CORE_IMPL_LIBRARIES.contains(ref.getUrl())) {
                  setLibraryProperties(isolate, ref.getId(), true);
                }
              } catch (IOException e) {

              }
            }
          }
        } finally {
          latch.countDown();
        }
      }
    });

    try {
      latch.await();
    } catch (InterruptedException e) {

    }
  }

  public void evaluateGlobal(VmIsolate isolate, String expression,
      final VmCallback<VmValue> callback) {
    // TODO(devoncarew): call through to the VM implementation when available

    VmResult<VmValue> result = VmResult.createErrorResult("unimplemented");
    callback.handleResult(result);
  }

  public void evaluateObject(VmIsolate isolate, VmValue value, String expression,
      final VmCallback<VmValue> callback) throws IOException {
    // TODO(devoncarew): this mock implementation is _very_ temporary
    final String ex = expression.trim();

    getObjectProperties(isolate, value.getObjectId(), new VmCallback<VmObject>() {
      @Override
      public void handleResult(VmResult<VmObject> result) {
        for (VmVariable variable : result.getResult().getFields()) {
          if (variable.getName().equals(ex)) {
            VmResult<VmValue> returnValue = new VmResult<VmValue>();
            returnValue.setResult(variable.getValue());
            callback.handleResult(returnValue);
            return;
          }
        }

        VmResult<VmValue> returnValue = VmResult.createErrorResult("error parsing expression");
        callback.handleResult(returnValue);
      }
    });
  }

  public void evaluateOnCallFrame(VmIsolate isolate, VmCallFrame callFrame, String expression,
      final VmCallback<VmValue> callback) {
    // TODO(devoncarew): call through to the VM implementation when available

    VmResult<VmValue> result = VmResult.createErrorResult("unimplemented");
    callback.handleResult(result);
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

  public int getLineNumberFromLocation(VmIsolate isolate, VmLocation location) {
    String cacheKey = location.getLibraryId() + ":" + location.getUrl();

    if (!lineNumberTableCache.containsKey(cacheKey)) {
      final CountDownLatch latch = new CountDownLatch(1);
      final VmLineNumberTable[] result = new VmLineNumberTable[1];

      try {
        getLineNumberTable(
            isolate,
            location.getLibraryId(),
            location.getUrl(),
            new VmCallback<VmLineNumberTable>() {
              @Override
              public void handleResult(VmResult<VmLineNumberTable> r) {
                result[0] = r.getResult();

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

      lineNumberTableCache.put(cacheKey, result[0]);
    }

    VmLineNumberTable lineNumberTable = lineNumberTableCache.get(cacheKey);

    if (lineNumberTable == null) {
      return 0;
    } else {
      return lineNumberTable.getLineForLocation(location);
    }
  }

  public void getLineNumberTable(final VmIsolate isolate, final int libraryId,
      final String eclipseUrl, final VmCallback<VmLineNumberTable> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    final String vmUrl = VmUtils.eclipseUrlToVm(eclipseUrl);

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getLineNumberTable");
      request.put("params", new JSONObject().put("libraryId", libraryId).put("url", vmUrl));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          VmResult<VmLineNumberTable> vmObjectResult = convertGetLineNumberTableResult(
              isolate,
              libraryId,
              eclipseUrl,
              result);

          callback.handleResult(vmObjectResult);
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

  public void getStackTrace(final VmIsolate isolate, final VmCallback<List<VmCallFrame>> callback)
      throws IOException {
    sendSimpleCommand("getStackTrace", isolate.getId(), new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        callback.handleResult(convertGetStackTraceResult(isolate, result));
      }
    });
  }

  public void interrupt(VmIsolate isolate) throws IOException {
    sendSimpleCommand("interrupt", isolate.getId());
  }

  /**
   * @return whether the connection is still open
   */
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

  public void resume(VmIsolate isolate) throws IOException {
    sendSimpleCommand("resume", isolate.getId(), resumeOnSuccess(isolate));
  }

  /**
   * TODO(devoncarew): we use a synchronous version of this call to work around a deadlock issue in
   * the VM
   * 
   * @param isolate
   * @param url
   * @param line
   * @param callback
   * @throws IOException
   */
  public void setBreakpointSync(VmIsolate isolate, final String url, final int line,
      final BreakpointResolvedCallback callback) throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);

    try {
      JSONObject request = new JSONObject();

      request.put("command", "setBreakpoint");
      request.put(
          "params",
          new JSONObject().put("url", VmUtils.eclipseUrlToVm(url)).put("line", line));

      sendRequest(request, isolate.getId(), new Callback() {
        @Override
        public void handleResult(JSONObject object) throws JSONException {
          try {
            if (!object.has("error")) {
              int breakpointId = JsonUtils.getInt(object.getJSONObject("result"), "breakpointId");

              VmBreakpoint bp = new VmBreakpoint(url, line, breakpointId);

              breakpoints.add(bp);

              if (callback != null) {
                //callback.handleResolved(bp);

                breakpointCallbackMap.put(breakpointId, callback);
              }
            }
          } finally {
            latch.countDown();
          }
        }
      });

      try {
        latch.await();
      } catch (InterruptedException e) {

      }
    } catch (JSONException exception) {
      latch.countDown();

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
   * @param isolate
   * @param kind
   * @throws IOException
   */
  public void setPauseOnException(VmIsolate isolate, BreakOnExceptionsType kind) throws IOException {
    setPauseOnException(isolate, kind, null);
  }

  /**
   * Set the VM to pause on exceptions.
   * 
   * @param isolate
   * @param kind
   * @param callback
   * @throws IOException
   */
  public void setPauseOnException(VmIsolate isolate, BreakOnExceptionsType kind,
      final VmCallback<Boolean> callback) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("command", "setPauseOnException");
      request.put("params", new JSONObject().put("exceptions", kind.toString()));

      if (callback == null) {
        sendRequest(request, isolate.getId(), null);
      } else {
        sendRequest(request, isolate.getId(), new Callback() {
          @Override
          public void handleResult(JSONObject result) throws JSONException {
            VmResult<Boolean> callbackResult = VmResult.createFrom(result);
            callbackResult.setResult(true);
            callback.handleResult(callbackResult);
          }
        });
      }
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

  protected synchronized void handleTerminated() {
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

  protected void processJson(final JSONObject result) {
    threadPool.execute(new Runnable() {
      @Override
      public void run() {
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
    });
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
      if (!isConnected()) {
        if (callback != null) {
          callback.handleResult(VmResult.createJsonErrorResult("connection termination"));
        }

        return;
      }

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

  private VmResult<VmLineNumberTable> convertGetLineNumberTableResult(VmIsolate isolate,
      int libraryId, String url, JSONObject object) throws JSONException {
    VmResult<VmLineNumberTable> result = VmResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(VmLineNumberTable.createFrom(
          isolate,
          libraryId,
          url,
          object.getJSONObject("result")));
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

  private VmResult<List<VmCallFrame>> convertGetStackTraceResult(VmIsolate isolate,
      JSONObject object) throws JSONException {
    VmResult<List<VmCallFrame>> result = VmResult.createFrom(object);

    if (object.has("result")) {
      List<VmCallFrame> frames = VmCallFrame.createFrom(
          isolate,
          object.getJSONObject("result").getJSONArray("callFrames"));

      result.setResult(frames);
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
        int isolateId;
        if (params.has("isolateId")) {
          isolateId = params.optInt("isolateId", -1);
        } else {
          isolateId = params.optInt("id", -1);
        }

        String reason = params.optString("reason", null);
        VmIsolate isolate = getCreateIsolate(isolateId);
        VmValue exception = VmValue.createFrom(isolate, params.optJSONObject("exception"));
        VmLocation location = VmLocation.createFrom(isolate, params.optJSONObject("location"));

        sendDelayedDebuggerPaused(PausedReason.parse(reason), isolate, location, exception);
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

              // TODO(devoncarew): we know this is occurring for exception text.
              // Possibly from toString() invocations?
              if (str.indexOf('\n') != -1) {
                DartDebugCorePlugin.logError("bad json from vm: " + str);

                str = str.replace("\n", "\\n");
              }

              if (DartDebugCorePlugin.LOGGING) {
                // Print the event / response from the VM.
                System.out.println("<== " + str);
              }

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

  private void sendDelayedDebuggerPaused(final PausedReason reason, final VmIsolate isolate,
      final VmLocation location, final VmValue exception) throws JSONException {
    try {
      getStackTrace(isolate, new VmCallback<List<VmCallFrame>>() {
        @Override
        public void handleResult(VmResult<List<VmCallFrame>> result) {
          if (result.isError()) {
            DartDebugCorePlugin.logError(result.getError());
          } else {
            List<VmCallFrame> frames = result.getResult();;

            for (VmListener listener : listeners) {
              listener.debuggerPaused(reason, isolate, frames, exception);
            }
          }
        }
      });
    } catch (IOException e) {
      throw new JSONException(e);
    }
  }

}
