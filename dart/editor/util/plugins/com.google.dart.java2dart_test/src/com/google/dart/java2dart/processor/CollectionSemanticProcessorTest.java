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
 * Test for {@link CollectionSemanticProcessor}.
 */
public class CollectionSemanticProcessorTest extends SemanticProcessorTest {
  public void test_ArrayList() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "import java.util.ArrayList;",
        "public class Test {",
        "  public List<String> foo() {",
        "    ArrayList<String> result = new ArrayList<String>();",
        "    return result;",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  List<String> foo() {",
        "    List<String> result = new List<String>();",
        "    return result;",
        "  }",
        "}");
  }

  public void test_Arrays_sort() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Arrays;",
        "public class Test {",
        "  public void foo(String[] items) {",
        "    Arrays.sort(items);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  void foo(List<String> items) {",
        "    items.sort();",
        "  }",
        "}");
  }

  public void test_Collection_isEmpty() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "public class Test {",
        "  public boolean foo(List<String> items) {",
        "    return items.isEmpty();",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  bool foo(List<String> items) => items.isEmpty;",
        "}");
  }

  public void test_Collection_size() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "public class Test {",
        "  public int foo(List<String> items) {",
        "    return items.size();",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  int foo(List<String> items) => items.length;",
        "}");
  }

  public void test_Comparator() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Comparator;",
        "public class Test {",
        "  Comparator<String> MY = new Comparator<String>() {",
        "    public int compare(String a, String b) {",
        "      return a.length() - b.length();",
        "    }",
        "  };",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  Comparator<String> MY = (String a, String b) => a.length() - b.length();",
        "}");
  }

  public void test_List_get() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "public class Test {",
        "  public String foo(List<String> items) {",
        "    return items.get(2);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  String foo(List<String> items) => items[2];",
        "}");
  }

  public void test_List_remove() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "public class Test {",
        "  public void foo(List<String> items) {",
        "    items.remove(2);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  void foo(List<String> items) {",
        "    items.removeAt(2);",
        "  }",
        "}");
  }

  public void test_List_toArray() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "public class Test {",
        "  String[] foo(List<String> items) {",
        "    return items.toArray(new String[items.length]);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  List<String> foo(List<String> items) => new List.from(items);",
        "}");
  }

  public void test_Map_get() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Map;",
        "public class Test {",
        "  public void foo(Map<Object, Object> items) {",
        "    items.get(this);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  void foo(Map<Object, Object> items) {",
        "    items[this];",
        "  }",
        "}");
  }

  public void test_Map_put() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Map;",
        "public class Test {",
        "  public void foo(Map<Object, Object> items) {",
        "    items.put(this, 42);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  void foo(Map<Object, Object> items) {",
        "    items[this] = 42;",
        "  }",
        "}");
  }
}
