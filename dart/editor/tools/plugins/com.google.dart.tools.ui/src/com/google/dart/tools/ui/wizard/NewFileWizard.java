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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.generator.FileGenerator;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.text.editor.ExternalCompilationUnitEditorInput;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import java.lang.reflect.InvocationTargetException;

/**
 * This wizard is used to create a new file.
 * 
 * @see NewFileWizardPage
 * @see FileGenerator
 */
public class NewFileWizard extends AbstractDartWizard implements INewWizard {

  private final FileGenerator fileGenerator = new FileGenerator();

  public NewFileWizard() {
    setWindowTitle(WizardMessages.NewFileWizard_newFile);
  }

  /**
   * Add pages to gather information about the project to be created
   */
  @Override
  public void addPages() {
    addPage(new NewFileWizardPage(fileGenerator));
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
    fileGenerator.execute(monitor);
  }

  /**
   * If the file was created, then open an editor on the new file.
   */
  protected void performPostOperationUIAction() {
    IFile file = fileGenerator.getFile();
    if (file != null) {
      String fileName = file.getName();
      if (DartCore.isDartLikeFileName(fileName)) {
        DartElement element = DartCore.create(file);
        if (element == null) {
          try {
            DartLibraryImpl library = (DartLibraryImpl) fileGenerator.getLibrary();
            IPath libraryPath = library.getDefiningCompilationUnit().getCorrespondingResource().getLocation();
            String relativePath = file.getLocation().makeRelativeTo(
                libraryPath.removeLastSegments(1)).toString();
            IEditorInput input = new ExternalCompilationUnitEditorInput(
                EFS.getStore(file.getLocationURI()), new ExternalCompilationUnitImpl(library,
                    relativePath));
            if (openEditor(DartUI.ID_CU_EDITOR, input)
                || openEditor(DartUI.ID_DEFAULT_TEXT_EDITOR, input)) {
              return;
            }
          } catch (Exception exception) {
            // If we couldn't open as an external unit, fall through to try the default approach.
          }
        }
        if (!openEditor(DartUI.ID_CU_EDITOR, file)) {
          openEditor(DartUI.ID_DEFAULT_TEXT_EDITOR, file);
        }
      } else {
        openEditor(DartUI.ID_DEFAULT_TEXT_EDITOR, file);
      }
    }
  }

}
