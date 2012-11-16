/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.formatter;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.formatter.CodeFormatter.Kind;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

/**
 * Synthesizes formatting tests out of input read from a companion file (
 * {@code CodeFormatterTest.data}).
 * <p>
 * Data is in a simple format, where {@code<<<<} delimits the start of the expected formatted
 * content and {@code>>>>} delimits the actual.
 */
public class CodeFormatterTest extends EngineTestCase {

  private static final String TEST_NAME_PREFIX = "format_test_";

  /**
   * Used to generate unique names for tests (e.g., {@code format_test_1}).
   */
  private static int testCount;

  /**
   * Create the test suite.
   */
  public static Test suite() throws IOException {
    return new DataDrivenTestBuilder(CodeFormatterTest.class) {
      @Override
      protected void addTest(TestSuite suite, String expected, String actual) {
        if (actual.length() > 0) {
          suite.addTest(new CodeFormatterTest(expected, actual));
        }
      }
    }.buildSuite();
  }

  private final String expected;
  private final String actual;

  /**
   * Creates a test instance.
   * 
   * @param expected the expected formatted value (as a String)
   * @param actual the actual formatted value (as a String)
   */
  public CodeFormatterTest(String expected, String actual) {
    setName(TEST_NAME_PREFIX + ++testCount);
    this.expected = expected;
    this.actual = actual;
  }

  @Override
  protected void runTest() throws Throwable {
    assertEqualString(expected, format(actual));
  }

  private String format(String source) throws FormatterException {
    return CodeFormatterRunner.getDefault().format(source, Kind.COMPILATION_UNIT);
  }

}
