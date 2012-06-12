/*
 * Copyright 2012 Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this libraryFile
 * except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Scan the specified folder and all subfolders for Dart libraries. Add any libraries and loose dart
 * files found to the {@link AnalysisServer}.
 */
public class LibraryScanTask extends Task {
  private static final long SCAN_BYTE_THRESHOLD = 10 * 1000 * 1024; // 10 MB
  private static final long SCAN_TIME_THRESHOLD = 10 * 1000; // ten seconds

  private final AnalysisServer server;
  private final Context context;
  private final File rootFile;
  private final boolean fullScan;
  private final ArrayList<File> filesToScan = new ArrayList<File>(200);
  private final HashSet<File> libraryFiles = new HashSet<File>(50);
  private final HashSet<File> looseFiles = new HashSet<File>(200);
  private final ArrayList<Library> librariesToAnalyze = new ArrayList<Library>(20);
  private final byte[] buffer = new byte[1024];

  private final DartIgnoreManager ignoreManager;

  private long bytesOfCode = 0;
  private long scanEndThreshold = 0;

  LibraryScanTask(AnalysisServer server, Context context, File rootFile, boolean fullScan) {
    this.server = server;
    this.context = context;
    this.rootFile = rootFile;
    this.fullScan = fullScan;
    this.filesToScan.add(rootFile);
    this.ignoreManager = DartIgnoreManager.getInstance();
  }

  @Override
  boolean isBackgroundAnalysis() {
    return false;
  }

  @Override
  boolean isPriority() {
    return false;
  }

  @Override
  void perform() {
    if (scanEndThreshold == 0) {
      scanEndThreshold = System.currentTimeMillis() + SCAN_TIME_THRESHOLD;
    }

    // Scan for files defining libraries

    while (true) {
      int size = filesToScan.size();
      if (size == 0) {
        break;
      }
      scanFile(filesToScan.remove(size - 1));

      // If over the scan threshold, then mark the root to be ignored and abort the scan

      if (!fullScan) {
        if (bytesOfCode > SCAN_BYTE_THRESHOLD || System.currentTimeMillis() > scanEndThreshold) {
          try {
            ignoreManager.addToIgnores(rootFile);
          } catch (IOException e) {
            DartCore.logError("Failed to ignore " + rootFile, e);
          }
          server.discard(rootFile);
          return;
        }
      }
    }

    // Parse libraries to determine sourced files

    Iterator<File> iter = libraryFiles.iterator();
    while (iter.hasNext()) {
      File libFile = iter.next();
      Library lib = context.getCachedLibrary(libFile);
      if (lib == null) {
        server.queueSubTask(new ParseLibraryFileTask(server, context, libFile, null));
        continue;
      }
      librariesToAnalyze.add(lib);
      looseFiles.removeAll(lib.getSourceFiles());
      for (File sourcedFile : lib.getSourceFiles()) {
        Library otherLib = context.getCachedLibrary(sourcedFile);
        if (otherLib != null && !otherLib.hasDirectives()) {
          server.discard(otherLib.getFile());
        }
      }
      iter.remove();
    }
    if (libraryFiles.size() > 0) {
      server.queueSubTask(this);
      return;
    }

    // Analyze libraries and remaining loose files

    for (Library lib : librariesToAnalyze) {
      server.analyze(lib.getFile());
    }
    for (File file : looseFiles) {
      if (context.getLibrariesContaining(file).length == 0) {
        server.analyze(file);
      }
    }
  }

  /**
   * Scan the input stream for a directive indicating that the Dart file is a library
   */
  private boolean hasDirective(InputStream in) throws IOException {
    int state = 0;
    int nestedCommentLevel = 0;
    while (true) {

      int count = in.read(buffer);
      if (count == -1) {
        return false;
      }

      int index = 0;
      while (index < count) {
        byte ch = buffer[index++];
        switch (state) {

          case 0: // scan for '#' indicating a directive
            if (ch == '#') {
              return true;
            }
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
              state = 0;
              break;
            }
            if (ch == '/') { // comment start
              state = 1;
              break;
            }
            return false;

          case 1: // scan single line or multi line comment
            if (ch == '/') { // single line comment start
              state = 2;
              break;
            }
            if (ch == '*') { // multi line comment start
              state = 3;
              break;
            }
            return false;

          case 2: // scan single line comment
            if (ch == '\r' || ch == '\n') {
              state = 0;
              break;
            }
            break;

          case 3: // scan for multi line comment which may be nested
            if (ch == '/') { // could be nested multi line comment
              state = 4;
              break;
            }
            if (ch == '*') { // could be end of this comment
              state = 5;
              break;
            }
            break;

          case 4: // possible nested comment
            if (ch == '*') { // nested multi line comment
              nestedCommentLevel++;
              state = 3;
              break;
            }
            if (ch == '/') { // could be a nested multi line comment
              state = 4;
              break;
            }
            state = 3;
            break;

          case 5: // possible end of multi line comment
            if (ch == '/') { // end of multi line comment
              if (nestedCommentLevel == 0) {
                state = 0;
                break;
              }
              nestedCommentLevel--;
              state = 3;
              break;
            }
            if (ch == '*') { // could be end of this comment
              state = 5;
              break;
            }
            state = 3;
            break;

          default:
            throw new IllegalStateException("invalid state: " + state);
        }
      }
    }
  }

  /**
   * Scan the specified file to see if it is a library. If the total number of bytes of source code
   * scanned exceeds the threshold, then abort the scan, mark the root folder as ignored, and
   * discard any cached information.
   */
  private void scanFile(File file) {
    if (ignoreManager.isIgnored(file) || file.getName().startsWith(".")) {
      return;
    }
    if (file.isDirectory()) {
      filesToScan.addAll(Arrays.asList(file.listFiles()));
      return;
    }
    if (!DartCore.isDartLikeFileName(file.getName())) {
      return;
    }

    bytesOfCode += file.length();

    // Check if the file contains directives

    FileInputStream in;
    try {
      in = new FileInputStream(file);
    } catch (FileNotFoundException e) {
      DartCore.logInformation("Failed to open for scan: " + file);
      return;
    }
    try {
      if (hasDirective(in)) {
        libraryFiles.add(file);
      } else {
        looseFiles.add(file);
      }
    } catch (IOException e) {
      DartCore.logInformation("Exception while scanning file: " + file);
      return;
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        DartCore.logInformation("Failed to close after scan: " + file);
      }
    }
  }
}
