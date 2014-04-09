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

import java.util.NoSuchElementException;

/**
 * The interface {@code MapIterator} defines the behavior of objects that iterate over the entries
 * in a map.
 * <p>
 * This interface defines the concept of a current entry and provides methods to access the key and
 * value in the current entry. When an iterator is first created it will be positioned before the
 * first entry and there is no current entry until {@link #moveNext()} is invoked. When all of the
 * entries have been accessed there will also be no current entry.
 * <p>
 * There is no guarantee made about the order in which the entries are accessible.
 */
public interface MapIterator<K, V> {
  /**
   * Return the key associated with the current element.
   * 
   * @return the key associated with the current element
   * @throws NoSuchElementException if there is no current element
   */
  public K getKey();

  /**
   * Return the value associated with the current element.
   * 
   * @return the value associated with the current element
   * @throws NoSuchElementException if there is no current element
   */
  public V getValue();

  /**
   * Advance to the next entry in the map. Return {@code true} if there is a current element that
   * can be accessed after this method returns. It is safe to invoke this method even if the
   * previous invocation returned {@code false}.
   * 
   * @return {@code true} if there is a current element that can be accessed
   */
  public boolean moveNext();

  /**
   * Set the value associated with the current element to the given value.
   * 
   * @param newValue the new value to be associated with the current element
   * @throws NoSuchElementException if there is no current element
   */
  public void setValue(V newValue);
}
