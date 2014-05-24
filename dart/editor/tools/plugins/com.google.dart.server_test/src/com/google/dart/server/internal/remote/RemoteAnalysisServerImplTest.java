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

import com.google.dart.server.VersionConsumer;
import com.google.dart.server.internal.integration.RemoteAnalysisServerImplIntegrationTest;

/**
 * Unit tests for {@link RemoteAnalysisServerImpl}, for integration tests which actually uses the
 * remote server, see {@link RemoteAnalysisServerImplIntegrationTest}.
 */
public class RemoteAnalysisServerImplTest extends AbstractRemoteServerTest {

  public void test_getVersion() throws Exception {
    final String[] versionPtr = {null};
    server.getVersion(new VersionConsumer() {
      @Override
      public void computedVersion(String version) {
        versionPtr[0] = version;
      }
    });
    putResponse(//
        "{",
        "  'id': '0',",
        "  'result': {",
        "    'version': '0.0.1'",
        "  }",
        "}");
    server.test_waitForWorkerComplete();
    assertEquals("0.0.1", versionPtr[0]);
  }

  public void test_setOptions() throws Exception {
    // TODO(scheglov) restore or remove for the new API
//    server.setOptions("contextId", new AnalysisOptionsImpl());
//    responseFromServer(parseJson(//
//        "{",
//        "  'id': '0'",
//        "}").toString());
//    server.test_waitForWorkerComplete();
  }

  public void test_setPrioritySources() throws Exception {
    // TODO(scheglov) restore or remove for the new API
//    String contextId = "id";
//    Source source = addSource(contextId, "test.dart", makeSource(""));
//    server.setPrioritySources(contextId, new Source[] {source});
//    responseFromServer(parseJson(//
//        "{",
//        "  'id': '0'",
//        "}").toString());
//    server.test_waitForWorkerComplete();
  }

  public void test_shutdown() throws Exception {
    server.shutdown();
    putResponse(//
        "{",
        "  'id': '0'",
        "}");
    server.test_waitForWorkerComplete();
  }
}
