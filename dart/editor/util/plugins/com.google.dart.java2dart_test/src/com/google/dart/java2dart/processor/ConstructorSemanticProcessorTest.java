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

/**
 * Test for {@link ConstructorSemanticProcessor}
 */
public class ConstructorSemanticProcessorTest extends SemanticProcessorTest {

  public void test_empty() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test extends Foo {",
        "  Test() {}",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test extends Foo {",
        "}");
  }

  public void test_hasStatements() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test extends Foo {",
        "  Test(String p) {",
        "    print(p);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test extends Foo {",
        "  Test(String p) {",
        "    print(p);",
        "  }",
        "}");
  }

  public void test_super_default() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test extends Foo {",
        "  Test() {",
        "    super();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test extends Foo {",
        "}");
  }

  public void test_super_withArguments() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test extends Foo {",
        "  Test(String boo) {",
        "    super(boo);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test extends Foo {",
        "  Test(String boo) : super(boo);",
        "}");
  }

  public void test_this_noOtherStatements() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  Test() {",
        "    this(42);",
        "  }",
        "  Test(int p) {",
        "    print(p);",
        "  }",
        "}");
//    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  Test() : this.con1(42);",
        "  Test.con1(int p) {",
        "    print(p);",
        "  }",
        "}");
  }

  private void runProcessor() {
    new ConstructorSemanticProcessor(context).process(unit);
  }
}
