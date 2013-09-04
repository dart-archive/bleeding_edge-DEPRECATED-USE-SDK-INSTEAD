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
  public void test_makeProperty_getSet() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public int getFoo() {",
        "    return foo;",
        "  }",
        "  public void setFoo(int v) {",
        "    this.foo = v + 1;",
        "  }",
        "  public void main() {",
        "    setFoo(1);",
        "    print(getFoo());",
        "    this.setFoo(2);",
        "    print(this.getFoo());",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  int _foo = 0;",
        "  int get foo => _foo;",
        "  void set foo(int v) {",
        "    this._foo = v + 1;",
        "  }",
        "  void main() {",
        "    foo = 1;",
        "    print(foo);",
        "    this.foo = 2;",
        "    print(this.foo);",
        "  }",
        "}");
  }

  public void test_makeProperty_isSet() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private boolean foo;",
        "  public boolean isFoo() {",
        "    return foo && true;",
        "  }",
        "  public void setFoo(boolean v) {",
        "    this.foo = v && true;",
        "  }",
        "  public void main() {",
        "    setFoo(true);",
        "    print(isFoo());",
        "    this.setFoo(false);",
        "    print(this.isFoo());",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  bool _foo = false;",
        "  bool get isFoo => _foo && true;",
        "  void set foo(bool v) {",
        "    this._foo = v && true;",
        "  }",
        "  void main() {",
        "    foo = true;",
        "    print(isFoo);",
        "    this.foo = false;",
        "    print(this.isFoo);",
        "  }",
        "}");
  }

  public void test_makeProperty_justField_getSet() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public int getFoo() {",
        "    return foo;",
        "  }",
        "  public void setFoo(int v) {",
        "    this.foo = v;",
        "  }",
        "  public void main() {",
        "    setFoo(1);",
        "    print(getFoo());",
        "    this.setFoo(2);",
        "    print(this.getFoo());",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  int foo = 0;",
        "  void main() {",
        "    foo = 1;",
        "    print(foo);",
        "    this.foo = 2;",
        "    print(this.foo);",
        "  }",
        "}");
  }

  public void test_makeProperty_justField_onlyGetter_noAssignments() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public int getFoo() {",
        "    return foo;",
        "  }",
        "  public void main() {",
        "    print(getFoo());",
        "    print(this.getFoo());",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  final int foo = 0;",
        "  void main() {",
        "    print(foo);",
        "    print(this.foo);",
        "  }",
        "}");
  }

  public void test_makeProperty_justField_override() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  private int foo;",
            "  public int getFoo() {",
            "    return foo;",
            "  }",
            "}"));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B extends A {",
            "  private int bar;",
            "  public int getFoo() {",
            "    return bar;",
            "  }",
            "  public void main() {",
            "    print(getFoo());",
            "    print(this.getFoo());",
            "  }",
            "}"));
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    unit = context.translate();
    runProcessor();
    assertFormattedSource(
        "class A {",
        "  int _foo = 0;",
        "  int get foo => _foo;",
        "}",
        "class B extends A {",
        "  int _bar = 0;",
        "  int get foo => _bar;",
        "  void main() {",
        "    print(foo);",
        "    print(this.foo);",
        "  }",
        "}");
  }

  public void test_makeProperty_justField_updateBinding() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int foo;",
        "  public int getFoo() {",
        "    return foo;",
        "  }",
        "  public void setFoo(int v) {",
        "    this.foo = v;",
        "  }",
        "  public void foo() {",
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
        "  int foo3 = 0;",
        "  void foo() {",
        "  }",
        "  void main() {",
        "    foo3 = 1;",
        "    print(foo3);",
        "    this.foo3 = 2;",
        "    print(this.foo3);",
        "  }",
        "}");
  }

  public void test_makeProperty_override() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public class Super {",
        "    public int getFoo() {",
        "      return 0;",
        "    }",
        "    public void setFoo(int v) {",
        "    }",
        "  }",
        "  public class Sub extends Super {",
        "    public int getFoo() {",
        "      return 2;",
        "    }",
        "    public void setFoo(int v2) {",
        "    }",
        "    public void main() {",
        "      setFoo(1);",
        "      print(getFoo());",
        "    }",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "}",
        "class Test_Super {",
        "  int get foo => 0;",
        "  void set foo(int v) {",
        "  }",
        "}",
        "class Test_Sub extends Test_Super {",
        "  int get foo => 2;",
        "  void set foo(int v2) {",
        "  }",
        "  void main() {",
        "    foo = 1;",
        "    print(foo);",
        "  }",
        "}");
  }

  public void test_makeProperty_shareGetSetNames() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public int getFoo() {",
        "    return 0;",
        "  }",
        "  public void setFoo(int v) {",
        "  }",
        "  public void main() {",
        "    setFoo(1);",
        "    print(getFoo());",
        "  }",
        "}");
    runProcessor();
    context.ensureUniqueClassMemberNames(unit);
    assertFormattedSource(
        "class Test {",
        "  int get foo => 0;",
        "  void set foo(int v) {",
        "  }",
        "  void main() {",
        "    foo = 1;",
        "    print(foo);",
        "  }",
        "}");
  }

  public void test_makeProperty_veto() throws Exception {
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
        "  public int setFoo(int v) {",
        "    this.foo = v + 1;",
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
        "  int setFoo(int v) {",
        "    this._foo = v + 1;",
        "    return 42;",
        "  }",
        "  void main() {",
        "    setFoo(1);",
        "    print(foo);",
        "  }",
        "}");
  }

  private void runProcessor() {
    new PropertySemanticProcessor(context).process(unit);
  }
}
