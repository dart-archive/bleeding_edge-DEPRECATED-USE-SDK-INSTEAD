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
 * Test for {@link DateSemanticProcessor}.
 */
public class DateSemanticProcessorTest extends SemanticProcessorTest {
  public void test_getTime() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Date;",
        "public class Test {",
        "  void main(Date date) {",
        "    return date.getTime();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void main(DateTime date) => date.millisecondsSinceEpoch;",
        "}");
  }

  public void test_getX() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Date;",
        "public class Test {",
        "  void main(Date p) {",
        "    p.getYear();",
        "    p.getMonth();",
        "    p.getDate();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void main(DateTime p) {",
        "    p.year;",
        "    p.month;",
        "    p.date;",
        "  }",
        "}");
  }

  public void test_new() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Date;",
        "public class Test {",
        "  void mainA() {",
        "    Date v = new Date();",
        "  }",
        "  void mainB() {",
        "    Date v = new Date(1234);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void mainA() {",
        "    DateTime v = new DateTime.now();",
        "  }",
        "  void mainB() {",
        "    DateTime v = new DateTime.fromMillisecondsSinceEpoch(1234);",
        "  }",
        "}");
  }

  public void test_toString() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.Date;",
        "public class Test {",
        "  String main(Date p) {",
        "    return date.toString();",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  String main(DateTime p) => date.toString();",
        "}");
  }

  private void runProcessor() {
    new DateSemanticProcessor(context).process(unit);
  }
}
