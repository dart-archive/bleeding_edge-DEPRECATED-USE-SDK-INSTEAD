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

final class LocationInfoManager {
  ByteArray array;
  public int[] ids;
  int size;

  IntArray positions;
  IntArray locationInfos;

  public LocationInfoManager(int i, AbstractStringEncoder optimizedStringEncoder) {
    this.ids = new int[i];
    array = new ByteArray(10000, optimizedStringEncoder);
    positions = new IntArray(4000);
    positions.add(0);
    locationInfos = new IntArray(4000);
    locationInfos.add(0);
  }

  public LocationInfoManager(int i, ByteArray array2) {
    this.ids = new int[i];
    array = array2;
  }

  public LocationInfoManager(int size2, int[] ids2, IntArray locInfos, IntArray poss,
      ByteArray array2, OptimizedStringEncoder2 optimizedStringEncoder2) {
    this.array = array2;
    this.ids = ids2;
    this.locationInfos = locInfos;
    this.positions = poss;
    this.size = size2;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    LocationInfoManager other = (LocationInfoManager) obj;
    int where = 0;
    boolean result = size == other.size && ((++where > 0) && array.equals(other.array))
        && ((++where > 0) && Arrays.equals(ids, other.ids))
        && ((++where > 0) && locationInfos.equals(other.locationInfos))
        && ((++where > 0) && positions.equals(other.positions));

    if (!result) {
      // where may be used here
    }
    return result;
  }

  public int getLocationInfo(int id) {
    return locationInfos.data[id];
  }

  public String getString(int id) {
    return array.decodeString(positions.data[id]);
  }

  public int memUsed() {
    return ids.length * 4 + positions.data.length * 4 + locationInfos.elementsCount * 4
        + array.elementsCount + 36 + array.encoder.memUsed();
  }

  int add(String s) {
    int k = k(s);
    int i = get(s);
    if (k != i) {
      i = get(s);
      throw new RuntimeException();
    }
    if (k == 0) {
      k(s);
      throw new RuntimeException();
    }
    return k;
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

  int getLocationInfo(String s) {
    int i = get(s);
    if (i == -1) {
      return -1;
    }
    return locationInfos.data[i];
  }

  void setLocationInfo(int p, int value) {
    locationInfos.data[p] = value;
  }

  void setLocationInfo(String s, int value) {
    int i = get(s);
    locationInfos.data[i] = value;
  }

  private int k(String s) {
    if (size > ((2 * ids.length) / 3)) {
      rehash();
    }
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
    size++;
    ids[pos] = positions.elementsCount;
    if (positions.elementsCount == 0) {
      throw new RuntimeException();
    }
    positions.add(array.addString(s));
    locationInfos.add(0);

    return ids[pos];
  }

  private void rehash() {
    LocationInfoManager simpleStringPool = new LocationInfoManager(this.ids.length * 2, this.array);
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
