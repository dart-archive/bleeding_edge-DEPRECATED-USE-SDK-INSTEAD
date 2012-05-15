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

/**
 * The interface {@code LibraryElement} defines the behavior of elements representing a library.
 */
public interface LibraryElement extends Element {
  /**
   * Return the compilation unit that defines this library.
   * 
   * @return the compilation unit that defines this library
   */
  public CompilationUnitElement getDefiningCompilationUnit();

  /**
   * Return the entry point for this library, or {@code null} if this library does not have an entry
   * point. The entry point is defined to be a zero argument top-level function whose name is
   * {@code main}.
   * 
   * @return the entry point for this library
   */
  public FunctionElement getEntryPoint();

  /**
   * Return an array containing all of the libraries that are imported into this library. This
   * includes all of the libraries that are imported using a prefix (also available through the
   * prefixes returned by {@link #getPrefixes()}) and those that are imported without a prefix.
   * 
   * @return an array containing all of the libraries that are imported into this library
   */
  public LibraryElement[] getImportedLibraries();

  /**
   * Return an array containing elements for each of the prefixes used to {@code #import} libraries
   * into this library. Each prefix can be used in more than one {@code #import} directive.
   * 
   * @return the prefixes used to {@code #import} libraries into this library
   */
  public PrefixElement[] getPrefixes();

  /**
   * Return an array containing all of the compilation units that are {@code #source}'d into this
   * library. This does not include the defining compilation unit that contains the {@code #source}
   * directives.
   * 
   * @return the compilation units that are {@code #source}'d into this library
   */
  public CompilationUnitElement[] getSourcedCompilationUnits();
}
