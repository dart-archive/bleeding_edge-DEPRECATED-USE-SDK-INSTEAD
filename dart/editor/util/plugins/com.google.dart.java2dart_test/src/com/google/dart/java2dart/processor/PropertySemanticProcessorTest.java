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
 * Test for {@link PropertySemanticProcessor}.
 */
public class PropertySemanticProcessorTest extends SemanticProcessorTest {
  public void test_field_BAD_accessedInConstructorInvocation() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public class Super {",
        "    public Super(int bar) {",
        "    }",
        "  }",
        "  public class Sub extends Super {",
        "    private int foo;",
        "    public int getFoo() {",
        "      return foo;",
        "    }",
        "    public void setFoo(int foo) {",
        "      this.foo = foo;",
        "    }",
        "    public Sub(int foo) {",
        "      super(foo + 1);",
        "      this.foo = foo;",
        "    }",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "}",
        "class Test_Super {",
        "  Test_Super(int bar);",
        "}",
        "class Test_Sub extends Test_Super {",
        "  int foo = 0;",
        "  Test_Sub(int foo) : super(foo + 1) {",
        "    this.foo = foo;",
        "  }",
        "}");
  }

  public void test_field_BAD_noSetter_butCannotBeFinal() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public int getFoo() {",
        "    return foo;",
        "  }",
        "  public Test(int foo) {",
        "     this.foo = foo;",
        "  }",
        "  public Test(boolean bar) {",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  int _foo = 0;",
        "  int get foo => _foo;",
        "  Test.con1(int foo) {",
        "    this._foo = foo;",
        "  }",
        "  Test.con2(bool bar);",
        "}");
  }

  public void test_field_BAD_notSameGetterSetterFields() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int fooA;",
        "  private int fooB;",
        "  public int getFoo() {",
        "    return fooA;",
        "  }",
        "  public void setFoo(int foo) {",
        "    this.fooB = foo;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  int _fooA = 0;",
        "  int _fooB = 0;",
        "  int get foo => _fooA;",
        "  void set foo(int foo) {",
        "    this._fooB = foo;",
        "  }",
        "}");
  }

  public void test_field_BAD_onlySetter() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public void setFoo(int foo) {",
        "    this.foo = foo;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  int _foo = 0;",
        "  void set foo(int foo) {",
        "    this._foo = foo;",
        "  }",
        "}");
  }

  public void test_field_BAD_overriden_getterSetter() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public class Super {",
        "    private int foo;",
        "    public int getFoo() {",
        "      return foo;",
        "    }",
        "    public void setFoo(int foo) {",
        "      this.foo = foo;",
        "    }",
        "  }",
        "  public class Sub extends Super {",
        "    private int foo;",
        "    public int getFoo() {",
        "      return foo;",
        "    }",
        "    public void setFoo(int foo) {",
        "      this.foo = foo;",
        "    }",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "}",
        "class Test_Super {",
        "  int _foo = 0;",
        "  int get foo => _foo;",
        "  void set foo(int foo) {",
        "    this._foo = foo;",
        "  }",
        "}",
        "class Test_Sub extends Test_Super {",
        "  int foo = 0;",
        "}");
  }

  public void test_field_BAD_overriden_setter() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public class Super {",
        "    private int foo;",
        "    public int getFoo() {",
        "      return foo;",
        "    }",
        "    public void setFoo(int foo) {",
        "      this.foo = foo;",
        "    }",
        "  }",
        "  public class Sub extends Super {",
        "    private int foo;",
        "    public void setFoo(int foo) {",
        "      this.foo = foo;",
        "    }",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "}",
        "class Test_Super {",
        "  int _foo = 0;",
        "  int get foo => _foo;",
        "  void set foo(int foo) {",
        "    this._foo = foo;",
        "  }",
        "}",
        "class Test_Sub extends Test_Super {",
        "  int _foo = 0;",
        "  void set foo(int foo) {",
        "    this._foo = foo;",
        "  }",
        "}");
  }

  public void test_field_OK_getter() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public int getFoo() {",
        "    return foo;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  final int foo = 0;",
        "}");
  }

  // XXX
  public void test_field_OK_getter_hasInitializer() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo = 123;",
        "  public int getFoo() {",
        "    return foo;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  final int foo = 123;",
        "}");
  }

  public void test_field_OK_getter_withConstructor() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public Test(int foo) {",
        "    this.foo = foo;",
        "  }",
        "  public int getFoo() {",
        "    return foo;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  final int foo;",
        "  Test(this.foo);",
        "}");
  }

  public void test_field_OK_getterSetter() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public int getFoo() {",
        "    return foo;",
        "  }",
        "  public void setFoo(int foo) {",
        "    this.foo = foo;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  int foo = 0;",
        "}");
  }

  public void test_field_OK_getterSetter_withConstructor() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public int getFoo() {",
        "    return foo;",
        "  }",
        "  public void setFoo(int foo) {",
        "    this.foo = foo;",
        "  }",
        "  public Test(int foo) {",
        "    this.foo = foo;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  int foo = 0;",
        "  Test(this.foo);",
        "}");
  }

  public void test_methodGetWithoutName() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public boolean get() {",
        "    return true;",
        "  }",
        "  public void main() {",
        "    print(get());",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  bool get() => true;",
        "  void main() {",
        "    print(get());",
        "  }",
        "}");
  }

  public void test_methodSetWithoutName() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public void set(int v) {",
        "  }",
        "  public void main() {",
        "    set(0);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void set(int v) {",
        "  }",
        "  void main() {",
        "    set(0);",
        "  }",
        "}");
  }

  public void test_renamePrivateFields() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int a;",
        "  private int b, c;",
        "  public int d;",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  int _a = 0;",
        "  int _b = 0, _c = 0;",
        "  int d = 0;",
        "}");
  }

  public void test_setterWithReturnType() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public int getFoo() {",
        "    return foo - 1;",
        "  }",
        "  public int setFoo(int foo) {",
        "    this.foo = foo + 1;",
        "    return 42;",
        "  }",
        "  public void main() {",
        "    setFoo(1);",
        "    print(getFoo());",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  int _foo = 0;",
        "  int get foo => _foo - 1;",
        "  int setFoo(int foo) {",
        "    this._foo = foo + 1;",
        "    return 42;",
        "  }",
        "  void main() {",
        "    setFoo(1);",
        "    print(foo);",
        "  }",
        "}");
  }

  public void test_shareGetSetNames() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int onlyBasicGettersSetters;",
        "  private int foo;",
        "  public int getFoo() {",
        "    return foo - 1;",
        "  }",
        "  public void setFoo(int foo) {",
        "    this.foo = foo + 1;",
        "  }",
        "  public void main() {",
        "    setFoo(1);",
        "    print(getFoo());",
        "    this.setFoo(2);",
        "    print(this.getFoo());",
        "  }",
        "}");
    runProcessor();
    context.ensureUniqueClassMemberNames(unit);
    assertFormattedSource(
        "class Test {",
        "  int _foo = 0;",
        "  int get foo => _foo - 1;",
        "  void set foo(int foo) {",
        "    this._foo = foo + 1;",
        "  }",
        "  void main() {",
        "    foo = 1;",
        "    print(foo);",
        "    this.foo = 2;",
        "    print(this.foo);",
        "  }",
        "}");
  }

  public void test_veto() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private boolean foo;",
        "  public boolean isFoo() {",
        "    return foo;",
        "  }",
        "  public void setFoo(boolean v) {",
        "    this.foo = v;",
        "  }",
        "  public void main() {",
        "    setFoo(true);",
        "    print(isFoo());",
        "    this.setFoo(false);",
        "    print(this.isFoo());",
        "  }",
        "}");
    context.addNotProperty("Ltest/Test;.isFoo()");
    context.addNotProperty("Ltest/Test;.setFoo(Z)");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  bool _foo = false;",
        "  bool isFoo() => _foo;",
        "  void setFoo(bool v) {",
        "    this._foo = v;",
        "  }",
        "  void main() {",
        "    setFoo(true);",
        "    print(isFoo());",
        "    this.setFoo(false);",
        "    print(this.isFoo());",
        "  }",
        "}");
  }

  private void runProcessor() {
    new PropertySemanticProcessor(context).process(unit);
  }
}
