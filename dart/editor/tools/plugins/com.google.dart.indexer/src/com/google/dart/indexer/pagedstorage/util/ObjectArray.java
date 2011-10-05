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

import com.google.dart.indexer.pagedstorage.DebugConstants;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * The object array is basically the same as ArrayList. It is a bit faster than ArrayList in some
 * versions of Java.
 */
public class ObjectArray<E> {
  /**
   * The iterator for this list.
   */
  class ObjectArrayIterator implements Iterator<E> {
    private int index;

    @Override
    public boolean hasNext() {
      return index < size;
    }

    @Override
    public E next() {
      return get(index++);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private static final int CAPACITY_INIT = 4, CAPACITY_SHRINK = 256;

  /**
   * Create a new object with the default initial capacity.
   * 
   * @return the object
   */
  public static <E> ObjectArray<E> newInstance() {
    return new ObjectArray<E>(CAPACITY_INIT);
  }

  /**
   * Create a new object with all elements of the given collection.
   * 
   * @param collection the collection with all elements
   * @return the object
   */
  public static <E> ObjectArray<E> newInstance(Collection<E> collection) {
    return new ObjectArray<E>(collection);
  }

  /**
   * Create a new object with the given initial capacity.
   * 
   * @param capacity the initial capacity
   * @return the object
   */
  public static <E> ObjectArray<E> newInstance(int capacity) {
    return new ObjectArray<E>(CAPACITY_INIT);
  }

  int size;

  private E[] data;

  private ObjectArray(Collection<E> collection) {
    size = collection.size();
    data = createArray(size);
    Iterator<E> it = collection.iterator();
    for (int i = 0; i < size; i++) {
      data[i] = it.next();
    }
  }

  private ObjectArray(int capacity) {
    data = createArray(capacity);
  }

  /**
   * Append an object at the end of the list.
   * 
   * @param value the value
   */
  public void add(E value) {
    if (size >= data.length) {
      ensureCapacity(size);
    }
    data[size++] = value;
  }

  /**
   * Insert an element at the given position. The element at this position and all elements with a
   * higher index move one element.
   * 
   * @param index the index where to insert the object
   * @param value the object to insert
   */
  @SuppressWarnings("unused")
  public void add(int index, E value) {
    if (DebugConstants.CHECK2 && index > size) {
      throwException(index);
    }
    ensureCapacity(size);
    if (index == size) {
      add(value);
    } else {
      System.arraycopy(data, index, data, index + 1, size - index);
      data[index] = value;
      size++;
    }
  }

  /**
   * Add all objects from the given list.
   * 
   * @param list the list
   */
  public void addAll(ObjectArray<E> list) {
    for (int i = 0; i < list.size; i++) {
      add(list.data[i]);
    }
  }

  /**
   * Remove all elements from the list.
   */
  public void clear() {
    if (data.length > CAPACITY_SHRINK) {
      data = createArray(CAPACITY_INIT);
    } else {
      for (int i = 0; i < size; i++) {
        data[i] = null;
      }
    }
    size = 0;
  }

  /**
   * Get the object at the given index.
   * 
   * @param index the index
   * @return the value
   */
  @SuppressWarnings("unused")
  public E get(int index) {
    if (DebugConstants.CHECK2 && index >= size) {
      throwException(index);
    }
    return data[index];
  }

  /**
   * Get the index of the given object, or -1 if not found.
   * 
   * @param o the object to search
   * @return the index
   */
  public int indexOf(E o) {
    for (int i = 0; i < size; i++) {
      if (data[i] == o) {
        return i;
      }
    }
    return -1;
  }

  public Iterator<E> iterator() {
    return new ObjectArrayIterator();
  }

  /**
   * Remove the object at the given index.
   * 
   * @param index the index
   * @return the removed object
   */
  @SuppressWarnings("unused")
  public E remove(int index) {
    // TODO performance: the app should (where possible)
    // remove from end to start, to avoid O(n^2)
    if (DebugConstants.CHECK2 && index >= size) {
      throwException(index);
    }
    E value = data[index];
    System.arraycopy(data, index + 1, data, index, size - index - 1);
    size--;
    data[size] = null;
    // TODO optimization / lib: could shrink ObjectArray on element remove
    return value;
  }

  /**
   * Remove a number of elements from the given start and end index.
   * 
   * @param from the start index
   * @param to the end index
   */
  @SuppressWarnings("unused")
  public void removeRange(int from, int to) {
    if (DebugConstants.CHECK2 && (to > size || from > to)) {
      throw new ArrayIndexOutOfBoundsException("to=" + to + " from=" + from + " size=" + size);
    }
    System.arraycopy(data, to, data, from, size - to);
    size -= to - from;
    for (int i = size + (to - from) - 1; i >= size; i--) {
      data[i] = null;
    }
  }

  /**
   * Update the object at the given index.
   * 
   * @param index the index
   * @param value the new value
   */
  @SuppressWarnings("unused")
  public void set(int index, E value) {
    if (DebugConstants.CHECK2 && index >= size) {
      throwException(index);
    }
    data[index] = value;
  }

  /**
   * Fill the list with empty elements until it reaches the given size.
   * 
   * @param size the new size
   */
  public void setSize(int size) {
    ensureCapacity(size);
    this.size = size;
  }

  /**
   * Get the size of the list.
   * 
   * @return the size
   */
  public int size() {
    return size;
  }

  /**
   * Sort the elements using the given comparator.
   * 
   * @param comp the comparator
   */
  public void sort(Comparator<E> comp) {
    sort(comp, 0, size - 1);
  }

  /**
   * Convert this list to an array. The target array must be big enough.
   * 
   * @param array the target array
   */
  public void toArray(E[] array) {
    ObjectUtils.arrayCopy(data, array, size);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < size; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      Object object = get(i);
      builder.append(object == null ? "" : object.toString());
    }
    return builder.append('}').toString();
  }

  /**
   * Shrink the array to the required size.
   */
  public void trimToSize() {
    E[] d = createArray(size);
    System.arraycopy(data, 0, d, 0, size);
    data = d;
  }

  @SuppressWarnings("unchecked")
  private E[] createArray(int capacity) {
    return (E[]) new Object[capacity > 1 ? capacity : 1];
  }

  private void ensureCapacity(int i) {
    while (i >= data.length) {
      E[] d = createArray(Math.max(CAPACITY_INIT, data.length * 2));
      System.arraycopy(data, 0, d, 0, size);
      data = d;
    }
  }

  /**
   * Sort using the quicksort algorithm.
   * 
   * @param comp the comparator
   * @param l the first element (left)
   * @param r the last element (right)
   */
  private void sort(Comparator<E> comp, int l, int r) {
    int i, j;
    while (r - l > 10) {
      // randomized pivot to avoid worst case
      i = RandomUtils.nextInt(r - l - 4) + l + 2;
      if (comp.compare(get(l), get(r)) > 0) {
        swap(l, r);
      }
      if (comp.compare(get(i), get(l)) < 0) {
        swap(l, i);
      } else if (comp.compare(get(i), get(r)) > 0) {
        swap(i, r);
      }
      j = r - 1;
      swap(i, j);
      E p = get(j);
      i = l;
      while (true) {
        do {
          ++i;
        } while (comp.compare(get(i), p) < 0);
        do {
          --j;
        } while (comp.compare(get(j), p) > 0);
        if (i >= j) {
          break;
        }
        swap(i, j);
      }
      swap(i, r - 1);
      sort(comp, l, i - 1);
      l = i + 1;
    }
    for (i = l + 1; i <= r; i++) {
      E Object = get(i);
      for (j = i - 1; j >= l && (comp.compare(get(j), Object) > 0); j--) {
        set(j + 1, get(j));
      }
      set(j + 1, Object);
    }
  }

  private void swap(int l, int r) {
    E Object = data[r];
    data[r] = data[l];
    data[l] = Object;
  }

  // public void sortInsertion(Comparator comp) {
  // for (int i = 1, j; i < size(); i++) {
  // Object Object = get(i);
  // for (j = i - 1; j >= 0 && (comp.compare(get(j), Object) < 0); j--) {
  // set(j + 1, get(j));
  // }
  // set(j + 1, Object);
  // }
  // }

  private void throwException(int index) {
    throw new ArrayIndexOutOfBoundsException("i=" + index + " size=" + size);
  }
}
