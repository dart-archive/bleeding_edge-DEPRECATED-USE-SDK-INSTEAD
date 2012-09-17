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
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.core.analysis.AnalysisUtility.equalsOrContains;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The context (saved on disk, editor buffer, refactoring) in which analysis occurs.
 */
public abstract class Context {

  private static final String END_CACHE_TAG = "</end-cache>";

  protected final AnalysisServer server;

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

  void cacheLibrary(Library library) {
    libraryCache.put(library.getFile(), library);
    // Sanity check
    if (library.getContext() != this) {
      PrintStringWriter msg = new PrintStringWriter();
      msg.print("Library ");
      msg.println(library.getFile());
      msg.print("  created in ");
      msg.println(library.getContext());
      msg.print("  but cached in ");
      msg.println(this);
      DartCore.logError(msg.toString());
    }
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
   * @return the collection of discarded libraries (not <code>null</code>, contains no
   *         <code>null</code>s)
   */
  Collection<Library> discardLibraries(File rootFile) {
    ArrayList<Library> discarded = new ArrayList<Library>(40);
    discardLibraries(rootFile, discarded);
    return discarded;
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
          server.getSavedContext().notifyDiscarded(library);
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
          server.getSavedContext().notifyDiscarded(library);
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
      server.getSavedContext().notifyDiscarded(library);
    }
    return library;
  }

  abstract File getApplicationDirectory();

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
    return getLibrariesSourcing(file, Library.NONE);
  }

  /**
   * Append the libraries that source the specified file to the specified array
   * 
   * @return an array of libraries (not <code>null</code>, contains no <code>null</code>s)
   */
  Library[] getLibrariesSourcing(File file, Library[] result) {
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
      Library lib = Library.readCache(server, this, libraryFile, reader);
      libraryCache.put(libraryFile, lib);
    }
  }

  /**
   * Resolve the specified path to a file.
   * 
   * @return the file or <code>null</code> if it could not be resolved
   */
  File resolvePath(URI base, String relPath) {
    if (relPath == null) {
      return null;
    }
    if (PackageLibraryManager.isDartSpec(relPath) || PackageLibraryManager.isPackageSpec(relPath)) {
      URI relativeUri;
      try {
        relativeUri = new URI(relPath);
      } catch (URISyntaxException e) {
        DartCore.logError("Failed to create URI: " + relPath, e);
        return null;
      }
      URI resolveUri = getLibraryManager().resolveDartUri(relativeUri);
      if (resolveUri == null) {
        return null;
      }
      return new File(resolveUri.getPath());
    }
    File file = new File(relPath);
    if (file.isAbsolute()) {
      return file;
    }
    try {
      String path = base.resolve(new URI(null, null, relPath, null)).normalize().getPath();
      if (path != null) {
        return new File(path);
      }
    } catch (URISyntaxException e) {
      //$FALL-THROUGH$
    }
    return null;
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
