/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartIgnoreEvent;
import com.google.dart.tools.core.model.DartIgnoreListener;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * The unique instance of the class <code>DartIgnoreManager</code> is used to manage the ignored
 * elements in the Dart element model.
 * 
 * @coverage dart.tools.core.model
 */
public class DartIgnoreManager {

  private static final String IGNORE_FILE_NAME = ".dartignore";

  private static final DartIgnoreManager INSTANCE = new DartIgnoreManager();

  public static final String[] DEFAULT_IGNORE_REGEX = new String[] {
      // Ignore pub build output
      ".*/build(/.*)?",
      // Ignore dart2js generated files
      ".*\\.js\\.info\\.html" //
  };

  private static final Pattern[] DEFAULT_IGNORE_PATTERNS = new Pattern[DEFAULT_IGNORE_REGEX.length];
  static {
    for (int i = 0; i < DEFAULT_IGNORE_PATTERNS.length; i++) {
      DEFAULT_IGNORE_PATTERNS[i] = Pattern.compile(DEFAULT_IGNORE_REGEX[i]);
    }
  }

  public static final DartIgnoreManager getInstance() {
    return INSTANCE;
  }

  /**
   * Return {@code true} if the absolute path for the given resource is included in the default
   * collection of paths to be ignored.
   * 
   * @param resource the resource
   * @return {@code true} if the resource is ignored by default
   */
  public static boolean isIgnoredByDefault(IResource resource) {
    return isIgnoredByDefault(getPathPattern(resource));
  }

  /**
   * Return {@code true} if a path is included in the default collection of paths to be ignored.
   * 
   * @param absolutePath the platform independent absolute path being tested. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   * @return <code>true</code> if the given path should be ignored
   */
  public static boolean isIgnoredByDefault(String absolutePath) {
    if (absolutePath != null) {
      for (Pattern pattern : DEFAULT_IGNORE_PATTERNS) {
        if (pattern.matcher(absolutePath).matches()) {
          return true;
        }
      }
    }
    return false;
  }

  private static String getPathPattern(File file) {
    return file != null ? file.getAbsolutePath().replace(File.separatorChar, '/') : null;
  }

  private static String getPathPattern(IPath location) {
    return location != null ? location.toPortableString() : null;
  }

  private static String getPathPattern(IResource resource) {
    return resource != null ? getPathPattern(resource.getLocation()) : null;
  }

  /**
   * Stores the patterns indicating which elements should be ignored (not {@code null}). Call
   * {@link #loadContent()} before accessing this field.
   */
  private final DartIgnoreFile storage;

  /**
   * Objects to be notified when the exclusion list changes.
   */
  private final ListenerList listeners = new ListenerList();

  /**
   * A list of exclusion patterns that are to be applied to determine which files are not currently
   * being analyzed, or <code>null</code> if the patterns have not yet been read from disk.
   */
  private ArrayList<String> exclusionPatterns;

  public DartIgnoreManager() {
    this(new DartIgnoreFile(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(
        IGNORE_FILE_NAME).toFile()));
  }

  public DartIgnoreManager(DartIgnoreFile storage) {
    this.storage = storage;
  }

  /**
   * Add the given listener for dart ignore changes to the Dart Model. Has no effect if an identical
   * listener is already registered.
   * 
   * @param listener the listener to add
   */
  public void addListener(DartIgnoreListener listener) {
    listeners.add(listener);
  }

  /**
   * Add the specified file to the list of ignores. Callers are responsible for deleting any
   * existing markers on ignored resources.
   * 
   * @param file the file to ignore
   * @return {@code true} if the list of ignores has changed
   */
  public boolean addToIgnores(File file) throws IOException {
    return addToIgnores(getPathPattern(file));
  }

  /**
   * Add the specified path to the list of ignores. Callers are responsible for deleting any
   * existing markers on ignored resources.
   * 
   * @param absolutePath the absolute path to ignore
   * @return {@code true} if the list of ignores has changed
   */
  public boolean addToIgnores(IPath absolutePath) throws IOException {
    return addToIgnores(getPathPattern(absolutePath));
  }

  /**
   * Add the path for the given resource to the list of ignores. Existing Dart problem markers on
   * the specified resource will be removed.
   * 
   * @param resource the resource to ignore
   * @return {@code true} if the list of ignores has changed
   * @throws IOException if there was an error accessing the ignore file
   * @throws CoreException if there was an error deleting markers
   */
  public boolean addToIgnores(IResource resource) throws IOException, CoreException {
    return addToIgnores(new IResource[] {resource});
  }

  /**
   * Add the paths for the given resources to the list of ignores. Existing Dart problem markers on
   * the specified resources will be removed.
   * 
   * @param resources the resources to ignore
   * @return {@code true} if the list of ignores has changed
   * @throws IOException if there was an error accessing the ignore file
   * @throws CoreException if there was an error deleting markers
   */
  public boolean addToIgnores(IResource[] resources) throws IOException, CoreException {
    String[] paths = new String[resources.length];
    for (int index = 0; index < paths.length; index++) {
      final IResource resource = resources[index];
      paths[index] = getPathPattern(resource);
      // TODO (danrubel): Move delete markers into ProjectManager listener
      if (resource != null) {
        resource.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
      }
    }
    return addToIgnores(paths);
  }

  /**
   * Add the specified path to the list of ignores. Callers are responsible for deleting any
   * existing markers on ignored resources.
   * 
   * @param absolutePath the platform independent absolute path being tested. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   * @return {@code true} if the list of ignores has changed
   */
  public boolean addToIgnores(String absolutePath) throws IOException {
    return addToIgnores(new String[] {absolutePath});
  }

  /**
   * Add the specified paths to the list of ignores. Callers are responsible for deleting any
   * existing markers on ignored resources.
   * 
   * @param absolutePaths the platform independent absolute paths being tested. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   * @return {@code true} if the list of ignores has changed
   */
  public boolean addToIgnores(String[] absolutePaths) throws IOException {
    if (absolutePaths != null) {
      boolean modified = false;
      for (String absolutePath : absolutePaths) {
        if (absolutePath != null) {
          loadContent();
          if (storage.add(absolutePath)) {
            modified = true;
          }
        }
      }
      if (modified) {
        cacheExclusions();
        storage.store();
        notifyListeners(new DartIgnoreEvent(absolutePaths, new String[] {}));
        return true;
      }
    }
    return false;
  }

  /**
   * Return a list of exclusion patterns that are to be applied to determine which files are not
   * currently being analyzed.
   * 
   * @return the exclusion patterns used to determine which files are not currently being analyzed
   */
  public ArrayList<String> getExclusionPatterns() {
    loadContent();
    return exclusionPatterns;
  }

  /**
   * Determine if the specified file should be analyzed. This means that the file is not
   * {@code null}, the file exists, and the file's location is not included in the collection of
   * paths to be ignored.
   * 
   * @param file the file
   * @return {@code true} if the file should be analyzed
   */
  public boolean isAnalyzed(File file) {
    return file != null && file.exists() && isAnalyzed(getPathPattern(file));
  }

  /**
   * Determine if the file or resource with the specified path should be analyzed. This means that
   * the specified path is not {@code null} and is not included in the collection of paths to be
   * ignored.
   * 
   * @param absolutePath the platform independent absolute path being tested. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   * @return {@code true} if the file or resource represented by this path should be analyzed
   */
  public boolean isAnalyzed(IPath absolutePath) {
    return absolutePath != null && !isIgnored(absolutePath);
  }

  /**
   * Determine if the specified resource should be analyzed. This means that the resource is not
   * {@code null}, the resource location is not {@code null}, the resource exists, and the
   * resource's location is not included in the collection of paths to be ignored.
   * 
   * @param resource the resource
   * @return {@code true} if the resource should be analyzed
   */
  public boolean isAnalyzed(IResource resource) {
    return resource != null && resource.exists() && isAnalyzed(getPathPattern(resource));
  }

  /**
   * Determine if the file or resource with the specified path should be analyzed. This means that
   * the specified path is not {@code null} and is not included in the collection of paths to be
   * ignored.
   * 
   * @param absolutePath the platform independent absolute path being tested. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   * @return {@code true} if the file or resource represented by this path should be analyzed
   */
  public boolean isAnalyzed(String absolutePath) {
    return absolutePath != null && !isIgnored(absolutePath);
  }

  /**
   * Return <code>true</code> if the specified file's path is included in the collection of paths to
   * be ignored.
   * 
   * @param file the file being tested
   * @return <code>true</code> if the given file should be ignored
   */
  public boolean isIgnored(File file) {
    return isIgnored(getPathPattern(file));
  }

  /**
   * Return <code>true</code> if the specified path is included in the collection of paths to be
   * ignored.
   * 
   * @param path the file being tested
   * @return <code>true</code> if the given file should be ignored
   */
  public boolean isIgnored(IPath path) {
    return isIgnored(getPathPattern(path));
  }

  /**
   * Return <code>true</code> if the specified resource's path is included in the collection of
   * paths to be ignored.
   * 
   * @param resource the resource being tested
   * @return <code>true</code> if the given resource should be ignored
   */
  public boolean isIgnored(IResource resource) {
    return isIgnored(getPathPattern(resource));
  }

  /**
   * Return <code>true</code> if the path is included in the collection of paths to be ignored.
   * 
   * @param absolutePath the platform independent absolute path being tested. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   * @return <code>true</code> if the given path should be ignored
   */
  public boolean isIgnored(String absolutePath) {
    if (absolutePath != null) {
      // TODO(brianwilkerson) Re-implement this once the real semantics have been decided on.
      ArrayList<String> patterns = getExclusionPatterns();
      if (patterns.size() > 0) {
        for (String pattern : patterns) {
          // TODO(brianwilkerson) Replace this with some form of pattern matching.
          if (absolutePath.equals(pattern) || absolutePath.startsWith(pattern + "/")) {
            return true;
          }
        }
      }
      return isIgnoredByDefault(absolutePath);
    }
    return false;
  }

  /**
   * Remove the file's path for the given resource from the list of ignores.
   * 
   * @param file the file to (un)ignore
   * @return {@code true} if the list of ignores has changed
   * @throws IOException if there was an error accessing the ignore file
   */
  public boolean removeFromIgnores(File file) throws IOException {
    return removeFromIgnores(getPathPattern(file));
  }

  /**
   * Remove the path from the list of ignores.
   * 
   * @param absolutePath the absolute path being removed.
   * @return {@code true} if the list of ignores has changed
   * @throws IOException if there was an error accessing the ignore file
   */
  public boolean removeFromIgnores(IPath absolutePath) throws IOException {
    return removeFromIgnores(getPathPattern(absolutePath));
  }

  /**
   * Remove the path for the given resource from the list of ignores.
   * 
   * @param resource the resource to (un)ignore
   * @return {@code true} if the list of ignores has changed
   * @throws IOException if there was an error accessing the ignore file
   */
  public boolean removeFromIgnores(IResource resource) throws IOException {
    return removeFromIgnores(new IResource[] {resource});
  }

  /**
   * Remove the paths for the given resources from the list of ignores.
   * 
   * @param resources the resources to (un)ignore
   * @return {@code true} if the list of ignores has changed
   * @throws IOException if there was an error accessing the ignore file
   */
  public boolean removeFromIgnores(IResource[] resources) throws IOException {
    String[] paths = new String[resources.length];
    for (int index = 0; index < paths.length; index++) {
      paths[index] = getPathPattern(resources[index]);
    }
    return removeFromIgnores(paths);
  }

  /**
   * Remove the path from the list of ignores.
   * 
   * @param absolutePath the platform independent absolute path being removed. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   * @return {@code true} if the list of ignores has changed
   * @throws IOException if there was an error accessing the ignore file
   */
  public boolean removeFromIgnores(String absolutePath) throws IOException {
    return removeFromIgnores(new String[] {absolutePath});
  }

  /**
   * Remove the paths from the list of ignores.
   * 
   * @param absolutePaths the platform independent absolute paths being removed. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   * @return {@code true} if the list of ignores has changed
   * @throws IOException if there was an error accessing the ignore file
   */
  public boolean removeFromIgnores(String[] absolutePaths) throws IOException {
    if (absolutePaths != null) {
      Collection<String> removed = new ArrayList<String>();
      for (String absolutePath : absolutePaths) {
        if (absolutePath != null) {
          loadContent();
          removed.addAll(storage.remove(absolutePath));
        }
      }
      if (removed.size() > 0) {
        cacheExclusions();
        storage.store();
        notifyListeners(new DartIgnoreEvent(new String[] {}, absolutePaths));
        return true;
      }
    }
    return false;
  }

  /**
   * Remove the given listener for dart ignore changes from the Dart Model. Has no effect if an
   * identical listener is not registered.
   * 
   * @param listener the non-<code>null</code> listener to remove
   */
  public void removeListener(DartIgnoreListener listener) {
    listeners.remove(listener);
  }

  /**
   * If the underlying storage has changed, recache the {@link #exclusionPatterns}.
   */
  private void cacheExclusions() {
    if (exclusionPatterns == null) {
      exclusionPatterns = new ArrayList<String>();
    } else {
      exclusionPatterns.clear();
    }
    exclusionPatterns.addAll(storage.getPatterns());
  }

  /**
   * Initialize the receiver's content if not already initialized.
   */
  private void loadContent() {
    // TODO(brianwilkerson) Re-implement this once the real semantics have been decided on.
    if (exclusionPatterns == null) {
      exclusionPatterns = new ArrayList<String>();
      try {
        storage.initFile();
        storage.load();
        cacheExclusions();
      } catch (IOException exception) {
        DartCore.logInformation("Could not read ignore file from workspace", exception);
      }
    }
  }

  /**
   * Notify listeners that the exclusion patterns have changed.
   */
  private void notifyListeners(DartIgnoreEvent event) {
    for (Object listener : listeners.getListeners()) {
      ((DartIgnoreListener) listener).ignoresChanged(event);
    }
  }
}
