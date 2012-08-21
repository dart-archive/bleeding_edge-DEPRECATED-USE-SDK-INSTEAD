/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.dart2js;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.IPath;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Object managing interaction with a Dart compilation and analysis service running in a separate
 * process.
 */
public class JsonServer {
  private final Object lock = new Object();
  private final Charset utf8Charset;
  private final Socket requestSocket;
  private final OutputStream requestStream;
  private final InputStream responseStream;
  private final Map<Integer, ResponseHandler> responseHandlers;
  private int nextRequestId = 0;

  public JsonServer(String host, int port) throws UnknownHostException, IOException {
    utf8Charset = Charset.forName("UTF-8");
    requestSocket = new Socket(host, port);
    requestStream = requestSocket.getOutputStream();
    responseStream = requestSocket.getInputStream();
    responseHandlers = new HashMap<Integer, ResponseHandler>();

    new Thread() {
      @Override
      public void run() {
        try {
          processResponses();
        } catch (SocketException exception) {
          // java.net.SocketException: Socket closed
          if (!exception.toString().contains(" closed")) {
//            if (DartCoreDebug.VERBOSE) {
//              DartCore.logError("Exception from JSON server", exception);
//            }
          }
        } catch (IOException exception) {
          // java.io.IOException: ...stream is closed
          if (!exception.toString().contains(" closed")) {
            DartCore.logError("Exception from JSON server", exception);
          }
        }
      };
    }.start();
  }

  public void compile(IPath inputPath, IPath outputPath, ResponseHandler handler)
      throws IOException {
    JSONObject request = new JSONObject();
    try {
      request.put("command", "compile");
      request.put("input", inputPath.toOSString());
      if (outputPath != null) {
        request.put("output", outputPath.toOSString());
      }
      sendRequest(request, handler);
    } catch (JSONException e) {
      throw new IOException("Failed to format request", e);
    }
  }

  /**
   * Close the socket and streams, signaling the external process to terminate
   */
  public void shutdown() throws IOException {
    JSONObject request = new JSONObject();
    try {
      request.put("command", "close");
      sendRequest(request, new ResponseHandler() {
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
    responseStream.close();
    requestStream.close();
    requestSocket.close();
  }

  /**
   * Process responses from the JSON server on a background thread.
   */
  private void processResponses() throws IOException {
    final byte[] buf = new byte[4];
    byte[] messageBuf = new byte[10];
    while (true) {
      readBytes(buf, 4);
      int messageLen = ((buf[0] & 0xFF) << 24) | ((buf[1] & 0xFF) << 16) | ((buf[2] & 0xFF) << 8)
          | (buf[3] & 0xFF);
      if (messageBuf.length < messageLen) {
        messageBuf = new byte[messageLen];
      }
      readBytes(messageBuf, messageLen);
      String message = new String(messageBuf, 0, messageLen, utf8Charset);
      Response response;
      int id;
      try {
        response = new Response(new JSONObject(message));
        id = response.getId();
      } catch (JSONException e) {
        DartCore.logError("Failed to parse server response: " + message, e);
        continue;
      }
      ResponseHandler handler = responseHandlers.get(id);
      if (handler == null) {
        DartCore.logError("Unknown handler for server response: " + message);
        continue;
      }
      if (handler.process(response)) {
        responseHandlers.remove(id);
      }
    }
  }

  /**
   * Read bytes from the response stream into the specified buffer. This blocks until either the
   * specified number of bytes have been received and placed into the buffer.
   * 
   * @param buffer the byte buffer (not <code>null</code>)
   * @param numBytes the number of bytes to receive
   */
  private void readBytes(final byte[] buffer, int numBytes) throws IOException {
    int start = 0;
    while (start < numBytes) {
      int count = responseStream.read(buffer, start, numBytes - start);
      if (count < 0) {
        throw new IOException("Failed to read response because stream is closed");
      }
      start += count;
    }
  }

  /**
   * Send a request to the JSON server.
   * 
   * @param message the message (not <code>null</code>)
   */
  private void sendMessage(String message) throws IOException {
    byte[] bytes = message.getBytes(utf8Charset);

    int len = bytes.length;

    byte[] temp = new byte[len + 4];
    System.arraycopy(bytes, 0, temp, 4, len);
    bytes = temp;

    bytes[0] = (byte) (len >>> 24);
    bytes[1] = (byte) (len >>> 16);
    bytes[2] = (byte) (len >>> 8);
    bytes[3] = (byte) len;

    // We send the bytes out in one write to decrease the chance that the receiving code will get
    // the data in multiple packets, and cause a false warning on the server side.
    requestStream.write(bytes);
    requestStream.flush();
  }

  /**
   * Send a request to the JSON server after adding a unique "id" to the request
   * 
   * @param request the request (not <code>null</code>)
   * @param handler the response handler
   * @return the response handler (not <code>null</code>)
   */
  private ResponseHandler sendRequest(JSONObject request, ResponseHandler handler)
      throws IOException, JSONException {
    if (handler == null) {
      throw new IllegalArgumentException("handler cannot be null");
    }
    synchronized (lock) {
      nextRequestId++;
      request.put("id", nextRequestId);
      responseHandlers.put(nextRequestId, handler);
    }
    sendMessage(request.toString());
    return handler;
  }

}
