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
package com.google.dart.tools.core.model;

/**
 * The interface <code>DartFunctionTypeAlias</code> defines the behavior of objects representing
 * function type aliases defined in compilation units.
 */
public interface DartFunctionTypeAlias extends CompilationUnitElement, SourceManipulation,
    SourceReference {
  /**
   * Return an array containing the full names of the parameter types for this function, or an empty
   * array if this function does not have any parameters. In the case where the type of a parameter
   * is a function type, this method will return a string that contains both the types and names of
   * the function's parameters.
   * 
   * @return an array containing the full names of the parameter types for this function
   * @throws DartModelException if the names of the parameter types cannot be accessed
   */
  public String[] getFullParameterTypeNames() throws DartModelException;

  /**
   * Return an array containing the names of the parameters for this function, or an empty array if
   * this function does not have any parameters.
   * 
   * @return an array containing the names of the parameters for this function
   * @throws DartModelException if the names of the parameters cannot be accessed
   */
  public String[] getParameterNames() throws DartModelException;

  /**
   * Return an array containing the names of the parameter types for this function, or an empty
   * array if this function does not have any parameters. In the case where the type of a parameter
   * is a function type, this method will return a string that contains only the types of the
   * function's parameters and not the parameter names.
   * 
   * @return an array containing the names of the parameter types for this function
   * @throws DartModelException if the names of the parameter types cannot be accessed
   */
  public String[] getParameterTypeNames() throws DartModelException;

  /**
   * Return the name of the return type of this function, or <code>null</code> if this function does
   * not have a return type.
   * 
   * @return the name of the return type of this function
   * @throws DartModelException if the return type of this function cannot be accessed
   */
  public String getReturnTypeName() throws DartModelException;

  /**
   * @return the {@link DartTypeParameter}s declared by this type
   */
  public DartTypeParameter[] getTypeParameters() throws DartModelException;
}
