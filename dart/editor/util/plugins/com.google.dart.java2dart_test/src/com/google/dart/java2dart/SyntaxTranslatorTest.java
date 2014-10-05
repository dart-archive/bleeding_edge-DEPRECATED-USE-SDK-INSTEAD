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
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link SyntaxTranslator}.
 */
public class SyntaxTranslatorTest extends AbstractSemanticTest {
  /**
   * @return the formatted Dart source dump of the given {@link AstNode}.
   */
  private static String toFormattedSource(AstNode node) {
    PrintStringWriter writer = new PrintStringWriter();
    node.accept(new ToFormattedSourceVisitor(writer));
    String result = writer.toString();
    return StringUtils.join(StringUtils.split(result, '\n'), "\n");
  }

  private Context context = new Context();

  private String javaSource;
  private org.eclipse.jdt.core.dom.CompilationUnit javaUnit;
  private com.google.dart.engine.ast.CompilationUnit dartUnit;

  public void test_annotation_marker() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public @interface DartOmit {",
        "}",
        "public class A {",
        "  @DartOmit",
        "  public void foo() {}",
        "  public void var() {}",
        "}");
    translate();
    String actual = context.getNodeAnnotations().toString();
    assertEquals("{void foo() {}=[DartOmit{}]}", actual);
  }

  public void test_annotation_normal() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public @interface DartLibrary {",
        "  String name();",
        "  String path();",
        "}",
        "public class A {",
        "  @DartLibrary(name = \"my.name\", path = \"my/path\")",
        "  public void foo() {}",
        "  public void var() {}",
        "}");
    translate();
    String actual = context.getNodeAnnotations().toString();
    assertEquals("{void foo() {}=[DartLibrary{name=my.name, path=my/path}]}", actual);
  }

  public void test_annotation_single() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public @interface DartBlockBody {",
        "  String[] value();",
        "}",
        "public class A {",
        "  @DartBlockBody({\"aaa\", \"bbb\", \"ccc\"})",
        "  public void foo() {}",
        "  public void var() {}",
        "}");
    translate();
    String actual = context.getNodeAnnotations().toString();
    assertEquals("{void foo() {}=[DartBlockBody{value=[aaa, bbb, ccc]}]}", actual);
  }

  public void test_classAbstract() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public abstract class A {",
        "}");
    assertDartSource(//
        "abstract class A {",
        "}");
  }

  public void test_classEmpty() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "}");
    assertDartSource(//
        "class A {",
        "}");
  }

  public void test_classExtends() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "}",
        "public class B extends A {",
        "}",
        "");
    assertDartSource(//
        "class A {",
        "}",
        "class B extends A {",
        "}");
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
    assertDartSource(
        "abstract class I1 {",
        "}",
        "abstract class I2 {",
        "}",
        "abstract class I3 {",
        "}",
        "class B implements I1, I2, I3 {",
        "}");
  }

  public void test_classTypeArguments_Void() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class Test<T> {",
        "  void main() {",
        "    new Test<Void>();",
        "  }",
        "}");
    assertDartSource(//
        "class Test<T> {",
        "  void main() {",
        "    new Test<Object>();",
        "  }",
        "}");
  }

  public void test_classTypeParameters() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A<K, V extends String> {",
        "  void test(K k, V v) {",
        "  }",
        "}");
    assertDartSource(//
        "class A<K, V extends String> {",
        "  void test(K k, V v) {",
        "  }",
        "}");
  }

  public void test_commentDoc_class() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "/**",
        " * Some comment.",
        " */",
        "public class A {",
        "}");
    assertDartSource(//
        "/**",
        " * Some comment.",
        " */",
        "class A {",
        "}");
  }

  public void test_commentDoc_escaping() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "/**",
        " * Some [ident] ignored.",
        " * Second line.",
        " */",
        "public class A {",
        "}");
    assertDartSource("/**", " * Some [ident] ignored.", " * Second line.", " */", "class A {", "}");
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
    assertDartSource(//
        "class A {",
        "  /**",
        "   * Some comment.",
        "   */",
        "  int foo;",
        "}");
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
    assertDartSource(
        "class A {",
        "  /**",
        "   * Some comment.",
        "   */",
        "  void foo() {",
        "  }",
        "}");
  }

  public void test_commentLine_beforeStatement_blockStyle() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void main() {",
        "    /*",
        "     * aaa",
        "     * bbb",
        "     */",
        "    int v = 0;",
        "  }",
        "}");
    assertDartSource(
        "class A {",
        "  void main() {",
        "    /*",
        "     * aaa",
        "     * bbb",
        "     */",
        "    int v = 0;",
        "  }",
        "}");
  }

  public void test_commentLine_beforeStatement_lineStyle() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void main() {",
        "    // aaa",
        "    // bbb",
        "    int v = 0;",
        "  }",
        "}");
    assertDartSource(
        "class A {",
        "  void main() {",
        "    // aaa",
        "    // bbb",
        "    int v = 0;",
        "  }",
        "}");
  }

  public void test_commentLine_endOfBlock_blockStyle() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void main() {",
        "    // aaa",
        "    int v = 0;",
        "    /*",
        "     * aaa",
        "     * bbb",
        "     */",
        "  }",
        "}");
    assertDartSource(
        "class A {",
        "  void main() {",
        "    // aaa",
        "    int v = 0;",
        "    /*",
        "     * aaa",
        "     * bbb",
        "     */",
        "  }",
        "}");
  }

  public void test_commentLine_endOfBlock_lineStyle() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void main() {",
        "    // aaa",
        "    int v = 0;",
        "    if (true) {",
        "      // bbb",
        "    } else {",
        "      // ccc",
        "    }",
        "    // ddd",
        "    // eee",
        "  }",
        "}");
    assertDartSource(
        "class A {",
        "  void main() {",
        "    // aaa",
        "    int v = 0;",
        "    if (true) {",
        "      // bbb",
        "    } else {",
        "      // ccc",
        "    }",
        "    // ddd",
        "    // eee",
        "  }",
        "}");
  }

  public void test_constructor() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(0);",
        "  }",
        "  A(int p) {",
        "    print(1);",
        "  }",
        "}");
    assertDartSource(
        "class A {",
        "  A.jtd_constructor_0_decl() {",
        "    print(0);",
        "  }",
        "  A.jtd_constructor_1_decl(int p) {",
        "    print(1);",
        "  }",
        "}");
  }

  public void test_enum_withImplements() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public interface I {}",
        "public enum Direction implements I {",
        "}");
    assertDartSource(
        "abstract class I {",
        "}",
        "class Direction extends Enum<Direction> implements I {",
        "  static const List<Direction> values = const [];",
        "  const Direction.jtd_constructor_0_decl(String name, int ordinal) : super(name, ordinal) {",
        "  }",
        "}");
  }

  public void test_expression_equals() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  boolean testObject(Object a, Object b) {",
        "    return a == b;",
        "  }",
        "  boolean testNull(Object p) {",
        "    return p == null;",
        "  }",
        "  boolean testBool(Object p) {",
        "    return p == true;",
        "  }",
        "  boolean testChar(Object p) {",
        "    return p == '0';",
        "  }",
        "  boolean testByte(Object p) {",
        "    return p == (byte) 1;",
        "  }",
        "  boolean testInt(Object p) {",
        "    return p == 2;",
        "  }",
        "  boolean testLong(Object p) {",
        "    return p == 3L;",
        "  }",
        "  boolean testFloat(Object p) {",
        "    return p == 4.0f;",
        "  }",
        "  boolean testDouble(Object p) {",
        "    return p == 5.0d;",
        "  }",
        "}");
    assertDartSource(
        "class A {",
        "  bool testObject(Object a, Object b) => identical(a, b);",
        "  bool testNull(Object p) => p == null;",
        "  bool testBool(Object p) => p == true;",
        "  bool testChar(Object p) => p == 0x30;",
        "  bool testByte(Object p) => p == 1;",
        "  bool testInt(Object p) => p == 2;",
        "  bool testLong(Object p) => p == 3;",
        "  bool testFloat(Object p) => p == 4.0;",
        "  bool testDouble(Object p) => p == 5.0;",
        "}");
  }

  public void test_expression_instanceOf() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test(Object p) {",
        "    boolean b1 = p instanceof String;",
        "    boolean b2 = !(p instanceof String);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test(Object p) {",
        "    bool b1 = p is String;",
        "    bool b2 = !(p is String);",
        "  }",
        "}");
  }

  public void test_expression_notEquals() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  boolean testA(Object a, Object b) {",
        "    return a != b;",
        "  }",
        "  boolean testB(Object p) {",
        "    return p != null || p != 1 || p != 2L || p != 3.0f || p != 4.0d;",
        "  }",
        "}");
    assertDartSource(
        "class A {",
        "  bool testA(Object a, Object b) => !identical(a, b);",
        "  bool testB(Object p) => p != null || p != 1 || p != 2 || p != 3.0 || p != 4.0;",
        "}");
  }

  public void test_expressionArrayAccess() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test(String[] items) {",
        "    print(items[0]);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test(List<String> items) {",
        "    print(items[0]);",
        "  }",
        "}");
  }

  public void test_expressionArrayCreation_dimensionOne() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    Object v = new int[] {1, 2, 3};",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    Object v = <int> [1, 2, 3];",
        "  }",
        "}");
  }

  public void test_expressionArrayCreation_dimensionTwo() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    Object v = new int[][] {new int[] {1, 2, 3}, new int[] {10, 20, 30}};",
        "  }",
        "}");
    assertDartSource(
        "class A {",
        "  void test() {",
        "    Object v = <List<int>> [<int> [1, 2, 3], <int> [10, 20, 30]];",
        "  }",
        "}");
  }

  public void test_expressionArrayCreation_noInitializer() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    String[] v = new String[3];",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    List<String> v = new List<String>(3);",
        "  }",
        "}");
  }

  public void test_expressionArrayInitializer() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    int[] v = {1, 2, 3};",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    List<int> v = [1, 2, 3];",
        "  }",
        "}");
  }

  public void test_expressionAssignment() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
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
    assertDartSource(
        "class A {",
        "  void test() {",
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
  }

  public void test_expressionCast() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class Test {",
        "  void main(Object p) {",
        "    print((Integer) p);",
        "  }",
        "}");
    assertDartSource(//
        "class Test {",
        "  void main(Object p) {",
        "    print((p as int));",
        "  }",
        "}");
  }

  public void test_expressionCast_toByte() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class Test {",
        "  void main(Object p) {",
        "    print((byte) p);",
        "    print((byte) 2);",
        "    print((byte) 256);",
        "    print((byte) 257);",
        "  }",
        "}");
    assertDartSource(//
        "class Test {",
        "  void main(Object p) {",
        "    print(toByte(p));",
        "    print(2);",
        "    print(0);",
        "    print(1);",
        "  }",
        "}");
  }

  public void test_expressionClassInstanceCreation() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test(int p) {",
        "  }",
        "  void foo() {",
        "    new A(123);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test(int p) {",
        "  }",
        "  void foo() {",
        "    new A(123);",
        "  }",
        "}");
  }

  public void test_expressionClassInstanceCreation_typeArguments() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A<K, V> {",
        "}",
        "public class B {",
        "  void test() {",
        "    new A<String, B>();",
        "  }",
        "}",
        "");
    assertDartSource(//
        "class A<K, V> {",
        "}",
        "class B {",
        "  void test() {",
        "    new A<String, B>();",
        "  }",
        "}");
  }

  public void test_expressionConditional() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    int v = true ? 1 : 2;",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    int v = true ? 1 : 2;",
        "  }",
        "}");
  }

  public void test_expressionFieldAccess_thisQualifier() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        " int field;",
        "  test() {",
        "    print(this.field);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  int field;",
        "  test() {",
        "    print(this.field);",
        "  }",
        "}");
  }

  public void test_expressionInfix() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "public class A {",
            "  public void test() {",
            "    int m1 = 0 + 1;",
            "    int m2 = 0 - 1;",
            "    int m3 = 0 * 1;",
            "    int m4 = 0 / 1;",
            "    int m5 = 0.0 / 1;",
            "    int m6 = 0 / 1.0;",
            "    int m7 = 0 % 1;",
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
            "}"));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    // do translate
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  void test() {",
            "    int m1 = 0 + 1;",
            "    int m2 = 0 - 1;",
            "    int m3 = 0 * 1;",
            "    int m4 = 0 ~/ 1;",
            "    int m5 = 0.0 / 1;",
            "    int m6 = 0 / 1.0;",
            "    int m7 = 0 % 1;",
            "    int s1 = 0 << 1;",
            "    int s2 = 0 >> 1;",
            "    int s3 = 0 >> 1;",
            "    bool c1 = false || true;",
            "    bool c2 = false && true;",
            "    int b1 = 0 ^ 1;",
            "    int b2 = 0 | 1;",
            "    int b3 = 0 & 1;",
            "    bool r1 = 0 < 1;",
            "    bool r2 = 0 > 1;",
            "    bool r3 = 0 <= 1;",
            "    bool r4 = 0 >= 1;",
            "    bool r5 = 0 == 1;",
            "    bool r6 = 0 != 1;",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_expressionInfix_multipleOperands() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    int v = 0 + 1 + 2 + 3 + 4;",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    int v = 0 + 1 + 2 + 3 + 4;",
        "  }",
        "}");
  }

  public void test_expressionInvocation_qualified() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    this.foo(0);",
        "  }",
        "  void foo(int p) {}",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    this.foo(0);",
        "  }",
        "  void foo(int p) {",
        "  }",
        "}");
  }

  public void test_expressionInvocation_unqualified() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(0);",
        "    print(1);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(0);",
        "    print(1);",
        "  }",
        "}");
  }

  public void test_expressionParenthesized() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    int v = (1 + 3) / 2;",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    int v = (1 + 3) / 2;",
        "  }",
        "}");
  }

  public void test_expressionPostfix() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    int v = 0;",
        "    int v1 = v++;",
        "    int v2 = v--;",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    int v = 0;",
        "    int v1 = v++;",
        "    int v2 = v--;",
        "  }",
        "}");
  }

  public void test_expressionPrefix() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    int v1 = +0;",
        "    int v2 = -0;",
        "    int v3 = ~0;",
        "    int v4 = !0;",
        "    int v5 = ++v1;",
        "    int v6 = --v1;",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    int v1 = 0;",
        "    int v2 = -0;",
        "    int v3 = ~0;",
        "    int v4 = !0;",
        "    int v5 = ++v1;",
        "    int v6 = --v1;",
        "  }",
        "}");
  }

  public void test_expressionQualifiedName_forField() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        " int field;",
        "}",
        "public class B {",
        "  void test(A a) {",
        "    print(a.field);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  int field;",
        "}",
        "class B {",
        "  void test(A a) {",
        "    print(a.field);",
        "  }",
        "}");
  }

  public void test_expressionThis() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(this);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(this);",
        "  }",
        "}");
  }

  public void test_expressionThrow() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    throw new Exception();",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    throw new Exception();",
        "  }",
        "}");
  }

  public void test_expressionTypeLiteral() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(A.class);",
        "    print(my.company.project.A.class);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(A);",
        "    print(my.company.project.A);",
        "  }",
        "}");
  }

  public void test_field() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  int field;",
        "  int f1 = 1, f2 = 2;",
        "}");
    assertDartSource(//
        "class A {",
        "  int field;",
        "  int f1 = 1, f2 = 2;",
        "}");
  }

  public void test_interface() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface A {",
        "  int VALUE = 42;",
        "  int foo();",
        "  void bar();",
        "}");
    assertDartSource(//
        "abstract class A {",
        "  static final int VALUE = 42;",
        "  int foo();",
        "  void bar();",
        "}");
  }

  public void test_javadoc_code() throws Exception {
    parseJava(//
        "/**",
        " * aaa {@code fooBar} bbb",
        " */",
        "public class A {",
        "}");
    assertDartSource(//
        "/**",
        " * aaa `fooBar` bbb",
        " */",
        "class A {",
        "}");
  }

  public void test_javadoc_li() throws Exception {
    parseJava(//
        "/**",
        " * <ul>",
        " * <li>foo bar",
        " * </ul>",
        " */",
        "public class A {",
        "}");
    assertDartSource(//
        "/**",
        " * * foo bar",
        " */",
        "class A {",
        "}");
  }

  public void test_javadoc_link() throws Exception {
    parseJava(//
        "/**",
        " * {@link #fooBar()}",
        " */",
        "public class A {",
        "}");
    assertDartSource(//
        "/**",
        " * [fooBar]",
        " */",
        "class A {",
        "}");
  }

  public void test_javadoc_multiLine() throws Exception {
    parseJava(//
        "/**",
        " * aaa bbb",
        " * {@link Source} ccc",
        " * ddd",
        " */",
        "public class A {",
        "}");
    assertDartSource(//
        "/**",
        " * aaa bbb",
        " * [Source] ccc",
        " * ddd",
        " */",
        "class A {",
        "}");
  }

  public void test_javadoc_namedLink() throws Exception {
    parseJava(//
        "/**",
        " * {@link Source source}",
        " */",
        "public class A {",
        "}");
    assertDartSource(//
        "/**",
        " * [Source]",
        " */",
        "class A {",
        "}");
  }

  public void test_javadoc_para() throws Exception {
    parseJava(//
        "/**",
        " * <p>",
        " */",
        "public class A {",
        "}");
    assertDartSource(//
        "/**",
        " *",
        " */",
        "class A {",
        "}");
  }

  public void test_literalBoolean() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(true);",
        "    print(false);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(true);",
        "    print(false);",
        "  }",
        "}");
  }

  public void test_literalCharacter() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    char c = '0';",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    int c = 0x30;",
        "  }",
        "}"

    );
  }

  public void test_literalDouble() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(0.0);",
        "    print(1d);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(0.0);",
        "    print(1.0);",
        "  }",
        "}");
  }

  public void test_literalInt() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(0);",
        "    print(1);",
        "    print(0xDEAD);",
        "    print(0xBEAF);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(0);",
        "    print(1);",
        "    print(0xDEAD);",
        "    print(0xBEAF);",
        "  }",
        "}");
  }

  public void test_literalLong() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(0l);",
        "    print(1L);",
        "    print(0xDEADl);",
        "    print(0xBEAFL);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(0);",
        "    print(1);",
        "    print(0xDEAD);",
        "    print(0xBEAF);",
        "  }",
        "}");
  }

  public void test_literalNull() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(null);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(null);",
        "  }",
        "}");
  }

  public void test_literalString() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(\"abc\");",
        "    print(\"a'b\");",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(\"abc\");",
        "    print(\"a'b\");",
        "  }",
        "}");
  }

  public void test_literalString_escapeInterpolation() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(\"a$b\");",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(\"a\\$b\");",
        "  }",
        "}");
  }

  public void test_literalString_newLine() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(\"A\\nB\");",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(\"A\\nB\");",
        "  }",
        "}");
  }

  public void test_methodEmpty() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "  }",
        "}");
  }

  public void test_methodParameters() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test(boolean b, int i, double d, String s) {",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test(bool b, int i, double d, String s) {",
        "  }",
        "}");
  }

  public void test_methodReturnType() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void foo() {",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void foo() {",
        "  }",
        "}");
  }

  /**
   * We don't generate "final" because it is very hard to simulate Java constructors in Dart.
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
    assertDartSource(//
        "class A {",
        "  int fPublic;",
        "  int fProtected;",
        "  int fDefault;",
        "  int fPrivate;",
        "  int fFinal;",
        "  static int fStaticFinal;",
        "}");
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
    assertDartSource(//
        "class A {",
        "  void mPublic() {",
        "  }",
        "  void mProtected() {",
        "  }",
        "  void mDefault() {",
        "  }",
        "  void mPrivate() {",
        "  }",
        "  static void mStatic() {",
        "  }",
        "}");
  }

  public void test_statementAssert() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    assert 1 < 2;",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    assert(1 < 2);",
        "  }",
        "}");
  }

  public void test_statementBreak() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    while (true) {",
        "      break;",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    while (true) {",
        "      break;",
        "    }",
        "  }",
        "}");
  }

  public void test_statementBreak_withLabel() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "   L: L2: while (true) {",
        "      break L;",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    L: L2: while (true) {",
        "      break L;",
        "    }",
        "  }",
        "}");
  }

  public void test_statementConstructorInvocation() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  public A() {",
        "    this(42);",
        "  }",
        "  public A(int p) {",
        "    print(p);",
        "  }",
        "}",
        "");
    assertDartSource(//
        "class A {",
        "  A.jtd_constructor_0_decl() {",
        "    thisConstructorRedirection(42);",
        "  }",
        "  A.jtd_constructor_1_decl(int p) {",
        "    print(p);",
        "  }",
        "}");
  }

  public void test_statementContinue() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    while (true) {",
        "      continue;",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    while (true) {",
        "      continue;",
        "    }",
        "  }",
        "}");
  }

  public void test_statementContinue_withLabel() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "   L: L2: while (true) {",
        "      continue L;",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    L: L2: while (true) {",
        "      continue L;",
        "    }",
        "  }",
        "}");
  }

  public void test_statementDo() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    do {",
        "      print(0);",
        "    } while (true);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    do {",
        "      print(0);",
        "    } while (true);",
        "  }",
        "}");
  }

  public void test_statementEmpty() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    ;",
        "    ;",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    ;",
        "    ;",
        "  }",
        "}");
  }

  public void test_statementFor() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    for (int i = 0; i < 10; i++) {",
        "      print(i);",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    for (int i = 0; i < 10; i++) {",
        "      print(i);",
        "    }",
        "  }",
        "}");
  }

  public void test_statementFor_noInitializer() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    int i = 0;",
        "    for (; i < 10; i++) {",
        "      print(i);",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    int i = 0;",
        "    for (; i < 10; i++) {",
        "      print(i);",
        "    }",
        "  }",
        "}");
  }

  public void test_statementFor2() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    int i;",
        "    for (i = 0; i < 10; i++) {",
        "      print(i);",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    int i;",
        "    for (i = 0; i < 10; i++) {",
        "      print(i);",
        "    }",
        "  }",
        "}");
  }

  public void test_statementForEach() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test(Iterable<String> items) {",
        "    for (String item: items) {",
        "      print(item);",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test(Iterable<String> items) {",
        "    for (String item in items) {",
        "      print(item);",
        "    }",
        "  }",
        "}");
  }

  public void test_statementIf() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    if (1 < 2) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    if (1 < 2) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
  }

  public void test_statementIfElse() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    if (1 < 2) {",
        "      print(1);",
        "    } else {",
        "      print(2);",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    if (1 < 2) {",
        "      print(1);",
        "    } else {",
        "      print(2);",
        "    }",
        "  }",
        "}");
  }

  public void test_statementReturn() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  int foo() {",
        "    return 42;",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  int foo() => 42;",
        "}");
  }

  public void test_statementSuperMethodInvocation() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test(int p) {}",
        "}",
        "public class B extends A {",
        "  void test() {",
        "    print(1);",
        "    super.test(2);",
        "    print(3);",
        "  }",
        "}",
        "");
    assertDartSource(
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
        "}");
  }

  public void test_statementSwitch() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void main(int p) {",
        "    switch (p) {",
        "      case 1:",
        "        if (0 == 1) {",
        "          print(0);",
        "          break;",
        "        }",
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
    assertDartSource(
        "class A {",
        "  void main(int p) {",
        "    while (true) {",
        "      if (p == 1) {",
        "        if (0 == 1) {",
        "          print(0);",
        "          break;",
        "        }",
        "        print(1);",
        "      } else if (p == 2 || p == 3) {",
        "        print(2);",
        "      } else {",
        "        print(3);",
        "      }",
        "      break;",
        "    }",
        "  }",
        "}");
  }

  public void test_statementSynchronized() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    synchronized (this) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(0);",
        "  }",
        "}");
  }

  public void test_statementTry_catch() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    try {",
        "      print(0);",
        "    } catch (E1 e) {",
        "      print(1);",
        "    } catch (E2 e) {",
        "      print(2);",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    try {",
        "      print(0);",
        "    } on E1 catch (e) {",
        "      print(1);",
        "    } on E2 catch (e) {",
        "      print(2);",
        "    }",
        "  }",
        "}");
  }

  public void test_statementTry_finally() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    try {",
        "      print(1);",
        "    } finally {",
        "      print(2);",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    try {",
        "      print(1);",
        "    } finally {",
        "      print(2);",
        "    }",
        "  }",
        "}");
  }

  public void test_statementVariableDeclaration() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    int a = 1;",
        "    int b = 2, c = 3;",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    int a = 1;",
        "    int b = 2, c = 3;",
        "  }",
        "}");
  }

  public void test_statementWhile() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    while (true) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    while (true) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
  }

  public void test_typeArray() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test(String[] items, String[][] labels) {",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test(List<String> items, List<List<String>> labels) {",
        "  }",
        "}");
  }

  public void test_typeArray2() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test(String items[], String labels[][]) {",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test(List<String> items, List<List<String>> labels) {",
        "  }",
        "}");
  }

  public void test_typeLiteral_wrapper() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
        "    print(Integer.class);",
        "    print(Double.class);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    print(int);",
        "    print(double);",
        "  }",
        "}");
  }

  public void test_typePrimitive() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test() {",
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
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    bool v1;",
        "    int v2;",
        "    int v3;",
        "    int v4;",
        "    int v5;",
        "    int v6;",
        "    double v7;",
        "    double v8;",
        "  }",
        "}");
  }

  public void test_typeWildcard() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import java.util.*;",
        "public class A {",
        "  void test() {",
        "    List<?> v1;",
        "    List<? extends String> v2;",
        "    Map<?, String> v3;",
        "    Map<?, ?> v4;",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test() {",
        "    List v1;",
        "    List<String> v2;",
        "    Map<dynamic, String> v3;",
        "    Map v4;",
        "  }",
        "}");
  }

  public void test_typeWrapper() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  Void testVoid() {return null;}",
        "  Boolean testBoolean() {return null;}",
        "  Short testShort() {return null;}",
        "  Integer testInteger() {return null;}",
        "  Long testLong() {return null;}",
        "  Float testFloat() {return null;}",
        "  Double testDouble() {return null;}",
        "  BigInteger testBigInteger() {return null;}",
        "}");
    assertDartSource(
        "class A {",
        "  Object testVoid() => null;",
        "  bool testBoolean() => null;",
        "  int testShort() => null;",
        "  int testInteger() => null;",
        "  int testLong() => null;",
        "  double testFloat() => null;",
        "  double testDouble() => null;",
        "  int testBigInteger() => null;",
        "}");
  }

  public void test_unitEmpty() throws Exception {
    parseJava();
    assertDartSource("");
  }

  /**
   * We can generate <code>List</code> for var-args declaration, but we don't know about them at
   * invocation point during syntax translation, only at semantic step.
   */
  public void test_varArgs() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test(int errorCode, Object ...args) {",
        "  }",
        "  void main() {",
        "    test(-1);",
        "    test(-1, 2, 3.0);",
        "  }",
        "}");
    assertDartSource(//
        "class A {",
        "  void test(int errorCode, List<Object> args) {",
        "  }",
        "  void main() {",
        "    test(-1);",
        "    test(-1, 2, 3.0);",
        "  }",
        "}");
  }

  void printFormattedSource() {
    translate();
    String source = toFormattedSource(dartUnit);
    String[] lines = StringUtils.split(source, '\n');
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      System.out.print("\"");
      line = StringUtils.replace(line, "\"", "\\\"");
      System.out.print(line);
      if (i != lines.length - 1) {
        System.out.println("\",");
      } else {
        System.out.println("\"");
      }
    }
  }

  /**
   * Translates {@link #javaUnit} into {@link #dartUnit} and check that it produces given Dart
   * source.
   */
  private void assertDartSource(String... lines) {
    translate();
    String actualDartSource = toFormattedSource(dartUnit);
    String expectedDartSource = Joiner.on("\n").join(lines);
    assertEquals(expectedDartSource, actualDartSource);
  }

  /**
   * Parse Java source lines into {@link #javaUnit}.
   */
  private void parseJava(String... lines) {
    javaSource = Joiner.on("\n").join(lines);
    ASTParser parser = ASTParser.newParser(AST.JLS4);
    parser.setCompilerOptions(ImmutableMap.of(
        JavaCore.COMPILER_SOURCE,
        JavaCore.VERSION_1_5,
        JavaCore.COMPILER_DOC_COMMENT_SUPPORT,
        JavaCore.ENABLED));
    parser.setSource(javaSource.toCharArray());
    javaUnit = (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(null);
    assertThat(javaUnit.getProblems()).isEmpty();
  }

  private void translate() {
    dartUnit = SyntaxTranslator.translate(context, javaUnit, javaSource);
  }
}
