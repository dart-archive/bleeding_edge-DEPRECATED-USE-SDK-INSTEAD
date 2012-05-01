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
package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.internal.model.EditorLibraryManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;

import junit.framework.Assert;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class Listener implements AnalysisListener {
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private final HashMap<String, HashSet<String>> parsed = new HashMap<String, HashSet<String>>();
  private final HashSet<String> resolved = new HashSet<String>();
  private final StringBuilder duplicates = new StringBuilder();

  private final ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();

  public Listener(AnalysisServer server) {
    server.addAnalysisListener(this);
  }

  @Override
  public void idle(boolean idle) {
  }

  @Override
  public void parsed(AnalysisEvent event) {
    String libFilePath = event.getLibraryFile().getPath();
    HashSet<String> parsedInLib = parsed.get(libFilePath);
    if (parsedInLib == null) {
      parsedInLib = new HashSet<String>();
      parsed.put(libFilePath, parsedInLib);
    }
    for (File file : event.getFiles()) {
      if (!parsedInLib.add(file.getPath())) {
        if (duplicates.length() > 0) {
          duplicates.append(LINE_SEPARATOR);
        }
        duplicates.append("Duplicate parse: " + file + LINE_SEPARATOR + "  in " + libFilePath);
      }
    }
    errors.addAll(event.getErrors());
  }

  @Override
  public void resolved(AnalysisEvent event) {
    String libPath = event.getLibraryFile().getPath();
    if (!resolved.add(libPath)) {
      if (duplicates.length() > 0) {
        duplicates.append(LINE_SEPARATOR);
      }
      duplicates.append("Duplicate resolution: " + libPath);
    }
    errors.addAll(event.getErrors());
  }

  void assertBundledLibrariesResolved() throws Exception {
    ArrayList<String> notResolved = new ArrayList<String>();
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    ArrayList<String> librarySpecs = new ArrayList<String>(libraryManager.getAllLibrarySpecs());
    for (String urlSpec : librarySpecs) {
      URI libraryUri = new URI(urlSpec);
      File libraryFile = new File(libraryManager.resolveDartUri(libraryUri));
      String libraryPath = libraryFile.getPath();
      if (!resolved.contains(libraryPath)) {
        notResolved.add(libraryPath);
      }
    }
    if (notResolved.size() > 0) {
      AnalysisServerTest.fail("Expected these libraries to be resolved: " + notResolved);
    }
  }

  void assertNoDuplicates() {
    if (duplicates.length() > 0) {
      AnalysisServerTest.fail(duplicates.toString());
    }
  }

  void assertWasParsed(File libFile, File file) {
    HashSet<String> parsedInLib = parsed.get(libFile.getPath());
    if (parsedInLib == null || !parsedInLib.contains(file.getPath())) {
      Assert.fail("Expected parsed file " + file + LINE_SEPARATOR + "  in " + libFile
          + " but found " + (parsedInLib != null ? parsedInLib : parsed));
    }
  }

  void assertWasResolved(File file) {
    if (!resolved.contains(file.getPath())) {
      Assert.fail("Expected parsed library " + file + " but found " + resolved);
    }
  }

  ArrayList<AnalysisError> getErrors() {
    return errors;
  }

  int getParsedCount() {
    int count = 0;
    for (HashSet<String> parsedInLib : parsed.values()) {
      count += parsedInLib.size();
    }
    return count;
  }

  HashSet<String> getResolved() {
    return resolved;
  }

  void reset() {
    parsed.clear();
    resolved.clear();
    duplicates.setLength(0);
    errors.clear();
  }
}
