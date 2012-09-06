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
package com.google.dart.tools.core.analysis.index;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ErrorCode;
import com.google.dart.compiler.ErrorSeverity;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.AnalysisError;
import com.google.dart.tools.core.analysis.AnalysisEvent;
import com.google.dart.tools.core.analysis.AnalysisListener;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.internal.util.ResourceUtil;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Updates problem markers on a background thread based upon information from the
 * {@link AnalysisServer}.
 */
class AnalysisMarkerManager implements AnalysisListener {

  /**
   * Adds markers for the specified errors and warnings
   */
  private class AddMarkersOp extends MarkerOp {

    private final Collection<AnalysisError> errors;

    AddMarkersOp(Collection<AnalysisError> errors) {
      this.errors = errors;
    }

    /**
     * Create an error marker for the specified file
     */
    void createMarker(File file, DartCompilationError error) {
      if (file == null || error == null) {
        return;
      }
      IResource res = ResourceUtil.getResource(file);
      if (res == null || !res.exists() || !DartCore.isAnalyzed(res)) {
        return;
      }

      int severity;
      ErrorSeverity errorSeverity = error.getErrorCode().getErrorSeverity();
      if (errorSeverity == ErrorSeverity.ERROR) {
        severity = IMarker.SEVERITY_ERROR;
      } else if (errorSeverity == ErrorSeverity.WARNING) {
        severity = IMarker.SEVERITY_WARNING;
      } else if (errorSeverity == ErrorSeverity.INFO) {
        severity = IMarker.SEVERITY_INFO;
      } else {
        return;
      }

      int offset = error.getStartPosition();
      int length = error.getLength();
      int lineNumber = error.getLineNumber();
      String errMsg = error.getMessage();
      String errorCodeString = ErrorCode.Helper.toQualifiedName(error.getErrorCode());

      // Remove newlines and indent spaces from the compiler's error messages

      if (errMsg.indexOf('\n') != -1) {
        errMsg = errMsg.replace('\n', ' ');
        errMsg = errMsg.replaceAll(" +", " ");
      }

      try {
        IMarker marker = res.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.MESSAGE, errMsg);
        marker.setAttribute(IMarker.CHAR_START, offset);
        marker.setAttribute(IMarker.CHAR_END, offset + length);
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
        marker.setAttribute("errorCode", errorCodeString);
      } catch (CoreException e) {
        DartCore.logError("Failed to create marker for " + res + "\n   at " + offset + " message: "
            + errMsg, e);
      }
    }

    @Override
    void perform() {
      for (AnalysisError error : errors) {
        createMarker(error.getDartFile(), error.getCompilationError());
      }
    }
  }

  /**
   * Operation to add or remove markers
   */
  private abstract class MarkerOp {
    abstract void perform();
  }

  /**
   * Removes the markers from a collection of files
   */
  private class RemoveMarkersOp extends MarkerOp {
    private final Collection<File> files;

    RemoveMarkersOp(Collection<File> files) {
      this.files = files;
    }

    @Override
    void perform() {
      for (File file : files) {
        removeMarkers(file);
      }
    }

    void removeMarkers(File file) {
      IResource res = ResourceUtil.getResource(file);
      if (res == null || !res.isAccessible()) {
        return;
      }
      try {
        res.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
      } catch (CoreException e) {
        DartCore.logError("Failed to clear markers for " + res, e);
      }
    }
  }

  /**
   * A collection of marker changes to be made. Synchronize against this field before accessing.
   */
  private final ArrayList<MarkerOp> queue;

  /**
   * A flag indicating whether markers should be updated.
   */
  private boolean updateMarkers;

  public AnalysisMarkerManager() {
    this.queue = new ArrayList<MarkerOp>();
    this.updateMarkers = true;
    Thread thread = new Thread(getClass().getSimpleName()) {
      @Override
      public void run() {
        updateMarkers();
      }
    };
    thread.start();
  }

  @Override
  public void discarded(AnalysisEvent event) {
    Collection<File> allFiles = new ArrayList<File>();
    allFiles.add(event.getLibraryFile());
    allFiles.addAll(event.getFiles());
    synchronized (queue) {
      queue.add(new RemoveMarkersOp(allFiles));
      queue.notifyAll();
    }
  }

  /**
   * Remove all existing problem markers for the specified files and create new problem markers for
   * any parse errors that were found.
   */
  @Override
  public void parsed(final AnalysisEvent event) {
    synchronized (queue) {
      queue.add(new RemoveMarkersOp(event.getFiles()));
      Collection<AnalysisError> errors = event.getErrors();
      if (errors.size() > 0) {
        queue.add(new AddMarkersOp(errors));
      }
      queue.notifyAll();
    }
  }

  /**
   * The {@link #parsed(AnalysisEvent)} notification has already removed any existing resolution
   * problem markers so just create new problem markers for any resolution errors that were found.
   */
  @Override
  public void resolved(final AnalysisEvent event) {
    synchronized (queue) {
      Collection<AnalysisError> errors = event.getErrors();
      if (errors.size() > 0) {
        queue.add(new AddMarkersOp(errors));
        queue.notifyAll();
      }
    }
  }

  public void stop() {
    updateMarkers = false;
    // Ensure background thread exits the wait
    synchronized (queue) {
      queue.notifyAll();
    }
  }

  /**
   * Called on the background thread to update markers by performing queued marker operations
   */
  private void updateMarkers() {
    while (updateMarkers) {

      // Wait for new marker operations

      final ArrayList<MarkerOp> todo = new ArrayList<MarkerOp>();
      synchronized (queue) {
        while (queue.isEmpty()) {
          try {
            queue.wait();
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
        todo.addAll(queue);
        queue.clear();
      }

      // Batch process marker operations

      IWorkspaceRunnable op = new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          for (MarkerOp op : todo) {
            op.perform();
          }
        }
      };
      try {
        if (updateMarkers) {
          ResourcesPlugin.getWorkspace().run(op, null);
        }
      } catch (CoreException e) {
        DartCore.logError("Exception translating errors/warnings into markers", e);
      }

      // Sleep for 1 second to allow marker operations to accumulate and be batched
      if (System.getProperty("dartEditorTesting") == null) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
    }
  }
}
