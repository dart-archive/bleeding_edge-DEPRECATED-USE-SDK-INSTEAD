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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.scope.LibraryScope;
import com.google.dart.engine.source.Source;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Instances of the class {@code Library} represent the data about a single library during the
 * resolution of some (possibly different) library. They are not intended to be used except during
 * the resolution process.
 * 
 * @coverage dart.engine.resolver
 */
public class Library {
  /**
   * The analysis context in which this library is being analyzed.
   */
  private InternalAnalysisContext analysisContext;

  /**
   * The inheritance manager which is used for this member lookups in this library.
   */
  private InheritanceManager inheritanceManager;

  /**
   * The listener to which analysis errors will be reported.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The source specifying the defining compilation unit of this library.
   */
  private Source librarySource;

  /**
   * The library element representing this library.
   */
  private LibraryElementImpl libraryElement;

  /**
   * A list containing all of the libraries that are imported into this library.
   */
  private HashMap<ImportDirective, Library> importedLibraries = new HashMap<ImportDirective, Library>();

  /**
   * A table mapping URI-based directive to the actual URI value.
   */
  private HashMap<UriBasedDirective, String> directiveUris = new HashMap<UriBasedDirective, String>();

  /**
   * A flag indicating whether this library explicitly imports core.
   */
  private boolean explicitlyImportsCore = false;

  /**
   * A list containing all of the libraries that are exported from this library.
   */
  private HashMap<ExportDirective, Library> exportedLibraries = new HashMap<ExportDirective, Library>();

  /**
   * A table mapping the sources for the compilation units in this library to their corresponding
   * AST structures.
   */
  private HashMap<Source, CompilationUnit> astMap = new HashMap<Source, CompilationUnit>();

  /**
   * The library scope used when resolving elements within this library's compilation units.
   */
  private LibraryScope libraryScope;

  /**
   * Initialize a newly created data holder that can maintain the data associated with a library.
   * 
   * @param analysisContext the analysis context in which this library is being analyzed
   * @param errorListener the listener to which analysis errors will be reported
   * @param librarySource the source specifying the defining compilation unit of this library
   */
  public Library(InternalAnalysisContext analysisContext, AnalysisErrorListener errorListener,
      Source librarySource) {
    this.analysisContext = analysisContext;
    this.errorListener = errorListener;
    this.librarySource = librarySource;
    this.libraryElement = (LibraryElementImpl) analysisContext.getLibraryElement(librarySource);
  }

  /**
   * Record that the given library is exported from this library.
   * 
   * @param importLibrary the library that is exported from this library
   */
  public void addExport(ExportDirective directive, Library exportLibrary) {
    exportedLibraries.put(directive, exportLibrary);
  }

  /**
   * Record that the given library is imported into this library.
   * 
   * @param importLibrary the library that is imported into this library
   */
  public void addImport(ImportDirective directive, Library importLibrary) {
    importedLibraries.put(directive, importLibrary);
  }

  /**
   * Return the AST structure associated with the given source.
   * 
   * @param source the source representing the compilation unit whose AST is to be returned
   * @return the AST structure associated with the given source
   * @throws AnalysisException if an AST structure could not be created for the compilation unit
   */
  public CompilationUnit getAST(Source source) throws AnalysisException {
    CompilationUnit unit = astMap.get(source);
    if (unit == null) {
      unit = analysisContext.computeResolvableCompilationUnit(source);
      astMap.put(source, unit);
    }
    return unit;
  }

  /**
   * Return a collection containing the sources for the compilation units in this library, including
   * the defining compilation unit.
   * 
   * @return the sources for the compilation units in this library
   */
  public Set<Source> getCompilationUnitSources() {
    return astMap.keySet();
  }

  /**
   * Return the AST structure associated with the defining compilation unit for this library.
   * 
   * @return the AST structure associated with the defining compilation unit for this library
   * @throws AnalysisException if an AST structure could not be created for the defining compilation
   *           unit
   */
  public CompilationUnit getDefiningCompilationUnit() throws AnalysisException {
    return getAST(getLibrarySource());
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
   * Return the library exported by the given directive.
   * 
   * @param directive the directive that exports the library to be returned
   * @return the library exported by the given directive
   */
  public Library getExport(ExportDirective directive) {
    return exportedLibraries.get(directive);
  }

  /**
   * Return an array containing the libraries that are exported from this library.
   * 
   * @return an array containing the libraries that are exported from this library
   */
  public Library[] getExports() {
    HashSet<Library> libraries = new HashSet<Library>();
    libraries.addAll(exportedLibraries.values());
    return libraries.toArray(new Library[libraries.size()]);
  }

  /**
   * Return the library imported by the given directive.
   * 
   * @param directive the directive that imports the library to be returned
   * @return the library imported by the given directive
   */
  public Library getImport(ImportDirective directive) {
    return importedLibraries.get(directive);
  }

  /**
   * Return an array containing the libraries that are imported into this library.
   * 
   * @return an array containing the libraries that are imported into this library
   */
  public Library[] getImports() {
    HashSet<Library> libraries = new HashSet<Library>();
    libraries.addAll(importedLibraries.values());
    return libraries.toArray(new Library[libraries.size()]);
  }

  /**
   * Return an array containing the libraries that are either imported or exported from this
   * library.
   * 
   * @return the libraries that are either imported or exported from this library
   */
  public Library[] getImportsAndExports() {
    HashSet<Library> libraries = new HashSet<Library>(importedLibraries.size()
        + exportedLibraries.size());
    libraries.addAll(importedLibraries.values());
    libraries.addAll(exportedLibraries.values());
    return libraries.toArray(new Library[libraries.size()]);
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
    if (libraryElement == null) {
      try {
        libraryElement = (LibraryElementImpl) analysisContext.computeLibraryElement(librarySource);
      } catch (AnalysisException exception) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not compute ilbrary element for " + librarySource.getFullName(),
            exception);
      }
    }
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
   * Return the result of resolving the URI of the given URI-based directive against the URI of the
   * library, or {@code null} if the URI is not valid. If the URI is not valid, report the error.
   * 
   * @param directive the directive which URI should be resolved
   * @return the result of resolving the URI against the URI of the library
   */
  public Source getSource(UriBasedDirective directive) {
    StringLiteral uriLiteral = directive.getUri();
    if (uriLiteral instanceof StringInterpolation) {
      errorListener.onError(new AnalysisError(
          librarySource,
          uriLiteral.getOffset(),
          uriLiteral.getLength(),
          CompileTimeErrorCode.URI_WITH_INTERPOLATION));
      return null;
    }
    String uriContent = uriLiteral.getStringValue().trim();
    directiveUris.put(directive, uriContent);
    try {
      new URI(uriContent);
      Source source = getSource(uriContent);
      if (source == null || !source.exists()) {
        errorListener.onError(new AnalysisError(
            librarySource,
            uriLiteral.getOffset(),
            uriLiteral.getLength(),
            CompileTimeErrorCode.URI_DOES_NOT_EXIST,
            uriContent));
      }
      return source;
    } catch (URISyntaxException exception) {
      errorListener.onError(new AnalysisError(
          librarySource,
          uriLiteral.getOffset(),
          uriLiteral.getLength(),
          CompileTimeErrorCode.INVALID_URI,
          uriContent));
    }
    return null;
  }

  /**
   * Returns the URI value of the given directive.
   */
  public String getUri(UriBasedDirective directive) {
    return directiveUris.get(directive);
  }

  /**
   * Set the AST structure associated with the defining compilation unit for this library to the
   * given AST structure.
   * 
   * @param unit the AST structure associated with the defining compilation unit for this library
   */
  public void setDefiningCompilationUnit(CompilationUnit unit) {
    astMap.put(getLibrarySource(), unit);
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

  /**
   * Return the result of resolving the given URI against the URI of the library, or {@code null} if
   * the URI is not valid.
   * 
   * @param uri the URI to be resolved
   * @return the result of resolving the given URI against the URI of the library
   */
  private Source getSource(String uri) {
    if (uri == null) {
      return null;
    }
    return analysisContext.getSourceFactory().resolveUri(librarySource, uri);
  }
}
