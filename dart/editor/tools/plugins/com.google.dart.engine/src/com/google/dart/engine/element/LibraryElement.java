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
 * 
 * @coverage dart.engine.element
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
   * Return an array containing all of the libraries that are exported from this library.
   * 
   * @return an array containing all of the libraries that are exported from this library
   */
  public LibraryElement[] getExportedLibraries();

  /**
   * Return an array containing all of the exports defined in this library.
   * 
   * @return the exports defined in this library
   */
  public ExportElement[] getExports();

  /**
   * Return an array containing all of the libraries that are imported into this library. This
   * includes all of the libraries that are imported using a prefix (also available through the
   * prefixes returned by {@link #getPrefixes()}) and those that are imported without a prefix.
   * 
   * @return an array containing all of the libraries that are imported into this library
   */
  public LibraryElement[] getImportedLibraries();

  /**
   * Return an array containing all of the imports defined in this library.
   * 
   * @return the imports defined in this library
   */
  public ImportElement[] getImports();

  /**
   * Return an array containing all of the imports that share the given prefix, or an empty array if
   * there are no such imports.
   * 
   * @param prefixElement the prefix element shared by the returned imports
   */
  public ImportElement[] getImportsWithPrefix(PrefixElement prefixElement);

  /**
   * Return the element representing the synthetic function {@code loadLibrary} that is implicitly
   * defined for this library if the library is imported using a deferred import.
   */
  public FunctionElement getLoadLibraryFunction();

  /**
   * Return an array containing all of the compilation units that are included in this library using
   * a {@code part} directive. This does not include the defining compilation unit that contains the
   * {@code part} directives.
   * 
   * @return the compilation units that are included in this library
   */
  public CompilationUnitElement[] getParts();

  /**
   * Return an array containing elements for each of the prefixes used to {@code import} libraries
   * into this library. Each prefix can be used in more than one {@code import} directive.
   * 
   * @return the prefixes used to {@code import} libraries into this library
   */
  public PrefixElement[] getPrefixes();

  /**
   * Return the class defined in this library that has the given name, or {@code null} if this
   * library does not define a class with the given name.
   * 
   * @param className the name of the class to be returned
   * @return the class with the given name that is defined in this library
   */
  public ClassElement getType(String className);

  /**
   * Return an array containing all of the compilation units this library consists of. This includes
   * the defining compilation unit and units included using the {@code part} directive.
   * 
   * @return the compilation units this library consists of
   */
  public CompilationUnitElement[] getUnits();

  /**
   * Return an array containing all directly and indirectly imported libraries.
   * 
   * @return all directly and indirectly imported libraries
   */
  public LibraryElement[] getVisibleLibraries();

  /**
   * Return {@code true} if the defining compilation unit of this library contains at least one
   * import directive whose URI uses the "dart-ext" scheme.
   */
  public boolean hasExtUri();

  /**
   * Return {@code true} if this library defines a top-level function named {@code loadLibrary}.
   * 
   * @return {@code true} if this library defines a top-level function named {@code loadLibrary}
   */
  public boolean hasLoadLibraryFunction();

  /**
   * Return {@code true} if this library is created for Angular analysis. If this library has not
   * yet had toolkit references resolved, then {@code false} will be returned.
   * 
   * @return {@code true} if this library is created for Angular analysis
   */
  public boolean isAngularHtml();

  /**
   * Return {@code true} if this library is an application that can be run in the browser.
   * 
   * @return {@code true} if this library is an application that can be run in the browser
   */
  public boolean isBrowserApplication();

  /**
   * Return {@code true} if this library is the dart:core library.
   * 
   * @return {@code true} if this library is the dart:core library
   */
  public boolean isDartCore();

  /**
   * Return {@code true} if this library is the dart:core library.
   * 
   * @return {@code true} if this library is the dart:core library
   */
  public boolean isInSdk();

  /**
   * Return {@code true} if this library is up to date with respect to the given time stamp. If any
   * transitively referenced Source is newer than the time stamp, this method returns false.
   * 
   * @param timeStamp the time stamp to compare against
   * @return {@code true} if this library is up to date with respect to the given time stamp
   */
  public boolean isUpToDate(long timeStamp);
}
