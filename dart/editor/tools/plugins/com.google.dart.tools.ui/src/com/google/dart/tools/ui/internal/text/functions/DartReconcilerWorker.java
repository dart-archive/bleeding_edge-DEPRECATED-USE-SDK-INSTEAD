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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

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

  private static final BlockingQueue<Task> taskQueue = new LinkedBlockingDeque<Task>();
  private static volatile boolean stopped = false;
  private static Thread thread = null;

  /**
   * Requests analysis of the {@link AnalysisContext} for given {@link Source}.
   * 
   * @param source the {@link Source} to analyze, may be <code>null</code> - will be ignored.
   */
  public static void scheduleAnalysis(Project project, Source source) {
    // may be stopped
    if (stopped) {
      return;
    }
    // check here to don't check in clients
    if (source == null) {
      return;
    }
    // OK, schedule task
    AnalysisContext context = source.getContext();
    taskQueue.offer(new Task(project, context));
    // ensure that thread in running
    ensureThreadStarted();
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
        Task task = taskQueue.take();
        AnalysisWorker worker = new AnalysisWorker(task.project, task.context);
        worker.performAnalysis();
      } catch (Throwable e) {
        DartCore.logError(e);
        ExecutionUtils.sleep(10);
      }
    }
  }

  /**
   * Call this method to cancel the background thread.
   */
  public void stop() {
    stopped = true;
  }
}
