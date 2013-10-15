/*
 * Copyright 2013 Dart project authors.
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

package com.google.dart.tools.debug.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A history list implementation. Elements can be added to the list, and you can navigate forwards
 * and backwards from the current location. This class can be used to implement browser style
 * navigation controls.
 * 
 * @param <T>
 */
public class HistoryList<T> {
  private List<T> list = new ArrayList<T>();
  private int current = -1;

  private List<HistoryListListener<T>> listeners = new ArrayList<HistoryListListener<T>>();

  public HistoryList() {

  }

  public void add(T t) {
    if (t == null) {
      throw new IllegalArgumentException("null not allowed");
    }

    fireAboutToChange(getCurrent());

    // trim off any items past the current pointer
    while (hasNext()) {
      list.remove(list.size() - 1);
    }

    list.add(t);
    current = list.size() - 1;

    fireChanged(getCurrent());
  }

  public void addListener(HistoryListListener<T> listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  public void clear() {
    current = -1;
    list.clear();

    fireChanged(getCurrent());
  }

  public T getCurrent() {
    if (current >= 0 && current < list.size()) {
      return list.get(current);
    } else {
      return null;
    }
  }

  public boolean hasNext() {
    return (current != -1) && (current + 1 < list.size());
  }

  public boolean hasPrevious() {
    return current > 0;
  }

  public void navigateNext() {
    if (hasNext()) {
      fireAboutToChange(getCurrent());

      current++;

      fireChanged(getCurrent());
    }
  }

  public void navigatePrevious() {
    if (hasPrevious()) {
      fireAboutToChange(getCurrent());

      current--;

      fireChanged(getCurrent());
    }
  }

  public void removeListener(HistoryListListener<T> listener) {
    listeners.remove(listener);
  }

  public void removeMatching(HistoryListMatcher<T> matcher) {
    boolean removedItem = false;

    T oldCurrent = getCurrent();

    for (int i = list.size() - 1; i >= 0; i--) {
      T t = list.get(i);

      if (matcher.matches(t)) {
        list.remove(i);
        removedItem = true;

        if (current > i) {
          // move the current selection left if we deleted to the left of it
          current--;
        } else if (current >= list.size()) {
          // ensure that we don't point past the end of the list
          current = list.size() - 1;
        }
      }
    }

    if (removedItem) {
      fireAboutToChange(oldCurrent);
      fireChanged(getCurrent());
    }
  }

  public int size() {
    return list.size();
  }

  private void fireAboutToChange(T current) {
    if (current != null && !listeners.isEmpty()) {
      for (HistoryListListener<T> l : listeners) {
        l.historyAboutToChange(current);
      }
    }
  }

  private void fireChanged(T current) {
    if (!listeners.isEmpty()) {
      for (HistoryListListener<T> l : listeners) {
        l.historyChanged(current);
      }
    }
  }

}
