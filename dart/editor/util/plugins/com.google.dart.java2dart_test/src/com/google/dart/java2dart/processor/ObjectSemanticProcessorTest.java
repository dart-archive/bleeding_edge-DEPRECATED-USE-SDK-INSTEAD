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

  public void test_Integer_toString() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String foo(int p) {",
        "    return Integer.toString(p);",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  String foo(int p) => p.toString();",
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
        "    print(a / b);",
        "    print(a << b);",
        "    print(a >> b);",
        "    print(~a);",
        "    print(-a);",
        "  }",
        "  void testString(String a, String b) {",
        "    print(a == b);",
        "    print(a != b);",
        "  }",
        "  static void print(Object p) {",
        "  }",
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
        "  int foo(String s) => s.charCodeAt(0);",
        "}");
  }

  public void test_String_concat() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String testA(String name, int position) {",
        "    return \"Node \" + name + \" at \" + position;",
        "  }",
        "  public String testB(String firstName, String lastName) {",
        "    return firstName + \".\" + lastName;",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  String testA(String name, int position) => \"Node ${name} at ${position}\";",
        "  String testB(String firstName, String lastName) => \"${firstName}.${lastName}\";",
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
        "  public int foo(String s) {",
        "    return s.length();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  int foo(String s) => s.length;",
        "}");
  }

  public void test_StringBuilder() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String main() {",
        "    StringBuilder sb = new StringBuilder();",
        "    sb.append(\"abc\");",
        "    sb.append(42);",
        "    return sb.toString();",
        "  }",
        "}");
    ObjectSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  String main() {",
        "    StringBuffer sb = new StringBuffer();",
        "    sb.add(\"abc\");",
        "    sb.add(42);",
        "    return sb.toString();",
        "  }",
        "}");
  }
}
