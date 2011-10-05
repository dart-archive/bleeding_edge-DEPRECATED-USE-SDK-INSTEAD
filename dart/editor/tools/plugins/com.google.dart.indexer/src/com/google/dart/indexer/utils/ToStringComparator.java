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
package com.google.dart.indexer.utils;

import java.util.Comparator;

/**
 * A comparator based on lexicographical ordering of toString().
 */
public class ToStringComparator<E> implements Comparator<E> {
  private static final ToStringComparator<Object> INSTANCE = new ToStringComparator<Object>();

  public static ToStringComparator<Object> getInstance() {
    return INSTANCE;
  }

  private ToStringComparator() {
  }

  @Override
  public int compare(E o1, E o2) throws NullPointerException {
    return o1.toString().compareTo(o2.toString());
  }
}
