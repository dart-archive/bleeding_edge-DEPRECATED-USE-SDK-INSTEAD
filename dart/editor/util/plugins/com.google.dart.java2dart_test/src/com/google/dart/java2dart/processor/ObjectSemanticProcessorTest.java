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
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  int main() => 0;",
        "}");
  }

  public void test_Boolean_and() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public boolean testAnd2(boolean a, boolean b) {",
        "    return a & b;",
        "  }",
        "  public boolean testAnd3(boolean a, boolean b, boolean c) {",
        "    return a & b & c;",
        "  }",
        "  public void testAndEq(boolean a, boolean b) {",
        "    a &= b;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  bool testAnd2(bool a, bool b) => javaBooleanAnd(a, b);",
        "  bool testAnd3(bool a, bool b, bool c) => javaBooleanAnd(javaBooleanAnd(a, b), c);",
        "  void testAndEq(bool a, bool b) {",
        "    a = javaBooleanAnd(a, b);",
        "  }",
        "}");
  }

  public void test_Boolean_or() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public boolean test1(boolean a, boolean b) {",
        "    return a | b;",
        "  }",
        "  public void test2(boolean a, boolean b) {",
        "    a |= b;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  bool test1(bool a, bool b) => javaBooleanOr(a, b);",
        "  void test2(bool a, bool b) {",
        "    a = javaBooleanOr(a, b);",
        "  }",
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
    runProcessor();
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
    runProcessor();
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
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  Object getAncestor(Type t) {",
        "    t.toString();",
        "    t.toString();",
        "  }",
        "}");
  }

  public void test_constructsRuntimeException() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public void test() throws Exception {",
        "    throw new RuntimeException(\"hello\");",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void test() {",
        "    throw new RuntimeException(message: \"hello\");",
        "  }",
        "}");
  }

  public void test_double_castTo_int() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public int testA(double p) {",
        "    return (int) p;",
        "  }",
        "  public long testB(double p) {",
        "    return (long) p;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  int testA(double p) => p.toInt();",
        "  int testB(double p) => p.toInt();",
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
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  double foo(String p) => double.parse(p);",
        "}");
  }

  public void test_Enum_name() throws Exception {
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
        "  public void test() {",
        "    return MyEnum.ONE.name();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void test() => MyEnum.ONE.name;",
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
    runProcessor();
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
    runProcessor();
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
    runProcessor();
    assertFormattedSource(
        "class Test extends JavaException {",
        "  Test();",
        "  Test.con1(String msg) : super(msg);",
        "  Test.con2(String msg, Exception e) : super(msg, e);",
        "  Test.con3(Exception e) : super.withCause(e);",
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
    runProcessor();
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
    runProcessor();
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
    runProcessor();
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
        "    try {",
        "      return Integer.parseInt(p);",
        "    } catch (NumberFormatException e) {",
        "      return 0;",
        "    }",
        "  }",
        "  public int testX(String p) {",
        "    return Integer.parseInt(p, 16);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  int test(String p) {",
        "    try {",
        "      return int.parse(p);",
        "    } on FormatException catch (e) {",
        "      return 0;",
        "    }",
        "  }",
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
    runProcessor();
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
    runProcessor();
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
    runProcessor();
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
    runProcessor();
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
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void main(String p) {",
        "    int.parse(p);",
        "    int.parse(p, radix: 16);",
        "  }",
        "}");
  }

  public void test_Number_intValue() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public int main(Number p) {",
        "    return p.intValue();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  int main(num p) => p.toInt();",
        "}");
  }

  public void test_Object_equals() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public Object p;",
        "  public boolean equals(Object o) {",
        "    return o instanceof Test && p.equals(((Test) o).p);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  Object p;",
        "  bool operator ==(Object o) => o is Test && p == ((o as Test)).p;",
        "}");
  }

  public void test_Object_equals_implicitThis() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public boolean equals(Object o) {",
        "    return equals(o);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
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
        "  public MyInterface o;",
        "  public boolean main(Object p) {",
        "    return 1 == 2 && o.equals(p);",
        "  }",
        "}");
    runProcessor();
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
        "  public Object o;",
        "  public Class<?> main() {",
        "    return o.getClass();",
        "  }",
        "}");
    runProcessor();
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
        "  public Object o;",
        "  public int hashCode() {",
        "    return o.hashCode();",
        "  }",
        "}");
    runProcessor();
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
        "  public static void print(Object p) {",
        "  }",
        "}");
    runProcessor();
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
        "    Integer.toString(p, 16);",
        "  }",
        "  public void mainLong(long p) {",
        "    Long.toString(p);",
        "    Long.toString(p, 16);",
        "  }",
        "  public void mainDouble(double p) {",
        "    Double.toString(p);",
        "  }",
        "  public void mainDouble2(Double p) {",
        "    return p.toString();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void mainInteger(int p) {",
        "    p.toString();",
        "    p.toRadixString(16);",
        "  }",
        "  void mainLong(int p) {",
        "    p.toString();",
        "    p.toRadixString(16);",
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
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void main(PrintWriter p) {",
        "    p.print('[');",
        "  }",
        "}");
  }

  public void test_PrintWriter_println() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.io.PrintWriter;",
        "public class Test {",
        "  public void main(PrintWriter p, String s) {",
        "    p.println();",
        "    p.println(s);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void main(PrintWriter p, String s) {",
        "    p.newLine();",
        "    p.println(s);",
        "  }",
        "}");
  }

  public void test_regex_Matcher() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.regex.*;",
        "public class Test {",
        "  public void main(Pattern p, String s) {",
        "    Matcher m = p.matcher(s);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void main(RegExp p, String s) {",
        "    JavaPatternMatcher m = new JavaPatternMatcher(p, s);",
        "  }",
        "}");
  }

  public void test_regex_Pattern() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.regex.*;",
        "public class Test {",
        "  public void main(String s) {",
        "    Pattern.compile(s);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void main(String s) {",
        "    new RegExp(s);",
        "  }",
        "}");
  }

  public void test_Set_equals() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Set",
        "public class Test {",
        "  public boolean main(Set a, Set b) {",
        "    return a.equals(b);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  bool main(Set a, Set b) => javaSetEquals(a, b);",
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
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  int foo(String s) => s.codeUnitAt(0);",
        "}");
  }

  public void test_String_concat() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String test(String a, String b) {",
        "    return a.concat(b);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  String test(String a, String b) => a + b;",
        "}");
  }

  public void test_String_concat_charString() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String test(char c, String rest) {",
        "    return c + rest;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  String test(int c, String rest) => \"${new String.fromCharCode(c)}${rest}\";",
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
    runProcessor();
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
    runProcessor();
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
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  String test(String firstName, String lastName) => \"${firstName}.${lastName}\";",
        "}");
  }

  public void test_String_contains() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String test(String a, String b) {",
        "    return a.contains(b);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  String test(String a, String b) => a.contains(b);",
        "}");
  }

  public void test_String_equalsIgnoreCase() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public boolean main(String a, String b) {",
        "    return a.equalsIgnoreCase(b);",
        "  }",
        "}");
    runProcessor();
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
    runProcessor();
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
        "    s.lastIndexOf('1');",
        "    s.lastIndexOf('2', 42);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void main(String s) {",
        "    s.indexOf('1');",
        "    JavaString.indexOf(s, '2', 42);",
        "    s.lastIndexOf('1');",
        "    JavaString.lastIndexOf(s, '2', 42);",
        "  }",
        "}");
  }

  /**
   * In Java using invalid index given {@code -1}, but in Dart - exception.
   */
  public void test_String_indexOf_fromIndex() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public void main(String s) {",
        "    s.indexOf(\"x\", -1);",
        "    s.indexOf(\"x\", 10);",
        "    s.indexOf('y', 10);",
        "    s.lastIndexOf(\"x\", -1);",
        "    s.lastIndexOf(\"x\", 10);",
        "    s.lastIndexOf('y', 10);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void main(String s) {",
        "    JavaString.indexOf(s, \"x\", -1);",
        "    JavaString.indexOf(s, \"x\", 10);",
        "    JavaString.indexOf(s, 'y', 10);",
        "    JavaString.lastIndexOf(s, \"x\", -1);",
        "    JavaString.lastIndexOf(s, \"x\", 10);",
        "    JavaString.lastIndexOf(s, 'y', 10);",
        "  }",
        "}");
  }

  public void test_String_length() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public void main(String s) {",
        "    s.length();",
        "    s.isEmpty();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void main(String s) {",
        "    s.length;",
        "    s.isEmpty;",
        "  }",
        "}");
  }

  public void test_String_plusEqualsChar() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public void test(String s) {",
        "    s += '=';",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  void test(String s) {",
        "    s += '=';",
        "  }",
        "}");
  }

  public void test_String_regionMatches() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public boolean main(String a, String b) {",
        "    return a.regionMatches(0, b, 2, 3);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  bool main(String a, String b) => javaStringRegionMatches(a, 0, b, 2, 3);",
        "}");
  }

  public void test_String_replace() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String testA(String s, String p, String r) {",
        "    return s.replace(p, r);",
        "  }",
        "  public String testB(String s, String r) {",
        "    return s.replace('/', r);",
        "  }",
        "  public String testC(String s, char r) {",
        "    return s.replace('/', r);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  String testA(String s, String p, String r) => s.replaceAll(p, r);",
        "  String testB(String s, String r) => s.replaceAll('/', r);",
        "  String testC(String s, int r) => s.replaceAll('/', new String.fromCharCode(r));",
        "}");
  }

  public void test_String_startsWith() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public boolean main(String s, String prefix, int index) {",
        "    return s.startsWith(prefix, index);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  bool main(String s, String prefix, int index) => JavaString.startsWithBefore(s, prefix, index);",
        "}");
  }

  public void test_String_valueOf_char() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public String test(char c) {",
        "    String.valueOf(c);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  String test(int c) {",
        "    new String.fromCharCode(c);",
        "  }",
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
    runProcessor();
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

  public void test_Throwable_getCause() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public main(Throwable e) {",
        "    e.getCause();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  main(Exception e) {",
        "    e.cause;",
        "  }",
        "}");
  }

  public void test_Throwable_getMessage() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  public main(Throwable e) {",
        "    e.getMessage();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  main(Exception e) {",
        "    e.toString();",
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
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  main(Exception e) {",
        "    print(e);",
        "  }",
        "}");
  }

  private void runProcessor() {
    new ObjectSemanticProcessor(context).process(unit);
  }
}
