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
package com.google.dart.command.analyze;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.error.ErrorType;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.ExplicitPackageUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.UriKind;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans, parses, and analyzes a library.
 */
class AnalyzerImpl {
  /**
   * The maximum number of sources for which AST structures should be kept in the cache.
   */
  private static final int MAX_CACHE_SIZE = 256;

  private static final HashMap<File, DirectoryBasedDartSdk> sdkMap = new HashMap<File, DirectoryBasedDartSdk>();

  private static ErrorSeverity getMaxErrorSeverity(List<AnalysisError> errors) {
    ErrorSeverity status = ErrorSeverity.NONE;

    for (AnalysisError error : errors) {
      ErrorSeverity severity = error.getErrorCode().getErrorSeverity();

      status = status.max(severity);
    }

    return status;
  }

  /**
   * @return the new or cached instance of the {@link DartSdk} with the given directory.
   */
  private static DirectoryBasedDartSdk getSdk(File sdkDirectory) {
    DirectoryBasedDartSdk sdk = sdkMap.get(sdkDirectory);
    if (sdk == null) {
      sdk = new DirectoryBasedDartSdk(sdkDirectory);
      sdkMap.put(sdkDirectory, sdk);
    }
    return sdk;
  }

  private AnalyzerOptions options;
  private DirectoryBasedDartSdk sdk;

  public AnalyzerImpl(AnalyzerOptions options) {
    this.options = options;
    this.sdk = getSdk(options.getDartSdkPath());
  }

  /**
   * Treats the {@code sourceFile} as the top level library and analyzes the unit for warnings and
   * errors.
   * 
   * @param sourceFile file to analyze
   * @param options configuration for this analysis pass
   * @param errors the list to add errors to
   * @return {@code  true} on success, {@code false} on failure.
   */
  public ErrorSeverity analyze(File sourceFile, List<AnalysisError> errors) throws IOException,
      AnalysisException {
    if (sourceFile == null) {
      throw new IllegalArgumentException("sourceFile cannot be null");
    }

    // prepare "packages" directory
    File packageDirectory;
    if (options.getPackageRootPath() != null) {
      packageDirectory = options.getPackageRootPath();
    } else {
      packageDirectory = getPackageDirectoryFor(sourceFile);
    }

    // create SourceFactory
    SourceFactory sourceFactory;

    if (options.getUsePackageMap()) {
      sourceFactory = new SourceFactory(
          new DartUriResolver(sdk),
          new FileUriResolver(),
          new ExplicitPackageUriResolver(sdk, getPubDir(sourceFile)));
    } else if (packageDirectory != null) {
      sourceFactory = new SourceFactory(
          new DartUriResolver(sdk),
          new FileUriResolver(),
          new PackageUriResolver(packageDirectory.getAbsoluteFile()));
    } else {
      sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    }

    // create options for context
    AnalysisOptionsImpl contextOptions = new AnalysisOptionsImpl();
    contextOptions.setCacheSize(MAX_CACHE_SIZE);
    contextOptions.setHint(!options.getDisableHints());

    // prepare AnalysisContext
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    context.setSourceFactory(sourceFactory);
    context.setAnalysisOptions(contextOptions);

    // prepare Source
    sourceFile = sourceFile.getAbsoluteFile();
    UriKind uriKind = getUriKind(sourceFile);
    Source librarySource = new FileBasedSource(sourceFactory.getContentCache(), sourceFile, uriKind);

    // don't try to analyzer parts
    CompilationUnit unit = context.parseCompilationUnit(librarySource);
    boolean hasLibraryDirective = false;
    boolean hasPartOfDirective = false;
    for (Directive directive : unit.getDirectives()) {
      hasLibraryDirective |= directive instanceof LibraryDirective;
      hasPartOfDirective |= directive instanceof PartOfDirective;
    }
    if (hasPartOfDirective && !hasLibraryDirective) {
      System.err.println("Only libraries can be analyzed.");
      System.err.println(sourceFile + " is a part and can not be analyzed.");
      return ErrorSeverity.NONE;
    }

    // analyze Source
    LibraryElement library = context.computeLibraryElement(librarySource);
    context.resolveCompilationUnit(librarySource, library);

    // prepare errors
    Set<Source> sources = getAllSources(library);
    getAllErrors(context, sources, errors);
    filterOutTodos(errors);
    return getMaxErrorSeverity(errors);
  }

  Set<Source> getAllSources(LibraryElement library) {
    Set<CompilationUnitElement> units = new HashSet<CompilationUnitElement>();
    Set<LibraryElement> libraries = new HashSet<LibraryElement>();
    Set<Source> sources = new HashSet<Source>();

    addLibrary(library, libraries, units, sources);

    return sources;
  }

  private void addCompilationUnit(CompilationUnitElement unit, Set<LibraryElement> libraries,
      Set<CompilationUnitElement> units, Set<Source> sources) {
    if (unit == null || units.contains(unit)) {
      return;
    }

    units.add(unit);

    sources.add(unit.getSource());
  }

  private void addLibrary(LibraryElement library, Set<LibraryElement> libraries,
      Set<CompilationUnitElement> units, Set<Source> sources) {
    if (library == null || libraries.contains(library)) {
      return;
    }

    UriKind uriKind = library.getSource().getUriKind();

    // Optionally skip package: libraries.
    if (!options.getShowPackageWarnings() && uriKind == UriKind.PACKAGE_URI) {
      return;
    }

    // Optionally skip SDK libraries.
    if (!options.getShowSdkWarnings() && uriKind == UriKind.DART_URI) {
      return;
    }

    libraries.add(library);

    // add compilation units
    addCompilationUnit(library.getDefiningCompilationUnit(), libraries, units, sources);

    for (CompilationUnitElement child : library.getParts()) {
      addCompilationUnit(child, libraries, units, sources);
    }

    // add referenced libraries
    for (LibraryElement child : library.getImportedLibraries()) {
      addLibrary(child, libraries, units, sources);
    }

    for (LibraryElement child : library.getExportedLibraries()) {
      addLibrary(child, libraries, units, sources);
    }
  }

  /**
   * Remove any todo error (ErrorSeverity.INFO severity and ErrorType.TODO type) from the passed
   * list.
   */
  private void filterOutTodos(List<AnalysisError> errors) {
    for (int i = errors.size() - 1; i >= 0; i--) {
      AnalysisError error = errors.get(i);

      if (error.getErrorCode().getType() == ErrorType.TODO) {
        errors.remove(i);
      }
    }
  }

  private void getAllErrors(AnalysisContext context, Set<Source> sources, List<AnalysisError> errors)
      throws AnalysisException {
    for (Source source : sources) {
      AnalysisError[] sourceErrors = context.computeErrors(source);
      errors.addAll(Arrays.asList(sourceErrors));
    }
  }

  private File getPackageDirectoryFor(File sourceFile) {
    // we are going to ask parent file, so get absolute path
    sourceFile = sourceFile.getAbsoluteFile();

    // look in the containing directories
    File dir = sourceFile.getParentFile();
    while (dir != null) {
      File packagesDir = new File(dir, "packages");
      if (packagesDir.exists()) {
        return packagesDir;
      }
      dir = dir.getParentFile();
    }

    return null;
  }

  /**
   * Return a directory containing a pubspec.yaml file. The search location starts at the parent of
   * the given source file and continues up the tree. If no pub directory is found, return the cwd.
   * 
   * @param sourceFile the starting location
   * @return a directory containing a pubspec.yaml file, or the cwd if no such directory is found
   */
  private File getPubDir(File sourceFile) {
    // we are going to ask parent file, so get absolute path
    sourceFile = sourceFile.getAbsoluteFile();

    // look in the containing directories
    File dir = sourceFile.getParentFile();

    while (dir != null) {
      File pubspecFile = new File(dir, "pubspec.yaml");

      if (pubspecFile.exists()) {
        return dir;
      }

      dir = dir.getParentFile();
    }

    // Else, return the cwd.
    return new File(System.getProperty("user.dir"));
  }

  /**
   * Returns the {@link UriKind} for the given input file. Usually {@link UriKind#FILE_URI}, but if
   * the given file is located in the "lib" directory of the {@link #sdk}, then returns
   * {@link UriKind#DART_URI}.
   */
  private UriKind getUriKind(File file) {
    // may be file in SDK
    if (sdk instanceof DirectoryBasedDartSdk) {
      DirectoryBasedDartSdk directoryBasedSdk = sdk;
      String sdkLibPath = directoryBasedSdk.getLibraryDirectory().getPath() + File.separator;
      if (file.getPath().startsWith(sdkLibPath)) {
        return UriKind.DART_URI;
      }
    }
    // some generic file
    return UriKind.FILE_URI;
  }

}
