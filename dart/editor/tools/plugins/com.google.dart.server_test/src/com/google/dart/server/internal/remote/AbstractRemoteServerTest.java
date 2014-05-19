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

import com.google.dart.server.internal.remote.RemoteAnalysisServerImpl.ServerResponseReaderThread;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public abstract class AbstractRemoteServerTest extends TestCase {

  protected RemoteAnalysisServerImpl server;

  protected ServerResponseReaderThread readerThread;

  InputStream inputStream;

  protected void responseFromServer(String response) {
    inputStream = new ByteArrayInputStream(response.getBytes());
    readerThread = server.new ServerResponseReaderThread(inputStream);
    readerThread.start();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    server = new RemoteAnalysisServerImpl("", "");
  }

  @Override
  protected void tearDown() throws Exception {
    server.shutdown();
    server = null;
    super.tearDown();
  }
}
