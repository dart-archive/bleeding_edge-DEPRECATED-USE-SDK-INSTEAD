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

package com.google.dart.java2dart;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.java2dart.processor.PropertySemanticProcessor;

import java.io.File;

/**
 * Test for general Java semantics to Dart translation.
 */
public class SemanticTest extends AbstractSemanticTest {

  public void test_anonymousClass_extendsClass_referenceFinalVariables() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public Test(int i, double f) {",
            "  }",
            "  public static main() {",
            "    final int myValue = 5;",
            "    Test v = new Test(1, 2.3) {",
            "      int foo() {",
            "        return myValue;",
            "      }",
            "    };",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  Test(int i, double f) {",
            "  }",
            "  static main() {",
            "    int myValue = 5;",
            "    Test v = new Test_0(1, 2.3, myValue);",
            "  }",
            "}",
            "class Test_0 extends Test {",
            "  int myValue = 0;",
            "  Test_0(int arg0, double arg1, this.myValue) : super(arg0, arg1);",
            "  int foo() => myValue;",
            "}"),
        getFormattedSource(unit));
  }

  public void test_anonymousClass_implementsInterface() throws Exception {
    setFileLines(
        "test/MyInterface.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public interface MyInterface {",
            "}"));
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public static main() {",
            "    MyInterface v = new MyInterface() {",
            "    };",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "abstract class MyInterface {",
            "}",
            "class Test {",
            "  static main() {",
            "    MyInterface v = new MyInterface_0();",
            "  }",
            "}",
            "class MyInterface_0 implements MyInterface {",
            "}"),
        getFormattedSource(unit));
  }

  public void test_anonymousClass_implementsInterface_referenceFinalVariables() throws Exception {
    setFileLines(
        "test/ErrorListener.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public interface ErrorListener {",
            "  void onError();",
            "}"));
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public static void main() {",
            "    final boolean[] hasErrors = {false};",
            "    ErrorListener v = new ErrorListener() {",
            "      void onError() {",
            "        hasErrors[0] = false;",
            "        hasErrors[0] = true;",
            "      }",
            "    };",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "abstract class ErrorListener {",
            "  void onError();",
            "}",
            "class Test {",
            "  static void main() {",
            "    List<bool> hasErrors = [false];",
            "    ErrorListener v = new ErrorListener_0(hasErrors);",
            "  }",
            "}",
            "class ErrorListener_0 implements ErrorListener {",
            "  List<bool> hasErrors;",
            "  ErrorListener_0(this.hasErrors);",
            "  void onError() {",
            "    hasErrors[0] = false;",
            "    hasErrors[0] = true;",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_anonymousClass_referenceEnclosingClassField() throws Exception {
    setFileLines(
        "test/ErrorListener.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public interface ErrorListener {",
            "  void onError();",
            "}"));
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  boolean hasErrors;",
            "  void foo() {};",
            "  void main() {",
            "    ErrorListener v = new ErrorListener() {",
            "      void onError() {",
            "        foo();",
            "        hasErrors = true;",
            "      }",
            "    };",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "abstract class ErrorListener {",
            "  void onError();",
            "}",
            "class Test {",
            "  bool hasErrors = false;",
            "  void foo() {",
            "  }",
            "  void main() {",
            "    ErrorListener v = new ErrorListener_0(this);",
            "  }",
            "}",
            "class ErrorListener_0 implements ErrorListener {",
            "  final Test Test_this;",
            "  ErrorListener_0(this.Test_this);",
            "  void onError() {",
            "    Test_this.foo();",
            "    Test_this.hasErrors = true;",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_buildSingleDartUnit() throws Exception {
    setFileLines(
        "test/Main.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Main {",
            "  static void foo() {}",
            "}",
            ""));
    setFileLines(
        "test/Second.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Second {",
            "  static void bar() {",
            "    Main.foo();",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Main {",
            "  static void foo() {",
            "  }",
            "}",
            "class Second {",
            "  static void bar() {",
            "    Main.foo();",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public static class B {",
            "    B() {}",
            "  }",
            "  void test1(B p) {}",
            "  void test2(A.B p) {}",
            "  void test3() {",
            "    new B();",
            "    new A.B();",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  void test1(A_B p) {",
            "  }",
            "  void test2(A_B p) {",
            "  }",
            "  void test3() {",
            "    new A_B();",
            "    new A_B();",
            "  }",
            "}",
            "class A_B {",
            "  A_B() {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner_referenceFromAnonymous() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public class B {",
            "    void foo() {}",
            "  }",
            "  void test(final B p) {",
            "    return new Object() {",
            "      void main() {",
            "        p.foo();",
            "      }",
            "    };",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  void test(A_B p) => new Object_0(p);",
            "}",
            "class A_B {",
            "  void foo() {",
            "  }",
            "}",
            "class Object_0 extends Object {",
            "  A_B p;",
            "  Object_0(this.p) : super();",
            "  void main() {",
            "    p.foo();",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner2() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public interface A {",
            "  public interface B {",
            "  }",
            "  void test(B p) {}",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "abstract class A {",
            "  void test(A_B p) {",
            "  }",
            "}",
            "abstract class A_B {",
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner3() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public static class B {",
            "    private static final int ZERO = 0;",
            "    public int getValue() {",
            "      return ZERO + 1;",
            "    }",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "}",
            "class A_B {",
            "  static int ZERO = 0;",
            "  int getValue() => ZERO + 1;",
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner4() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public interface B {",
            "  }",
            "  void test1(B p) {}",
            "  A.B test2() {",
            "    return new A.B() {};",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  void test1(A_B p) {",
            "  }",
            "  A_B test2() => new A_B_0();",
            "}",
            "abstract class A_B {",
            "}",
            "class A_B_0 implements A_B {",
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner5() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public class B {",
            "  }",
            "  void test1(B p) {}",
            "  A.B test2() {",
            "    return new A.B() {};",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  void test1(A_B p) {",
            "  }",
            "  A_B test2() => new A_B_0();",
            "}",
            "class A_B {",
            "}",
            "class A_B_0 extends A_B {",
            "  A_B_0() : super();",
            "}"),
        getFormattedSource(unit));
  }

  public void test_configureRenameField() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  int foo;",
            "  static void foo() {}",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B {",
            "  static void bar() {",
            "    print(A.foo);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    context.addRename("Ltest/A;.foo", "myField");
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  int myField = 0;",
            "  static void foo() {",
            "  }",
            "}",
            "class B {",
            "  static void bar() {",
            "    print(A.myField);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_configureRenameMethod() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  static void foo() {}",
            "  static void foo(int p) {}",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B {",
            "  static void bar() {",
            "    A.foo(42);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    context.addRename("Ltest/A;.foo(I)", "fooWithInt");
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  static void foo() {",
            "  }",
            "  static void fooWithInt(int p) {",
            "  }",
            "}",
            "class B {",
            "  static void bar() {",
            "    A.fooWithInt(42);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_constructor_configureNames() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  Test() {",
            "    print(0);",
            "  }",
            "  Test(int p) {",
            "    print(1);",
            "  }",
            "  Test(double p) {",
            "    print(2);",
            "  }",
            "  static void main() {",
            "    new Test();",
            "    new Test(2);",
            "    new Test(3.0);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // configure names for constructors
    context.addRename("Ltest/Test;.(I)", "forInt");
    context.addRename("Ltest/Test;.(D)", "forDouble");
    // do translate
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  Test() {",
            "    _jtd_constructor_0_impl();",
            "  }",
            "  _jtd_constructor_0_impl() {",
            "    print(0);",
            "  }",
            "  Test.forInt(int p) {",
            "    _jtd_constructor_1_impl(p);",
            "  }",
            "  _jtd_constructor_1_impl(int p) {",
            "    print(1);",
            "  }",
            "  Test.forDouble(double p) {",
            "    _jtd_constructor_2_impl(p);",
            "  }",
            "  _jtd_constructor_2_impl(double p) {",
            "    print(2);",
            "  }",
            "  static void main() {",
            "    new Test();",
            "    new Test.forInt(2);",
            "    new Test.forDouble(3.0);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_constructor_defaultNames() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  Test() {",
            "    print(0);",
            "  }",
            "  Test(int p) {",
            "    print(1);",
            "  }",
            "  Test(double p) {",
            "    print(2);",
            "  }",
            "  static void main() {",
            "    new Test();",
            "    new Test(2);",
            "    new Test(3.0);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  Test() {",
            "    _jtd_constructor_0_impl();",
            "  }",
            "  _jtd_constructor_0_impl() {",
            "    print(0);",
            "  }",
            "  Test.con1(int p) {",
            "    _jtd_constructor_1_impl(p);",
            "  }",
            "  _jtd_constructor_1_impl(int p) {",
            "    print(1);",
            "  }",
            "  Test.con2(double p) {",
            "    _jtd_constructor_2_impl(p);",
            "  }",
            "  _jtd_constructor_2_impl(double p) {",
            "    print(2);",
            "  }",
            "  static void main() {",
            "    new Test();",
            "    new Test.con1(2);",
            "    new Test.con2(3.0);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_ensurePrimitiveFieldInitializer() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  boolean booleanF;",
            "  byte byteF;",
            "  char charF;",
            "  short shortF;",
            "  int intF;",
            "  long longF;",
            "  float floatF;",
            "  double doubleF;",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  bool booleanF = false;",
            "  int byteF = 0;",
            "  int charF = 0;",
            "  int shortF = 0;",
            "  int intF = 0;",
            "  int longF = 0;",
            "  double floatF = 0.0;",
            "  double doubleF = 0.0;",
            "}"),
        getFormattedSource(unit));
  }

  public void test_enum_constantWithSubclass() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public enum Test {",
            "  EOF(5) {",
            "    void foo() {",
            "      print(2);",
            "    }",
            "  }, DEF;",
            "  private Test() {",
            "  }",
            "  private Test(int p) {",
            "  }",
            "  void foo() {",
            "    print(1);",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test implements Comparable<Test> {",
            "  static final Test EOF = new Test_EOF('EOF', 0, 5);",
            "  static final Test DEF = new Test.con1('DEF', 1);",
            "  static final List<Test> values = [EOF, DEF];",
            "  String __name;",
            "  int __ordinal = 0;",
            "  int get ordinal => __ordinal;",
            "  Test.con1(String ___name, int ___ordinal) {",
            "    _jtd_constructor_0_impl(___name, ___ordinal);",
            "  }",
            "  _jtd_constructor_0_impl(String ___name, int ___ordinal) {",
            "    __name = ___name;",
            "    __ordinal = ___ordinal;",
            "  }",
            "  Test.con2(String ___name, int ___ordinal, int p) {",
            "    _jtd_constructor_1_impl(___name, ___ordinal, p);",
            "  }",
            "  _jtd_constructor_1_impl(String ___name, int ___ordinal, int p) {",
            "    __name = ___name;",
            "    __ordinal = ___ordinal;",
            "  }",
            "  void foo() {",
            "    print(1);",
            "  }",
            "  int compareTo(Test other) => __ordinal - other.__ordinal;",
            "  String toString() => __name;",
            "}",
            "class Test_EOF extends Test {",
            "  Test_EOF(String ___name, int ___ordinal, int arg0) : super.con2(___name, ___ordinal, arg0);",
            "  void foo() {",
            "    print(2);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_enum_inner() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public enum MyEnum {",
            "    ONE, TWO;",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "}",
            "class MyEnum implements Comparable<MyEnum> {",
            "  static final MyEnum ONE = new MyEnum('ONE', 0);",
            "  static final MyEnum TWO = new MyEnum('TWO', 1);",
            "  static final List<MyEnum> values = [ONE, TWO];",
            "  final String __name;",
            "  final int __ordinal;",
            "  int get ordinal => __ordinal;",
            "  MyEnum(this.__name, this.__ordinal) {",
            "  }",
            "  int compareTo(MyEnum other) => __ordinal - other.__ordinal;",
            "  String toString() => __name;",
            "}"),
        getFormattedSource(unit));
  }

  public void test_enum_noConstructor() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public enum Test {",
            "  ONE(), TWO;",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test implements Comparable<Test> {",
            "  static final Test ONE = new Test('ONE', 0);",
            "  static final Test TWO = new Test('TWO', 1);",
            "  static final List<Test> values = [ONE, TWO];",
            "  final String __name;",
            "  final int __ordinal;",
            "  int get ordinal => __ordinal;",
            "  Test(this.__name, this.__ordinal) {",
            "  }",
            "  int compareTo(Test other) => __ordinal - other.__ordinal;",
            "  String toString() => __name;",
            "}"),
        getFormattedSource(unit));
  }

  public void test_enum_twoConstructors() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public enum Test {",
            "  ONE(), TWO(2);",
            "  private Test() {",
            "    this(0);",
            "  }",
            "  private Test(int p) {",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    context.addRename("Ltest/Test;.(Ljava/lang/String;II)", "withPriority");
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test implements Comparable<Test> {",
            "  static final Test ONE = new Test.con1('ONE', 0);",
            "  static final Test TWO = new Test.withPriority('TWO', 1, 2);",
            "  static final List<Test> values = [ONE, TWO];",
            "  String __name;",
            "  int __ordinal = 0;",
            "  int get ordinal => __ordinal;",
            "  Test.con1(String ___name, int ___ordinal) {",
            "    _jtd_constructor_0_impl(___name, ___ordinal);",
            "  }",
            "  _jtd_constructor_0_impl(String ___name, int ___ordinal) {",
            "    _jtd_constructor_1_impl(___name, ___ordinal, 0);",
            "  }",
            "  Test.withPriority(String ___name, int ___ordinal, int p) {",
            "    _jtd_constructor_1_impl(___name, ___ordinal, p);",
            "  }",
            "  _jtd_constructor_1_impl(String ___name, int ___ordinal, int p) {",
            "    __name = ___name;",
            "    __ordinal = ___ordinal;",
            "  }",
            "  int compareTo(Test other) => __ordinal - other.__ordinal;",
            "  String toString() => __name;",
            "}"),
        getFormattedSource(unit));
  }

  public void test_forbiddenNames_methods() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  void in() {}",
            "  void with() {}",
            "  void with(int p) {}",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  void in2() {",
            "  }",
            "  void with2() {",
            "  }",
            "  void with3(int p) {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_forbiddenNames_variable() throws Exception {
    File file = setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  static void main() {",
            "    int in = 1;",
            "    int with = 2;",
            "    print(in);",
            "    print(with);",
            "  }",
            "  static void print(int p) {}",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFile(file);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  static void main() {",
            "    int in2 = 1;",
            "    int with2 = 2;",
            "    print(in2);",
            "    print(with2);",
            "  }",
            "  static void print(int p) {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_genericField() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "import java.util.List;",
            "public class Test<T> {",
            "  private List<T> elements;",
            "  void foo() {",
            "    elements.add(null);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    // This will rename "elements" to "_elements".
    // We need to make sure that all references are renamed.
    PropertySemanticProcessor.INSTANCE.process(context, unit);
    assertEquals(
        toString(
            "class Test<T> {",
            "  List<T> _elements;",
            "  void foo() {",
            "    _elements.add(null);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_giveUniqueName_methodField() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  private int value;",
            "  public int value() {",
            "    return value;",
            "  }",
            "  public void bar() {",
            "    value();",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  int value2 = 0;",
            "  int value() => value2;",
            "  void bar() {",
            "    value();",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  /**
   * In Java we can have method parameter "foo" and invoke method named "foo", and parameter will
   * not shadow invoked method. But in Dart it will.
   */
  public void test_giveUniqueName_methodParameter() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public int foo() {",
            "    return 42;",
            "  }",
            "  public int bar1(int foo) {",
            "    return foo(foo);",
            "  }",
            "  public int bar2(int foo) {",
            "    return foo;",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  int foo() => 42;",
            "  int bar1(int foo2) => foo(foo2);",
            "  int bar2(int foo) => foo;",
            "}"),
        getFormattedSource(unit));
  }

  public void test_giveUniqueName_methods() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  static void foo() {}",
            "  static void foo(int p) {}",
            "  static void foo(double p) {}",
            "  static void bar() {",
            "    foo(42);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  static void foo() {",
            "  }",
            "  static void foo2(int p) {",
            "  }",
            "  static void foo3(double p) {",
            "  }",
            "  static void bar() {",
            "    foo2(42);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  /**
   * We should not rename "equals" which comes from Object.
   */
  public void test_giveUniqueName_methods_fromObject() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  static boolean equals(int a, int b) {",
            "    return a < b;",
            "  }",
            "  public boolean equals(Object o) {",
            "    return false;",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  static bool equals2(int a, int b) => a < b;",
            "  bool equals(Object o) => false;",
            "}"),
        getFormattedSource(unit));
  }

  public void test_giveUniqueName_methods_hierarchy() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  void foo() {}",
            "  void foo(int p) {}",
            "  void foo(double p) {}",
            "}",
            ""));
    setFileLines(
        "test/Test2.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test2 extends Test {",
            "  void foo() {}",
            "  void foo(int p) {}",
            "  void foo(double p) {}",
            "}",
            ""));
    setFileLines(
        "test/Test3.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test3 extends Test2 {",
            "  void foo() {}",
            "  void foo(int p) {}",
            "  void foo(double p) {}",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  void foo() {",
            "  }",
            "  void foo2(int p) {",
            "  }",
            "  void foo3(double p) {",
            "  }",
            "}",
            "class Test2 extends Test {",
            "  void foo() {",
            "  }",
            "  void foo2(int p) {",
            "  }",
            "  void foo3(double p) {",
            "  }",
            "}",
            "class Test3 extends Test2 {",
            "  void foo() {",
            "  }",
            "  void foo2(int p) {",
            "  }",
            "  void foo3(double p) {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_giveUniqueName_variableInitializer() throws Exception {
    File file = setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  static int foo() {return 42;}",
            "  static void bar() {",
            "    int foo = foo();",
            "    baz(foo);",
            "  }",
            "  static void baz(int p) {}",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFile(file);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  static int foo() => 42;",
            "  static void bar() {",
            "    int foo2 = foo();",
            "    baz(foo2);",
            "  }",
            "  static void baz(int p) {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_giveUniqueName_withStatic() throws Exception {
    setFileLines(
        "test/Super.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Super {",
            "  static int add(int a, int b) {return a + b;}",
            "}",
            ""));
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Sub extends Super {",
            "  int add(int a) {return add(a, 2);}",
            "  void main() {",
            "    add(1, 2);",
            "    add(3);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Super {",
            "  static int add(int a, int b) => a + b;",
            "}",
            "class Sub extends Super {",
            "  int add2(int a) => Super.add(a, 2);",
            "  void main() {",
            "    Super.add(1, 2);",
            "    add2(3);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_importStatic_field() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  public static final int ZERO;",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "import static test.A.ZERO;",
            "public class B {",
            "  int myInstanceField;",
            "  static int myStaticField;",
            "  void main() {",
            "    print(A.ZERO);",
            "    print(ZERO);",
            "    this.myInstanceField = 1;",
            "    myInstanceField = 2;",
            "    myInstanceField = ZERO;",
            "    myInstanceField = ZERO + 1;",
            "    myInstanceField = 2 + ZERO;",
            "    myInstanceField = 1 + 2 + 3 + ZERO;",
            "    myStaticField = 3;",
            "  }",
            "  int main2() {",
            "    return ZERO;",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  static int ZERO = 0;",
            "}",
            "class B {",
            "  int myInstanceField = 0;",
            "  static int myStaticField = 0;",
            "  void main() {",
            "    print(A.ZERO);",
            "    print(A.ZERO);",
            "    this.myInstanceField = 1;",
            "    myInstanceField = 2;",
            "    myInstanceField = A.ZERO;",
            "    myInstanceField = A.ZERO + 1;",
            "    myInstanceField = 2 + A.ZERO;",
            "    myInstanceField = 1 + 2 + 3 + A.ZERO;",
            "    myStaticField = 3;",
            "  }",
            "  int main2() => A.ZERO;",
            "}"),
        getFormattedSource(unit));
  }

  public void test_importStatic_method() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  public static int zero() {return 0;}",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "import static test.A.zero;",
            "public class B {",
            "  public static int one() {return 1;}",
            "  void main() {",
            "    print(A.zero());",
            "    print(zero());",
            "    print(one());",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  static int zero() => 0;",
            "}",
            "class B {",
            "  static int one() => 1;",
            "  void main() {",
            "    print(A.zero());",
            "    print(A.zero());",
            "    print(one());",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  /**
   * https://code.google.com/p/dart/issues/detail?id=8854
   */
  public void test_initializePrimitiveArrays() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  void main() {",
            "    Object res;",
            "    res = new boolean[5];",
            "    res = new byte[5];",
            "    res = new char[5];",
            "    res = new short[5];",
            "    res = new int[5];",
            "    res = new long[5];",
            "    res = new float[5];",
            "    res = new double[5];",
            "    res = new Object[5];",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  void main() {",
            "    Object res;",
            "    res = new List<bool>.filled(5, false);",
            "    res = new List<int>.filled(5, 0);",
            "    res = new List<int>.filled(5, 0);",
            "    res = new List<int>.filled(5, 0);",
            "    res = new List<int>.filled(5, 0);",
            "    res = new List<int>.filled(5, 0);",
            "    res = new List<double>.filled(5, 0.0);",
            "    res = new List<double>.filled(5, 0.0);",
            "    res = new List<Object>(5);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_redirectingConstructorInvocation() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public Test() {",
            "    this(42);",
            "  }",
            "  public Test(int p) {",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  Test() {",
            "    _jtd_constructor_0_impl();",
            "  }",
            "  _jtd_constructor_0_impl() {",
            "    _jtd_constructor_1_impl(42);",
            "  }",
            "  Test.con1(int p) {",
            "    _jtd_constructor_1_impl(p);",
            "  }",
            "  _jtd_constructor_1_impl(int p) {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_statementSwitch_enum() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public enum A {",
            "  ONE, TWO;",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B {",
            "  void main(A p) {",
            "    switch (p) {",
            "      case ONE:",
            "        print(1);",
            "        break;",
            "      case TWO:",
            "        print(2);",
            "        break;",
            "    }",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A implements Comparable<A> {",
            "  static final A ONE = new A('ONE', 0);",
            "  static final A TWO = new A('TWO', 1);",
            "  static final List<A> values = [ONE, TWO];",
            "  final String __name;",
            "  final int __ordinal;",
            "  int get ordinal => __ordinal;",
            "  A(this.__name, this.__ordinal) {",
            "  }",
            "  int compareTo(A other) => __ordinal - other.__ordinal;",
            "  String toString() => __name;",
            "}",
            "class B {",
            "  void main(A p) {",
            "    while (true) {",
            "      if (p == A.ONE) {",
            "        print(1);",
            "      } else if (p == A.TWO) {",
            "        print(2);",
            "      }",
            "      break;",
            "    }",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_superConstructorInvocation() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  A(int p) {}",
            "  A(double p) {}",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B extends A {",
            "  B() {",
            "    super(1.0);",
            "    print(2);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  A.con1(int p) {",
            "    _jtd_constructor_0_impl(p);",
            "  }",
            "  _jtd_constructor_0_impl(int p) {",
            "  }",
            "  A.con2(double p) {",
            "    _jtd_constructor_1_impl(p);",
            "  }",
            "  _jtd_constructor_1_impl(double p) {",
            "  }",
            "}",
            "class B extends A {",
            "  B() : super.con2(1.0) {",
            "    print(2);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_superMethodInvocation() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  void test(int p) {}",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B extends A {",
            "  void test() {",
            "    print(1);",
            "    super.test(2);",
            "    print(3);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  void test(int p) {",
            "  }",
            "}",
            "class B extends A {",
            "  void test() {",
            "    print(1);",
            "    super.test(2);",
            "    print(3);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_thisInFieldInitializer_noConstructor() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  Object foo = this;",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(toString(//
        "class Test {",
        "  Object foo;",
        "  Test() {",
        "    this.foo = this;",
        "  }",
        "}"), getFormattedSource(unit));
  }

  public void test_thisInFieldInitializer_singleConstructor() throws Exception {
    setFileLines(
        "test/Super.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Super {",
            "  public Super(int p) {",
            "  }",
            "}",
            ""));
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  Object foo = this;",
            "  public Test(int p) {",
            "    super(p);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Super {",
            "  Super(int p) {",
            "  }",
            "}",
            "class Test {",
            "  Object foo;",
            "  Test(int p) : super(p) {",
            "    this.foo = this;",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_thisInFieldInitializer_twoConstructors() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  Object foo = this;",
            "  Object bar = this;",
            "  public Test() {",
            "    print(1);",
            "  }",
            "  public Test(int p) {",
            "    print(2);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  Object foo;",
            "  Object bar;",
            "  Test() {",
            "    _jtd_constructor_0_impl();",
            "  }",
            "  _jtd_constructor_0_impl() {",
            "    this.foo = this;",
            "    this.bar = this;",
            "    print(1);",
            "  }",
            "  Test.con1(int p) {",
            "    _jtd_constructor_1_impl(p);",
            "  }",
            "  _jtd_constructor_1_impl(int p) {",
            "    this.foo = this;",
            "    this.bar = this;",
            "    print(2);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_tryCatch_Exception() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class Test {",
            "  void main() {",
            "    try {",
            "    } catch (Exception e) {",
            "    }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  void main() {",
            "    try {",
            "    } catch (e) {",
            "    }",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_typeVariable_inGenericMethod() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "}"));
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "import java.util.ArrayList;",
            "public class Test {",
            "  public static <T extends A> ArrayList<T> foo() {",
            "    return new ArrayList<T>();",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "}",
            "class Test {",
            "  static ArrayList foo() => new ArrayList();",
            "}"),
        getFormattedSource(unit));
  }

  public void test_varArgs() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  void test(int errorCode, Object ...args) {",
            "  }",
            "  void main() {",
            "    test(-1);",
            "    test(-1, 2, 3.0);",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  void test(int errorCode, List<Object> args) {",
            "  }",
            "  void main() {",
            "    test(-1, []);",
            "    test(-1, [2, 3.0]);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_varArgs_alreadyArray() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  String[] EMPTY = {};",
            "  void test(String ...args) {",
            "  }",
            "  void main() {",
            "    test(EMPTY);",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  List<String> EMPTY = [];",
            "  void test(List<String> args) {",
            "  }",
            "  void main() {",
            "    test(EMPTY);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_varArgs_alreadyArray_constructor() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  A(Object o, int ...args) {",
            "  }",
            "  A(int ...args) {",
            "    this(args);",
            "  }",
            "  void main() {",
            "    new A(null, 1, 2, 3);",
            "    new A(1, 2, 3);",
            "  }",
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  A.con1(Object o, List<int> args) {",
            "    _jtd_constructor_0_impl(o, args);",
            "  }",
            "  _jtd_constructor_0_impl(Object o, List<int> args) {",
            "  }",
            "  A.con2(List<int> args) {",
            "    _jtd_constructor_1_impl(args);",
            "  }",
            "  _jtd_constructor_1_impl(List<int> args) {",
            "    _jtd_constructor_1_impl(args);",
            "  }",
            "  void main() {",
            "    new A.con1(null, [1, 2, 3]);",
            "    new A.con1(1, [2, 3]);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }
}
