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

import com.google.dart.compiler.Backend;
import com.google.dart.compiler.CommandLineOptions.CompilerOptions;
import com.google.dart.compiler.CompilerConfiguration;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompiler;
import com.google.dart.compiler.DartCompilerErrorCode;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.DefaultCompilerConfiguration;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.UrlDartSource;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.compiler.parser.DartParser;
import com.google.dart.indexer.utilities.io.FileUtilities;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.CachingArtifactProvider;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

/**
 * Static utility methods
 */
class AnalysisUtility {
  private static final CompilerConfiguration config = new DefaultCompilerConfiguration(
      new CompilerOptions(), SystemLibraryManagerProvider.getSystemLibraryManager(),
      new Backend[] {}) {

    @Override
    public boolean checkOnly() {
      return true;
    }

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
   * Parse a single file and report the errors/warnings
   */
  static DartUnit parse(AnalysisServer server, File libraryFile, UrlLibrarySource librarySource,
      File sourceFile) {
    AnalysisEvent event = new AnalysisEvent(libraryFile);
    event.addFile(sourceFile);
    DartSource source = new UrlDartSource(sourceFile, librarySource);

    String sourceCode = null;
    try {
      sourceCode = FileUtilities.getContents(sourceFile);
    } catch (IOException e) {
      event.addError(newIoError(source, e));
    }

    DartUnit unit = null;
    if (sourceCode != null) {
      try {
        ErrorListener errorListener = new ErrorListener(event);
        DartParser parser = new DartParser(source, sourceCode, errorListener);
        unit = parser.parseUnit(source);
        event.addUnit(sourceFile, unit);
      } catch (Throwable e) {
        DartCore.logError("Exception while parsing " + sourceFile.getPath(), e);
        event.addError(newParseFailure(source, e));
      }
    }

    for (AnalysisListener listener : server.getAnalysisListeners()) {
      listener.parsed(event);
    }
    return unit != null ? unit : new DartUnit(source, false);
  }

  /**
   * Resolve references in the specified library
   */
  static LibraryUnit resolve(AnalysisServer server, Library library, HashMap<URI, DartUnit> units) {
    File libraryFile = library.getFile();
    UrlLibrarySource librarySource = library.getLibrarySource();
    AnalysisEvent event = new AnalysisEvent(libraryFile);
    event.addFile(libraryFile);
    event.addFiles(library.getSourceFiles());
    provider.clearCachedArtifacts();

    LibraryUnit libUnit = null;
    try {
      ErrorListener errorListener = new ErrorListener(event);
      libUnit = DartCompiler.analyzeLibrary(librarySource, units, config, provider, errorListener);
      event.addUnits(library.getCachedUnits());
    } catch (IOException e) {
      event.addError(newIoError(librarySource, e));
    } catch (Throwable e) {
      DartCore.logError("Exception while resolving " + libraryFile.getPath(), e);
      DartCompilationError error = new DartCompilationError(librarySource,
          AnalysisErrorCode.RESOLUTION_FAILURE, e.getMessage());
      error.setSource(librarySource);
      event.addError(error);
    }

    for (AnalysisListener listener : server.getAnalysisListeners()) {
      listener.resolved(event);
    }
    return libUnit != null ? libUnit : new LibraryUnit(librarySource);
  }

  private static DartCompilationError newIoError(Source source, IOException e) {
    DartCompilationError event = new DartCompilationError(source, DartCompilerErrorCode.IO,
        e.getMessage());
    event.setSource(source);
    return event;
  }

  private static DartCompilationError newParseFailure(DartSource source, Throwable e) {
    DartCompilationError error = new DartCompilationError(source, AnalysisErrorCode.PARSE_FAILURE,
        e.getMessage());
    error.setSource(source);
    return error;
  }
}
