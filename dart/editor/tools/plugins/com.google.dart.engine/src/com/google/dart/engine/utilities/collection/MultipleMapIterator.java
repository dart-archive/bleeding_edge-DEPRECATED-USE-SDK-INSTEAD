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

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Instances of the class {@code MultipleMapIterator} implement an iterator that can be used to
 * sequentially access the entries in multiple maps.
 */
public class MultipleMapIterator<K, V> implements MapIterator<K, V> {
  /**
   * The iterators used to access the entries.
   */
  private MapIterator<K, V>[] iterators;

  /**
   * The index of the iterator currently being used to access the entries.
   */
  private int iteratorIndex = -1;

  /**
   * The current iterator, or {@code null} if there is no current iterator.
   */
  private MapIterator<K, V> currentIterator;

  /**
   * Initialize a newly created iterator to return the entries from the given maps.
   * 
   * @param maps the maps containing the entries to be iterated
   */
  @SuppressWarnings("unchecked")
  public MultipleMapIterator(Map<K, V>[] maps) {
    int count = maps.length;
    iterators = new MapIterator[count];
    for (int i = 0; i < count; i++) {
      iterators[i] = new SingleMapIterator<K, V>(maps[i]);
    }
  }

  @Override
  public K getKey() {
    if (currentIterator == null) {
      throw new NoSuchElementException();
    }
    return currentIterator.getKey();
  }

  @Override
  public V getValue() {
    if (currentIterator == null) {
      throw new NoSuchElementException();
    }
    return currentIterator.getValue();
  }

  @Override
  public boolean moveNext() {
    if (iteratorIndex < 0) {
      if (iterators.length == 0) {
        currentIterator = null;
        return false;
      }
      if (advanceToNextIterator()) {
        return true;
      } else {
        currentIterator = null;
        return false;
      }
    }
    if (currentIterator.moveNext()) {
      return true;
    } else if (advanceToNextIterator()) {
      return true;
    } else {
      currentIterator = null;
      return false;
    }
  }

  @Override
  public void setValue(V newValue) {
    if (currentIterator == null) {
      throw new NoSuchElementException();
    }
    currentIterator.setValue(newValue);
  }

  /**
   * Under the assumption that there are no more entries that can be returned using the current
   * iterator, advance to the next iterator that has entries.
   * 
   * @return {@code true} if there is a current iterator that has entries
   */
  private boolean advanceToNextIterator() {
    iteratorIndex++;
    while (iteratorIndex < iterators.length) {
      MapIterator<K, V> iterator = iterators[iteratorIndex];
      if (iterator.moveNext()) {
        currentIterator = iterator;
        return true;
      }
      iteratorIndex++;
    }
    return false;
  }
}
