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
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
        public void run(IProgressMonitor pm) throws InterruptedException {
          waitReadyForRefactoring(pm);
        }
      });
      return true;
    } catch (Throwable ie) {
      return false;
    } finally {
      if (focusControl != null) {
        focusControl.setFocus();
      }
    }
  }

  /**
   * Waits until all background tasks affecting refactoring are finished.
   * 
   * @throws OperationCanceledException if {@link IProgressMonitor} was cancelled.
   */
  public static void waitReadyForRefactoring(IProgressMonitor pm) throws OperationCanceledException {
    waitReadyForRefactoring(pm, null);
  }

  /**
   * Waits until all background tasks affecting refactoring are finished.
   * 
   * @return {@code true} if waiting was successful or {@code Do It Now} button was pressed,
   *         {@code false} if cancelled.
   */
  public static boolean waitReadyForRefactoring2() {
    Control focusControl = Display.getCurrent().getFocusControl();
    try {
      final AtomicBoolean stopFlag = new AtomicBoolean();
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor pm) throws InterruptedException {
          waitReadyForRefactoring(pm, stopFlag);
        }
      };
      // run in ProgressMonitorDialog
      Shell parentShell = DartToolsPlugin.getActiveWorkbenchShell();
      ProgressMonitorDialog dialog = new ProgressMonitorDialog(parentShell) {
        @Override
        protected void createButtonsForButtonBar(Composite parent) {
          createButton(parent, IDialogConstants.OK_ID, "Do It Now", false);
          super.createButtonsForButtonBar(parent);
        }

        @Override
        protected void okPressed() {
          stopFlag.set(true);
          getProgressMonitor().setCanceled(true);
        }
      };
      dialog.run(true, true, runnable);
      // force Shell activation, otherwise we get NPE because "dialog" is disposed now
      parentShell.notifyListeners(SWT.Activate, null);
      // done
      return dialog.getReturnCode() == 0;
    } catch (Throwable ie) {
      return false;
    } finally {
      if (focusControl != null) {
        focusControl.setFocus();
      }
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

  /**
   * Waits until all background tasks affecting refactoring are finished.
   * 
   * @param stop is set to {@code true} when user wants to stop waiting and perform an operation
   *          with the available information; at the same time the given {@link IProgressMonitor} is
   *          also cancelled
   * @throws OperationCanceledException if {@link IProgressMonitor} was cancelled.
   */
  private static void waitReadyForRefactoring(IProgressMonitor pm, AtomicBoolean stop)
      throws OperationCanceledException {
    pm.beginTask("Waiting for background analysis...", IProgressMonitor.UNKNOWN);
    // builder
    try {
      IJobManager jobManager = Job.getJobManager();
      jobManager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, new SubProgressMonitor(pm, 1));
      jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, new SubProgressMonitor(pm, 1));
    } catch (InterruptedException e) {
      if (stop != null && stop.get()) {
        return;
      }
      throw new OperationCanceledException(e.getMessage());
    }
    // wait for AnalysisWorker(s)
    while (true) {
      if (pm.isCanceled()) {
        if (stop != null && stop.get()) {
          return;
        }
        throw new OperationCanceledException();
      }
      boolean done = AnalysisWorker.waitForBackgroundAnalysis(100);
      if (done) {
        break;
      }
    }
  }
}
