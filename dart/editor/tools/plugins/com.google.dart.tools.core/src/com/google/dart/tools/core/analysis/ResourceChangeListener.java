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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Listens for resource changes and forwards them to the {@link AnalysisServer}
 */
public class ResourceChangeListener implements IResourceChangeListener, IResourceDeltaVisitor {

  private static final int DELTA_MASK = IResourceDelta.CONTENT | IResourceDelta.REPLACED;

  private final AnalysisServer server;

  /**
   * A collection of files and directories to be scanned
   */
  private final ArrayList<File> filesToScan;

  /**
   * A flag indicating that the background thread should continue to scan
   */
  private boolean scan;

  /**
   * Performance measurement - scan start time
   */
  private long scanStart;

  /**
   * Performance measurement - number of files scanned
   */
  private int scanCount;

  /**
   * The buffer used by the background thread for scanning files
   */
  private final byte[] buffer;

  /**
   * Construct a new instance that listens for resource changes and forwards that information on to
   * the specified {@link AnalysisServer}
   */
  public ResourceChangeListener(AnalysisServer server) {
    this.server = server;
    this.scan = true;
    this.filesToScan = new ArrayList<File>();
    this.buffer = new byte[1024];

    // Background thread to scan new files and directories
    new Thread(new Runnable() {

      @Override
      public void run() {
        scanFiles();
      }
    }, ResourceChangeListener.class.getSimpleName()).start();

    // Scan all projects
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      File file = project.getLocation().toFile();
      addFileToScan(file);
    }
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    try {
      event.getDelta().accept(this);
    } catch (Exception e) {
      DartCore.logError("Failed to process resource changes for " + event.getResource(), e);
    }
  }

  /**
   * Stop listening for resource changes
   */
  public void stop() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    scan = false;
  }

  @Override
  public boolean visit(IResourceDelta delta) throws CoreException {
    switch (delta.getKind()) {

      case IResourceDelta.ADDED:
        File file = delta.getResource().getLocation().toFile();
        addFileToScan(file);
        return false;

      case IResourceDelta.CHANGED:
        IResource resource = delta.getResource();
        if (resource.getType() != IResource.FILE) {
          return true;
        }
        int flags = delta.getFlags();
        if ((flags & DELTA_MASK) == 0) {
          return false;
        }
        IPath path = resource.getLocation();
        if (path != null) {
          server.fileChanged(path.toFile());
        }
        return false;

      case IResourceDelta.REMOVED:
        // TODO (danrubel): stop analyzing removed libraries
        return false;

      default:
        return false;
    }
  }

  /**
   * Scan the input stream for a directive indicating that the Dart file is a library
   * 
   * @param in the input stream (not <code>null</code>)
   */
  boolean hasDirective(InputStream in) throws IOException {
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
   * Add multiple files to the list of files to be scanned and wakeup the background scanning thread
   * 
   * @param files the files to add (not <code>null</code>, contains no <code>null</code>s)
   */
  private void addFilesToScan(File[] files) {
    synchronized (filesToScan) {
      for (File file : files) {
        filesToScan.add(file);
      }
      filesToScan.notifyAll();
    }
  }

  /**
   * Add a file to the list of files to be scanned and wakeup the background scanning thread
   * 
   * @param file the file (not <code>null</code>)
   */
  private void addFileToScan(File file) {
    synchronized (filesToScan) {
      filesToScan.add(file);
      filesToScan.notifyAll();
    }
  }

  /**
   * Remove a file from the list of files to be scanned. This method will block until there is a
   * file to be scanned
   * 
   * @return a file to scan (not <code>null</code>)
   */
  private File getFileToScan() {
    synchronized (filesToScan) {
      while (filesToScan.size() == 0) {
        if (scanStart > 0) {
          if (DartCoreDebug.PERF_ANALYSIS_FILESCAN) {
            long delta = System.currentTimeMillis() - scanStart;
            long average = delta / scanCount;
            DartCore.logInformation("Scanning for library files: " + scanCount + " files, " + delta
                + " ms, " + average + " ms/file");
          }
          scanStart = 0;
          scanCount = 0;
        }
        try {
          filesToScan.wait();
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
      if (scanStart == 0) {
        scanStart = System.currentTimeMillis();
      }
      scanCount++;
      return filesToScan.remove(filesToScan.size() - 1);
    }
  }

  /**
   * Scan the specified file to determine if it is a Dart library file
   * 
   * @param file the file to scan (not <code>null</code>)
   */
  private void scanFile(File file) {
    if (!DartCore.isDartLikeFileName(file.getName())) {
      return;
    }
    FileInputStream in;
    try {
      in = new FileInputStream(file);
    } catch (FileNotFoundException e) {
      DartCore.logInformation("Failed to open for scan: " + file);
      return;
    }
    try {
      // TODO (danrubel) Need to analyze libraries without directives that are unreferenced
      if (hasDirective(in)) {
        server.analyzeLibrary(file);
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

  /**
   * Called on the background thread to scan files looking for Dart library files
   */
  private void scanFiles() {
    // TODO (danrubel) revisit this if/when we decide to cache the element model
    while (scan) {
      File file = getFileToScan();
      if (file.isDirectory()) {
        addFilesToScan(file.listFiles());
      } else {
        scanFile(file);
      }
    }
  }
}
