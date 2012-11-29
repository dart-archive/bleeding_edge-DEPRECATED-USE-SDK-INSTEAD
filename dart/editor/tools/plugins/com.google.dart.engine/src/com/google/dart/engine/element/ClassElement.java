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
 * The interface {@code ClassElement} defines the behavior of elements that represent a class.
 */
public interface ClassElement extends Element {
  /**
   * Return an array containing all of the accessors (getters and setters) contained in this class.
   * 
   * @return the accessors contained in this class
   */
  public PropertyAccessorElement[] getAccessors();

  /**
   * Return an array containing all of the constructors contained in this class.
   * 
   * @return the constructors contained in this class
   */
  public ConstructorElement[] getConstructors();

  /**
   * Return an array containing all of the fields contained in this class.
   * 
   * @return the fields contained in this class
   */
  public FieldElement[] getFields();

  /**
   * Return an array containing all of the interfaces that are implemented by this class.
   * 
   * @return the interfaces that are implemented by this class
   */
  public Type[] getInterfaces();

  /**
   * Return an array containing all of the methods contained in this class.
   * 
   * @return the methods contained in this class
   */
  public MethodElement[] getMethods();

  /**
   * Return an array containing all of the mixins that are applied to the class being extended in
   * order to derive the superclass of this class.
   * 
   * @return the mixins that are applied to derive the superclass of this class
   */
  public Type[] getMixins();

  /**
   * Return the superclass of this class. Return {@code null} if the class does not have an explicit
   * superclass.
   * 
   * @return the superclass of this class
   */
  public Type getSupertype();

  /**
   * Return the type defined by the class.
   * 
   * @return the type defined by the class
   */
  public Type getType();

  /**
   * Return an array containing all of the type variables defined for this class.
   * 
   * @return the type variables defined for this class
   */
  public TypeVariableElement[] getTypeVariables();

  /**
   * Return {@code true} if this class is abstract. A class is abstract if it has an explicit
   * {@code abstract} modifier. Note, that this definition of <i>abstract</i> is different from
   * <i>has unimplemented members</i>.
   * 
   * @return {@code true} if this class is abstract
   */
  public boolean isAbstract();
}
