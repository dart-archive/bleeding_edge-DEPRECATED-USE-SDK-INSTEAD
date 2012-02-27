/*
 * Copyright 2012 Dart project authors.
 * 
 * Licensed under the Eclipse License v1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
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
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The context (saved on disk, editor buffer, refactoring) in which analysis occurs.
 */
class Context {

  private final AnalysisServer server;

  /**
   * The libraries in this context, including imported libraries. This should only be accessed on
   * the background thread.
   */
  private final HashMap<File, Library> libraryCache;

  Context(AnalysisServer server) {
    this.server = server;
    this.libraryCache = new HashMap<File, Library>();
  }

  void cacheLibrary(Library library) {
    libraryCache.put(library.getFile(), library);
  }

  void discardLibrary(Library library) {
    libraryCache.remove(library.getFile());
  }

  /**
   * Answer the cached library or <code>null</code> if not cached
   */
  Library getCachedLibrary(File file) {
    return libraryCache.get(file);
  }

  /**
   * Answer the libraries importing the specified file
   */
  ArrayList<Library> getLibrariesImporting(File file) {
    ArrayList<Library> result = new ArrayList<Library>();
    for (Library cachedLibrary : libraryCache.values()) {
      if (cachedLibrary.getImportedFiles().contains(file)) {
        result.add(cachedLibrary);
      }
    }
    return result;
  }

  /**
   * Answer the library containing the specified file or <code>null</code> if unknown
   */
  Library getLibraryContaining(File file) {
    Library library = libraryCache.get(file);
    if (library != null) {
      return library;
    }
    for (Library cachedLibrary : libraryCache.values()) {
      if (cachedLibrary.getSourceFiles().contains(file)) {
        return cachedLibrary;
      }
    }
    return null;
  }
}
