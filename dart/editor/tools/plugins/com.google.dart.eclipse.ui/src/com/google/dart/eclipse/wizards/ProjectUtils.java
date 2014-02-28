/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.eclipse.wizards;

import com.google.dart.eclipse.DartEclipseUI;
import com.google.dart.eclipse.ui.internal.DartPerspective;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.resource.IProjectUtilities;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.StatusUtil;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;

import java.lang.reflect.InvocationTargetException;

/**
 * Utility methods for creating projects
 */
@SuppressWarnings("restriction")
public class ProjectUtils {

  public static void addProjectDescription(IProjectDescription description) {
    description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
    ICommand command = description.newCommand();
    command.setBuilderName(DartCore.DART_BUILDER_ID);
    description.setBuildSpec(new ICommand[] {command});
  }

  public static IProject createNewProject(String projectName, String projectLocation,
      final IRunnableContext runnableContext, final Shell shell) {

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

    final IProjectDescription description = new ProjectDescription();
    description.setName(projectName);

    if (projectLocation != null
        && !projectLocation.startsWith(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString())) {
      description.setLocation(new Path(projectLocation));
    }

    addProjectDescription(description);

    String name = description.getName();

    final IProject project = root.getProject(name);

    // create the new project operation
    IRunnableWithProgress op = new IRunnableWithProgress() {
      @Override
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        CreateProjectOperation op = new CreateProjectOperation(description, "Creating project");
        try {
          op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(shell));
        } catch (ExecutionException e) {
          throw new InvocationTargetException(e);
        }
      }
    };

    // run the new project creation operation
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
          status = new StatusAdapter(StatusUtil.newStatus(IStatus.WARNING,
              NLS.bind(ResourceMessages.NewProject_caseVariantExistsError, project.getName()),
              cause));
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

    try {
      IProjectUtilities.configurePackagesFilter(project);

    } catch (CoreException e) {
      DartEclipseUI.logError(e);
    }

    return project;
  }

  public static void updatePerspective() {
    try {
      PlatformUI.getWorkbench().showPerspective(DartPerspective.ID,
          PlatformUI.getWorkbench().getActiveWorkbenchWindow());
    } catch (WorkbenchException e) {
      DartEclipseUI.logError(e);
    }
  }

}
