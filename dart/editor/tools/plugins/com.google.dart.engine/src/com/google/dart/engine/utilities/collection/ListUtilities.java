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

import java.util.List;

/**
 * The class {@code ListUtilities} defines utility methods useful for working with {@link List
 * lists}.
 */
public final class ListUtilities {
  /**
   * Add all of the elements in the given array to the given list.
   * 
   * @param list the list to which the elements are to be added
   * @param elements the elements to be added to the list
   */
  public static <E> void addAll(List<E> list, E[] elements) {
    int count = elements.length;
    for (int i = 0; i < count; i++) {
      list.add(elements[i]);
    }
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private ListUtilities() {
    super();
  }
}
