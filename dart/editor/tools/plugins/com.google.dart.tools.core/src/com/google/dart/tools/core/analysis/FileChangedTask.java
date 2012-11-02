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
import java.util.HashSet;

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
    HashSet<File> reanalyze = new HashSet<File>();

    // Discard and scan cached libraries
    for (Library library : savedContext.getCachedLibraries(rootFile)) {
      Context context = library.getContext();

      // Discard and re-analyze only if this library is not already up to date
      if (rootFile.lastModified() != library.lastModified(rootFile)) {
        task = new ScanTask(server, rootFile, null);

        // Discard and scan any libraries that were incorrectly sourced
        Collection<File> sourceFiles = library.getSourceFiles();
        task.addFilesToScan(sourceFiles);
        for (File sourceFile : sourceFiles) {
          Library sourcedLibrary = context.discardLibrary(sourceFile);
          if (sourcedLibrary != null) {
            reanalyze.add(sourcedLibrary.getFile());
          }
        }

        // Discard and reanalyze the library and any downstream libraries
        for (Library discarded : context.discardLibraries(library.getFile())) {
          reanalyze.add(discarded.getFile());
        }
      }
    }

    // If this rootFile is sourced by another library, then scan the rootFile for directive changes
    for (Library otherLibrary : savedContext.getLibrariesSourcing(rootFile)) {
      Context context = otherLibrary.getContext();

      // Discard and re-analyze only if this library is not already up to date
      if (rootFile.lastModified() != otherLibrary.lastModified(rootFile)) {

        if (task == null) {
          task = new ScanTask(server, rootFile, null);
        }
        task.addFilesToScan(otherLibrary.getFile());

        // Discard and reanalyze the library and any downstream libraries
        for (Library discarded : context.discardLibraries(otherLibrary.getFile())) {
          reanalyze.add(discarded.getFile());
        }
      }
    }

    if (task != null) {
      server.queueSubTask(task);
      //server.queueAnalyzeContext();
      server.queueAnalyzeSubTasks(reanalyze);
    }
  }
}
