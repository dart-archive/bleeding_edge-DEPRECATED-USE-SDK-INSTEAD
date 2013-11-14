/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.element;

/**
 * The interface {@code ElementAnnotation} defines the behavior of objects representing a single
 * annotation associated with an element.
 * 
 * @coverage dart.engine.element
 */
public interface ElementAnnotation {
  /**
   * Return the element representing the field, variable, or const constructor being used as an
   * annotation.
   * 
   * @return the field, variable, or constructor being used as an annotation
   */
  public Element getElement();

  /**
   * Return {@code true} if this annotation marks the associated element as being deprecated.
   * 
   * @return {@code true} if this annotation marks the associated element as being deprecated
   */
  public boolean isDeprecated();

  /**
   * Return {@code true} if this annotation marks the associated method as being expected to
   * override an inherited method.
   * 
   * @return {@code true} if this annotation marks the associated method as overriding another
   *         method
   */
  public boolean isOverride();

  /**
   * Return {@code true} if this annotation marks the associated class as implementing a proxy
   * object.
   * 
   * @return {@code true} if this annotation marks the associated class as implementing a proxy
   *         object
   */
  public boolean isProxy();
}
