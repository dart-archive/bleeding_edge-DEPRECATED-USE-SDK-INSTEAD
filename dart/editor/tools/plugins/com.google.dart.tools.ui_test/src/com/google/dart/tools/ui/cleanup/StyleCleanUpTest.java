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
package com.google.dart.tools.ui.cleanup;

import com.google.dart.tools.ui.internal.cleanup.style.Style_trailingSpace_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.style.Style_useBlocks_CleanUp;

/**
 * Test for "Code Style" clean ups.
 */
public final class StyleCleanUpTest extends AbstractCleanUpTest {

  public void test_trailingSpace() throws Exception {
    ICleanUp cleanUp = new Style_trailingSpace_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "",
        "  ",
        "  //",
        "  //  ",
        "class A { ",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "",
        "",
        "  //",
        "  //",
        "class A {",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_useBlocks_always() throws Exception {
    Style_useBlocks_CleanUp cleanUp = new Style_useBlocks_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true)",
        "    process();",
        "  if (true)",
        "    process();",
        "  else",
        "    process();",
        "  if (false) {",
        "    process();",
        "  } else",
        "    process();",
        "  while (true)",
        "    process();",
        "  for (var item in [])",
        "    process();",
        "  for (var i = 0; i < 10; i++)",
        "    process();",
        "}",
        "process() {}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) {",
        "    process();",
        "  }",
        "  if (true) {",
        "    process();",
        "  } else {",
        "    process();",
        "  }",
        "  if (false) {",
        "    process();",
        "  } else {",
        "    process();",
        "  }",
        "  while (true) {",
        "    process();",
        "  }",
        "  for (var item in []) {",
        "    process();",
        "  }",
        "  for (var i = 0; i < 10; i++) {",
        "    process();",
        "  }",
        "}",
        "process() {}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_useBlocks_always_ifElseIf() throws Exception {
    Style_useBlocks_CleanUp cleanUp = new Style_useBlocks_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (0 == 0)",
        "    print(0);",
        "  else if (1 == 1)",
        "    print(0);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (0 == 0) {",
        "    print(0);",
        "  } else if (1 == 1) {",
        "    print(0);",
        "  }",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_useBlocks_always_nop() throws Exception {
    Style_useBlocks_CleanUp cleanUp = new Style_useBlocks_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) {",
        "    process();",
        "  }",
        "  while (true) {",
        "    process();",
        "  }",
        "  for (var item in []) {",
        "    process();",
        "  }",
        "  for (var i = 0; i < 10; i++) {",
        "    process();",
        "  }",
        "}",
        "process() {}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_useBlocks_always_nop_oneLine() throws Exception {
    Style_useBlocks_CleanUp cleanUp = new Style_useBlocks_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) return;",
        "  for (var v in []) print(v);",
        "  for (var i = 0; i < 10; i++) print(i);",
        "  while (true) print(0);",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

}
