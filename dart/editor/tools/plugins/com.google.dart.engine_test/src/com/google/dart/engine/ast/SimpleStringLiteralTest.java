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
package com.google.dart.engine.ast;

import com.google.dart.engine.parser.ParserTestCase;
import com.google.dart.engine.scanner.Token;

import static com.google.dart.engine.scanner.TokenFactory.tokenFromString;

public class SimpleStringLiteralTest extends ParserTestCase {
  public void test_getValueOffset() throws Exception {
    assertEquals(1, new SimpleStringLiteral(tokenFromString("'X'"), "X").getValueOffset());
    assertEquals(1, new SimpleStringLiteral(tokenFromString("\"X\""), "X").getValueOffset());
    assertEquals(3, new SimpleStringLiteral(tokenFromString("\"\"\"X\"\"\""), "X").getValueOffset());
    assertEquals(3, new SimpleStringLiteral(tokenFromString("'''X'''"), "X").getValueOffset());
    assertEquals(2, new SimpleStringLiteral(tokenFromString("r'X'"), "X").getValueOffset());
    assertEquals(2, new SimpleStringLiteral(tokenFromString("r\"X\""), "X").getValueOffset());
    assertEquals(
        4,
        new SimpleStringLiteral(tokenFromString("r\"\"\"X\"\"\""), "X").getValueOffset());
    assertEquals(4, new SimpleStringLiteral(tokenFromString("r'''X'''"), "X").getValueOffset());
  }

  public void test_isMultiline() throws Exception {
    assertFalse(new SimpleStringLiteral(tokenFromString("'X'"), "X").isMultiline());
    assertFalse(new SimpleStringLiteral(tokenFromString("r'X'"), "X").isMultiline());
    assertFalse(new SimpleStringLiteral(tokenFromString("\"X\""), "X").isMultiline());
    assertFalse(new SimpleStringLiteral(tokenFromString("r\"X\""), "X").isMultiline());
    assertTrue(new SimpleStringLiteral(tokenFromString("'''X'''"), "X").isMultiline());
    assertTrue(new SimpleStringLiteral(tokenFromString("r'''X'''"), "X").isMultiline());
    assertTrue(new SimpleStringLiteral(tokenFromString("\"\"\"X\"\"\""), "X").isMultiline());
    assertTrue(new SimpleStringLiteral(tokenFromString("r\"\"\"X\"\"\""), "X").isMultiline());
  }

  public void test_isRaw() throws Exception {
    assertFalse(new SimpleStringLiteral(tokenFromString("'X'"), "X").isRaw());
    assertFalse(new SimpleStringLiteral(tokenFromString("\"X\""), "X").isRaw());
    assertFalse(new SimpleStringLiteral(tokenFromString("\"\"\"X\"\"\""), "X").isRaw());
    assertFalse(new SimpleStringLiteral(tokenFromString("'''X'''"), "X").isRaw());
    assertTrue(new SimpleStringLiteral(tokenFromString("r'X'"), "X").isRaw());
    assertTrue(new SimpleStringLiteral(tokenFromString("r\"X\""), "X").isRaw());
    assertTrue(new SimpleStringLiteral(tokenFromString("r\"\"\"X\"\"\""), "X").isRaw());
    assertTrue(new SimpleStringLiteral(tokenFromString("r'''X'''"), "X").isRaw());
  }

  public void test_simple() throws Exception {
    Token token = tokenFromString("'value'");
    SimpleStringLiteral stringLiteral = new SimpleStringLiteral(token, "value");
    assertSame(token, stringLiteral.getLiteral());
    assertSame(token, stringLiteral.getBeginToken());
    assertSame(token, stringLiteral.getEndToken());
    assertEquals("value", stringLiteral.getValue());
  }
}
