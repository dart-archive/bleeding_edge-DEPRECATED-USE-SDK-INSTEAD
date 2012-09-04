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

import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;
import java.net.URI;
import java.util.List;

/**
 * The interface <code>DartLibrary</code> defines the behavior of objects representing a Dart
 * library.
 */
public interface DartLibrary extends OpenableElement, ParentElement {
  /**
   * An empty array of libraries.
   */
  public static final DartLibrary[] EMPTY_LIBRARY_ARRAY = new DartLibrary[0];

  /**
   * Add a #source directive that will cause the given file to be included in this library. This
   * method does not verify that it is valid for the file to be included using a #source directive
   * (does not itself contain any directives). Return the compilation unit representing the source
   * file that was added. Note that if the changes to the compilation unit defining the library were
   * not saved (typically because the file is being edited) that the compilation unit that is
   * returned will not yet be a child of this library.
   * 
   * @param file the file to be added to the library
   * @param monitor the progress monitor used to provide feedback to the user, or <code>null</code>
   *          if no feedback is desired
   * @return the compilation unit representing the source file that was added
   * @throws DartModelException if the directive cannot be added
   */
  public CompilationUnit addSource(File file, IProgressMonitor monitor) throws DartModelException;

  /**
   * Delete this library. This has the effect of deleting the library's project and any derived
   * files associated with the library, except that the final compiled form of the library is not
   * deleted.
   * 
   * @param monitor the progress monitor used to provide feedback to the user, or <code>null</code>
   *          if no feedback is desired
   * @throws DartModelException if the library cannot be deleted for some reason
   */
  public void delete(IProgressMonitor monitor) throws DartModelException;

  /**
   * @return the top-level {@link DartElement} with the given name that is declared within this
   *         library, may be <code>null</code>.
   */
  public DartElement findTopLevelElement(String name) throws DartModelException;

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
   * Return the compilation unit with the specified name in this library (for example,
   * <code>"Object.dart"</code>). The name has to be a valid compilation unit name. This is a
   * handle-only method. The compilation unit may or may not be present.
   * 
   * @param name the name of the compilation unit to be returned
   * @return the compilation unit with the specified name in this package
   */
  public CompilationUnit getCompilationUnit(String name);

  /**
   * Return the compilation unit with the specified file in this library (for example, some URI with
   * file name <code>"Object.dart"</code>). The name has to be a valid compilation unit name. This
   * is a handle-only method. The compilation unit may or may not be present.
   * 
   * @param uri the uri of the compilation unit to be returned
   * @return the compilation unit with the specified name in this package
   */
  public CompilationUnit getCompilationUnit(URI uri);

  /**
   * Return an array containing all of the compilation units defined in this library.
   * 
   * @return an array containing all of the compilation units defined in this library
   * @throws DartModelException if the compilation units defined in this library cannot be
   *           determined for some reason
   */
  public CompilationUnit[] getCompilationUnits() throws DartModelException;

  /**
   * @return {@link CompilationUnit}s that contribute to this library. Here "contribute" means that
   *         unit is defined in this library or any in any directly imported library (not
   *         transitive).
   */
  public List<CompilationUnit> getCompilationUnitsInScope() throws DartModelException;

  /**
   * @return all the {@link CompilationUnit}s that are transitively included in this library. This
   *         is the transitive closure of all referenced Dart source files.
   * @throws DartModelException if the transitive compilation units cannot be determined for some
   *           reason
   */
  public List<CompilationUnit> getCompilationUnitsTransitively() throws DartModelException;

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
   * @return the name specified in "library" directive, may be <code>null</code>.
   */
  public String getLibraryDirectiveName() throws DartModelException;

  /**
   * Return a possibly empty list of the libraries that reference this library.
   * 
   * @return a list of the libraries that reference this library
   * @throws DartModelException if the list of libraries could not be determined
   */
  public List<DartLibrary> getReferencingLibraries() throws DartModelException;

  /**
   * Return the URI of the library file that defines this library, or <code>null</code> if there is
   * no such file or if the URI for the file cannot be determined for some reason.
   * 
   * @return the URI of the library file that defines this library
   */
  public URI getUri();

  /**
   * Return <code>true</code> if this library is defined in a workspace resource, or
   * <code>false</code> if it does not exist in the workspace. Libraries on disk, but not mapped
   * into the workspace and libraries bundled in a plugin are considered non-local.
   * 
   * @return <code>true</code> if the library exists in the workspace
   */
  public boolean isLocal();

  /**
   * Return <code>true</code> if this library is a top-level library. A top-level library is one
   * that the user has explicitly opened.
   * 
   * @return <code>true</code> if this library is a top-level library
   */
  public boolean isTopLevel();

  /**
   * Return <code>true</code> if this library is unreferenced. A library is referenced if it is
   * either a top-level library or if it is imported by a referenced library.
   * 
   * @return <code>true</code> if this library is unreferenced
   * @throws DartModelException if it cannot be determined whether this library is referenced
   */
  public boolean isUnreferenced() throws DartModelException;

  /**
   * Set whether this library is a top-level library to match the given value
   * 
   * @param topLevel <code>true</code> if this library is a top-level library
   */
  public void setTopLevel(boolean topLevel);
}
