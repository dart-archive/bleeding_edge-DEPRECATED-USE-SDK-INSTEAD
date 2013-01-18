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
    assertFormattedSource(
        "class Test {",
        "  List<MyEnum> foo() {",
        "    return MyEnum.values;",
        "  }",
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
        "  Object foo() {",
        "    return <Object> [42];",
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
    assertFormattedSource(
        "class Test {",
        "  Object o;",
        "  int get hashCode {",
        "    return o.hashCode;",
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
    assertFormattedSource(
        "class Test {",
        "  int foo(String s) {",
        "    return s.charCodeAt(0);",
        "  }",
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
        "  String foo(String fmt, String name, int position) {",
        "    return JavaString.format(fmt, [name, position]);",
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
    assertFormattedSource(
        "class Test {",
        "  int foo(String s) {",
        "    return s.length;",
        "  }",
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
