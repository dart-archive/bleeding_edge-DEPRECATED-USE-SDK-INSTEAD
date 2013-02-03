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

  public void test_Arrays_equals() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "import java.util.Arrays;",
        "public class Test {",
        "  boolean main(List<String> a, List<String> b) {",
        "    return Arrays.equals(a, b);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  bool main(List<String> a, List<String> b) => JavaArrays.equals(a, b);",
        "}");
  }

  public void test_Arrays_hashCode() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "import java.util.Arrays;",
        "public class Test {",
        "  boolean main(List<String> a) {",
        "    return Arrays.hashCode(a);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  bool main(List<String> a) => JavaArrays.makeHashCode(a);",
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

  public void test_EnumSet() throws Exception {
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
        "import java.util.EnumSet;",
        "public class Test {",
        "  void main() {",
        "    EnumSet<MyEnum> set = EnumSet.noneOf(MyEnum.class);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void main() {",
        "    Set<MyEnum> set = new Set();",
        "  }",
        "}");
  }

  public void test_HashMap() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.HashMap;",
        "public class Test {",
        "  void main() {",
        "    HashMap<String, Integer> map = new HashMap<String, Integer>(5);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  void main() {",
        "    Map<String, int> map = new Map<String, int>();",
        "  }",
        "}");
  }

  public void test_HashSet_constructorSizeArgument() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.HashSet;",
        "public class Test {",
        "  public void main() {",
        "    HashSet<String> result = new HashSet<String>(5);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void main() {",
        "    Set<String> result = new Set<String>();",
        "  }",
        "}");
  }

  public void test_HashSet_toArray() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.HashSet;",
        "public class Test {",
        "  String[] main(HashSet<String> items) {",
        "    return items.toArray(new String[items.length]);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  List<String> main(Set<String> items) => new List.from(items);",
        "}");
  }

  public void test_Iterator() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.*;",
        "public class Test {",
        "  void main(List<String> items) {",
        "    Iterator<String> iter = items.iterator();",
        "    if (iter.hasNext()) {",
        "      iter.next();",
        "    }",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void main(List<String> items) {",
        "    HasNextIterator<String> iter = new HasNextIterator(items.iterator);",
        "    if (iter.hasNext) {",
        "      iter.next();",
        "    }",
        "  }",
        "}");
  }

  public void test_List_addAtIndex() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "public class Test {",
        "  public void foo(List<Integer> items) {",
        "    items.add(2, 42);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  void foo(List<int> items) {",
        "    items.insertRange(2, 1, 42);",
        "  }",
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

  public void test_List_remove_byIndex() throws Exception {
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

  public void test_List_remove_byValue() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "public class Test {",
        "  public void foo(List<String> items) {",
        "    items.remove(this);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test {",
        "  void foo(List<String> items) {",
        "    items.remove(this);",
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

  public void test_Map_entrySet() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Map;",
        "public class Test {",
        "  void main(Map<String, Integer> items) {",
        "    Set<Map.Entry<String, Integer>> entries = items.entrySet();",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void main(Map<String, int> items) {",
        "    Set<MapEntry<String, int>> entries = getMapEntrySet(items);", // from javalib
        "  }",
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

  public void test_Map_size() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Map;",
        "public class Test {",
        "  int main(Map<String, String> items) {",
        "    return items.size();",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  int main(Map<String, String> items) => items.length;",
        "}");
  }

  public void test_Map_values_keySet() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Map;",
        "public class Test {",
        "  void main(Map<String, Integer> items) {",
        "    items.values();",
        "    items.keySet();",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void main(Map<String, int> items) {",
        "    items.values;",
        "    items.keys.toSet();",
        "  }",
        "}");
  }

  public void test_Set_add() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Set;",
        "public class Test {",
        "  public void foo(Set<Integer> items) {",
        "    items.add(42);",
        "  }",
        "}");
    CollectionSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(//
        "class Test {",
        "  void foo(Set<int> items) {",
        "    javaSetAdd(items, 42);",
        "  }",
        "}");
  }
}
