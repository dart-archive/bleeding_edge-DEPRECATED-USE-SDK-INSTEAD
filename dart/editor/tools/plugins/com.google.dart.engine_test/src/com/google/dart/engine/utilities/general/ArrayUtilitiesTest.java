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
package com.google.dart.engine.utilities.general;

import junit.framework.TestCase;

public class ArrayUtilitiesTest extends TestCase {
  public void test_contains_emptyArray() {
    String[] array = {};
    String target = "a";
    assertFalse(ArrayUtilities.contains(array, target));
  }

  public void test_contains_first() {
    String[] array = {"a", "b", "c"};
    String target = "a";
    assertTrue(ArrayUtilities.contains(array, target));
  }

  public void test_contains_last() {
    String[] array = {"a", "b", "c"};
    String target = "c";
    assertTrue(ArrayUtilities.contains(array, target));
  }

  public void test_contains_middle() {
    String[] array = {"a", "b", "c"};
    String target = "b";
    assertTrue(ArrayUtilities.contains(array, target));
  }

  public void test_contains_nonExistent() {
    String[] array = {"a", "b", "c"};
    String target = "d";
    assertFalse(ArrayUtilities.contains(array, target));
  }

  public void test_containsAny_containsFirst() {
    String[] array = {"a", "b", "c"};
    String[] targets = {"b", "d"};
    assertTrue(ArrayUtilities.containsAny(array, targets));
  }

  public void test_containsAny_containsLast() {
    String[] array = {"a", "b", "c"};
    String[] targets = {"d", "b"};
    assertTrue(ArrayUtilities.containsAny(array, targets));
  }

  public void test_containsAny_containsNone() {
    String[] array = {"a", "b", "c"};
    String[] targets = {"d", "e"};
    assertFalse(ArrayUtilities.containsAny(array, targets));
  }
}
