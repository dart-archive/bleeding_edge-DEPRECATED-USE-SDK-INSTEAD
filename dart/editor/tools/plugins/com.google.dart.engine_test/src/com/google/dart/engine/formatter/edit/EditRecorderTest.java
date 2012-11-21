/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.formatter.edit;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.formatter.FakeFactory.FakeRecorder;
import com.google.dart.engine.internal.formatter.edit.StringEditOperation;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.TokenType;

import static com.google.dart.engine.formatter.FakeFactory.createRecorder;

/**
 * Basic {@link EditRecorder} tests.
 */
public class EditRecorderTest extends EngineTestCase {

  public static final String NEW_LINE = System.getProperty("line.separator");

  private FakeRecorder recorder = createRecorder("");

  public void testCountWhitespace() throws Exception {
    assertEquals(5, createRecorder("     ").countWhitespace());
    assertEquals(0, createRecorder("").countWhitespace());
    assertEquals(3, createRecorder("   foo").countWhitespace());
  }

  public void testIndent() throws Exception {
    assertEquals(0, recorder.indentationLevel);
    assertEquals(2, recorder.indentPerLevel);
    recorder.indent();
    assertEquals(2, recorder.indentationLevel);
    assertEquals(1, recorder.numberOfIndentations);
  }

  public void testIsNewlineAt() throws Exception {
    assertTrue(createRecorder("012" + NEW_LINE).isNewlineAt(3));
    assertFalse(createRecorder("0" + NEW_LINE).isNewlineAt(0));
    assertFalse(createRecorder("0" + NEW_LINE).isNewlineAt(2));
  }

  public void testNewline_eats_extra_ws_1() throws Exception {
    String src = " " + NEW_LINE;
    recorder = createRecorder(src);
    recorder.newline();
    assertResultEquals("" + NEW_LINE);
  }

  public void testNewline_eats_extra_ws_2() throws Exception {
    String src = "class A " + NEW_LINE;
    recorder = createRecorder(src);
    KeywordToken token = new KeywordToken(Keyword.CLASS, 0);
    token.setNext(new StringToken(TokenType.IDENTIFIER, "A", 6));
    recorder.setStart(token);
    recorder.advance("class");
    recorder.space();
    recorder.advance("A");
    recorder.newline();
    assertResultEquals("class A" + NEW_LINE);
  }

  public void testSpace_advances_1() throws Exception {
    recorder = createRecorder(" foo");
    int startColumn = recorder.column;
    recorder.space();
    assertEquals(startColumn + 1, recorder.column);
  }

  public void testSpace_eats_extra_ws_1() throws Exception {
    String src = "  class";
    recorder = createRecorder(src);
    recorder.setStart(new KeywordToken(Keyword.CLASS, 3));
    recorder.space();
    recorder.advance("class");
    assertResultEquals(" class");
  }

  public void testSpace_eats_extra_ws_2() throws Exception {
    String src = "class  A";
    recorder = createRecorder(src);
    KeywordToken token = new KeywordToken(Keyword.CLASS, 0);
    token.setNext(new StringToken(TokenType.IDENTIFIER, "A", 7));
    recorder.setStart(token);
    recorder.advance("class");
    recorder.space();
    recorder.advance("A");
    assertResultEquals("class A");
  }

  public void testSpace_eats_extra_ws_3() throws Exception {
    String src = "class  A  extends";
    recorder = createRecorder(src);
    KeywordToken token = new KeywordToken(Keyword.CLASS, 0);
    StringToken token2 = new StringToken(TokenType.IDENTIFIER, "A", 7);
    KeywordToken token3 = new KeywordToken(Keyword.EXTENDS, 10);
    token.setNext(token2);
    token2.setNext(token3);
    recorder.setStart(token);
    recorder.advance("class");
    recorder.space();
    recorder.advance("A");
    recorder.space();
    recorder.advance("extends");
    assertResultEquals("class A extends");
  }

  public void testUnindent() throws Exception {
    assertEquals(0, recorder.indentationLevel);
    assertEquals(2, recorder.indentPerLevel);
    recorder.indent();
    recorder.unIndent();
    assertEquals(0, recorder.indentationLevel);
  }

//  public void testSpace_eats_extra_ws() throws Exception {
//    recorder = createRecorder("  foo");
//    int startColumn = recorder.column;
//    recorder.space();
//    assertEquals(startColumn + 1, recorder.column);
//    Edit edit = recorder.getLastEdit();
//    assertThat(edit, matches(edit(1, 1, "")));
//  }

  private void assertResultEquals(String expected) {
    assertEqualString(
        expected,
        new StringEditOperation(recorder.getEdits()).applyTo(recorder.getSource()));
  }

}
