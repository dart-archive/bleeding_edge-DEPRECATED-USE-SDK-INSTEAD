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

import com.google.dart.engine.type.FunctionType;

/**
 * The interface {@code TypeAliasElement} defines the behavior of elements representing a type alias
 * ({@code typedef}).
 */
public interface TypeAliasElement extends Element {
  /**
   * Return the compilation unit in which this type alias is defined.
   * 
   * @return the compilation unit in which this type alias is defined
   */
  @Override
  public CompilationUnitElement getEnclosingElement();

  /**
   * Return an array containing all of the parameters defined by this type alias.
   * 
   * @return the parameters defined by this type alias
   */
  public VariableElement[] getParameters();

  /**
   * Return the type of function defined by this type alias.
   * 
   * @return the type of function defined by this type alias
   */
  public FunctionType getType();

  /**
   * Return an array containing all of the type variables defined for this type.
   * 
   * @return the type variables defined for this type
   */
  public TypeVariableElement[] getTypeVariables();
}
