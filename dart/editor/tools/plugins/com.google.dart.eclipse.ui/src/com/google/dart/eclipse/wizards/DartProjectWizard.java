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
package com.google.dart.eclipse.wizards;

import com.google.dart.eclipse.DartEclipseUI;
import com.google.dart.eclipse.ui.internal.DartPerspective;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.generator.ApplicationGenerator;
import com.google.dart.tools.core.generator.DartIdentifierUtil;
import com.google.dart.tools.core.utilities.resource.IProjectUtilities;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage.ProjectType;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.StatusUtil;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Standard workbench wizard that creates a new Dart project resource in the workspace.
 */
@SuppressWarnings("restriction")
public class DartProjectWizard extends Wizard implements INewWizard {

  protected static void selectAndReveal(IResource resource, IWorkbenchWindow window) {
    // validate the input
    if (window == null || resource == null) {
      return;
    }
    IWorkbenchPage page = window.getActivePage();
    if (page == null) {
      return;
    }

    // get all the view and editor parts
    List<IWorkbenchPart> parts = new ArrayList<IWorkbenchPart>();
    IWorkbenchPartReference refs[] = page.getViewReferences();
    for (int i = 0; i < refs.length; i++) {
      IWorkbenchPart part = refs[i].getPart(false);
      if (part != null) {
        parts.add(part);
      }
    }
    refs = page.getEditorReferences();
    for (int i = 0; i < refs.length; i++) {
      if (refs[i].getPart(false) != null) {
        parts.add(refs[i].getPart(false));
      }
    }

    final ISelection selection = new StructuredSelection(resource);
    Iterator<IWorkbenchPart> itr = parts.iterator();
    while (itr.hasNext()) {
      IWorkbenchPart part = itr.next();

      // get the part's ISetSelectionTarget implementation
      ISetSelectionTarget target = null;
      if (part instanceof ISetSelectionTarget) {
        target = (ISetSelectionTarget) part;
      } else {
        target = (ISetSelectionTarget) part.getAdapter(ISetSelectionTarget.class);
      }

      if (target != null) {
        // select and reveal resource
        final ISetSelectionTarget finalTarget = target;
        window.getShell().getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            finalTarget.selectReveal(selection);
          }
        });
      }
    }
  }

  private IWorkbench workbench;
  private ISelection selection;
  private IProject newProject;

  private DartProjectWizardPage page;

  private String perspectiveId;
  private IFile createdSampleFile;

  public DartProjectWizard() {
    this(DartPerspective.ID);
  }

  protected DartProjectWizard(String perspectiveId) {
    this.perspectiveId = perspectiveId;
    setWindowTitle("New Dart Project");
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    page = new DartProjectWizardPage(selection);
    addPage(page);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.workbench = workbench;
    this.selection = selection;
  }

  @Override
  public boolean performFinish() {
    return createAndRevealNewProject();
  }

  protected void addProjectDescription(IProjectDescription description) {
    description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
    ICommand command = description.newCommand();
    command.setBuilderName(DartCore.DART_BUILDER_ID);
    description.setBuildSpec(new ICommand[] {command});
  }

  protected boolean createAndRevealNewProject() {

    createNewProject();

    if (newProject == null) {
      return false;
    }

    updatePerspective();

    if (createdSampleFile != null) {

      selectAndReveal(createdSampleFile);

      try {
        IDE.openEditor(workbench.getActiveWorkbenchWindow().getActivePage(), createdSampleFile);
      } catch (PartInitException e) {
        DartToolsPlugin.log(e);
      }
    } else {
      selectAndReveal(newProject);
    }

    return true;
  }

  protected void selectAndReveal(IResource newResource) {
    selectAndReveal(newResource, workbench.getActiveWorkbenchWindow());
  }

  private IProject createNewProject() {
    if (newProject != null) {
      return newProject;
    }

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

    final IProjectDescription description = new ProjectDescription();
    description.setName(page.getProjectName());

    String location = page.getProjectLocation();
    if (location != null) {
      description.setLocation(new Path(location));
    }

    addProjectDescription(description);

    String name = description.getName();

    final IProject project = root.getProject(name);

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

    // run the new project creation operation
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
              NLS.bind(ResourceMessages.NewProject_caseVariantExistsError, project.getName()),
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
        StatusAdapter status = new StatusAdapter(new Status(IStatus.WARNING,
            IDEWorkbenchPlugin.IDE_WORKBENCH, 0, NLS.bind(
                ResourceMessages.NewProject_internalError,
                t.getMessage()), t));
        status.setProperty(
            IStatusAdapterConstants.TITLE_PROPERTY,
            ResourceMessages.NewProject_errorMessage);
        StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.BLOCK);
      }
      return null;
    }

    try {

      IProjectUtilities.configurePackagesFilter(project);

      ProjectType contentType = page.getSampleContentType();

      if (contentType != ProjectType.NONE) {

        ApplicationGenerator generator = new ApplicationGenerator(project);

        generator.setApplicationLocation(project.getLocation().toOSString());
        generator.setApplicationName(DartIdentifierUtil.createValidIdentifier(name));
        generator.setWebApplication(contentType == ProjectType.WEB);
        generator.setHasPubSupport(page.hasPubSupport());

        generator.execute(new NullProgressMonitor());

        createdSampleFile = generator.getFile();
      }

    } catch (CoreException e) {
      DartEclipseUI.logError(e);
    }

    newProject = project;

    return newProject;
  }

  private void updatePerspective() {
    try {
      PlatformUI.getWorkbench().showPerspective(
          perspectiveId,
          PlatformUI.getWorkbench().getActiveWorkbenchWindow());
    } catch (WorkbenchException e) {
      DartEclipseUI.logError(e);
    }
  }

}
