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
package com.google.dart.tools.ui.actions;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.ide.undo.DeleteResourcesOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.internal.ide.actions.LTKLauncher;
import org.eclipse.ui.progress.WorkbenchJob;

import java.util.List;

/**
 * Standard action for deleting the currently selected resources.
 * <p>
 * NOTE: based on {@link org.eclipse.ui.actions.DeleteResourceAction} and modified to improve
 * confirmation flow.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings("restriction")
public class DeleteResourceAction extends SelectionListenerAction {

  static class DeleteProjectDialog extends MessageDialog {

    static String getMessage(IResource[] projects) {
      if (projects.length == 1) {
        IProject project = (IProject) projects[0];
        return NLS.bind(ActionMessages.DeleteResourceAction_confirmProject1, project.getName());
      }
      return NLS.bind(ActionMessages.DeleteResourceAction_confirmProjectN, new Integer(
          projects.length));
    }

    static String getTitle(IResource[] projects) {
      if (projects.length == 1) {
        return ActionMessages.DeleteResourceAction_titleProject1;
      }
      return ActionMessages.DeleteResourceAction_titleProjectN;
    }

    private IResource[] projects;

    private boolean deleteContent = false;

    private Button radio1;
    private Button radio2;

    private SelectionListener selectionListener = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        Button button = (Button) e.widget;
        if (button.getSelection()) {
          deleteContent = (button == radio1);
        }
      }
    };

    DeleteProjectDialog(Shell parentShell, IResource[] projects) {
      super(parentShell, getTitle(projects), null, /* default window icon */
      getMessage(projects), MessageDialog.QUESTION, new String[] {
          IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0 /* yes is default */);
      this.projects = projects;
      setShellStyle(getShellStyle() | SWT.SHEET);
    }

    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell,
          IIDEHelpContextIds.DELETE_PROJECT_DIALOG);
    }

    @Override
    protected Control createCustomArea(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(new GridLayout());
      radio1 = new Button(composite, SWT.RADIO);
      radio1.addSelectionListener(selectionListener);
      String text1;
      if (projects.length == 1) {
        IProject project = (IProject) projects[0];
        if (project == null || project.getLocation() == null) {
          text1 = ActionMessages.DeleteResourceAction_deleteContentsN;
        } else {
          text1 = ActionMessages.DeleteResourceAction_deleteContents1;
        }
      } else {
        text1 = ActionMessages.DeleteResourceAction_deleteContentsN;
      }
      radio1.setText(text1);
      radio1.setFont(parent.getFont());

      // Add explanatory label that the action cannot be undone.
      // We can't put multi-line formatted text in a radio button,
      // so we have to create a separate label.
      Label detailsLabel = new Label(composite, SWT.LEFT);
      detailsLabel.setText(ActionMessages.DeleteResourceAction_deleteContentsDetails);
      detailsLabel.setFont(parent.getFont());
      // indent the explanatory label
      GridData data = new GridData();
      data.horizontalIndent = IDialogConstants.INDENT;
      detailsLabel.setLayoutData(data);
      // add a listener so that clicking on the label selects the
      // corresponding radio button.
      // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=172574
      detailsLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          deleteContent = true;
          radio1.setSelection(deleteContent);
          radio2.setSelection(!deleteContent);
        }
      });
      // Add a spacer label
      new Label(composite, SWT.LEFT);

      radio2 = new Button(composite, SWT.RADIO);
      radio2.addSelectionListener(selectionListener);
      String text2 = IDEWorkbenchMessages.DeleteResourceAction_doNotDeleteContents;
      radio2.setText(text2);
      radio2.setFont(parent.getFont());

      // set initial state
      radio1.setSelection(deleteContent);
      radio2.setSelection(!deleteContent);

      return composite;
    }

    boolean getDeleteContent() {
      return deleteContent;
    }

  }

  /**
   * The id of this action.
   */
  public static final String ID = PlatformUI.PLUGIN_ID + ".DeleteResourceAction";//$NON-NLS-1$

  private IShellProvider shellProvider = null;

  /**
   * Whether or not we are deleting content for projects.
   */
  private boolean deleteContent = false;

  private String[] modelProviderIds;

  /**
   * Creates a new delete resource action.
   * 
   * @param provider the shell provider to use. Must not be <code>null</code>.
   */
  public DeleteResourceAction(IShellProvider provider) {
    super(IDEWorkbenchMessages.DeleteResourceAction_text);
    Assert.isNotNull(provider);
    initAction();
    setShellProvider(provider);
  }

  /**
   * Returns the model provider ids that are known to the client that instantiated this operation.
   * 
   * @return the model provider ids that are known to the client that instantiated this operation.
   */
  public String[] getModelProviderIds() {
    return modelProviderIds;
  }

  @Override
  public void run() {
    final IResource[] resources = getSelectedResourcesArray();

    if (LTKLauncher.openDeleteWizard(getStructuredSelection())) {
      return;
    }

    // WARNING: do not query the selected resources more than once
    // since the selection may change during the run,
    // e.g. due to window activation when the prompt dialog is dismissed.
    // For more details, see Bug 60606 [Navigator] (data loss) Navigator
    // deletes/moves the wrong file
    if (!confirmDelete(resources)) {
      return;
    }

    Job deletionCheckJob = new Job(IDEWorkbenchMessages.DeleteResourceAction_checkJobName) {

      @Override
      public boolean belongsTo(Object family) {
        if (IDEWorkbenchMessages.DeleteResourceAction_jobName.equals(family)) {
          return true;
        }
        return super.belongsTo(family);
      }

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        if (resources.length == 0) {
          return Status.CANCEL_STATUS;
        }
        scheduleDeleteJob(resources);
        return Status.OK_STATUS;
      }
    };

    deletionCheckJob.schedule();

  }

  /**
   * Sets the model provider ids that are known to the client that instantiated this operation. Any
   * potential side effects reported by these models during validation will be ignored.
   * 
   * @param modelProviderIds the model providers known to the client who is using this operation.
   */
  public void setModelProviderIds(String[] modelProviderIds) {
    this.modelProviderIds = modelProviderIds;
  }

  /**
   * The <code>DeleteResourceAction</code> implementation of this
   * <code>SelectionListenerAction</code> method disables the action if the selection contains
   * phantom resources or non-resources
   */
  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    return super.updateSelection(selection) && canDelete(getSelectedResourcesArray());
  }

  /**
   * Returns whether delete can be performed on the current selection.
   * 
   * @param resources the selected resources
   * @return <code>true</code> if the resources can be deleted, and <code>false</code> if the
   *         selection contains non-resources or phantom resources
   */
  private boolean canDelete(IResource[] resources) {
    // allow only projects or only non-projects to be selected;
    // note that the selection may contain multiple types of resource
    if (!(containsOnlyProjects(resources) || containsOnlyNonProjects(resources))) {
      return false;
    }

    if (resources.length == 0) {
      return false;
    }
    // Return true if everything in the selection exists.
    for (int i = 0; i < resources.length; i++) {
      IResource resource = resources[i];
      if (resource.isPhantom()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Asks the user to confirm a delete operation.
   * 
   * @param resources the selected resources
   * @return <code>true</code> if the user says to go ahead, and <code>false</code> if the deletion
   *         should be abandoned
   */
  private boolean confirmDelete(IResource[] resources) {
    if (containsOnlyProjects(resources)) {
      return confirmDeleteProjects(resources);
    }
    return confirmDeleteNonProjects(resources);

  }

  /**
   * Asks the user to confirm a delete operation, where the selection contains no projects.
   * 
   * @param resources the selected resources
   * @return <code>true</code> if the user says to go ahead, and <code>false</code> if the deletion
   *         should be abandoned
   */
  private boolean confirmDeleteNonProjects(IResource[] resources) {
    String title;
    String msg;
    if (resources.length == 1) {
      title = IDEWorkbenchMessages.DeleteResourceAction_title1;
      IResource resource = resources[0];
      if (resource.isLinked()) {
        msg = NLS.bind(IDEWorkbenchMessages.DeleteResourceAction_confirmLinkedResource1,
            resource.getName());
      } else {
        msg = NLS.bind(IDEWorkbenchMessages.DeleteResourceAction_confirm1, resource.getName());
      }
    } else {
      title = IDEWorkbenchMessages.DeleteResourceAction_titleN;
      if (containsLinkedResource(resources)) {
        msg = NLS.bind(IDEWorkbenchMessages.DeleteResourceAction_confirmLinkedResourceN,
            new Integer(resources.length));
      } else {
        msg = NLS.bind(IDEWorkbenchMessages.DeleteResourceAction_confirmN, new Integer(
            resources.length));
      }
    }
    return MessageDialog.openQuestion(shellProvider.getShell(), title, msg);
  }

  /**
   * Asks the user to confirm a delete operation, where the selection contains only projects. Also
   * remembers whether project content should be deleted.
   * 
   * @param resources the selected resources
   * @return <code>true</code> if the user says to go ahead, and <code>false</code> if the deletion
   *         should be abandoned
   */
  private boolean confirmDeleteProjects(IResource[] resources) {
    DeleteProjectDialog dialog = new DeleteProjectDialog(shellProvider.getShell(), resources);
    int code = dialog.open();
    deleteContent = dialog.getDeleteContent();
    return code == 0; // YES
  }

  /**
   * Returns whether the selection contains linked resources.
   * 
   * @param resources the selected resources
   * @return <code>true</code> if the resources contain linked resources, and <code>false</code>
   *         otherwise
   */
  private boolean containsLinkedResource(IResource[] resources) {
    for (int i = 0; i < resources.length; i++) {
      IResource resource = resources[i];
      if (resource.isLinked()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether the selection contains only non-projects.
   * 
   * @param resources the selected resources
   * @return <code>true</code> if the resources contains only non-projects, and <code>false</code>
   *         otherwise
   */
  private boolean containsOnlyNonProjects(IResource[] resources) {
    int types = getSelectedResourceTypes(resources);
    // check for empty selection
    if (types == 0) {
      return false;
    }
    // note that the selection may contain multiple types of resource
    return (types & IResource.PROJECT) == 0;
  }

  /**
   * Returns whether the selection contains only projects.
   * 
   * @param resources the selected resources
   * @return <code>true</code> if the resources contains only projects, and <code>false</code>
   *         otherwise
   */
  private boolean containsOnlyProjects(IResource[] resources) {
    int types = getSelectedResourceTypes(resources);
    // note that the selection may contain multiple types of resource
    return types == IResource.PROJECT;
  }

  /**
   * Return an array of the currently selected resources.
   * 
   * @return the selected resources
   */
  private IResource[] getSelectedResourcesArray() {
    List<?> selection = getSelectedResources();
    IResource[] resources = new IResource[selection.size()];
    selection.toArray(resources);
    return resources;
  }

  /**
   * Returns a bit-mask containing the types of resources in the selection.
   * 
   * @param resources the selected resources
   */
  private int getSelectedResourceTypes(IResource[] resources) {
    int types = 0;
    for (int i = 0; i < resources.length; i++) {
      types |= resources[i].getType();
    }
    return types;
  }

  /**
   * Action initialization.
   */
  private void initAction() {
    setToolTipText(IDEWorkbenchMessages.DeleteResourceAction_toolTip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IIDEHelpContextIds.DELETE_RESOURCE_ACTION);
    setId(ID);
  }

  /**
   * Schedule a job to delete the resources to delete.
   * 
   * @param resourcesToDelete
   */
  private void scheduleDeleteJob(final IResource[] resourcesToDelete) {
    // use a non-workspace job with a runnable inside so we can avoid
    // periodic updates
    Job deleteJob = new Job(IDEWorkbenchMessages.DeleteResourceAction_jobName) {
      @Override
      public boolean belongsTo(Object family) {
        if (IDEWorkbenchMessages.DeleteResourceAction_jobName.equals(family)) {
          return true;
        }
        return super.belongsTo(family);
      }

      @Override
      public IStatus run(final IProgressMonitor monitor) {
        try {
          final DeleteResourcesOperation op = new DeleteResourcesOperation(resourcesToDelete,
              IDEWorkbenchMessages.DeleteResourceAction_operationLabel, deleteContent);
          op.setModelProviderIds(getModelProviderIds());
          // If we are deleting projects and their content, do not
          // execute the operation in the undo history, since it cannot be
          // properly restored.  Just execute it directly so it won't be
          // added to the undo history.
          if (deleteContent && containsOnlyProjects(resourcesToDelete)) {
            // We must compute the execution status first so that any user prompting
            // or validation checking occurs.  Do it in a syncExec because
            // we are calling this from a Job.
            WorkbenchJob statusJob = new WorkbenchJob("Status checking") { //$NON-NLS-1$
              @Override
              public IStatus runInUIThread(IProgressMonitor monitor) {
                return op.computeExecutionStatus(monitor);
              }

            };

            statusJob.setSystem(true);
            statusJob.schedule();
            try {//block until the status is ready
              statusJob.join();
            } catch (InterruptedException e) {
              //Do nothing as status will be a cancel
            }

            if (statusJob.getResult().isOK()) {
              return op.execute(monitor,
                  WorkspaceUndoUtil.getUIInfoAdapter(shellProvider.getShell()));
            }
            return statusJob.getResult();
          }
          return PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(op,
              monitor, WorkspaceUndoUtil.getUIInfoAdapter(shellProvider.getShell()));
        } catch (ExecutionException e) {
          if (e.getCause() instanceof CoreException) {
            return ((CoreException) e.getCause()).getStatus();
          }
          return new Status(IStatus.ERROR, IDEWorkbenchPlugin.IDE_WORKBENCH, e.getMessage(), e);
        }
      }

    };
    deleteJob.setUser(true);
    deleteJob.schedule();
  }

  private void setShellProvider(IShellProvider provider) {
    shellProvider = provider;
  }
}
