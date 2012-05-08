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

/**
 * A low level interface to the Dart VM debugger protocol.
 */
public class VmConnection {

  static interface Callback {
    public void handleResult(JSONObject result) throws JSONException;
  }

  private static final String EVENT_PAUSED = "paused";

  private static Charset UTF8 = Charset.forName("UTF-8");

  private List<VmListener> listeners = new ArrayList<VmListener>();
  private int port;

  private Map<Integer, Callback> callbackMap = new HashMap<Integer, Callback>();

  private int nextCommandId = 1;

  private Socket socket;
  private OutputStream out;

  private List<VmBreakpoint> breakpoints = Collections.synchronizedList(new ArrayList<VmBreakpoint>());

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
        } catch (IOException e) {

        } finally {
          socket = null;
        }
      }
    }).start();
  }

  public List<VmBreakpoint> getBreakpoints() {
    return breakpoints;
  }

  public boolean isConnected() {
    return socket != null;
  }

  public void pause() throws IOException {
    sendSimpleCommand("pause");
  }

  public void removeBreakpoint(VmBreakpoint breakpoint) throws IOException {
    // TODO(devoncarew): implement this. make the VM call, update breakpoints based on the result code

  }

  public void removeListener(VmListener listener) {
    listeners.remove(listener);
  }

  public void resume() throws IOException {
    sendSimpleCommand("resume", new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        // TODO(devoncarew): right now the vm tells us a resume happened by responding w/ a success
        // result. We probably want to change this to an explicit resumed event.
        VmResult<String> response = VmResult.createFrom(result);

        if (!response.isError()) {
          for (VmListener listener : listeners) {
            listener.debuggerResumed();
          }
        }
      }
    });
  }

  public void setBreakpoint(final String url, final int line) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("command", "setBreakpoint");
      request.put("params", new JSONObject().put("url", url).put("line", line));

      sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject object) throws JSONException {
          if (object.has("error")) {
            String error = JsonUtils.getString(object, "error");

            DartDebugCorePlugin.logWarning("error setting breakpoint at " + url + ", " + line
                + ": " + error);
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

  private void handleNotification(String command, JSONObject result, JSONObject params)
      throws JSONException {
    if (command.equals(EVENT_PAUSED)) {
      List<VmCallFrame> frames = VmCallFrame.createFrom(params.getJSONArray("callFrames"));

      for (VmListener listener : listeners) {
        listener.debuggerPaused(frames);
      }
    } else {
      DartDebugCorePlugin.logInfo("no handler for notification: " + command);
    }
  }

  private void processNotification(JSONObject result) throws JSONException {
    if (result.has("command")) {
      String command = result.getString("command");

      if (result.has("params")) {
        handleNotification(command, result, result.getJSONObject("params"));
      } else {
        handleNotification(command, result, null);
      }
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
    int curlyCount = 0;

    int c = in.read();

    while (true) {
      if (c == -1) {
        throw new EOFException();
      }

      builder.append((char) c);

      if (c == '"') {
        inQuote = !inQuote;
      }

      if (c == '{' && !inQuote) {
        curlyCount++;
      } else if (c == '}' && !inQuote) {
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

      c = in.read();
    }
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
