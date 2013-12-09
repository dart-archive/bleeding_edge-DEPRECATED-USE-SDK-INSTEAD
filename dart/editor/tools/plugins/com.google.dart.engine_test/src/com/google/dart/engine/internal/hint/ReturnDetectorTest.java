/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.hint;

import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.parser.ParserTestCase;

public class ReturnDetectorTest extends ParserTestCase {
  public void test_block_empty() throws Exception {
    assertFalse("{}");
  }

  public void test_block_noReturn() throws Exception {
    assertFalse("{ throw 'a'; }");
  }

  public void test_block_return() throws Exception {
    assertTrue("{ return 0; }");
  }

  public void test_block_returnNotLast() throws Exception {
    assertTrue("{ return 0; throw 'a'; }");
  }

  public void test_creation() {
    assertNotNull(new ReturnDetector());
  }

  public void test_if_noReturn() throws Exception {
    assertFalse("if (c) i++;");
  }

  public void test_if_return() throws Exception {
    assertFalse("if (c) return 0;");
  }

  public void test_ifElse_bothReturn() throws Exception {
    assertTrue("if (c) return 0; else return 1;");
  }

  public void test_ifElse_elseReturn() throws Exception {
    assertFalse("if (c) i++; else return 1;");
  }

  public void test_ifElse_noReturn() throws Exception {
    assertFalse("if (c) i++; else j++;");
  }

  public void test_ifElse_thenReturn() throws Exception {
    assertFalse("if (c) return 0; else j++;");
  }

  public void test_return() throws Exception {
    assertTrue("return 0;");
  }

  public void test_switch_allReturn() throws Exception {
    assertTrue("switch (i) { case 0: return 0; default: return 1; }");
  }

  public void test_switch_noDefault() throws Exception {
    assertFalse("switch (i) { case 0: return 0; }");
  }

  public void test_switch_nonReturn() throws Exception {
    assertFalse("switch (i) { case 0: i++; default: return 1; }");
  }

  private void assertFalse(String source) throws Exception {
    assertHasReturn(false, source);
  }

  private void assertHasReturn(boolean expectedResult, String source) throws Exception {
    ReturnDetector detector = new ReturnDetector();
    Statement statement = parseStatement(source);
    assertSame(expectedResult, statement.accept(detector));
  }

  private void assertTrue(String source) throws Exception {
    assertHasReturn(true, source);
  }
}
