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
 * Test for {@link LoggerSemanticProcessor}.
 */
public class LoggerSemanticProcessorTest extends SemanticProcessorTest {
  public void test_getLogger() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import java.util.logging.Logger;",
        "public class Test {",
        "  static Logger LOGGER = Logger.getLogger(Test.class.getName());",
        "}");
    runProcessor();
    assertFormattedSource(//
        "class Test {",
        "  static Logger LOGGER = new Logger('Test');",
        "}");
  }

  private void runProcessor() {
    new LoggerSemanticProcessor(context).process(unit);
  }
}
