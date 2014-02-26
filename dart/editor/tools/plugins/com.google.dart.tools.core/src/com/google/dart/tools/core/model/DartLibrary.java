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
 * The interface <code>DartLibrary</code> defines the behavior of objects representing a Dart
 * library.
 * 
 * @coverage dart.tools.core.model
 */
public interface DartLibrary extends OpenableElement, ParentElement {
  /**
   * Return the type with the given name that is declared within this library, or <code>null</code>
   * if there is no such type declared in this library.
   * 
   * @param typeName the name of the type to be returned
   * @return the type with the given name that is declared within this library
   * @throws DartModelException if the types defined in this library cannot be determined for some
   *           reason
   */
  public Type findType(String typeName) throws DartModelException;

  /**
   * @return the {@link Type} with the given name that is visible within this library, or
   *         <code>null</code> if there is no such type visible in this library. Here "visible"
   *         means that type declared in this library or declared in one of the imported libraries
   *         and not private.
   */
  public Type findTypeInScope(String typeName) throws DartModelException;

  /**
   * Return an array containing all of the compilation units defined in this library.
   * 
   * @return an array containing all of the compilation units defined in this library
   * @throws DartModelException if the compilation units defined in this library cannot be
   *           determined for some reason
   */
  public CompilationUnit[] getCompilationUnits() throws DartModelException;

  /**
   * Return the compilation unit that defines this library.
   * 
   * @return the compilation unit that defines this library
   * @throws DartModelException if the defining compilation unit cannot be determined
   */
  public CompilationUnit getDefiningCompilationUnit() throws DartModelException;

  /**
   * Return the name of this element as it should appear in the user interface. Typically, this is
   * the same as {@link #getElementName()}. This is a handle-only method.
   * 
   * @return the name of this element
   */
  public String getDisplayName();

  /**
   * Return an array containing all of the libraries imported by this library. The returned
   * libraries are not included in the list of children for the library.
   * 
   * @return an array containing the imported libraries (not <code>null</code>, contains no
   *         <code>null</code>s)
   * @throws DartModelException if the imported libraries cannot be determined
   */
  public DartLibrary[] getImportedLibraries() throws DartModelException;

  /**
   * @return the {@link DartImport}s for all imported libraries into this library, may be empty, but
   *         not <code>null</code>.
   */
  public DartImport[] getImports() throws DartModelException;

  /**
   * Set whether this library is a top-level library to match the given value
   * 
   * @param topLevel <code>true</code> if this library is a top-level library
   */
  public void setTopLevel(boolean topLevel);
}
