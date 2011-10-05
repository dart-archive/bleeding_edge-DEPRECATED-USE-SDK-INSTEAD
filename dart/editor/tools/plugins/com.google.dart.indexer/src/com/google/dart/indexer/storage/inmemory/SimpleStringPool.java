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

import java.util.Arrays;

public class SimpleStringPool {
  ByteArray array;

  int[] ids;

  int size;

  IntArray positions;

  public SimpleStringPool(int initialCapacity) {
    this.ids = new int[initialCapacity];
    array = new ByteArray(10000, new PlainStringEncoder());
    positions = new IntArray(4000);
    positions.add(0);

  }

  public SimpleStringPool(int i, ByteArray array2) {
    this.ids = new int[i];
    array = array2;
  }

  public SimpleStringPool(int sz, int[] idds, IntArray poss, ByteArray br) {
    this.array = br;
    this.size = sz;
    this.positions = poss;
    this.ids = idds;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    SimpleStringPool other = (SimpleStringPool) obj;
    return (size == other.size && Arrays.equals(this.ids, other.ids)
        && positions.equals(other.positions) && array.equals(other.array));
  }

  public String getString(int id) {
    return array.decodeString(positions.data[id]);
  }

  public int memUsed() {
    return ids.length * 4 + positions.data.length * 4 + array.elementsCount + 36;
  }

  int add(String s) {
    int hashCode = s.hashCode();
    int pos = Math.abs(hashCode % ids.length);
    int i = ids[pos];
    while (i != 0) {
      if (array.equalString(s, positions.data[i])) {
        return i;
      }
      pos++;
      if (pos >= ids.length) {
        pos = 0;
      }
      i = ids[pos];
    }
    int elementsCount = positions.elementsCount;
    ids[pos] = elementsCount;
    positions.add(array.addString(s));

    size++;
    if (size > ((2 * ids.length) / 3)) {
      rehash();
    }
    return elementsCount;
  }

  void addUnsafe(String s, int id) {
    int hashCode = s.hashCode();
    int pos = Math.abs(hashCode % ids.length);
    int i = (ids[pos]);
    while (i != 0) {
      pos++;
      if (pos >= ids.length) {
        pos = 0;
      }
      i = ids[pos];
    }
    ids[pos] = id;
    size++;
  }

  int get(String s) {
    int hashCode = s.hashCode();
    int pos = Math.abs(hashCode % ids.length);
    int i = ids[pos];
    while (i != 0) {
      if (array.equalString(s, positions.data[i])) {
        return i;
      }
      pos++;
      if (pos >= ids.length) {
        pos = 0;
      }
      i = ids[pos];
    }
    return -1;
  }

  private void rehash() {
    SimpleStringPool simpleStringPool = new SimpleStringPool(this.ids.length * 2, this.array);
    for (int a = 0; a < ids.length; a++) {
      int k = ids[a];
      if (k != 0) {
        int pos = positions.data[k];
        simpleStringPool.addUnsafe(array.decodeString(pos), k);
      }
    }
    this.ids = simpleStringPool.ids;
  }
}
