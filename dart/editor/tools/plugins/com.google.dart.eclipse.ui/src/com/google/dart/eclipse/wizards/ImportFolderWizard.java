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

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * Import Dart Wizard. Imports existing Dart source as a project.
 */
public class ImportFolderWizard extends Wizard implements IImportWizard, INewWizard {

  /**
   * A visitor that checks for the build directory and marks it as derived.
   */
  class BuildDirectoryFinder implements IResourceProxyVisitor {

    @Override
    public boolean visit(IResourceProxy proxy) throws CoreException {
      if (proxy.getType() == IResource.FOLDER) {
        if (proxy.getName().equals(DartCore.BUILD_DIRECTORY_NAME)) {
          IFolder folder = (IFolder) proxy.requestResource();
          if (DartCore.isBuildDirectory(folder)) {
            folder.setDerived(true, null);
          }
          return false;
        }
        return true;
      }
      return true;
    }
  }

  private IWorkbench workbench;
  private ImportFolderWizardPage page;
  private IProject newProject;

  public ImportFolderWizard() {
    setWindowTitle("Import");
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    page = new ImportFolderWizardPage();
    addPage(page);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.workbench = workbench;
  }

  @Override
  public boolean performFinish() {

    newProject = ProjectUtils.createNewProject(page.getProjectName(), page.getProjectLocation(),
        getContainer(), getShell());

    if (newProject == null) {
      return false;
    }

    ProjectUtils.updatePerspective();

    try {
      newProject.accept(new BuildDirectoryFinder(), IResource.DEPTH_INFINITE);
    } catch (CoreException e) {

    }

    BasicNewResourceWizard.selectAndReveal(newProject, workbench.getActiveWorkbenchWindow());

    return true;
  }
}
