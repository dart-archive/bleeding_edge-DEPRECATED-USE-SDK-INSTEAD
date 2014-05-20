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

import com.google.common.base.Joiner;
import com.google.dart.server.VersionConsumer;
import com.google.dart.server.internal.integration.RemoteAnalysisServerImplIntegrationTest;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Unit tests for {@link RemoteAnalysisServerImpl}, for integration tests which actually uses the
 * remote server, see {@link RemoteAnalysisServerImplIntegrationTest}.
 */
public class RemoteAnalysisServerImplTest extends AbstractRemoteServerTest {

  public void test_version() throws Exception {
    server.test_putOnConsumerMap("id", new VersionConsumer() {
      @Override
      public void computedVersion(String version) {
        assertEquals("0.0.1", version);
      }
    });
    responseFromServer(parseJson(//
        "{",
        "  'id': 'id',",
        "  'result': {",
        "    'version': '0.0.1'",
        "  }",
        "}").toString());
  }

  /**
   * Builds a JSON string from the given lines. Replaces single quotes with double quotes. Then
   * parses this string as a {@link JsonElement}.
   */
  private JsonElement parseJson(String... lines) {
    String json = Joiner.on('\n').join(lines);
    json = json.replace('\'', '"');
    return new JsonParser().parse(json);
  }

}
