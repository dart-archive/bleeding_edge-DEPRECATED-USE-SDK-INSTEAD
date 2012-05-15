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

import com.google.dart.engine.element.ExecutableElement;

import java.util.Map;

/**
 * The interface {@code FunctionType} defines the behavior common to objects representing the type
 * of a function, method, constructor, getter, or setter.
 */
public interface FunctionType extends Type {
  @Override
  public ExecutableElement getElement();

  /**
   * Return a map from the names of named parameters to the types of the named parameters of this
   * type of function. The entries in the map will be iterated in the same order as the order in
   * which the named parameters were defined.
   * 
   * @return a map from the name to the types of the named parameters of this type of function
   */
  public Map<String, Type> getNamedParameterTypes();

  /**
   * Return an array containing the types of the normal parameters of this type of function. The
   * parameter types are in the same order as they appear in the declaration of the function.
   * 
   * @return the types of the normal parameters of this type of function
   */
  public Type[] getNormalParameterTypes();

  /**
   * Return the type of object returned by this type of function.
   * 
   * @return the type of object returned by this type of function
   */
  public Type getReturnType();
}
