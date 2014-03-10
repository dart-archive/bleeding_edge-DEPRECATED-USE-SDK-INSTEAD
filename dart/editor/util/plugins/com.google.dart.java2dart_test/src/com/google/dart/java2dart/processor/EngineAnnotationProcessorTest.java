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
        "public class Test {",
        "  @DartExpressionBody('42')",
        "  public int foo() {",
        "    return 0;",
        "  }",
        "  public void bar() {",
        "    return 1;",
        "  }",
        "}");
    assertFormattedSource(//
        "class Test {",
        "  int foo() => 42;",
        "  void bar() => 1;",
        "}");
  }

  public void test_DartName_class() throws Exception {
    declareDartName();
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "@DartName('NewName')",
        "public class Test {",
        "  public Test(int p) {",
        "  }",
        "  public Test create() {",
        "    return new Test(42);",
        "  }",
        "}");
    assertFormattedSource(//
        "class NewName {",
        "  NewName(int p);",
        "  NewName create() => new NewName(42);",
        "}");
  }

  public void test_DartName_constructor() throws Exception {
    declareDartName();
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  @DartName('forInt')",
        "  public Test(int p) {",
        "  }",
        "  @DartName('forDouble')",
        "  public Test(double p) {",
        "  }",
        "  public Test(String p) {",
        "  }",
        "  public Test createInt() {",
        "    return new Test(42);",
        "  }",
        "  public Test createDouble() {",
        "    return new Test(4.2);",
        "  }",
        "  public Test createString() {",
        "    return new Test('abc');",
        "  }",
        "}");
    assertFormattedSource(//
        "class Test {",
        "  Test.forInt(int p);",
        "  Test.forDouble(double p);",
        "  Test(String p);",
        "  Test createInt() => new Test.forInt(42);",
        "  Test createDouble() => new Test.forDouble(4.2);",
        "  Test createString() => new Test('abc');",
        "}");
  }

  public void test_DartName_field() throws Exception {
    declareDartName();
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  @DartName('_newName')",
        "  private int foo;",
        "  public void bar() {",
        "    foo = 42;",
        "    print(foo);",
        "  }",
        "}");
    assertFormattedSource(//
        "class Test {",
        "  int _newName = 0;",
        "  void bar() {",
        "    _newName = 42;",
        "    print(_newName);",
        "  }",
        "}");
  }

  public void test_DartName_method() throws Exception {
    declareDartName();
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  @DartName('newName')",
        "  public void foo() {",
        "  }",
        "  public void bar() {",
        "    foo();",
        "  }",
        "}");
    assertFormattedSource(//
        "class Test {",
        "  void newName() {",
        "  }",
        "  void bar() {",
        "    newName();",
        "  }",
        "}");
  }

  public void test_DartOmit_class() throws Exception {
    declareDartOmit();
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  @DartOmit",
        "  class A {",
        "  }",
        "  class B {",
        "  }",
        "}");
    assertFormattedSource(//
        "class Test {",
        "}",
        "class Test_B {",
        "}");
  }

  public void test_DartOmit_field() throws Exception {
    declareDartOmit();
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
    assertFormattedSource(//
        "class Test {",
        "  int fieldA = 0;",
        "  int fieldC = 2;",
        "}");
  }

  public void test_DartOmit_method() throws Exception {
    declareDartOmit();
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
    assertFormattedSource(//
        "class Test {",
        "  void bar() {",
        "  }",
        "}");
  }

  public void test_DartOptional_method_named() throws Exception {
    declareDartOmit();
    declareDartOptional();
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.dart.engine.utilities.collection.IntList;",
        "public class Test {",
        "  @DartOmit",
        "  public void foo(int a) {",
        "    foo(a, 42);",
        "  }",
        "  public void foo(int a, @DartOptional(defaultValue = '42', kind = ParameterKind.NAMED) int b) {",
        "  }",
        "  public void bar() {",
        "    foo(1);",
        "    foo(1, 2);",
        "  }",
        "}");
    assertFormattedSource(//
        "class Test {",
        "  void foo(int a, {int b: 42}) {",
        "  }",
        "  void bar() {",
        "    foo(1);",
        "    foo(1, b: 2);",
        "  }",
        "}");
  }

  public void test_DartOptional_method_positional() throws Exception {
    declareDartOmit();
    declareDartOptional();
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.dart.engine.utilities.collection.IntList;",
        "public class Test {",
        "  @DartOmit",
        "  public void foo(int a) {",
        "    foo(a, 42);",
        "  }",
        "  public void foo(int a, @DartOptional(defaultValue = '42', kind = ParameterKind.POSITIONAL) int b) {",
        "  }",
        "  public void bar() {",
        "    foo(1);",
        "    foo(1, 2);",
        "  }",
        "}");
    assertFormattedSource(//
        "class Test {",
        "  void foo(int a, [int b = 42]) {",
        "  }",
        "  void bar() {",
        "    foo(1);",
        "    foo(1, 2);",
        "  }",
        "}");
  }

  public void test_DartOptional_method_positional_byDefault() throws Exception {
    declareDartOmit();
    declareDartOptional();
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.dart.engine.utilities.collection.IntList;",
        "public class Test {",
        "  @DartOmit",
        "  public void foo(int a) {",
        "    foo(a, 42);",
        "  }",
        "  public void foo(int a, @DartOptional(defaultValue = '42') int b) {",
        "  }",
        "  public void bar() {",
        "    foo(1);",
        "    foo(1, 2);",
        "  }",
        "}");
    assertFormattedSource(//
        "class Test {",
        "  void foo(int a, [int b = 42]) {",
        "  }",
        "  void bar() {",
        "    foo(1);",
        "    foo(1, 2);",
        "  }",
        "}");
  }

  public void test_override() throws Exception {
    declareDartName();
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public static class A {",
        "    public void foo() {}",
        "  }",
        "  public static class B extends A {",
        "    @Override",
        "    public void foo() {}",
        "    public void bar() {}",
        "  }",
        "}");
    assertFormattedSource(//
        "class Test {",
        "}",
        "class Test_A {",
        "  void foo() {",
        "  }",
        "}",
        "class Test_B extends Test_A {",
        "  @override",
        "  void foo() {",
        "  }",
        "  void bar() {",
        "  }",
        "}");
  }

  @Override
  protected void applyPostTranslateProcessors() {
    new EngineAnnotationProcessor(context).process(unit);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    replaceSingleQuotes = true;
  }

  private void declareDartName() throws Exception {
    setFileLines(
        "test/DartName.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "import java.lang.annotation.*;",
            "@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})",
            "public @interface DartOmit {",
            "  String value();",
            "}"));
  }

  private void declareDartOmit() throws Exception {
    setFileLines(
        "test/DartOmit.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "import java.lang.annotation.*;",
            "@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})",
            "public @interface DartOmit {",
            "}"));
  }

  private void declareDartOptional() throws Exception {
    setFileLines(
        "test/ParameterKind.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "import java.lang.annotation.*;",
            "public enum ParameterKind {",
            "  REQUIRED, POSITIONAL, NAMED;",
            "}"));
    setFileLines(
        "test/DartOptional.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "import java.lang.annotation.*;",
            "@Target(ElementType.PARAMETER)",
            "public @interface DartOptional {",
            "  String defaultValue() default '';",
            "  ParameterKind kind()  default ParameterKind.POSITIONAL;",
            "}"));
  }
}
