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
package com.google.dart.tools.ui.internal.projects;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.generator.ApplicationGenerator;
import com.google.dart.tools.core.generator.DartIdentifierUtil;
import com.google.dart.tools.core.utilities.resource.IProjectUtilities;
import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage.ProjectType;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.StatusUtil;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

/**
 * Project-related utilities.
 */
@SuppressWarnings("restriction")
public class ProjectUtils {

  /**
   * Creates a new project resource.
   * 
   * @param name the project name
   * @param newProjectHandle the project handle
   * @param projectType the type of project
   * @param location the location
   * @param runnableContext a context for executing the creation operation
   * @param shell the shell (for UI context)
   * @return the created project resource, or <code>null</code> if the project was not created
   */
  public static IProject createNewProject(String name, final IProject newProjectHandle,
      final ProjectType projectType, URI location, final IRunnableContext runnableContext,
      final Shell shell) {

    final IProjectDescription description = createProjectDescription(newProjectHandle, location);

    // create the new project operation
    IRunnableWithProgress op = new IRunnableWithProgress() {
      @Override
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        CreateProjectOperation op = new CreateProjectOperation(
            description,
            ResourceMessages.NewProject_windowTitle);
        try {
          IStatus status = op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(shell));

          if (status.isOK() && projectType != ProjectType.NONE) {
            createProjectContent(newProjectHandle, projectType);
          }
        } catch (ExecutionException e) {
          throw new InvocationTargetException(e);
        } catch (CoreException e) {
          throw new InvocationTargetException(e);
        }
      }
    };

    try {
      runnableContext.run(true, true, op);
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
    try {
      IProjectUtilities.configurePackagesFilter(newProjectHandle);
    } catch (CoreException e) {
      DartCore.logError("Could not set package filter on folder " + newProjectHandle.getName(), e);
    }

    return newProjectHandle;
  }

  /**
   * Selects and reveals the newly added resource in all parts of the active workbench window's
   * active page.
   * 
   * @see ISetSelectionTarget
   */
  public static void selectAndReveal(IResource newResource) {
    BasicNewResourceWizard.selectAndReveal(
        newResource,
        PlatformUI.getWorkbench().getActiveWorkbenchWindow());
  }

  private static IFile createProjectContent(IProject project, ProjectType projectType)
      throws CoreException {
    ApplicationGenerator generator = new ApplicationGenerator(project);

    generator.setApplicationLocation(project.getLocation().toOSString());
    generator.setApplicationName(DartIdentifierUtil.createValidIdentifier(project.getName()));
    generator.setWebApplication(projectType == ProjectType.WEB);

    generator.execute(new NullProgressMonitor());

    return generator.getFile();
  }

  private static IProjectDescription createProjectDescription(IProject project, URI location) {
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
