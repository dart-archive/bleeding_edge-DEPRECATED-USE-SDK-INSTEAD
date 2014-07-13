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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.angular.AngularViewElement;

/**
 * The interface {@code CompilationUnitElement} defines the behavior of elements representing a
 * compilation unit.
 * 
 * @coverage dart.engine.element
 */
public interface CompilationUnitElement extends Element, UriReferencedElement {
  /**
   * Return an array containing all of the top-level accessors (getters and setters) contained in
   * this compilation unit.
   * 
   * @return the top-level accessors contained in this compilation unit
   */
  public PropertyAccessorElement[] getAccessors();

  /**
   * Return an array containing all of the Angular views defined in this compilation unit. The array
   * will be empty if the element does not have any Angular views or if the compilation unit has not
   * yet had toolkit references resolved.
   * 
   * @return the Angular views defined in this compilation unit.
   */
  public AngularViewElement[] getAngularViews();

  /**
   * Return the library in which this compilation unit is defined.
   * 
   * @return the library in which this compilation unit is defined
   */
  @Override
  public LibraryElement getEnclosingElement();

  /**
   * Return the enum defined in this compilation unit that has the given name, or {@code null} if
   * this compilation unit does not define an enum with the given name.
   * 
   * @param enumName the name of the enum to be returned
   * @return the enum with the given name that is defined in this compilation unit
   */
  public ClassElement getEnum(String enumName);

  /**
   * Return an array containing all of the enums contained in this compilation unit.
   * 
   * @return an array containing all of the enums contained in this compilation unit
   */
  public ClassElement[] getEnums();

  /**
   * Return an array containing all of the top-level functions contained in this compilation unit.
   * 
   * @return the top-level functions contained in this compilation unit
   */
  public FunctionElement[] getFunctions();

  /**
   * Return an array containing all of the function type aliases contained in this compilation unit.
   * 
   * @return the function type aliases contained in this compilation unit
   */
  public FunctionTypeAliasElement[] getFunctionTypeAliases();

  /**
   * Return the resolved {@link CompilationUnit} node that declares this element.
   * <p>
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   * 
   * @return the resolved {@link CompilationUnit}, not {@code null}.
   */
  @Override
  public CompilationUnit getNode() throws AnalysisException;

  /**
   * Return an array containing all of the top-level variables contained in this compilation unit.
   * 
   * @return the top-level variables contained in this compilation unit
   */
  public TopLevelVariableElement[] getTopLevelVariables();

  /**
   * Return the class defined in this compilation unit that has the given name, or {@code null} if
   * this compilation unit does not define a class with the given name.
   * 
   * @param className the name of the class to be returned
   * @return the class with the given name that is defined in this compilation unit
   */
  public ClassElement getType(String className);

  /**
   * Return an array containing all of the classes contained in this compilation unit.
   * 
   * @return the classes contained in this compilation unit
   */
  public ClassElement[] getTypes();

  /**
   * Return {@code true} if this compilation unit defines a top-level function named
   * {@code loadLibrary}.
   * 
   * @return {@code true} if this compilation unit defines a top-level function named
   *         {@code loadLibrary}
   */
  public boolean hasLoadLibraryFunction();
}
