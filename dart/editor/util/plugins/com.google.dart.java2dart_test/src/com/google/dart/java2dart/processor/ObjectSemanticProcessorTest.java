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
 * Test for {@link ObjectSemanticProcessor}.
 */
public class ObjectSemanticProcessorTest extends SemanticProcessorTest {
  public void test_BigInteger_ZERO() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.math.BigInteger;",
        "public class Test {",
        "  public BigInteger main() {",
        "    return BigInteger.ZERO;",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  int main() => 0;",
        "}");
  }

  public void test_Boolean_or() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public boolean test(boolean a, boolean b) {",
        "    return a | b;",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  bool test(bool a, bool b) => javaBooleanOr(a, b);",
        "}");
  }

  public void test_Boolean_TRUE() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public Object testTrue() {",
        "    return Boolean.TRUE;",
        "  }",
        "  public Object testFalse() {",
        "    return Boolean.FALSE;",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  Object testTrue() => true;",
        "  Object testFalse() => false;",
        "}");
  }

  public void test_Class() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public <T> T getAncestor(Class<T> t) {",
        "    if (t.isInstance(this)) {",
        "      return (T) this;",
        "    }",
        "    return null;",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  Object getAncestor(Type t) {",
        "    if (isInstanceOf(this, t)) {", // from javalib
        "      return (this as Object);",
        "    }",
        "    return null;",
        "  }",
        "}");
  }

  public void test_Class_getName() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public <T> T getAncestor(Class<T> t) {",
        "    t.getName();",
        "    t.getSimpleName();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  Object getAncestor(Type t) {",
        "    t.toString();",
        "    t.toString();",
        "  }",
        "}");
  }

  public void test_Double_parseDouble() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public double foo(String p) {",
        "    return Double.parseDouble(p);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  double foo(String p) => double.parse(p);",
        "}");
  }

  public void test_Enum_ordinal() throws Exception {
    setFileLines(
        "test/MyEnum.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public enum MyEnum {",
            "  ONE, TWO;",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public int main(MyEnum p) {",
        "    return p.ordinal();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  int main(MyEnum p) => p.ordinal;",
        "}");
  }

  public void test_Enum_values() throws Exception {
    setFileLines(
        "test/MyEnum.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public enum MyEnum {",
            "  ONE, TWO;",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public MyEnum[] foo() {",
        "    return MyEnum.values();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  List<MyEnum> foo() => MyEnum.values;",
        "}");
  }

  public void test_extendsException() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test extends Exception {",
        "  public Test() {",
        "  }",
        "  public Test(String msg) {",
        "    super(msg);",
        "  }",
        "  public Test(String msg, Throwable e) {",
        "    super(msg, e);",
        "  }",
        "  public Test(Throwable e) {",
        "    super(e);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test extends JavaException {",
        "  Test() {",
        "    _jtd_constructor_0_impl();",
        "  }",
        "  _jtd_constructor_0_impl() {",
        "  }",
        "  Test.con1(String msg) : super(msg) {",
        "    _jtd_constructor_1_impl(msg);",
        "  }",
        "  _jtd_constructor_1_impl(String msg) {",
        "  }",
        "  Test.con2(String msg, Exception e) : super(msg, e) {",
        "    _jtd_constructor_2_impl(msg, e);",
        "  }",
        "  _jtd_constructor_2_impl(String msg, Exception e) {",
        "  }",
        "  Test.con3(Exception e) : super.withCause(e) {",
        "    _jtd_constructor_3_impl(e);",
        "  }",
        "  _jtd_constructor_3_impl(Exception e) {",
        "  }",
        "}");
  }

  public void test_IndexOutOfBoundsException() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public void mainA() {",
        "    throw new IndexOutOfBoundsException();",
        "  }",
        "  public void main() {",
        "    try {",
        "    } catch (IndexOutOfBoundsException e) {",
        "    }",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  void mainA() {",
        "    throw new RangeError();",
        "  }",
        "  void main() {",
        "    try {",
        "    } on RangeError catch (e) {",
        "    }",
        "  }",
        "}");
  }

  public void test_Integer_intValue() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public int main(Integer p) {",
        "    return p.intValue();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  int main(int p) => p;",
        "}");
  }

  public void test_Integer_MAX_VALUE() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public Object testMin() {",
        "    return Integer.MIN_VALUE;",
        "  }",
        "  public Object testMax() {",
        "    return Integer.MAX_VALUE;",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  Object testMin() => -2147483648;",
        "  Object testMax() => 2147483647;",
        "}");
  }

  public void test_Integer_parseInt() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public int test(String p) {",
        "    return Integer.parseInt(p);",
        "  }",
        "  public int testX(String p) {",
        "    return Integer.parseInt(p, 16);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  int test(String p) => int.parse(p);",
        "  int testX(String p) => int.parse(p, radix: 16);",
        "}");
  }

  public void test_Integer_valueOf() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public Object foo() {",
        "    return new Object[]{Integer.valueOf(42)};",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  Object foo() => <Object> [42];",
        "}");
  }

  public void test_Long_longValue() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public long main(Long p) {",
        "    return p.longValue();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  int main(int p) => p;",
        "}");
  }

  public void test_Long_valueOf() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public Object foo() {",
        "    return new Object[]{Long.valueOf(42)};",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  Object foo() => <Object> [42];",
        "}");
  }

  /**
   * In Dart method cannot have type parameters, so we replace them with bound.
   */
  public void test_methodTypeParameter() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test<E> {",
        "  public E testA(E p) {",
        "    return p;",
        "  }",
        "  public static <T extends String> T testB(T p) {",
        "    return p;",
        "  }",
        "  public static <T> T testC(T p) {",
        "    return p;",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test<E> {",
        "  E testA(E p) => p;",
        "  static String testB(String p) => p;",
        "  static Object testC(Object p) => p;",
        "}");
  }

  public void test_newBigInteger_fromString() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.math.*;",
        "public class Test {",
        "  public void main(String p) {",
        "    new BigInteger(p);",
        "    new BigInteger(p, 16);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void main(String p) {",
        "    int.parse(p);",
        "    int.parse(p, radix: 16);",
        "  }",
        "}");
  }

  public void test_Object_equals() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  Object o;",
        "  public boolean equals(Object o) {",
        "    return this.equals(o);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  Object o;",
        "  bool operator ==(Object o) => this == o;",
        "}");
  }

  public void test_Object_equals2() throws Exception {
    setFileLines(
        "test/MyInterface.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public interface MyInterface {",
            "  boolean equals(Object o);",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  MyInterface o;",
        "  boolean main(Object p) {",
        "    return 1 == 2 && o.equals(p);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  MyInterface o;",
        "  bool main(Object p) => 1 == 2 && o == p;",
        "}");
  }

  public void test_Object_getClass() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  Object o;",
        "  public Class<?> main() {",
        "    return o.getClass();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  Object o;",
        "  Type main() => o.runtimeType;",
        "}");
  }

  public void test_Object_hashCode() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  Object o;",
        "  public int hashCode() {",
        "    return o.hashCode();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  Object o;",
        "  int get hashCode => o.hashCode;",
        "}");
  }

  public void test_PrimitiveWrapper_operations() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.math.BigInteger;",
        "public class Test {",
        "  public void testBoolean(Boolean a, Boolean b) {",
        "    print(a.booleanValue());",
        "  }",
        "  public void testDouble(Double a, Double b) {",
        "    print(Double.valueOf(1.2));",
        "    print(a.doubleValue() + b.doubleValue());",
        "    print(a.equals(b));",
        "    print(!a.equals(b));",
        "    print(Math.floor(3.3 / 2).longValue());",
        "  }",
        "  public void testBigInteger(BigInteger a, BigInteger b) {",
        "    print(BigInteger.valueOf(42));",
        "    print(a.intValue());",
        "    print(a.equals(b));",
        "    print(!a.equals(b));",
        "    print(a.and(b));",
        "    print(a.or(b));",
        "    print(a.xor(b));",
        "    print(a.add(b));",
        "    print(a.subtract(b));",
        "    print(a.multiply(b));",
        "    print(a.divide(b));",
        "    print(a.shiftLeft(b));",
        "    print(a.shiftRight(b));",
        "    print(a.not());",
        "    print(a.negate());",
        "    print(a.doubleValue());",
        "  }",
        "  public void testString(String a, String b) {",
        "    print(a.equals(b));",
        "    print(!a.equals(b));",
        "  }",
        "  private static void print(Object p) {",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  void testBoolean(bool a, bool b) {",
        "    print(a);",
        "  }",
        "  void testDouble(double a, double b) {",
        "    print(1.2);",
        "    print(a + b);",
        "    print(a == b);",
        "    print(a != b);",
        "    print(3.3 ~/ 2);",
        "  }",
        "  void testBigInteger(int a, int b) {",
        "    print(42);",
        "    print(a);",
        "    print(a == b);",
        "    print(a != b);",
        "    print(a & b);",
        "    print(a | b);",
        "    print(a ^ b);",
        "    print(a + b);",
        "    print(a - b);",
        "    print(a * b);",
        "    print(a ~/ b);",
        "    print(a << b);",
        "    print(a >> b);",
        "    print(~a);",
        "    print(-a);",
        "    print(a.toDouble());",
        "  }",
        "  void testString(String a, String b) {",
        "    print(a == b);",
        "    print(a != b);",
        "  }",
        "  static void print(Object p) {",
        "  }",
        "}");
  }

  public void test_primitiveWrapper_toString() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public void mainInteger(int p) {",
        "    Integer.toString(p);",
        "  }",
        "  public void mainLong(long p) {",
        "    Long.toString(p);",
        "  }",
        "  public void mainDouble(double p) {",
        "    Double.toString(p);",
        "  }",
        "  public void mainDouble2(Double p) {",
        "    return p.toString();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  void mainInteger(int p) {",
        "    p.toString();",
        "  }",
        "  void mainLong(int p) {",
        "    p.toString();",
        "  }",
        "  void mainDouble(double p) {",
        "    p.toString();",
        "  }",
        "  void mainDouble2(double p) => p.toString();",
        "}");
  }

  public void test_PrintWriter_char() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.io.PrintWriter;",
        "public class Test {",
        "  public void main(PrintWriter p) {",
        "    p.print('[');",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void main(PrintWriter p) {",
        "    p.print('[');",
        "  }",
        "}");
  }

  public void test_PrintWriter_printlnString() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.io.PrintWriter;",
        "public class Test {",
        "  public void main(PrintWriter p) {",
        "    p.println(\"msg\");",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void main(PrintWriter p) {",
        "    p.printlnObject(\"msg\");",
        "  }",
        "}");
  }

  public void test_String_charAt() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public char foo(String s) {",
        "    return s.charAt(0);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  int foo(String s) => s.codeUnitAt(0);",
        "}");
  }

  public void test_String_concat_literals() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String testA(String name, int position) {",
        "    return \"Node \" + name + \" \\n at \" + position;",
        "  }",
        "  public String testB(String firstName, String lastName) {",
        "    return firstName + \".\" + lastName;",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  String testA(String name, int position) => \"Node ${name} \\n at ${position}\";",
        "  String testB(String firstName, String lastName) => \"${firstName}.${lastName}\";",
        "}");
  }

  public void test_String_concat_rewriteParts() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String test(String name, Class<?> type) {",
        "    return name + \" of \" + type.getName();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  String test(String name, Type type) => \"${name} of ${type.toString()}\";",
        "}");
  }

  public void test_String_concat_stringObjects() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String test(String firstName, String lastName) {",
        "    return firstName + '.' + lastName;",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  String test(String firstName, String lastName) => \"${firstName}.${lastName}\";",
        "}");
  }

  public void test_String_equalsIgnoreCase() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  boolean main(String a, String b) {",
        "    return a.equalsIgnoreCase(b);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  bool main(String a, String b) => javaStringEqualsIgnoreCase(a, b);",
        "}");
  }

  public void test_String_format() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String foo(String fmt, String name, int position) {",
        "    return String.format(fmt, name, position);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  String foo(String fmt, String name, int position) => JavaString.format(fmt, [name, position]);",
        "}");
  }

  public void test_String_indexOf_char() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public void main(String s) {",
        "    s.indexOf('1');",
        "    s.indexOf('2', 42);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void main(String s) {",
        "    s.indexOf('1');",
        "    s.indexOf('2', 42);",
        "  }",
        "}");
  }

  public void test_String_length() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  void main(String s) {",
        "    s.length();",
        "    s.isEmpty();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void main(String s) {",
        "    s.length;",
        "    s.isEmpty;",
        "  }",
        "}");
  }

  public void test_String_replace() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String foo(String s, String p, String r) {",
        "    return s.replace(p, r);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  String foo(String s, String p, String r) => s.replaceAll(p, r);",
        "}");
  }

  public void test_StringBuilder() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String main() {",
        "    StringBuilder sb = new StringBuilder(24);",
        "    sb.append(\"abc\");",
        "    sb.append(42);",
        "    sb.append('0');",
        "    sb.length();",
        "    sb.setLength(0);",
        "    return sb.toString();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  String main() {",
        "    JavaStringBuilder sb = new JavaStringBuilder();",
        "    sb.append(\"abc\");",
        "    sb.append(42);",
        "    sb.appendChar(0x30);",
        "    sb.length;",
        "    sb.length = 0;",
        "    return sb.toString();",
        "  }",
        "}");
  }

  public void test_Throwable_printStackTrace() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public main(Throwable e) {",
        "    e.printStackTrace();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  main(Exception e) {",
        "    print(e);",
        "  }",
        "}");
  }
}
