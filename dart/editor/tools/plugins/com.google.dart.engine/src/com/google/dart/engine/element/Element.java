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
 * The interface <code>Element</code> defines the behavior common to all of the elements in the
 * element model. The element model is a model of the program elements that are declared with a name
 * and hence can be referenced elsewhere in the code.
 */
public interface Element {
  /**
   * Return the element that either physically or logically encloses this element.
   * 
   * @return the element that encloses this element
   */
  public Element getEnclosingElement();

  /**
   * Return the kind of element that this is.
   * 
   * @return the kind of this element
   */
  public ElementKind getKind();

  /**
   * Return the name of this element.
   * 
   * @return the name of this element
   */
  public String getName();

  /**
   * Return <code>true</code> if this element is synthetic. A synthetic element is an element that
   * is not represented in the source code explicitly, but is implied by the source code, such as
   * the default constructor for a class that does not explicitly define any constructors.
   * 
   * @return <code>true</code> if this element is synthetic
   */
  public boolean isSynthetic();
}
