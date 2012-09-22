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

import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.core.analysis.AnalysisUtility.isSdkLibrary;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;

/**
 * Analyze a library
 */
public class AnalyzeLibraryTask extends Task {

  private final AnalysisServer server;
  private final SavedContext savedContext;
  private final File rootLibraryFile;
  private final ResolveCallback callback;
  private final HashSet<File> parsed;
  private final ArrayList<Library> toResolve;

  private Context rootLibraryContext;

  private long start = 0;

  public AnalyzeLibraryTask(AnalysisServer server, File libraryFile, ResolveCallback callback) {
    this.server = server;
    this.savedContext = server.getSavedContext();
    this.rootLibraryFile = libraryFile;
    this.callback = callback;
    this.parsed = new HashSet<File>(100);
    this.toResolve = new ArrayList<Library>(100);
  }

  @Override
  public boolean canRemove(File discarded) {
    return callback == null;
  }

  @Override
  public boolean isPriority() {
    return false;
  }

  @Override
  public void perform() {
    if (start == 0) {
      start = System.currentTimeMillis();
    }

    // Determine the proper context in which to analyze this library

    if (rootLibraryContext == null) {
      rootLibraryContext = savedContext.getSuggestedContext(rootLibraryFile);
      if (rootLibraryContext == null) {
        server.queueSubTask(this);
        return;
      }
    }
    Context context = rootLibraryContext;

    // Recursively parse the library and its imports

    parsed.clear();
    toResolve.clear();
    if (parse(context, rootLibraryFile)) {
      server.queueSubTask(this);
      return;
    }

    // If this library has directives, 
    // then discard any sourced files that don't have directives

    Library rootLibrary = context.getCachedLibrary(rootLibraryFile);
    if (rootLibrary.hasDirectives()) {
      for (File sourceFile : rootLibrary.getSourceFiles()) {
        Library library = context.getCachedLibrary(sourceFile);
        if (library == null || !library.hasDirectives()) {
          server.discard(sourceFile);
        }
      }
    }

    // If this library does not have directives and it is a background task (no callback)
    // then discard this library if it is sourced by another library but not imported by any

    else if (callback == null) {
      if (context.getLibrariesSourcing(rootLibraryFile).length > 0
          && context.getLibrariesImporting(rootLibraryFile).size() == 0) {
        context.discardLibrary(rootLibraryFile);
        return;
      }
    }

    // Resolve each library

    boolean subTasksQueued = false;
    for (Library library : toResolve) {
      if (resolve(library)) {
        subTasksQueued = true;
      }
    }
    if (subTasksQueued) {
      server.queueSubTask(this);
      return;
    }

    // Notify that analysis for this library is complete

    if (callback != null) {
      try {
        callback.resolved(context.getCachedLibrary(rootLibraryFile).getLibraryUnit());
      } catch (Throwable e) {
        DartCore.logError("Exception during resolution notification", e);
      }
    }
  }

  /**
   * Recursively parse the library and its imports, building the list of libraries to be analyzed
   * 
   * @param libraryFile the library file (not <code>null</code>)
   * @param the context in which to parse the library file
   * @return <code>true</code> if sub tasks were queued
   */
  protected boolean parse(Context context, File libraryFile) {
    if (parsed.contains(libraryFile)) {
      return false;
    }
    parsed.add(libraryFile);

    // SDK libraries should always reside in the saved context... not a packages context

    if (isSdkLibrary(libraryFile)) {
      context = savedContext;
    }

    // If this analysis is in a package context, 
    // then remove libraries from saved context as appropriate.

    else if (context != savedContext) {
      Library library = savedContext.getCachedLibrary(libraryFile);
      if (library != null && savedContext.getLibrariesImporting(libraryFile).size() == 0) {
        savedContext.discardLibrary(libraryFile);
      }
    }

    Library library = context.getCachedLibrary(libraryFile);
    if (library == null) {
      server.queueSubTask(new ParseTask(server, context, libraryFile));
      return true;
    }
    boolean subTasksQueued = false;
    for (File importedFile : library.getImportedFiles()) {
      if (parse(context, importedFile)) {
        subTasksQueued = true;
      }
    }
    toResolve.add(library);
    return subTasksQueued;
  }

  /**
   * Resolve the library
   * 
   * @param libraryFile the library file (not <code>null</code>)
   * @return <code>true</code> if sub tasks were queued
   */
  protected boolean resolve(Library library) {
    if (library.getLibraryUnit() != null) {
      return false;
    }
    Context context = library.getContext();
    for (Entry<String, File> entry : library.getRelativeSourcePathsAndFiles()) {
      File file = entry.getValue();
      if (library.getDartUnit(file) == null) {
        String relPath = entry.getKey();
        server.queueSubTask(new ParseTask(server, context, library.getFile(), relPath, file));
      }
    }
    server.queueSubTask(new ResolveTask(server, context, library));
    return true;
  }

  File getRootLibraryFile() {
    return rootLibraryFile;
  }
}
