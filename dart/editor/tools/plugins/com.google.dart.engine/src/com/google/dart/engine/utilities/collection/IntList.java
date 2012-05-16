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
package com.google.dart.engine.utilities.collection;

/**
 * Instances of the class {@code IntList} implement an extensible list of integer values.
 */
public class IntList {
  /**
   * The number of elements in the list.
   */
  private int count;

  /**
   * An array containing the elements in the list.
   */
  private int[] values;

  /**
   * Initialize a new list of integers to be empty.
   */
  public IntList() {
    this(128);
  }

  /**
   * Initialize a new list of integers to be empty but to have room for the given number of elements
   * without having to grow.
   * 
   * @param initialCapacity the initial capacity of the list
   */
  public IntList(int initialCapacity) {
    count = 0;
    values = new int[initialCapacity];
  }

  /**
   * Add the given value to this list of values.
   * 
   * @param value the values to be added
   */
  public void add(int value) {
    int length = values.length;
    if (count >= length) {
      int[] newValues = new int[length + 64];
      System.arraycopy(values, 0, newValues, 0, length);
      values = newValues;
    }
    values[count++] = value;
  }

  /**
   * Return the number of elements in this list.
   * 
   * @return the number of elements in this list
   */
  public int size() {
    return count;
  }

  /**
   * Return an array containing all of the elements of this list, in the same order as they were
   * added to the list, that is exactly the same length as the list.
   * 
   * @return an array containing all of the elements of this list
   */
  public int[] toArray() {
    int[] result = new int[count];
    System.arraycopy(values, 0, result, 0, count);
    return result;
  }
}
