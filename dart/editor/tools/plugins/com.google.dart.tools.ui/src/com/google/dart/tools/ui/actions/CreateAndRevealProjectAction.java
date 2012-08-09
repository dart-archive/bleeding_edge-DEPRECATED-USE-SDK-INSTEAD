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

import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage.ProjectType;
import com.google.dart.tools.ui.internal.projects.ProjectMessages;
import com.google.dart.tools.ui.internal.projects.ProjectUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import java.io.File;
import java.net.URI;
import java.text.MessageFormat;

/**
 * Creates projects in the workspace.
 */
public class CreateAndRevealProjectAction extends Action {

  //TODO(pquitslund): permit/handle overwriting existing projects

  private final IWorkbenchWindow window;
  private final String[] directories;
  private IStatus status;

  /**
   * Create the action.
   * 
   * @param window the workbench window
   * @param directories directory paths
   */
  public CreateAndRevealProjectAction(IWorkbenchWindow window, String... directories) {
    this.directories = directories;
    this.window = window;
  }

  /**
   * Return {@link Status#OK_STATUS} if creation succeeded, {@link Status#CANCEL_STATUS} otherwise.
   * (Returns null if the action has not been run yet.)
   */
  public IStatus getStatus() {
    return status;
  }

  @Override
  public void run() {
    status = Status.OK_STATUS; //will get unset if any creation fails
    for (String d : directories) {
      createAndRevealProject(d);
    }
  }

  protected void createAndRevealProject(String directoryPath) {
    Path path = new Path(directoryPath);
    String name = path.lastSegment();

    IProject projectHandle = getProjectHandle(name);
    if (projectHandle.exists()) {
      if (projectHandle.getLocation().equals(path)) {
        ProjectUtils.selectAndReveal(projectHandle);
        return;
      } else {
        name = generateUniqueNameFrom(name);
        projectHandle = getProjectHandle(name);
      }
    }
    if (!isNestedByAnExistingProject(path) && !nestsAnExistingProject(path)) {
      URI location = new File(directoryPath).toURI();

      IProject project = ProjectUtils.createNewProject(
          name,
          projectHandle,
          ProjectType.NONE,
          location,
          window,
          getShell());

      ProjectUtils.selectAndReveal(project);
    } else {
      status = Status.CANCEL_STATUS;
    }
  }

  private String generateUniqueNameFrom(String baseName) {
    int index = 1;
    int copyIndex = baseName.lastIndexOf("-"); //$NON-NLS-1$
    if (copyIndex > -1) {
      String trailer = baseName.substring(copyIndex + 1);
      if (isNumber(trailer)) {
        try {
          index = Integer.parseInt(trailer);
          baseName = baseName.substring(0, copyIndex);
        } catch (NumberFormatException nfe) {
        }
      }
    }
    String newName = baseName;
    while (getProjectHandle(newName).exists()) {
      newName = MessageFormat.format(
          ProjectMessages.CreateAndRevealProjectAction_projectName,
          new Object[] {baseName, Integer.toString(index)});
      index++;
    }
    return newName;
  }

  private IProject getProjectHandle(String name) {
    return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
  }

  private Shell getShell() {
    return window.getShell();
  }

  private boolean isNestedByAnExistingProject(IPath path) {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      IPath location = project.getLocation();
      if (location.isPrefixOf(path)) {
        IResource resource = ResourceUtil.getResource(path.toFile());
        ProjectUtils.selectAndReveal(resource);
        return true;
      }
    }
    return false;
  }

  private boolean isNumber(String string) {
    int numChars = string.length();
    if (numChars == 0) {
      return false;
    }
    for (int i = 0; i < numChars; i++) {
      if (!Character.isDigit(string.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean nestsAnExistingProject(IPath path) {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      IPath location = project.getLocation();
      if (path.isPrefixOf(location)) {
        String folderName = path.lastSegment();
        String projectName = project.getName();
        MessageDialog.openError(
            getShell(),
            ProjectMessages.OpenExistingFolderWizardAction_nesting_title,
            NLS.bind(
                ProjectMessages.OpenExistingFolderWizardAction_nesting_msg,
                folderName,
                projectName));
        return true;
      }
    }
    return false;
  }

}
