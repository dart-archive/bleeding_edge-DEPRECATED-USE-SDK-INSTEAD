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
import com.google.dart.tools.core.generator.AbstractSample;
import com.google.dart.tools.core.generator.DartIdentifierUtil;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * Standard workbench wizard that creates a new Dart project resource in the workspace.
 */
public class DartProjectWizard extends Wizard implements INewWizard {
  private IWorkbench workbench;
  private ISelection selection;
  private IProject newProject;

  private DartProjectWizardPage page;

  private IFile createdSampleFile;

  public DartProjectWizard() {
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

  protected boolean createAndRevealNewProject() {

    String name = page.getProjectName();

    newProject = ProjectUtils.createNewProject(name, page.getProjectLocation(), getContainer(),
        getShell());

    if (newProject == null) {
      return false;
    }

    try {
      AbstractSample sampleContent = page.getSampleContent();

      if (sampleContent != null) {
        createdSampleFile = sampleContent.generateInto(newProject,
            DartIdentifierUtil.createValidIdentifier(name));
      }
    } catch (CoreException e) {
      DartEclipseUI.logError(e);
    }

    ProjectUtils.updatePerspective();

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
    BasicNewResourceWizard.selectAndReveal(newResource, workbench.getActiveWorkbenchWindow());
  }

}
