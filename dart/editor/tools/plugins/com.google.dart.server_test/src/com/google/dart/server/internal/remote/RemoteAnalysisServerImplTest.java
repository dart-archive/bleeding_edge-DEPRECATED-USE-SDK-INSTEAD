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

import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.server.ServerService;
import com.google.dart.server.VersionConsumer;
import com.google.dart.server.internal.integration.RemoteAnalysisServerImplIntegrationTest;

import java.util.ArrayList;

/**
 * Unit tests for {@link RemoteAnalysisServerImpl}, for integration tests which actually uses the
 * remote server, see {@link RemoteAnalysisServerImplIntegrationTest}.
 */
public class RemoteAnalysisServerImplTest extends AbstractRemoteServerTest {

  public void test_analysis_notification_errors() throws Exception {
    putResponse(//
        "{",
        "  'event': 'analysis.errors',",
        "  'params': {",
        "    'file': '/test.dart',",
        "    'errors' : [",
        "      {",
        "        'file': '/the/same/file.dart',",
        "        'errorCode': 'ParserErrorCode.ABSTRACT_CLASS_MEMBER',",
        "        'offset': 1,",
        "        'length': 2,",
        "        'message': 'message A',",
        "        'correction': 'correction A'",
        "      },",
        "      {",
        "        'file': '/the/same/file.dart',",
        "        'errorCode': 'CompileTimeErrorCode.AMBIGUOUS_EXPORT',",
        "        'offset': 10,",
        "        'length': 20,",
        "        'message': 'message B',",
        "        'correction': 'correction B'",
        "      }",
        "    ]",
        "  }",
        "}");
    responseStream.waitForEmpty();
    server.test_waitForWorkerComplete();
    listener.assertErrorsWithCodes(
        "/test.dart",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER,
        CompileTimeErrorCode.AMBIGUOUS_EXPORT);
  }

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

  public void test_setServerSubscriptions() throws Exception {
    server.setServerSubscriptions(new ArrayList<ServerService>());
    putResponse(//
        "{",
        "  'id': '0'",
        "}");
    server.test_waitForWorkerComplete();
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
