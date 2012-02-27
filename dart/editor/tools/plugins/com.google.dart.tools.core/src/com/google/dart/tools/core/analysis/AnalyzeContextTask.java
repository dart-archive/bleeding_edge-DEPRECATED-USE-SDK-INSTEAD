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

import java.io.File;

/**
 * Analyze all libraries in a context
 */
class AnalyzeContextTask extends Task {
  private final AnalysisServer server;
  private final Context context;
  private File activeFile;

  AnalyzeContextTask(AnalysisServer server, Context context, File activeFile) {
    this.server = server;
    this.context = context;
    this.activeFile = activeFile;
  }

  @Override
  void perform() {

    // Analyze the currently active library first

    if (activeFile != null) {
      if (server.shouldAnalyzeLibrary(activeFile)) {
        server.queueSubTask(new AnalyzeLibraryTask(server, context, activeFile));
        server.queueSubTask(this);
        activeFile = null;
        return;
      }
    }

    // Parse library files

    boolean found = false;
    for (File libFile : server.getLibraryFiles()) {
      if (context.getCachedLibrary(libFile) == null) {
        server.queueSubTask(new ParseLibraryFileTask(server, context, libFile));
        found = true;
      }
    }
    if (found) {
      server.queueSubTask(this);
      return;
    }

    // Find the currently active library and analyze it first

    if (activeFile != null) {
      Library library = context.getLibraryContaining(activeFile);
      if (library != null) {
        server.queueSubTask(new AnalyzeLibraryTask(server, context, library.getFile()));
        server.queueSubTask(this);
        return;
      }
    }

    // Analyze all libraries

    for (File libFile : server.getLibraryFiles()) {
      server.queueSubTask(new AnalyzeLibraryTask(server, context, libFile));
    }
  }
}
