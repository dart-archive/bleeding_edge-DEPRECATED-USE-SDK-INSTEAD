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

package com.google.dart.engine.utilities.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * {@link FastRemoveList} is a collection (not {@link Collection} and not {@link List}) with fast
 * "add", "iterate" and "remove" operations.
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong>
 * <p>
 * The iterator returned by this class's <tt>iterator</tt> method is <strong>NOT</strong>
 * <i>fail-fast</i>. You never should modify {@link FastRemoveList} while iterating over it.
 * <p>
 * Order of iteration is not guaranteed.
 * 
 * @coverage dart.engine.utilities
 */
public class FastRemoveList<E> implements Iterable<E> {
  /**
   * Handle which can be used to remove element.
   */
  public static interface Handle {
    /**
     * Removes element corresponding this {@link Handle}.
     */
    void remove();
  }

  private static class Entry<E> implements Handle {
    private final E value;
    private Entry<E> prev;
    private Entry<E> next;

    public Entry(E value, Entry<E> next) {
      this.value = value;
      this.next = next;
    }

    @Override
    public void remove() {
      if (prev != null) {
        prev.next = next;
      }
      if (next != null) {
        next.prev = prev;
      }
      prev = null;
      next = null;
    }
  }

  /**
   * {@link Iterator} for {@link FastRemoveList}.
   */
  private static class ListIterator<E> implements Iterator<E> {
    private Entry<E> entry;
    private Entry<E> next;

    public ListIterator(Entry<E> head) {
      this.next = head;
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public E next() {
      entry = next;
      next = next.next;
      return entry.value;
    }

    @Override
    public void remove() {
      throw new AbstractMethodError();
    }
  }

  /**
   * Creates new instance of {@link FastRemoveList}. Use this method to don't write type argument.
   * 
   * @return the new instance of {@link FastRemoveList}.
   */
  public static <E> FastRemoveList<E> newInstance() {
    return new FastRemoveList<E>();
  }

  private final Entry<E> head = new Entry<E>(null, null);

  private FastRemoveList() {
  }

  /**
   * Adds given element. It is not specified where element will be added - to the end, to the
   * beginning or in the middle. Only guarantee is that it can be accessed later using
   * {@link #iterator()}.
   * 
   * @return the {@link Handle} which can be used to remove element.
   */
  public Handle add(E value) {
    Entry<E> newEntry = new Entry<E>(value, head.next);
    if (head.next != null) {
      head.next.prev = newEntry;
    }
    newEntry.prev = head;
    head.next = newEntry;
    return newEntry;
  }

  /**
   * Returns an {@link Iterator} over elements. Returned {@link Iterator} is <strong>NOT</strong>
   * <i>fail-fast</i>. Also <code>remove()</code> method is not implemented.
   * 
   * @return an {@link Iterator}.
   */
  @Override
  public Iterator<E> iterator() {
    return new ListIterator<E>(head.next);
  }

  /**
   * Returns the number of elements in this {@link FastRemoveList}. This operation is
   * <strong>expensive</strong> and should not be used for anything except debugging.
   * 
   * @return the number of elements.
   */
  public int size() {
    int size = 0;
    Entry<E> entry = head.next;
    while (entry != null) {
      size++;
      entry = entry.next;
    }
    return size;
  }
}
