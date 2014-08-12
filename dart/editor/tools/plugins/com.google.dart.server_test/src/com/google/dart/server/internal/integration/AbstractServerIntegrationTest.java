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

import com.google.dart.server.AnalysisServer;

import junit.framework.TestCase;

public abstract class AbstractServerIntegrationTest extends TestCase {

  protected AnalysisServer server;

  //TODO(jwren) restore or remove for the new API
//  public void test_getVersion() throws Exception {
//    final String[] versionPtr = {null};
//    server.getVersion(new VersionConsumer() {
//      @Override
//      public void computedVersion(String version) {
//        versionPtr[0] = version;
//      }
//    });
//    waitForAllServerResponses();
//    assertEquals("0.0.1", versionPtr[0]);
//  }
//
//  public void test_getVersion2() throws Exception {
//    // This tests that when two responses are sent back at the same time, that the responses are
//    // handled appropriately.
//    // On the remote server side, this verifies that when two server responses happen on the input
//    // stream, that they are read correctly.
//    final String[] versionPtr1 = {null};
//    final String[] versionPtr2 = {null};
//    server.getVersion(new VersionConsumer() {
//      @Override
//      public void computedVersion(String version) {
//        versionPtr1[0] = version;
//      }
//    });
//    server.getVersion(new VersionConsumer() {
//      @Override
//      public void computedVersion(String version) {
//        versionPtr2[0] = version;
//      }
//    });
//    waitForAllServerResponses();
//    assertEquals("0.0.1", versionPtr1[0]);
//    assertEquals("0.0.1", versionPtr2[0]);
//  }

  // TODO(scheglov) restore or remove for the new API
//  public void test_setOptions() throws Exception {
//    server.setOptions("contextId", new AnalysisOptionsImpl());
//    waitForAllServerResponses();
//  }

  public void test_shutdown() throws Exception {
    server.server_shutdown();
  }

  protected abstract void initServer() throws Exception;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    initServer();
  }

  @Override
  protected void tearDown() throws Exception {
    server.server_shutdown();
    server = null;
    super.tearDown();
  }

  protected abstract void waitForAllServerResponses() throws Exception;
}
