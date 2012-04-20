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
 * Parse the library file and all files referenced in #source directives
 */
class ParseLibraryTask extends Task {
  private final AnalysisServer server;
  private final Context context;
  private final File libraryFile;

  ParseLibraryTask(AnalysisServer server, Context context, File libraryFile) {
    this.server = server;
    this.context = context;
    this.libraryFile = libraryFile;
  }

  @Override
  void perform() {

    // Parse the library file

    Library library = context.getCachedLibrary(libraryFile);
    if (library == null) {
      if (!libraryFile.exists()) {
        return;
      }
      server.queueSubTask(new ParseLibraryFileTask(server, context, libraryFile, null));
      server.queueSubTask(this);
      return;
    }

    // Parse the files sourced by the library

    for (File file : library.getSourceFiles()) {
      if (library.getCachedUnit(file) == null && file.exists()) {
        server.queueSubTask(new ParseFileTask(server, context, libraryFile, file));
      }
    }
  }
}
