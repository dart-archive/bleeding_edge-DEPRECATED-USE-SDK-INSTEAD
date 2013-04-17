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
 * Test for {@link MathSemanticProcessor}.
 */
public class MathSemanticProcessorTest extends SemanticProcessorTest {
  public void test_abs() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  int asbInt(int p) {",
        "    return Math.abs(p);",
        "  }",
        "  int asbLong(long p) {",
        "    return Math.abs(p);",
        "  }",
        "  int asbFloat(float p) {",
        "    return Math.abs(p);",
        "  }",
        "  int asbDouble(double p) {",
        "    return Math.abs(p);",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  int asbInt(int p) => p.abs();",
        "  int asbLong(int p) => p.abs();",
        "  int asbFloat(double p) => p.abs();",
        "  int asbDouble(double p) => p.abs();",
        "}");
  }

  private void runProcessor() {
    new MathSemanticProcessor(context).process(unit);
  }
}
