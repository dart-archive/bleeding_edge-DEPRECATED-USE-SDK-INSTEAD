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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Debugging {
  public final static String INDENT = " ";

  public static <E> List<E> sortByStrings(Collection<E> items) {
    ArrayList<E> locations = new ArrayList<E>(items);
    Collections.sort(locations, ToStringComparator/* Compatibility */.getInstance());
    return locations;
  }

  public static <E> Collection<E> sortByStrings1(Collection<E> sourceLocations) {
    ArrayList<E> locations = new ArrayList<E>(sourceLocations);
    Collections.sort(locations, ToStringComparator.getInstance());
    return locations;
  }
}
