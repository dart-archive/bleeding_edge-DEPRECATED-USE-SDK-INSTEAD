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
package com.google.dart.engine;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.util.List;

/**
 * The class {@code EngineTestCase} defines utility methods for making assertions.
 */
public class EngineTestCase extends TestCase {
  /**
   * Assert that the array of actual values contain exactly the same values as those in the array of
   * expected value, with the exception that the order of the elements is not required to be the
   * same.
   * 
   * @param expectedValues the values that are expected to be found
   * @param actualValues the actual values that are being compared against the expected values
   */
  public static void assertEqualsIgnoreOrder(Object[] expectedValues, Object[] actualValues) {
    assertNotNull(actualValues);
    int expectedLength = expectedValues.length;
    assertEquals(expectedLength, actualValues.length);
    boolean[] found = new boolean[expectedLength];
    for (int i = 0; i < expectedLength; i++) {
      found[i] = false;
    }
    for (Object actualValue : actualValues) {
      boolean wasExpected = false;
      for (int i = 0; i < expectedLength; i++) {
        if (expectedValues[i].equals(actualValue) && !found[i]) {
          found[i] = true;
          wasExpected = true;
          break;
        }
      }
      if (!wasExpected) {
        fail("The actual value " + actualValue + " was not expected");
      }
    }
  }

  /**
   * Assert that the given object is an instance of the expected class.
   * 
   * @param expectedClass the class that the object is expected to be an instance of
   * @param object the object being tested
   * @return the object that was being tested
   * @throws Exception if the object is not an instance of the expected class
   */
  @SuppressWarnings("unchecked")
  public static <E> E assertInstanceOf(Class<E> expectedClass, Object object) {
    if (!expectedClass.isInstance(object)) {
      fail("Expected instance of " + expectedClass.getName() + ", found "
          + (object == null ? "null" : object.getClass().getName()));
    }
    return (E) object;
  }

  /**
   * Assert that the given array is non-{@code null} and has the expected number of elements.
   * 
   * @param expectedLength the expected number of elements
   * @param array the array being tested
   * @throws AssertionFailedError if the array is {@code null} or does not have the expected number
   *           of elements
   */
  public static void assertLength(int expectedLength, Object[] array) {
    if (array == null) {
      fail("Expected array of length " + expectedLength + "; found null");
    } else if (array.length != expectedLength) {
      fail("Expected array of length " + expectedLength + "; contained " + array.length
          + " elements");
    }
  }

  /**
   * Assert that the given list is non-{@code null} and has the expected number of elements.
   * 
   * @param expectedSize the expected number of elements
   * @param list the list being tested
   * @throws AssertionFailedError if the list is {@code null} or does not have the expected number
   *           of elements
   */
  public static void assertSize(int expectedSize, List<?> list) {
    if (list == null) {
      fail("Expected list of size " + expectedSize + "; found null");
    } else if (list.size() != expectedSize) {
      fail("Expected list of size " + expectedSize + "; contained " + list.size() + " elements");
    }
  }
}
