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
package com.google.dart.engine.utilities.source;

import junit.framework.TestCase;

public class LineInfoTest extends TestCase {
  public void test_creation() {
    assertNotNull(new LineInfo(new int[] {0}));
  }

  public void test_creation_empty() {
    try {
      new LineInfo(new int[] {});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_creation_null() {
    try {
      new LineInfo(null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_firstLine() {
    LineInfo info = new LineInfo(new int[] {0, 12, 34});
    LineInfo.Location location = info.getLocation(4);
    assertEquals(1, location.getLineNumber());
    assertEquals(5, location.getColumnNumber());
  }

  public void test_lastLine() {
    LineInfo info = new LineInfo(new int[] {0, 12, 34});
    LineInfo.Location location = info.getLocation(36);
    assertEquals(3, location.getLineNumber());
    assertEquals(3, location.getColumnNumber());
  }

  public void test_middleLine() {
    LineInfo info = new LineInfo(new int[] {0, 12, 34});
    LineInfo.Location location = info.getLocation(12);
    assertEquals(2, location.getLineNumber());
    assertEquals(1, location.getColumnNumber());
  }
}
