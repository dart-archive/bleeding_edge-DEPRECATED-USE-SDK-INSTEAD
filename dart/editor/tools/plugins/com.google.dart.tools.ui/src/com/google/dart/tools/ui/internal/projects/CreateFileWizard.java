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

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.DialogUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * Standard workbench wizard that creates a new file resource in the workspace.
 * <p>
 * NOTE: this is essentially a riff on {@link BasicNewFileResourceWizard}, modified:
 * <ol>
 * <li>in {@link #performFinish()} to update the Dart model and write to the .children file and</li>
 * <li>with a custom file creation page that suppresses the "advanced" linking options</li>
 * </ol>
 */
@SuppressWarnings("restriction")
public class CreateFileWizard extends BasicNewResourceWizard {

  /**
   * The wizard id for creating new files in the workspace.
   */
  public static final String WIZARD_ID = "com.google.dart.tools.ui.new.file"; //$NON-NLS-1$

  private WizardNewFileCreationPage mainPage;

  @Override
  public void addPages() {
    super.addPages();

    mainPage = new CreateFileWizardPage("newFilePage1", getSelection());
    mainPage.setTitle(ResourceMessages.FileResource_pageTitle);
    mainPage.setDescription(ResourceMessages.FileResource_description);
    addPage(mainPage);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
    super.init(workbench, currentSelection);
    setWindowTitle(ResourceMessages.FileResource_shellTitle);
    setNeedsProgressMonitor(true);
  }

  @Override
  public boolean performFinish() {
    IFile file = mainPage.createNewFile();
    if (file == null) {
      return false;
    }

    selectAndReveal(file);
    openEditor(file);

    return true;
  }

  @Override
  protected void initializeDefaultPageImageDescriptor() {
    ImageDescriptor desc = IDEWorkbenchPlugin.getIDEImageDescriptor("wizban/newfile_wiz.png");//$NON-NLS-1$
    setDefaultPageImageDescriptor(desc);
  }

  protected void openEditor(IFile file) {
    IWorkbenchWindow dw = getWorkbench().getActiveWorkbenchWindow();
    try {
      if (dw != null) {
        IWorkbenchPage page = dw.getActivePage();
        if (page != null) {
          IDE.openEditor(page, file, true);
        }
      }
    } catch (PartInitException e) {
      DialogUtil.openError(
          dw.getShell(),
          ResourceMessages.FileResource_errorMessage,
          e.getMessage(),
          e);
    }
  }
}
