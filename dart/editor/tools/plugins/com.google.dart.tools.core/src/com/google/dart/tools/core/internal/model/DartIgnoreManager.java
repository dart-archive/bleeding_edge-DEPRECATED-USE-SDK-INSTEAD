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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The unique instance of the class <code>DartModelManager</code> is used to manage the ignored
 * elements in the Dart element model.
 */
public class DartIgnoreManager {

  private static final String IGNORE_FILE_NAME = ".dartignore";

  private static final DartIgnoreManager INSTANCE = new DartIgnoreManager();

  public static final DartIgnoreManager getInstance() {
    return INSTANCE;
  }

  private static File getIgnoreFile() throws IOException {
    File file = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(IGNORE_FILE_NAME).toFile();
    if (!file.exists()) {
      file.createNewFile();
    }
    if (!file.canRead()) {
      file.setReadable(true);
    }
    if (!file.canWrite()) {
      file.setWritable(true);
    }
    return file;
  }

  private static String getPathPattern(IResource resource) {
    return resource.getLocation().toPortableString();
  }

  private static DartIgnoreFile loadIgnoreFile() throws IOException {
    File patternFile = getIgnoreFile();
    return new DartIgnoreFile(patternFile).load();
  }

  /**
   * A list of exclusion patterns that are to be applied to determine which files are not currently
   * being analyzed, or <code>null</code> if the patterns have not yet been read from disk.
   */
  private ArrayList<String> exclusionPatterns;

  /**
   * Add the specified path to the list of ignores. Callers are responsible for deleting any
   * existing markers on ignored resources.
   * 
   * @param absolutePath the platform independent absolute path being tested. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   */
  public void addToIgnores(File file) throws IOException {
    addToIgnores(file.getAbsolutePath().replace(File.separatorChar, '/'));
  }

  /**
   * Add the specified path to the list of ignores. Callers are responsible for deleting any
   * existing markers on ignored resources.
   * 
   * @param absolutePath the platform independent absolute path being tested. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   */
  public void addToIgnores(String absolutePath) throws IOException {
    DartIgnoreFile ignoreFile = loadIgnoreFile();
    ignoreFile.add(absolutePath);
    cacheExclusions(ignoreFile);
    ignoreFile.store();
  }

  /**
   * Return a list of exclusion patterns that are to be applied to determine which files are not
   * currently being analyzed.
   * 
   * @return the exclusion patterns used to determine which files are not currently being analyzed
   */
  public ArrayList<String> getExclusionPatterns() {
    // TODO(brianwilkerson) Re-implement this once the real semantics have been decided on.
    if (exclusionPatterns == null) {
      exclusionPatterns = new ArrayList<String>();
      try {
        DartIgnoreFile ignoreFile = loadIgnoreFile();
        exclusionPatterns.addAll(ignoreFile.getPatterns());
      } catch (IOException exception) {
        DartCore.logInformation("Could not read ignore file from workspace", exception);
      }
    }
    return exclusionPatterns;
  }

  /**
   * Return <code>true</code> if the specified file's path is included in the collection of paths to
   * be ignored.
   * 
   * @param file the file being tested
   * @return <code>true</code> if the given file should be ignored
   */
  public boolean isIgnored(File file) {
    return isIgnored(file.getAbsolutePath().replace(File.separatorChar, '/'));
  }

  /**
   * Return <code>true</code> if the path is included in the collection of paths to be ignored.
   * 
   * @param absolutePath the platform independent absolute path being tested. On Windows, any '\'
   *          must be converted to '/' before calling this method.
   * @return <code>true</code> if the given path should be ignored
   */
  public boolean isIgnored(String absolutePath) {
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
    return false;
  }

  /**
   * Add the path for the given resource to the list of ignores.
   * 
   * @param resource the resource to ignore
   * @throws IOException if there was an error accessing the ignore file
   * @throws CoreException if there was an error deleting markers
   */
  protected void addToIgnores(IResource resource) throws IOException, CoreException {
    addToIgnores(getPathPattern(resource));
    resource.deleteMarkers(null /* all types */, true, IResource.DEPTH_INFINITE);
  }

  /**
   * Remove the path for the given resource from the list of ignores.
   * 
   * @param resource the resource to (un)ignore
   * @throws IOException if there was an error accessing the ignore file
   */
  protected void removeFromIgnores(IResource resource) throws IOException {

    DartIgnoreFile ignoreFile = loadIgnoreFile();
    String path = getPathPattern(resource);
    ignoreFile.remove(path);
    cacheExclusions(ignoreFile);
    ignoreFile.store();
  }

  private void cacheExclusions(DartIgnoreFile ignoreFile) {
    if (exclusionPatterns == null) {
      exclusionPatterns = new ArrayList<String>();
    } else {
      exclusionPatterns.clear();
    }
    exclusionPatterns.addAll(ignoreFile.getPatterns());
  }
}
