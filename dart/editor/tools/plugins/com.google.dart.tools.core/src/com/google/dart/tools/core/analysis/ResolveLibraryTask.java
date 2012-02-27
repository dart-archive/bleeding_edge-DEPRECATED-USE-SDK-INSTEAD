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

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;

import static com.google.dart.tools.core.analysis.AnalysisUtility.resolve;

import java.io.File;
import java.net.URI;
import java.util.HashMap;

/**
 * Resolve types and references in the specified library
 */
class ResolveLibraryTask extends Task {
  private final AnalysisServer server;
  private final Library library;
  private HashMap<URI, DartUnit> parsedUnits;
  private LibraryUnit unit;

  ResolveLibraryTask(AnalysisServer server, Context context, Library library) {
    this.server = server;
    this.library = library;
    this.parsedUnits = new HashMap<URI, DartUnit>();
    // TODO (danrubel) revise DartCompiler API to pass in resolved imported library units
//    for (File importedFile : library.getImportedFiles()) {
//      Library importedLibrary = context.getCachedLibrary(importedFile);
//      for (File sourceFile : importedLibrary.getSourceFiles()) {
//        parsedUnits.put(sourceFile.toURI(), importedLibrary.getCachedUnit(sourceFile));
//      }
//    }
    for (File sourceFile : library.getSourceFiles()) {
      parsedUnits.put(sourceFile.toURI(), library.getCachedUnit(sourceFile));
    }
  }

  @Override
  void perform() {
    if (library.getLibraryUnit() != null) {
      return;
    }
    unit = resolve(server, library, parsedUnits);
    library.cacheLibraryUnit(unit);
  }
}
