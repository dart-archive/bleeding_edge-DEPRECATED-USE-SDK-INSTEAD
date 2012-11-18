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

import static com.google.dart.engine.formatter.FakeFactory.createRecorder;
import static com.google.dart.engine.formatter.FakeFactory.edit;
import static com.google.dart.engine.formatter.MatcherFactory.matches;

import static org.junit.Assert.assertThat;

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

  public void testSpace_advances_1() throws Exception {
    recorder = createRecorder(" foo");
    int startColumn = recorder.column;
    recorder.space();
    assertEquals(startColumn + 1, recorder.column);
  }

  public void testSpace_advances_2() throws Exception {
    recorder = createRecorder("foo");
    int startColumn = recorder.column;
    recorder.space();
    assertEquals(startColumn + 1, recorder.column);
  }

  public void testSpace_eats_extra_ws() throws Exception {
    recorder = createRecorder("  foo");
    int startColumn = recorder.column;
    recorder.space();
    assertEquals(startColumn + 1, recorder.column);
    Edit edit = recorder.getLastEdit();
    assertThat(edit, matches(edit(1, 1, "")));
  }

  public void testUnindent() throws Exception {
    assertEquals(0, recorder.indentationLevel);
    assertEquals(2, recorder.indentPerLevel);
    recorder.indent();
    recorder.unIndent();
    assertEquals(0, recorder.indentationLevel);
  }

}
