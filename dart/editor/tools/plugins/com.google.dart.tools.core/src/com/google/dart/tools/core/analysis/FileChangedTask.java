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

  /**
   * Remove the library from the cached libraries along with any downstream libraries
   */
  void discardLibrary(Library library) {
    context.discardLibrary(library);
    for (Library cachedLibrary : context.getLibrariesImporting(library.getFile())) {
      discardLibrary(cachedLibrary);
    }
  }

  @Override
  void perform() {
    Library[] libraries = context.getLibrariesContaining(file);
    for (Library library : libraries) {

      // Discard the library and any downstream libraries
      discardLibrary(library);

      // Append analysis task to the end of the queue so that any user requests take precedence
      server.queueAnalyzeContext();
    }
  }
}
