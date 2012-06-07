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
import java.util.ArrayList;
import java.util.Collection;
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

  private final Library rootLibrary;

  ResolveLibraryTask(AnalysisServer server, Context context, Library library) {
    this.server = server;
    this.context = context;
    this.rootLibrary = library;
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
    if (rootLibrary.getLibraryUnit() != null) {
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
        rootLibrary.getFile(),
        rootLibrary.getLibrarySource(),
        resolvedLibs,
        parsedUnits,
        errorListener);

    for (LibraryUnit libUnit : newlyResolved.values()) {

      // Cache the resolved library

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

      // Collect the errors and warnings for this library

      Collection<AnalysisError> newErrors = AnalysisError.NONE;
      for (AnalysisError error : errorListener.getErrors()) {
        if (libraryFile.equals(error.getLibraryFile())) {
          if (newErrors == AnalysisError.NONE) {
            newErrors = new ArrayList<AnalysisError>();
          }
          newErrors.add(error);
        }
      }

      // If this is a library whose state is being reloaded, then skip notifications

      if (!library.shouldNotify) {
        continue;
      }

      // Notify listeners about units that were parsed, if any, during the resolution process
      // and notify listeners that the library was resolved

      AnalysisEvent parseEvent = null;
      AnalysisEvent resolutionEvent = new AnalysisEvent(libraryFile, newErrors);
      Iterator<DartUnit> iter = libUnit.getUnits().iterator();
      while (iter.hasNext()) {
        DartUnit dartUnit = iter.next();
        File dartFile = toFile(server, dartUnit.getSourceInfo().getSource().getUri());
        if (dartFile == null) {
          continue;
        }
        if (!parsedUnitURIs.contains(dartFile.toURI())) {
          if (parseEvent == null) {
            // Do not report errors during this notification because they will be reported
            // as part of the resolution notification
            parseEvent = new AnalysisEvent(libraryFile);
          }
          parseEvent.addFileAndDartUnit(dartFile, dartUnit);
        }
        resolutionEvent.addFileAndDartUnit(dartFile, dartUnit);
      }
      if (parseEvent != null) {
        parseEvent.notifyParsed(context);
      }
      resolutionEvent.notifyResolved(context);
    }

    // If the expected library was not resolved then log an error and insert a placeholder
    // so that we don't try to continually resolve a library which cannot be resolved

    if (rootLibrary.getLibraryUnit() == null) {
      DartCore.logError("Failed to resolve " + rootLibrary.getFile());
      LibraryUnit libUnit = new LibraryUnit(rootLibrary.getLibrarySource());
      rootLibrary.cacheLibraryUnit(server, libUnit);
    }
  }
}
