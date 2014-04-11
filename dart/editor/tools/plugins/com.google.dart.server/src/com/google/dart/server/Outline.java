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

import com.google.dart.engine.element.ElementKind;

/**
 * The interface {@code Outline} defines the behavior of objects that represent an outline for a
 * single source.
 */
public interface Outline {
  /**
   * Return the argument list for the element, or {@code null} if the element is not a method or
   * function. If the element has zero arguments, the string {@code "()"} will be returned.
   * 
   * @return the argument list for the element
   */
  public String getArguments();

  /**
   * Return an array containing the children of the element. The array will be empty if the element
   * has no children.
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
   * Return the length of the element's name.
   * 
   * @return the length of the element's name
   */
  public int getLength();

  /**
   * Return the name of the element.
   * 
   * @return the name of the element
   */
  public String getName();

  /**
   * Return the offset to the beginning of the element's name.
   * 
   * @return the offset to the beginning of the element's name
   */
  public int getOffset();

  /**
   * Return the return type of the element, or {@code null} if the element is not a method or
   * function. If the element does not have a declared return type then an empty string will be
   * returned.
   * 
   * @return the return type of the element
   */
  public String getReturnType();
}
