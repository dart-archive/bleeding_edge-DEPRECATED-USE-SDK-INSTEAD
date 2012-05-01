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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A low level interface to the Dart VM debugger protocol.
 */
public class VmConnection {
  private static Charset UTF8 = Charset.forName("UTF-8");

  private List<VmListener> listeners = new ArrayList<VmListener>();
  private int port;

  private Map<Integer, VmCallback> callbackMap = new HashMap<Integer, VmCallback>();

  private int nextCommandId = 1;

  private Socket socket;
  private OutputStream out;

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
          if (socket != null) {
            e.printStackTrace();
          }
        } finally {
          socket = null;
        }
      }
    }).start();
  }

  public boolean isConnected() {
    return socket != null;
  }

  public void removeListener(VmListener listener) {
    listeners.remove(listener);
  }

  public void sendRequest(JSONObject request, VmCallback callback) throws IOException {
    int id = 0;

    synchronized (this) {
      id = ++nextCommandId;

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
      send(request.toString().getBytes(UTF8));
    } catch (IOException ex) {
      if (callback != null) {
        synchronized (this) {
          callbackMap.remove(id);
        }
      }

      throw ex;
    }
  }

  protected void sendSimpleCommand(String command) throws IOException {
    sendSimpleCommand(command, null);
  }

  protected void sendSimpleCommand(String command, VmCallback callback) throws IOException {
    try {
      sendRequest(new JSONObject().put("method", command), callback);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  private void processJson(JSONObject result) {
    try {
      if (result.has("id")) {
        // Process a command result.
        int id = result.getInt("id");

        VmCallback callback;

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
      } else {
        // TODO(devoncarew): Process a vm notification.

      }
    } catch (Throwable exception) {
      DartDebugCorePlugin.logError(exception);
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
            return new JSONObject(builder.toString());
          } catch (JSONException e) {
            throw new IOException(e);
          }
        }
      }
    }
  }

  private void send(byte[] bytes) throws IOException {
    out.write(bytes);
    out.flush();
  }

}
