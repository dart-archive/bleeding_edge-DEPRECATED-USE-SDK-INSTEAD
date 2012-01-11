/*
 * Copyright 2012 Google Inc.
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

package com.google.dart.tools.ui.internal.projects;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.StatusUtil;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

/**
 * Create project wizard.
 */
@SuppressWarnings("restriction")
public class CreateProjectWizard extends BasicNewResourceWizard {

  private NewProjectCreationPage page;
  private IProject newProject;

  @Override
  public void addPages() {
    super.addPages();
    addPage(page = new NewProjectCreationPage());
  }

  @Override
  public boolean performFinish() {
    createNewProject();

    if (newProject == null) {
      return false;
    }

    // TODO(pquitslund): reveal, etc.
    return true;
  }

  /**
   * Creates a new project resource with the selected name.
   * <p>
   * In normal usage, this method is invoked after the user has pressed Finish on the wizard; the
   * enablement of the Finish button implies that all controls on the pages currently contain valid
   * values.
   * </p>
   * <p>
   * Note that this wizard caches the new project once it has been successfully created; subsequent
   * invocations of this method will answer the same project resource without attempting to create
   * it again.
   * </p>
   * 
   * @return the created project resource, or <code>null</code> if the project was not created
   */
  private IProject createNewProject() {
    if (newProject != null) {
      return newProject;
    }

    // get a project handle
    final IProject newProjectHandle = page.getProjectHandle();

    // get a project descriptor
    URI location = page.getLocationURI();

    final IProjectDescription description = createProjectDescription(newProjectHandle, location);

    // create the new project operation
    IRunnableWithProgress op = new IRunnableWithProgress() {

      @Override
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        CreateProjectOperation op = new CreateProjectOperation(description,
            ResourceMessages.NewProject_windowTitle);
        try {
          op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
        } catch (ExecutionException e) {
          throw new InvocationTargetException(e);
        }
      }
    };

    try {
      getContainer().run(true, true, op);
    } catch (InterruptedException e) {
      return null;
    } catch (InvocationTargetException e) {
      Throwable t = e.getTargetException();
      if (t instanceof ExecutionException && t.getCause() instanceof CoreException) {
        CoreException cause = (CoreException) t.getCause();
        StatusAdapter status;
        if (cause.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
          status = new StatusAdapter(StatusUtil.newStatus(
              IStatus.WARNING,
              NLS.bind(ResourceMessages.NewProject_caseVariantExistsError,
                  newProjectHandle.getName()), cause));
        } else {
          status = new StatusAdapter(StatusUtil.newStatus(cause.getStatus().getSeverity(),
              ResourceMessages.NewProject_errorMessage, cause));
        }
        status.setProperty(IStatusAdapterConstants.TITLE_PROPERTY,
            ResourceMessages.NewProject_errorMessage);
        StatusManager.getManager().handle(status, StatusManager.BLOCK);
      } else {
        StatusAdapter status = new StatusAdapter(new Status(IStatus.WARNING,
            IDEWorkbenchPlugin.IDE_WORKBENCH, 0, NLS.bind(
                ResourceMessages.NewProject_internalError, t.getMessage()), t));
        status.setProperty(IStatusAdapterConstants.TITLE_PROPERTY,
            ResourceMessages.NewProject_errorMessage);
        StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.BLOCK);
      }
      return null;
    }

    newProject = newProjectHandle;

    return newProject;
  }

  private IProjectDescription createProjectDescription(IProject project, URI location) {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProjectDescription description = workspace.newProjectDescription(project.getName());
    description.setLocationURI(location);
    description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
    ICommand command = description.newCommand();
    command.setBuilderName(DartCore.DART_BUILDER_ID);
    description.setBuildSpec(new ICommand[] {command});
    return description;
  }

}
