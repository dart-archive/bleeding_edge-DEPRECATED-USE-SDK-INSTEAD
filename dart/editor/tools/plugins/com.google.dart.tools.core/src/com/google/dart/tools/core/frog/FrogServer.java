/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core.frog;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.IPath;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Object managing interaction with a Dart compilation and analysis service running in a separate
 * process.
 */
public class FrogServer {
  private final class NullResponseHandler extends ResponseHandler {
    @Override
    public void response(ResponseObject response) throws IOException, JSONException {
      // ignored
    }
  }

  private final Object lock = new Object();
  private final Charset utf8Charset;
  private final Socket requestSocket;
  private final OutputStream requestStream;
  private final InputStream responseStream;
  private final Map<Integer, ResponseHandler> responseHandlers;
  private int nextRequestId = 0;

  public FrogServer(String host, int port) throws UnknownHostException, IOException {
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
        } catch (IOException e) {
          DartCore.logError("Failed to get response from frog server", e);
        }
        DartCore.logInformation("Exiting response processing thread");
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
  public void shutdown() {
    JSONObject request = new JSONObject();
    try {
      request.put("command", "close");
      sendRequest(request, null);
      responseStream.close();
      requestStream.close();
      requestSocket.close();
    } catch (Exception e) {
      DartCore.logError("Failed to close server connection", e);
    }
  }

  /**
   * Process responses from the frog server on a background thread.
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
      System.out.println(message);
      ResponseObject response;
      try {
        response = new ResponseObject(message);
        int id = response.getId();
        String kind = response.getKind();
        ResponseHandler handler;
        if (kind.equals("done")) {
          handler = responseHandlers.remove(id);
        } else {
          handler = responseHandlers.get(id);
        }
        if (handler != null) {
          handler.response(response);
        } else {
          DartCore.logError("Unknown handler for server response: " + message);
        }
      } catch (JSONException e) {
        throw new IOException("Exception handling response: " + message, e);
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
   * Send a request to the frog server.
   * 
   * @param message the message (not <code>null</code>)
   */
  private void sendMessage(String message) throws IOException {
    byte[] bytes = message.getBytes(utf8Charset);
    int len = bytes.length;
    requestStream.write((len >>> 24) & 0xFF);
    requestStream.write((len >>> 16) & 0xFF);
    requestStream.write((len >>> 8) & 0xFF);
    requestStream.write(len & 0xFF);
    requestStream.write(bytes);
    requestStream.flush();
  }

  /**
   * Send a request to the frog server after adding a unique "id" to the request
   * 
   * @param request the request (not <code>null</code>)
   * @param handler the response handler
   * @return the response handler (not <code>null</code>)
   */
  private ResponseHandler sendRequest(JSONObject request, ResponseHandler handler)
      throws IOException, JSONException {
    if (handler == null) {
      handler = new NullResponseHandler();
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
