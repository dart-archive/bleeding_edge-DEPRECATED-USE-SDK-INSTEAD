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

import org.eclipse.core.resources.IProject;
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
    BasicNewResourceWizard.selectAndReveal(newProject, workbench.getActiveWorkbenchWindow());

    return true;
  }

}
