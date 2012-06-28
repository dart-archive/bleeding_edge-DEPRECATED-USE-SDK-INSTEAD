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
package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.ui.DartPluginImages;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.StatusUtil;

import java.lang.reflect.InvocationTargetException;

/**
 * Standard action for refreshing selected resources in the workspace.
 */
@SuppressWarnings("restriction")
public class RefreshAction extends org.eclipse.ui.actions.RefreshAction {

  private final FilesView view;

  /**
   * Creates a new action.
   */
  public RefreshAction(FilesView view) {
    super(new SameShellProvider(view.getShell()));
    DartPluginImages.setLocalImageDescriptors(this, "refresh.gif");//$NON-NLS-1$
    setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
    this.view = view;
  }

  @Override
  public void run() {

    //viewer refresh smarts based on a similar action for the eclipse Resource Navigator
    //(see org.eclipse.ui.views.navigator.WorkspaceActionGroup)

    final IStatus[] errorStatus = new IStatus[1];
    errorStatus[0] = Status.OK_STATUS;
    final WorkspaceModifyOperation op = (WorkspaceModifyOperation) createOperation(errorStatus);
    WorkspaceJob job = new WorkspaceJob("refresh") { //$NON-NLS-1$

      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        try {
          op.run(monitor);
          Shell shell = view.getShell();
          if (shell != null && !shell.isDisposed()) {
            shell.getDisplay().asyncExec(new Runnable() {
              @Override
              public void run() {
                TreeViewer viewer = view.getViewer();
                if (viewer != null && viewer.getControl() != null
                    && !viewer.getControl().isDisposed()) {
                  viewer.refresh();
                }
              }
            });
          }
        } catch (InvocationTargetException e) {
          String msg = NLS.bind(
              IDEWorkbenchMessages.WorkspaceAction_logTitle,
              getClass().getName(),
              e.getTargetException());
          throw new CoreException(StatusUtil.newStatus(IStatus.ERROR, msg, e.getTargetException()));
        } catch (InterruptedException e) {
          return Status.CANCEL_STATUS;
        }
        return errorStatus[0];
      }

    };
    ISchedulingRule rule = op.getRule();
    if (rule != null) {
      job.setRule(rule);
    }
    job.setUser(true);
    job.schedule();
  }

}
