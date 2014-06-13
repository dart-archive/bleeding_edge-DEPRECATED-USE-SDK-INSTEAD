/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server;

/**
 * The interface {@code Outline} defines the behavior of objects that represent an outline for an
 * element.
 * 
 * @coverage dart.server
 */
public interface Outline {
  /**
   * An empty array of outlines.
   */
  Outline[] EMPTY_ARRAY = new Outline[0];

  /**
   * Check if <code>offset</code> is in [elementOffset, elementOffset + elementLength] interval.
   */
  public boolean containsInclusive(int offset);

  /**
   * Return an array containing the children outline. The array will be empty if the outline has no
   * children.
   * 
   * @return an array containing the children of the element
   */
  public Outline[] getChildren();

  /**
   * A description of the element represented by this node.
   * 
   * @return the {@link Element}
   */
  public Element getElement();

  /**
   * Return the length of the element.
   * 
   * @return the length of the element
   */
  public int getLength();

  /**
   * Return the offset of the first character of the element.
   * 
   * @return the offset of the first character of the element
   */
  public int getOffset();

  /**
   * Return the outline that either physically or logically encloses this outline. This will be
   * {@code null} if this outline is a unit outline.
   * 
   * @return the outline that encloses this outline
   */
  public Outline getParent();

}
