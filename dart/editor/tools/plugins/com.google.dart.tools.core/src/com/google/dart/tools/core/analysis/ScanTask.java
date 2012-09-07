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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Scan the specified folder and all subfolders for Dart libraries. Add any libraries and loose dart
 * files found to the {@link AnalysisServer}.
 */
public class ScanTask extends Task implements TaskListener {

  public enum DartFileType {
    Library,
    PartOf,
    Unknown
  };

  /**
   * Scan the input stream for a directive indicating that the Dart file is a library
   */
  public static DartFileType scanContent(InputStream in, byte[] buffer) throws IOException {
    int state = 0;
    int nestedCommentLevel = 0;
    while (true) {

      int count = in.read(buffer);
      if (count == -1) {
        return DartFileType.Unknown;
      }

      int index = 0;
      while (index < count) {
        byte ch = buffer[index++];
        switch (state) {

          case 0: // scan for '#' indicating a directive
            if (ch == '#') {
              return DartFileType.Library;
            }
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
              state = 0;
              break;
            }
            if (ch == '/') { // comment start
              state = 1;
              break;
            }
            if (ch == 'l' && matchIdentifier(buffer, index - 1, "library")) {
              return DartFileType.Library;
            }
            if (ch == 'i' && matchIdentifier(buffer, index - 1, "import")) {
              return DartFileType.Library;
            }
            if (ch == 'p' && matchIdentifier(buffer, index - 1, "part")) {
              index += 3;
              state = 6;
              break;
            }
            return DartFileType.Unknown;

          case 1: // scan single line or multi line comment
            if (ch == '/') { // single line comment start
              state = 2;
              break;
            }
            if (ch == '*') { // multi line comment start
              state = 3;
              break;
            }
            return DartFileType.Unknown;

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

          case 6: // skipping white space after "part" looking for "part of"
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
              break;
            }
            if (matchIdentifier(buffer, index - 1, "of")) {
              return DartFileType.PartOf;
            }
            if (ch == '\'' || ch == '"') {
              return DartFileType.Library;
            }
            return DartFileType.Unknown;

          default:
            throw new IllegalStateException("invalid state: " + state);
        }
      }
    }
  }

  /**
   * Answer <code>true</code> if the buffer contains the specified identifier starting at the
   * specified index
   */
  private static boolean matchIdentifier(byte[] buffer, int start, String identifier) {
    int index = start;

    // Match identifer

    for (byte ch : identifier.getBytes()) {
      if (index >= buffer.length || ch != buffer[index]) {
        return false;
      }
      index++;
    }

    // Ensure followed by non identifier character

    if (index >= buffer.length) {
      return false;
    }
    byte ch = buffer[index++];
    return !(('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z') || ('0' <= ch && ch <= '9'));
  }

  private final AnalysisServer server;
  private final File rootFile;
  private final ScanCallback callback;
  private final ArrayList<File> filesToScan = new ArrayList<File>(200);
  private final HashSet<File> libraryFiles = new HashSet<File>(50);
  private final HashSet<File> looseFiles = new HashSet<File>(200);
  private final ArrayList<Library> librariesToAnalyze = new ArrayList<Library>(20);
  private final byte[] buffer = new byte[1024];

  private final DartIgnoreManager ignoreManager;
  private int count = 1;

  private float progress = 0;

  ScanTask(AnalysisServer server, File rootFile, ScanCallback callback) {
    this.server = server;
    this.rootFile = rootFile;
    this.callback = callback;
    this.filesToScan.add(rootFile);
    this.ignoreManager = DartIgnoreManager.getInstance();
  }

  /**
   * Called by the analysis server when all tasks have been performed
   */
  @Override
  public void idle(boolean idle) {
    server.removeIdleListener(this);
  }

  @Override
  public boolean isBackgroundAnalysis() {
    return callback == null || callback.isCanceled();
  }

  @Override
  public boolean isPriority() {
    return false;
  }

  @Override
  public void perform() {
    SavedContext savedContext = server.getSavedContext();

    // Scan for files defining libraries

    while (true) {
      int size = filesToScan.size();
      if (size == 0) {
        break;
      }
      scanFile(filesToScan.remove(size - 1));

      // Report progress and if canceled then mark the root to be ignored and abort the scan
      if (callback != null) {
        count++;
        progress = (float) (Math.log(count) / 100.0);
        callback.progress(progress);
        if (callback.isCanceled()) {
          discardScanResults();
          return;
        }
      }
    }

    // Hook up a listener to report analysis progress back to the caller

    if (callback != null) {
      server.addIdleListener(this);
    }

    Iterator<File> iter = libraryFiles.iterator();
    while (iter.hasNext()) {

      // If canceled, then mark the root to be ignored and abort the scan

      if (callback != null && callback.isCanceled()) {
        discardScanResults();
        return;
      }

      // Parse libraries that have not been parsed

      File libFile = iter.next();
      Library lib = savedContext.getCachedLibrary(libFile);
      if (lib == null) {
        queueParseTask(libFile);
        continue;
      }

      // Exclude source files

      librariesToAnalyze.add(lib);
      looseFiles.removeAll(lib.getSourceFiles());
      for (File sourcedFile : lib.getSourceFiles()) {
        Library otherLib = savedContext.getCachedLibrary(sourcedFile);
        // If there is no information about the sourcedFile currently in the cache
        // make the assumption that it is not a library file rather than doing more work
        if (otherLib == null || !otherLib.hasDirectives()) {
          server.discard(sourcedFile);
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
      if (savedContext.getLibrariesSourcing(file).length == 0) {
        queueParseTask(file);
        server.analyze(file);
      }
    }

    // If canceled, then mark the root to be ignored and abort the scan

    if (callback != null && callback.isCanceled()) {
      discardScanResults();
      return;
    }
  }

  /**
   * Called by the analysis server, and used by the receiver to report progress to the receiver's
   * callback.
   */
  @Override
  public void processing(int toBeProcessed) {

    // If canceled, then mark the root to be ignored and abort the scan

    if (callback != null && callback.isCanceled()) {
      discardScanResults();
      return;
    }

    // Report progress via the callback

    if (count > 1) {
      count--;
    }
    progress += (0.98 - progress) / (toBeProcessed + count);
    callback.progress(progress);
    if (toBeProcessed == 0) {
      callback.scanComplete();
    }
  }

  void addFilesToScan(Collection<File> files) {
    filesToScan.addAll(files);
  }

  void addFilesToScan(File... files) {
    Collections.addAll(filesToScan, files);
  }

  private void discardScanResults() {
    server.removeIdleListener(this);
    if (callback != null) {
      callback.scanCanceled(rootFile);
    }
    server.discard(rootFile);
    return;
  }

  private void queueParseTask(File libFile) {
    File libDir = libFile.getParentFile();
    if (DartCore.containsPackagesDirectory(libDir)) {
      server.getSavedContext().getOrCreatePackageContext(libDir);
    }
    server.queueSubTask(new ParseTask(server, server.getSavedContext(), libFile, null));
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
      if (!DartCore.isPackagesDirectory(file)) {
        filesToScan.addAll(Arrays.asList(file.listFiles()));
      }
      return;
    }
    if (!DartCore.isDartLikeFileName(file.getName())) {
      return;
    }

    // Check if the file contains directives

    FileInputStream in;
    try {
      in = new FileInputStream(file);
    } catch (FileNotFoundException e) {
      DartCore.logInformation("Failed to open for scan: " + file);
      return;
    }
    try {
      switch (scanContent(in, buffer)) {
        case Library:
          libraryFiles.add(file);
          break;
        case PartOf:
          // Will be analyzed as part of analyzing the library containing it
          break;
        case Unknown:
          looseFiles.add(file);
          break;
        default:
          looseFiles.add(file);
          break;
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
