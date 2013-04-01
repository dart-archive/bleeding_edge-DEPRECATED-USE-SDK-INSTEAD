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

import java.util.Arrays;
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
   * {@link Iterator} for {@link FastRemoveList}.
   */
  private class ListIterator implements Iterator<E> {
    private final int lastElementIndex;
    private int index = -1;

    public ListIterator(int lastElementIndex) {
      this.lastElementIndex = lastElementIndex;
    }

    @Override
    public boolean hasNext() {
      return index < lastElementIndex;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E next() {
      while (true) {
        E e = (E) elements[++index];
        if (e != null) {
          return e;
        }
      }
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

  private Object[] elements = new Object[1];
  private int firstPossibleEmpty = 0;

  /**
   * Adds given element. It is not specified where element will be added - to the end, to the
   * beginning or in the middle. Only guarantee is that it can be accessed later using
   * {@link #iterator()}.
   * 
   * @return an integer handle which can be used to remove element.
   */
  public int add(E value) {
    if (value == null) {
      throw new NullPointerException("Null element cannot be added.");
    }
    int len = elements.length;
    // try to find empty position
    for (int i = firstPossibleEmpty; i < len; i++) {
      if (elements[i] == null) {
        elements[i] = value;
        firstPossibleEmpty = i + 1;
        return i;
      }
    }
    // no empty position, expand array
    int newLen;
    if (len < 16) {
      newLen = len + 2;
    } else {
      newLen = (len * 5) / 4;
    }
    elements = Arrays.copyOf(elements, newLen);
    // set element
    elements[len] = value;
    // we sure about next empty position
    firstPossibleEmpty = len + 1;
    // done
    return len;
  }

  /**
   * Returns an {@link Iterator} over elements. Returned {@link Iterator} is <strong>NOT</strong>
   * <i>fail-fast</i>. Also <code>remove()</code> method is not implemented.
   * 
   * @return an {@link Iterator}.
   */
  @Override
  public Iterator<E> iterator() {
    int lastElementIndex = -1;
    for (int i = elements.length - 1; i >= 0; i--) {
      if (elements[i] != null) {
        lastElementIndex = i;
        break;
      }
    }
    return new ListIterator(lastElementIndex);
  }

  /**
   * Removes element with given handle.
   */
  public void remove(int handle) {
    elements[handle] = null;
    firstPossibleEmpty = Math.min(firstPossibleEmpty, handle);
  }

  /**
   * Returns the number of elements in this {@link FastRemoveList}. This operation is
   * <strong>expensive</strong> and should not be used for anything except debugging.
   * 
   * @return the number of elements.
   */
  public int size() {
    int size = 0;
    for (int i = elements.length - 1; i >= 0; i--) {
      if (elements[i] != null) {
        size++;
      }
    }
    return size;
  }
}
