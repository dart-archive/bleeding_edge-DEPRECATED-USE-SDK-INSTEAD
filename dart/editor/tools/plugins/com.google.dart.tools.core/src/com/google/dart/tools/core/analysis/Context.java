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
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCore;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * The context (saved on disk, editor buffer, refactoring) in which analysis occurs.
 */
class Context {

  private static final String END_CACHE_TAG = "</end-cache>";

  private static final Library[] NO_LIBRARIES = new Library[] {};

  private AnalysisServer server;

  private AnalysisListener[] analysisListeners = new AnalysisListener[0];

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

  Context(AnalysisServer server) {
    this.server = server;
    this.libraryCache = new HashMap<File, Library>();
    this.unresolvedUnits = new HashMap<URI, DartUnit>();
  }

  public void addAnalysisListener(AnalysisListener listener) {
    for (int i = 0; i < analysisListeners.length; i++) {
      if (analysisListeners[i] == listener) {
        return;
      }
    }
    int oldLen = analysisListeners.length;
    AnalysisListener[] newListeners = new AnalysisListener[oldLen + 1];
    System.arraycopy(analysisListeners, 0, newListeners, 0, oldLen);
    newListeners[oldLen] = listener;
    analysisListeners = newListeners;
  }

  /**
   * Parse the specified file, without adding the library to the list of libraries to be tracked.
   * 
   * @param libraryFile the library containing the dart file to be parsed (not <code>null</code>)
   * @param dartFile the dart file to be parsed (not <code>null</code>). This may be the same as the
   *          libraryFile.
   * @param milliseconds the number of milliseconds to wait for the file to be parsed.
   * @return the parsed dart unit or <code>null</code> if the file was not parsed within the
   *         specified amount of time. This unit may or may not be resolved.
   * @throws RuntimeException if the parse takes longer than the specified time
   */
  public DartUnit parse(File libraryFile, File dartFile, long milliseconds) {
    ParseCallback.Sync callback = new ParseCallback.Sync();
    parse(libraryFile, dartFile, callback);
    DartUnit result = callback.waitForParse(milliseconds);
    if (result == null) {
      throw new RuntimeException("Timed out waiting for parse: " + dartFile + " in " + libraryFile);
    }
    return result;
  }

  /**
   * Parse the specified file, without adding the library to the list of libraries to be tracked.
   * 
   * @param libraryFile the library containing the dart file to be parsed (not <code>null</code>)
   * @param dartFile the dart file to be parsed (not <code>null</code>). This may be the same as the
   *          libraryFile
   * @param callback a listener that will be notified when the library file has been parsed or
   *          <code>null</code> if none
   */
  public void parse(File libraryFile, File dartFile, ParseCallback callback) {
    String relPath = libraryFile.toURI().relativize(dartFile.toURI()).getPath();
    server.queueNewTask(new ParseFileTask(server, this, libraryFile, relPath, dartFile, callback));
  }

  /**
   * Resolve the specified library. Similar to {@link #analyze(File)}, but does not add the library
   * to the list of libraries to be tracked.
   * 
   * @param libraryFile the library file (not <code>null</code>).
   * @param milliseconds the number of milliseconds to wait for the library to be resolved.
   * @return the resolved library (not <code>null</code>)
   * @throws RuntimeException if the resolution takes longer than the specified time
   */
  public LibraryUnit resolve(File libraryFile, long milliseconds) {
    ResolveCallback.Sync callback = new ResolveCallback.Sync();
    resolve(libraryFile, callback);
    LibraryUnit result = callback.waitForResolve(milliseconds);
    if (result == null) {
      throw new RuntimeException("Timed out waiting for library to be resolved: " + libraryFile);
    }
    return result;
  }

  /**
   * Resolve the specified library. Similar to {@link #analyze(File)}, but does not add the library
   * to the list of libraries to be tracked.
   * 
   * @param libraryFile the library file (not <code>null</code>)
   * @param callback a listener that will be notified when the library has been resolved or
   *          <code>null</code> if none
   */
  public void resolve(File libraryFile, ResolveCallback callback) {
    if (!libraryFile.isAbsolute()) {
      throw new IllegalArgumentException("File path must be absolute: " + libraryFile);
    }
    server.queueNewTask(new AnalyzeLibraryTask(server, this, libraryFile, callback));
  }

  void cacheLibrary(Library library) {
    libraryCache.put(library.getFile(), library);
  }

  void cacheUnresolvedUnit(File file, DartUnit unit) {
    unresolvedUnits.put(file.toURI(), unit);
  }

  void discardLibraries() {
    libraryCache.clear();
    unresolvedUnits.clear();
  }

  void discardLibrary(Library library) {
    File libraryFile = library.getFile();
    libraryCache.remove(libraryFile);
    unresolvedUnits.remove(libraryFile.toURI());
    for (File sourcedFile : library.getSourceFiles()) {
      unresolvedUnits.remove(sourcedFile.toURI());
    }
  }

  void discardLibraryAndReferencingLibraries(Library library) {
    discardLibrary(library);
    for (Library cachedLibrary : getLibrariesImporting(library.getFile())) {
      discardLibraryAndReferencingLibraries(cachedLibrary);
    }
  }

  AnalysisListener[] getAnalysisListeners() {
    return analysisListeners;
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
   * Answer a resolved or unresolved unit, or <code>null</code> if none
   */
  DartUnit getCachedUnit(Library library, File dartFile) {
    if (library != null) {
      DartUnit unit = library.getResolvedUnit(dartFile);
      if (unit != null) {
        return unit;
      }
    }
    return unresolvedUnits.get(dartFile.toURI());
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
   * Answer units that have been parsed by not resolved.
   */
  HashMap<URI, DartUnit> getUnresolvedUnits() {
    return unresolvedUnits;
  }

  /**
   * Reload cached libraries
   */
  void readCache(LineNumberReader reader) throws IOException {
    while (true) {
      String filePath = reader.readLine();
      if (filePath == null) {
        throw new IOException("Expected " + END_CACHE_TAG + " but found EOF");
      }
      if (filePath.equals(END_CACHE_TAG)) {
        break;
      }
      File libraryFile = new File(filePath);
      Library lib = Library.readCache(server, libraryFile, reader);
      libraryCache.put(libraryFile, lib);
    }
  }

  /**
   * Write information for each cached library. Don't include unresolved libraries so that listeners
   * will be notified when the cache is reloaded.
   */
  void writeCache(PrintWriter writer) {
    for (Entry<File, Library> entry : libraryCache.entrySet()) {
      Library library = entry.getValue();
      if (library.hasBeenResolved()) {
        writer.println(entry.getKey().getPath());
        library.writeCache(writer);
      }
    }
    writer.println(END_CACHE_TAG);
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
