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
package com.google.dart.engine.utilities.general;

/**
 * The class {@code ArrayUtilities} defines utility methods used to operate on arrays.
 */
public final class ArrayUtilities {
  /**
   * Return {@code true} if the given array contains the given target.
   * 
   * @param array the array being searched
   * @param target the target being searched for
   * @return {@code true} if the given target is in the array
   */
  public static <E> boolean contains(E[] array, E target) {
    for (E element : array) {
      if (element.equals(target)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return {@code true} if the given array contains any of the given targets.
   * 
   * @param array the array being searched
   * @param targets the targets being searched for
   * @return {@code true} if any of the given targets are in the array
   */
  public static <E> boolean containsAny(E[] array, E[] targets) {
    for (E target : targets) {
      if (contains(array, target)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private ArrayUtilities() {
    super();
  }
}
