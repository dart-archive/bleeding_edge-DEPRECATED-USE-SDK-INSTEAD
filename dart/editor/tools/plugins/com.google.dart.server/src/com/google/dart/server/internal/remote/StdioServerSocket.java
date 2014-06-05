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
package com.google.dart.server.internal.remote;

import com.google.common.base.Preconditions;

/**
 * A remote server socket over standard input and output.
 * 
 * @coverage dart.server.remote
 */
public class StdioServerSocket {
  private final String runtimePath;
  private final String analysisServerPath;
  private RequestSink requestSink;
  private ResponseStream responseStream;
  private ByteLineReaderStream errorStream;

  public StdioServerSocket(String runtimePath, String analysisServerPath) {
    this.runtimePath = runtimePath;
    this.analysisServerPath = analysisServerPath;
  }

  /**
   * Return the error stream.
   */
  public ByteLineReaderStream getErrorStream() {
    Preconditions.checkNotNull(errorStream, "Server is not started.");
    return errorStream;
  }

  /**
   * Return the request sink.
   */
  public RequestSink getRequestSink() {
    Preconditions.checkNotNull(requestSink, "Server is not started.");
    return requestSink;
  }

  /**
   * Return the response stream.
   */
  public ResponseStream getResponseStream() {
    Preconditions.checkNotNull(responseStream, "Server is not started.");
    return responseStream;
  }

  /**
   * Start the remote server and initialize request sink and response stream.
   */
  public void start() throws Exception {
    ProcessBuilder processBuilder = new ProcessBuilder(runtimePath, analysisServerPath);
    Process process = processBuilder.start();
    requestSink = new ByteRequestSink(process.getOutputStream());
    responseStream = new ByteResponseStream(process.getInputStream());
    errorStream = new ByteLineReaderStream(process.getErrorStream());
  }
}
