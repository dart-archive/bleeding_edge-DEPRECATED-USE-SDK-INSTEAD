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

import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.Type;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Instances of the class {@code FunctionTypeImpl} defines the behavior common to objects
 * representing the type of a function, method, constructor, getter, or setter.
 */
public class FunctionTypeImpl extends TypeImpl implements FunctionType {
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
    super(element, element.getName());
  }

  @Override
  public ExecutableElement getElement() {
    return (ExecutableElement) super.getElement();
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
}
