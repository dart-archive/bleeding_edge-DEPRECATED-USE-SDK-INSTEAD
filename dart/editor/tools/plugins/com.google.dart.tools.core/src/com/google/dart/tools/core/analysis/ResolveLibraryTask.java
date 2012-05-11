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

import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;

import static com.google.dart.tools.core.analysis.AnalysisUtility.resolve;
import static com.google.dart.tools.core.analysis.AnalysisUtility.toFile;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolve types and references in the specified library
 */
class ResolveLibraryTask extends Task {

  private final AnalysisServer server;
  private final Context context;
  private final Library library;

  ResolveLibraryTask(AnalysisServer server, Context context, Library library) {
    this.server = server;
    this.context = context;
    this.library = library;
  }

  @Override
  boolean isBackgroundAnalysis() {
    return true;
  }

  @Override
  void perform() {
    if (library.getLibraryUnit() != null) {
      return;
    }

    // Collect resolved libraries and parsed units

    HashMap<URI, LibraryUnit> resolvedLibs = new HashMap<URI, LibraryUnit>();
    HashMap<URI, DartUnit> parsedUnits = context.getUnresolvedUnits();

    for (Library lib : context.getCachedLibraries()) {
      LibraryUnit libUnit = lib.getLibraryUnit();
      if (libUnit != null) {
        resolvedLibs.put(libUnit.getSource().getUri(), libUnit);
      }
    }

    // Resolve

    Map<URI, LibraryUnit> newlyResolved = resolve(server, library, resolvedLibs, parsedUnits);

    // Cache the resolved libraries

    for (LibraryUnit libUnit : newlyResolved.values()) {
      File libFile = toFile(server, libUnit.getSource().getUri());
      if (libFile == null) {
        continue;
      }
      Library lib = context.getCachedLibrary(libFile);
      if (lib == null) {
        LibrarySource librarySource = libUnit.getSource();
        List<DartDirective> directives = libUnit.getSelfDartUnit().getDirectives();
        lib = Library.fromDartUnit(server, libFile, librarySource, directives);
        context.cacheLibrary(lib);
      }
      lib.cacheLibraryUnit(server, libUnit);
    }
  }
}
