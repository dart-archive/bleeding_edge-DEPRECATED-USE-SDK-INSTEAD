/*
 * Copyright (c) 2012, the Dart project authors.
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

import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.internal.actions.WorkbenchRunnableAdapter;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.internal.ui.refactoring.ChangeExceptionHandler;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.InvocationTargetException;

/**
 * A helper class to execute a refactoring. The class takes care of pushing the undo change onto the
 * undo stack and folding editor edits into one editor undo object.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RefactoringExecutionHelper {

  private class Operation implements IWorkspaceRunnable {
    public Change fChange;
    public PerformChangeOperation fPerformChangeOperation;
    private final boolean fForked;
    private final boolean fForkChangeExecution;

    public Operation(boolean forked, boolean forkChangeExecution) {
      fForked = forked;
      fForkChangeExecution = forkChangeExecution;
    }

    @Override
    public void run(IProgressMonitor pm) throws CoreException {
      try {
        pm.beginTask("", fForked && !fForkChangeExecution ? 7 : 11); //$NON-NLS-1$
        pm.subTask(""); //$NON-NLS-1$

        final RefactoringStatus status = fRefactoring.checkAllConditions(new SubProgressMonitor(pm,
            4, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
        if (status.getSeverity() >= fStopSeverity) {
          final boolean[] canceled = {false};
          if (fForked) {
            fParent.getDisplay().syncExec(new Runnable() {
              @Override
              public void run() {
                canceled[0] = showStatusDialog(status);
              }
            });
          } else {
            canceled[0] = showStatusDialog(status);
          }
          if (canceled[0]) {
            throw new OperationCanceledException();
          }
        }

        fChange = fRefactoring.createChange(new SubProgressMonitor(pm, 2,
            SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
        fChange.initializeValidationData(new SubProgressMonitor(pm, 1,
            SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));

        fPerformChangeOperation = new PerformChangeOperation(fChange);//RefactoringUI.createUIAwareChangeOperation(fChange);
        fPerformChangeOperation.setUndoManager(
            RefactoringCore.getUndoManager(),
            fRefactoring.getName());
//        if (fRefactoring instanceof IScheduledRefactoring) {
//          fPerformChangeOperation.setSchedulingRule(((IScheduledRefactoring) fRefactoring).getSchedulingRule());
//        }

        if (!fForked || fForkChangeExecution) {
          fPerformChangeOperation.run(new SubProgressMonitor(pm, 4,
              SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
        }
      } finally {
        pm.done();
      }
    }

    /**
     * @param status the status to show
     * @return <code>true</code> iff the operation should be cancelled
     */
    private boolean showStatusDialog(RefactoringStatus status) {
      return UserInteractions.showStatusDialog.open(status, fParent, fRefactoring.getName());
    }
  }

  private final Refactoring fRefactoring;
  private final Shell fParent;
  private final IRunnableContext fExecContext;
  private final int fStopSeverity;

  private final int fSaveMode;

  /**
   * Creates a new refactoring execution helper.
   * 
   * @param refactoring the refactoring
   * @param stopSeverity a refactoring status constant from {@link RefactoringStatus}
   * @param saveMode a save mode from {@link RefactoringSaveHelper}
   * @param parent the parent shell
   * @param context the runnable context
   */
  public RefactoringExecutionHelper(Refactoring refactoring, int stopSeverity, int saveMode,
      Shell parent, IRunnableContext context) {
    super();
    Assert.isNotNull(refactoring);
    Assert.isNotNull(parent);
    Assert.isNotNull(context);
    fRefactoring = refactoring;
    fStopSeverity = stopSeverity;
    fParent = parent;
    fExecContext = context;
    fSaveMode = saveMode;
  }

  /**
   * Must be called in the UI thread.
   * 
   * @param fork if set, the operation will be forked
   * @param cancelable if set, the operation will be cancelable
   * @throws InterruptedException thrown when the operation is cancelled
   * @throws InvocationTargetException thrown when the operation failed to execute
   */
  public void perform(boolean fork, boolean cancelable) throws InterruptedException,
      InvocationTargetException {
    perform(fork, false, cancelable);
  }

  /**
   * Must be called in the UI thread.<br>
   * <strong>Use {@link #perform(boolean, boolean)} unless you know exactly what you are
   * doing!</strong>
   * 
   * @param fork if set, the operation will be forked
   * @param forkChangeExecution if the change should not be executed in the UI thread: This may not
   *          work in any case
   * @param cancelable if set, the operation will be cancelable
   * @throws InterruptedException thrown when the operation is cancelled
   * @throws InvocationTargetException thrown when the operation failed to execute
   */
  public void perform(boolean fork, boolean forkChangeExecution, boolean cancelable)
      throws InterruptedException, InvocationTargetException {
    Assert.isTrue(Display.getCurrent() != null);
    final IJobManager manager = Job.getJobManager();
    final ISchedulingRule rule;
//    if (fRefactoring instanceof IScheduledRefactoring) {
//      rule = ((IScheduledRefactoring) fRefactoring).getSchedulingRule();
//    } else 
    {
      rule = ResourcesPlugin.getWorkspace().getRoot();
    }
    try {
      try {
        Runnable r = new Runnable() {
          @Override
          public void run() {
            manager.beginRule(rule, null);
          }
        };
        BusyIndicator.showWhile(fParent.getDisplay(), r);
      } catch (OperationCanceledException e) {
        throw new InterruptedException(e.getMessage());
      }

      RefactoringSaveHelper saveHelper = new RefactoringSaveHelper(fSaveMode);
      if (!saveHelper.saveEditors(fParent)) {
        throw new InterruptedException();
      }
      final Operation op = new Operation(fork, forkChangeExecution);
      fRefactoring.setValidationContext(fParent);
      try {
        fExecContext.run(fork, cancelable, new WorkbenchRunnableAdapter(op, rule, true));
        if (fork && !forkChangeExecution && op.fPerformChangeOperation != null) {
          fExecContext.run(false, false, new WorkbenchRunnableAdapter(op.fPerformChangeOperation,
              rule, true));
        }

        if (op.fPerformChangeOperation != null) {
          RefactoringStatus validationStatus = op.fPerformChangeOperation.getValidationStatus();
          if (validationStatus != null && validationStatus.hasFatalError()) {
            MessageDialog.openError(fParent, fRefactoring.getName(), Messages.format(
                RefactoringMessages.RefactoringExecutionHelper_cannot_execute,
                validationStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL)));
            throw new InterruptedException();
          }
        }
      } catch (InvocationTargetException e) {
        PerformChangeOperation pco = op.fPerformChangeOperation;
        if (pco != null && pco.changeExecutionFailed()) {
          ChangeExceptionHandler handler = new ChangeExceptionHandler(fParent, fRefactoring);
          Throwable inner = e.getTargetException();
          if (inner instanceof RuntimeException) {
            handler.handle(pco.getChange(), (RuntimeException) inner);
          } else if (inner instanceof CoreException) {
            handler.handle(pco.getChange(), (CoreException) inner);
          } else {
            throw e;
          }
        } else {
          throw e;
        }
      } catch (OperationCanceledException e) {
        throw new InterruptedException(e.getMessage());
      } finally {
        saveHelper.triggerIncrementalBuild();
      }
    } finally {
      manager.endRule(rule);
      fRefactoring.setValidationContext(null);
    }
  }
}
