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
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Listens for resource changes and forwards them to the {@link AnalysisServer}
 */
public class ResourceChangeListener {

  private class Listener implements IResourceChangeListener, IResourceDeltaVisitor {

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      try {
        event.getDelta().accept(this);
      } catch (Exception e) {
        DartCore.logError("Failed to process resource changes for " + event.getResource(), e);
      }
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
      File file;
      switch (delta.getKind()) {

        case IResourceDelta.ADDED:
          file = delta.getResource().getLocation().toFile();
          server.changed(file);
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
          file = resource.getLocation().toFile();
          server.changed(file);
          return false;

        case IResourceDelta.REMOVED:
          file = delta.getResource().getLocation().toFile();
          server.changed(file);
          server.discard(file);
          return false;

        default:
          return false;
      }
    }
  }

  /**
   * This {@link ParseLibraryFileCallback} adds any sourced files discovered while parsing dart
   * files that have directives to the {@link #sourcedFiles} {@link HashSet}, so that we can later
   * compute the set of all loose files.
   * 
   * @see ResourceChangeListener#startBackgroundScan()
   */
  private class SourcedFilesCallback implements ParseLibraryFileCallback {

    @Override
    public void parsed(ParseLibraryFileEvent event) {
      synchronized (sourcedFiles) {
        sourcedFiles.addAll(event.getSourcedFiles());
        callbackCounter--;
        if (callbackCounter == 0) {
          sourcedFiles.notifyAll();
        }
      }
    }
  }

//  private static final int EVENT_MASK = IResourceChangeEvent.POST_CHANGE;
  private static final int DELTA_MASK = IResourceDelta.CONTENT | IResourceDelta.REPLACED;

  private final AnalysisServer server;

  /**
   * A collection of files and directories to be scanned. It is assumed that each element's
   * {@link File#getPath()} will return an absolute path. Synchronize against this field before
   * accessing it.
   */
  private final ArrayList<File> filesToScan;

  /**
   * The background thread currently scanning files or <code>null</code> if it has not been started
   * or has already finished. Synchronize against {@link #filesToScan} field before accessing this
   * field.
   */
  private Thread scanThread;

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
   * The resource change listener or <code>null</code> if the receiver is not currently listening
   * for resource changes. Use {@link #start()} to start listening for resource changes and
   * {@link #stop()} to stop. Synchronize against {@link #filesToScan} field before accessing this
   * field.
   */
  private Listener listener;

  /**
   * Used by {@link ResourceChangeListener#startBackgroundScan()} to keep track of the set all files
   * skipped over by the analysis server because they don't have a directive at the top of the file.
   * <p>
   * All operations on this list should be within a synchronized block against {@link #sourcedFiles}.
   */
  private ArrayList<File> nonDirectiveFiles;

  /**
   * Used by {@link ResourceChangeListener#startBackgroundScan()} to keep track of the set of all
   * dart files referenced by dart files with directives.
   * <p>
   * All operations on this set should be within a synchronized block against itself.
   */
  private HashSet<File> sourcedFiles;

  /**
   * A local {@link ParseLibraryFileCallback} used to gather the set stored in {@link #sourcedFiles}
   */
  private final SourcedFilesCallback callback;

  /**
   * Used by {@link ResourceChangeListener#startBackgroundScan()} to keep track of the number of
   * outstanding library files that haven't finished being analyzed yet.
   * <p>
   * All operations on this integer should be within a synchronized block against
   * {@link #sourcedFiles}.
   */
  private int callbackCounter;

  /**
   * The manager used to determine which files should be scanned
   */
  private final DartIgnoreManager ignoreManager;

  /**
   * Construct a new instance that listens for resource changes and forwards that information on to
   * the specified {@link AnalysisServer}
   */
  public ResourceChangeListener(AnalysisServer server) {
    this.server = server;
    this.filesToScan = new ArrayList<File>();
    this.buffer = new byte[1024];
    this.callback = new SourcedFilesCallback();
    this.ignoreManager = DartIgnoreManager.getInstance();
  }

  /**
   * Add multiple files and/or directories to the list of files to be scanned and wakeup the
   * background scanning thread
   * 
   * @param files the files to add (not <code>null</code>, contains no <code>null</code>s)
   */
  public void addFilesToScan(File[] files) {
    synchronized (filesToScan) {
      for (File file : files) {
        filesToScan.add(file);
      }
      startBackgroundScan();
    }
  }

  /**
   * Add a file or directory to the list of files to be scanned and wakeup the background scanning
   * thread
   * 
   * @param file the file (not <code>null</code>)
   */
  public void addFileToScan(File file) {
    synchronized (filesToScan) {
      filesToScan.add(file);
      startBackgroundScan();
    }
  }

  /**
   * Add projects in the workspace to the list of files to be scanned and wakeup the background
   * scanning thread
   */
  public void addWorkspaceToScan() {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      File file = project.getLocation().toFile();
      addFileToScan(file);
    }
  }

  /**
   * Add the receiver as a workspace resource change listener to update the the associated analysis
   * server when changes occur
   */
  // TODO (danrubel) merge with delta processor or remove resource change listener
//  public void start() {
//    synchronized (filesToScan) {
//      if (listener != null) {
//        return;
//      }
//      listener = new Listener();
//      ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, EVENT_MASK);
//    }
//  }

  /**
   * Stop listening for resource changes
   */
//  public void stop() {
//    synchronized (filesToScan) {
//      if (listener == null) {
//        return;
//      }
//      ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
//      listener = null;
//    }
//  }

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
   * Remove a file from the list of files to be scanned. If the receiver is listening for resource
   * changes (see {@link #listener}) then this method will block until there is a file to be
   * scanned. Otherwise this method will clear the {@link #scanThread} field and return
   * <code>null</code> if there are no files to be scanned.
   * 
   * @return a file to scan or <code>null</code> if the background scan is complete
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
        if (listener == null) {
          scanThread = null;
          return null;
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
    if (ignoreManager.isIgnored(file) || file.getName().startsWith(".")) {
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
      if (hasDirective(in)) {
        server.analyzeLibrary(file);
        synchronized (sourcedFiles) {
          callbackCounter++;
        }
        server.parseLibraryFile(file, callback);
      } else {
        synchronized (sourcedFiles) {
          nonDirectiveFiles.add(file);
        }
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
    while (true) {
      File file = getFileToScan();
      if (file == null) {
        return;
      }
      if (file.isDirectory()) {
        addFilesToScan(file.listFiles());
      } else {
        scanFile(file);
      }
    }
  }

  private void startBackgroundScan() {
    synchronized (filesToScan) {
      if (scanThread != null) {
        filesToScan.notifyAll();
        return;
      }
      scanThread = new Thread(new Runnable() {

        @Override
        public void run() {
          nonDirectiveFiles = new ArrayList<File>();
          sourcedFiles = new HashSet<File>();
          callbackCounter = 0;
          scanFiles();
          synchronized (sourcedFiles) {
            while (callbackCounter > 0) {
              try {
                sourcedFiles.wait();
              } catch (InterruptedException e) {
              }
            }
          }
          // this if statement is inserted as an optimization:
          // if nonDirectiveFiles is empty, there is no reason to start the synchronized block
          if (!nonDirectiveFiles.isEmpty()) {
            synchronized (sourcedFiles) {
              nonDirectiveFiles.removeAll(sourcedFiles);
              for (File looseFile : nonDirectiveFiles) {
                server.analyzeLibrary(looseFile);
              }
            }
          }
        }
      }, ResourceChangeListener.class.getSimpleName());
      scanThread.start();
    }
  }
}
