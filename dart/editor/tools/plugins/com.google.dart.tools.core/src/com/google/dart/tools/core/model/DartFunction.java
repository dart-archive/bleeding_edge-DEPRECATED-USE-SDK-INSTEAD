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
 * The interface <code>DartFunction</code> defines the behavior of elements representing the
 * definition of a function within Dart source code.
 */
public interface DartFunction extends CompilationUnitElement, ParentElement, SourceReference {
  /**
   * The function main
   */
  public static String MAIN = "main";

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
   * Return an array containing all of the local variables and parameters defined for this function,
   * or an empty array if this function does not have any local variables or parameters.
   * 
   * @return an array containing the local variables and parameters defined for this function
   * @throws DartModelException if the local variables and parameters cannot be accessed
   */
  public DartVariableDeclaration[] getLocalVariables() throws DartModelException;

  /**
   * Return an array containing the names of the parameters for this function, or an empty array if
   * this function does not have any parameters.
   * 
   * @return an array containing the names of the parameters for this function
   * @throws DartModelException if the names of the parameters cannot be accessed
   */
  public String[] getParameterNames() throws DartModelException;

  /**
   * @return the {@link SourceRange} of close paren in parameters declaration.
   */
  public SourceRange getParametersCloseParen() throws DartModelException;

  /**
   * @return the {@link SourceRange} of the close character optional parameters declaration,
   *         <code>]</code> or <code>}</code>. May be <code>null</code> if no optional parameters.
   */
  public SourceRange getOptionalParametersClosingGroupChar() throws DartModelException;

  /**
   * @return the {@link SourceRange} of the open character optional parameters declaration,
   *         <code>[</code> or <code>{</code>. May be <code>null</code> if no optional parameters.
   */
  public SourceRange getOptionalParametersOpeningGroupChar() throws DartModelException;

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
   * @return the {@link SourceRange} in which this local function is visible, undefined if not
   *         {@link #isLocal()} or has no name.
   */
  public SourceRange getVisibleRange() throws DartModelException;

  /**
   * @return <code>true</code> if this function is global (defined at the top-level of a compilation
   *         unit)
   */
  public boolean isGlobal();

  /**
   * @return <code>true</code> if this function is local to a method or function
   */
  public boolean isLocal();

  /**
   * Returns whether this function is an entry point.
   * 
   * @return whether this function is an entry point
   */
  public boolean isMain();
}
