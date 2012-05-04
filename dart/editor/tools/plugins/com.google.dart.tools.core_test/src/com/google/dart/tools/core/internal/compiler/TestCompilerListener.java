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
package com.google.dart.tools.core.internal.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompilerListener;

/**
 * A listener for validating that zero or more specific errors occurred during a compilation.
 */
public class TestCompilerListener extends DartCompilerListener.Empty {
  private String[] messages;
  private int[] line;
  private int[] column;
  private int total;
  private int current;

  /**
   * Creates a listener with expected errors (if any).
   * 
   * @param errors a sequence of errors represented as triples of the form (msg, line, column)
   */
  public TestCompilerListener(Object... errors) {
    assertEquals("Invalid sequence of error expectations", 0, errors.length % 3);
    this.total = errors.length / 3;
    this.current = 0;
    this.messages = new String[total];
    this.line = new int[total];
    this.column = new int[total];
    for (int i = 0; i < total; i++) {
      this.messages[i] = (String) errors[3 * i];
      this.line[i] = (Integer) errors[(3 * i) + 1];
      this.column[i] = (Integer) errors[(3 * i) + 2];
    }
  }

  /**
   * Asserts that all expected errors were reported.
   */
  public void assertAllErrorsReported() {
    assertEquals("Not all expected errors were reported", total, current);
  }

  @Override
  public void onError(DartCompilationError event) {
    assertTrue(
        "More errors (" + (current + 1) + ") than expected (" + total + "):\n" + event,
        current < total);
    assertEquals("Wrong error message", messages[current], event.getMessage());
    assertEquals("Wrong line number", line[current], event.getLineNumber());
    assertEquals("Wrong column number", column[current], event.getColumnNumber());
    current++;
  }
}
