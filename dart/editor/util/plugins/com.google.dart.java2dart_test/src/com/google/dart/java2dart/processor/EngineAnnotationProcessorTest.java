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
package com.google.dart.java2dart.processor;

import com.google.dart.java2dart.engine.EngineAnnotationProcessor;

/**
 * Test for {@link EngineAnnotationProcessor}.
 */
public class EngineAnnotationProcessorTest extends SemanticProcessorTest {

  public void test_DartBlockBody() throws Exception {
    setFileLines(
        "test/DartBlockBody.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class DartBlockBody {",
            "  String[] value();",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.dart.engine.utilities.collection.IntList;",
        "public class Test {",
        "  @DartBlockBody({'if (true) {', '  return 42;', '}', 'return 5;'})",
        "  public int foo() {",
        "    return 0;",
        "  }",
        "  @DartBlockBody({})",
        "  public void bar() {",
        "    var v = 0;",
        "  }",
        "  public int baz() {",
        "    return 2;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  int foo() {",
        "    if (true) {",
        "      return 42;",
        "    }",
        "    return 5;",
        "  }",
        "  void bar() {",
        "  }",
        "  int baz() => 2;",
        "}");
  }

  public void test_DartExpressionBody() throws Exception {
    setFileLines(
        "test/DartExpressionBody.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class DartExpressionBody {",
            "  String[] value();",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.dart.engine.utilities.collection.IntList;",
        "public class Test {",
        "  @DartExpressionBody('42')",
        "  public int foo() {",
        "    return 0;",
        "  }",
        "  public void bar() {",
        "    return 1;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  int foo() => 42;",
        "  void bar() => 1;",
        "}");
  }

  public void test_DartOmit_class() throws Exception {
    setFileLines(
        "test/DartOmit.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class DartOmit {",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.dart.engine.utilities.collection.IntList;",
        "public class Test {",
        "  @DartOmit",
        "  class A {",
        "  }",
        "  class B {",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "}",
        "class Test_B {",
        "}");
  }

  public void test_DartOmit_field() throws Exception {
    setFileLines(
        "test/DartOmit.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class DartOmit {",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.dart.engine.utilities.collection.IntList;",
        "public class Test {",
        "  public int fieldA = 0;",
        "  @DartOmit",
        "  public int fieldB = 1;",
        "  public int fieldC = 2;",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  int fieldA = 0;",
        "  int fieldC = 2;",
        "}");
  }

  public void test_DartOmit_method() throws Exception {
    setFileLines(
        "test/DartOmit.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class DartOmit {",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.dart.engine.utilities.collection.IntList;",
        "public class Test {",
        "  @DartOmit",
        "  public void foo() {",
        "  }",
        "  public void bar() {",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void bar() {",
        "  }",
        "}");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    replaceSingleQuotes = true;
  }

  private void runProcessor() {
    new EngineAnnotationProcessor(context).process(unit);
  }
}
