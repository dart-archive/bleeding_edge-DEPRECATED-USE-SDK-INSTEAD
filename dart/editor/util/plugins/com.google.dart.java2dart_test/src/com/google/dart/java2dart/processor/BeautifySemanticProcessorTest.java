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
  public void test_expression_instanceOf() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void test(Object p) {",
        "    boolean b1 = p instanceof String;",
        "    boolean b2 = !(p instanceof String);",
        "  }",
        "}");
    BeautifySemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class A {",
        "  void test(Object p) {",
        "    bool b1 = p is String;",
        "    bool b2 = p is! String;",
        "  }",
        "}");
  }
}
