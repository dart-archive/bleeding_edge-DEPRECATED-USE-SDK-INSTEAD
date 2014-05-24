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
package com.google.dart.server.internal.integration;

import com.google.dart.server.internal.remote.RemoteAnalysisServerImpl;
import com.google.dart.server.internal.remote.StdioServerSocket;

public class RemoteAnalysisServerImplIntegrationTest extends AbstractServerIntegrationTest {
  @Override
  protected void initServer() throws Exception {
    // prepare environment
    String runtimePath = System.getProperty("com.google.dart.runtime");
    String analysisServerPath = System.getProperty("com.google.dart.analysis.server");
    if (runtimePath == null) {
      fail("Add the dart runtime (com.google.dart.runtime) as a JVM argument");
    }
    if (analysisServerPath == null) {
      fail("Add the analysis server (com.google.dart.analysis.server) as a JVM argument");
    }
    // start the server process
    StdioServerSocket serverSocket = new StdioServerSocket(runtimePath, analysisServerPath);
    serverSocket.start();
    // start the server interface
    server = new RemoteAnalysisServerImpl(
        serverSocket.getRequestSink(),
        serverSocket.getResponseStream());
  }

  @Override
  protected void waitForAllServerResponses() throws Exception {
    ((RemoteAnalysisServerImpl) server).test_waitForWorkerComplete();
  }
}
