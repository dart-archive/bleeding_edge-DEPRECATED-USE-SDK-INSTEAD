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
import java.util.Collection;

/**
 * Update the model given that that specified file has changed
 */
class FileChangedTask extends Task {

  private final AnalysisServer server;
  private final Context context;
  private final File file;

  FileChangedTask(AnalysisServer server, Context context, File file) {
    this.server = server;
    this.context = context;
    this.file = file;
  }

  @Override
  public boolean isBackgroundAnalysis() {
    return false;
  }

  @Override
  public boolean isPriority() {
    return true;
  }

  @Override
  public void perform() {
    ScanTask task = null;
    Library library = context.getCachedLibrary(file);
    Library[] librariesSourcing = context.getLibrariesSourcing(file);

    // If this file is a library, then scan the library and all its files for directive changes
    if (library != null) {

      // Discard and re-analyze only if this library is not already up to date
      if (file.lastModified() != library.lastModified(file)) {
        task = new ScanTask(server, context, file, null);

        // Discard and scan any libraries that were incorrectly sourced
        Collection<File> sourceFiles = library.getSourceFiles();
        task.addFilesToScan(sourceFiles);
        for (File sourceFile : sourceFiles) {
          Library sourcedLibrary = context.getCachedLibrary(sourceFile);
          if (sourcedLibrary != null) {
            context.discardLibrary(sourcedLibrary);
          }
        }

        // Discard the library and any downstream libraries
        context.discardLibraryAndReferencingLibraries(library);
      }
    }

    // If this file is sourced by another library, then scan the file for directive changes
    for (Library otherLibrary : librariesSourcing) {

      // Discard and re-analyze only if this library is not already up to date
      if (file.lastModified() != otherLibrary.lastModified(file)) {

        if (task == null) {
          task = new ScanTask(server, context, file, null);
        }
        task.addFilesToScan(otherLibrary.getFile());

        // Discard the library and any downstream libraries
        context.discardLibraryAndReferencingLibraries(otherLibrary);
      }
    }

    if (task != null) {
      server.queueSubTask(task);
      server.queueAnalyzeContext();
    }
  }
}
