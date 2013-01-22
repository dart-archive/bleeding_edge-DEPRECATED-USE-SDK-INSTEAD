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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Instances of the class {@code FunctionTypeImpl} defines the behavior common to objects
 * representing the type of a function, method, constructor, getter, or setter.
 */
public class FunctionTypeImpl extends TypeImpl implements FunctionType {
  /**
   * Return {@code true} if all of the types in the first array are equal to the corresponding types
   * in the second array.
   * 
   * @param firstTypes the first array of types being compared
   * @param secondTypes the second array of types being compared
   * @return {@code true} if all of the types in the first array are equal to the corresponding
   *         types in the second array
   */
  private static boolean equals(LinkedHashMap<String, Type> firstTypes,
      LinkedHashMap<String, Type> secondTypes) {
    if (secondTypes.size() != firstTypes.size()) {
      return false;
    }
    Iterator<Map.Entry<String, Type>> firstIterator = firstTypes.entrySet().iterator();
    Iterator<Map.Entry<String, Type>> secondIterator = firstTypes.entrySet().iterator();
    while (firstIterator.hasNext()) {
      Map.Entry<String, Type> firstEntry = firstIterator.next();
      Map.Entry<String, Type> secondEntry = secondIterator.next();
      if (!firstEntry.getKey().equals(secondEntry.getKey())
          || !firstEntry.getValue().equals(secondEntry.getValue())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return a map containing the results of using the given argument types and parameter types to
   * perform a substitution on all of the values in the given map. The order of the entries will be
   * preserved.
   * 
   * @param types the types on which a substitution is to be performed
   * @param argumentTypes the argument types for the substitution
   * @param parameterTypes the parameter types for the substitution
   * @return the result of performing the substitution on each of the types
   */
  private static LinkedHashMap<String, Type> substitute(LinkedHashMap<String, Type> types,
      Type[] argumentTypes, Type[] parameterTypes) {
    LinkedHashMap<String, Type> newTypes = new LinkedHashMap<String, Type>();
    for (Map.Entry<String, Type> entry : types.entrySet()) {
      newTypes.put(entry.getKey(), entry.getValue().substitute(argumentTypes, parameterTypes));
    }
    return newTypes;
  }

  /**
   * An array containing the actual types of the type arguments.
   */
  private Type[] typeArguments = TypeImpl.EMPTY_ARRAY;

  /**
   * An array containing the types of the normal parameters of this type of function. The parameter
   * types are in the same order as they appear in the declaration of the function.
   * 
   * @return the types of the normal parameters of this type of function
   */
  private Type[] normalParameterTypes = TypeImpl.EMPTY_ARRAY;

  /**
   * A table mapping the names of optional (positional) parameters to the types of the optional
   * parameters of this type of function.
   */
  private Type[] optionalParameterTypes = TypeImpl.EMPTY_ARRAY;

  /**
   * A table mapping the names of named parameters to the types of the named parameters of this type
   * of function.
   */
  private LinkedHashMap<String, Type> namedParameterTypes = new LinkedHashMap<String, Type>();

  /**
   * The type of object returned by this type of function.
   */
  private Type returnType = VoidTypeImpl.getInstance();

  /**
   * Initialize a newly created function type to be declared by the given element and to have the
   * given name.
   * 
   * @param element the element representing the declaration of the function type
   */
  public FunctionTypeImpl(ExecutableElement element) {
    super(element, element == null ? null : element.getName());
  }

  /**
   * Initialize a newly created function type to be declared by the given element and to have the
   * given name.
   * 
   * @param element the element representing the declaration of the function type
   */
  public FunctionTypeImpl(TypeAliasElement element) {
    super(element, element == null ? null : element.getName());
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof FunctionTypeImpl)) {
      return false;
    }
    FunctionTypeImpl otherType = (FunctionTypeImpl) object;
    return ObjectUtilities.equals(getElement(), otherType.getElement())
        && Arrays.equals(normalParameterTypes, otherType.normalParameterTypes)
        && Arrays.equals(optionalParameterTypes, otherType.optionalParameterTypes)
        && equals(namedParameterTypes, otherType.namedParameterTypes);
  }

  @Override
  public Map<String, Type> getNamedParameterTypes() {
    return namedParameterTypes;
  }

  @Override
  public Type[] getNormalParameterTypes() {
    return normalParameterTypes;
  }

  @Override
  public Type[] getOptionalParameterTypes() {
    return optionalParameterTypes;
  }

  @Override
  public Type getReturnType() {
    return returnType;
  }

  @Override
  public Type[] getTypeArguments() {
    return typeArguments;
  }

  @Override
  public int hashCode() {
    return getElement().hashCode();
  }

  @Override
  public boolean isSubtypeOf(Type type) {
    // TODO(brianwilkerson) Implement this
    return false;
  }

  /**
   * Set the mapping of the names of named parameters to the types of the named parameters of this
   * type of function to the given mapping.
   * 
   * @param namedParameterTypes the mapping of the names of named parameters to the types of the
   *          named parameters of this type of function
   */
  public void setNamedParameterTypes(LinkedHashMap<String, Type> namedParameterTypes) {
    this.namedParameterTypes = namedParameterTypes;
  }

  /**
   * Set the types of the normal parameters of this type of function to the types in the given
   * array.
   * 
   * @param normalParameterTypes the types of the normal parameters of this type of function
   */
  public void setNormalParameterTypes(Type[] normalParameterTypes) {
    this.normalParameterTypes = normalParameterTypes;
  }

  /**
   * Set the types of the optional parameters of this type of function to the types in the given
   * array.
   * 
   * @param optionalParameterTypes the types of the optional parameters of this type of function
   */
  public void setOptionalParameterTypes(Type[] optionalParameterTypes) {
    this.optionalParameterTypes = optionalParameterTypes;
  }

  /**
   * Set the type of object returned by this type of function to the given type.
   * 
   * @param returnType the type of object returned by this type of function
   */
  public void setReturnType(Type returnType) {
    this.returnType = returnType;
  }

  /**
   * Set the actual types of the type arguments to the given types.
   * 
   * @param typeArguments the actual types of the type arguments
   */
  public void setTypeArguments(Type[] typeArguments) {
    this.typeArguments = typeArguments;
  }

  @Override
  public FunctionTypeImpl substitute(Type[] argumentTypes) {
    return substitute(argumentTypes, getTypeArguments());
  }

  @Override
  public FunctionTypeImpl substitute(Type[] argumentTypes, Type[] parameterTypes) {
    if (argumentTypes.length != parameterTypes.length) {
      throw new IllegalArgumentException("argumentTypes.length (" + argumentTypes.length
          + ") != parameterTypes.length (" + parameterTypes.length + ")");
    }
    if (argumentTypes.length == 0) {
      return this;
    }
    Element element = getElement();
    FunctionTypeImpl newType = (element instanceof ExecutableElement) ? new FunctionTypeImpl(
        (ExecutableElement) element) : new FunctionTypeImpl((TypeAliasElement) element);
    newType.setReturnType(returnType.substitute(argumentTypes, parameterTypes));
    newType.setNormalParameterTypes(substitute(normalParameterTypes, argumentTypes, parameterTypes));
    newType.setOptionalParameterTypes(substitute(
        optionalParameterTypes,
        argumentTypes,
        parameterTypes));
    newType.setNamedParameterTypes(substitute(namedParameterTypes, argumentTypes, parameterTypes));
    return newType;
  }
}
