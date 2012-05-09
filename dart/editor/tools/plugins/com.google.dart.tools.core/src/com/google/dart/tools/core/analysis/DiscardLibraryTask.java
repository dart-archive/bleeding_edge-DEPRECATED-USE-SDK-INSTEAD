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
 * Remove any information cached about the library and any tasks related to analyzing the library
 */
public class DiscardLibraryTask extends Task {

  private final AnalysisServer server;
  private final Context context;
  private final File libraryFile;

  public DiscardLibraryTask(AnalysisServer server, Context context, File libraryFile) {
    this.server = server;
    this.context = context;
    this.libraryFile = libraryFile;
  }

  @Override
  boolean isBackgroundAnalysis() {
    return false;
  }

  @Override
  void perform() {
    AnalysisEvent event = null;
    Library library = context.getCachedLibrary(libraryFile);
    if (library != null) {
      event = new AnalysisEvent(libraryFile, library.getSourceFiles());
      context.discardLibraryAndReferencingLibraries(library);
    }
    // Remove all pending analysis tasks as they may have been related to the discarded library
    server.removeAllBackgroundAnalysisTasks();
    // Reanalyze any libraries not already cached
    server.queueAnalyzeContext();

    // If any analysis has been performed on the library
    // then notify others that the library is no longer being analyzed
    if (event != null) {
      for (AnalysisListener listener : server.getAnalysisListeners()) {
        try {
          listener.discarded(event);
        } catch (Throwable e) {
          DartCore.logError("Exception during discard notification", e);
        }
      }
    }
  }
}
