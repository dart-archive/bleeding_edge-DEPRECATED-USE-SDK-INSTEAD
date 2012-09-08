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

import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartSdkManager;

import static com.google.dart.tools.core.analysis.AnalysisUtility.equalsOrContains;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The context (saved on disk, editor buffer, refactoring) in which analysis occurs.
 */
public class Context {

  private static final String END_CACHE_TAG = "</end-cache>";

  protected final AnalysisServer server;

  private AnalysisListener[] analysisListeners = new AnalysisListener[0];

  /**
   * The target (VM, Dartium, JS) against which user libraries are resolved. Targets are immutable
   * and can be accessed on any thread.
   */
  private final PackageLibraryManager libraryManager;

  /**
   * The libraries in this context, including imported libraries. This should only be accessed on
   * the background thread.
   */
  private final HashMap<File, Library> libraryCache;

  Context(AnalysisServer server, PackageLibraryManager libraryManager) {
    this.server = server;
    this.libraryCache = new HashMap<File, Library>();
    this.libraryManager = libraryManager;
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
   * Answer the currently resolved system libraries. If the result is not available within the
   * specified amount of time, then return an empty map.
   * 
   * @param millseconds the number of milliseconds to wait for a result
   * @return a map of absolute File URI to resolved library. Modifications to this map will not
   *         affect future analysis.
   */
  // TODO (danrubel): Optimization for code completion
  // Remove this method once code completion calls analysis server
  @SuppressWarnings("unchecked")
  public Map<URI, LibraryUnit> getResolvedLibraries(long millseconds) {
    final Map<?, ?>[] result = new Map[1];
    server.queueNewTask(new Task() {

      @Override
      public boolean canRemove(File discarded) {
        return false;
      }

      @Override
      public boolean isPriority() {
        return false;
      }

      @Override
      public void perform() {
        HashMap<URI, LibraryUnit> resolvedLibraries = getResolvedLibraries();
        synchronized (result) {
          result[0] = resolvedLibraries;
          result.notifyAll();
        }
      }
    });
    long end = System.currentTimeMillis() + millseconds;
    synchronized (result) {
      while (result[0] == null) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          return new HashMap<URI, LibraryUnit>();
        }
        try {
          result.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
      return (Map<URI, LibraryUnit>) result[0];
    }
  }

  /**
   * Parse the specified file, without adding the library to the list of libraries to be tracked.
   * 
   * @param libraryFile the library containing the dart file to be parsed (not <code>null</code>)
   * @param dartFile the dart file to be parsed (not <code>null</code>). This may be the same as the
   *          libraryFile.
   * @param milliseconds the number of milliseconds to wait for the file to be parsed.
   * @return the parse result (not <code>null</code>).
   * @throws RuntimeException if the parse takes longer than the specified time
   */
  public ParseResult parse(File libraryFile, File dartFile, long milliseconds) {
    ParseCallback.Sync callback = new ParseCallback.Sync();
    parse(libraryFile, dartFile, callback);
    ParseResult result = callback.waitForParse(milliseconds);
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
    if (!libraryFile.isAbsolute()) {
      throw new IllegalArgumentException("File path must be absolute: " + libraryFile);
    }
    if (libraryFile.isDirectory()) {
      throw new IllegalArgumentException("Cannot parse a directory: " + libraryFile);
    }
    String relPath = libraryFile.toURI().relativize(dartFile.toURI()).getPath();
    server.queueNewTask(new ParseTask(server, this, libraryFile, relPath, dartFile, callback));
  }

  public void removeAnalysisListener(AnalysisListener listener) {
    for (int i = 0; i < analysisListeners.length; i++) {
      if (analysisListeners[i] == listener) {
        int oldLen = analysisListeners.length;
        AnalysisListener[] newListeners = new AnalysisListener[oldLen - 1];
        System.arraycopy(analysisListeners, 0, newListeners, 0, i);
        System.arraycopy(analysisListeners, i + 1, newListeners, i, oldLen - i - 1);
        analysisListeners = newListeners;
        return;
      }
    }
  }

  /**
   * Resolve the specified library. Similar to {@link AnalysisServer#analyze(File)}, but does not
   * add the library to the list of libraries to be tracked.
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
   * Resolve the specified library. Similar to {@link AnalysisServer#analyze(File)}, but does not
   * add the library to the list of libraries to be tracked.
   * 
   * @param libraryFile the library file (not <code>null</code>)
   * @param callback a listener that will be notified when the library has been resolved or
   *          <code>null</code> if none
   */
  public void resolve(File libraryFile, ResolveCallback callback) {
    if (!DartSdkManager.getManager().hasSdk()) {
      return;
    }
    if (!libraryFile.isAbsolute()) {
      throw new IllegalArgumentException("File path must be absolute: " + libraryFile);
    }
    if (libraryFile.isDirectory()) {
      throw new IllegalArgumentException("Cannot resolve a directory: " + libraryFile);
    }
    server.queueNewTask(new AnalyzeLibraryTask(server, this, libraryFile, callback));
  }

  void cacheLibrary(Library library) {
    libraryCache.put(library.getFile(), library);
  }

  /**
   * Discard all libraries cached in this context without notifying any listeners.
   */
  void discardAllLibraries() {
    libraryCache.clear();
  }

  /**
   * If the specified file is a directory, then discard all libraries in that directory tree
   * otherwise discard the specified library. In both cases, discard all libraries that directly or
   * indirectly reference the discarded libraries.
   * 
   * @param rootFile the original file or directory to discard
   * @param discarded the collection of libraries that have already been discarded and to which
   *          newly discarded libraries should be added
   */
  void discardLibraries(File rootFile, ArrayList<Library> discarded) {
    // If this is a dart file, then discard the cached library
    if (rootFile.isFile()
        || (!rootFile.exists() && DartCore.isDartLikeFileName(rootFile.getName()))) {
      Library library = discardLibrary(rootFile);
      if (library != null) {
        discarded.add(library);
      }
    }

    // Otherwise discard all cached libraries in the specified directory tree
    else {
      Iterator<Library> iter = libraryCache.values().iterator();
      while (iter.hasNext()) {
        Library library = iter.next();
        if (equalsOrContains(rootFile, library.getFile())) {
          iter.remove();
          notifyDiscarded(library);
          discarded.add(library);
        }
      }
    }

    // Recursively discard all libraries referencing the discarded libraries
    for (int index = 0; index < discarded.size(); index++) {
      File discardedLibraryFile = discarded.get(index).getFile();
      Iterator<Library> iter = libraryCache.values().iterator();
      while (iter.hasNext()) {
        Library library = iter.next();
        if (library.getImportedFiles().contains(discardedLibraryFile)) {
          iter.remove();
          notifyDiscarded(library);
          discarded.add(library);
        }
      }
    }
  }

  /**
   * If there is a library associated with the specified library file, discard it and notify each
   * listener.
   * 
   * @param libraryFile the library file (not <code>null</code>)
   * @return the library discarded, or <code>null</code> if none found
   */
  Library discardLibrary(File libraryFile) {
    Library library = libraryCache.remove(libraryFile);
    if (library != null) {
      notifyDiscarded(library);
    }
    return library;
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
  Library getCachedLibrary(File libraryFile) {
    return libraryCache.get(libraryFile);
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
   * Answer the libraries that source the specified file
   * 
   * @return an array of libraries (not <code>null</code>, contains no <code>null</code>s)
   */
  Library[] getLibrariesSourcing(File file) {
    Library[] result = Library.NONE;
    for (Library cachedLibrary : libraryCache.values()) {
      if (cachedLibrary.getSourceFiles().contains(file)) {
        result = AnalysisUtility.append(result, cachedLibrary);
      }
    }
    return result;
  }

  PackageLibraryManager getLibraryManager() {
    return libraryManager;
  }

  /**
   * Answer the currently cached and resolved libraries
   */
  HashMap<URI, LibraryUnit> getResolvedLibraries() {
    HashMap<URI, LibraryUnit> resolvedLibs = new HashMap<URI, LibraryUnit>();
    for (Library lib : getCachedLibraries()) {
      LibraryUnit libUnit = lib.getLibraryUnit();
      if (libUnit != null) {
        resolvedLibs.put(libUnit.getSource().getUri(), libUnit);
      }
    }
    return resolvedLibs;
  }

  /**
   * Notify listeners that the specified library was discarded
   */
  void notifyDiscarded(Library library) {
    AnalysisEvent event = new AnalysisEvent(
        library.getFile(),
        library.getSourceFiles(),
        AnalysisError.NONE);
    for (AnalysisListener listener : getAnalysisListeners()) {
      try {
        listener.discarded(event);
      } catch (Throwable e) {
        DartCore.logError("Exception during discard notification", e);
      }
    }
  }

  /**
   * Reload cached libraries
   */
  void readCache(CacheReader reader) throws IOException {
    while (true) {
      String filePath = reader.readString();
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
  void writeCache(CacheWriter writer) {
    for (Entry<File, Library> entry : libraryCache.entrySet()) {
      Library library = entry.getValue();
      if (library.hasBeenResolved()) {
        writer.writeString(entry.getKey().getPath());
        library.writeCache(writer);
      }
    }
    writer.writeString(END_CACHE_TAG);
  }
}
