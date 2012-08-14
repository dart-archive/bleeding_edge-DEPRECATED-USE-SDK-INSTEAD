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
import com.google.dart.tools.core.model.DartSdk;

import static com.google.dart.tools.core.analysis.AnalysisUtility.resolve;
import static com.google.dart.tools.core.analysis.AnalysisUtility.toFile;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Resolve types and references in the specified library
 */
class ResolveTask extends Task {

  private final AnalysisServer server;
  private final Context context;
  private final Library rootLibrary;

  ResolveTask(AnalysisServer server, Context context, Library library) {
    this.server = server;
    this.context = context;
    this.rootLibrary = library;
  }

  @Override
  public boolean isBackgroundAnalysis() {
    return true;
  }

  @Override
  public boolean isPriority() {
    return false;
  }

  @Override
  public void perform() {

    if (!DartSdk.isInstalled()) {
      return;
    }

    if (rootLibrary.getLibraryUnit() != null) {
      return;
    }
    ErrorListener errorListener = new ErrorListener(server);

    // Resolve

    SelectiveCacheAdapter selectiveCache = new SelectiveCacheAdapter(server, context);
    Map<URI, LibraryUnit> newlyResolved = resolve(
        rootLibrary.getFile(),
        rootLibrary.getLibrarySource(),
        selectiveCache,
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
        List<DartDirective> directives;

        if (libUnit.getSelfDartUnit() == null) {
          directives = Collections.emptyList();
        } else {
          directives = libUnit.getSelfDartUnit().getDirectives();
        }

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

      HashSet<File> parsedFiles = selectiveCache.getFilesParsedInLibrary(libraryFile);
      if (parsedFiles != null && parsedFiles.size() > 0) {
        // Do not report errors during this notification because they will be reported
        // as part of the resolution notification
        AnalysisEvent parseEvent = new AnalysisEvent(libraryFile);
        Iterator<DartUnit> iter = libUnit.getUnits().iterator();
        while (iter.hasNext()) {
          DartUnit dartUnit = iter.next();
          File dartFile = toFile(server, dartUnit.getSourceInfo().getSource().getUri());
          if (dartFile == null) {
            continue;
          }
          if (parsedFiles.contains(dartFile)) {
            parseEvent.addFileAndDartUnit(dartFile, dartUnit);
          }
        }
        parseEvent.notifyParsed(context);
      }

      // Notify listeners that the library was resolved

      AnalysisEvent resolutionEvent = new AnalysisEvent(libraryFile, newErrors);
      Iterator<DartUnit> iter = libUnit.getUnits().iterator();
      while (iter.hasNext()) {
        DartUnit dartUnit = iter.next();
        File dartFile = toFile(server, dartUnit.getSourceInfo().getSource().getUri());
        if (dartFile == null) {
          continue;
        }
        resolutionEvent.addFileAndDartUnit(dartFile, dartUnit);
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
