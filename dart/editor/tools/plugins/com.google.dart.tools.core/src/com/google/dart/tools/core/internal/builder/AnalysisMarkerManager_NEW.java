/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.AnalysisErrorSeverity;
import com.google.dart.server.generated.types.Location;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.analysis.model.WorkspaceAnalysisServerListener;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
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
 * Typically the {@link WorkspaceAnalysisServerListener} repeatedly calls
 * {@link #queueErrors(IResource, LineInfo, AnalysisError[])} until all errors have been queued.
 * <p>
 * When the workspace is shutdown, {@link #stop()} should be called to gracefully exit the
 * background process if it is running.
 * 
 * @coverage dart.tools.core.builder
 */
public class AnalysisMarkerManager_NEW {

  /**
   * Errors to be translated into markers
   */
  private static final class ErrorResult implements Result {
    final IResource resource;
    final AnalysisError[] errors;

    ErrorResult(IResource resource, AnalysisError[] errors) {
      this.resource = resource;
      this.errors = errors;
    }

    @Override
    public IResource getResource() {
      return resource;
    }

    @Override
    public void showErrors() throws CoreException {
      if (!resource.isAccessible()) {
        return;
      }

      resource.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_ZERO);
      resource.deleteMarkers(DartCore.DART_TASK_MARKER_TYPE, true, IResource.DEPTH_ZERO);
      resource.deleteMarkers(DartCore.ANGULAR_WARNING_MARKER_TYPE, true, IResource.DEPTH_ZERO);

      // Ignore if user requested to don't analyze resource.
      if (!DartCore.isAnalyzed(resource)) {
        return;
      }

      // Show errors first, then warnings, followed by everything else
      // while limiting the total number of markers added to MAX_ERROR_COUNT
      int errorCount = 0;
      errorCount = showErrors(errorCount, AnalysisErrorSeverity.ERROR, IMarker.SEVERITY_ERROR);
      errorCount = showErrors(errorCount, AnalysisErrorSeverity.WARNING, IMarker.SEVERITY_WARNING);
      errorCount = showErrors(errorCount, AnalysisErrorSeverity.INFO, IMarker.SEVERITY_INFO);

      if (errorCount >= MAX_ERROR_COUNT) {
        IMarker marker = resource.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
        marker.setAttribute(IMarker.LINE_NUMBER, 1);
        marker.setAttribute(IMarker.MESSAGE, "There are more then " + MAX_ERROR_COUNT
            + " errors; not showing any more...");
      }
    }

    private int showErrors(int errorCount, String errorSeverity, int markerSeverity)
        throws CoreException {

      for (AnalysisError error : errors) {
        if (!error.getSeverity().equals(errorSeverity)) {
          continue;
        }
        Location location = error.getLocation();

        boolean isHint = error.getType().equals(com.google.dart.engine.error.ErrorType.HINT.name());// == ErrorType.HINT;

        String markerType = DartCore.DART_PROBLEM_MARKER_TYPE;
        // Server doesn't have the angular error type
//        if (errorCode.getType() == ErrorType.ANGULAR) {
//          markerType = DartCore.ANGULAR_WARNING_MARKER_TYPE;
//          markerSeverity = IMarker.SEVERITY_WARNING;
//        } else
        if (error.getType().equals(com.google.dart.engine.error.ErrorType.TODO.name())) {
          markerType = DartCore.DART_TASK_MARKER_TYPE;
        } else if (isHint) {
          markerType = DartCore.DART_HINT_MARKER_TYPE;
        }

        IMarker marker = resource.createMarker(markerType);
        marker.setAttribute(IMarker.SEVERITY, markerSeverity);
        marker.setAttribute(IMarker.CHAR_START, location.getOffset());
        marker.setAttribute(IMarker.CHAR_END, location.getOffset() + location.getLength());
        marker.setAttribute(IMarker.LINE_NUMBER, location.getStartLine());
        marker.setAttribute(IMarker.MESSAGE, error.getMessage());

        if (isHint) {
          marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
        }

        errorCount++;
        if (errorCount >= MAX_ERROR_COUNT) {
          break;
        }
      }
      return errorCount;
    }
  }

  /**
   * Add/remove marker indicating that a particular project has an SDK associated with it
   */
  private final class HasSdkResult implements Result {

    private final IProject project;
    private final boolean hasSdk;

    public HasSdkResult(IProject project, boolean hasSdk) {
      this.project = project;
      this.hasSdk = hasSdk;
    }

    @Override
    public IResource getResource() {
      return project;
    }

    @Override
    public void showErrors() throws CoreException {
      if (!project.isAccessible()) {
        return;
      }

      project.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_ZERO);

      if (!hasSdk) {
        IMarker marker = project.createMarker(DartCore.DART_PROBLEM_MARKER_TYPE);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        marker.setAttribute(IMarker.CHAR_START, 0);
        marker.setAttribute(IMarker.CHAR_END, 0);
        marker.setAttribute(IMarker.LINE_NUMBER, 1);
        //TODO (danrubel): improve error message to indicate action to install SDK
        marker.setAttribute(IMarker.MESSAGE, "Missing Dart SDK");
        //TODO (danrubel): Quick Fix ?
      }
    }
  }

  /**
   * Results to be translated into markers
   */
  private interface Result {

    /**
     * The resource for which markers are being added / removed.
     * 
     * @return the resource
     */
    IResource getResource();

    /**
     * Set markers on the specified resource to represent the cached analysis errors
     */
    void showErrors() throws CoreException;
  }

  private static final int MAX_ERROR_COUNT = 500;

  /**
   * The singleton used for translating {@link AnalysisError}s into Eclipse markers.
   */
  private static final AnalysisMarkerManager_NEW INSTANCE = new AnalysisMarkerManager_NEW(
      ResourcesPlugin.getWorkspace());

  /**
   * Answer the singleton used for translating {@link AnalysisError}s into Eclipse markers.
   * 
   * @return the marker manager (not {@code null})
   */
  public static AnalysisMarkerManager_NEW getInstance() {
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
   * The time when {@link #results} was updated last time.
   */
  private long lastResultsUpdate = 0;

  /**
   * The background thread that translates {@link AnalysisError}s into Eclipse markers or
   * {@code null} if either {@link #translateErrors()} has not been called or background processing
   * is complete and there are no new errors to translate.
   * <p>
   * Note: Only access this field while synchronized on {@link #lock}.
   */
  private Thread updateThread;

  /**
   * Used exclusively by the background thread during translation. Should not be accessed in any
   * other code.
   */
  private ArrayList<Result> resultsBeingTranslated;

  /**
   * Construct a new instance for translating errors to markers using the specified workspace.
   */
  public AnalysisMarkerManager_NEW(IWorkspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Call this to clear markers and remove resource from error queue
   */
  public void clearMarkers(IResource resource) {

    //TODO(keertip): remove resource from queue
    try {
      if (resource.isAccessible()) {
        if (resource instanceof IContainer) {
          resource.deleteMarkers(null, false, IResource.DEPTH_INFINITE);
        } else {
          resource.deleteMarkers(null, false, IResource.DEPTH_ZERO);
        }
      }
    } catch (Exception e) {
      DartCore.logError(e);
    }
  }

  /**
   * Queue the specified errors for later translation to Eclipse markers.
   * 
   * @param resource the resource on which the errors should be displayed (not {@code null})
   * @param errors the errors to be translated (not {@code null}, contains no {@code null}s)
   */
  public void queueErrors(IResource resource, AnalysisError[] errors) {
    queueResult(new ErrorResult(resource, errors));
    lastResultsUpdate = System.currentTimeMillis();
  }

  /**
   * Queue the specified information about whether the project has a Dart SDK associated with it so
   * that the information can be translated into an Eclipse marker at a later time.
   * 
   * @param resource the resource (not {@code null})
   * @param hasSdk {@code true} if there is a Dart SDK, else {@code false}
   */
  public void queueHasDartSdk(IResource resource, boolean hasSdk) {
    IProject project = resource.getProject();
    // workspace root getProject() returns null
    if (project != null) {
      queueResult(new HasSdkResult(project, hasSdk));
    }
  }

  /**
   * Call this method to cancel the background thread.
   */
  public void stop() {
    monitor.setCanceled(true);
  }

  /**
   * Queue the specified result for later translation to Eclipse markers.
   * 
   * @param result the result to be translated (not {@code null})
   */
  private void queueResult(Result result) {
    synchronized (lock) {
      // queue the errors to be translated
      if (results == null) {
        results = new ArrayList<Result>();
      }
      results.add(result);

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
   * Call this on the background thread to translate errors into Eclipse markers.
   */
  private void translateErrors() {
    while (true) {
      synchronized (lock) {
        try {
          lock.wait(50);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }

        // Exit if nothing to translate
        if (results == null) {
          updateThread = null;
          return;
        }

        // Wait at least 45 milliseconds to get more errors to translate
        if (System.currentTimeMillis() - lastResultsUpdate < 45) {
          continue;
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
              DartCore.logError("Failed to show errors for " + result.getResource(), e);
            }
          }
          resultsBeingTranslated = null;
        }
      };
      try {
        workspace.run(op, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);
      } catch (CoreException e) {
        DartCore.logError("Exception translating analysis errors to markers", e);
      } catch (NullPointerException e) {
        // Suppress this error if we are shutting down causing the workspace is in an invalid state
        if (!monitor.isCanceled()) {
          throw e;
        }
      }
    }
  }
}
