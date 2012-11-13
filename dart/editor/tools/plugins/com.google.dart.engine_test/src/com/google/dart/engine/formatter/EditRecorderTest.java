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
package com.google.dart.engine.formatter;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.formatter.FakeFactory.FakeRecorder;

/**
 * Basic {@link EditRecorder} tests.
 */
public class EditRecorderTest extends EngineTestCase {

  private final FakeRecorder recorder = FakeFactory.createRecorder();

  public void testIndent() throws Exception {
    assertEquals(0, recorder.indentationLevel);
    assertEquals(2, recorder.indentationSize);
    recorder.indent();
    assertEquals(2, recorder.indentationLevel);
    assertEquals(1, recorder.numberOfIndentations);
  }

  public void testSpace_1() throws Exception {
    recorder.needSpace = false;
    int startColumn = recorder.column;
    recorder.space();
    assertEquals(startColumn, recorder.column);
  }

  public void testSpace_2() throws Exception {
    recorder.needSpace = true;
    int startColumn = recorder.column;
    recorder.space();
    assertEquals(startColumn + 1, recorder.column);
  }

  public void testUnindent() throws Exception {
    assertEquals(0, recorder.indentationLevel);
    assertEquals(2, recorder.indentationSize);
    recorder.indent();
    recorder.unIndent();
    assertEquals(0, recorder.indentationLevel);
  }

}
