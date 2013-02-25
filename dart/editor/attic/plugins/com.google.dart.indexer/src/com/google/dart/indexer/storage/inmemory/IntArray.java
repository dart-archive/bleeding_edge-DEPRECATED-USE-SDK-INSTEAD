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
package com.google.dart.indexer.storage.inmemory;

public final class IntArray {
  public int[] data;
  public int elementsCount = 0;

  public IntArray(int capacity) {
    data = new int[capacity];
  }

  public IntArray(int[] data2) {
    elementsCount = data2.length;
    data = data2;
  }

  public void add(int value) {
    if (data.length == elementsCount) {
      int[] newData = new int[data.length * 2 + 1];
      for (int i = 0; i < elementsCount; i++) {
        newData[i] = data[i];
      }
      data = newData;
    }
    data[elementsCount] = value;
    elementsCount++;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    IntArray other = (IntArray) obj;
    return (elementsCount == other.elementsCount && arraysEqual(data, other.data));
  }

  public int[] toArray() {
    if (data.length == elementsCount) {
      return data;
    } else {
      int[] ret = new int[elementsCount];
      System.arraycopy(data, 0, ret, 0, elementsCount);
      data = ret;
      return ret;
    }
  }

  private boolean arraysEqual(int[] arr1, int[] arr2) {
    int min = Math.min(arr1.length, arr2.length);
    for (int i = 0; i < min; i++) {
      if (arr1[i] != arr2[i]) {
        return false;
      }
    }
    return true;
  }
}
