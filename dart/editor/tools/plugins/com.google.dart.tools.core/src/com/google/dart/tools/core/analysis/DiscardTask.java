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

import static com.google.dart.tools.core.analysis.AnalysisUtility.equalsOrContains;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Remove package contexts, cached libraries, and any tasks related to analyzing those libraries
 */
class DiscardTask extends Task {

  private final AnalysisServer server;
  private final SavedContext savedContext;
  private final File rootFile;
  private final ArrayList<Library> discardedLibraries;

  public DiscardTask(AnalysisServer server, File file) {
    this.server = server;
    this.savedContext = server.getSavedContext();
    this.rootFile = file;
    this.discardedLibraries = new ArrayList<Library>();
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

    // If this is a dart file, then discard the cached library
    if (rootFile.isFile()
        || (!rootFile.exists() && DartCore.isDartLikeFileName(rootFile.getName()))) {
      for (PackageContext packageContext : savedContext.getPackageContexts()) {
        discardLibrary(packageContext, rootFile);
      }
      discardLibrary(savedContext, rootFile);
    }

    // otherwise discard all cached libraries in the specified directory tree
    else {
      for (PackageContext packageContext : savedContext.getPackageContexts()) {
        discardLibrariesIn(packageContext, rootFile);

        // Discard any package contexts in the specified directory tree
        if (equalsOrContains(rootFile, packageContext.getApplicationDirectory())) {
          savedContext.discardPackageContext(packageContext);
        }
      }
      discardLibrariesIn(savedContext, rootFile);
    }

    // Remove any referenced libraries
    int index = 0;
    while (index < discardedLibraries.size()) {
      Library library = discardedLibraries.get(index++);
      for (PackageContext packageContext : savedContext.getPackageContexts()) {
        discardReferencingLibraries(packageContext, library);
      }
      discardReferencingLibraries(savedContext, library);
    }

    // Remove all pending analysis tasks as they may have been related to the discarded library
    server.removeAllBackgroundAnalysisTasks();
    // Reanalyze any libraries not already cached
    server.queueAnalyzeContext();
  }

  /**
   * Discard all cached libraries in the specified directory tree
   */
  private void discardLibrariesIn(Context context, File directory) {
    Collection<Library> cachedLibraries = new ArrayList<Library>(context.getCachedLibraries());
    for (Library library : cachedLibraries) {
      if (equalsOrContains(directory, library.getFile())) {
        discardLibrary(context, library);
      }
    }
  }

  /**
   * If there is a cached library, then discard the library and notify others that the library is no
   * longer being analyzed
   */
  private void discardLibrary(Context context, File libraryFile) {
    Library library = context.getCachedLibrary(libraryFile);
    if (library != null) {
      discardLibrary(context, library);
    }
  }

  /**
   * Discard the library and notify others that the library is no longer being analyzed
   */
  private void discardLibrary(Context context, Library library) {
    AnalysisEvent event = new AnalysisEvent(
        library.getFile(),
        library.getSourceFiles(),
        AnalysisError.NONE);

    context.discardLibrary(library);
    discardedLibraries.add(library);

    for (AnalysisListener listener : savedContext.getAnalysisListeners()) {
      try {
        listener.discarded(event);
      } catch (Throwable e) {
        DartCore.logError("Exception during discard notification", e);
      }
    }
  }

  /**
   * Discard any libraries referencing the specified library
   */
  private void discardReferencingLibraries(Context context, Library library) {
    for (Library referencingLibrary : context.getLibrariesImporting(library.getFile())) {
      discardLibrary(context, referencingLibrary);
    }
  }
}
