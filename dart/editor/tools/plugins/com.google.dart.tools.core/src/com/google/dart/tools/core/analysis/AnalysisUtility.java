/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.CommandLineOptions.CompilerOptions;
import com.google.dart.compiler.CompilerConfiguration;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompilerErrorCode;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.DefaultCompilerConfiguration;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.compiler.parser.DartParser;
import com.google.dart.compiler.parser.DartPrefixParser;
import com.google.dart.compiler.parser.DartScannerParserContext;
import com.google.dart.compiler.parser.ParserContext;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.CachingArtifactProvider;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.utilities.io.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Static utility methods
 */
class AnalysisUtility {
  private static final CompilerConfiguration config = new DefaultCompilerConfiguration(
      new CompilerOptions(),
      SystemLibraryManagerProvider.getSystemLibraryManager()) {
    @Override
    public boolean incremental() {
      return false;
    }

    @Override
    public boolean resolveDespiteParseErrors() {
      return true;
    }
  };

  private static final CachingArtifactProvider provider = new CachingArtifactProvider() {
  };

  /**
   * Parse a single file and report the errors/warnings.
   * 
   * @param relPath the path to the file to be parsed relative to the library containing that file.
   *          This path should contain '/' rather than {@link File#separatorChar}
   * @param prefixes the collection of import prefixes. If the file being parsed contains import
   *          directives, then this collection will be updated to include any specified prefixes
   */
  static DartUnit parse(AnalysisServer server, File libraryFile, LibrarySource librarySource,
      String relPath, Set<String> prefixes) {
    ErrorListener errorListener = new ErrorListener(server);
    DartSource source = librarySource.getSourceFor(relPath);
    File sourceFile = new File(libraryFile.toURI().resolve(relPath));

    String sourceCode = null;
    try {
      sourceCode = FileUtilities.getContents(sourceFile);
    } catch (IOException e) {
      errorListener.onError(newIoError(source, e));
    }

    DartUnit dartUnit = null;
    if (sourceCode != null) {
      try {
        ParserContext parserCtx = new DartScannerParserContext(source, sourceCode, errorListener);
        DartParser parser = new DartPrefixParser(parserCtx, false, prefixes);
        dartUnit = DartCompilerUtilities.secureParseUnit(parser, source);
      } catch (Throwable e) {
        DartCore.logError("Exception while parsing " + sourceFile.getPath(), e);
        errorListener.onError(newParseFailure(source, e));
      }
    }

    errorListener.notifyParsed(libraryFile, sourceFile, dartUnit);
    return dartUnit != null ? dartUnit : new DartUnit(source, false);
  }

  /**
   * Resolve the specified library and any imported libraries that have not already been resolved.
   * 
   * @param parsedUnits A collection of unresolved ASTs that should be used instead of parsing the
   *          associated source from storage. Units are removed from this map as they are used.
   * @return a map of newly resolved libraries
   */
  static Map<URI, LibraryUnit> resolve(AnalysisServer server, Library library,
      Map<URI, LibraryUnit> resolvedLibs, Map<URI, DartUnit> parsedUnits) {
    ErrorListener errorListener = new ErrorListener(server);

    File libraryFile = library.getFile();
    LibrarySource librarySource = library.getLibrarySource();
    provider.clearCachedArtifacts();

    // analyzeLibraries modifies map of parsed units,
    // thus we copy the map to know which units were already parsed
    // before calling analyzeLibraries
    HashMap<URI, DartUnit> parsedUnitsCopy = new HashMap<URI, DartUnit>(parsedUnits);

    Map<URI, LibraryUnit> newlyResolved = null;
    try {

      newlyResolved = DartCompilerUtilities.secureAnalyzeLibraries(
          librarySource,
          resolvedLibs,
          parsedUnits,
          config,
          provider,
          server.getLibraryManager(),
          errorListener,
          true);
    } catch (IOException e) {
      errorListener.onError(newIoError(librarySource, e));
    } catch (Throwable e) {
      DartCore.logError("Exception while resolving " + libraryFile.getPath(), e);
      DartCompilationError error = new DartCompilationError(
          librarySource,
          AnalysisErrorCode.RESOLUTION_FAILURE,
          e.getMessage());
      error.setSource(librarySource);
      errorListener.onError(error);
    }

    if (newlyResolved == null) {
      newlyResolved = new HashMap<URI, LibraryUnit>();
      newlyResolved.put(libraryFile.toURI(), new LibraryUnit(librarySource));
    }
    notifyParsedDuringResolve(server, parsedUnitsCopy, newlyResolved.values(), errorListener);
    errorListener.notifyResolved(newlyResolved);
    return newlyResolved;
  }

  /**
   * Answer the absolute file for the specified URI
   * 
   * @return the file or <code>null</code> if unknown
   */
  static File toFile(AnalysisServer server, URI uri) {
    String scheme = uri.getScheme();
    if (scheme == null || "file".equals(scheme)) {
      File file = new File(uri.getPath());
      if (file.isAbsolute()) {
        return file;
      }
      DartCore.logError("Non absolute path: " + file);
      return null;
    }
    if (SystemLibraryManager.isDartUri(uri)) {
      URI resolveUri = server.getLibraryManager().resolveDartUri(uri);
      if (resolveUri == null) {
        DartCore.logError("Failed to resolve: " + uri);
        return null;
      }
      return new File(resolveUri.getPath());
    }
    DartCore.logError("Unknown library scheme : " + uri);
    return null;
  }

  private static DartCompilationError newIoError(Source source, IOException e) {
    DartCompilationError event = new DartCompilationError(
        source,
        DartCompilerErrorCode.IO,
        e.getMessage());
    event.setSource(source);
    return event;
  }

  private static DartCompilationError newParseFailure(DartSource source, Throwable e) {
    DartCompilationError error = new DartCompilationError(
        source,
        AnalysisErrorCode.PARSE_FAILURE,
        e.getMessage());
    error.setSource(source);
    return error;
  }

  /**
   * Notify listeners of source files that were parsed during resolution
   * 
   * @param parsedUnits the units that were parsed prior to the resolve (not <code>null</code>)
   * @param newlyResolved the newly resolved library units (not <code>null</code>, contains no
   *          <code>null</code>s)
   * @param errorListener the error listener used during resolution (not <code>null</code>)
   */
  private static void notifyParsedDuringResolve(AnalysisServer server,
      Map<URI, DartUnit> parsedUnits, Collection<LibraryUnit> newlyResolved,
      ErrorListener errorListener) {
    for (LibraryUnit libUnit : newlyResolved) {
      AnalysisEvent event = null;
      Iterator<DartUnit> iter = libUnit.getUnits().iterator();
      while (iter.hasNext()) {
        DartUnit dartUnit = iter.next();
        File dartFile = toFile(server, dartUnit.getSourceInfo().getSource().getUri());
        if (parsedUnits.get(dartFile.toURI()) == null) {
          if (event == null) {
            event = new AnalysisEvent(toFile(server, libUnit.getSource().getUri()));
          }
          event.addFileAndDartUnit(dartFile, dartUnit);
        }
      }
      if (event != null) {
        errorListener.notifyParsed(event);
      }
    }
  }
}
