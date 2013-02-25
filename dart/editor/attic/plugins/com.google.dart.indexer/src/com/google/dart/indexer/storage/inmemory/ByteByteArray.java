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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public final class ByteByteArray {
  public byte[][] data;
  public int elementsCount = 0;

  public ByteByteArray(DataInputStream stream) throws IOException {
    this.elementsCount = stream.readInt();
    data = new byte[elementsCount][];
    for (int a = 0; a < elementsCount; a++) {
      int readInt = stream.readInt();
      if (readInt != 0) {
        byte[] ls = new byte[readInt];
        stream.readFully(ls);
        data[a] = ls;
      }
    }
  }

  public ByteByteArray(int capacity) {
    data = new byte[capacity][];
  }

  public void add(byte[] value) {
    if (data.length == elementsCount) {
      byte[][] newData = new byte[data.length * 2][];
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
    if (this == obj) {
      return true;
    }
    ByteByteArray other = (ByteByteArray) obj;

    return elementsCount == other.elementsCount && arrayEqual(data, other.data, elementsCount);
  }

  public byte[][] toArray() {
    if (data.length == elementsCount) {
      return data;
    } else {
      byte[][] ret = new byte[elementsCount][];
      System.arraycopy(data, 0, ret, 0, elementsCount);
      data = ret;
      return ret;
    }
  }

  void store(DataOutputStream stream) throws IOException {
    stream.writeInt(elementsCount);
    for (int a = 0; a < elementsCount; a++) {
      byte[] b = data[a];
      if (b == null) {
        stream.writeInt(0);
      } else {
        stream.writeInt(b.length);
        stream.write(b);
      }
    }
  }

  private boolean arrayEqual(byte[][] data2, byte[][] data3, int count) {
    if (data2 == data3) {
      return true;
    }
    // if (data2.length != data3.length) return false;
    for (int i = 0; i < count; i++) {
      if (!Arrays.equals(data2[i], data3[i])) {
        return false;
      }
    }
    return true;
  }
}
