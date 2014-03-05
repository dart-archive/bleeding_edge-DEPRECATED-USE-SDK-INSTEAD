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

/**
 * The class {@code BooleanArray} defines methods for operating on integers as if they were arrays
 * of booleans. These arrays can be indexed by either integers or by enumeration constants.
 */
public final class BooleanArray {
  /**
   * Return the value of the element at the given index.
   * 
   * @param array the array being accessed
   * @param index the index of the element being accessed
   * @return the value of the element at the given index
   * @throws IndexOutOfBoundsException if the index is not between zero (0) and 31, inclusive
   */
  public static boolean get(int array, int index) {
    checkIndex(index);
    return (array & (1 << index)) > 0;
  }

  /**
   * Return the value of the element at the given index.
   * 
   * @param array the array being accessed
   * @param index the index of the element being accessed
   * @return the value of the element at the given index
   * @throws IndexOutOfBoundsException if the index is not between zero (0) and 31, inclusive
   */
  public static boolean getEnum(int array, Enum<?> index) {
    return get(array, index.ordinal());
  }

  /**
   * Set the value of the element at the given index to the given value.
   * 
   * @param array the array being modified
   * @param index the index of the element being set
   * @param value the value to be assigned to the element
   * @return the updated value of the array
   * @throws IndexOutOfBoundsException if the index is not between zero (0) and 31, inclusive
   */
  public static int set(int array, int index, boolean value) {
    checkIndex(index);
    if (value) {
      return array | (1 << index);
    } else {
      return array & ~(1 << index);
    }
  }

  /**
   * Set the value of the element at the given index to the given value.
   * 
   * @param array the array being modified
   * @param index the index of the element being set
   * @param value the value to be assigned to the element
   * @return the updated value of the array
   * @throws IndexOutOfBoundsException if the index is not between zero (0) and 31, inclusive
   */
  public static int setEnum(int array, Enum<?> index, boolean value) {
    return set(array, index.ordinal(), value);
  }

  /**
   * Throw an exception if the index is not within the bounds allowed for an integer-encoded array
   * of boolean values.
   * 
   * @throws IndexOutOfBoundsException if the index is not between zero (0) and 31, inclusive
   */
  private static void checkIndex(int index) {
    if (index < 0 || index > 30) {
      throw new IndexOutOfBoundsException("Index not between 0 and 30: " + index);
    }
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private BooleanArray() {
    super();
  }
}
