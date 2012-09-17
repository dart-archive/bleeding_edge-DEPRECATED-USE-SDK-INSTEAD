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

/**
 * Analyze all libraries in a context
 */
class AnalyzeContextTask extends Task {
  private final AnalysisServer server;

  AnalyzeContextTask(AnalysisServer server) {
    this.server = server;
  }

  @Override
  public boolean canRemove(File discarded) {
    return true;
  }

  @Override
  public boolean isPriority() {
    return false;
  }

  @Override
  public void perform() {
    File[] libraryFiles = server.getTrackedLibraryFiles();
    ArrayList<File> todo = new ArrayList<File>(libraryFiles.length);

    // Analyze libraries in application directories first

    for (File libFile : libraryFiles) {
      File libDir = libFile.getParentFile();
      if (DartCore.isApplicationDirectory(libDir)) {
        server.queueSubTask(new AnalyzeLibraryTask(server, libFile, null));
      } else {
        todo.add(libFile);
      }
    }

    // Then analyze all remaining libraries in the saved context

    for (File libFile : todo) {
      server.queueSubTask(new AnalyzeLibraryTask(server, libFile, null));
    }
  }
}
