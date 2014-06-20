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

import java.io.PrintStream;

/**
 * A remote server socket over standard input and output.
 * 
 * @coverage dart.server.remote
 */
public class StdioServerSocket {
  private final String runtimePath;
  private final String analysisServerPath;
  private final PrintStream debugStream;
  private RequestSink requestSink;
  private ResponseStream responseStream;
  private ByteLineReaderStream errorStream;
  private Process process;

  public StdioServerSocket(String runtimePath, String analysisServerPath, PrintStream debugStream) {
    this.runtimePath = runtimePath;
    this.analysisServerPath = analysisServerPath;
    this.debugStream = debugStream;
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
    process = processBuilder.start();
    requestSink = new ByteRequestSink(process.getOutputStream(), debugStream);
    responseStream = new ByteResponseStream(process.getInputStream(), debugStream);
    errorStream = new ByteLineReaderStream(process.getErrorStream());
  }

  /**
   * Wait up to 5 seconds for process to gracefully exit, then forcibly terminate the process if it
   * is still running.
   */
  public void stop() {
    if (process == null) {
      return;
    }
    final Process processToStop = process;
    process = null;
    long endTime = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < endTime) {
      try {
        int exit = processToStop.exitValue();
        if (exit != 0) {
          System.out.println("Non-zero exit code: " + exit + " for\n   " + analysisServerPath);
        }
        return;
      } catch (IllegalThreadStateException e) {
        //$FALL-THROUGH$
      }
      try {
        Thread.sleep(20);
      } catch (InterruptedException e) {
        //$FALL-THROUGH$
      }
    }
    processToStop.destroy();
    System.out.println("Terminated " + analysisServerPath);
  }
}
