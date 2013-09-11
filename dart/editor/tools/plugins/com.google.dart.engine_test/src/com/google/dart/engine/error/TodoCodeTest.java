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

package com.google.dart.engine.error;

import com.google.dart.engine.EngineTestCase;

import java.util.regex.Matcher;

public class TodoCodeTest extends EngineTestCase {

  public void test_locateLineCommentTodo() {
    locate("//TODO", 2, 4);
    locate("//TODO:", 2, 5);
    locate("// TODO(sdsdf): ", 3, 13);
    locate("// TODO (sdsdf): ", 3, 14);
    locate("//TODO(sdsdf)", 2, 11);
    locate("//  TODO(sdsdf): ", 4, 13);
  }

  public void test_locateMultiLineCommentTodo() {
    locate("* TODO \n * foo", 2, 5);
    locate("*TODO:\n * foo", 1, 5);
    locate("*TODO(sdsdf): \n * foo", 1, 13);
    locate("* TODO(sdsdf)\n * foo", 2, 11);
    locate(" * TODO(sdsdf): \n * foo", 3, 13);
    locate(" * sdfsdf \n * TODO(sdsdf): \n * foo", 14, 13);
  }

  public void test_locateMultipleComments() {
    Matcher m = TodoCode.TODO_REGEX.matcher("/**\n * TODO: foo bar\n * TODO bar baz\n*/");

    assertTrue(m.find());
    assertEquals(7, m.start(1));
    assertEquals(13, m.end(1) - m.start(1));

    assertTrue(m.find());
    assertEquals(24, m.start(1));
    assertEquals(12, m.end(1) - m.start(1));

    assertFalse(m.find());
  }

  public void test_negativeLineCommentTodo() {
    negative("// TODOS");
    negative("// todo");
  }

  public void test_negativeMultiLineCommentTodo() {
    negative(" * TODOS    \n * foo");
    negative(" * todo\n * foo");
  }

  private void locate(String comment, int start, int length) {
    Matcher m = TodoCode.TODO_REGEX.matcher(comment);

    assertTrue(m.find());
    assertEquals(start, m.start(1));
    assertEquals(length, m.end(1) - m.start(1));
  }

  private void negative(String comment) {
    Matcher m = TodoCode.TODO_REGEX.matcher(comment);

    assertFalse(m.find());
  }

}
