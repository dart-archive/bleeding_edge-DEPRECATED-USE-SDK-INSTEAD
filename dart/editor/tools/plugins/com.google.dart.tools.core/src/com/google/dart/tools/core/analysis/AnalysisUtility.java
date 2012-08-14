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

import com.google.dart.compiler.CompilerConfiguration;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompiler.SelectiveCache;
import com.google.dart.compiler.DartCompilerErrorCode;
import com.google.dart.compiler.DartCompilerListener;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.DefaultCompilerConfiguration;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.compiler.parser.DartParser;
import com.google.dart.compiler.parser.DartPrefixParser;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.CachingArtifactProvider;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.utilities.io.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Static utility methods
 */
class AnalysisUtility {
  private static final CompilerConfiguration config = new DefaultCompilerConfiguration(
      DartCompilerUtilities.COMPILER_OPTIONS,
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
   * Parse a single file.
   * 
   * @param relPath the path to the file to be parsed relative to the library containing that file.
   *          This path should contain '/' rather than {@link File#separatorChar}
   * @param prefixes the collection of import prefixes. If the file being parsed contains import
   *          directives, then this collection will be updated to include any specified prefixes
   * @param listener the analysis listener (not <code>null</code>)
   */
  static DartUnit parse(File sourceFile, DartSource source, Set<String> prefixes,
      DartCompilerListener listener) {
    String sourceCode = null;
    try {
      sourceCode = FileUtilities.getDartContents(sourceFile);
    } catch (IOException e) {
      listener.onError(newIoError(source, e));
    }
    DartUnit dartUnit = null;
    if (sourceCode != null) {
      try {
        DartParser parser = new DartPrefixParser(
            source,
            sourceCode,
            false,
            prefixes,
            listener,
            null);
        dartUnit = DartCompilerUtilities.secureParseUnit(parser, source);
      } catch (Throwable e) {
        DartCore.logError("Exception while parsing " + sourceFile.getPath(), e);
        listener.onError(newParseFailure(source, e));
      }
    }
    return dartUnit != null ? dartUnit : new DartUnit(source, false);
  }

  /**
   * Resolve the specified library and any imported libraries that have not already been resolved.
   * 
   * @param selectiveCache Provides cached parse and resolution results during selective compilation
   *          or <code>null</code> if no cache available
   * @return a map of newly resolved libraries
   */
  static Map<URI, LibraryUnit> resolve(File libraryFile, LibrarySource librarySource,
      SelectiveCache selectiveCache, DartCompilerListener errorListener) {

    provider.clearCachedArtifacts();
    Map<URI, LibraryUnit> newlyResolved = null;
    try {
      newlyResolved = DartCompilerUtilities.secureAnalyzeLibraries(
          librarySource,
          selectiveCache,
          config,
          provider,
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
    if (SystemLibraryManager.isDartUri(uri) || SystemLibraryManager.isPackageUri(uri)) {
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

  /**
   * Answer the {@link LibrarySource} for the specified library file
   * 
   * @return the library source (not <code>null</code>)
   */
  static UrlLibrarySource toLibrarySource(AnalysisServer server, File libraryFile) {
    URI libUri = toLibraryUri(server, libraryFile);
    return new UrlLibrarySource(libUri, server.getLibraryManager());
  }

  /**
   * Answer the "file:" or "dart:" URI for the specified library file
   * 
   * @return the library URI (not <code>null</code>)
   */
  static URI toLibraryUri(AnalysisServer server, File libraryFile) {
    URI fileUri = libraryFile.toURI();
    URI shortUri = server.getLibraryManager().getRelativeUri(fileUri);
    return shortUri != null && !SystemLibraryManager.isPackageUri(shortUri) ? shortUri : fileUri;
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
}
