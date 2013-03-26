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

package com.google.dart.engine.services.util;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.type.InterfaceType;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Helper for {@link ClassElement} hierarchy.
 */
public class HierarchyUtils {
  /**
   * @return the {@link List} with direct sub {@link ClassElement}s of the given.
   */
  public static List<ClassElement> getDirectSubClasses(SearchEngine searchEngine, ClassElement seed) {
    List<ClassElement> subClasses = Lists.newArrayList();
    // ask SearchEngine
    List<SearchMatch> subMatches = searchEngine.searchSubtypes(seed, null, null);
    for (SearchMatch subMatch : subMatches) {
      ClassElement subClass = (ClassElement) subMatch.getElement();
      subClasses.add(subClass);
    }
    // done
    return subClasses;
  }

  /**
   * @return the {@link Set} with all direct and indirect sub {@link ClassElement}s of the given.
   */
  public static Set<ClassElement> getSubClasses(SearchEngine searchEngine, ClassElement seed) {
    Set<ClassElement> subClasses = Sets.newHashSet();
    // prepare queue
    LinkedList<ClassElement> subClassQueue = Lists.newLinkedList();
    subClassQueue.add(seed);
    // process queue
    while (!subClassQueue.isEmpty()) {
      ClassElement subClass = subClassQueue.removeFirst();
      if (subClasses.add(subClass)) {
        List<SearchMatch> subMatches = searchEngine.searchSubtypes(subClass, null, null);
        for (SearchMatch subMatch : subMatches) {
          ClassElement subClassNew = (ClassElement) subMatch.getElement();
          subClassQueue.addLast(subClassNew);
        }
      }
    }
    // we don't need "seed" itself
    subClasses.remove(seed);
    return subClasses;
  }

  /**
   * @return the {@link Set} with all direct and indirect super {@link ClassElement}s of the given.
   */
  public static List<ClassElement> getSuperClasses(ClassElement classElement) {
    LinkedList<ClassElement> classes = Lists.newLinkedList();
    while (classElement != null) {
      // prepare super InterfaceType 
      InterfaceType superType = classElement.getSupertype();
      if (superType == null) {
        break;
      }
      // check super ClassElement
      classElement = superType.getElement();
      if (classElement != null) {
        // stop at Object
        if (Objects.equal(classElement.getName(), "Object")) {
          break;
        }
        // OK, add
        classes.addFirst(classElement);
      }
    }
    return classes;
  }
}
