/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.element.ExecutableElement;

/**
 * This class is used to replace uses of {@code HashMap<String, ExecutableElement>} which are not as
 * performant as this class.
 */
public class MemberMap {

  /**
   * The current size of this map.
   */
  private int size = 0;

  /**
   * The array of keys.
   */
  private String[] keys;

  /**
   * The array of ExecutableElement values.
   */
  private ExecutableElement[] values;

  /**
   * Default constructor.
   */
  public MemberMap() {
    this(10);
  }

  /**
   * This constructor takes an initial capacity of the map.
   * 
   * @param initialCapacity the initial capacity
   */
  public MemberMap(int initialCapacity) {
    initArrays(initialCapacity);
  }

  /**
   * Copy constructor.
   */
  public MemberMap(MemberMap memberMap) {
    initArrays(memberMap.size + 5);
    for (int i = 0; i < memberMap.size; i++) {
      keys[i] = memberMap.keys[i];
      values[i] = memberMap.values[i];
    }
    size = memberMap.size;
  }

  /**
   * Given some key, return the ExecutableElement value from the map, if the key does not exist in
   * the map, {@code null} is returned.
   * 
   * @param key some key to look up in the map
   * @return the associated ExecutableElement value from the map, if the key does not exist in the
   *         map, {@code null} is returned
   */
  public ExecutableElement get(String key) {
    for (int i = 0; i < size; i++) {
      if (keys[i] != null && keys[i].equals(key)) {
        return values[i];
      }
    }
    return null;
  }

  /**
   * Get and return the key at the specified location. If the key/value pair has been removed from
   * the set, then {@code null} is returned.
   * 
   * @param i some non-zero value less than size
   * @return the key at the passed index
   * @throw ArrayIndexOutOfBoundsException this exception is thrown if the passed index is less than
   *        zero or greater than or equal to the capacity of the arrays
   */
  public String getKey(int i) {
    return keys[i];
  }

  /**
   * The size of the map.
   * 
   * @return the size of the map.
   */
  public int getSize() {
    return size;
  }

  /**
   * Get and return the ExecutableElement at the specified location. If the key/value pair has been
   * removed from the set, then then {@code null} is returned.
   * 
   * @param i some non-zero value less than size
   * @return the key at the passed index
   * @throw ArrayIndexOutOfBoundsException this exception is thrown if the passed index is less than
   *        zero or greater than or equal to the capacity of the arrays
   */
  public ExecutableElement getValue(int i) {
    return values[i];
  }

  /**
   * Given some key/value pair, store the pair in the map. If the key exists already, then the new
   * value overrides the old value.
   * 
   * @param key the key to store in the map
   * @param value the ExecutableElement value to store in the map
   */
  public void put(String key, ExecutableElement value) {
    // If we already have a value with this key, override the value
    for (int i = 0; i < size; i++) {
      if (keys[i] != null && keys[i].equals(key)) {
        values[i] = value;
        return;
      }
    }

    // If needed, double the size of our arrays and copy values over in both arrays
    if (size == keys.length) {
      int newArrayLength = size * 2;
      String[] keys_new_array = new String[newArrayLength];
      ExecutableElement[] values_new_array = new ExecutableElement[newArrayLength];
      for (int i = 0; i < size; i++) {
        keys_new_array[i] = keys[i];
      }
      for (int i = 0; i < size; i++) {
        values_new_array[i] = values[i];
      }
      keys = keys_new_array;
      values = values_new_array;
    }

    // Put new value at end of array
    keys[size] = key;
    values[size] = value;
    size++;
  }

  /**
   * Given some {@link String} key, this method replaces the associated key and value pair with
   * {@code null}. The size is not decremented with this call, instead it is expected that the users
   * check for {@code null}.
   * 
   * @param key the key of the key/value pair to remove from the map
   */
  public void remove(String key) {
    for (int i = 0; i < size; i++) {
      if (keys[i].equals(key)) {
        keys[i] = null;
        values[i] = null;
        return;
      }
    }
  }

  /**
   * Sets the ExecutableElement at the specified location.
   * 
   * @param i some non-zero value less than size
   * @param value the ExecutableElement value to store in the map
   */
  public void setValue(int i, ExecutableElement value) {
    values[i] = value;
  }

  /**
   * Initializes {@link #keys} and {@link #values}.
   */
  private void initArrays(int initialCapacity) {
    keys = new String[initialCapacity];
    values = new ExecutableElement[initialCapacity];
  }

}
