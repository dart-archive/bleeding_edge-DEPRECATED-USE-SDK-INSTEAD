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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.util.ArrayList;

/**
 * Instances of {@code AnalysisWorker} perform analysis by repeatedly calling
 * {@link AnalysisContext#performAnalysisTask()} and update both the index and the error markers
 * based upon the analysis results.
 */
public class AnalysisWorker {

  /**
   * A build level job processing workers in {@link AnalysisWorker#backgroundQueue}.
   */
  private class BackgroundAnalysisJob extends Job {
    public BackgroundAnalysisJob() {
      super("Analyzing");
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      while (true) {
        AnalysisWorker worker;
        synchronized (backgroundQueue) {
          if (backgroundQueue.isEmpty()) {
            backgroundJob = null;
            backgroundQueue.notifyAll();
            return Status.OK_STATUS;
          }
          worker = backgroundQueue.remove(0);
        }

        if (worker.contextManager instanceof Project) {
          setName("Analyzing " + ((Project) worker.contextManager).getResource().getName());
        } else if (worker.contextManager instanceof ProjectManager) {
          setName("Analyzing SDK");
        }
        worker.performAnalysis();
      }
    }
  }

  /**
   * A collection of workers to be run on a background job. Synchronize against this field before
   * accessing it.
   */
  private static final ArrayList<AnalysisWorker> backgroundQueue = new ArrayList<AnalysisWorker>();

  /**
   * The background job on which the queued workers are executed, or {@code null} if none.
   * Synchronize against {@link #backgroundQueue} before accessing this field.
   */
  private static BackgroundAnalysisJob backgroundJob = null;

  /**
   * Wait for any scheduled background analysis to complete or for the specified duration to elapse.
   * 
   * @param milliseconds the number of milliseconds to wait
   * @return {@code true} if the background analysis has completed, else {@code false}
   */
  public static boolean waitForBackgroundAnalysis(long milliseconds) {
    synchronized (backgroundQueue) {
      long end = System.currentTimeMillis() + milliseconds;
      while (backgroundJob != null) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          return false;
        }
        try {
          backgroundQueue.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
      return true;
    }
  }

  /**
   * The context manager containing the source for this context (not {@code null}).
   */
  protected final ContextManager contextManager;

  /**
   * An object used to synchronously access the {@link #context} field.
   */
  private final Object lock = new Object();

  /**
   * The analysis context on which analysis is performed or {@code null} if either the analysis is
   * stopped or complete. Synchronize against {@link #lock} before accessing this field.
   */
  private AnalysisContext context;

  /**
   * The marker manager used to translate errors into Eclipse markers (not {@code null}).
   */
  private final AnalysisMarkerManager markerManager;

  /**
   * The project manager used to obtain the index to be updated and used to notify others when
   * analysis is complete (not {@code null}).
   */
  private final ProjectManager projectManager;

  /**
   * The index to be updated (not {@code null}).
   */
  private final Index index;

  /**
   * Construct a new instance for performing analysis which updates the
   * {@link ProjectManager#getIndex() default index} and uses the
   * {@link AnalysisMarkerManager#getInstance() default marker manager} to translate errors into
   * Eclipse markers.
   * 
   * @param project the project containing sources for the specified context (not {@code null})
   * @param context the context used to perform the analysis (not {@code null})
   */
  public AnalysisWorker(ContextManager contextManager, AnalysisContext context) {
    this(contextManager, context, DartCore.getProjectManager(), AnalysisMarkerManager.getInstance());
  }

  /**
   * Construct a new instance for performing analysis.
   * 
   * @param context the context containing sources for the specified context (not {@code null})
   * @param context the context used to perform the analysis (not {@code null})
   * @param projectManager used to obtain the index to be updated and notified others when analysis
   *          is complete (not {@code null})
   * @param markerManager used to translate errors into Eclipse markers (not {@code null})
   */
  public AnalysisWorker(ContextManager contextManager, AnalysisContext context,
      ProjectManager projectManager, AnalysisMarkerManager markerManager) {
    this.contextManager = contextManager;
    this.context = context;
    this.projectManager = projectManager;
    this.index = projectManager.getIndex();
    this.markerManager = markerManager;
    this.contextManager.addWorker(this);
  }

  /**
   * Answer the context being processed by the receiver.
   * 
   * @return the context or {@code null} if processing has been stopped or is complete
   */
  public AnalysisContext getContext() {
    synchronized (lock) {
      return context;
    }
  }

  /**
   * Perform analysis by repeatedly calling {@link AnalysisContext#performAnalysisTask()} and update
   * both the index and the error markers based upon the analysis results.
   */
  public void performAnalysis() {
    boolean analysisComplete = false;
    while (true) {

      // Check if the context has been set to null indicating that analysis should stop
      AnalysisContext context;
      synchronized (lock) {
        context = this.context;
        if (context == null) {
          break;
        }
      }

      // Exit if no more analysis to be performed (changes == null)
      ChangeNotice[] changes = context.performAnalysisTask();
      if (changes == null) {
        analysisComplete = true;
        break;
      }

      // Process changes and allow subclasses to check results
      processChanges(changes);
      checkResults(context);
    }
    stop();
    markerManager.done();

    // Notify others that analysis is complete
    if (analysisComplete && contextManager instanceof Project) {
      projectManager.projectAnalyzed((Project) contextManager);
    }
  }

  /**
   * Queue this worker to have {@link #performAnalysis()} called in a background job.
   */
  public void performAnalysisInBackground() {
    synchronized (backgroundQueue) {
      if (!backgroundQueue.contains(this)) {
        backgroundQueue.add(this);
        if (backgroundJob == null) {
          backgroundJob = new BackgroundAnalysisJob();
          backgroundJob.setPriority(Job.BUILD);
          backgroundJob.schedule();
        }
      }
    }
  }

  /**
   * Signal the receiver to stop analysis.
   */
  public void stop() {
    synchronized (lock) {
      context = null;
    }
    contextManager.removeWorker(this);
  }

  /**
   * Subclasses may override this method to call various "get" methods on the context looking to see
   * if information it needs is cached.
   * 
   * @param context the analysis context being processed (not {@code null})
   */
  protected void checkResults(AnalysisContext context) {
  }

  /**
   * Update both the index and the error markers based upon the analysis results.
   * 
   * @param changes the changes to be processed (not {@code null})
   */
  private void processChanges(ChangeNotice[] changes) {
    for (ChangeNotice change : changes) {

      // If errors are available, then queue the errors to be translated to markers
      AnalysisError[] errors = change.getErrors();
      if (errors != null) {
        Source source = change.getSource();
        IResource res = contextManager.getResource(source);
        if (res == null) {
          // TODO (danrubel): log unmatched sources once context 
          // only returns errors for added sources
          // DartCore.logError("Failed to determine resource for: " + source);
        } else {
          if (DartCore.isAnalyzed(res)) {
            IPath location = res.getLocation();
            if (location != null && !DartCore.isContainedInPackages(location.toFile())) {
              LineInfo lineInfo = change.getLineInfo();
              if (lineInfo == null) {
                DartCore.logError("Missing line information for: " + source);
              } else {
                markerManager.queueErrors(res, lineInfo, errors);
              }
            }
          }
        }
      }

      // If there is a unit to be indexed, then do so
      CompilationUnit unit = change.getCompilationUnit();
      if (unit != null) {
        index.indexUnit(context, unit);
      }
    }
  }
}
