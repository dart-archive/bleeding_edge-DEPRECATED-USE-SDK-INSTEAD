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
package com.google.dart.engine.utilities.collection;

import com.google.dart.engine.utilities.translation.DartOmit;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * Instances of the class {@code SingleMapIterator} implement an iterator that can be used to access
 * the entries in a single map.
 */
@DartOmit
public class SingleMapIterator<K, V> implements MapIterator<K, V> {
  /**
   * Returns a new {@link SingleMapIterator} instance for the given {@link Map}.
   */
  public static <K, V> SingleMapIterator<K, V> forMap(Map<K, V> map) {
    return new SingleMapIterator<K, V>(map);
  }

  /**
   * The iterator used to access the entries.
   */
  private Iterator<Entry<K, V>> iterator;

  /**
   * The current entry, or {@code null} if there is no current entry.
   */
  private Entry<K, V> currentEntry;

  /**
   * Initialize a newly created iterator to return the entries from the given map.
   * 
   * @param map the map containing the entries to be iterated over
   */
  public SingleMapIterator(Map<K, V> map) {
    this.iterator = map.entrySet().iterator();
  }

  @Override
  public K getKey() {
    if (currentEntry == null) {
      throw new NoSuchElementException();
    }
    return currentEntry.getKey();
  }

  @Override
  public V getValue() {
    if (currentEntry == null) {
      throw new NoSuchElementException();
    }
    return currentEntry.getValue();
  }

  @Override
  public boolean moveNext() {
    if (iterator.hasNext()) {
      currentEntry = iterator.next();
      return true;
    } else {
      currentEntry = null;
      return false;
    }
  }

  @Override
  public void setValue(V newValue) {
    if (currentEntry == null) {
      throw new NoSuchElementException();
    }
    currentEntry.setValue(newValue);
  }
}
