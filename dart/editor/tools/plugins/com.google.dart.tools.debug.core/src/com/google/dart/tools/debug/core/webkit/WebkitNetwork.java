/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.tools.debug.core.webkit.WebkitConnection.NotificationHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A WIP network domain object.
 * <p>
 * Network domain allows tracking network activities of the page. It exposes information about http,
 * file, data and other requests and responses, their headers, bodies, timing, etc.
 */
public class WebkitNetwork extends WebkitDomain {

  public static interface NetworkListener {

    public void webSocketClosed(String requestId, long timestamp);

    public void webSocketCreated(String requestId, String url);

    public void webSocketFrameError(String requestId, long timestamp, String errorMessage);

    public void webSocketFrameReceived(String requestId, long timestamp, WebSocketFrame frame);

    public void webSocketFrameSent(String requestId, long timestamp, WebSocketFrame frame);

    public void webSocketHandshakeResponseReceived(String requestId, long timestamp,
        WebSocketResponse response);

    public void webSocketWillSendHandshakeRequest(String requestId, long timestamp,
        WebSocketRequest request);

  }

  public static abstract class NetworkListenerAdapter implements NetworkListener {
    @Override
    public void webSocketClosed(String requestId, long timestamp) {
    }

    @Override
    public void webSocketCreated(String requestId, String url) {

    }

    @Override
    public void webSocketFrameError(String requestId, long timestamp, String errorMessage) {

    }

    @Override
    public void webSocketFrameReceived(String requestId, long timestamp, WebSocketFrame frame) {
    }

    @Override
    public void webSocketFrameSent(String requestId, long timestamp, WebSocketFrame frame) {

    }

    @Override
    public void webSocketHandshakeResponseReceived(String requestId, long timestamp,
        WebSocketResponse response) {

    }

    @Override
    public void webSocketWillSendHandshakeRequest(String requestId, long timestamp,
        WebSocketRequest request) {

    }
  }

  /**
   * WebSocket frame data.
   */
  @WebkitUnsupported
  public static class WebSocketFrame {
    public static WebSocketFrame createFrom(JSONObject obj) throws JSONException {
      WebSocketFrame frame = new WebSocketFrame();

      frame.opcode = obj.getInt("opcode");
      frame.mask = obj.getBoolean("mask");
      frame.payloadData = obj.getString("payloadData");

      return frame;
    }

    /** WebSocket frame opcode. */
    public int opcode;

    /** WebSocket frame mask. */
    public boolean mask;

    /** WebSocket frame payload data. */
    public String payloadData;
  }

  /**
   * WebSocket request data.
   */
  @WebkitUnsupported
  public static class WebSocketRequest {
    public static WebSocketRequest createFrom(JSONObject obj) throws JSONException {
      WebSocketRequest request = new WebSocketRequest();

      request.headers = obj.getJSONObject("headers");

      return request;
    }

    /** HTTP response headers, as key/values of a json object. */
    public JSONObject headers;
  }

  /**
   * WebSocket response data.
   */
  @WebkitUnsupported
  public static class WebSocketResponse {
    public static WebSocketResponse createFrom(JSONObject obj) throws JSONException {
      WebSocketResponse response = new WebSocketResponse();

      response.status = obj.getInt("status");
      response.statusText = obj.getString("statusText");
      response.headers = obj.getJSONObject("headers");

      return response;
    }

    /** HTTP response status code. */
    public int status;

    /** HTTP response status text. */
    public String statusText;

    /** HTTP response headers, as key/values of a json object. */
    public JSONObject headers;
  }

  private static final String WEB_SOCKET_CREATED = "Network.webSocketCreated";
  private static final String WEB_SOCKET_HANDSHAKE_REQUEST = "Network.webSocketWillSendHandshakeRequest";
  private static final String WEB_SOCKET_HANDSHAKE_RESPONSE = "Network.webSocketHandshakeResponseReceived";
  private static final String WEB_SOCKET_FRAME_SENT = "Network.webSocketFrameSent";
  private static final String WEB_SOCKET_FRAME_RECEIVED = "Network.webSocketFrameReceived";
  private static final String WEB_SOCKET_FRAME_ERROR = "Network.webSocketFrameError";
  private static final String WEB_SOCKET_CLOSED = "Network.webSocketClosed";

  private List<NetworkListener> listeners = new ArrayList<NetworkListener>();

  public WebkitNetwork(WebkitConnection connection) {
    super(connection);

    connection.registerNotificationHandler("Network.", new NotificationHandler() {
      @Override
      public void handleNotification(String method, JSONObject params) throws JSONException {
        handleNetworkNotification(method, params);
      }
    });
  }

  public void addNetworkListener(NetworkListener listener) {
    listeners.add(listener);
  }

  /**
   * Disables network tracking, prevents network events from being sent to the client.
   * 
   * @throws IOException
   */
  public void disable() throws IOException {
    sendSimpleCommand("Network.disable");
  }

  /**
   * Enables network tracking, network events will now be delivered to the client.
   * 
   * @throws IOException
   */
  public void enable() throws IOException {
    sendSimpleCommand("Network.enable");
  }

  public void removeNetworkListener(NetworkListener listener) {
    listeners.remove(listener);
  }

  protected void handleNetworkNotification(String method, JSONObject params) throws JSONException {
    if (method.equals(WEB_SOCKET_CREATED)) {
      String requestId = params.getString("requestId");
      String url = params.getString("url");

      for (NetworkListener listener : listeners) {
        listener.webSocketCreated(requestId, url);
      }
    } else if (method.equals(WEB_SOCKET_CLOSED)) {
      String requestId = params.getString("requestId");
      long timestamp = params.getLong("timestamp");

      for (NetworkListener listener : listeners) {
        listener.webSocketClosed(requestId, timestamp);
      }
    } else if (method.equals(WEB_SOCKET_HANDSHAKE_REQUEST)) {
      String requestId = params.getString("requestId");
      long timestamp = params.getLong("timestamp");
      WebSocketRequest request = WebSocketRequest.createFrom(params.getJSONObject("request"));

      for (NetworkListener listener : listeners) {
        listener.webSocketWillSendHandshakeRequest(requestId, timestamp, request);
      }
    } else if (method.equals(WEB_SOCKET_HANDSHAKE_RESPONSE)) {
      String requestId = params.getString("requestId");
      long timestamp = params.getLong("timestamp");
      WebSocketResponse response = WebSocketResponse.createFrom(params.getJSONObject("response"));

      for (NetworkListener listener : listeners) {
        listener.webSocketHandshakeResponseReceived(requestId, timestamp, response);
      }
    } else if (method.equals(WEB_SOCKET_FRAME_SENT)) {
      String requestId = params.getString("requestId");
      long timestamp = params.getLong("timestamp");
      WebSocketFrame frame = WebSocketFrame.createFrom(params.getJSONObject("response"));

      for (NetworkListener listener : listeners) {
        listener.webSocketFrameSent(requestId, timestamp, frame);
      }
    } else if (method.equals(WEB_SOCKET_FRAME_RECEIVED)) {
      String requestId = params.getString("requestId");
      long timestamp = params.getLong("timestamp");
      WebSocketFrame frame = WebSocketFrame.createFrom(params.getJSONObject("response"));

      for (NetworkListener listener : listeners) {
        listener.webSocketFrameReceived(requestId, timestamp, frame);
      }
    } else if (method.equals(WEB_SOCKET_FRAME_ERROR)) {
      String requestId = params.getString("requestId");
      long timestamp = params.getLong("timestamp");
      String errorMessage = params.getString("errorMessage");

      for (NetworkListener listener : listeners) {
        listener.webSocketFrameError(requestId, timestamp, errorMessage);
      }
    } else {
      // There are a lot of other network events - we ignore them for now.
      //DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

}
