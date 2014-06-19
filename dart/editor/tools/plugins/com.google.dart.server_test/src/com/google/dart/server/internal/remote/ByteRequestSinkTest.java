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

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import junit.framework.TestCase;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Test for {@link ByteRequestSink}.
 */
public class ByteRequestSinkTest extends TestCase {
  private ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

  public void test_add() throws Exception {
    ByteRequestSink requestSink = new ByteRequestSink(byteStream, null);
    JsonObject originalJsonObject = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'server.shutdown'",
        "}");
    // write
    requestSink.add(originalJsonObject);
    // restore and compare
    byte[] bytes = byteStream.toByteArray();
    String actualJsonString = new String(bytes, Charsets.UTF_8);
    JsonObject actualJsonObject = parseJson(actualJsonString);
    assertEquals(originalJsonObject, actualJsonObject);
  }

  public void test_add_debugStream() throws Exception {
    PrintStream debugStream = mock(PrintStream.class);
    ByteRequestSink requestSink = new ByteRequestSink(byteStream, debugStream);
    requestSink.add(parseJson("{}"));
    // verify
    verify(debugStream).println(anyString());
  }

  /**
   * Builds a JSON string from the given lines. Replaces single quotes with double quotes. Then
   * parses this string as a {@link JsonObject}.
   */
  private JsonObject parseJson(String... lines) {
    String json = Joiner.on('\n').join(lines);
    json = json.replace('\'', '"');
    return (JsonObject) new JsonParser().parse(json);
  }
}
