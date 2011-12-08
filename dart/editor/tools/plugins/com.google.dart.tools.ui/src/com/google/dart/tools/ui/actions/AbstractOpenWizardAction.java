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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Abstract base classed used for the open wizard actions.
 */
public abstract class AbstractOpenWizardAction extends AbstractInstrumentedAction {

  private Shell shell;
  private IStructuredSelection selection;

  /**
   * Creates the action.
   */
  protected AbstractOpenWizardAction() {
    shell = null;
    selection = null;
  }

  @Override
  public void run() {
    EmitInstrumentationCommand();
    Shell shell = getShell();
    try {
      INewWizard wizard = createWizard();
      wizard.init(PlatformUI.getWorkbench(), getSelection());

      WizardDialog dialog = new WizardDialog(shell, wizard);
      PixelConverter converter = new PixelConverter(JFaceResources.getDialogFont());
      dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70),
          converter.convertHeightInCharsToPixels(20));
      dialog.create();
      int res = dialog.open();
      notifyResult(res == Window.OK);
    } catch (CoreException e) {
      String title = ActionMessages.AbstractOpenWizardAction_createerror_title;
      String message = ActionMessages.AbstractOpenWizardAction_createerror_message;
      ExceptionHandler.handle(e, shell, title, message);
    }
  }

  /**
   * Configures the selection to be used as initial selection of the wizard.
   * 
   * @param selection the selection to be set or <code>null</code> to use the selection of the
   *          active workbench window
   */
  public void setSelection(IStructuredSelection selection) {
    this.selection = selection;
  }

  /**
   * Configures the shell to be used as parent shell by the wizard.
   * 
   * @param shell the shell to be set or <code>null</code> to use the shell of the active workbench
   *          window
   */
  public void setShell(Shell shell) {
    this.shell = shell;
  }

  /**
   * Creates and configures the wizard. This method should only be called once.
   * 
   * @return returns the created wizard.
   * @throws CoreException exception is thrown when the creation was not successful.
   */
  abstract protected INewWizard createWizard() throws CoreException;

  /**
   * Returns the configured selection. If no selection has been configured using
   * {@link #setSelection(IStructuredSelection)}, the currently selected element of the active
   * workbench is returned.
   * 
   * @return the configured selection
   */
  protected IStructuredSelection getSelection() {
    if (selection == null) {
      return evaluateCurrentSelection();
    }
    return selection;
  }

  /**
   * Returns the configured shell. If no shell has been configured using {@link #setShell(Shell)},
   * the shell of the currently active workbench is returned.
   * 
   * @return the configured shell
   */
  protected Shell getShell() {
    if (shell == null) {
      return DartToolsPlugin.getActiveWorkbenchShell();
    }
    return shell;
  }

  private IStructuredSelection evaluateCurrentSelection() {
    IWorkbenchWindow window = DartToolsPlugin.getActiveWorkbenchWindow();
    if (window != null) {
      ISelection selection = window.getSelectionService().getSelection();
      if (selection instanceof IStructuredSelection) {
        return (IStructuredSelection) selection;
      }
    }
    return StructuredSelection.EMPTY;
  }

}
