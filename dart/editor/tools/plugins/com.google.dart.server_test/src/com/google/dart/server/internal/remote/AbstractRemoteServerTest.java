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

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.internal.remote.RemoteAnalysisServerImpl.ServerResponseReaderThread;
import com.google.dart.server.internal.shared.AbstractServerTest;

public abstract class AbstractRemoteServerTest extends AbstractServerTest {

  protected RemoteAnalysisServerImpl server;

  protected ServerResponseReaderThread readerThread;

  @Override
  protected AnalysisServer createServer() {
    server = new RemoteAnalysisServerImpl("", "");
    return server;
  }

  protected void responseFromServer(String response) {
    responseFromServer(new String[] {response});
  }

  protected void responseFromServer(String[] responses) {
    readerThread = server.new ServerResponseReaderThread(responses);
    readerThread.start();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Set a print writer so that we won't get a NPE when calls are made on the writer.
    server.test_setPrintWriter(new PrintStringWriter());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    server.test_setPrintWriter(null);
  }
}
