// Copyright (c) 2011, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility methods for operating on memory-efficient lists. All lists of size 0 or 1 are assumed to
 * be immutable. All lists of size greater than 1 are assumed to be mutable.
 */
public class Lists {
  public static <T> List<T> add(List<T> list, T toAdd) {
    switch (list.size()) {
      case 0:
        // Empty -> Singleton
        return Collections.singletonList(toAdd);
      case 1: {
        // Singleton -> ArrayList
        List<T> result = new ArrayList<T>(2);
        result.add(list.get(0));
        result.add(toAdd);
        return result;
      }
      default:
        // ArrayList
        list.add(toAdd);
        return list;
    }
  }

  public static <T> List<T> create() {
    return Collections.emptyList();
  }

  public static <T> List<T> create(Collection<T> collection) {
    switch (collection.size()) {
      case 0:
        return create();
      default:
        return new ArrayList<T>(collection);
    }
  }

  public static <T> List<T> create(T item) {
    return Collections.singletonList(item);
  }

  public static <T> List<T> create(T... items) {
    switch (items.length) {
      case 0:
        return create();
      case 1:
        return create(items[0]);
      default:
        return new ArrayList<T>(Arrays.asList(items));
    }
  }

  public static <T> List<T> remove(List<T> list, int toRemove) {
    switch (list.size()) {
      case 0:
        // Empty
        throw newIndexOutOfBounds(list, toRemove);
      case 1:
        // Singleton -> Empty
        if (toRemove == 0) {
          return Collections.emptyList();
        } else {
          throw newIndexOutOfBounds(list, toRemove);
        }
      case 2:
        // ArrayList -> Singleton
        switch (toRemove) {
          case 0:
            return Collections.singletonList(list.get(1));
          case 1:
            return Collections.singletonList(list.get(0));
          default:
            throw newIndexOutOfBounds(list, toRemove);
        }
      default:
        // ArrayList
        list.remove(toRemove);
        return list;
    }
  }

  public static <T> List<T> sort(List<T> list, Comparator<? super T> sort) {
    if (list.size() > 1) {
      Collections.sort(list, sort);
    }
    return list;
  }

  private static <T> IndexOutOfBoundsException newIndexOutOfBounds(List<T> list, int index) {
    return new IndexOutOfBoundsException("Index: " + index + ", Size: " + list.size());
  }
}
