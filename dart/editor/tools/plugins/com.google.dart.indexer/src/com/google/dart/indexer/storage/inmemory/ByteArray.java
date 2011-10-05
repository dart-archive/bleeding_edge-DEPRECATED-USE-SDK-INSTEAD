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

import java.nio.ByteBuffer;

public final class ByteArray {
  byte[] data;
  ByteBuffer buffer;
  AbstractStringEncoder encoder;
  public int elementsCount = 0;

  public ByteArray(int capacity, AbstractStringEncoder encoder) {
    data = new byte[capacity];
    this.encoder = encoder;
  }

  public ByteArray(int elementsCount, byte[] data, AbstractStringEncoder encoder) {
    this.elementsCount = elementsCount;
    this.data = data;
    this.encoder = encoder;
  }

  public void add(byte value) {
    if (data.length == elementsCount) {
      expand();
    }
    data[elementsCount] = value;
    elementsCount++;
  }

  public void addInt(int add) {
    if (elementsCount + 4 >= data.length) {
      expand();
    }
    getBuffer().putInt(elementsCount, add);
    elementsCount += 4;
  }

  public int addString(String value) {
    int k = elementsCount;
    encoder.encode(value, this);
    String decode = encoder.decode(this, k);
    if (!decode.equals(value)) {
      encoder.decode(this, k); // XXX: leftover debugging code? turn into
                               // assertion?
    }
    return k;
  }

  public String decodeString(int element) {
    return encoder.decode(this, element);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    ByteArray other = (ByteArray) obj;
    int where = 0;
    boolean result = elementsCount == other.elementsCount
        && (arraysEqual(data, other.data) && (++where > 0));

    return result;
  }

  public boolean equalString(String s, int i) {
    return encoder.equals(this, i, s);
  }

  public ByteBuffer getBuffer() {
    if (buffer == null) {
      buffer = ByteBuffer.wrap(data);
    }
    return buffer;
  }

  public byte[] toArray() {
    if (data.length == elementsCount) {
      return data;
    } else {
      byte[] ret = new byte[elementsCount];
      System.arraycopy(data, 0, ret, 0, elementsCount);
      data = ret;
      return ret;
    }
  }

  private boolean arraysEqual(byte[] arr1, byte[] arr2) {
    int min = Math.min(arr1.length, arr2.length);
    for (int i = 0; i < min; i++) {
      if (arr1[i] != arr2[i]) {
        return false;
      }
    }
    return true;
  }

  private void expand() {
    buffer = null;
    byte[] newData = new byte[data.length * 2 + 1];
    System.arraycopy(data, 0, newData, 0, elementsCount);
    data = newData;
  }
}
