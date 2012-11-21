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
import com.google.dart.engine.internal.formatter.edit.StringEditOperation;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic {@link StringEditOperation} tests.
 */
public class StringEditOperationTest extends EngineTestCase {

  final List<Edit> edits = new ArrayList<Edit>();

  public void test_replace_same_len() throws Exception {
    //'fun house' -> 'foo bar'
    edits.add(new Edit(1, 2, "oo"));
    edits.add(new Edit(4, 5, "bar"));
    assertEditEquals("fun house", "foo bar");
  }

  public void test_replace_shorten() throws Exception {
    //'AaBbCc' -> 'abc'
    edits.add(new Edit(0, 2, "a"));
    edits.add(new Edit(2, 2, "b"));
    edits.add(new Edit(4, 2, "c"));
    assertEditEquals("AaBbCc", "abc");
  }

  private void assertEditEquals(String src, String result) {
    assertEquals("Strings don't match:", result, new StringEditOperation(edits).applyTo(src));
  }

}
