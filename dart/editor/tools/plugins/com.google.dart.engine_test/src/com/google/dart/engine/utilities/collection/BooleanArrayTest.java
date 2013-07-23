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
package com.google.dart.engine.utilities.collection;

import junit.framework.TestCase;

public class BooleanArrayTest extends TestCase {
  public void test_get_negative() {
    try {
      BooleanArray.get(0, -1);
      fail("Expected ");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_get_tooBig() {
    try {
      BooleanArray.get(0, 31);
      fail("Expected ");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_get_valid() {
    assertEquals(false, BooleanArray.get(0, 0));
    assertEquals(true, BooleanArray.get(1, 0));

    assertEquals(false, BooleanArray.get(0, 30));
    assertEquals(true, BooleanArray.get(1 << 30, 30));
  }

  public void test_set_negative() {
    try {
      BooleanArray.set(0, -1, true);
      fail("Expected ");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_set_tooBig() {
    try {
      BooleanArray.set(0, 32, true);
      fail("Expected ");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_set_valueChanging() {
    assertEquals(1, BooleanArray.set(0, 0, true));
    assertEquals(0, BooleanArray.set(1, 0, false));

    assertEquals(1 << 30, BooleanArray.set(0, 30, true));
    assertEquals(0, BooleanArray.set(1 << 30, 30, false));
  }

  public void test_set_valuePreserving() {
    assertEquals(0, BooleanArray.set(0, 0, false));
    assertEquals(1, BooleanArray.set(1, 0, true));

    assertEquals(0, BooleanArray.set(0, 30, false));
    assertEquals(1 << 30, BooleanArray.set(1 << 30, 30, true));
  }
}
