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
 * Test for {@link TypeSemanticProcessor}.
 */
public class TypeSemanticProcessorTest extends SemanticProcessorTest {
  public void test_forceTypeCast_enum() throws Exception {
    setFileLines("test/MyIntf.java", "package test; public class MyIntf {}");
    setFileLines("test/A.java", "package test; public class A implements Enum<A>, MyIntf {}");
    setFileLines("test/B.java", "package test; public class B implements Enum<B>, MyIntf {}");
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "public class Test {",
        "  void main(boolean z, A a, B b) {",
        "    MyIntf res = z ? a : b;",
        "  }",
        "}");
    runProcessor();
    assertFormattedSource(
        "class Test {",
        "  void main(bool z, A a, B b) {",
        "    MyIntf res = (z ? a : b) as MyIntf;",
        "  }",
        "}");
  }

  private void runProcessor() {
    new TypeSemanticProcessor(context).process(unit);
  }
}
