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

import com.google.dart.java2dart.engine.EngineSemanticProcessor;

/**
 * Test for {@link EngineSemanticProcessor}.
 */
public class EngineSemanticProcessorTest extends SemanticProcessorTest {

  public void test_IntList() throws Exception {
    setFileLines(
        "com/google/dart/engine/utilities/collection/IntList.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package com.google.dart.engine.utilities.collection;",
            "public class IntList {",
            "  public void add(Object o) {}",
            "  public int[] toArray() {return null;}",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.dart.engine.utilities.collection.IntList;",
        "public class Test {",
        "  public int[] foo() {",
        "    IntList lineStarts = new IntList();",
        "    lineStarts.add(42);",
        "    return lineStarts.toArray();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  List<int> foo() {",
        "    List<int> lineStarts = new List<int>();",
        "    lineStarts.add(42);",
        "    return lineStarts;",
        "  }",
        "}");
  }

  public void test_ObjectUtilities() throws Exception {
    setFileLines(
        "com/google/dart/engine/utilities/general/ObjectUtilities.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package com.google.dart.engine.utilities.general;",
            "public class ObjectUtilities {",
            "  public boolean equals(Object o1, Object o2) { return false; }",
            "}"));
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import com.google.dart.engine.utilities.general.ObjectUtilities;",
        "public class Test {",
        "  public void test_equals(Object o1, Object o2) {",
        "    boolean b1 = ObjectUtilities.equals(o1, o2);",
        "    boolean b2 = !ObjectUtilities.equals(o1, o2);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void test_equals(Object o1, Object o2) {",
        "    bool b1 = (o1 == o2);",
        "    bool b2 = (o1 != o2);",
        "  }",
        "}");
  }

  private void runProcessor() {
    new EngineSemanticProcessor(context).process(unit);
  }
}
