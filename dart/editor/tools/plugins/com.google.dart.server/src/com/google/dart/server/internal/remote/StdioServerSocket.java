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
import com.google.dart.server.AnalysisServerSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * A remote server socket over standard input and output.
 * 
 * @coverage dart.server.remote
 */
public class StdioServerSocket implements AnalysisServerSocket {
  /**
   * Find and return an unused server socket port.
   */
  public static int findUnusedPort() {
    try {
      ServerSocket ss = new ServerSocket(0);
      int port = ss.getLocalPort();
      ss.close();
      return port;
    } catch (IOException ioe) {
      //$FALL-THROUGH$
    }
    return -1;
  }

  private final String runtimePath;
  private final String analysisServerPath;
  private final DebugPrintStream debugStream;
  private final boolean debugRemoteProcess;
  private final boolean profileRemoteProcess;
  private int httpPort;
  private RequestSink requestSink;
  private ResponseStream responseStream;
  private ByteLineReaderStream errorStream;
  private Process process;
  private final String[] additionalProgramArguments;

  /**
   * If non-null, the package root that should be provided to Dart when running the analysis server.
   */
  private final String packageRoot;

  /**
   * Boolean used to have the <code>--no-error-notification</code> which disables all error
   * notifications from the server.
   */
  private final boolean noErrorNotification;

  public StdioServerSocket(String runtimePath, String analysisServerPath, String packageRoot,
      DebugPrintStream debugStream, boolean debugRemoteProcess, boolean profileRemoteProcess,
      int httpPort) {
    this(
        runtimePath,
        analysisServerPath,
        packageRoot,
        debugStream,
        new String[] {},
        debugRemoteProcess,
        profileRemoteProcess,
        httpPort,
        false);
  }

  public StdioServerSocket(String runtimePath, String analysisServerPath, String packageRoot,
      DebugPrintStream debugStream, String[] additionalProgramArguments,
      boolean debugRemoteProcess, boolean profileRemoteProcess, int httpPort,
      boolean noErrorNotification) {
    this.runtimePath = runtimePath;
    this.analysisServerPath = analysisServerPath;
    this.packageRoot = packageRoot;
    this.debugStream = debugStream;
    this.additionalProgramArguments = additionalProgramArguments;
    this.debugRemoteProcess = debugRemoteProcess;
    this.profileRemoteProcess = profileRemoteProcess;
    this.httpPort = httpPort;
    this.noErrorNotification = noErrorNotification;
  }

  @Override
  public ByteLineReaderStream getErrorStream() {
    Preconditions.checkNotNull(errorStream, "Server is not started.");
    return errorStream;
  }

  @Override
  public RequestSink getRequestSink() {
    Preconditions.checkNotNull(requestSink, "Server is not started.");
    return requestSink;
  }

  @Override
  public ResponseStream getResponseStream() {
    Preconditions.checkNotNull(responseStream, "Server is not started.");
    return responseStream;
  }

  @Override
  public void start() throws Exception {
    int debugPort = findUnusedPort();
    List<String> args = new ArrayList<String>();
    args.add(runtimePath);
    if (packageRoot != null) {
      args.add("--package-root=" + packageRoot);
    }
    if (debugRemoteProcess) {
      args.add("--debug:" + debugPort);
    }
    if (profileRemoteProcess) {
      args.add("--observe");
      args.add("--pause-isolates-on-exit");
    }
    if (noErrorNotification) {
      args.add("--no-error-notification");
    }
    args.add(analysisServerPath);
    if (httpPort != 0) {
      args.add("--port=" + httpPort);
    }
    for (String arg : additionalProgramArguments) {
      args.add(arg);
    }
    String[] arguments = args.toArray(new String[args.size()]);
    if (debugStream != null) {
      StringBuilder builder = new StringBuilder();
      builder.append("  ");
      int count = arguments.length;
      for (int i = 0; i < count; i++) {
        if (i > 0) {
          builder.append(' ');
        }
        builder.append(arguments[i]);
      }
      debugStream.println(System.currentTimeMillis() + " started analysis server:");
      debugStream.println(builder.toString());
    }
    ProcessBuilder processBuilder = new ProcessBuilder(arguments);
    process = processBuilder.start();
    requestSink = new ByteRequestSink(process.getOutputStream(), debugStream);
    responseStream = new ByteResponseStream(process.getInputStream(), debugStream);
    errorStream = new ByteLineReaderStream(process.getErrorStream());
    if (debugRemoteProcess) {
      System.out.println("Analysis server debug port " + debugPort);
    }
    if (httpPort != 0) {
      System.out.println("Analysis server http port " + httpPort);
    }
  }

  /**
   * Wait up to 5 seconds for process to gracefully exit, then forcibly terminate the process if it
   * is still running.
   */
  @Override
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
