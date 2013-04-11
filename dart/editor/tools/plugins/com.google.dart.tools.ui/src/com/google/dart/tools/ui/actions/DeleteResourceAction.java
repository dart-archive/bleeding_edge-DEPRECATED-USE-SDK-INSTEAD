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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.undo.DeleteResourcesOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.internal.ide.actions.LTKLauncher;
import org.eclipse.ui.progress.WorkbenchJob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    DeleteProjectDialog(Shell parentShell, IResource[] projects) {
      super(parentShell, getTitle(projects), null, /* default window icon */
      getMessage(projects), MessageDialog.QUESTION, new String[] {
          IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1 /* no is default */);
      setShellStyle(getShellStyle() | SWT.SHEET);
    }

    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          newShell,
          IIDEHelpContextIds.DELETE_PROJECT_DIALOG);
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

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("DeleteResourceAction.runClick");
    try {

      final IResource[] resources = getSelectedResourcesArray();
      instrumentation.record(resources);

      // Cache paths now since they can't be retrieved post delete
      final IPath[] resourcePaths = new IPath[resources.length];
      for (int i = 0; i < resources.length; i++) {
        resourcePaths[i] = resources[i].getLocation();
      }

      // on Windows platform call out to system to do the delete, since 
      // Eclipse has no knowledge of junctions used in packages
      if (DartCore.isWindows()) {
        if (confirmDelete(resources)) {
          windowsDelete(resources);
          removeFromIgnores(resourcePaths);
        }

        instrumentation.metric("windows-delete", "true");

        return;
      }

      if (LTKLauncher.openDeleteWizard(getStructuredSelection())) {

        EditorUtility.closeOrphanedEditors();
        instrumentation.metric("DeleteWizardShown", "true");

        removeFromIgnores(resourcePaths);
        return;

      }

      // WARNING: do not query the selected resources more than once
      // since the selection may change during the run,
      // e.g. due to window activation when the prompt dialog is dismissed.
      // For more details, see Bug 60606 [Navigator] (data loss) Navigator
      // deletes/moves the wrong file
      if (!confirmDelete(resources)) {

        instrumentation.metric("Confirmed", "false");
        return;

      }

      Job deletionCheckJob = new InstrumentedJob(
          IDEWorkbenchMessages.DeleteResourceAction_checkJobName) {

        @Override
        public boolean belongsTo(Object family) {
          if (IDEWorkbenchMessages.DeleteResourceAction_jobName.equals(family)) {
            return true;
          }
          return super.belongsTo(family);
        }

        @Override
        protected IStatus doRun(IProgressMonitor monitor, UIInstrumentationBuilder instrumentation) {
          if (resources.length == 0) {
            instrumentation.metric("Problem", "Resources.length == 0");
            return Status.CANCEL_STATUS;
          }
          instrumentation.metric("resources-length", resources.length);
          scheduleDeleteJob(resources);

          removeFromIgnores(resourcePaths);
          return Status.OK_STATUS;
        }
      };

      deletionCheckJob.schedule();

    } finally {
      instrumentation.log();
    }
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
        msg = NLS.bind(
            IDEWorkbenchMessages.DeleteResourceAction_confirmLinkedResource1,
            resource.getName());
      } else {
        msg = NLS.bind(IDEWorkbenchMessages.DeleteResourceAction_confirm1, resource.getName());
      }
    } else {
      title = IDEWorkbenchMessages.DeleteResourceAction_titleN;
      if (containsLinkedResource(resources)) {
        msg = NLS.bind(
            IDEWorkbenchMessages.DeleteResourceAction_confirmLinkedResourceN,
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
    deleteContent = code == 0 /* YES */;
    return deleteContent;
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
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        IIDEHelpContextIds.DELETE_RESOURCE_ACTION);
    setId(ID);
  }

  private void removeFromIgnores(IPath[] resourcePaths) {
    if (resourcePaths != null) {
      for (IPath path : resourcePaths) {
        try {
          DartCore.removeFromIgnores(path);
        } catch (IOException e) {
          DartToolsPlugin.log(e);
        }
      }
    }
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
          final DeleteResourcesOperation op = new DeleteResourcesOperation(
              resourcesToDelete,
              IDEWorkbenchMessages.DeleteResourceAction_operationLabel,
              deleteContent);
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
              return op.execute(
                  monitor,
                  WorkspaceUndoUtil.getUIInfoAdapter(shellProvider.getShell()));
            }
            return statusJob.getResult();
          }
          return PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(
              op,
              monitor,
              WorkspaceUndoUtil.getUIInfoAdapter(shellProvider.getShell()));
        } catch (ExecutionException e) {
          if (e.getCause() instanceof CoreException) {
            return ((CoreException) e.getCause()).getStatus();
          }
          return new Status(IStatus.ERROR, IDEWorkbenchPlugin.IDE_WORKBENCH, e.getMessage(), e);
        }
      }

    };

    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        IDE.saveAllEditors(resourcesToDelete, false);
      }
    });

    deleteJob.setUser(true);
    deleteJob.schedule();
  }

  private void setShellProvider(IShellProvider provider) {
    shellProvider = provider;
  }

  /**
   * Method is called for all deletes on Windows platform, since Eclipse does not have support for
   * Junctions which are used by Pub to create symlinks.
   */
  private void windowsDelete(IResource[] resources) {

    List<IResource> resourceList = Arrays.asList(resources);

    List<IResource> folders = new ArrayList<IResource>();
    List<IResource> files = new ArrayList<IResource>();
    for (IResource resource : resources) {
      if (!resourceList.contains(resource.getParent())) {
        if (resource.getType() == IResource.FILE) {
          files.add(resource);
        } else {
          folders.add(resource);
        }
      }
    }

    List<String> commandsList = new ArrayList<String>();
    try {
      if (!files.isEmpty()) {
        commandsList.add("cmd");
        commandsList.add("/C");
        commandsList.add("del");
        commandsList.add("/q");
        for (IResource resource : files) {
          commandsList.add(resource.getLocation().toOSString());
        }
        ProcessBuilder builder = new ProcessBuilder(commandsList);
        Process process = builder.start();
        process.waitFor();
      }

      if (!folders.isEmpty()) {
        commandsList.clear();
        commandsList.add("cmd");
        commandsList.add("/C");
        commandsList.add("rmdir");
        commandsList.add("/s");
        commandsList.add("/q");
        for (IResource resource : folders) {
          commandsList.add(resource.getLocation().toOSString());
          if (resource instanceof IProject) {
            ((IProject) resource).delete(false, true, new NullProgressMonitor());
          }
        }
        ProcessBuilder builder = new ProcessBuilder(commandsList);
        Process process = builder.start();
        process.waitFor();
      }

      ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
          IResource.DEPTH_INFINITE,
          new NullProgressMonitor());
      EditorUtility.closeOrphanedEditors();

    } catch (Exception e) {
      DartToolsPlugin.log(e);
    }
  }
}
