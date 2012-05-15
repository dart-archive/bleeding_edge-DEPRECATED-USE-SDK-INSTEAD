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

import com.google.dart.engine.type.Type;

/**
 * The interface {@code TypeElement} defines the behavior of elements that represent a type (either
 * a class or an interface).
 */
public interface TypeElement extends Element {
  /**
   * Return an array containing all of the accessors (getters and setters) contained in this type.
   * 
   * @return the accessors contained in this type
   */
  public PropertyAccessorElement[] getAccessors();

  /**
   * Return an array containing all of the constructors contained in this type.
   * 
   * @return the constructors contained in this type
   */
  public ConstructorElement[] getConstructors();

  /**
   * If this type represents an interface, return the default class for the interface. Return
   * {@code null} if either the interface does not have a default class or this type represents a
   * class.
   * 
   * @return the default class for this type
   */
  public Type getDefaultClass();

  /**
   * Return an array containing all of the fields contained in this type.
   * 
   * @return the fields contained in this type
   */
  public FieldElement[] getFields();

  /**
   * Return an array containing all of the interfaces that are implemented or extended by this type.
   * 
   * @return the interfaces that are implemented or extended by this type
   */
  public Type[] getInterfaces();

  /**
   * Return an array containing all of the methods contained in this type.
   * 
   * @return the methods contained in this type
   */
  public MethodElement[] getMethods();

  /**
   * If this type represents a class, return the superclass of the class. Return {@code null} if
   * either the class does not have an explicit superclass or if this type represents an interface.
   * 
   * @return the superclass of this type
   */
  public Type getSupertype();

  /**
   * Return an array containing all of the type variables defined for this type.
   * 
   * @return the type variables defined for this type
   */
  public TypeVariableElement[] getTypeVariables();

  /**
   * Return {@code true} if this type is abstract. A type is abstract if it is an interface, it has
   * an explicit {@code abstract} modifier, or it has an abstract method. Note, that this definition
   * of <i>abstract</i> is different from <i>has unimplemented members</i>.
   * 
   * @return {@code true} if this type is abstract
   */
  public boolean isAbstract();

  /**
   * Return {@code true} if this element represents an interface.
   * 
   * @return {@code true} if this element represents an interface
   */
  public boolean isInterface();
}
