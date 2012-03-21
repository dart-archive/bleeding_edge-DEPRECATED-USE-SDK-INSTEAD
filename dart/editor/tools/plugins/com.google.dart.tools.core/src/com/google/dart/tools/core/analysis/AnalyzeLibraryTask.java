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

/**
 * Analyze a library
 */
class AnalyzeLibraryTask extends Task {

  private final AnalysisServer server;
  private final Context context;
  private final File libraryFile;
  private final ResolveLibraryListener resolutionListener;

  private boolean analyzeIfNotTracked;
  private long start = 0;

  AnalyzeLibraryTask(AnalysisServer server, Context context, File libraryFile) {
    this(server, context, libraryFile, null);
  }

  AnalyzeLibraryTask(AnalysisServer server, Context context, File libraryFile,
      ResolveLibraryListener resolutionListener) {
    this.server = server;
    this.context = context;
    this.libraryFile = libraryFile;
    this.resolutionListener = resolutionListener;
  }

  @Override
  void perform() {

    // Determine if the library should still be analyzed

    if (!(analyzeIfNotTracked || server.isTrackedLibraryFile(libraryFile)) || !libraryFile.exists()) {
      return;
    }
    if (start == 0) {
      start = System.currentTimeMillis();
    }

    // Parse the library file

    Library library = context.getCachedLibrary(libraryFile);
    if (library == null) {
      server.queueSubTask(new ParseLibraryTask(server, context, libraryFile));
      server.queueSubTask(this);
      return;
    }

    // Parse imported libraries

    boolean found = false;
    for (File file : library.getImportedFiles()) {
      if (context.getCachedLibrary(file) == null && file.exists()) {
        server.queueSubTask(new ParseLibraryTask(server, context, file));
        found = true;
      }
    }
    if (found) {
      server.queueSubTask(this);
      return;
    }

    // Resolve the library

    if (library.getLibraryUnit() == null) {
      server.queueSubTask(new ResolveLibraryTask(server, context, library));
      server.queueSubTask(this);
      return;
    }

    // Notify that analysis for this library is complete

    PerformanceListener listener = AnalysisServer.getPerformanceListener();
    if (listener != null) {
      listener.analysisComplete(start, libraryFile);
    }
    if (resolutionListener != null) {
      try {
        resolutionListener.resolved(library.getLibraryUnit());
      } catch (Throwable e) {
        DartCore.logError("Exception during resolution notification", e);
      }
    }
  }

  void setAnalyzeIfNotTracked(boolean analyzeIfNotTracked) {
    this.analyzeIfNotTracked = analyzeIfNotTracked;
  }
}
