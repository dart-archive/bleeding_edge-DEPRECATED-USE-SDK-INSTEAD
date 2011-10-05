/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.util;

import java.util.Arrays;

/**
 * Instances of the class <code>ToStringSorter</code> take a collection of objects and returns a
 * sorted collection of these objects. The sorting of these objects is based on their toString().
 * They are sorted in alphabetical order.
 */
public class ToStringSorter<E> {
  /**
   * An array containing the objects in sorted order.
   */
  private E[] sortedObjects;

  /**
   * An array containing the Strings corresponding to the objects in sorted order.
   */
  private String[] sortedStrings;

  /**
   * Returns true if stringTwo is 'greater than' stringOne This is the 'ordering' method of the sort
   * operation.
   */
  public boolean compare(String stringOne, String stringTwo) {
    return stringOne.compareTo(stringTwo) < 0;
  }

  /**
   * Return an array containing the objects in sorted order.
   * 
   * @return an array containing the objects in sorted order
   */
  public E[] getSortedObjects() {
    return sortedObjects;
  }

  /**
   * Return an array containing the Strings corresponding to the objects in sorted order.
   * 
   * @return an array containing the Strings corresponding to the objects in sorted order
   */
  public String[] getSortedStrings() {
    return sortedStrings;
  }

  /**
   * Return a new sorted collection from this unsorted collection. Sort using quick sort.
   */
  public void sort(E[] unSortedObjects, String[] unsortedStrings) {
    int size = unSortedObjects.length;
    this.sortedObjects = Arrays.copyOf(unSortedObjects, size);
    this.sortedStrings = Arrays.copyOf(unsortedStrings, size);
    if (size > 1) {
      quickSort(0, size - 1);
    }
  }

  /**
   * Sort the objects in sorted collection and return that collection.
   */
  private void quickSort(int left, int right) {
    int originalLeft = left;
    int originalRight = right;
    int midIndex = left + (right - left) / 2;
    String midToString = this.sortedStrings[midIndex];

    do {
      while (compare(this.sortedStrings[left], midToString)) {
        left++;
      }
      while (compare(midToString, this.sortedStrings[right])) {
        right--;
      }
      if (left <= right) {
        E tmp = this.sortedObjects[left];
        this.sortedObjects[left] = this.sortedObjects[right];
        this.sortedObjects[right] = tmp;
        String tmpToString = this.sortedStrings[left];
        this.sortedStrings[left] = this.sortedStrings[right];
        this.sortedStrings[right] = tmpToString;
        left++;
        right--;
      }
    } while (left <= right);

    if (originalLeft < right) {
      quickSort(originalLeft, right);
    }
    if (left < originalRight) {
      quickSort(left, originalRight);
    }
  }
}
