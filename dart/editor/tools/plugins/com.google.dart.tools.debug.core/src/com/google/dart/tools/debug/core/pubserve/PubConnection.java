/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.debug.core.pubserve;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import de.roderick.weberknecht.WebSocket;
import de.roderick.weberknecht.WebSocketEventHandler;
import de.roderick.weberknecht.WebSocketException;
import de.roderick.weberknecht.WebSocketMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A pub protocol websocket connection.
 */
public class PubConnection {
  public static interface PubConnectionListener {
    public void connectionClosed(PubConnection connection);
  }

  static interface Callback {
    public void handleResult(JSONObject result) throws JSONException;
  }

  private String host;
  private int port;
  private String webSocketFile;

  private URI webSocketUri;

  private WebSocket websocket;
  private boolean connected;
  private PubCommands commands;

  private List<PubConnectionListener> connectionListeners = new ArrayList<PubConnectionListener>();
  private Map<Integer, Callback> callbackMap = new HashMap<Integer, Callback>();

  private int requestId = 0;

  public PubConnection(String host, int port, String webSocketFile) {
    this.host = host;
    this.port = port;
    this.webSocketFile = webSocketFile;
  }

  public PubConnection(URI webSocketUri) {
    this.webSocketUri = webSocketUri;
  }

  public void addConnectionListener(PubConnectionListener listener) {
    connectionListeners.add(listener);
  }

  public void close() throws IOException {
    if (websocket != null) {
      try {
        websocket.close();
      } catch (WebSocketException exception) {
        throw new IOException(exception);
      } finally {
        websocket = null;
      }
    }
  }

  public void connect() throws IOException {
    try {
      if (webSocketUri != null) {
        websocket = new WebSocket(webSocketUri);
      } else {
        websocket = new WebSocket(host, port, webSocketFile);
      }

      // Register Event Handlers
      websocket.setEventHandler(new WebSocketEventHandler() {
        @Override
        public void onClose() {
          websocket = null;

          notifyClosed();
        }

        @Override
        public void onMessage(WebSocketMessage message) {
          processWebSocketMessage(message);
        }

        @Override
        public void onOpen() {
          connected = true;
        }

        @Override
        public void onPing() {
          // nothing to do

        }

        @Override
        public void onPong() {
          // nothing to do

        }
      });

      websocket.connect();
    } catch (WebSocketException exception) {
      throw new IOException(exception);
    } catch (Throwable exception) {
      // Defensively catch any programming errors from the weberknecht library.
      throw new IOException(exception);
    }
  }

  public PubCommands getCommands() {
    if (commands == null) {
      commands = new PubCommands(this);
    }
    return commands;
  }

  public boolean isConnected() {
    return websocket != null && connected;
  }

  public void removeConnectionListener(PubConnectionListener listener) {
    connectionListeners.remove(listener);
  }

  protected void notifyClosed() {
    for (PubConnectionListener listener : connectionListeners) {
      listener.connectionClosed(this);
    }

    // Clean up the callbackMap on termination.
    List<Callback> callbacks = new ArrayList<Callback>(callbackMap.values());

    for (Callback callback : callbacks) {
      try {
        callback.handleResult(PubResult.createJsonErrorResult("connection termination"));
      } catch (JSONException e) {

      }
    }

    callbackMap.clear();
  }

  protected void processWebSocketMessage(WebSocketMessage message) {
    try {
      JSONObject object = new JSONObject(message.getText());

      DartDebugCorePlugin.log("pub <== " + object);

      if (object.has("id")) {
        processResponse(object);
      } else {
        processNotification(object);
      }
    } catch (JSONException exception) {
      DartDebugCorePlugin.logError("Could not process message " + message.getText(), exception);
    }
  }

  protected void sendRequest(JSONObject request) throws IOException, JSONException {
    sendRequest(request, null);
  }

  protected void sendRequest(JSONObject request, Callback callback) throws IOException,
      JSONException {
    if (!isConnected()) {
      throw new IOException("connection terminated");
    }

    int id = 0;

    try {
      synchronized (this) {
        id = getNextRequestId();

        request.put("id", id);

        if (callback != null) {
          callbackMap.put(id, callback);
        }
      }

      DartDebugCorePlugin.log("pub ==> " + request);

      websocket.send(request.toString());
    } catch (WebSocketException exception) {
      if (callback != null) {
        synchronized (this) {
          callbackMap.remove(id);
        }
      }

      throw new IOException(exception);
    }
  }

  private int getNextRequestId() {
    return ++requestId;
  }

  private void processNotification(JSONObject object) {
    // TODO: pub does not yet send notifications

  }

  private void processResponse(JSONObject result) {
    try {
      int id = result.optInt("id", -1);

      Callback callback;

      synchronized (this) {
        callback = callbackMap.remove(id);
      }

      if (callback != null) {
        callback.handleResult(result);
      } else if (result.has("error")) {
        // If we get an error back, and nobody was listening for the result, then log it.
        PubResult<?> pubResult = PubResult.createFrom(result);

        DartDebugCorePlugin.logInfo("Error from command id " + id + ": " + pubResult.getError());
      }
    } catch (Throwable exception) {
      DartDebugCorePlugin.logError(exception);
    }
  }

}
