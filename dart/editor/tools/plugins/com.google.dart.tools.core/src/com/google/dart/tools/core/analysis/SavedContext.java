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

import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartSdkManager;

import static com.google.dart.tools.core.analysis.AnalysisUtility.equalsOrContains;
import static com.google.dart.tools.core.analysis.AnalysisUtility.isSdkLibrary;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Analysis of Dart source saved on disk
 */
public class SavedContext extends Context {

  /**
   * A mapping of application directories (directories containing a "packages" directory) to current
   * package contexts. This should only be accessed on the background thread.
   */
  private HashMap<File, PackageContext> packageContexts = new HashMap<File, PackageContext>();

  private AnalysisListener[] analysisListeners = new AnalysisListener[0];

  SavedContext(AnalysisServer server, PackageLibraryManager libraryManager) {
    super(server, libraryManager);
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
   * Look up the directory hierarchy for a pre-existing context
   * 
   * @param libFileOrDir the library file or directory
   * @return the context in which the specified library should be analyzed (not <code>null</code>)
   */
  public Context getSuggestedContext(File libFileOrDir) {
    File dir = libFileOrDir;
    while (dir != null) {
      PackageContext context = packageContexts.get(dir);
      if (context != null) {
        return context;
      }
      dir = dir.getParentFile();
    }
    return this;
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
    server.queueNewTask(new ParseRequestTask(server, libraryFile, relPath, dartFile, callback));
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
    server.queueNewTask(new AnalyzeLibraryTask(server, libraryFile, callback));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  /**
   * Discard all package contexts in addition to all libraries without notifying any listeners about
   * discarded libraries
   */
  @Override
  void discardAllLibraries() {
    packageContexts.clear();
    super.discardAllLibraries();
  }

  /**
   * If the specified file is a directory, then discard all libraries and package contexts in that
   * directory tree otherwise discard the specified library. In both cases, discard all libraries
   * that directly or indirectly reference the discarded libraries.
   * 
   * @param rootFile the original file or directory to discard
   * @param discarded the collection of discarded libraries and to which newly discarded libraries
   *          should be added (not <code>null</code>, contains no <code>null</code>s)
   */
  @Override
  void discardLibraries(File rootFile, ArrayList<Library> discarded) {
    super.discardLibraries(rootFile, discarded);
    Iterator<PackageContext> iter = packageContexts.values().iterator();
    while (iter.hasNext()) {
      PackageContext context = iter.next();
      context.discardLibraries(rootFile, discarded);
      if (equalsOrContains(rootFile, context.getApplicationDirectory())) {
        iter.remove();
      }
    }
  }

  @Override
  File getApplicationDirectory() {
    return null;
  }

  /**
   * Answer a collection of zero or more libraries for the specified file.
   * 
   * @return the cached libraries (not <code>null</code>, contains no <code>null</code>s)
   */
  Library[] getCachedLibraries(File libFile) {
    Library[] result = getCachedLibrariesInPackageContexts(libFile);
    Library lib = getCachedLibrary(libFile);
    if (lib != null) {
      result = AnalysisUtility.append(result, lib);
    }
    return result;
  }

  /**
   * Answer a collection of zero or more libraries for the specified file cached in a
   * {@link PackageContext} but not in the receiver.
   * 
   * @return the cached libraries (not <code>null</code>, contains no <code>null</code>s)
   */
  Library[] getCachedLibrariesInPackageContexts(File libFile) {
    Library[] result = Library.NONE;
    for (PackageContext context : packageContexts.values()) {
      Library lib = context.getCachedLibrary(libFile);
      if (lib != null) {
        result = AnalysisUtility.append(result, lib);
      }
    }
    return result;
  }

  /**
   * Append all libraries from all contexts that source the specified file to the specified array.
   * 
   * @return an array of libraries (not <code>null</code>, contains no <code>null</code>s)
   */
  @Override
  Library[] getLibrariesSourcing(File file, Library[] result) {
    for (PackageContext context : packageContexts.values()) {
      result = context.getLibrariesSourcing(file, result);
    }
    return super.getLibrariesSourcing(file, result);
  }

  /**
   * Answer the package context for the specified application directory, creating and caching a new
   * one if one does not already exist.
   * 
   * @param applicationDirectory a directory containing a "packages" directory
   * @return the cached or created package context
   */
  PackageContext getOrCreatePackageContext(File applicationDirectory) {
    PackageContext context = packageContexts.get(applicationDirectory);
    if (context == null) {
      context = new PackageContext(server, applicationDirectory);
      packageContexts.put(applicationDirectory, context);
    }
    return context;
  }

  /**
   * Answer the currently cached and resolved SDK libraries
   */
  HashMap<URI, LibraryUnit> getResolvedSdkLibraries() {
    HashMap<URI, LibraryUnit> resolvedLibs = new HashMap<URI, LibraryUnit>();
    for (Library lib : getCachedLibraries()) {
      LibraryUnit libUnit = lib.getLibraryUnit();
      if (libUnit != null && isSdkLibrary(lib.getFile())) {
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
        library.getContext().getApplicationDirectory(),
        library.getFile(),
        library.getSourceFiles(),
        AnalysisError.NONE);
    for (AnalysisListener listener : analysisListeners) {
      try {
        listener.discarded(event);
      } catch (Throwable e) {
        DartCore.logError("Exception during discard notification", e);
      }
    }
  }

  /**
   * Notify listeners of Dart units that were parsed
   */
  void notifyParsed(AnalysisEvent event) {
    for (AnalysisListener listener : analysisListeners) {
      try {
        listener.parsed(event);
      } catch (Throwable e) {
        DartCore.logError("Exception during parsed notification", e);
      }
    }
  }

  /**
   * Notify listeners of Dart libraries that were resolved
   */
  void notifyResolved(AnalysisEvent event) {
    for (AnalysisListener listener : analysisListeners) {
      try {
        listener.resolved(event);
      } catch (Throwable e) {
        DartCore.logError("Exception during resolved notification", e);
      }
    }
  }

  /**
   * Reload cached libraries for the receiver and the specified number of package contexts
   */
  void readCache(CacheReader cacheReader, int packageContextCount) throws IOException {
    readCache(cacheReader);
    for (int i = 0; i < packageContextCount; i++) {
      File appDir = new File(cacheReader.readString());
      Context context = getOrCreatePackageContext(appDir);
      context.readCache(cacheReader);
    }
  }

  @Override
  void writeCache(CacheWriter writer) {
    writer.writeInt(packageContexts.size());
    super.writeCache(writer);
    for (PackageContext context : packageContexts.values()) {
      writer.writeString(context.getApplicationDirectory().getPath());
      context.writeCache(writer);
    }
  }
}
