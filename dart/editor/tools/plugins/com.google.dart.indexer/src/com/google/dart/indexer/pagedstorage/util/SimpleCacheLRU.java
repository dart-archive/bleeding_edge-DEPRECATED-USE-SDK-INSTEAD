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
package com.google.dart.indexer.pagedstorage.util;

public class SimpleCacheLRU {
  static class CacheHead extends SimpleCacheObject {
    public CacheHead() {
      super(null);
    }
  }

  private static void checkPowerOf2(int len) {
    if ((len & (len - 1)) != 0 && len > 0) {
      throw new AssertionError("not a power of 2: " + len);
    }
  }

  private static int nextPowerOf2(int x) {
    long i = 1;
    while (i < x && i < (Integer.MAX_VALUE / 2)) {
      i += i;
    }
    return (int) i;
  }

  private final SimpleCacheObject head = new CacheHead();

  private final int hashTableSize;
  private final int mask;
  private SimpleCacheObject[] values;
  private int maxSize;
  private int itemCount;

  private int usedSize;

  private int minItems;

  private int hits = 0, misses = 0;

  public SimpleCacheLRU(int maxSize, int averageItemSize, int minItems) {
    if (minItems < 1) {
      throw new IllegalArgumentException("minItems must be >= 1, got " + minItems);
    }
    this.minItems = minItems;
    this.maxSize = maxSize;
    this.hashTableSize = nextPowerOf2(maxSize * 4 / averageItemSize / 3); // just
                                                                          // a
                                                                          // wild
                                                                          // guess
                                                                          // on
                                                                          // hash
                                                                          // table
                                                                          // size
    this.mask = hashTableSize - 1;
    checkPowerOf2(hashTableSize);
    clear();
  }

  public void clear() {
    head.next = head.previous = head;
    // first set to null - avoiding out of memory
    values = null;
    values = new SimpleCacheObject[hashTableSize];
    itemCount = 0;
    usedSize = 0;
  }

  public SimpleCacheObject get(Object data) {
    SimpleCacheObject rec = find(data);
    if (rec != null) {
      removeFromLinkedList(rec);
      addToFront(rec);
      ++hits;
    } else {
      ++misses;
    }
    return rec;
  }

  public int getHits() {
    return hits;
  }

  public int getMisses() {
    return misses;
  }

  public SimpleCacheObject put(SimpleCacheObject rec) {
    SimpleCacheObject old = find(rec.data);
    if (old == null) {
      putNew(rec);
    } else {
      if (old != rec) {
        throw new IllegalArgumentException("old != rec: " + old.toString() + " != "
            + rec.toString());
      }
      removeFromLinkedList(rec);
      addToFront(rec);
    }
    return old;
  }

  public void putNew(SimpleCacheObject rec) {
    int index = rec.data.hashCode() & mask;
    rec.chained = values[index];
    values[index] = rec;
    itemCount++;
    usedSize += rec.getMemorySize();
    addToFront(rec);
    removeOldIfRequired();
  }

  public void remove(Object data) {
    int hash = data.hashCode();
    int index = hash & mask;
    SimpleCacheObject rec = values[index];
    if (rec == null) {
      return;
    }
    if (rec.data.hashCode() == hash && data.equals(rec.data)) {
      values[index] = rec.chained;
    } else {
      SimpleCacheObject last;
      do {
        last = rec;
        rec = rec.chained;
        if (rec == null) {
          return;
        }
      } while (!data.equals(rec.data));
      last.chained = rec.chained;
    }
    itemCount--;
    usedSize -= rec.getMemorySize();
    removeFromLinkedList(rec);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("LRU count=" + itemCount + " used=" + usedSize + " max=" + maxSize + "\n");
    int max = Math.max(itemCount, 100);
    int count = 0;
    for (SimpleCacheObject current = head.previous; current != head && count < max; current = current.previous, ++count) {
      result.append(current + "\n");
    }
    return result.toString();
  }

  private void addToFront(SimpleCacheObject rec) {
    rec.next = head;
    rec.previous = head.previous;
    rec.previous.next = rec;
    head.previous = rec;
  }

  private SimpleCacheObject find(Object data) {
    int hash = data.hashCode();
    SimpleCacheObject rec = values[hash & mask];
    while (rec != null && !data.equals(rec.data)) {
      rec = rec.chained;
    }
    return rec;
  }

  private void removeFromLinkedList(SimpleCacheObject rec) {
    rec.previous.next = rec.next;
    rec.next.previous = rec.previous;
    // help garbage collection (optional)
    rec.next = null;
    rec.previous = null;
  }

  private void removeOld() {
    while (usedSize * 4 > maxSize * 3 && itemCount > minItems) {
      SimpleCacheObject last = head.next;
      remove(last.data);
    }
  }

  private void removeOldIfRequired() {
    // a small method, to allow inlining
    if (usedSize > maxSize) {
      removeOld();
    }
  }
}
