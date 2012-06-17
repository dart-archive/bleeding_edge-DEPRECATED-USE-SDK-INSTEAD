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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;

/**
 * Analyze a library
 */
class AnalyzeLibraryTask extends Task {

  private final AnalysisServer server;
  private final Context context;
  private final File rootLibraryFile;
  private final ResolveCallback callback;
  private final HashSet<File> parsed;
  private final ArrayList<Library> toAnalyze;

  private long start = 0;

  AnalyzeLibraryTask(AnalysisServer server, Context context, File libraryFile,
      ResolveCallback callback) {
    this.server = server;
    this.context = context;
    this.rootLibraryFile = libraryFile;
    this.callback = callback;
    this.parsed = new HashSet<File>(100);
    this.toAnalyze = new ArrayList<Library>(100);
  }

  public File getRootLibraryFile() {
    return rootLibraryFile;
  }

  @Override
  boolean isBackgroundAnalysis() {
    return callback == null;
  }

  @Override
  boolean isPriority() {
    return false;
  }

  @Override
  void perform() {
    if (start == 0) {
      start = System.currentTimeMillis();
    }

    // Recursively parse the library and its imports

    parsed.clear();
    toAnalyze.clear();
    boolean subTasksQueued = parse(rootLibraryFile);
    if (subTasksQueued) {
      server.queueSubTask(this);
      return;
    }

    // Resolve each library

    for (Library library : toAnalyze) {
      if (resolve(library)) {
        subTasksQueued = true;
      }
    }
    if (subTasksQueued) {
      server.queueSubTask(this);
      return;
    }

    // Notify that analysis for this library is complete

    PerformanceListener listener = AnalysisServer.getPerformanceListener();
    if (listener != null) {
      listener.analysisComplete(start, rootLibraryFile);
    }
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
   * @return <code>true</code> if sub tasks were queued
   */
  private boolean parse(File libraryFile) {
    if (parsed.contains(libraryFile)) {
      return false;
    }
    parsed.add(libraryFile);
    Library library = context.getCachedLibrary(libraryFile);
    if (library == null) {
      server.queueSubTask(new ParseLibraryFileTask(server, context, libraryFile, null));
      return true;
    }
    boolean subTasksQueued = false;
    for (File importedFile : library.getImportedFiles()) {
      if (parse(importedFile)) {
        subTasksQueued = true;
      }
    }
    toAnalyze.add(library);
    return subTasksQueued;
  }

  /**
   * Resolve the library
   * 
   * @param libraryFile the library file (not <code>null</code>)
   * @return <code>true</code> if sub tasks were queued
   */
  private boolean resolve(Library library) {
    if (library.getLibraryUnit() != null) {
      return false;
    }
    for (Entry<String, File> entry : library.getRelativeSourcePathsAndFiles()) {
      String relPath = entry.getKey();
      File file = entry.getValue();
      server.queueSubTask(new ParseFileTask(server, context, library.getFile(), relPath, file, null));
    }
    server.queueSubTask(new ResolveLibraryTask(server, context, library));
    return true;
  }
}
