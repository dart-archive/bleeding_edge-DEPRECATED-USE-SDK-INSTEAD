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
    String sdkPath = System.getProperty("com.google.dart.sdk");
    if (sdkPath == null) {
      fail("Add the dart sdk (com.google.dart.sdk) as a JVM argument");
      System.exit(1);
    }
    String svnRoot = System.getProperty("com.google.dart.svnRoot");
    if (svnRoot == null) {
      fail("Add the dart svnRoot (com.google.dart.svnRoot) as a JVM argument");
      System.exit(1);
    }
    String runtimePath = sdkPath + "/bin/dart";
    String analysisServerPath = svnRoot + "/pkg/analysis_server/bin/server.dart";
    StdioServerSocket serverSocket = new StdioServerSocket(
        runtimePath,
        analysisServerPath,
        null,
        false);
    // start the server interface
    server = new RemoteAnalysisServerImpl(serverSocket);
    server.start(0);
  }

  @Override
  protected void waitForAllServerResponses() throws Exception {
    ((RemoteAnalysisServerImpl) server).test_waitForWorkerComplete();
  }
}
