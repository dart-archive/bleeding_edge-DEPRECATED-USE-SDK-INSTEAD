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
import com.google.dart.java2dart.processor.RenameConstructorsSemanticProcessor;

import java.io.File;

/**
 * Test for general Java semantics to Dart translation.
 */
public class SemanticTest extends AbstractSemanticTest {
  private final Context context = new Context();
  private CompilationUnit unit;

  public void test_anonymousClass_extendsClass() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public Test(int i) {",
            "  }",
            "  public boolean foo() {",
            "    return false;",
            "  }",
            "  public static main() {",
            "    Test v = new Test(42) {",
            "      public boolean foo() {",
            "        return true;",
            "      }",
            "    };",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  Test(int i);",
            "  bool foo() => false;",
            "  static main() {",
            "    Test v = new Test_main(42);",
            "  }",
            "}",
            "class Test_main extends Test {",
            "  Test_main(int arg0) : super(arg0);",
            "  bool foo() => true;",
            "}"),
        getFormattedSource(unit));
  }

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
            "      public int foo() {",
            "        return myValue;",
            "      }",
            "    };",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  Test(int i, double f);",
            "  static main() {",
            "    int myValue = 5;",
            "    Test v = new Test_main(1, 2.3, myValue);",
            "  }",
            "}",
            "class Test_main extends Test {",
            "  int myValue = 0;",
            "  Test_main(int arg0, double arg1, this.myValue) : super(arg0, arg1);",
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "abstract class MyInterface {",
            "}",
            "class Test {",
            "  static main() {",
            "    MyInterface v = new MyInterface_Test_main();",
            "  }",
            "}",
            "class MyInterface_Test_main implements MyInterface {",
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
            "  public void onError();",
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
            "      public void onError() {",
            "        hasErrors[0] = false;",
            "        hasErrors[0] = true;",
            "      }",
            "    };",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "abstract class ErrorListener {",
            "  void onError();",
            "}",
            "class Test {",
            "  static void main() {",
            "    List<bool> hasErrors = [false];",
            "    ErrorListener v = new ErrorListener_Test_main(hasErrors);",
            "  }",
            "}",
            "class ErrorListener_Test_main implements ErrorListener {",
            "  List<bool> hasErrors;",
            "  ErrorListener_Test_main(this.hasErrors);",
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
            "  public void onError();",
            "}"));
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  boolean hasErrors;",
            "  static boolean staticField;",
            "  public void foo() {};",
            "  public void main() {",
            "    ErrorListener v = new ErrorListener() {",
            "      public void onError() {",
            "        foo();",
            "        hasErrors = true;",
            "        staticField = true;",
            "      }",
            "    };",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "abstract class ErrorListener {",
            "  void onError();",
            "}",
            "class Test {",
            "  bool _hasErrors = false;",
            "  static bool _staticField = false;",
            "  void foo() {",
            "  }",
            "  void main() {",
            "    ErrorListener v = new ErrorListener_Test_main(this);",
            "  }",
            "}",
            "class ErrorListener_Test_main implements ErrorListener {",
            "  final Test Test_this;",
            "  ErrorListener_Test_main(this.Test_this);",
            "  void onError() {",
            "    Test_this.foo();",
            "    Test_this._hasErrors = true;",
            "    Test._staticField = true;",
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
            "  public static void foo() {}",
            "}",
            ""));
    setFileLines(
        "test/Second.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Second {",
            "  public static void bar() {",
            "    Main.foo();",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "    public B() {}",
            "  }",
            "  public void test1(B p) {}",
            "  public void test2(A.B p) {}",
            "  public void test3() {",
            "    new B();",
            "    new A.B();",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
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
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner_enclosingSuperClass() throws Exception {
    setFileLines(
        "test/S.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class S {",
            "  public void outerMethod() {}",
            "}"));
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A extends S {",
            "  public class B {",
            "    public B() {}",
            "    public void test() {",
            "      outerMethod();",
            "    }",
            "  }",
            "  public B test2() {",
            "    return new B();",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(
        toString(
            "class A extends S {",
            "  A_B test2() => new A_B(this);",
            "}",
            "class A_B {",
            "  final A A_this;",
            "  A_B(this.A_this);",
            "  void test() {",
            "    A_this.outerMethod();",
            "  }",
            "}",
            "class S {",
            "  void outerMethod() {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner_enclosingThisQualifier() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public class B {",
            "    public void test() {",
            "      A.this.outerField = 5;",
            "      A.this.outerMethod();",
            "      test3(A.this);",
            "    }",
            "  }",
            "  int outerField;",
            "  public void outerMethod() {}",
            "  public B test2() {",
            "    return new B();",
            "  }",
            "  public void test3(A) {",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(
        toString(
            "class A {",
            "  int _outerField = 0;",
            "  void outerMethod() {",
            "  }",
            "  A_B test2() => new A_B(this);",
            "  void test3() {",
            "  }",
            "}",
            "class A_B {",
            "  final A A_this;",
            "  A_B(this.A_this);",
            "  void test() {",
            "    A_this._outerField = 5;",
            "    A_this.outerMethod();",
            "    A_this.test3(A_this);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner_referenceEnclosingClassField_array() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public class B {",
            "    public int test() {",
            "      return values.length;",
            "    }",
            "  }",
            "  int[] values;",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(
        toString(
            "class A {",
            "  List<int> _values;",
            "}",
            "class A_B {",
            "  final A A_this;",
            "  A_B(this.A_this);",
            "  int test() => A_this._values.length;",
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
            "    public void foo() {}",
            "  }",
            "  public void test(final B p) {",
            "    return new Object() {",
            "      public void main() {",
            "        p.foo();",
            "      }",
            "    };",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(
        toString(
            "class A {",
            "  void test(A_B p) => new Object_A_test(p);",
            "}",
            "class A_B {",
            "  void foo() {",
            "  }",
            "}",
            "class Object_A_test extends Object {",
            "  A_B p;",
            "  Object_A_test(this.p) : super();",
            "  void main() {",
            "    p.foo();",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner_static_generic() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A<T> {",
            "  public static class B<T> {",
            "    public B(A<T> a) {",
            "    }",
            "    public int getValue() {",
            "      return 42;",
            "    }",
            "  }",
            "  int test() {",
            "    B<T> b = new B<T>(this);",
            "    return b.getValue();",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(
        toString(
            "class A<T> {",
            "  int _test() {",
            "    A_B<T> b = new A_B<T>(this);",
            "    return b.getValue();",
            "  }",
            "}",
            "class A_B<T> {",
            "  A_B(A<T> a);",
            "  int getValue() => 42;",
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(
        toString(
            "class A {",
            "}",
            "class A_B {",
            "  static int _ZERO = 0;",
            "  int getValue() => _ZERO + 1;",
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
            "  public void test1(B p) {}",
            "  public A.B test2() {",
            "    return new A.B() {};",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(
        toString(
            "class A {",
            "  void test1(A_B p) {",
            "  }",
            "  A_B test2() => new A_B_A_test2();",
            "}",
            "abstract class A_B {",
            "}",
            "class A_B_A_test2 implements A_B {",
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
            "  public void test1(B p) {}",
            "  public A.B test2() {",
            "    return new A.B() {};",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(
        toString(
            "class A {",
            "  void test1(A_B p) {",
            "  }",
            "  A_B test2() => new A_B_A_test2();",
            "}",
            "class A_B {",
            "}",
            "class A_B_A_test2 extends A_B {",
            "}"),
        getFormattedSource(unit));
  }

  public void test_classInner6() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public class B {",
            "    public B(int p) {}",
            "    public void test() {",
            "      outerMethod();",
            "    }",
            "  }",
            "  public void outerMethod() {}",
            "  public B test2() {",
            "    return new B(42);",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(
        toString(
            "class A {",
            "  void outerMethod() {",
            "  }",
            "  A_B test2() => new A_B(this, 42);",
            "}",
            "class A_B {",
            "  final A A_this;",
            "  A_B(this.A_this, int p);",
            "  void test() {",
            "    A_this.outerMethod();",
            "  }",
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
            "  public int foo;",
            "  public static void foo() {}",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B {",
            "  public static void bar() {",
            "    print(A.foo);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    context.addRename("Ltest/A;.foo", "myField");
    translate();
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
            "  public static void foo() {}",
            "  public static void foo(int p) {}",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B {",
            "  public static void bar() {",
            "    A.foo(42);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    context.addRename("Ltest/A;.foo(I)", "fooWithInt");
    translate();
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

  public void test_constructor_defaultNames() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public Test() {",
            "    print(0);",
            "  }",
            "  public Test(int p) {",
            "    print(1);",
            "  }",
            "  public Test(double p) {",
            "    print(2);",
            "  }",
            "  public static void main() {",
            "    new Test();",
            "    new Test(2);",
            "    new Test(3.0);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  Test() {",
            "    print(0);",
            "  }",
            "  Test.con1(int p) {",
            "    print(1);",
            "  }",
            "  Test.con2(double p) {",
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

  public void test_constructor_typeArgs() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test<T> {",
            "  public Test() {",
            "    print(0);",
            "  }",
            "  public Test(int p) {",
            "    print(1);",
            "  }",
            "  public static void main() {",
            "    new Test<String>();",
            "    new Test<String>(2);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test<T> {",
            "  Test() {",
            "    print(0);",
            "  }",
            "  Test.con1(int p) {",
            "    print(1);",
            "  }",
            "  static void main() {",
            "    new Test<String>();",
            "    new Test<String>.con1(2);",
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
            "  public boolean booleanF;",
            "  public byte byteF;",
            "  public char charF;",
            "  public short shortF;",
            "  public int intF;",
            "  public long longF;",
            "  public float floatF;",
            "  public double doubleF;",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "    public void foo() {",
            "      print(2);",
            "    }",
            "  }, DEF;",
            "  private Test() {",
            "  }",
            "  private Test(int p) {",
            "  }",
            "  public void foo() {",
            "    print(1);",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test extends Enum<Test> {",
            "  static const Test EOF = const Test_EOF('EOF', 0, 5);",
            "  static const Test DEF = const Test.con1('DEF', 1);",
            "  static const List<Test> values = const [EOF, DEF];",
            "  const Test.con1(String name, int ordinal) : super(name, ordinal);",
            "  const Test.con2(String name, int ordinal, int p) : super(name, ordinal);",
            "  void foo() {",
            "    print(1);",
            "  }",
            "}",
            "class Test_EOF extends Test {",
            "  const Test_EOF(String name, int ordinal, int arg0) : super.con2(name, ordinal, arg0);",
            "  void foo() {",
            "    print(2);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_enum_equals() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public static enum Direction {",
            "    LEFT, RIGHT;",
            "  }",
            "  ",
            "  public void test(Object a, Direction b) {",
            "    print(a == Direction.LEFT);",
            "    print(a != Direction.RIGHT);",
            "    print(b == Direction.LEFT);",
            "    print(b != Direction.RIGHT);",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class A {",
            "  void test(Object a, Direction b) {",
            "    print(identical(a, Direction.LEFT));",
            "    print(!identical(a, Direction.RIGHT));",
            "    print(b == Direction.LEFT);",
            "    print(b != Direction.RIGHT);",
            "  }",
            "}",
            "class Direction extends Enum<Direction> {",
            "  static const Direction LEFT = const Direction('LEFT', 0);",
            "  static const Direction RIGHT = const Direction('RIGHT', 1);",
            "  static const List<Direction> values = const [LEFT, RIGHT];",
            "  const Direction(String name, int ordinal) : super(name, ordinal);",
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "}",
            "class MyEnum extends Enum<MyEnum> {",
            "  static const MyEnum ONE = const MyEnum('ONE', 0);",
            "  static const MyEnum TWO = const MyEnum('TWO', 1);",
            "  static const List<MyEnum> values = const [ONE, TWO];",
            "  const MyEnum(String name, int ordinal) : super(name, ordinal);",
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test extends Enum<Test> {",
            "  static const Test ONE = const Test('ONE', 0);",
            "  static const Test TWO = const Test('TWO', 1);",
            "  static const List<Test> values = const [ONE, TWO];",
            "  const Test(String name, int ordinal) : super(name, ordinal);",
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
            "    print(p);",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test extends Enum<Test> {",
            "  static const Test ONE = const Test.con1('ONE', 0);",
            "  static const Test TWO = const Test.con2('TWO', 1, 2);",
            "  static const List<Test> values = const [ONE, TWO];",
            "  const Test.con1(String name, int ordinal) : this.con2(name, ordinal, 0);",
            "  const Test.con2(String name, int ordinal, int p) : super(name, ordinal) {",
            "    print(p);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_expression_equals() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class Test {",
            "  boolean testObject(Object a, Object b) {",
            "    return a == b;",
            "  }",
            "  boolean testNull(Object p) {",
            "    return p == null;",
            "  }",
            "  boolean testBool(Object p, boolean t) {",
            "    return p == true || p == t;",
            "  }",
            "  boolean testChar(Object p, char t) {",
            "    return p == '0' || p == t;",
            "  }",
            "  boolean testByte(Object p, byte t) {",
            "    return p == (byte) 1 || p == t;",
            "  }",
            "  boolean testInt(Object p, int t) {",
            "    return p == 2 || p == t;",
            "  }",
            "  boolean testLong(Object p, long t) {",
            "    return p == 3L || p == t;",
            "  }",
            "  boolean testFloat(Object p, float t) {",
            "    return p == 4.0f || p == t;",
            "  }",
            "  boolean testDouble(Object p, double t) {",
            "    return p == 5.0d || p == t;",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  bool _testObject(Object a, Object b) => identical(a, b);",
            "  bool _testNull(Object p) => p == null;",
            "  bool _testBool(Object p, bool t) => p == true || p == t;",
            "  bool _testChar(Object p, int t) => p == 0x30 || p == t;",
            "  bool _testByte(Object p, int t) => p == 1 || p == t;",
            "  bool _testInt(Object p, int t) => p == 2 || p == t;",
            "  bool _testLong(Object p, int t) => p == 3 || p == t;",
            "  bool _testFloat(Object p, double t) => p == 4.0 || p == t;",
            "  bool _testDouble(Object p, double t) => p == 5.0 || p == t;",
            "}"),
        getFormattedSource(unit));
  }

  /**
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=9845
   */
  public void test_expression_equals_Class() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public boolean testA(Class a, Class b) {",
            "    return a == b;",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(toString(//
        "class A {",
        "  bool testA(Class a, Class b) => a == b;",
        "}"), getFormattedSource(unit));
  }

  public void test_forbiddenNames_forEach() throws Exception {
    File file = setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public static void main(String [] vars) {",
            "    for (String var: vars) {",
            "      print(var);",
            "    }",
            "  }",
            "  public static void print(String p) {}",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFile(file);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  static void main(List<String> vars) {",
            "    for (String var2 in vars) {",
            "      print(var2);",
            "    }",
            "  }",
            "  static void print(String p) {",
            "  }",
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
            "  public void in() {}",
            "  public void with() {}",
            "  public void with(int p) {}",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "  public static void main() {",
            "    int in = 1;",
            "    int with = 2;",
            "    print(in);",
            "    print(with);",
            "  }",
            "  public static void print(int p) {}",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFile(file);
    translate();
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
            "  public void foo() {",
            "    elements.add(null);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    // This will rename "elements" to "_elements".
    // We need to make sure that all references are renamed.
    new PropertySemanticProcessor(context).process(unit);
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  int _value = 0;",
            "  int value() => _value;",
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  int foo() => 42;",
            "  int bar1(int foo) => this.foo(foo);",
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
            "  public static void foo() {}",
            "  public static void foo(int p) {}",
            "  public static void foo(double p) {}",
            "  public static void bar() {",
            "    foo(42);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "  public static boolean equals(int a, int b) {",
            "    return a < b;",
            "  }",
            "  public boolean equals(Object o) {",
            "    return false;",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "  public void foo() {}",
            "  public void foo(int p) {}",
            "  public void foo(double p) {}",
            "}",
            ""));
    setFileLines(
        "test/Test2.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test2 extends Test {",
            "  public void foo() {}",
            "  public void foo(int p) {}",
            "  public void foo(double p) {}",
            "}",
            ""));
    setFileLines(
        "test/Test3.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test3 extends Test2 {",
            "  public void foo() {}",
            "  public void foo(int p) {}",
            "  public void foo(double p) {}",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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

  public void test_giveUniqueName_methods_hierarchy_overloaded() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  <E> public E void foo(E p) {",
            "    return null;",
            "  }",
            "}",
            ""));
    setFileLines(
        "test/Test2.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test2 extends Test {",
            "  public int foo(int p) {",
            "    return 0;",
            "  }",
            "  public void main() {",
            "    foo(this);",
            "    foo(42);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  void foo(Object p) => null;",
            "}",
            "class Test2 extends Test {",
            "  int foo2(int p) => 0;",
            "  void main() {",
            "    foo(this);",
            "    foo2(42);",
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
            "  public static int add(int a, int b) {return a + b;}",
            "}",
            ""));
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Sub extends Super {",
            "  public int add(int a) {return add(a, 2);}",
            "  public void main() {",
            "    add(1, 2);",
            "    add(3);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "  public int myInstanceField;",
            "  public static int myStaticField;",
            "  public void main() {",
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
            "  public int main2() {",
            "    return ZERO;",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "  public void main() {",
            "    print(A.zero());",
            "    print(zero());",
            "    print(one());",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "  public void main() {",
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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

  public void test_localVariableShadow_qualified() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public int element;",
            "  public void main() {",
            "    int v = this.element;",
            "    int element = 42;",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  int element = 0;",
            "  void main() {",
            "    int v = this.element;",
            "    int element = 42;",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_localVariableShadow_renamedProperty_catch() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  private int test;",
            "  int getTest() { return test; }",
            "  public void main() {",
            "    try {",
            "    } catch (Exception test) {",
            "      int v = getTest();",
            "    }",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    new PropertySemanticProcessor(context).process(unit);
    context.applyLocalVariableSemanticChanges(unit);
    assertEquals(
        toString(
            "class Test {",
            "  final int test = 0;",
            "  void main() {",
            "    try {",
            "    } catch (test) {",
            "      int v = this.test;",
            "    }",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_localVariableShadow_renamedProperty_var() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  private int test;",
            "  public int getTest() { return test; }",
            "  public void main(int test) {",
            "    int v = getTest();",
            "    int element = 42;",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    new PropertySemanticProcessor(context).process(unit);
    context.applyLocalVariableSemanticChanges(unit);
    assertEquals(
        toString(
            "class Test {",
            "  final int test = 0;",
            "  void main(int test) {",
            "    int v = this.test;",
            "    int element = 42;",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_localVariableShadow_shadowedField() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public int element;",
            "  public void main() {",
            "    int v = element;",
            "    int element = 42;",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  int element = 0;",
            "  void main() {",
            "    int v = this.element;",
            "    int element = 42;",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_localVariableShadow_shadowedMethod() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public void element() {}",
            "  public void main() {",
            "    element();",
            "    int element = 42;",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  void element() {",
            "  }",
            "  void main() {",
            "    this.element();",
            "    int element = 42;",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_localVariableShadow_staticMethod() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public static void element() {}",
            "  public void main() {",
            "    element();",
            "    int element = 42;",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  static void element() {",
            "  }",
            "  void main() {",
            "    Test.element();",
            "    int element = 42;",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_private_field() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  private int field = 42;",
            "  public int foo() {",
            "    return field;",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(toString(//
        "class A {",
        "  int _field = 42;",
        "  int foo() => _field;",
        "}"), getFormattedSource(unit));
  }

  public void test_private_method() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  private void test() {}",
            "  private void foo() {",
            "    test();",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
    assertEquals(toString(//
        "class A {",
        "  void _test() {",
        "  }",
        "  void _foo() {",
        "    _test();",
        "  }",
        "}"), getFormattedSource(unit));
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(toString(//
        "class Test {",
        "  Test() : this.con1(42);",
        "  Test.con1(int p);",
        "}"), getFormattedSource(unit));
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
            "  public void main(A p) {",
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class A extends Enum<A> {",
            "  static const A ONE = const A('ONE', 0);",
            "  static const A TWO = const A('TWO', 1);",
            "  static const List<A> values = const [ONE, TWO];",
            "  const A(String name, int ordinal) : super(name, ordinal);",
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class A {",
            "  A.con1(int p);",
            "  A.con2(double p);",
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
            "  public void test(int p) {}",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B extends A {",
            "  public void test() {",
            "    print(1);",
            "    super.test(2);",
            "    print(3);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class A {",
            "  void test(int p) {",
            "  }",
            "}",
            "class B extends A {",
            "  void test2() {",
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
            "  public Object foo = this;",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "  public Object foo = this;",
            "  public Test(int p) {",
            "    super(p);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Super {",
            "  Super(int p);",
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
            "  public Object foo = this;",
            "  public Object bar = this;",
            "  public Test() {",
            "    print(1);",
            "  }",
            "  public Test(int p) {",
            "    print(2);",
            "  }",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  Object foo;",
            "  Object bar;",
            "  Test() {",
            "    this.foo = this;",
            "    this.bar = this;",
            "    print(1);",
            "  }",
            "  Test.con1(int p) {",
            "    this.foo = this;",
            "    this.bar = this;",
            "    print(2);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_tryCatch_Throwable() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class Test {",
            "  public void main() {",
            "    try {",
            "    } catch (Throwable e) {",
            "    }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    translate();
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
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "  public void test(int errorCode, Object ...args) {",
            "  }",
            "  public void main() {",
            "    test(-1);",
            "    test(-1, 2, 3.0);",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "  public String[] EMPTY = {};",
            "  public void test(String ...args) {",
            "  }",
            "  public void main() {",
            "    test(EMPTY);",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
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
            "public class Test {",
            "  public Test(String o, int ...args) {",
            "  }",
            "  public Test(int ...args) {",
            "    this(null, args);",
            "  }",
            "  public void main(int ...args) {",
            "    new Test(null, 1, 2, 3);",
            "    new Test(1, 2, 3);",
            "    new Test(null, args);",
            "    new Test(args);",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  Test.con1(String o, List<int> args);",
            "  Test.con2(List<int> args) : this.con1(null, args);",
            "  void main(List<int> args) {",
            "    new Test.con1(null, [1, 2, 3]);",
            "    new Test.con2([1, 2, 3]);",
            "    new Test.con1(null, args);",
            "    new Test.con2(args);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_varArgs_alreadyArray_superConstructor() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  private static class A {",
            "    A(int ...args) {",
            "    }",
            "  }",
            "  private static class B extends A {",
            "    B(int ...args) {",
            "      super(args);",
            "    }",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "}",
            "class Test_A {",
            "  Test_A(List<int> args);",
            "}",
            "class Test_B extends Test_A {",
            "  Test_B(List<int> args) : super(args);",
            "}"),
        getFormattedSource(unit));
  }

  public void test_variableInitializer_qualifiedReference() throws Exception {
    File file = setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public int foo() {return 42;}",
            "  public int bar;",
            "  public static void mainA() {",
            "    int foo = this.foo();",
            "    process(foo);",
            "  }",
            "  public static void mainB() {",
            "    int bar = this.bar;",
            "    process(bar);",
            "  }",
            "  public void process(int x) {}",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFile(file);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  int foo() => 42;",
            "  int bar = 0;",
            "  static void mainA() {",
            "    int foo = this.foo();",
            "    process(foo);",
            "  }",
            "  static void mainB() {",
            "    int bar = this.bar;",
            "    process(bar);",
            "  }",
            "  void process(int x) {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_variableInitializer_useThisQualifier() throws Exception {
    File file = setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public static int foo() {return 42;}",
            "  public static void barA() {",
            "    int foo = foo();",
            "    baz(foo);",
            "  }",
            "  public static void barB() {",
            "    int foo = foo();",
            "    baz(foo);",
            "  }",
            "  public static void baz(int p) {}",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFile(file);
    translate();
    assertEquals(
        toString(
            "class Test {",
            "  static int foo() => 42;",
            "  static void barA() {",
            "    int foo = Test.foo();",
            "    baz(foo);",
            "  }",
            "  static void barB() {",
            "    int foo = Test.foo();",
            "    baz(foo);",
            "  }",
            "  static void baz(int p) {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_variableInitializer_useThisQualifier_propertyReference() throws Exception {
    File file = setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  public int getFoo() {return 42;}",
            "  public static void main() {",
            "    int foo = getFoo();",
            "    process(foo);",
            "  }",
            "  public void process(int x) {}",
            "}",
            ""));
    context.addSourceFolder(tmpFolder);
    context.addSourceFile(file);
    translate();
    // convert to properties and run variable checks again
    new PropertySemanticProcessor(context).process(unit);
    context.applyLocalVariableSemanticChanges(unit);
    // verify
    assertEquals(
        toString(
            "class Test {",
            "  int get foo => 42;",
            "  static void main() {",
            "    int foo = this.foo;",
            "    process(foo);",
            "  }",
            "  void process(int x) {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  private void translate() throws Exception {
    unit = context.translate();
    context.ensureUniqueClassMemberNames();
    context.applyLocalVariableSemanticChanges(unit);
    new RenameConstructorsSemanticProcessor(context).process(unit);
  }
}
