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
 * The interface <code>DartVariableDeclaration</code> defines the behavior of elements representing
 * a variable defined within another element. Variables can be defined in {@link DartFunction
 * functions}, {@link Method methods} and {@link CompilationUnit compilation units}, and include
 * parameters defined for either methods or functions.
 */
public interface DartVariableDeclaration extends CompilationUnitElement, SourceReference {
  /**
   * Return the name of the type of this variable, or <code>null</code> if this variable does not
   * have a declared type. In the case where the type is a function type, this method will return a
   * string that contains both the types and names of the function's parameters.
   * 
   * @return the name of the type of this variable
   * @throws DartModelException if the type of this variable cannot be accessed
   */
  public String getFullTypeName() throws DartModelException;

  /**
   * Return the name of the type of this variable, or <code>null</code> if this variable does not
   * have a declared type. In the case where the type is a function type, this method will return a
   * string that contains only the types of the function's parameters and not the parameter names.
   * 
   * @return the name of the type of this variable
   * @throws DartModelException if the type of this variable cannot be accessed
   */
  public String getTypeName() throws DartModelException;

  /**
   * @return the {@link SourceRange} in which this variable is visible.
   */
  public SourceRange getVisibleRange() throws DartModelException;

  /**
   * Return <code>true</code> if this variable is global (defined at the top-level of a compilation
   * unit).
   * 
   * @return <code>true</code> if this variable is global
   */
  public boolean isGlobal();

  /**
   * Return <code>true</code> if this variable is defined within a method or function (parameters
   * are considered to be local).
   * 
   * @return <code>true</code> if this variable is local to a method or function
   */
  public boolean isLocal();

  /**
   * Return <code>true</code> if this local variable is a method or function parameter.
   * 
   * @return <code>true</code> if this local variable is a method or function parameter
   */
  public boolean isParameter();
}
