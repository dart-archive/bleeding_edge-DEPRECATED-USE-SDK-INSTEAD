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
  private final File rootFile;

  FileChangedTask(AnalysisServer server, File file) {
    this.server = server;
    this.rootFile = file;
  }

  @Override
  public boolean canRemove(File discarded) {
    return false;
  }

  @Override
  public boolean isPriority() {
    return true;
  }

  @Override
  public void perform() {
    SavedContext savedContext = server.getSavedContext();
    ScanTask task = null;

    Library library = savedContext.getCachedLibrary(rootFile);
    Library[] librariesSourcing = savedContext.getLibrariesSourcing(rootFile);

    // If this rootFile is a library, then scan the library and all its files for directive changes
    if (library != null) {

      // Discard and re-analyze only if this library is not already up to date
      if (rootFile.lastModified() != library.lastModified(rootFile)) {
        task = new ScanTask(server, rootFile, null);

        // Discard and scan any libraries that were incorrectly sourced
        Collection<File> sourceFiles = library.getSourceFiles();
        task.addFilesToScan(sourceFiles);
        for (File sourceFile : sourceFiles) {
          savedContext.discardLibrary(sourceFile);
        }

        // Discard the library and any downstream libraries
        savedContext.discardLibraries(library.getFile());
      }
    }

    // If this rootFile is sourced by another library, then scan the rootFile for directive changes
    for (Library otherLibrary : librariesSourcing) {

      // Discard and re-analyze only if this library is not already up to date
      if (rootFile.lastModified() != otherLibrary.lastModified(rootFile)) {

        if (task == null) {
          task = new ScanTask(server, rootFile, null);
        }
        task.addFilesToScan(otherLibrary.getFile());

        // Discard the library and any downstream libraries
        savedContext.discardLibraries(otherLibrary.getFile());
      }
    }

    if (task != null) {
      server.queueSubTask(task);
      server.queueAnalyzeContext();
    }
  }
}
