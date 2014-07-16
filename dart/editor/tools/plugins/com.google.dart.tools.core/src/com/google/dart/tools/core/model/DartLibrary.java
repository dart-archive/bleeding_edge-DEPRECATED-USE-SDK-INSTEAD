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
   * Return an array containing all of the compilation units defined in this library.
   * 
   * @return an array containing all of the compilation units defined in this library
   * @throws DartModelException if the compilation units defined in this library cannot be
   *           determined for some reason
   */
  public CompilationUnit[] getCompilationUnits() throws DartModelException;

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
}
