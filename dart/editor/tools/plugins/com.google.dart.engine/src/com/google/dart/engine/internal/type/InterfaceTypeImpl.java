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
   * The instance representing the type {@code dynamic}.
   */
  private static final InterfaceTypeImpl DYNAMIC_TYPE = new InterfaceTypeImpl("dynamic");

  /**
   * Return a shared instance of this class representing the type {@code dynamic}.
   * 
   * @return an instance of this class representing the type {@code dynamic}
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

  @Override
  public boolean isDirectSupertypeOf(InterfaceType type) {
    TypeElement i = getElement();
    TypeElement j = type.getElement();
    Type supertype = j.getSupertype();
    //
    // I is Object, and J has no extends clause.
    //
    if (supertype == null) {
      // TODO(brianwilkerson) Finish implementing this.
//      if (i.isObject()) {
//        return true;
//      }
      return false;
    }
    TypeElement supertypeElement = (TypeElement) supertype.getElement();
    //
    // I is listed in the extends clause of J.
    //
    if (supertypeElement.equals(i)) {
      return true;
    }
    //
    // I is listed in the implements clause of J.
    //
    for (Type interfaceType : j.getInterfaces()) {
      if (interfaceType.equals(i)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isMoreSpecificThan(Type type) {
    if (!(type instanceof InterfaceType)) {
      return false;
    }
    InterfaceType s = (InterfaceType) type;
    //
    // A type T is more specific than a type S, written T << S,  if one of the following conditions
    // is met:
    //
    //
    // Reflexivity: T is S.
    //
    if (this.equals(s)) {
      return true;
    }
    //
    // T is bottom.
    //
    // This case is handled by the class BottomTypeImpl.
    //
    // S is dynamic.
    //
    if (s.equals(DYNAMIC_TYPE)) {
      return true;
    }
    //
    // Direct supertype: S is a direct supertype of T.
    //
    if (s.isDirectSupertypeOf(this)) {
      return true;
    }
    //
    // Covariance: T is of the form I<T1, ..., Tn> and S is of the form I<S1, ..., Sn> and Ti << Si, 1 <= i <= n.
    //
    // TODO(brianwilkerson) Implement this.
//    TypeElement tElement = getElement();
//    TypeElement sElement = s.getElement();
//    if (tElement.equals(sElement)) {
//      TypeVariableElement[] tVariables = tElement.getTypeVariables();
//      TypeVariableElement[] sVariables = sElement.getTypeVariables();
//      if (tVariables.length != sVariables.length) {
//        return false;
//      }
//      for (int i = 0; i < tVariables.length; i++) {
//        if (!tVariables[0].isMoreSpecificThan(sVariables[0])) {
//          return false;
//        }
//      }
//    }
    //
    // Transitivity: <i>T &laquo; U</i> and <i>U &laquo; S</i>.
    //
    // TODO(brianwilkerson) Implement this.
    return false;
  }

  @Override
  public boolean isSubtypeOf(Type type) {
    //
    // T is a subtype of S, written T <: S, iff [bottom/dynamic]T << S
    //
    // TODO(brianwilkerson) This is an approximation that needs to be fixed once the type
    // substitution operation is implemented.
    if (!(type instanceof InterfaceType)) {
      return false;
    } else if (this.equals(type)) {
      return true;
    }
    return getElement().getSupertype().isSubtypeOf(type);
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
