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
  public void test_makeProperty() throws Exception {
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
    PropertySemanticProcessor.INSTANCE.process(context, unit);
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
    PropertySemanticProcessor.INSTANCE.process(context, unit);
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
    PropertySemanticProcessor.INSTANCE.process(context, unit);
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

  public void test_renamePrivateFields() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  private int a;",
        "  private int b, c;",
        "  public int d;",
        "}");
    PropertySemanticProcessor.INSTANCE.process(context, unit);
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
        "    return foo;",
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
    PropertySemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  int _foo = 0;",
        "  int get foo => _foo;",
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
}
