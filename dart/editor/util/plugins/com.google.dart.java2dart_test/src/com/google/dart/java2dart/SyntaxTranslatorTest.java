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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.Comment;

import junit.framework.TestCase;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link SyntaxTranslator}.
 */
public class SyntaxTranslatorTest extends TestCase {

  /**
   * @return the Dart source of the given {@link com.google.dart.engine.ast.CompilationUnit}.
   */
  private static String toSource(com.google.dart.engine.ast.CompilationUnit dartUnit) {
    return dartUnit.toSource();
  }

  private Context context = new Context();
  private org.eclipse.jdt.core.dom.CompilationUnit javaUnit;
  private com.google.dart.engine.ast.CompilationUnit dartUnit;

  public void test_classAbstract() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public abstract class A {",
        "}");
    assertDartSource("abstract class A {}");
  }

  public void test_classEmpty() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "}");
    assertDartSource("class A {}");
  }

  public void test_classExtends() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "}",
        "public class B extends A {",
        "}",
        "");
    assertDartSource("class A {} class B extends A {}");
  }

  public void test_classImplements() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public interface I1 {}",
        "public interface I2 {}",
        "public interface I3 {}",
        "public class B implements I1, I2, I3 {",
        "}",
        "");
    assertDartSource("abstract class I1 {} abstract class I2 {} abstract class I3 {}"
        + " class B implements I1, I2, I3 {}");
  }

  public void test_classTypeParameters() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A<K, V extends String> {",
        "  A(K k, V v) {",
        "  }",
        "}");
    assertDartSource("class A<K, V extends String> {A(K k, V v) {}}");
  }

  public void test_commentDoc_class() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "/**",
        " * Some comment.",
        " */",
        "public class A {",
        "}");
    assertDartSource("class A {}");
    {
      Comment docComment = dartUnit.getDeclarations().get(0).getDocumentationComment();
      assertEquals("/**\n * Some comment.\n */\n", docComment.getBeginToken().toString());
    }
  }

  public void test_commentDoc_field() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  /**",
        "   * Some comment.",
        "   */",
        "  int foo;",
        "}");
    assertDartSource("class A {int foo;}");
    {
      ClassDeclaration classA = (ClassDeclaration) dartUnit.getDeclarations().get(0);
      Comment docComment = classA.getMembers().get(0).getDocumentationComment();
      assertEquals("/**\n * Some comment.\n */\n", docComment.getBeginToken().toString());
    }
  }

  public void test_commentDoc_method() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  /**",
        "   * Some comment.",
        "   */",
        "  void foo() {}",
        "}");
    assertDartSource("class A {void foo() {}}");
    {
      ClassDeclaration classA = (ClassDeclaration) dartUnit.getDeclarations().get(0);
      Comment docComment = classA.getMembers().get(0).getDocumentationComment();
      assertEquals("/**\n * Some comment.\n */\n", docComment.getBeginToken().toString());
    }
  }

  public void test_enum() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public enum Direction {",
        "  UP(false), DOWN(false), LEFT(true), RIGHT(true);",
        "  private Direction(boolean horizontal) {",
        "    this.horizontal = horizontal;",
        "  }",
        "  private final boolean horizontal;",
        "  public boolean isHorizontal() {",
        "    return horizontal;",
        "  }",
        "}");
    assertDartSource("class Direction {"
        + "static final Direction UP = new Direction(false); static final Direction DOWN = new Direction(false); "
        + "static final Direction LEFT = new Direction(true); static final Direction RIGHT = new Direction(true); "
        + "Direction(bool horizontal) {this.horizontal = horizontal;} "
        + "final bool horizontal; bool isHorizontal() {return horizontal;}}");
  }

  public void test_enum_withImplements() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public interface I {}",
        "public enum Direction implements I {",
        "}");
    assertDartSource("abstract class I {} class Direction implements I {}");
  }

  public void test_expressionArrayAccess() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A(String[] items) {",
        "    print(items[0]);",
        "  }",
        "}");
    assertDartSource("class A {A(List<String> items) {print(items[0]);}}");
  }

  public void test_expressionArrayCreation_dimensionOne() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    Object v = new int[] {1, 2, 3};",
        "  }",
        "}");
    assertDartSource("class A {A() {Object v = <int> [1, 2, 3];}}");
  }

  public void test_expressionArrayCreation_dimensionTwo() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    Object v = new int[][] {new int[] {1, 2, 3}, new int[] {10, 20, 30}};",
        "  }",
        "}");
    assertDartSource("class A {A() {Object v = <List<int>> [<int> [1, 2, 3], <int> [10, 20, 30]];}}");
  }

  public void test_expressionArrayCreation_noInitializer() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    String[] v = new String[3];",
        "  }",
        "}");
    assertDartSource("class A {A() {List<String> v = new List<String>.fixedLength(3);}}");
  }

  public void test_expressionArrayInitializer() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    int[] v = {1, 2, 3};",
        "  }",
        "}");
    assertDartSource("class A {A() {List<int> v = [1, 2, 3];}}");
  }

  public void test_expressionAssignment() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    int v;",
        "    v = 1;",
        "    v += 1;",
        "    v -= 1;",
        "    v *= 1;",
        "    v /= 1;",
        "    v %= 1;",
        "    v ^= 1;",
        "    v |= 1;",
        "    v &= 1;",
        "    v <<= 1;",
        "    v >>= 1;",
        "  }",
        "}");
    assertDartSource("class A {A() {int v; v = 1; v += 1; v -= 1; v *= 1; v /= 1; v %= 1;"
        + " v ^= 1; v |= 1; v &= 1; v <<= 1; v >>= 1;}}");
  }

  public void test_expressionCast_byte() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    byte b = (byte) 0;",
        "  }",
        "}");
    assertDartSource("class A {A() {int b = 0;}}");
  }

  public void test_expressionClassInstanceCreation() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A(int p) {",
        "  }",
        "  void foo() {",
        "    new A(123);",
        "  }",
        "}");
    assertDartSource("class A {A(int p) {} void foo() {new A(123);}}");
  }

  public void test_expressionClassInstanceCreation_typeArguments() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A<K, V> {",
        "}",
        "public class B {",
        "  B() {",
        "    new A<String, B>();",
        "  }",
        "}",
        "");
    assertDartSource("class A<K, V> {} class B {B() {new A<String, B>();}}");
  }

  public void test_expressionConditional() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    int v = true ? 1 : 2;",
        "  }",
        "}");
    assertDartSource("class A {A() {int v = true ? 1 : 2;}}");
  }

  public void test_expressionFieldAccess_thisQualifier() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        " int field;",
        "  A() {",
        "    print(this.field);",
        "  }",
        "}");
    assertDartSource("class A {int field; A() {print(this.field);}}");
  }

  public void test_expressionInfix() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    int m1 = 0 + 1;",
        "    int m2 = 0 - 1;",
        "    int m3 = 0 * 1;",
        "    int m4 = 0 / 1;",
        "    int m5 = 0 % 1;",
        "    int s1 = 0 << 1;",
        "    int s2 = 0 >> 1;",
        "    int s3 = 0 >>> 1;",
        "    boolean c1 = false || true;",
        "    boolean c2 = false && true;",
        "    int b1 = 0 ^ 1;",
        "    int b2 = 0 | 1;",
        "    int b3 = 0 & 1;",
        "    boolean r1 = 0 < 1;",
        "    boolean r2 = 0 > 1;",
        "    boolean r3 = 0 <= 1;",
        "    boolean r4 = 0 >= 1;",
        "    boolean r5 = 0 == 1;",
        "    boolean r6 = 0 != 1;",
        "  }",
        "}");
    assertDartSource("class A {A() {"
        + "int m1 = 0 + 1; int m2 = 0 - 1; int m3 = 0 * 1; int m4 = 0 / 1; int m5 = 0 % 1; "
        + "int s1 = 0 << 1; int s2 = 0 >> 1; int s3 = 0 >> 1; "
        + "bool c1 = false || true; bool c2 = false && true; "
        + "int b1 = 0 ^ 1; int b2 = 0 | 1; int b3 = 0 & 1; "
        + "bool r1 = 0 < 1; bool r2 = 0 > 1; bool r3 = 0 <= 1; "
        + "bool r4 = 0 >= 1; bool r5 = 0 == 1; bool r6 = 0 != 1;}}");
  }

  public void test_expressionInfix_multipleOperands() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    int v = 0 + 1 + 2 + 3 + 4;",
        "  }",
        "}");
    assertDartSource("class A {A() {int v = 0 + 1 + 2 + 3 + 4;}}");
  }

  public void test_expressionInstanceOf() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    boolean b = \"\" instanceof String;",
        "  }",
        "}");
    assertDartSource("class A {A() {bool b = \"\" is String;}}");
  }

  public void test_expressionInvocation_qualified() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    this.foo(0);",
        "  }",
        "  void foo(int p) {}",
        "}");
    assertDartSource("class A {A() {this.foo(0);} void foo(int p) {}}");
  }

  public void test_expressionInvocation_unqualified() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(0);",
        "    print(1);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(0); print(1);}}");
  }

  public void test_expressionParenthesized() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    int v = (1 + 3) / 2;",
        "  }",
        "}");
    assertDartSource("class A {A() {int v = (1 + 3) / 2;}}");
  }

  public void test_expressionPostfix() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    int v = 0;",
        "    int v1 = v++;",
        "    int v2 = v--;",
        "  }",
        "}");
    assertDartSource("class A {A() {int v = 0; int v1 = v++; int v2 = v--;}}");
  }

  public void test_expressionPrefix() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    int v1 = +0;",
        "    int v2 = -0;",
        "    int v3 = ~0;",
        "    int v4 = !0;",
        "    int v5 = ++v1;",
        "    int v6 = --v1;",
        "  }",
        "}");
    assertDartSource("class A {A() {int v1 = 0; int v2 = -0; int v3 = ~0; int v4 = !0;"
        + " int v5 = ++v1; int v6 = --v1;}}");
  }

  public void test_expressionQualifiedName_forField() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        " int field;",
        "}",
        "public class B {",
        "  B(A a) {",
        "    print(a.field);",
        "  }",
        "}");
    assertDartSource("class A {int field;} class B {B(A a) {print(a.field);}}");
  }

  public void test_expressionThis() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(this);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(this);}}");
  }

  public void test_expressionThrow() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    throw new Exception();",
        "  }",
        "}");
    assertDartSource("class A {A() {throw new Exception();}}");
  }

  public void test_expressionTypeLiteral() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(A.class);",
        "    print(my.company.project.A.class);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(A); print(my.company.project.A);}}");
  }

  public void test_field() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  int field;",
        "  int f1 = 1, f2 = 2;",
        "}");
    assertDartSource("class A {int field; int f1 = 1, f2 = 2;}");
  }

  public void test_interface() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface A {",
        "  int foo();",
        "  void bar();",
        "}");
    assertDartSource("abstract class A {int foo(); void bar();}");
  }

  public void test_literalBoolean() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(true);",
        "    print(false);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(true); print(false);}}");
  }

  public void test_literalCharacter() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    char c = '0';",
        "  }",
        "}");
    assertDartSource("class A {A() {int c = 0x30;}}");
  }

  public void test_literalDouble() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(0.0);",
        "    print(1d);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(0.0); print(1.0);}}");
  }

  public void test_literalInt() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(0);",
        "    print(1);",
        "    print(0xDEAD);",
        "    print(0xBEAF);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(0); print(1); print(0xDEAD); print(0xBEAF);}}");
  }

  public void test_literalLong() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(0l);",
        "    print(1L);",
        "    print(0xDEADl);",
        "    print(0xBEAFL);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(0); print(1); print(0xDEAD); print(0xBEAF);}}");
  }

  public void test_literalNull() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(null);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(null);}}");
  }

  public void test_literalString() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(\"abc\");",
        "    print(\"a'b\");",
        "  }",
        "}");
    assertDartSource("class A {A() {print(\"abc\"); print(\"a'b\");}}");
  }

  public void test_literalString_escapeInterpolation() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(\"a$b\");",
        "  }",
        "}");
    assertDartSource("class A {A() {print(\"a\\$b\");}}");
  }

  public void test_methodEmpty() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "  }",
        "}");
    assertDartSource("class A {A() {}}");
  }

  public void test_methodParameters() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A(boolean b, int i, double d, String s) {",
        "  }",
        "}");
    assertDartSource("class A {A(bool b, int i, double d, String s) {}}");
  }

  public void test_methodReturnType() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void foo() {",
        "  }",
        "}");
    assertDartSource("class A {void foo() {}}");
  }

  /**
   * We don't generate modifiers, everything is public!
   */
  public void test_modifiers_field() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  public int fPublic;",
        "  protected int fProtected;",
        "  int fDefault;",
        "  private int fPrivate;",
        "  final int fFinal;",
        "  static final int fStaticFinal;",
        "}");
    assertDartSource("class A {int fPublic; int fProtected; int fDefault; int fPrivate; "
        + "final int fFinal; static final int fStaticFinal;}");
  }

  /**
   * We don't generate modifiers, everything is public!
   */
  public void test_modifiers_method() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  public void mPublic() {}",
        "  protected void mProtected() {}",
        "  void mDefault() {}",
        "  private void mPrivate() {}",
        "  static void mStatic() {}",
        "}");
    assertDartSource("class A {void mPublic() {} void mProtected() {} void mDefault() {} "
        + "void mPrivate() {} static void mStatic() {}}");
  }

  public void test_statementAssert() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    assert 1 == 2;",
        "  }",
        "}");
    assertDartSource("class A {A() {assert (1 == 2);}}");
  }

  public void test_statementBreak() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    while (true) {",
        "      break;",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {while (true) {break;}}}");
  }

  public void test_statementBreak_withLabel() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "   L: L2: while (true) {",
        "      break L;",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {L: L2: while (true) {break L;}}}");
  }

  public void test_statementConstructorInvocation() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  public A() {",
        "    this(42);",
        "  }",
        "  public A(int p) {",
        "  }",
        "}",
        "");
    assertDartSource("class A {A() : this(42); A(int p) {}}");
  }

  /**
   * TODO(scheglov) we could support body in redirecting constructors using intermediate classes.
   */
  public void test_statementConstructorInvocation_hasBody() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  public A() {",
        "    this(42);",
        "    print(12345);",
        "  }",
        "  public A(int p) {",
        "  }",
        "}",
        "");
    try {
      SyntaxTranslator.translate(context, javaUnit);
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  public void test_statementContinue() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    while (true) {",
        "      continue;",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {while (true) {continue;}}}");
  }

  public void test_statementContinue_withLabel() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "   L: L2: while (true) {",
        "      continue L;",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {L: L2: while (true) {continue L;}}}");
  }

  public void test_statementDo() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    do {",
        "      print(0);",
        "    } while (true);",
        "  }",
        "}");
    assertDartSource("class A {A() {do {print(0);} while (true);}}");
  }

  public void test_statementEmpty() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    ;",
        "    ;",
        "  }",
        "}");
    assertDartSource("class A {A() {; ;}}");
  }

  public void test_statementFor() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    for (int i = 0; i < 10; i++) {",
        "      print(i);",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {for (int i = 0; i < 10; i++) {print(i);}}}");
  }

  public void test_statementFor2() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    int i;",
        "    for (i = 0; i < 10; i++) {",
        "      print(i);",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {int i; for (i = 0; i < 10; i++) {print(i);}}}");
  }

  public void test_statementForEach() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A(Iterable<String> items) {",
        "    for (String item: items) {",
        "      print(item);",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A(Iterable<String> items) {for (String item in items) {print(item);}}}");
  }

  public void test_statementIf() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    if (1 == 1) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {if (1 == 1) {print(0);}}}");
  }

  public void test_statementIfElse() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    if (1 == 1) {",
        "      print(1);",
        "    } else {",
        "      print(2);",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {if (1 == 1) {print(1);} else {print(2);}}}");
  }

  public void test_statementReturn() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  int foo() {",
        "    return 42;",
        "  }",
        "}");
    assertDartSource("class A {int foo() {return 42;}}");
  }

  public void test_statementSuperConstructorInvocation() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  public A(int p) {",
        "  }",
        "}",
        "public class B extends A {",
        "  public B() {",
        "    super(42);",
        "    print(0);",
        "    print(1);",
        "  }",
        "}",
        "");
    assertDartSource("class A {A(int p) {}}"
        + " class B extends A {B() : super(42) {print(0); print(1);}}");
  }

  public void test_statementSwitch() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    switch (0) {",
        "      case 1:",
        "        print(1);",
        "        break;",
        "      case 2:",
        "      case 3:",
        "        print(2);",
        "        break;",
        "      default:",
        "        print(3);",
        "        break;",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {switch (0) {"
        + "case 1: print(1); break; case 2:  case 3: print(2); break; default: print(3); break;}}}");
  }

  public void test_statementSynchronized() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    synchronized (this) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {{print(0);}}}");
  }

  public void test_statementTry_catch() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    try {",
        "      print(0);",
        "    } catch (E1 e) {",
        "      print(1);",
        "    } catch (E2 e) {",
        "      print(2);",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {try {print(0);}"
        + " on E1 catch (e) {print(1);} on E2 catch (e) {print(2);}}}");
  }

  public void test_statementTry_finally() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    try {",
        "      print(1);",
        "    } finally {",
        "      print(2);",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {try {print(1);} finally {print(2);}}}");
  }

  public void test_statementVariableDeclaration() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    int a = 1;",
        "    int b = 2, c = 3;",
        "  }",
        "}");
    assertDartSource("class A {A() {int a = 1; int b = 2, c = 3;}}");
  }

  public void test_statementWhile() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    while (true) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    assertDartSource("class A {A() {while (true) {print(0);}}}");
  }

  public void test_typeArray() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A(String[] items, String[][] labels) {",
        "  }",
        "}");
    assertDartSource("class A {A(List<String> items, List<List<String>> labels) {}}");
  }

  public void test_typeArray2() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A(String items[], String labels[][]) {",
        "  }",
        "}");
    assertDartSource("class A {A(List<String> items, List<List<String>> labels) {}}");
  }

  public void test_typePrimitive() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    boolean v1;",
        "    byte v2;",
        "    char v3;",
        "    short v4;",
        "    int v5;",
        "    long v6;",
        "    float v7;",
        "    double v8;",
        "  }",
        "}");
    assertDartSource("class A {A() {bool v1; int v2; int v3; int v4; int v5; int v6; double v7; double v8;}}");
  }

  public void test_unitEmpty() throws Exception {
    parseJava();
    assertDartSource("");
  }

  /**
   * Translates {@link #javaUnit} into {@link #dartUnit} and check that it produces given Dart
   * source.
   */
  private void assertDartSource(String... lines) {
    dartUnit = SyntaxTranslator.translate(context, javaUnit);
    String actualDartSource = toSource(dartUnit);
    String expectedDartSource = Joiner.on("\n").join(lines);
    assertEquals(expectedDartSource, actualDartSource);
  }

  /**
   * Parse Java source lines into {@link #javaUnit}.
   */
  private void parseJava(String... lines) {
    String source = Joiner.on("\n").join(lines);
    ASTParser parser = ASTParser.newParser(AST.JLS4);
    parser.setCompilerOptions(ImmutableMap.of(
        JavaCore.COMPILER_SOURCE,
        JavaCore.VERSION_1_5,
        JavaCore.COMPILER_DOC_COMMENT_SUPPORT,
        JavaCore.ENABLED));
    parser.setSource(source.toCharArray());
    javaUnit = (CompilationUnit) parser.createAST(null);
    assertThat(javaUnit.getProblems()).isEmpty();
  }
}
