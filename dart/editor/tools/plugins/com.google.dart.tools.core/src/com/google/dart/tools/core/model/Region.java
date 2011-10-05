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
package com.google.dart.tools.core.model;

import com.google.dart.tools.core.DartCore;

/**
 * The interface <code>Region</code> defines the behavior common to objects that describe a
 * hierarchical set of elements. Regions are often used to describe a set of elements to be
 * considered when performing operations; for example, the set of elements to be considered during a
 * search. A region may include elements from different projects.
 * <p>
 * When an element is included in a region, all of its children are considered to be included.
 * Children of an included element <b>cannot</b> be selectively excluded.
 * <p>
 * Instances can be created via the {@link DartCore#newRegion()} method.
 */
public interface Region {
  /**
   * Add the given element and all of its descendants to this region. If the specified element is
   * already included, or one of its ancestors is already included, this has no effect. If the
   * element being added is an ancestor of an element already contained in this region, the ancestor
   * subsumes the descendant.
   * 
   * @param element the given element
   */
  public void add(DartElement element);

  /**
   * Return <code>true</code> if the given element is contained in this region.
   * 
   * @param element the element being tested for containment
   * @return <code>true</code> if the given element is contained in this region
   */
  public boolean contains(DartElement element);

  /**
   * Return the top level elements in this region. All descendants of these elements are also
   * included in this region.
   * 
   * @return the top level elements in this region
   */
  public DartElement[] getElements();

  /**
   * Remove the specified element from the region and return <code>true</code> if successful or
   * <code>false</code> if the remove fails. If an ancestor of the given element is included, the
   * remove fails (in other words, it is not possible to selectively exclude descendants of included
   * ancestors).
   * 
   * @param element the element to be removed
   * @return <code>true</code> if successful
   */
  boolean remove(DartElement element);
}
