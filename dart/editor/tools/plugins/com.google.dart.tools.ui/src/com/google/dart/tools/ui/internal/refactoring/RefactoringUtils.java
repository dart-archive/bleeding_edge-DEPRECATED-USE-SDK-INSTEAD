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

package com.google.dart.tools.ui.internal.refactoring;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for refactoring UI.
 */
public class RefactoringUtils {
  /**
   * Waits until all background tasks affecting refactoring are finished.
   * 
   * @return {@code true} if waiting was successful, {@code false} if cancelled.
   */
  public static boolean waitReadyForRefactoring() {
    Control focusControl = Display.getCurrent().getFocusControl();
    try {
      IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
      progressService.busyCursorWhile(new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor pm) throws InvocationTargetException, InterruptedException {
          waitReadyForRefactoring(pm);
        }
      });
      return true;
    } catch (Throwable ie) {
      return false;
    } finally {
      focusControl.setFocus();
    }
  }

  /**
   * Waits until all background tasks affecting refactoring are finished.
   * 
   * @throws OperationCanceledException if {@link IProgressMonitor} was cancelled.
   */
  public static void waitReadyForRefactoring(IProgressMonitor pm)
      throws OperationCanceledException {
    pm.beginTask("Waiting for background analysis...", IProgressMonitor.UNKNOWN);
    // builder
    try {
      IJobManager jobManager = Job.getJobManager();
      jobManager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, new SubProgressMonitor(pm, 1));
      jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, new SubProgressMonitor(pm, 1));
    } catch (InterruptedException e) {
      throw new OperationCanceledException(e.getMessage());
    }
    // wait for AnalysisWorker(s)
    while (true) {
      if (pm.isCanceled()) {
        throw new OperationCanceledException();
      }
      boolean done = AnalysisWorker.waitForBackgroundAnalysis(5);
      if (done) {
        break;
      }
      Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * User just stopped typing new name in the editor. Wait for resolved CompilationUnit.
   * 
   * @throws OperationCanceledException if {@link IProgressMonitor} was cancelled.
   */
  public static void waitResolvedCompilationUnit(DartEditor editor, IProgressMonitor pm) {
    if (editor == null) {
      return;
    }
    while (true) {
      if (pm.isCanceled()) {
        throw new OperationCanceledException();
      }
      if (editor.getInputUnit() != null) {
        break;
      }
      Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS);
    }
  }
}
