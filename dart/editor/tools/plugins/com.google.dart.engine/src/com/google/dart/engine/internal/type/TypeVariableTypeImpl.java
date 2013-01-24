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

import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeVariableType;
import com.google.dart.engine.utilities.general.ObjectUtilities;

/**
 * Instances of the class {@code TypeVariableTypeImpl} defines the behavior of objects representing
 * the type introduced by a type variable.
 */
public class TypeVariableTypeImpl extends TypeImpl implements TypeVariableType {
  /**
   * Return an array containing the type variable types defined by the given array of type variable
   * elements.
   * 
   * @param typeVariables the type variable elements defining the type variable types to be returned
   * @return the type variable types defined by the type variable elements
   */
  public static TypeVariableType[] getTypes(TypeVariableElement[] typeVariables) {
    int count = typeVariables.length;
    TypeVariableType[] types = new TypeVariableType[count];
    for (int i = 0; i < count; i++) {
      types[i] = typeVariables[i].getType();
    }
    return types;
  }

  /**
   * Initialize a newly created type variable to be declared by the given element and to have the
   * given name.
   * 
   * @param element the element representing the declaration of the type variable
   */
  public TypeVariableTypeImpl(TypeVariableElement element) {
    super(element, element.getName());
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof TypeVariableTypeImpl
        && ObjectUtilities.equals(getElement(), ((TypeVariableTypeImpl) object).getElement());
  }

  @Override
  public TypeVariableElement getElement() {
    return (TypeVariableElement) super.getElement();
  }

  @Override
  public int hashCode() {
    return getElement().hashCode();
  }

  @Override
  public boolean isMoreSpecificThan(Type type) {
    //
    // T is a type variable and S is the upper bound of T.
    //
    Type upperBound = getElement().getBound();
    return type.equals(upperBound);
  }

  @Override
  public boolean isSubtypeOf(Type type) {
    return true;
  }

  @Override
  public Type substitute(Type[] argumentTypes, Type[] parameterTypes) {
    int length = parameterTypes.length;
    for (int i = 0; i < length; i++) {
      if (parameterTypes[i].equals(this)) {
        return argumentTypes[i];
      }
    }
    return this;
  }
}
