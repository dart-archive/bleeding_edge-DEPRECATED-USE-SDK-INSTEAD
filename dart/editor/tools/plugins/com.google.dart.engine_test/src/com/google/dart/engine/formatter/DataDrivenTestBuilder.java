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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Data driven test suite builder.
 * <p>
 * Synthesizes tests out of input read from a companion data file, found relative to the test class
 * and named by convention <code>TestClass.class.getSimpleName() + ".data"</code>.
 */
public abstract class DataDrivenTestBuilder {

  private static final String ACTUAL_START_TOKEN = "<<<<";
  private static final String EXPECTED_START_TOKEN = ">>>>";

  private static final String NEW_LINE = System.getProperty("line.separator");

  private static InputStream openDataStream(Class<?> cls) {
    return cls.getResourceAsStream(cls.getSimpleName() + ".data");
  }

  private final Class<? extends TestCase> testClass;

  /**
   * Create a builder instance.
   * 
   * @param testClass the test class
   */
  DataDrivenTestBuilder(Class<? extends TestCase> testClass) {
    this.testClass = testClass;
  }

  public final Test buildSuite() throws IOException {
    TestSuite suite = new TestSuite();
    addTests(suite);
    return suite;
  }

  protected abstract void addTest(TestSuite suite, String expected, String actual);

  private void addTest(TestSuite suite, StringBuilder expected, StringBuilder actual) {
    addTest(suite, expected.toString(), actual.toString());
  }

  private void addTests(TestSuite suite) throws IOException {

    InputStream in = openDataStream(testClass);
    InputStreamReader is = new InputStreamReader(in);
    BufferedReader br = new BufferedReader(is);

    StringBuilder expected = new StringBuilder();
    StringBuilder actual = new StringBuilder();

    StringBuilder sb = null;

    try {

      String read = br.readLine();

      while (read != null) {
        if (read.startsWith(ACTUAL_START_TOKEN)) {
          addTest(suite, actual, expected);
          actual.setLength(0);
          expected.setLength(0);
          sb = expected;
        } else if (read.startsWith(EXPECTED_START_TOKEN)) {
          sb = actual;
        } else {
          sb.append(read);
          sb.append(NEW_LINE);
        }
        read = br.readLine();
      }

    } finally {
      in.close();
    }

    addTest(suite, expected, actual);
  }
}
