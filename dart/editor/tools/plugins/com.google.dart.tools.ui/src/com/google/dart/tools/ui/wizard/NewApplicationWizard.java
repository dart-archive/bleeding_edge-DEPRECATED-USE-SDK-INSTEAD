/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.generator.ApplicationGenerator;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.handlers.NewFileCommandState;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.services.ISourceProviderService;

import java.lang.reflect.InvocationTargetException;

/**
 * This wizard is used to create a new library file.
 * 
 * @see NewApplicationWizardPage
 * @see ApplicationGenerator
 */
public class NewApplicationWizard extends AbstractDartWizard implements INewWizard {

  private final ApplicationGenerator appGenerator = new ApplicationGenerator();

  private IWorkbench workbench;

  public NewApplicationWizard() {

    setWindowTitle(WizardMessages.NewApplicationWizard_title);
  }

  /**
   * Add pages to gather information about the library to be created
   */
  @Override
  public void addPages() {
    addPage(new NewApplicationWizardPage(appGenerator));
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
    getShell().getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        performPostOperationUIAction();
      }
    });
    return true;
  }

  /**
   * Perform the operation by creating the project, the library, and the type as specified by the
   * user.
   * 
   * @param monitor the operation progress monitor (not <code>null</code>)
   */
  protected void performOperation(IProgressMonitor monitor) throws CoreException,
      InterruptedException {
    if (monitor.isCanceled()) {
      throw new InterruptedException();
    }
    appGenerator.execute(monitor);
  }

  /**
   * If a class was created, then open an editor on the new class
   */
  protected void performPostOperationUIAction() {
    if (appGenerator.getLibraryFileName().length() > 0) {
      IFile file = appGenerator.getFile();
      if (file.exists()) {
        openEditor(file);
      }

      ISourceProviderService service = (ISourceProviderService) workbench.getActiveWorkbenchWindow().getService(
          ISourceProviderService.class);

      NewFileCommandState newFileCommandStateProvider = (NewFileCommandState) service.getSourceProvider(NewFileCommandState.NEW_FILE_STATE);

      newFileCommandStateProvider.checkState();
    }
  }
}
