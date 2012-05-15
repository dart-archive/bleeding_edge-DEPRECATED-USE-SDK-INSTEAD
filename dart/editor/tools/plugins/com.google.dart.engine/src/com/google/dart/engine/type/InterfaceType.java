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

import com.google.dart.engine.element.TypeElement;

/**
 * The interface {@code InterfaceType} defines the behavior common to objects representing the type
 * introduced by either a class or an interface, or a reference to such a type.
 */
public interface InterfaceType extends Type {
  @Override
  public TypeElement getElement();

  /**
   * Return an array containing the actual types of the type arguments. If this type's element does
   * not have type parameters, then the array should be empty (although it is possible for type
   * arguments to be erroneously declared). If the element has type parameters and the actual type
   * does not explicitly include argument values, then the type "Dynamic" will be automatically
   * provided.
   * 
   * @return the actual types of the type arguments
   */
  public Type[] getTypeArguments();
}
