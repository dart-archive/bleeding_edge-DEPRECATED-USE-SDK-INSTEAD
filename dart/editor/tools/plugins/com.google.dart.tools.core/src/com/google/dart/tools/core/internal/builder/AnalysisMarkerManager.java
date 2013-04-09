/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.ArrayList;

/**
 * Instances of {@code AnalysisMarkerManager} queue {@link AnalysisError}s from sources such as
 * {@link AnalysisWorker} and translate those errors into Eclipse markers on a separate thread.
 * There is a single instance accessible via {@link #getInstance()} for use during normal execution,
 * but other instances can be created for testing purposes.
 * <p>
 * Typically the {@link AnalysisWorker} repeatedly calls
 * {@link #queueErrors(IResource, LineInfo, AnalysisError[])} until all errors have been queued,
 * then calls {@link #done()} to indicate that, at least for the time being, all errors have been
 * queued.
 * <p>
 * When the workspace is shutdown, {@link #stop()} should be called to gracefully exit the
 * background process if it is running.
 */
public class AnalysisMarkerManager {
  /**
   * Errors to be translated into markers
   */
  private static final class Result {
    final IResource resource;
    final LineInfo lineInfo;
    final AnalysisError[] errors;

    Result(IResource resource, LineInfo lineInfo, AnalysisError[] errors) {
      this.resource = resource;
      this.lineInfo = lineInfo;
      this.errors = errors;
    }

    /**
     * Set markers on the specified resource to represent the cached analysis errors
     */
    void showErrors() throws CoreException {
      if (!resource.isAccessible()) {
        return;
      }

      resource.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_ZERO);

      int errorCount = 0;

      for (AnalysisError error : errors) {
        int severity;
        ErrorSeverity errorSeverity = error.getErrorCode().getErrorSeverity();
        if (errorSeverity == ErrorSeverity.ERROR) {
          severity = IMarker.SEVERITY_ERROR;
        } else if (errorSeverity == ErrorSeverity.WARNING) {
          severity = IMarker.SEVERITY_WARNING;
//        } else if (errorSeverity == ErrorSeverity.INFO) {
//          severity = IMarker.SEVERITY_INFO;
        } else {
          continue;
        }

        int lineNum = lineInfo.getLocation(error.getOffset()).getLineNumber();

        IMarker marker = resource.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.CHAR_START, error.getOffset());
        marker.setAttribute(IMarker.CHAR_END, error.getOffset() + error.getLength());
        marker.setAttribute(IMarker.LINE_NUMBER, lineNum);
//        marker.setAttribute("errorCode", error.getErrorCode());
        marker.setAttribute(IMarker.MESSAGE, error.getMessage());

        errorCount++;

        if (errorCount >= MAX_ERROR_COUNT) {
          marker = resource.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
          marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
          marker.setAttribute(IMarker.LINE_NUMBER, 1);
          marker.setAttribute(IMarker.MESSAGE, "There are more then " + MAX_ERROR_COUNT
              + " errors; not showing any more...");

          break;
        }
      }
    }
  }

  private static final int MAX_ERROR_COUNT = 500;

  /**
   * The singleton used for translating {@link AnalysisError}s into Eclipse markers.
   */
  private static final AnalysisMarkerManager INSTANCE = new AnalysisMarkerManager(
      ResourcesPlugin.getWorkspace());

  /**
   * Answer the singleton used for translating {@link AnalysisError}s into Eclipse markers.
   * 
   * @return the marker manager (not {@code null})
   */
  public static AnalysisMarkerManager getInstance() {
    return INSTANCE;
  }

  /**
   * The workspace used to batch translation of errors to Eclipse markers (not {@code null}).
   */
  private final IWorkspace workspace;

  /**
   * The progress monitor used for canceling the background process.
   */
  private final NullProgressMonitor monitor = new NullProgressMonitor();

  /**
   * Synchronize against this object before accessing private fields and method in this class.
   */
  private final Object lock = new Object();

  /**
   * A queue of results to be displayed.
   * <p>
   * Note: Only access this field while synchronized on {@link #lock}.
   */
  private ArrayList<Result> results;

  /**
   * The background thread that translates {@link AnalysisError}s into Eclipse markers or
   * {@code null} if either {@link #translateErrors()} has not been called or background processing
   * is complete and there are no new errors to translate.
   * <p>
   * Note: Only access this field while synchronized on {@link #lock}.
   */
  private Thread updateThread;

  /**
   * {@code true} if no call to {@link #queueErrors(IResource, LineInfo, AnalysisError[])} was made
   * since the last call to {@link #done()}.
   * <p>
   * Note: Only access this field while synchronized on {@link #lock}.
   */
  private boolean done;

  /**
   * Used exclusively by the background thread during translation. Should not be accessed in any
   * other code.
   */
  private ArrayList<Result> resultsBeingTranslated;

  /**
   * Construct a new instance for translating errors to markers using the specified workspace.
   */
  public AnalysisMarkerManager(IWorkspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Call this to clear markers and remove resource from error queue
   */
  public void clearMarkers(IResource resource) {

    //TODO(keertip): remove resource from queue
    try {
      if (resource instanceof IContainer) {
        resource.deleteMarkers(null, false, IResource.DEPTH_INFINITE);
      } else {
        resource.deleteMarkers(null, false, IResource.DEPTH_ZERO);
      }
    } catch (Exception e) {
      DartCore.logError(e);
    }
  }

  /**
   * Signal the background process to convert errors to markers, if it is not doing so already.
   */
  public void done() {
    synchronized (lock) {
      done = true;
      lock.notifyAll();
    }
  }

  /**
   * Queue the specified errors for later translation to Eclipse markers.
   * 
   * @param resource the resource on which the errors should be displayed (not {@code null})
   * @param lineInfo the line information (not {@code null})
   * @param errors the errors to be translated (not {@code null}, contains no {@code null}s)
   */
  public void queueErrors(IResource resource, LineInfo lineInfo, AnalysisError[] errors) {
    synchronized (lock) {
      done = false;

      // queue the errors to be translated
      if (results == null) {
        results = new ArrayList<Result>();
      }
      results.add(new Result(resource, lineInfo, errors));

      // kick off a background thread if one has not already been started
      if (updateThread == null) {
        updateThread = new Thread(getClass().getSimpleName()) {
          @Override
          public void run() {
            translateErrors();
          }
        };
        updateThread.start();
      }
    }
  }

  /**
   * Call this method to cancel the background thread.
   */
  public void stop() {
    monitor.setCanceled(true);
  }

  /**
   * Wait up to the specified number of milliseconds for the markers to be translated.
   * 
   * @param milliseconds the number of milliseconds to wait for the markers to be translated
   * @return {@code true} if all markers were translated, else {@code false}
   */
  public boolean waitForMarkers(long milliseconds) {
    synchronized (lock) {
      long end = System.currentTimeMillis() + milliseconds;
      while (updateThread != null) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          return false;
        }
        try {
          lock.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
      return true;
    }
  }

  /**
   * Call this on the background thread to translate errors into Eclipse markers.
   */
  private void translateErrors() {
    while (true) {
      synchronized (lock) {

        // If not done, then wait up to 1 second or until signaled
        if (!done) {
          try {
            lock.wait(1000);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }

        // Exit if nothing to translate
        if (results == null) {
          lock.notifyAll();
          updateThread = null;
          return;
        }

        // Grab the current collection of results to be translated
        resultsBeingTranslated = results;
        results = null;
      }

      // Batch translation of the errors
      IWorkspaceRunnable op = new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) {
          for (Result result : resultsBeingTranslated) {
            if (monitor.isCanceled()) {
              //TODO (danrubel): Investigate pushing remaining work back on the queue
              // or serializing it on shutdown
              break;
            }
            try {
              result.showErrors();
            } catch (CoreException e) {
              DartCore.logError("Failed to show errors for " + result.resource, e);
            }
          }
          resultsBeingTranslated = null;
        }
      };
      try {
        workspace.run(op, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);
      } catch (CoreException e) {
        DartCore.logError("Exception translating analysis errors to markers", e);
      }
    }
  }
}
