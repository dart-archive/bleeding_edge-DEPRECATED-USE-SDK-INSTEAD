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
import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.core.analysis.AnalysisUtility.resolve;
import static com.google.dart.tools.core.analysis.AnalysisUtility.toFile;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
  boolean isPriority() {
    return false;
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
    ErrorListener errorListener = new ErrorListener(server);

    // Calling #resolve(...) modifies map of parsed units,
    // thus we copy the map to know which units were already parsed before calling resolve

    HashSet<URI> parsedUnitURIs = new HashSet<URI>(parsedUnits.keySet());

    // Resolve

    Map<URI, LibraryUnit> newlyResolved = resolve(
        server.getLibraryManager(),
        library.getFile(),
        library.getLibrarySource(),
        resolvedLibs,
        parsedUnits,
        errorListener);

    for (LibraryUnit libUnit : newlyResolved.values()) {

      // Cache the resolved libraries

      LibrarySource librarySource = libUnit.getSource();
      File libraryFile = toFile(server, librarySource.getUri());
      if (libraryFile == null) {
        continue;
      }
      Library library = context.getCachedLibrary(libraryFile);
      if (library == null) {
        List<DartDirective> directives = libUnit.getSelfDartUnit().getDirectives();
        library = Library.fromDartUnit(server, libraryFile, librarySource, directives);
        context.cacheLibrary(library);
      }
      library.cacheLibraryUnit(server, libUnit);

      // If this is a library whose state is being reloaded, then skip notifications

      if (!library.shouldNotify) {
        continue;
      }

      // Notify listeners about units that were parsed during the resolution process

      AnalysisEvent event = null;
      Iterator<DartUnit> iter = libUnit.getUnits().iterator();
      while (iter.hasNext()) {
        DartUnit dartUnit = iter.next();
        URI dartUnitUri = dartUnit.getSourceInfo().getSource().getUri();
        File dartFile = toFile(server, dartUnitUri);
        if (!parsedUnitURIs.contains(dartFile.toURI())) {
          if (event == null) {
            event = new AnalysisEvent(libraryFile);
          }
          event.addFileAndDartUnit(dartFile, dartUnit);
        }
      }
      if (event != null) {
        errorListener.notifyParsed(event);
      }

      // Notify listeners about libraries that were resolved

      errorListener.notifyResolved(libraryFile, libUnit);
    }

    // If the expected library was not resolved then log an error and insert a placeholder
    // so that we don't try to continually resolve a library which cannot be resolved

    if (library.getLibraryUnit() == null) {
      DartCore.logError("Failed to resolve " + library.getFile());
      LibraryUnit libUnit = new LibraryUnit(library.getLibrarySource());
      library.cacheLibraryUnit(server, libUnit);
    }
  }
}
