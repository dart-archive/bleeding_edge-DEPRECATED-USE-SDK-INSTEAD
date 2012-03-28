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

import com.google.dart.tools.core.utilities.io.FileUtilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a list of patterns that describe resources to be ignored from analysis.
 */
public class DartIgnoreFile {

  private static final Object NEW_LINE = System.getProperty("line.separator");

  /* public for testing */
  public static Collection<String> getSubsumedPatterns(String pattern, Iterable<String> ignores) {
    ArrayList<String> containers = new ArrayList<String>();
    for (String ignore : ignores) {
      if (ignore.startsWith(pattern)) {
        containers.add(ignore);
      }
    }
    return containers;
  }

  /* public for testing */
  public static boolean isSubsumedIn(String pattern, Iterable<String> ignores) {
    for (String ignore : ignores) {
      if (pattern.startsWith(ignore)) {
        return true;
      }
    }
    return false;
  }

  private final Set<String> ignores = new HashSet<String>();

  private final File file;

  /**
   * Create an instance.
   * 
   * @param file the backing file
   */
  public DartIgnoreFile(File file) {
    this.file = file;
  }

  /**
   * Add the given pattern to the list.
   * 
   * @param pattern the pattern to add
   * @return a reference to this ignore file
   */
  public DartIgnoreFile add(String pattern) {

    //only add if not subsumed by an existing pattern
    if (!isSubsumedIn(pattern, ignores)) {
      //remove subsumed patterns
      Collection<String> submsumed = getSubsumedPatterns(pattern, ignores);
      ignores.removeAll(submsumed);
      ignores.add(pattern);
    }
    return this;
  }

  /**
   * Get the list of patterns.
   * 
   * @return the list of patterns.
   */
  public Collection<String> getPatterns() {
    return ignores;
  }

  /**
   * Load contents from the backing file.
   * 
   * @return a reference to this ignore file
   * @throws IOException if an error occurred when reading from the backing file.
   */
  public DartIgnoreFile load() throws IOException {

    if (file.exists()) {
      BufferedReader reader = new BufferedReader(new StringReader(FileUtilities.getContents(file)));
      String nextLine = reader.readLine();
      while (nextLine != null) {
        add(nextLine);
        nextLine = reader.readLine();
      }
    }
    return this;
  }

  /**
   * Remove this pattern from the list of ignores.
   * <p>
   * <b>NOTE:</b>Provisionally a remove is careful to ensure that patterns that contain the target
   * pattern are also removed.
   * </p>
   * 
   * @param pattern the pattern to remove
   * @return a reference to this ignore file
   */
  public DartIgnoreFile remove(String pattern) {

    ignores.remove(pattern);

    //collect all patterns that contain the target pattern
    ArrayList<String> toRemove = new ArrayList<String>();
    for (String p : ignores) {
      if (pattern.startsWith(p)) {
        toRemove.add(p);
      }
    }

    ignores.removeAll(toRemove);

    return this;
  }

  /**
   * Store contents to the backing file.
   * 
   * @return a reference to this ignore file
   * @throws IOException if an error occurred when storing to the backing file.
   */
  public DartIgnoreFile store() throws IOException {

    if (!file.exists()) {
      file.createNewFile();
    }

    StringBuilder sb = new StringBuilder();
    for (String pattern : ignores) {
      sb.append(pattern);
      sb.append(NEW_LINE);
    }

    FileUtilities.setContents(file, sb.toString());

    return this;
  }
}
