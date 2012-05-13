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

import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.core.analysis.AnalysisUtility.parse;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parse a library source file and create the associated {@link Library}
 */
class ParseLibraryFileTask extends Task {
  private final AnalysisServer server;
  private final Context context;
  private final File libraryFile;
  private final UrlLibrarySource librarySource;
  private final ParseLibraryFileCallback callback;

  ParseLibraryFileTask(AnalysisServer server, Context context, File libraryFile,
      ParseLibraryFileCallback callback) {
    this.server = server;
    this.context = context;
    this.libraryFile = libraryFile;
    this.callback = callback;
    URI fileUri = libraryFile.toURI();
    URI shortUri = server.getLibraryManager().getRelativeUri(fileUri);
    URI libUri = shortUri != null ? shortUri : fileUri;
    this.librarySource = new UrlLibrarySource(libUri, server.getLibraryManager());
  }

  @Override
  boolean isBackgroundAnalysis() {
    return callback == null;
  }

  @Override
  void perform() {
    Library library = context.getCachedLibrary(libraryFile);

    // Get the cached unit or parse the source

    DartUnit unit = null;
    if (library != null) {
      unit = library.getResolvedUnit(libraryFile);
    }
    if (unit == null) {
      unit = context.getUnresolvedUnit(libraryFile);
      if (unit == null) {
        Set<String> prefixes = new HashSet<String>();
        unit = parse(server, libraryFile, librarySource, libraryFile.getName(), prefixes);
        context.cacheUnresolvedUnit(libraryFile, unit);
      }
    }

    // Ensure the library is built

    if (library == null) {
      List<DartDirective> directives = unit.getDirectives();
      library = Library.fromDartUnit(server, libraryFile, librarySource, directives);
      context.cacheLibrary(library);
    }

    // Notify the caller

    if (callback != null) {
      try {
        callback.parsed(new ParseLibraryFileEvent(library, unit));
      } catch (Throwable e) {
        DartCore.logError("Exception during parse notification", e);
      }
    }
  }
}
