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
   * Return an array containing the children outline. The array will be empty if the outline has no
   * children.
   * 
   * @return an array containing the children of the element
   */
  public Outline[] getChildren();

  /**
   * Return the information about the element.
   * 
   * @return the information about the element
   */
  public Element getElement();

  /**
   * Return the outline that either physically or logically encloses this outline. This will be
   * {@code null} if this outline is a unit outline.
   * 
   * @return the outline that encloses this outline
   */
  public Outline getParent();

  /**
   * Return the source range associated with this outline.
   * 
   * @return the source range associated with this outline
   */
  public SourceRegion getSourceRegion();
}
