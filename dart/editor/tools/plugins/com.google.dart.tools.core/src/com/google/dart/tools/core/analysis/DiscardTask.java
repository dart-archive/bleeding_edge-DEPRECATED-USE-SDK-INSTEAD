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
 * Remove package contexts, cached libraries, and any tasks related to analyzing those libraries
 */
class DiscardTask extends Task {

  private final AnalysisServer server;
  private final File rootFile;

  public DiscardTask(AnalysisServer server, File file) {
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
    File file = rootFile;

    // If a "packages" directory was discarded, then discard the application context
    // The pubspec may have already been discarded, so only check the directory name
    if (file.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
      file = file.getParentFile();
    }

    // Discard libraries and package contexts
    if (server.getSavedContext().discardLibraries(file).size() == 0) {
      return;
    }

    // Remove all pending analysis tasks as they may have been related to the discarded libraries
    server.removeBackgroundTasks(file);

    // Reanalyze any libraries not already cached
    server.queueAnalyzeContext();
  }
}
