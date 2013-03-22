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

package com.google.dart.tools.debug.core.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * A light-weight URL / URLConnection replacement class that allows client to bind to a null host.
 * This is the same as binding to localhost / 127.0.0.1 / ::1, but it more reliable cross OS wrt
 * IPv4 and IPv6 issues.
 */
public class HttpUrlConnector {
  private static final String CRLF = "\r\n";

  private String host;
  private int port;
  private String reqFile;

  private InputStream in;

  private int statusCode = 200;
  private String statusText;

  private Map<String, String> headers = new HashMap<String, String>();

  public HttpUrlConnector(String host, int port, String reqFile) {
    this.host = host;
    this.port = port;
    this.reqFile = reqFile;
  }

  public int getContentLength() {
    String contentLength = headers.get("Content-Length");

    try {
      if (contentLength != null) {
        return Integer.parseInt(contentLength);
      }
    } catch (NumberFormatException nfe) {

    }

    return -1;
  }

  public InputStream getInputStream() throws IOException {
    connect();

    return in;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getStatusText() {
    return statusText;
  }

  private void connect() throws IOException {
    // host == null is a valid option
    Socket socket = new Socket(host, port);

    OutputStream out = socket.getOutputStream();
    InputStream socketIn = in = socket.getInputStream();
    DataInputStream dataInput = new DataInputStream(socketIn);

    //GET /json 1.0
    //

    out.write(("GET " + reqFile + " 1.0" + CRLF + CRLF).getBytes());
    out.flush();

    //HTTP/1.1 200 OK
    //Content-Type:application/json; charset=UTF-8
    //Content-Length:871
    //

    String line = dataInput.readLine();

    if (line == null) {
      throw new IOException();
    }

    parseHeaderLine(line);

    line = dataInput.readLine();

    while (line != null && line.length() > 0) {
      String[] strs = line.split(":");

      if (strs.length >= 2) {
        putHeaderField(strs[0], strs[1]);
      }

      line = dataInput.readLine();
    }

    if (getContentLength() != -1) {
      byte[] buffer = new byte[getContentLength()];

      dataInput.readFully(buffer);

      // We've read all the data out of it; close the stream.
      dataInput.close();

      this.in = new ByteArrayInputStream(buffer);
    } else {
      this.in = dataInput;
    }
  }

  private void parseHeaderLine(String line) {
    // HTTP/1.1 200 OK
    // HTTP/1.1 404 Not Found

    int index1 = line.indexOf(' ');

    if (index1 != -1) {
      int index2 = line.indexOf(' ', index1 + 1);

      if (index2 != -1) {
        statusText = line.substring(index2 + 1);

        try {
          statusCode = Integer.parseInt(line.substring(index1 + 1, index2));
        } catch (NumberFormatException nfe) {

        }
      }
    }
  }

  private void putHeaderField(String key, String value) {
    headers.put(key, value.trim());
  }

}
