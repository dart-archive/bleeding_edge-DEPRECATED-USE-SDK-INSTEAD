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
package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartProject;

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
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * This class was originally copied from
 * <code>org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard</code>, but modified in
 * {@link #performFinish()} to update the Dart model and write to the .children file.
 * <p>
 * Standard workbench wizard that create a new file resource in the workspace.
 * <p>
 * This class may be instantiated and used without further configuration; this class is not intended
 * to be subclassed.
 * </p>
 * <p>
 * Example:
 * 
 * <pre>
 * IWorkbenchWizard wizard = new BasicNewFileResourceWizard();
 * wizard.init(workbench, selection);
 * WizardDialog dialog = new WizardDialog(shell, wizard);
 * dialog.open();
 * </pre>
 * During the call to <code>open</code>, the wizard dialog is presented to the user. When the user
 * hits Finish, a file resource at the user-specified workspace path is created, the dialog closes,
 * and the call to <code>open</code> returns.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings("restriction")
public class BasicNewFileResourceWizard extends BasicNewResourceWizard {

  /**
   * The wizard id for creating new files in the workspace.
   * 
   * @since 3.4
   */
  public static final String WIZARD_ID = "org.eclipse.ui.wizards.new.file"; //$NON-NLS-1$

  private WizardNewFileCreationPage mainPage;

  /**
   * Creates a wizard for creating a new file resource in the workspace.
   */
  public BasicNewFileResourceWizard() {
    super();
  }

  /*
   * (non-Javadoc) Method declared on IWizard.
   */
  @Override
  public void addPages() {
    super.addPages();
    mainPage = new WizardNewFileCreationPage("newFilePage1", getSelection());//$NON-NLS-1$
    mainPage.setTitle(ResourceMessages.FileResource_pageTitle);
    mainPage.setDescription(ResourceMessages.FileResource_description);
    addPage(mainPage);
  }

  /*
   * (non-Javadoc) Method declared on IWorkbenchWizard.
   */
  @Override
  public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
    super.init(workbench, currentSelection);
    setWindowTitle(ResourceMessages.FileResource_shellTitle);
    setNeedsProgressMonitor(true);
  }

  /*
   * (non-Javadoc) Method declared on IWizard.
   */
  @Override
  public boolean performFinish() {
    IFile file = mainPage.createNewFile();
    if (file == null) {
      return false;
    }

    // If this new Dart file, add it to the model (and .children file, cached model)
    if (DartCore.isDartLikeFileName(file.getName())) {
      DartProject dartProject = DartCore.create(file.getProject());
      dartProject.addLibraryFile(file);
    }

    selectAndReveal(file);

    // Open editor on new file.
    IWorkbenchWindow dw = getWorkbench().getActiveWorkbenchWindow();
    try {
      if (dw != null) {
        IWorkbenchPage page = dw.getActivePage();
        if (page != null) {
          IDE.openEditor(page, file, true);
        }
      }
    } catch (PartInitException e) {
      DialogUtil.openError(dw.getShell(), ResourceMessages.FileResource_errorMessage,
          e.getMessage(), e);
    }

    return true;
  }

  @Override
  protected void initializeDefaultPageImageDescriptor() {
    ImageDescriptor desc = IDEWorkbenchPlugin.getIDEImageDescriptor("wizban/newfile_wiz.png");//$NON-NLS-1$
    setDefaultPageImageDescriptor(desc);
  }
}
