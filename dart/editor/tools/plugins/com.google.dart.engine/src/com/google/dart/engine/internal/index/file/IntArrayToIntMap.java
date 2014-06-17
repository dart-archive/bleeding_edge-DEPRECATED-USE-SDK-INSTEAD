/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.internal.index.file;

import java.util.Arrays;

/**
 * A hash map with {@code int[]} keys and {@code int} values.
 * 
 * @coverage dart.engine.index
 */
public class IntArrayToIntMap {
  private static class Entry {
    private final int[] key;
    private int value;
    Entry next;

    public Entry(int[] key, int value, Entry next) {
      this.key = key;
      this.value = value;
      this.next = next;
    }
  }

  private final float loadFactor;
  private int capacity;
  private int threshold;
  private int size;
  private int[][] keys;
  private int[] values;
  private Entry[] entries;

  public IntArrayToIntMap(int initialCapacity, float loadFactor) {
    this.loadFactor = loadFactor;
    capacity = initialCapacity;
    threshold = (int) (capacity * loadFactor);
    size = 0;
    keys = new int[capacity][];
    values = new int[capacity];
    entries = new Entry[capacity];
  }

  /**
   * Returns the value to which the specified key is mapped, or the given default value if this map
   * contains no mapping for the key.
   * 
   * @param key the key whose associated value is to be returned
   * @param defaultValue the default value to to returned if there is no value associated with the
   *          given key
   * @return the value associated with {@code key}, or {@code defaultValue} if there is no mapping
   *         for {@code key}
   */
  public int get(int[] key, int defaultValue) {
    int hash = hash(key);
    int index = hash % capacity;
    // try "intKeys"
    {
      int[] intKey = keys[index];
      if (intKey != null && Arrays.equals(intKey, key)) {
        return values[index];
      }
    }
    // try "entries"
    {
      Entry entry = entries[index];
      while (entry != null) {
        if (Arrays.equals(entry.key, key)) {
          return entry.value;
        }
        entry = entry.next;
      }
    }
    // not found
    return defaultValue;
  }

  /**
   * Associates the specified value with the specified key in this map.
   */
  public void put(int[] key, int value) {
    if (size >= threshold) {
      rehash();
    }
    // prepare hash
    int hash = hash(key);
    int index = hash % capacity;
    // try "keys"
    Entry entry = entries[index];
    if (entry == null) {
      int[] existingKey = keys[index];
      if (existingKey == null) {
        keys[index] = key;
        values[index] = value;
        size++;
        return;
      }
      if (Arrays.equals(existingKey, key)) {
        values[index] = value;
        return;
      }
      // collision, create new Entry
      Entry existingEntry = new Entry(existingKey, values[index], null);
      keys[index] = null;
      entries[index] = new Entry(key, value, existingEntry);
      size++;
      return;
    }
    // check existing entries
    while (entry != null) {
      if (Arrays.equals(entry.key, key)) {
        entry.value = value;
        return;
      }
      entry = entry.next;
    }
    // add new Entry
    entries[index] = new Entry(key, value, entries[index]);
    size++;
  }

  /**
   * Removes the mapping for a key from this map if it is present.
   * 
   * @param key the key whose mapping is to be removed from the map
   * @param defaultValue the default value to to returned if there is no value associated with the
   *          given key
   * @return the previous value associated with {@code key}, or {@code defaultValue} if there was no
   *         mapping for {@code key}
   */
  public int remove(int[] key, int defaultValue) {
    int hash = hash(key);
    int index = hash % capacity;
    // try "keys"
    {
      int[] existingKey = keys[index];
      if (existingKey != null && Arrays.equals(existingKey, key)) {
        size--;
        keys[index] = null;
        return values[index];
      }
    }
    // try "entries"
    {
      Entry entry = entries[index];
      Entry prev = null;
      while (entry != null) {
        if (Arrays.equals(entry.key, key)) {
          size--;
          int value = entry.value;
          if (entries[index] == entry) {
            entries[index] = entry.next;
          } else {
            prev.next = entry.next;
          }
          return value;
        }
        prev = entry;
        entry = entry.next;
      }
    }
    // not found
    return defaultValue;
  }

  /**
   * Returns the number of key-value mappings in this map.
   */
  public int size() {
    return size;
  }

  private int hash(int[] key) {
    int result = 1;
    for (int element : key) {
      result = 31 * result + element;
    }
    return result & 0x7FFFFFFF;
  }

  private void rehash() {
    IntArrayToIntMap newMap = new IntArrayToIntMap(capacity * 2 + 1, loadFactor);
    // put values
    for (int i = 0; i < keys.length; i++) {
      int[] key = keys[i];
      if (key != null) {
        newMap.put(key, values[i]);
      }
    }
    for (int i = 0; i < entries.length; i++) {
      Entry entry = entries[i];
      while (entry != null) {
        newMap.put(entry.key, entry.value);
        entry = entry.next;
      }
    }
    // copy data
    capacity = newMap.capacity;
    threshold = newMap.threshold;
    keys = newMap.keys;
    values = newMap.values;
    entries = newMap.entries;
  }
}
