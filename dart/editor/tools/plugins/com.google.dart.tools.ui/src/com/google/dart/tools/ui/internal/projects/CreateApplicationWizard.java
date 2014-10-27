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
import com.google.dart.tools.core.generator.AbstractSample;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.utilities.resource.IProjectUtilities;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.undo.CreateFolderOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
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
public class CreateApplicationWizard extends BasicNewResourceWizard {
  private NewApplicationCreationPage page;
  private IFile createdFile;
  private IProject newProject;

  public CreateApplicationWizard() {
    setWindowTitle("New Project");
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    addPage(page = new NewApplicationCreationPage());
  }

  @Override
  public boolean performFinish() {

    IPath locationPath = new Path(page.getLocationURI().getPath());

    DartToolsPlugin.getDefault().getDialogSettingsSection(
        NewApplicationCreationPage.NEW_APPPLICATION_SETTINGS).put(
        NewApplicationCreationPage.PARENT_DIR,
        locationPath.removeLastSegments(1).toPortableString());

    if (isNestedByAnExistingProject(locationPath)) {
      createFolder(locationPath);
    } else {
      createNewProject();
    }

    if (newProject == null) {
      return false;
    }

    if (createdFile != null) {
      selectAndReveal(createdFile);

      try {
        IDE.openEditor(getWorkbench().getActiveWorkbenchWindow().getActivePage(), createdFile);
      } catch (PartInitException e) {
        DartToolsPlugin.log(e);
      }
    } else {
      selectAndReveal(newProject);
    }

    return true;
  }

  private void createFolder(IPath path) {
    final AbstractSample sampleContent = page.getCurrentSample();

    IPath containerPath = path.removeLastSegments(1);

    IResource container = ResourceUtil.getResource(containerPath.toFile());

    if (container != null) {
      IPath newFolderPath = container.getFullPath().append(path.lastSegment());

      final IFolder newFolderHandle = IDEWorkbenchPlugin.getPluginWorkspace().getRoot().getFolder(
          newFolderPath);

      IRunnableWithProgress op = new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException {
          AbstractOperation op;
          op = new CreateFolderOperation(
              newFolderHandle,
              null,
              IDEWorkbenchMessages.WizardNewFolderCreationPage_title);
          try {

            IStatus status = op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));

            if (status.isOK()) {
              createdFile = createProjectContent(
                  newProject,
                  newFolderHandle,
                  newFolderHandle.getName(),
                  sampleContent);
            }

          } catch (ExecutionException e) {
            throw new InvocationTargetException(e);
          } catch (CoreException e) {
            throw new InvocationTargetException(e);
          }
        }
      };

      try {
        getContainer().run(true, true, op);
      } catch (InterruptedException e) {

      } catch (InvocationTargetException e) {
        // ExecutionExceptions are handled above, but unexpected runtime
        // exceptions and errors may still occur.
        IDEWorkbenchPlugin.log(getClass(), "createNewFolder()", e.getTargetException()); //$NON-NLS-1$
        MessageDialog.open(
            MessageDialog.ERROR,
            getContainer().getShell(),
            IDEWorkbenchMessages.WizardNewFolderCreationPage_internalErrorTitle,
            NLS.bind(
                IDEWorkbenchMessages.WizardNewFolder_internalError,
                e.getTargetException().getMessage()),
            SWT.SHEET);

      }

      newProject = newFolderHandle.getProject();
    }
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
    final String projectName = page.getProjectName();
    IProject projectHandle = page.getProjectHandle(projectName);
    if (projectHandle.exists()) {
      String newProjectName = ProjectUtils.generateUniqueNameFrom(projectName);
      projectHandle = page.getProjectHandle(newProjectName);
    }
    final AbstractSample sampleContent = page.getCurrentSample();
    final IProject newProjectHandle = projectHandle;
    // get a project descriptor
    URI location = page.getLocationURI();

    final IProjectDescription description = createProjectDescription(newProjectHandle, location);

    // create the new project operation
    IRunnableWithProgress op = new IRunnableWithProgress() {
      @Override
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        CreateProjectOperation op = new CreateProjectOperation(
            description,
            ResourceMessages.NewProject_windowTitle);
        try {
          IStatus status = op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));

          if (status.isOK()) {
            createdFile = createProjectContent(newProjectHandle, null, projectName, sampleContent);
          }
        } catch (ExecutionException e) {
          throw new InvocationTargetException(e);
        } catch (CoreException e) {
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
              NLS.bind(
                  ResourceMessages.NewProject_caseVariantExistsError,
                  newProjectHandle.getName()),
              cause));
        } else {
          status = new StatusAdapter(StatusUtil.newStatus(
              cause.getStatus().getSeverity(),
              ResourceMessages.NewProject_errorMessage,
              cause));
        }
        status.setProperty(
            IStatusAdapterConstants.TITLE_PROPERTY,
            ResourceMessages.NewProject_errorMessage);
        StatusManager.getManager().handle(status, StatusManager.BLOCK);
      } else {
        StatusAdapter status = new StatusAdapter(new Status(
            IStatus.WARNING,
            IDEWorkbenchPlugin.IDE_WORKBENCH,
            0,
            NLS.bind(ResourceMessages.NewProject_internalError, t.getMessage()),
            t));
        status.setProperty(
            IStatusAdapterConstants.TITLE_PROPERTY,
            ResourceMessages.NewProject_errorMessage);
        StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.BLOCK);
      }
      return null;
    }

    newProject = newProjectHandle;

    return newProject;
  }

  /**
   * Create a ProjectType.WEB project or a ProjectType.SERVER project.
   * 
   * @param project
   * @param projectType
   * @throws CoreException
   */
  private IFile createProjectContent(IProject project, IFolder folder, String name,
      AbstractSample sampleContent) throws CoreException {
    IProjectUtilities.configurePackagesFilter(project);

    if (sampleContent == null) {
      return null;
    }

    IContainer container = project;

    if (folder != null) {
      container = folder;
    }

    return sampleContent.generateInto(container, name);
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

  private boolean isNestedByAnExistingProject(IPath path) {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      IPath location = project.getLocation();
      if (location.isPrefixOf(path)) {
        newProject = project;
        return true;
      }
    }
    return false;
  }

}
