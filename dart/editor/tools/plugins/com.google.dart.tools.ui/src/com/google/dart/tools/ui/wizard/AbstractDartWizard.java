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

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Abstract wizard for sharing behavior
 */
public abstract class AbstractDartWizard extends Wizard implements IWorkbenchWizard {

  // TODO (danrubel) move to central location
  private static final String DART_PERSPECTIVE_ID = "com.google.dart.tools.ui.DartPerspective";
  private static final String OPEN_PERSPECTIVE = "openPerspective";

  protected IWorkbench workbench;

  /**
   * Initialize the wizard based upon the current workbench state
   */
  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.workbench = workbench;
  }

  /**
   * Prompt the user to open the Dart perspective, if not already open
   */
  protected void openDartPerspective() {

    // Check to see if the Dart perspective is already open
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    IPerspectiveDescriptor perspective = window.getActivePage().getPerspective();
    if (perspective != null && DART_PERSPECTIVE_ID.equals(perspective.getId())) {
      return;
    }

    // Prompt the user to open the Dart perspective
    Shell shell = window.getShell();
    IPreferenceStore prefs = DartToolsPlugin.getDefault().getPreferenceStore();
    String open = prefs.getString(OPEN_PERSPECTIVE);
    if (MessageDialogWithToggle.NEVER.equals(open)) {
      return;
    }
    if (!MessageDialogWithToggle.ALWAYS.equals(open)) {
      MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(shell,
          "Open Perspective", "Would you like to open the Dart perspective?",
          "Don't show this again", false, prefs, OPEN_PERSPECTIVE);
      if (dialog.getReturnCode() != IDialogConstants.YES_ID) {
        return;
      }
    }

    // Open the perspective
    try {
      workbench.showPerspective(DART_PERSPECTIVE_ID, window);
    } catch (WorkbenchException e) {
      String message = "Failed to open Dart perspective";
      DartToolsPlugin.log(message, e);
      MessageDialog.openError(getShell(), "Open Perspective Exception", message);
    }
  }

  /**
   * Open the editor on the specified input.
   * 
   * @param editorId the identifier for the editor to be opened
   * @param input the input to be edited
   * @return <code>true</code> if the editor was successfully opened
   */
  protected boolean openEditor(String editorId, IEditorInput input) {
    try {
      IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
      activePage.openEditor(input, editorId);
      return true;
    } catch (Throwable e) {
      String message = "Failed to open editor " + editorId + " on " + input.getToolTipText();
      DartToolsPlugin.log(message, e);
      MessageDialog.openError(getShell(), "Open Editor Exception", message);
      return false;
    }
  }

  /**
   * Open the editor on the specified file
   * 
   * @param editorId the identifier for the editor to be opened
   * @param file the file to be edited
   * @return <code>true</code> if the editor was successfully opened
   */
  protected boolean openEditor(String editorId, IFile file) {
    try {
      IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
      activePage.openEditor(new FileEditorInput(file), editorId);
      return true;
    } catch (Throwable e) {
      String message = "Failed to open editor " + editorId + " on " + file;
      DartToolsPlugin.log(message, e);
      MessageDialog.openError(getShell(), "Open Editor Exception", message);
      return false;
    }
  }
}
