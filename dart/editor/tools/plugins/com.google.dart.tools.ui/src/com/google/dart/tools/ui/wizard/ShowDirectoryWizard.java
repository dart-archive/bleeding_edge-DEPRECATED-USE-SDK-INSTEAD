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
package com.google.dart.tools.ui.wizard;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.directoryset.DirectorySetManager;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.view.files.FilesView;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import java.lang.reflect.InvocationTargetException;

/**
 * This wizard is used to add a new top-level directory into the {@link FilesView}.
 * 
 * @see ShowDirectoryWizardPage
 */
public class ShowDirectoryWizard extends AbstractDartWizard implements INewWizard {

  private ShowDirectoryWizardPage page;

  public ShowDirectoryWizard() {
    setWindowTitle(WizardMessages.ShowDirectoryWizard_title);
    page = new ShowDirectoryWizardPage();
  }

  /**
   * Add the single page, {@link ShowDirectoryWizardPage}.
   */
  @Override
  public void addPages() {
    addPage(page);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    super.init(workbench, selection);
    this.workbench = workbench;
  }

  @Override
  public boolean performFinish() {
    try {
      getContainer().run(true, true, new WorkspaceModifyOperation() {
        @Override
        protected void execute(IProgressMonitor monitor) throws CoreException,
            InvocationTargetException, InterruptedException {
          performOperation(monitor);
        }
      });
    } catch (InvocationTargetException e) {
      DartToolsPlugin.log(e);
    } catch (InterruptedException e) {
      // Ignored because operation canceled by user
      return false;
    }
//    getShell().getDisplay().asyncExec(new Runnable() {
//      @Override
//      public void run() {
//        performPostOperationUIAction();
//      }
//    });
    return true;
  }

  /**
   * Perform the operation by ....
   * 
   * @param monitor the operation progress monitor (not <code>null</code>)
   */
  protected void performOperation(IProgressMonitor monitor) throws CoreException,
      InterruptedException {
    if (monitor.isCanceled()) {
      throw new InterruptedException();
    }
    DirectorySetManager directorySetManager = DartCore.getDirectorySetManager();
    directorySetManager.addPath(page.getDirectory());
  }

//  protected void performPostOperationUIAction() {
//  }
}
