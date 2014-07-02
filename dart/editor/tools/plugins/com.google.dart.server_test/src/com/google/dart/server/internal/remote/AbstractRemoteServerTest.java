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

import com.google.dart.server.AnalysisServer;
import com.google.dart.server.internal.AbstractServerTest;
import com.google.dart.server.internal.TestAnalysisServerListener;

public abstract class AbstractRemoteServerTest extends AbstractServerTest {
  protected RemoteAnalysisServerImpl server;
  protected TestAnalysisServerListener listener = new TestAnalysisServerListener();
  protected TestServerSocket socket = new TestServerSocket();
  protected TestRequestSink requestSink;
  protected TestResponseStream responseStream;

  @Override
  protected AnalysisServer createServer() throws Exception {
    server = new RemoteAnalysisServerImpl(socket);
    server.start(0);
    requestSink = socket.getRequestSink();
    responseStream = socket.getResponseStream();
    return server;
  }

  protected final void putResponse(String... lines) throws Exception {
    responseStream.put(lines);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    server.addAnalysisServerListener(listener);
  }

  @Override
  protected void tearDown() throws Exception {
    server.removeAnalysisServerListener(listener);
    server = null;
    listener = null;
    requestSink = null;
    responseStream = null;
    super.tearDown();
  }
}
