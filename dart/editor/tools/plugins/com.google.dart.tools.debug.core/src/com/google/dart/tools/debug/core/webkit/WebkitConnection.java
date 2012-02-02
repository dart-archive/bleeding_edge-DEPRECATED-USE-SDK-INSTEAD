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

import de.roderick.weberknecht.WebSocket;
import de.roderick.weberknecht.WebSocketConnection;
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
 * A class to connect to and communicate with a Webkit Inspection Protocol server.
 * 
 * @see http://code.google.com/chrome/devtools/docs/protocol/tot/index.html
 */
public class WebkitConnection {

  public static interface WebkitConnectionListener {
    public void connectionClosed(WebkitConnection connection);
  }

  static interface Callback {
    public void handleResult(JSONObject result) throws JSONException;
  }

  static interface NotificationHandler {
    public void handleNotification(String method, JSONObject params) throws JSONException;
  }

  private URI webSocketUri;

  private WebSocket websocket;
  private boolean connected;

  private WebkitConsole console;
  private WebkitDebugger debugger;
  private WebkitPage page;

  private int requestId = 0;

  private Map<String, NotificationHandler> notificationHandlers = new HashMap<String, NotificationHandler>();
  private Map<Integer, Callback> callbackMap = new HashMap<Integer, Callback>();

  private List<WebkitConnectionListener> connectionListeners = new ArrayList<WebkitConnectionListener>();

  public WebkitConnection(String webSocketUri) {
    this(URI.create(webSocketUri));
  }

  public WebkitConnection(URI webSocketUri) {
    this.webSocketUri = webSocketUri;
  }

  public void addConnectionListener(WebkitConnectionListener listener) {
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
      websocket = new WebSocketConnection(webSocketUri);

      // Register Event Handlers
      websocket.setEventHandler(new WebSocketEventHandler() {
        @Override
        public void onClose() {
          websocket = null;

          notifyClosed();
        }

        @Override
        public void onMessage(WebSocketMessage message) {
          process(message);
        }

        @Override
        public void onOpen() {
          connected = true;
        }
      });

      websocket.connect();
    } catch (WebSocketException exception) {
      throw new IOException(exception);
    }
  }

  public WebkitConsole getConsole() {
    if (console == null) {
      console = new WebkitConsole(this);
    }

    return console;
  }

  public WebkitDebugger getDebugger() {
    if (debugger == null) {
      debugger = new WebkitDebugger(this);
    }

    return debugger;
  }

  public WebkitPage getPage() {
    if (page == null) {
      page = new WebkitPage(this);
    }

    return page;
  }

  public boolean isConnected() {
    return websocket != null && connected;
  }

  public void removeConnectionListener(WebkitConnectionListener listener) {
    connectionListeners.remove(listener);
  }

  protected void notifyClosed() {
    for (WebkitConnectionListener listener : connectionListeners) {
      listener.connectionClosed(this);
    }
  }

  protected void process(WebSocketMessage message) {
    try {
      JSONObject object = new JSONObject(message.getText());

      if (object.has("id")) {
        processResponse(object);
      } else {
        processNotification(object);
      }
    } catch (JSONException exception) {
      DartDebugCorePlugin.logError(exception);
    }
  }

  protected void registerNotificationHandler(String prefix, NotificationHandler handler) {
    notificationHandlers.put(prefix, handler);
  }

  protected void sendRequest(JSONObject request) throws IOException, JSONException {
    sendRequest(request, null);
  }

  protected void sendRequest(JSONObject request, Callback callback) throws IOException,
      JSONException {
    int id = getNextRequestId();

    request.put("id", id);

    try {
      if (callback != null) {
        callbackMap.put(id, callback);
      }

      websocket.send(request.toString());
    } catch (WebSocketException exception) {
      if (callback != null) {
        callbackMap.remove(id);
      }

      throw new IOException(exception);
    }
  }

  private int getNextRequestId() {
    return ++requestId;
  }

  private void processNotification(JSONObject object) throws JSONException {
    if (object.has("method")) {
      String method = object.getString("method");

      for (String prefix : notificationHandlers.keySet()) {
        if (method.startsWith(prefix)) {
          NotificationHandler handler = notificationHandlers.get(prefix);

          if (object.has("params")) {
            handler.handleNotification(method, object.getJSONObject("params"));
          } else {
            handler.handleNotification(method, null);
          }

          return;
        }
      }
    }

    DartDebugCorePlugin.logWarning("no handler for notification: " + object);
  }

  private void processResponse(JSONObject result) throws JSONException {
    try {
      int id = result.getInt("id");

      Callback callback = callbackMap.remove(id);

      if (callback != null) {
        callback.handleResult(result);
      }
    } catch (Throwable exception) {
      DartDebugCorePlugin.logError(exception);
    }
  }

}
