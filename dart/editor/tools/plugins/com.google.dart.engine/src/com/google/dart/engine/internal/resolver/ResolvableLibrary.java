/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.context.ResolvableCompilationUnit;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.scope.LibraryScope;
import com.google.dart.engine.source.Source;

import java.util.HashSet;

/**
 * Instances of the class {@code Library} represent the data about a single library during the
 * resolution of some (possibly different) library. They are not intended to be used except during
 * the resolution process.
 * 
 * @coverage dart.engine.resolver
 */
public class ResolvableLibrary {
  /**
   * The source specifying the defining compilation unit of this library.
   */
  private Source librarySource;

  /**
   * A list containing all of the libraries that are imported into this library.
   */
  private ResolvableLibrary[] importedLibraries = EMPTY_ARRAY;

  /**
   * A flag indicating whether this library explicitly imports core.
   */
  private boolean explicitlyImportsCore = false;

  /**
   * An array containing all of the libraries that are exported from this library.
   */
  private ResolvableLibrary[] exportedLibraries = EMPTY_ARRAY;

  /**
   * An array containing the compilation units that comprise this library. The defining compilation
   * unit is always first.
   */
  private ResolvableCompilationUnit[] compilationUnits;

  /**
   * The library element representing this library.
   */
  private LibraryElementImpl libraryElement;

  /**
   * The listener to which analysis errors will be reported.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The inheritance manager which is used for member lookups in this library.
   */
  private InheritanceManager inheritanceManager;

  /**
   * An empty array that can be used to initialize lists of libraries.
   */
  private static final ResolvableLibrary[] EMPTY_ARRAY = new ResolvableLibrary[0];

  /**
   * The library scope used when resolving elements within this library's compilation units.
   */
  private LibraryScope libraryScope;

  /**
   * Initialize a newly created data holder that can maintain the data associated with a library.
   * 
   * @param librarySource the source specifying the defining compilation unit of this library
   * @param errorListener the listener to which analysis errors will be reported
   */
  public ResolvableLibrary(Source librarySource) {
    this.librarySource = librarySource;
  }

  /**
   * Return the AST structure associated with the given source, or {@code null} if the source does
   * not represent a compilation unit that is included in this library.
   * 
   * @param source the source representing the compilation unit whose AST is to be returned
   * @return the AST structure associated with the given source
   * @throws AnalysisException if an AST structure could not be created for the compilation unit
   */
  public CompilationUnit getAST(Source source) {
    int count = compilationUnits.length;
    for (int i = 0; i < count; i++) {
      if (compilationUnits[i].getSource().equals(source)) {
        return compilationUnits[i].getCompilationUnit();
      }
    }
    return null;
  }

  /**
   * Return an array of the {@link CompilationUnit}s that make up the library. The first unit is
   * always the defining unit.
   * 
   * @return an array of the {@link CompilationUnit}s that make up the library. The first unit is
   *         always the defining unit
   */
  public CompilationUnit[] getCompilationUnits() throws AnalysisException {
    int count = compilationUnits.length;
    CompilationUnit[] units = new CompilationUnit[count];
    for (int i = 0; i < count; i++) {
      units[i] = compilationUnits[i].getCompilationUnit();
    }
    return units;
  }

  /**
   * Return an array containing the sources for the compilation units in this library, including the
   * defining compilation unit.
   * 
   * @return the sources for the compilation units in this library
   */
  public Source[] getCompilationUnitSources() {
    int count = compilationUnits.length;
    Source[] sources = new Source[count];
    for (int i = 0; i < count; i++) {
      sources[i] = compilationUnits[i].getSource();
    }
    return sources;
  }

  /**
   * Return the AST structure associated with the defining compilation unit for this library.
   * 
   * @return the AST structure associated with the defining compilation unit for this library
   * @throws AnalysisException if an AST structure could not be created for the defining compilation
   *           unit
   */
  public CompilationUnit getDefiningCompilationUnit() throws AnalysisException {
    return compilationUnits[0].getCompilationUnit();
  }

  /**
   * Return {@code true} if this library explicitly imports core.
   * 
   * @return {@code true} if this library explicitly imports core
   */
  public boolean getExplicitlyImportsCore() {
    return explicitlyImportsCore;
  }

  /**
   * Return an array containing the libraries that are exported from this library.
   * 
   * @return an array containing the libraries that are exported from this library
   */
  public ResolvableLibrary[] getExports() {
    return exportedLibraries;
  }

  /**
   * Return an array containing the libraries that are imported into this library.
   * 
   * @return an array containing the libraries that are imported into this library
   */
  public ResolvableLibrary[] getImports() {
    return importedLibraries;
  }

  /**
   * Return an array containing the libraries that are either imported or exported from this
   * library.
   * 
   * @return the libraries that are either imported or exported from this library
   */
  public ResolvableLibrary[] getImportsAndExports() {
    HashSet<ResolvableLibrary> libraries = new HashSet<ResolvableLibrary>(importedLibraries.length
        + exportedLibraries.length);
    for (ResolvableLibrary library : importedLibraries) {
      libraries.add(library);
    }
    for (ResolvableLibrary library : exportedLibraries) {
      libraries.add(library);
    }
    return libraries.toArray(new ResolvableLibrary[libraries.size()]);
  }

  /**
   * Return the inheritance manager for this library.
   * 
   * @return the inheritance manager for this library
   */
  public InheritanceManager getInheritanceManager() {
    if (inheritanceManager == null) {
      return inheritanceManager = new InheritanceManager(libraryElement);
    }
    return inheritanceManager;
  }

  /**
   * Return the library element representing this library, creating it if necessary.
   * 
   * @return the library element representing this library
   */
  public LibraryElementImpl getLibraryElement() {
    return libraryElement;
  }

  /**
   * Return the library scope used when resolving elements within this library's compilation units.
   * 
   * @return the library scope used when resolving elements within this library's compilation units
   */
  public LibraryScope getLibraryScope() {
    if (libraryScope == null) {
      libraryScope = new LibraryScope(libraryElement, errorListener);
    }
    return libraryScope;
  }

  /**
   * Return the source specifying the defining compilation unit of this library.
   * 
   * @return the source specifying the defining compilation unit of this library
   */
  public Source getLibrarySource() {
    return librarySource;
  }

  /**
   * Return the modification time associated with the given source.
   * 
   * @param source the source representing the compilation unit whose modification time is to be
   *          returned
   * @return the modification time associated with the given source
   * @throws AnalysisException if an AST structure could not be created for the compilation unit
   */
  public long getModificationTime(Source source) throws AnalysisException {
    int count = compilationUnits.length;
    for (int i = 0; i < count; i++) {
      if (source.equals(compilationUnits[i].getSource())) {
        return compilationUnits[i].getModificationTime();
      }
    }
    return -1L;
  }

  /**
   * Return an array containing the compilation units that comprise this library. The defining
   * compilation unit is always first.
   * 
   * @return the compilation units that comprise this library
   */
  public ResolvableCompilationUnit[] getResolvableCompilationUnits() {
    return compilationUnits;
  }

  /**
   * Set the compilation unit in this library to the given compilation units. The defining
   * compilation unit must be the first element of the array.
   * 
   * @param units the compilation units in this library
   */
  public void setResolvableCompilationUnits(ResolvableCompilationUnit[] units) {
    compilationUnits = units;
  }

  /**
   * Set the listener to which analysis errors will be reported to be the given listener.
   * 
   * @param errorListener the listener to which analysis errors will be reported
   */
  public void setErrorListener(AnalysisErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  /**
   * Set whether this library explicitly imports core to match the given value.
   * 
   * @param explicitlyImportsCore {@code true} if this library explicitly imports core
   */
  public void setExplicitlyImportsCore(boolean explicitlyImportsCore) {
    this.explicitlyImportsCore = explicitlyImportsCore;
  }

  /**
   * Set the libraries that are exported by this library to be those in the given array.
   * 
   * @param exportedLibraries the libraries that are exported by this library
   */
  public void setExportedLibraries(ResolvableLibrary[] exportedLibraries) {
    this.exportedLibraries = exportedLibraries;
  }

  /**
   * Set the libraries that are imported into this library to be those in the given array.
   * 
   * @param importedLibraries the libraries that are imported into this library
   */
  public void setImportedLibraries(ResolvableLibrary[] importedLibraries) {
    this.importedLibraries = importedLibraries;
  }

  /**
   * Set the library element representing this library to the given library element.
   * 
   * @param libraryElement the library element representing this library
   */
  public void setLibraryElement(LibraryElementImpl libraryElement) {
    this.libraryElement = libraryElement;
    if (inheritanceManager != null) {
      inheritanceManager.setLibraryElement(libraryElement);
    }
  }

  @Override
  public String toString() {
    return librarySource.getShortName();
  }
}
