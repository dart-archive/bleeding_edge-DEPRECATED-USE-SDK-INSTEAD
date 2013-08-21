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
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisEvent;
import com.google.dart.tools.core.analysis.model.AnalysisListener;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.ResolvedEvent;
import com.google.dart.tools.core.model.DartSdkManager;

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
 * 
 * @coverage dart.tools.core.builder
 */
public class AnalysisWorker {

  public class Event implements ResolvedEvent, AnalysisEvent {
    private final AnalysisContext context;
    CompilationUnit unit;
    Source source;
    IResource resource;

    public Event(AnalysisContext context) {
      this.context = context;
    }

    @Override
    public AnalysisContext getContext() {
      return context;
    }

    @Override
    public ContextManager getContextManager() {
      return contextManager;
    }

    @Override
    public IResource getResource() {
      return resource;
    }

    @Override
    public Source getSource() {
      return source;
    }

    @Override
    public CompilationUnit getUnit() {
      return unit;
    }
  }

  /**
   * A build level job processing workers in {@link AnalysisWorker#backgroundQueue}.
   */
  private static class BackgroundAnalysisJob extends Job {
    public BackgroundAnalysisJob() {
      super("Analyzing");
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      while (true) {
        AnalysisWorker worker;
        synchronized (backgroundQueue) {
          if (backgroundQueue.isEmpty() || pauseCount > 0) {
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
        activeWorker = worker;
        try {
          worker.performAnalysis();
        } finally {
          activeWorker = null;
        }
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
   * The number of times {@link #pauseBackgroundAnalysis()} has been called without a balancing call
   * to {@link #resumeBackgroundAnalysis()}. Synchronize against {@link #backgroundQueue} before
   * accessing this field.
   */
  private static int pauseCount = 0;

  /**
   * The currently executing {@link AnalysisWorker}.
   */
  private static AnalysisWorker activeWorker = null;

  /**
   * Objects to be notified when each compilation unit has been resolved. Contents of this array
   * will not change, but the array itself may be replaced. Synchronize against
   * {@link #allListenersLock} before accessing this field.
   */
  private static AnalysisListener[] allListeners = new AnalysisListener[] {};

  /**
   * Synchronize against {@code #allListenersLock} before accessing {@link #allListeners}
   */
  private static final Object allListenersLock = new Object();

  /**
   * Add a listener to be notified when compilation units are resolved
   * 
   * @param listener the listener
   */
  public static void addListener(AnalysisListener listener) {
    if (listener == null) {
      return;
    }
    synchronized (allListenersLock) {
      for (AnalysisListener each : allListeners) {
        if (listener == each) {
          return;
        }
      }
      int oldLen = allListeners.length;
      AnalysisListener[] newListeners = new AnalysisListener[oldLen + 1];
      System.arraycopy(allListeners, 0, newListeners, 0, oldLen);
      newListeners[oldLen] = listener;
      allListeners = newListeners;
    }
  }

  /**
   * @return the currently executing {@link AnalysisWorker}, may be {@code null}.
   */
  public static AnalysisWorker getActiveWorker() {
    return activeWorker;
  }

  /**
   * @return the {@link AnalysisWorker}s in the queue, may be empty, but not {@code null}.
   */
  public static AnalysisWorker[] getQueueWorkers() {
    synchronized (backgroundQueue) {
      return backgroundQueue.toArray(new AnalysisWorker[backgroundQueue.size()]);
    }
  }

  /**
   * Pause background analysis until {@link #resumeBackgroundAnalysis()} is called.
   */
  public static void pauseBackgroundAnalysis() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("AnalysisWorker-pause");
    try {
      synchronized (backgroundQueue) {
        pauseCount++;
        instrumentation.metric("PauseCount", pauseCount);
      }
    } finally {
      instrumentation.log();
    }
  }

  /**
   * Ensure that a worker is at the front of the queue to update the analysis for the context.
   * 
   * @param manager the manager containing the context to be analyzed (not {@code null})
   * @param context the context to be analyzed (not {@code null})
   * @see #performAnalysis()
   */
  public static void performAnalysisInBackground(ContextManager manager, AnalysisContext context) {
    synchronized (backgroundQueue) {
      if (backgroundQueue.size() == 0 || backgroundQueue.get(0).getContext() != context) {
        new AnalysisWorker(manager, context).performAnalysisInBackground();
      }
    }
  }

  /**
   * Remove a listener from the list of objects to be notified.
   * 
   * @param listener the listener to be removed
   */
  public static void removeListener(AnalysisListener listener) {
    synchronized (allListenersLock) {
      for (int index = 0; index < allListeners.length; index++) {
        if (listener == allListeners[index]) {
          int oldLen = allListeners.length;
          AnalysisListener[] newListeners = new AnalysisListener[oldLen - 1];
          System.arraycopy(allListeners, 0, newListeners, 0, index);
          System.arraycopy(allListeners, index + 1, newListeners, index, oldLen - index - 1);
          allListeners = newListeners;
          return;
        }
      }
    }
  }

  /**
   * Resume background analysis.
   */
  public static void resumeBackgroundAnalysis() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("AnalysisWorker-resume");
    try {
      synchronized (backgroundQueue) {
        if (pauseCount > 0) {
          pauseCount--;
          instrumentation.metric("PauseCount", pauseCount);
          if (pauseCount == 0) {
            startBackgroundAnalysis();
          }
        } else {
          instrumentation.metric("Problem", "resume-called-before-pause");
        }
      }
    } finally {
      instrumentation.log();
    }
  }

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
   * Start a job to perform background analysis if it has not already been started.
   */
  private static void startBackgroundAnalysis() {
    synchronized (backgroundQueue) {
      if (backgroundJob == null) {
        backgroundJob = new BackgroundAnalysisJob();
        backgroundJob.setPriority(Job.BUILD);
      }
      backgroundJob.schedule();
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
   * Contains information about the compilation unit that was resolved.
   */
  private final Event event;

  /**
   * Flag to prevent log from being saturated with exceptions.
   */
  private static boolean exceptionLogged = false;

  /**
   * Construct a new instance for performing analysis which updates the
   * {@link ProjectManager#getIndex() default index} and uses the
   * {@link AnalysisMarkerManager#getInstance() default marker manager} to translate errors into
   * Eclipse markers.
   * 
   * @param manager the manager containing sources for the specified context (not {@code null})
   * @param context the context used to perform the analysis (not {@code null})
   */
  public AnalysisWorker(ContextManager manager, AnalysisContext context) {
    this(manager, context, DartCore.getProjectManager(), AnalysisMarkerManager.getInstance());
  }

  /**
   * Construct a new instance for performing analysis.
   * 
   * @param contextManager manager containing sources for the specified context (not {@code null})
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
    this.markerManager = markerManager;
    this.contextManager.addWorker(this);
    this.event = new Event(context);
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

    // Check for a valid context and SDK
    DartSdk sdk;
    synchronized (lock) {
      if (context == null) {
        return;
      }
      sdk = context.getSourceFactory().getDartSdk();
    }
    boolean hasSdk = sdk != DartSdkManager.NO_SDK;
    markerManager.queueHasDartSdk(this.contextManager.getResource(), hasSdk);
    if (!hasSdk) {
      return;
    }

    boolean analysisComplete = false;
    while (true) {

      // If background analysis has been paused, push the receiver back on the queue
      synchronized (backgroundQueue) {
        if (pauseCount > 0) {
          backgroundQueue.add(0, this);
          return;
        }
      }

      // Check if the context has been set to null indicating that analysis should stop
      AnalysisContext context;
      synchronized (lock) {
        context = this.context;
        if (context == null) {
          break;
        }
      }

      // Exit if no more analysis to be performed (changes == null)
      ChangeNotice[] changes;
      try {
        changes = context.performAnalysisTask();
      } catch (RuntimeException e) {
        DartCore.logError("Analysis Failed: " + contextManager, e);
        break;
      }
      if (changes == null) {
        analysisComplete = true;
        break;
      }

      // Process changes and allow subclasses to check results
      processChanges(context, changes);
      checkResults(context);
    }
    stop();
    markerManager.done();

    // Notify others that analysis is complete
    if (analysisComplete) {
      notifyComplete();
    }
  }

  /**
   * Queue this worker to have {@link #performAnalysis()} called in a background job.
   * 
   * @see #performAnalysisInBackground(Project, AnalysisContext)
   */
  public void performAnalysisInBackground() {
    synchronized (backgroundQueue) {
      if (!backgroundQueue.contains(this)) {
        backgroundQueue.add(0, this);
        if (pauseCount == 0) {
          startBackgroundAnalysis();
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
   * Notify those interested that the analysis is complete.
   */
  private void notifyComplete() {
    if (contextManager instanceof Project) {
      projectManager.projectAnalyzed((Project) contextManager);
    }
    AnalysisListener[] currentListeners;
    synchronized (allListenersLock) {
      currentListeners = allListeners;
    }
    for (AnalysisListener listener : currentListeners) {
      try {
        listener.complete(event);
      } catch (Exception e) {
        if (!exceptionLogged) {
          // Log at most one exception so as not to flood the log
          exceptionLogged = true;
          DartCore.logError("Exception notifying listener that analysis is complete", e);
        }
      }
    }
  }

  /**
   * Notify those interested that a compilation unit has been resolved.
   * 
   * @param context the analysis context containing the unit that was resolved (not {@code null})
   * @param unit the unit that was resolved (not {@code null})
   * @param source the source of the unit that was resolved (not {@code null})
   * @param resource the resource of the unit that was resolved or {@code null} if outside the
   *          workspace
   */
  private void notifyResolved(AnalysisContext context, CompilationUnit unit, Source source,
      IResource resource) {
    AnalysisListener[] currentListeners;
    synchronized (allListenersLock) {
      currentListeners = allListeners;
    }
    event.unit = unit;
    event.source = source;
    event.resource = resource;
    for (AnalysisListener listener : currentListeners) {
      try {
        listener.resolved(event);
      } catch (Exception e) {
        if (!exceptionLogged) {
          // Log at most one exception so as not to flood the log
          exceptionLogged = true;
          DartCore.logError("Exception notifying listener of resolved unit: " + source, e);
        }
      }
    }
  }

  /**
   * Update both the index and the error markers based upon the analysis results.
   * 
   * @param context the analysis context containing the unit that was resolved (not {@code null})
   * @param changes the changes to be processed (not {@code null})
   */
  private void processChanges(AnalysisContext context, ChangeNotice[] changes) {
    for (ChangeNotice change : changes) {
      Source source = change.getSource();
      IResource res = contextManager.getResource(source);

      // If errors are available, then queue the errors to be translated to markers
      AnalysisError[] errors = change.getErrors();
      if (errors != null) {
        if (res == null) {
          // TODO (danrubel): log unmatched sources once context 
          // only returns errors for added sources
          // DartCore.logError("Failed to determine resource for: " + source);
        } else {
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

      // If there is a resolved unit, then then notify others such as indexer
      CompilationUnit unit = change.getCompilationUnit();
      if (unit != null) {
        notifyResolved(context, unit, source, res);
      }
    }
  }
}
