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

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * The context (saved on disk, editor buffer, refactoring) in which analysis occurs.
 */
class Context {

  private static final Library[] NO_LIBRARIES = new Library[] {};

  /**
   * The libraries in this context, including imported libraries. This should only be accessed on
   * the background thread.
   */
  private final HashMap<File, Library> libraryCache;

  /**
   * A map of URI (as needed by DartC) to parsed but unresolved unit. Units are added to this
   * collection by {@link ParseFileTask} and {@link ParseLibraryFileTask}, and removed from this
   * collection by {@link ResolveLibraryTask} when it calls
   * {@link AnalysisUtility#resolve(AnalysisServer, Library, java.util.Map, java.util.Map)}
   */
  private final HashMap<URI, DartUnit> unresolvedUnits;

  Context() {
    this.libraryCache = new HashMap<File, Library>();
    this.unresolvedUnits = new HashMap<URI, DartUnit>();
  }

  void cacheLibrary(Library library) {
    libraryCache.put(library.getFile(), library);
  }

  void cacheUnresolvedUnit(File file, DartUnit unit) {
    unresolvedUnits.put(file.toURI(), unit);
  }

  void discardLibraries() {
    libraryCache.clear();
  }

  void discardLibrary(Library library) {
    libraryCache.remove(library.getFile());
  }

  void discardLibraryAndReferencingLibraries(Library library) {
    discardLibrary(library);
    for (Library cachedLibrary : getLibrariesImporting(library.getFile())) {
      discardLibraryAndReferencingLibraries(cachedLibrary);
    }
  }

  Collection<Library> getCachedLibraries() {
    return libraryCache.values();
  }

  /**
   * Answer the cached library or <code>null</code> if not cached
   */
  Library getCachedLibrary(File file) {
    return libraryCache.get(file);
  }

  /**
   * Answer the libraries containing the specified file or contain files in the specified directory
   * tree
   * 
   * @return an array of libraries (not <code>null</code>, contains no <code>null</code>s)
   */
  Library[] getLibrariesContaining(File file) {

    // Quick check if the file is a library

    Library library = libraryCache.get(file);
    if (library != null) {
      return new Library[] {library};
    }
    Library[] result = NO_LIBRARIES;

    // If this is a file, then return the libraries that source the file

    if (file.isFile() || (!file.exists() && DartCore.isDartLikeFileName(file.getName()))) {
      for (Library cachedLibrary : libraryCache.values()) {
        if (cachedLibrary.getSourceFiles().contains(file)) {
          result = append(result, cachedLibrary);
        }
      }
      return result;
    }

    // Otherwise return the libraries containing files in the specified directory tree

    String prefix = file.getAbsolutePath() + File.separator;
    for (Library cachedLibrary : libraryCache.values()) {
      for (File sourceFile : cachedLibrary.getSourceFiles()) {
        if (sourceFile.getPath().startsWith(prefix)) {
          result = append(result, cachedLibrary);
          break;
        }
      }
    }
    return result;
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
   * Answer a unit that has been parsed but not resolved, or <code>null</code> if none
   */
  DartUnit getUnresolvedUnit(File file) {
    return unresolvedUnits.get(file.toURI());
  }

  /**
   * Answer units that have been parsed by not resolved.
   */
  HashMap<URI, DartUnit> getUnresolvedUnits() {
    return unresolvedUnits;
  }

  private Library[] append(Library[] oldArray, Library library) {
    if (oldArray.length == 0) {
      return new Library[] {library};
    }
    int oldLen = oldArray.length;
    Library[] newArray = new Library[oldLen + 1];
    System.arraycopy(oldArray, 0, newArray, 0, oldLen);
    newArray[oldLen] = library;
    return newArray;
  }
}
