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
package com.google.dart.engine.type;

import com.google.dart.engine.element.Element;

/**
 * The interface {@code Type} defines the behavior of objects representing the declared type of
 * elements in the element model.
 */
public interface Type {
  /**
   * Return the element representing the declaration of this type, or {@code null} if the type has
   * not, or cannot, be associated with an element. The former case will occur if the element model
   * is not yet complete; the latter case will occur if this object represents an undefined type.
   * 
   * @return the element representing the declaration of this type
   */
  public Element getElement();

  /**
   * Return the name of this type, or {@code null} if the type does not have a name, such as when
   * the type represents the type of an unnamed function.
   * 
   * @return the name of this type
   */
  public String getName();

  /**
   * Return {@code true} if this type is assignable to the given type. A type <i>T</i> may be
   * assigned to a type <i>S</i>, written <i>T</i> &hArr; <i>S</i>, iff either <i>T</i> <: <i>S</i>
   * or <i>S</i> <: <i>T</i>.
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is assignable to the given type
   */
  public boolean isAssignableTo(Type type);

  /**
   * Return {@code true} if this type is more specific than the given type.
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is more specific than the given type
   */
  public boolean isMoreSpecificThan(Type type);

  /**
   * Return {@code true} if this type is a subtype of the given type.
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is a subtype of the given type
   */
  public boolean isSubtypeOf(Type type);

  /**
   * Return {@code true} if this type is a supertype of the given type. A type <i>S</i> is a
   * supertype of <i>T</i>, written <i>S</i> :> <i>T</i>, iff <i>T</i> is a subtype of <i>S</i>.
   * 
   * @param type the type being compared with this type
   * @return {@code true} if this type is a supertype of the given type
   */
  public boolean isSupertypeOf(Type type);
}
