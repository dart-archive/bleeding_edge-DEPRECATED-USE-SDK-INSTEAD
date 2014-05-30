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
package com.google.dart.server.internal.remote.processor;

import com.google.common.base.Joiner;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.HighlightType;
import com.google.dart.server.internal.shared.TestAnalysisServerListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;

public class NotificationHighlightsProcessorTest extends TestCase {
  private TestAnalysisServerListener listener = new TestAnalysisServerListener();
  private NotificationHighlightsProcessor processor = new NotificationHighlightsProcessor(listener);

  public void test_OK() throws Exception {
    processor.process(parseJson(//
        "{",
        "  'event': 'analysis.highlights',",
        "  'params': {",
        "    'file': '/my/file.dart',",
        "    'regions' : [",
        "      {",
        "        'type': 'CLASS',",
        "        'offset': 1,",
        "        'length': 2",
        "      },",
        "      {",
        "        'type': 'FIELD',",
        "        'offset': 10,",
        "        'length': 20",
        "      }",
        "    ]",
        "  }",
        "}"));
    HighlightRegion[] regions = listener.getHighlightRegions("/my/file.dart");
    assertThat(regions).hasSize(2);
    {
      HighlightRegion error = regions[0];
      assertSame(HighlightType.CLASS, error.getType());
      assertEquals(1, error.getOffset());
      assertEquals(2, error.getLength());
    }
    {
      HighlightRegion error = regions[1];
      assertSame(HighlightType.FIELD, error.getType());
      assertEquals(10, error.getOffset());
      assertEquals(20, error.getLength());
    }
  }

  /**
   * Builds a {@link JsonObject} from the given lines.
   */
  private JsonObject parseJson(String... lines) {
    String json = Joiner.on('\n').join(lines);
    json = json.replace('\'', '"');
    return new JsonParser().parse(json).getAsJsonObject();
  }
}
