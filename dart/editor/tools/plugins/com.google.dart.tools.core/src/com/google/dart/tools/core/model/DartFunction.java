/*
 * Copyright (c) 2011, the Dart project authors.
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
   * Returns the dartdoc range if this element is from source or if this element is a binary element
   * with an attached source, null otherwise.
   * <p>
   * If this element is from source, the dartdoc range is extracted from the corresponding source.
   * </p>
   * <p>
   * If this element is from a binary, the dartdoc is extracted from the attached source if present.
   * </p>
   * <p>
   * If this element's openable is not consistent, then null is returned.
   * </p>
   * 
   * @exception DartModelException if this element does not exist or if an exception occurs while
   *              accessing its corresponding resource.
   * @return a source range corresponding to the dartdoc source or <code>null</code> if no source is
   *         available, this element has no dartdoc comment or this element's openable is not
   *         consistent
   * @see IOpenable#isConsistent()
   */
  public SourceRange getDartDocRange() throws DartModelException;

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
   * Return an array containing the names of the parameter types for this function, or an empty
   * array if this function does not have any parameters.
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
   * Returns whether this function is an entry point.
   * 
   * @return whether this function is an entry point
   */
  public boolean isMain();

}
