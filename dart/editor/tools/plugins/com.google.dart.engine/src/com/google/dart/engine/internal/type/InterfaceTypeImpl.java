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
package com.google.dart.engine.internal.type;

import com.google.dart.engine.element.TypeElement;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code InterfaceTypeImpl} defines the behavior common to objects
 * representing the type introduced by either a class or an interface, or a reference to such a
 * type.
 */
public class InterfaceTypeImpl extends TypeImpl implements InterfaceType {
  /**
   * An array containing the actual types of the type arguments.
   */
  private Type[] typeArguments;

  /**
   * The instance representing the type "Dynamic".
   */
  private static final InterfaceTypeImpl DYNAMIC_TYPE = new InterfaceTypeImpl("Dynamic");

  /**
   * Return a shared instance of this class representing the type {@code Dynamic}.
   * 
   * @return an instance of this class representing the type {@code Dynamic}
   */
  public static InterfaceTypeImpl getDynamic() {
    return DYNAMIC_TYPE;
  }

  /**
   * Initialize a newly created type to be declared by the given element.
   * 
   * @param element the element representing the declaration of the type
   */
  public InterfaceTypeImpl(TypeElement element) {
    super(element, element.getName());
  }

  /**
   * Initialize a newly created type to have the given name. This constructor should only be used in
   * cases where there is no declaration of the type.
   * 
   * @param name the name of the type
   */
  private InterfaceTypeImpl(String name) {
    super(null, name);
  }

  @Override
  public TypeElement getElement() {
    return (TypeElement) super.getElement();
  }

  @Override
  public Type[] getTypeArguments() {
    return typeArguments;
  }

  /**
   * Set the actual types of the type arguments to those in the given array.
   * 
   * @param typeArguments the actual types of the type arguments
   */
  public void setTypeArguments(Type[] typeArguments) {
    this.typeArguments = typeArguments;
  }
}
