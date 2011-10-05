/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.util;

import junit.framework.TestCase;

public class JSONErrorMessageParserTest extends TestCase {

  public void test_JSONErrorMessageParser_atCharacter() {
    testParser("foo at character 34", 34, "foo");
  }

  public void test_JSONErrorMessageParser_empty() {
    testParser("", 0, "");
  }

  public void test_JSONErrorMessageParser_errorReading() {
    testParser("Error reading /usr/foo: Expected 'x' at character 81", 81, "Expected 'x'");
  }

  public void test_JSONErrorMessageParser_missing() {
    testParser("JSONObject[\"baz\"] is missing", 0, "\"baz\" is missing");
  }

  public void test_JSONErrorMessageParser_other() {
    testParser("message", 0, "message");
  }

  protected void testParser(String originalErrMsg, int expectedOffset, String expectedMessage) {
    JSONErrorMessageParser p = new JSONErrorMessageParser(originalErrMsg);
    assertEquals(expectedOffset, p.getOffset());
    assertEquals(expectedMessage, p.getErrorMessage());
  }
}
