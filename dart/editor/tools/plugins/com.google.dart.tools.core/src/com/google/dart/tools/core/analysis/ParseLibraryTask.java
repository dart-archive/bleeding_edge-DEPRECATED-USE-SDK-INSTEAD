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

import java.io.File;
import java.util.Map.Entry;

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
  boolean isBackgroundAnalysis() {
    return true;
  }

  @Override
  boolean isPriority() {
    return false;
  }

  @Override
  void perform() {

    // Parse the library file

    Library library = context.getCachedLibrary(libraryFile);
    DartUnit unit = context.getCachedUnit(library, libraryFile);
    if (library == null || unit == null) {
      server.queueSubTask(new ParseLibraryFileTask(server, context, libraryFile, null));
      server.queueSubTask(this);
      return;
    }

    // Parse the files sourced by the library

    for (Entry<String, File> entry : library.getRelativeSourcePathsAndFiles()) {
      String relPath = entry.getKey();
      File file = entry.getValue();
      server.queueSubTask(new ParseFileTask(server, context, libraryFile, relPath, file, null));
    }
  }
}
