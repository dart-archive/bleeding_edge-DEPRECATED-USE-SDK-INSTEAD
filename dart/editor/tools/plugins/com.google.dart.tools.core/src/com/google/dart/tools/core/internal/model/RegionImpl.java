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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Region;

import java.util.ArrayList;

/**
 * Instances of the class <code>RegionImpl</code> implement a {@link Region}.
 */
public class RegionImpl implements Region {
  /**
   * A collection of the top level elements that have been added to the region
   */
  private ArrayList<DartElement> rootElements;

  /**
   * Initialize a newly created region to be empty.
   */
  public RegionImpl() {
    rootElements = new ArrayList<DartElement>(1);
  }

  @Override
  public void add(DartElement element) {
    if (!contains(element)) {
      // "new" element added to region
      removeAllChildren(element);
      rootElements.add(element);
      rootElements.trimToSize();
    }
  }

  @Override
  public boolean contains(DartElement element) {
    int size = rootElements.size();
    ArrayList<DartElement> parents = getAncestors(element);
    for (int i = 0; i < size; i++) {
      DartElement aTop = rootElements.get(i);
      if (aTop.equals(element)) {
        return true;
      }
      for (int j = 0, pSize = parents.size(); j < pSize; j++) {
        if (aTop.equals(parents.get(j))) {
          // an ancestor is already included
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public DartElement[] getElements() {
    int size = rootElements.size();
    DartElement[] roots = new DartElement[size];
    for (int i = 0; i < size; i++) {
      roots[i] = rootElements.get(i);
    }
    return roots;
  }

  @Override
  public boolean remove(DartElement element) {
    removeAllChildren(element);
    return rootElements.remove(element);
  }

  /**
   * Return a textual representation of this region.
   * 
   * @return a textual representation of this region
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    DartElement[] roots = getElements();
    builder.append('[');
    for (int i = 0; i < roots.length; i++) {
      builder.append(roots[i].getElementName());
      if (i < (roots.length - 1)) {
        builder.append(", "); //$NON-NLS-1$
      }
    }
    builder.append(']');
    return builder.toString();
  }

  /**
   * Removes any children of this element that are contained within this region as this parent is
   * about to be added to the region.
   * <p>
   * Children are all children, not just direct children.
   * 
   * @param element the element whose children are to be removed
   */
  protected void removeAllChildren(DartElement element) {
    ArrayList<DartElement> newRootElements = new ArrayList<DartElement>();
    for (DartElement currentRoot : rootElements) {
      if (isDescendant(element, currentRoot)) {
        newRootElements.add(currentRoot);
      }
    }
    rootElements = newRootElements;
  }

  /**
   * Return a list containing all of the ancestors of this element with the closest ancestor (the
   * parent) being listed first and the most distant ancestor being listed last.
   * 
   * @param element the element whose ancestors are to be returned
   * @return a list containing all of the ancestors of this element
   */
  private ArrayList<DartElement> getAncestors(DartElement element) {
    ArrayList<DartElement> ancestors = new ArrayList<DartElement>();
    DartElement parent = element.getParent();
    while (parent != null) {
      ancestors.add(parent);
      parent = parent.getParent();
    }
    ancestors.trimToSize();
    return ancestors;
  }

  /**
   * Return <code>true</code> if the given descendant is a descendant of the given ancestor.
   * 
   * @param ancestor the ancestor of the possible descendant
   * @param descendant the descendant of the possible ancestor
   * @return <code>true</code> if the given descendant is a descendant of the given ancestor
   */
  private boolean isDescendant(DartElement ancestor, DartElement descendant) {
    DartElement parent = descendant.getParent();
    while (parent != null) {
      if (parent.equals(ancestor)) {
        return true;
      }
      parent = parent.getParent();
    }
    return false;
  }
}
