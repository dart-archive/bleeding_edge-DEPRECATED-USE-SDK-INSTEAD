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
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartUnit;

import static com.google.dart.tools.core.analysis.AnalysisUtility.parse;

import java.io.File;
import java.net.URI;
import java.util.HashMap;

/**
 * Parse a library source file and create the associated {@link Library}
 */
class ParseLibraryFileTask extends Task {
  private static final String DART_CORE = "dart:core";

  private final AnalysisServer server;
  private final Context context;
  private final File libraryFile;
  private final UrlLibrarySource librarySource;

  ParseLibraryFileTask(AnalysisServer server, Context context, File libraryFile) {
    this.server = server;
    this.context = context;
    this.libraryFile = libraryFile;
    this.librarySource = new UrlLibrarySource(libraryFile);
  }

  @Override
  void perform() {
    if (!libraryFile.exists()) {
      return;
    }
    DartUnit unit = parse(server, libraryFile, librarySource, libraryFile);

    HashMap<String, File> imports = new HashMap<String, File>();
    HashMap<String, File> sources = new HashMap<String, File>();
    URI base = libraryFile.getParentFile().toURI();

    // Resolve all #import and #source directives

    for (DartDirective directive : unit.getDirectives()) {
      String relPath;
      if (directive instanceof DartImportDirective) {
        relPath = ((DartImportDirective) directive).getLibraryUri().getValue();
        File file = server.resolveImport(base, relPath);
        if (file == null) {
          // Resolution errors reported by ResolveLibraryTask
        } else {
          imports.put(relPath, file);
        }
      } else if (directive instanceof DartSourceDirective) {
        relPath = ((DartSourceDirective) directive).getSourceUri().getValue();
        File file = server.resolveFile(base, relPath);
        if (file == null) {
          // Resolution errors reported by ResolveLibraryTask
        } else {
          sources.put(relPath, file);
        }
      }
    }

    // Import "dart:core" if it was not explicitly imported

    if (imports.get(DART_CORE) == null) {
      File file = server.resolveImport(base, DART_CORE);
      if (file == null) {
        // Resolution errors reported by ResolveLibraryTask
      } else {
        imports.put(DART_CORE, file);
      }
    }

    context.cacheLibrary(new Library(libraryFile, librarySource, unit, imports, sources));
  }
}
