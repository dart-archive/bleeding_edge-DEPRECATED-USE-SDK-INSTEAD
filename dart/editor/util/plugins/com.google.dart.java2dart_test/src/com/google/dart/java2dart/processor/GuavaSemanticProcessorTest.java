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
 * Test for {@link GuavaSemanticProcessor}.
 */
public class GuavaSemanticProcessorTest extends SemanticProcessorTest {

  public void test_ImmutableMap_of_empty() throws Exception {
    setFileLines(
        "com/google/common/collect/ImmutableMap.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package com.google.common.collect;",
            "import java.util.Map;",
            "public class ImmutableMap {",
            "  public static <K, V> Map<K, V> of() { return null; }",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Map;",
        "import com.google.common.collect.ImmutableMap;",
        "public class Test {",
        "  Map<String, String> m = ImmutableMap.of();",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  Map<String, String> m = new Map();",
        "}");
  }

  public void test_Lists() throws Exception {
    setFileLines(
        "com/google/common/collect/Lists.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package com.google.common.collect;",
            "import java.util.ArrayList;",
            "import java.util.LinkedList;",
            "public class Lists {",
            "  public static <T> ArrayList<T> newArrayList() { return null; }",
            "  public static <T> LinkedList<T> newLinkedList() { return null; }",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "import com.google.common.collect.Lists;",
        "public class Test {",
        "  Object test_newArrayList() { return Lists.newArrayList(); }",
        "  Object test_newLinkedList() { return Lists.newLinkedList(); }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  Object test_newArrayList() => [];",
        "  Object test_newLinkedList() => new Queue();",
        "}");
  }

  public void test_Maps_newHashMap() throws Exception {
    setFileLines(
        "com/google/common/collect/Maps.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package com.google.common.collect;",
            "import java.util.HashMap;",
            "public class Maps {",
            "  public static <T> HashMap<T> newHashMap() { return null; }",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.List;",
        "import java.util.Map;",
        "import com.google.common.collect.Maps;",
        "public class Test {",
        "  Map<String, List<String>> m = Maps.newHashMap();",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  Map<String, List<String>> m = {};",
        "}");
  }

  public void test_Objects() throws Exception {
    setFileLines(
        "com/google/common/base/Objects.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package com.google.common.base;",
            "public class Objects {",
            "  public boolean equal(Object a, Object b) {return false;}",
            "  public boolean hashCode(Object ...elements) {return 0;}",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.common.base.Objects;",
        "public class Test {",
        "  public boolean run_equal(Object a, Object b) {",
        "    return Objects.equal(a, b);",
        "  }",
        "  public boolean run_equalNot(Object a, Object b) {",
        "    return !Objects.equal(a, b);",
        "  }",
        "  public boolean run_hashCode(Object a, Object b, Object c) {",
        "    return Objects.hashCode(a, b, c);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  bool run_equal(Object a, Object b) => a == b;",
        "  bool run_equalNot(Object a, Object b) => a != b;",
        "  bool run_hashCode(Object a, Object b, Object c) => JavaArrays.makeHashCode([a, b, c]);",
        "}");
  }

  public void test_Sets() throws Exception {
    setFileLines(
        "com/google/common/collect/Sets.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package com.google.common.collect;",
            "import java.util.Set;",
            "public class Sets {",
            "  public static <T> Set<T> newHashSet() { return null; }",
            "  public static <T> Set<T> difference(Set<T> s, Set<?> t) { return null; }",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Set;",
        "import com.google.common.collect.Sets;",
        "public class Test {",
        "  Object test_newHashSet() { return Sets.newHashSet(); }",
        "  Object test_difference(Set<String> s, Set<String> t) { return Sets.difference(s, t); }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  Object test_newHashSet() => new Set();",
        "  Object test_difference(Set<String> s, Set<String> t) => s.difference(t);",
        "}");
  }

  private void runProcessor() {
    new GuavaSemanticProcessor(context).process(unit);
  }
}
