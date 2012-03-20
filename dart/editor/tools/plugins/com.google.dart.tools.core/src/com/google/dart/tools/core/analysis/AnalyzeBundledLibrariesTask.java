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
import com.google.dart.tools.core.internal.model.EditorLibraryManager;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

/**
 * Analyze all bundled libraries, then signal the specified latch that the operation is complete.
 */
public class AnalyzeBundledLibrariesTask extends Task {

  private final AnalysisServer server;
  private final Context context;
  private final CountDownLatch latch;
  private Collection<File> libraryFiles;

  public AnalyzeBundledLibrariesTask(AnalysisServer server, Context context, CountDownLatch latch) {
    this.server = server;
    this.context = context;
    this.latch = latch;
  }

  @Override
  void perform() {

    // Get the bundled libraries

    if (libraryFiles == null) {
      libraryFiles = new ArrayList<File>();
      EditorLibraryManager libraryManager = server.getLibraryManager();
      for (String spec : libraryManager.getAllLibrarySpecs()) {
        URI relativeUri;
        try {
          relativeUri = new URI(spec);
        } catch (URISyntaxException e) {
          DartCore.logError("Failed to create URI: " + spec, e);
          continue;
        }
        URI resolveUri = libraryManager.resolveDartUri(relativeUri);
        if (resolveUri == null) {
          DartCore.logError("Failed to analyze bundled library: " + spec);
        }
        libraryFiles.add(new File(resolveUri.getPath()));
      }
    }

    // Ensure that each bundled library has been analyzed

    boolean found = false;
    for (File libraryFile : libraryFiles) {
      Library cachedLibrary = context.getCachedLibrary(libraryFile);
      if (cachedLibrary == null) {
        AnalyzeLibraryTask subtask = new AnalyzeLibraryTask(server, context, libraryFile);
        subtask.setAnalyzeIfNotTracked(true);
        server.queueSubTask(subtask);
        found = true;
      }
    }
    if (found) {
      server.queueSubTask(this);
      return;
    }

    // Once complete, notify interested objects

    latch.countDown();
  }
}
