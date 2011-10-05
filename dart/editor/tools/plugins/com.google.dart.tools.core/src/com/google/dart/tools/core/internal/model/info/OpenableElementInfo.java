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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.OpenableElement;

/**
 * Instances of the class <code>OpenableElementInfo</code> implement the information associated with
 * an {@link OpenableElement} model element.
 */
public class OpenableElementInfo extends DartElementInfo {
  /**
   * Collection of handles of immediate children of this object. This is an empty array if this
   * element has no children.
   */
  private DartElement[] children = DartElementImpl.EMPTY_ARRAY;

  /**
   * A flag indicating whether the structure of this element known.
   */
  private boolean isStructureKnown = false;

  public void addChild(DartElement child) {
    int length = children.length;
    if (length == 0) {
      children = new DartElement[] {child};
    } else {
      for (int i = 0; i < length; i++) {
        if (children[i].equals(child)) {
          return; // already included
        }
      }
      System.arraycopy(children, 0, children = new DartElement[length + 1], 0, length);
      children[length] = child;
    }
  }

  @Override
  public DartElement[] getChildren() {
    return children;
  }

  /**
   * Return <code>true</code> if the structure of this element is known.
   * 
   * @return <code>true</code> if the structure of this element is known
   */
  public boolean isStructureKnown() {
    return isStructureKnown;
  }

  public void removeChild(DartElement child) {
    for (int i = 0, length = children.length; i < length; i++) {
      DartElement element = children[i];
      if (element.equals(child)) {
        if (length == 1) {
          children = DartElementImpl.EMPTY_ARRAY;
        } else {
          DartElement[] newChildren = new DartElement[length - 1];
          System.arraycopy(children, 0, newChildren, 0, i);
          if (i < length - 1) {
            System.arraycopy(children, i + 1, newChildren, i, length - 1 - i);
          }
          children = newChildren;
        }
        break;
      }
    }
  }

  public void setChildren(DartElement[] newChildren) {
    children = newChildren;
  }

  /**
   * Sets whether the structure of this element known.
   * 
   * @param newIsStructureKnown <code>true</code> if the structure of this element is known
   */
  public void setIsStructureKnown(boolean newIsStructureKnown) {
    isStructureKnown = newIsStructureKnown;
  }
}
