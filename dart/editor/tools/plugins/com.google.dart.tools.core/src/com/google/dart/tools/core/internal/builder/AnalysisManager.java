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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.util.ArrayList;

/**
 * Instances of {@code AnalysisManager} manage a queue of {@link AnalysisWorker} instances and
 * perform analysis via those instances.
 */
public class AnalysisManager {

  /**
   * The instance of {@link AnalysisManager} typically used for background processing.
   */
  private static final AnalysisManager INSTANCE = new AnalysisManager();

  /**
   * @return the default background analysis processor.
   */
  public static AnalysisManager getInstance() {
    return INSTANCE;
  }

  /**
   * A collection of workers waiting to perform analysis. Synchronize against this field before
   * accessing it.
   */
  private final ArrayList<AnalysisWorker> backgroundQueue = new ArrayList<AnalysisWorker>();

  /**
   * The background job on which the queued workers are executed, or {@code null} if none.
   * Synchronize against {@link backgroundQueue} before accessing this field.
   */
  private Job backgroundJob = null;

  /**
   * The currently executing {@link AnalysisWorker}.
   */
  private AnalysisWorker activeWorker = null;

  /**
   * Flag indicating whether {@link #stopBackgroundAnalysis()} has been called.
   */
  private boolean stopped = false;

  /**
   * Add the given worker to the queue of workers that will be processed.
   * 
   * @param worker the worker to add (not {@code null})
   */
  public void addWorker(AnalysisWorker worker) {
    synchronized (backgroundQueue) {
      if (!backgroundQueue.contains(worker)) {
        backgroundQueue.add(0, worker);
        startBackgroundAnalysis();
      }
    }
  }

  /**
   * Answer the currently executing {@link AnalysisWorker}.
   * 
   * @return the worker or {@code null} if none
   */
  public AnalysisWorker getActiveWorker() {
    synchronized (backgroundQueue) {
      return activeWorker;
    }
  }

  /**
   * Answer the next queued worker or {@code null} if the processor is paused or the queue is empty.
   * 
   * @return the next worker or {@code null}
   */
  public AnalysisWorker getNextWorker() {
    synchronized (backgroundQueue) {
      if (backgroundQueue.isEmpty()) {
        backgroundJob = null;
        backgroundQueue.notifyAll();
        return null;
      }
      return backgroundQueue.remove(0);
    }
  }

  /**
   * Answer the {@link AnalysisWorker}s in the queue.
   * 
   * @return an array of workers (not {@code null}, contains no {@code null}s)
   */
  public AnalysisWorker[] getQueueWorkers() {
    synchronized (backgroundQueue) {
      return backgroundQueue.toArray(new AnalysisWorker[backgroundQueue.size()]);
    }
  }

  /**
   * For each queued {@link AnalysisWorker}, remove that worker from the queue and call the
   * {@link AnalysisWorker#performAnalysis(AnalysisManager)} to perform analysis. Continue until the
   * queue is empty. This is typically called indirectly on a background thread via
   * {@link #startBackgroundAnalysis()}.
   * 
   * @param job The job on which the analysis is performed or {@code null} if none.
   */
  public void performAnalysis(Job job) {
    while (true) {
      AnalysisWorker worker;
      synchronized (backgroundQueue) {
        worker = getNextWorker();
        if (worker == null) {
          break;
        }
        activeWorker = worker;
      }
      try {
        if (job != null) {
          if (worker.contextManager instanceof Project) {
            job.setName("Analyzing " + ((Project) worker.contextManager).getResource().getName());
          } else if (worker.contextManager instanceof ProjectManager) {
            job.setName("Analyzing SDK");
          } else {
            job.setName("Analyzing");
          }
        }
        worker.performAnalysis(this);
      } finally {
        synchronized (backgroundQueue) {
          activeWorker = null;
        }
      }
    }
  }

  /**
   * Ensure that a worker is at the front of the queue to update the analysis for the context.
   * 
   * @param manager the manager containing the context to be analyzed (not {@code null})
   * @param context the context to be analyzed (not {@code null})
   * @see #performAnalysis(AnalysisManager)
   */
  public void performAnalysisInBackground(ContextManager manager, AnalysisContext context) {
    synchronized (backgroundQueue) {
      if (backgroundQueue.size() == 0 || backgroundQueue.get(0).getContext() != context) {
        addWorker(new AnalysisWorker(manager, context));
      }
    }
  }

  /**
   * Start a job to perform background analysis if it has not already been started.
   */
  public void startBackgroundAnalysis() {
    synchronized (backgroundQueue) {
      if (stopped) {
        return;
      }
      if (backgroundJob == null) {
        backgroundJob = new Job("Analyzing") {
          @Override
          protected IStatus run(IProgressMonitor monitor) {
            performAnalysis(backgroundJob);
            return Status.OK_STATUS;
          }
        };
        backgroundJob.setPriority(Job.BUILD);
      }
      backgroundJob.schedule();
    }
  }

  /**
   * Stop all background analysis and discard all pending work
   */
  public void stopBackgroundAnalysis() {
    synchronized (backgroundQueue) {
      stopped = true;
      if (activeWorker != null) {
        activeWorker.stop();
      }
      backgroundQueue.clear();
      if (backgroundJob != null) {
        backgroundJob.cancel();
        backgroundJob = null;
      }
      backgroundQueue.notifyAll();
    }
  }

  /**
   * Wait for any scheduled background analysis to complete or for the specified duration to elapse.
   * 
   * @param milliseconds the number of milliseconds to wait
   * @return {@code true} if the background analysis has completed, else {@code false}
   */
  public boolean waitForBackgroundAnalysis(long milliseconds) {
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
}
