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
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import java.net.URI;

/**
 * Opens the "Open..." dialog.
 */
public class OpenExternalFolderDialogAction extends AbstractInstrumentedAction implements
    IWorkbenchAction {

  private static final String ACTION_ID = "com.google.dart.tools.ui.folder.open";
  private final IWorkbenchWindow window;

  public OpenExternalFolderDialogAction(IWorkbenchWindow window) {
    setText(ActionMessages.OpenExistingFolderWizardAction_text);
    setDescription(ActionMessages.OpenExistingFolderWizardAction_description);
    setToolTipText(ActionMessages.OpenExistingFolderWizardAction_tooltip);
    setId(ACTION_ID);
    this.window = window;
  }

  @Override
  public void dispose() {
  }

  @Override
  public void run() {
    String directory = new DirectoryDialog(getShell()).open();
    if (directory == null) {
      return;
    }

    Path path = new Path(directory);
    String name = path.lastSegment();

    IProject projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    if (projectHandle.exists()) {
      ProjectUtils.selectAndReveal(projectHandle);
    } else if (!isNestedByAnExistingProject(path) && !nestsAnExistingProject(path)) {
      URI location = URI.create(directory);
      ProjectUtils.createNewProject(name, projectHandle, ProjectType.NONE, location, window,
          getShell());
    }
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

  private boolean nestsAnExistingProject(IPath path) {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      IPath location = project.getLocation();
      if (path.isPrefixOf(location)) {
        String folderName = path.lastSegment();
        String projectName = project.getName();
        MessageDialog.openError(getShell(),
            ProjectMessages.OpenExistingFolderWizardAction_nesting_title,
            NLS.bind(ProjectMessages.OpenExistingFolderWizardAction_nesting_msg, folderName,
                projectName));
        return true;
      }
    }
    return false;
  }
}
