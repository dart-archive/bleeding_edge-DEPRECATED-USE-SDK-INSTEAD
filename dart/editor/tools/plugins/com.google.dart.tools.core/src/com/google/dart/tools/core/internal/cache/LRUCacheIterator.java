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
package com.google.dart.tools.core.internal.cache;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Instances of the class <code>LRUCacheIterator</code> implement an iterator over the contents of
 * an {@link LRUCache}. The iterator returns its elements in the order they are found in the
 * {@link LRUCache}, with the most recent elements first.
 * <p>
 * Once the iterator is created, elements which are later added to the cache are not returned by the
 * iterator. However, elements returned from the iterator could have been closed by the cache.
 */
public class LRUCacheIterator<V> implements Iterator<V> {
  public static class Element<V> {
    /**
     * Value returned by <code>next()</code>;
     */
    public V value;

    /**
     * Next element
     */
    public Element<V> next;

    /**
     * Constructor
     */
    public Element(V value) {
      this.value = value;
    }
  }

  /**
   * Current element;
   */
  private Element<V> elementQueue;

  /**
   * Initialize a newly created iterator on the given list of elements.
   */
  public LRUCacheIterator(Element<V> firstElement) {
    elementQueue = firstElement;
  }

  @Override
  public boolean hasNext() {
    return elementQueue != null;
  }

  @Override
  public V next() {
    if (elementQueue == null) {
      throw new NoSuchElementException();
    }
    V temp = elementQueue.value;
    elementQueue = elementQueue.next;
    return temp;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
