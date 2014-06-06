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
import de.roderick.weberknecht.WebSocketEventHandler;
import de.roderick.weberknecht.WebSocketException;
import de.roderick.weberknecht.WebSocketMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class to connect to and communicate with a Webkit Inspection Protocol server.
 * 
 * <pre>
    WebkitConnection connection = new WebkitConnection(ChromiumConnector.getWebSocketURLFor(port, 1));

    connection.addConnectionListener(new WebkitConnectionListener() {
      @Override
      public void connectionClosed(WebkitConnection connection) {
        System.out.println("connection closed");
      }
    });

    connection.connect();

    // add a console listener
    connection.getConsole().addConsoleListener(new ConsoleListener() {
      public void messageAdded(String message) {
        System.out.println("message added: " + message);
      }
      public void messageRepeatCountUpdated(int count) {

      }
      public void messagesCleared() {
        System.out.println("messages cleared");
      }
    });

    // enable console events
    connection.getConsole().enable();

    // navigate to cheese.com
    connection.getPage().navigate("http://www.cheese.com");
</pre>
 * 
 * @see http://code.google.com/chrome/devtools/docs/protocol/tot/index.html
 * @see http://trac.webkit.org/browser/trunk/Source/WebCore/inspector/Inspector.json
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

  private String host;
  private int port;
  private String webSocketFile;

  private WebSocket websocket;
  private boolean connected;

  private WebkitConsole console;
  private WebkitDebugger debugger;
  private WebkitPage page;
  private WebkitRuntime runtime;
  private WebkitCSS css;
  private WebkitDom dom;
  private WebkitDomDebugger domDebugger;
  private WebkitWorker worker;
  private WebkitNetwork network;
  private WebkitObservatory observatory;

  private int requestId = 0;

  private Map<String, NotificationHandler> notificationHandlers = new HashMap<String, NotificationHandler>();
  private Map<Integer, Callback> callbackMap = new HashMap<Integer, Callback>();

  private List<WebkitConnectionListener> connectionListeners = new ArrayList<WebkitConnectionListener>();

  public WebkitConnection(String host, int port, String webSocketFile) {
    this.host = host;
    this.port = port;
    this.webSocketFile = webSocketFile;
  }

  public WebkitConnection(URI webSocketUri) {
    this.webSocketUri = webSocketUri;
  }

  /**
   * A copy constructor for DartiumDebugTarget.
   * 
   * @param connection
   */
  public WebkitConnection(WebkitConnection connection) {
    this.host = connection.host;
    this.port = connection.port;
    this.webSocketFile = connection.webSocketFile;

    this.webSocketUri = connection.webSocketUri;
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

  public WebkitConsole getConsole() {
    if (console == null) {
      console = new WebkitConsole(this);
    }

    return console;
  }

  public WebkitCSS getCSS() {
    if (css == null) {
      css = new WebkitCSS(this);
    }

    return css;
  }

  public WebkitDebugger getDebugger() {
    if (debugger == null) {
      debugger = new WebkitDebugger(this);
    }

    return debugger;
  }

  public WebkitDom getDom() {
    if (dom == null) {
      dom = new WebkitDom(this);
    }

    return dom;
  }

  public WebkitDomDebugger getDomDebugger() {
    if (domDebugger == null) {
      domDebugger = new WebkitDomDebugger(this);
    }

    return domDebugger;
  }

  public WebkitNetwork getNetwork() {
    if (network == null) {
      network = new WebkitNetwork(this);
    }

    return network;
  }

  public WebkitObservatory getObservatory() {
    if (observatory == null) {
      observatory = new WebkitObservatory(this);
    }

    return observatory;
  }

  public WebkitPage getPage() {
    if (page == null) {
      page = new WebkitPage(this);
    }

    return page;
  }

  public WebkitRuntime getRuntime() {
    if (runtime == null) {
      runtime = new WebkitRuntime(this);
    }

    return runtime;
  }

  public URI getWebSocketUri() {
    if (webSocketUri != null) {
      return webSocketUri;
    } else {
      try {
        return new URI("ws", null, host, port, webSocketFile, null, null);
      } catch (URISyntaxException e) {
        return null;
      }
    }
  }

  public WebkitWorker getWorker() {
    if (worker == null) {
      worker = new WebkitWorker(this);
    }

    return worker;
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

    // Clean up the callbackMap on termination.
    List<Callback> callbacks = new ArrayList<Callback>(callbackMap.values());

    for (Callback callback : callbacks) {
      try {
        callback.handleResult(WebkitResult.createJsonErrorResult("connection termination"));
      } catch (JSONException e) {

      }
    }

    callbackMap.clear();
  }

  protected void processWebSocketMessage(WebSocketMessage message) {
    final int MAX_PRINT_LENGTH = 2000;

    try {
      String text = message.getText();

      if (text.length() > MAX_PRINT_LENGTH) {
        DartDebugCorePlugin.log("<== " + text.substring(0, MAX_PRINT_LENGTH) + "...");
        DartDebugCorePlugin.log("<== (long line: " + text.length() + " chars)");
      } else {
        DartDebugCorePlugin.log("<== " + text);
      }

      JSONObject object = new JSONObject(text);

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

      DartDebugCorePlugin.log("==> " + request);

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

  private void processNotification(JSONObject object) throws JSONException {
    // Two notifications we receive but don't do anything with:
    //   "Profiler.resetProfiles", "CSS.mediaQueryResultChanged"

    final String[] ignoreDomains = {"Profiler.", "Inspector."};

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

      for (String prefix : ignoreDomains) {
        if (method.startsWith(prefix)) {
          return;
        }
      }
    }

    DartDebugCorePlugin.logInfo("no handler for notification: " + object);
  }

  private void processResponse(JSONObject result) throws JSONException {
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
        WebkitResult<?> webkitResult = WebkitResult.createFrom(result);

        DartDebugCorePlugin.logInfo("Error from command id " + id + ": " + webkitResult.getError());
      }
    } catch (Throwable exception) {
      DartDebugCorePlugin.logError(exception);
    }
  }

}
