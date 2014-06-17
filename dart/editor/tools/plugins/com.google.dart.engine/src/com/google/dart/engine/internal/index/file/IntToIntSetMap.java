package com.google.dart.engine.internal.index.file;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

/**
 * A table mapping {@code int} keys to sets of {@code int}s.
 * 
 * @coverage dart.engine.index
 */
public class IntToIntSetMap {
  private static class Entry {
    private final int key;
    private int[] value;
    Entry next;

    public Entry(int key, int[] value, Entry next) {
      this.key = key;
      this.value = value;
      this.next = next;
    }
  }

  private final float loadFactor;
  private int capacity;
  private int threshold;
  private int size;
  private int[] keys;
  private int[][] values;
  private Entry[] entries;

  public IntToIntSetMap(int initialCapacity, float loadFactor) {
    this.loadFactor = loadFactor;
    capacity = initialCapacity;
    threshold = (int) (capacity * loadFactor);
    size = 0;
    keys = new int[capacity];
    values = new int[capacity][];
    entries = new Entry[capacity];
    Arrays.fill(keys, -1);
  }

  /**
   * Adds the given value into the set associated with the given key in this map.
   */
  public void add(int key, int value) {
    if (key < 0) {
      throw new IllegalArgumentException("Key must be a positive integer or null, but " + key
          + " is given.");
    }
    // rehash
    if (size >= threshold) {
      rehash();
    }
    // prepare hash
    int hash = hash(key);
    int index = hash % capacity;
    // try "intKeys"
    Entry entry = entries[index];
    if (entry == null) {
      int intKey = keys[index];
      if (intKey == -1) {
        keys[index] = key;
        values[index] = addValue(values[index], value);
        size++;
        return;
      }
      if (intKey == key) {
        values[index] = addValue(values[index], value);
        return;
      }
      // collision, create new Entry
      Entry existingEntry = new Entry(intKey, values[index], null);
      entries[index] = new Entry(key, addValue(null, value), existingEntry);
      keys[index] = -1;
      values[index] = null;
      size++;
      return;
    }
    // check existing entries
    while (entry != null) {
      if (entry.key == key) {
        entry.value = addValue(entry.value, value);
        return;
      }
      entry = entry.next;
    }
    // add new Entry
    entries[index] = new Entry(key, addValue(null, value), entries[index]);
    size++;
  }

  /**
   * Removes all of the mappings from this map.
   */
  public void clear() {
    size = 0;
    Arrays.fill(keys, -1);
    Arrays.fill(values, null);
    Arrays.fill(entries, null);
  }

  /**
   * Returns the values to which the specified key is mapped, or an empty {@code int[]} array if
   * this map contains no mapping for the key.
   * 
   * @param key the key whose associated value is to be returned
   * @return the values associated with {@code key}, or an empty {@code int[]} array if there is no
   *         mapping for {@code key}
   */
  public int[] get(int key) {
    int hash = hash(key);
    int index = hash % capacity;
    // try "keys"
    {
      int intKey = keys[index];
      if (intKey == key) {
        return values[index];
      }
    }
    // try "entries"
    {
      Entry entry = entries[index];
      while (entry != null) {
        if (entry.key == key) {
          return entry.value;
        }
        entry = entry.next;
      }
    }
    return ArrayUtils.EMPTY_INT_ARRAY;
  }

  /**
   * Returns the number of key-value mappings in this map.
   */
  public int size() {
    return size;
  }

  private int[] addValue(int[] set, int value) {
    if (set == null) {
      return new int[] {value};
    }
    if (ArrayUtils.indexOf(set, value) != -1) {
      return set;
    }
    return ArrayUtils.add(set, value);
  }

  private void addValues(int key, int[] values) {
    for (int value : values) {
      add(key, value);
    }
  }

  private int hash(int h) {
    h ^= (h >>> 20) ^ (h >>> 12);
    h = h ^ (h >>> 7) ^ (h >>> 4);
    return h & 0x7FFFFFFF;
  }

  private void rehash() {
    IntToIntSetMap newMap = new IntToIntSetMap(capacity * 2 + 1, loadFactor);
    // put values
    for (int i = 0; i < keys.length; i++) {
      int key = keys[i];
      if (key != -1) {
        newMap.addValues(key, values[i]);
      }
    }
    for (int i = 0; i < entries.length; i++) {
      Entry entry = entries[i];
      while (entry != null) {
        newMap.addValues(entry.key, entry.value);
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
