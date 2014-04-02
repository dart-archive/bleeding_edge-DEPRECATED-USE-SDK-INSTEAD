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
 * Test for {@link BeautifySemanticProcessor}.
 */
public class BeautifySemanticProcessorTest extends SemanticProcessorTest {
  public void test_expression_castIntToInt() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  public short main() {",
        "    return (short) 0xFFFF;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class A {",
        "  int main() => 0xFFFF;",
        "}");
  }

  public void test_expression_extraParenthesis() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  public void main(Object p) {",
        "    print((Integer) p);",
        "    print(-(Integer) p);",
        "    print((Integer) p + 1);",
        "    print((Integer) p - 1);",
        "    print(((Integer) p).hashCode());",
        "    print(((42)));",
        "    print(new int[] {(42)});",
        "    Object v = (Integer) p;",
        "    v = (Integer) p;",
        "    if((1 != 1)) {}",
        "  }",
        "  public int main2(Object p) {",
        "    return (Integer) p;",
        "  }",
        "  public int main3(Object p) {",
        "    print(0);",
        "    return (Integer) p;",
        "  }",
        "  public void print(Object x) {",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class A {",
        "  void main(Object p) {",
        "    print(p as int);",
        "    print(-(p as int));",
        "    print((p as int) + 1);",
        "    print((p as int) - 1);",
        "    print((p as int).hashCode());",
        "    print(42);",
        "    print(<int> [42]);",
        "    Object v = p as int;",
        "    v = p as int;",
        "    if (1 != 1) {",
        "    }",
        "  }",
        "  int main2(Object p) => p as int;",
        "  int main3(Object p) {",
        "    print(0);",
        "    return p as int;",
        "  }",
        "  void print(Object x) {",
        "  }",
        "}");
  }

  public void test_expression_instanceOf() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  public void test(Object p) {",
        "    boolean b1 = p instanceof String;",
        "    boolean b2 = !(p instanceof String);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class A {",
        "  void test(Object p) {",
        "    bool b1 = p is String;",
        "    bool b2 = p is! String;",
        "  }",
        "}");
  }

  private void runProcessor() {
    new BeautifySemanticProcessor(context).process(unit);
  }
}
