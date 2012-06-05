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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

// TODO(devoncarew): implement getLibraryProperties

/**
 * A low level interface to the Dart VM debugger protocol.
 */
public class VmConnection {

  static interface Callback {
    public void handleResult(JSONObject result) throws JSONException;
  }

  private static final String EVENT_PAUSED = "paused";
  private static final String EVENT_BREAKPOINTRESOLVED = "breakpointResolved";

  private static Charset UTF8 = Charset.forName("UTF-8");

  private List<VmListener> listeners = new ArrayList<VmListener>();
  private int port;

  private Map<Integer, Callback> callbackMap = new HashMap<Integer, Callback>();

  private int nextCommandId = 1;

  private Socket socket;
  private OutputStream out;

  private List<VmBreakpoint> breakpoints = Collections.synchronizedList(new ArrayList<VmBreakpoint>());

  private Map<String, String> sourceCache = new HashMap<String, String>();

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
   * Connect to the VM debug server; wait a max of timeoutMs for a good debug connection.
   * 
   * @param timeoutMs
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

        } catch (IOException e) {
          DartDebugCorePlugin.logError(e);
        } finally {
          socket = null;
        }
      }
    }).start();
  }

  public List<VmBreakpoint> getBreakpoints() {
    return breakpoints;
  }

  public void getClassProperties(final int classId, final VmCallback<VmClass> callback)
      throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getClassProperties");
      request.put("params", new JSONObject().put("classId", classId));

      sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetClassPropertiesResult(classId, result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void getLibraries(final VmCallback<List<VmLibrary>> callback) throws IOException {
    sendSimpleCommand("getLibraries", new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        callback.handleResult(convertGetLibrariesResult(result));
      }
    });
  }

  public void getObjectProperties(final int objectId, final VmCallback<VmObject> callback)
      throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getObjectProperties");
      request.put("params", new JSONObject().put("objectId", objectId));

      sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetObjectPropertiesResult(objectId, result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void getScriptSource(int libraryId, String url, final VmCallback<String> callback)
      throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getScriptSource");
      request.put("params", new JSONObject().put("libraryId", libraryId).put("url", url));

      sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetScriptSourceResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * This synchronous, potentially long-running call returns the cached source for the given script
   * url, if any.
   * 
   * @param url
   * @return
   */
  public String getScriptSource(final String url) {
    if (!sourceCache.containsKey(url)) {
      try {
        final CountDownLatch latch = new CountDownLatch(1);

        getLibraries(new VmCallback<List<VmLibrary>>() {
          @Override
          public void handleResult(VmResult<List<VmLibrary>> result) {
            if (result.isError()) {
              sourceCache.put(url, null);
              latch.countDown();
            } else {
              for (VmLibrary library : result.getResult()) {
                if (url.equals(library.getUrl())) {
                  try {
                    getScriptSource(library.getId(), url, new VmCallback<String>() {
                      @Override
                      public void handleResult(VmResult<String> result) {
                        if (result.isError()) {
                          sourceCache.put(url, null);
                          latch.countDown();
                        } else {
                          sourceCache.put(url, result.getResult());
                          latch.countDown();
                        }
                      }
                    });
                  } catch (IOException e) {
                    sourceCache.put(url, null);
                    latch.countDown();
                  }

                  return;
                }
              }

              // No matches found.
              sourceCache.put(url, null);
              latch.countDown();
            }
          }
        });

        try {
          latch.await();
        } catch (InterruptedException e) {

        }
      } catch (IOException ioe) {
        sourceCache.put(url, null);
      }
    }

    return sourceCache.get(url);
  }

  public void getScriptURLs(int libraryId, final VmCallback<List<String>> callback)
      throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("command", "getScriptURLs");
      request.put("params", new JSONObject().put("libraryId", libraryId));

      sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetScriptURLsResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void getStackTrace(final VmCallback<List<VmCallFrame>> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("a callback is required");
    }

    sendSimpleCommand("getStackTrace", new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        callback.handleResult(convertGetStackTraceResult(result));
      }
    });
  }

  public boolean isConnected() {
    return socket != null;
  }

  public void pause() throws IOException {
    sendSimpleCommand("pause");
  }

  public void removeBreakpoint(final VmBreakpoint breakpoint) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("command", "removeBreakpoint");
      request.put("params", new JSONObject().put("breakpointId", breakpoint.getBreakpointId()));

      sendRequest(request, new Callback() {
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

  public void resume() throws IOException {
    sendSimpleCommand("resume", resumeOnSuccess());
  }

  public void setBreakpoint(final String url, final int line) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("command", "setBreakpoint");
      request.put(
          "params",
          new JSONObject().put("url", VmUtils.eclipseUrlToVm(url)).put("line", line));

      sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject object) throws JSONException {
          if (object.has("error")) {
            DartDebugCorePlugin.logInfo("error setting breakpoint at " + url + ", " + line + ": "
                + JsonUtils.getString(object, "error"));
          } else {
            int breakpointId = JsonUtils.getInt(object.getJSONObject("result"), "breakpointId");

            breakpoints.add(new VmBreakpoint(url, line, breakpointId));
          }
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void stepInto() throws IOException {
    sendSimpleCommand("stepInto", resumeOnSuccess());
  }

  public void stepOut() throws IOException {
    sendSimpleCommand("stepOut", resumeOnSuccess());
  }

  public void stepOver() throws IOException {
    sendSimpleCommand("stepOver", resumeOnSuccess());
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

  protected void sendSimpleCommand(String command) throws IOException {
    sendSimpleCommand(command, null);
  }

  protected void sendSimpleCommand(String command, Callback callback) throws IOException {
    try {
      sendRequest(new JSONObject().put("command", command), callback);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  void sendRequest(JSONObject request, Callback callback) throws IOException {
    int id = 0;

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

  private VmResult<VmClass> convertGetClassPropertiesResult(int classId, JSONObject object)
      throws JSONException {
    VmResult<VmClass> result = VmResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(VmClass.createFrom(object.getJSONObject("result")));
      result.getResult().setClassId(classId);
    }

    return result;
  }

  private VmResult<List<VmLibrary>> convertGetLibrariesResult(JSONObject object)
      throws JSONException {
    VmResult<List<VmLibrary>> result = VmResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(VmLibrary.createFrom(object.getJSONObject("result").optJSONArray("libraries")));
    }

    return result;
  }

  private VmResult<VmObject> convertGetObjectPropertiesResult(int objectId, JSONObject object)
      throws JSONException {
    VmResult<VmObject> result = VmResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(VmObject.createFrom(object.getJSONObject("result")));
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

  private VmResult<List<VmCallFrame>> convertGetStackTraceResult(JSONObject object)
      throws JSONException {
    VmResult<List<VmCallFrame>> result = VmResult.createFrom(object);

    if (object.has("result")) {
      List<VmCallFrame> frames = VmCallFrame.createFrom(object.getJSONArray("result"));

      result.setResult(frames);
    }

    return result;
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

    // TODO(devoncarew): notify anyone?
    if (breakpoint == null) {
      VmBreakpoint bp = new VmBreakpoint(url, line, breakpointId);

      breakpoints.add(bp);
    } else {
      // TODO(devoncarew): ensure that line == bp.line?

    }
  }

  private void processNotification(JSONObject result) throws JSONException {
    if (result.has("event")) {
      String eventName = result.getString("event");
      JSONObject params = result.optJSONObject("params");

      if (eventName.equals(EVENT_PAUSED)) {
        // { "event": "paused", "params": { "callFrames" : [  { "functionName": "main" , "location": { "url": "file:///Users/devoncarew/tools/eclipse_37/eclipse/samples/time/time_server.dart", "lineNumber": 15 }}]}}

        List<VmCallFrame> frames = VmCallFrame.createFrom(params.getJSONArray("callFrames"));

        for (VmListener listener : listeners) {
          listener.debuggerPaused(frames);
        }
      } else if (eventName.equals(EVENT_BREAKPOINTRESOLVED)) {
        // { "event": "breakpointResolved", "params": {"breakpointId": 2, "url": "file:///Users/devoncarew/tools/eclipse_37/eclipse/samples/time/time_server.dart", "line": 19 }}

        int breakpointId = params.optInt("breakpointId");
        String url = VmUtils.vmUrlToEclipse(params.optString("url"));
        int line = params.optInt("line");

        handleBreakpointResolved(breakpointId, url, line);
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

  private Callback resumeOnSuccess() {
    return new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        VmResult<String> response = VmResult.createFrom(result);

        if (!response.isError()) {
          for (VmListener listener : listeners) {
            listener.debuggerResumed();
          }
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
