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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.utilities.general.ObjectUtilities;

/**
 * The class {@code ElementPair} is a pair of {@link Element}s. {@link Object#equals(Object)} and
 * {@link Object#hashCode()} so this class can be used in hashed data structures.
 */
public class ElementPair {
  /**
   * The first {@link Element}
   */
  private Element first;

  /**
   * The second {@link Element}
   */
  private Element second;

  /**
   * The sole constructor for this class, taking two {@link Element}s.
   * 
   * @param first the first element
   * @param second the second element
   */
  public ElementPair(Element first, Element second) {
    this.first = first;
    this.second = second;
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof ElementPair) {
      ElementPair elementPair = (ElementPair) object;
      return ObjectUtilities.equals(first, elementPair.first)
          && ObjectUtilities.equals(second, elementPair.second);
    }
    return false;
  }

  /**
   * Return the first element.
   * 
   * @return the first element
   */
  public Element getFirstElt() {
    return first;
  }

  /**
   * Return the second element
   * 
   * @return the second element
   */
  public Element getSecondElt() {
    return second;
  }

  @Override
  public int hashCode() {
    return ObjectUtilities.combineHashCodes(first.hashCode(), second.hashCode());
  }
}
