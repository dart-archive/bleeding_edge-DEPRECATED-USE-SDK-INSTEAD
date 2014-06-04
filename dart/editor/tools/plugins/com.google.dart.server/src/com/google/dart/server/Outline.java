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
public interface Outline extends SourceRegion {
  /**
   * An empty array of outlines.
   */
  Outline[] EMPTY_ARRAY = new Outline[0];

  /**
   * Return the argument list for the element. If the element is not a method or function this field
   * will not be defined. If the element has zero arguments, this field will have a value of "()".
   * 
   * @return the argument list for the element
   */
  public String getArguments();

  /**
   * Return an array containing the children outline. The array will be empty if the outline has no
   * children.
   * 
   * @return an array containing the children of the element
   */
  public Outline[] getChildren();

  /**
   * Return the kind of the element.
   * 
   * @return the kind of the element
   */
  public ElementKind getKind();

  /**
   * Return the name of the element. This is typically used as the label in the outline.
   * 
   * @return the name of the element
   */
  public String getName();

  /**
   * Return the outline that either physically or logically encloses this outline. This will be
   * {@code null} if this outline is a unit outline.
   * 
   * @return the outline that encloses this outline
   */
  public Outline getParent();

  /**
   * Return the return type of the element. If the element is not a method or function this field
   * will not be defined. If the element does not have a declared return type, this field will
   * contain an empty string.
   * 
   * @return the return type of the element
   */
  public String getReturnType();

  /**
   * True if the element is an abstract member of a class, or is an abstract class itself.
   * 
   * @return {@code true} if the element is an abstract member of a class, or is an abstract class
   *         itself
   */
  public boolean isAbstract();

  /**
   * True if the element is private.
   * 
   * @return {@code true} if the element is private
   */
  public boolean isPrivate();

  /**
   * True if the element is a static member of a class or is a top-level function or field.
   * 
   * @return {@code true} if the element is a static member of a class or is a top-level function or
   *         field
   */
  public boolean isStatic();

}
