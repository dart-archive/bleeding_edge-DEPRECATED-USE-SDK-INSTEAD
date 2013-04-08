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

package com.google.dart.tools.ui.internal.text.functions;

import com.google.common.collect.Lists;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.LinkedList;

/**
 * {@link DartReconcilerWorker} is used to schedule resolving elements required by
 * {@link DartEditor}s, activated from {@link DartReconciler}.
 */
public class DartReconcilerWorker {
  private static class Task {
    private final Project project;
    private final AnalysisContext context;

    public Task(Project project, AnalysisContext context) {
      this.project = project;
      this.context = context;
    }
  }

  /**
   * Queue of {@link Task}s.
   */
  private static class TaskQueue {
    private final LinkedList<Task> taskQueue = Lists.newLinkedList();

    /**
     * Adds new {@link Task} to the end of the queue.
     */
    public void add(Task task) {
      synchronized (taskQueue) {
        taskQueue.addLast(task);
        taskQueue.notifyAll();
      }
    }

    /**
     * @return <code>true</code> if this queue has no elements.
     */
    public boolean isEmpty() {
      synchronized (taskQueue) {
        return taskQueue.isEmpty();
      }
    }

    /**
     * Returns first {@link Task} (but does not remove it), waits if queue is empty.
     */
    public Task peekWait() throws InterruptedException {
      synchronized (taskQueue) {
        if (taskQueue.isEmpty()) {
          taskQueue.wait();
        }
        return taskQueue.getFirst();
      }
    }

    /**
     * Removes first {@link Task} from the queue.
     */
    public void remove() {
      synchronized (taskQueue) {
        taskQueue.removeFirst();
        taskQueue.notifyAll();
      }
    }
  }

  private static final Task STOP_TASK = new Task(null, null);

  private static final TaskQueue taskQueue = new TaskQueue();
  private static volatile boolean stopped = false;

  private static Thread thread = null;

  /**
   * Requests analysis of the {@link AnalysisContext} for given {@link Source}.
   * 
   * @param context the {@link AnalysisContext} to analyze, may be <code>null</code> - will be
   *          ignored.
   */
  public static void scheduleAnalysis(Project project, AnalysisContext context) {
    // may be stopped
    if (stopped) {
      return;
    }
    // check here to don't check in clients
    if (context == null) {
      return;
    }
    // OK, schedule task
    taskQueue.add(new Task(project, context));
    // ensure that thread in running
    ensureThreadStarted();
  }

  /**
   * Call this method to cancel the background thread.
   */
  public static void stop() {
    stopped = true;
    taskQueue.add(STOP_TASK);
  }

  /**
   * Waits until all tasks are executed or {@link IProgressMonitor} cancelled.
   * 
   * @return <code>true</code> if all tasks were executes or <code>false</code> if cancelled.
   */
  public static boolean waitForEmpty(IProgressMonitor pm) {
    while (true) {
      if (pm.isCanceled()) {
        return false;
      }
      if (taskQueue.isEmpty()) {
        return true;
      }
      ExecutionUtils.sleep(10);
    }
  }

  /**
   * Starts {@link #thread} if not started yet.
   */
  private synchronized static void ensureThreadStarted() {
    if (thread == null) {
      thread = new Thread("DartReconcilerWorker") {
        @Override
        public void run() {
          mainLoop();
        }
      };
      thread.start();
    }
  }

  /**
   * The loop executing {@link Task}s from the {@value #taskQueue}.
   */
  private static void mainLoop() {
    while (!stopped) {
      try {
        Task task = taskQueue.peekWait();
        // may be stop request
        if (task == STOP_TASK) {
          break;
        }
        // execute Task and remove
        try {
          AnalysisWorker worker = new AnalysisWorker(task.project, task.context);
          worker.performAnalysis();
        } catch (Throwable e) {
          DartCore.logError(e);
        } finally {
          taskQueue.remove();
        }
      } catch (Throwable e) {
        DartCore.logError(e);
      }
    }
  }
}
