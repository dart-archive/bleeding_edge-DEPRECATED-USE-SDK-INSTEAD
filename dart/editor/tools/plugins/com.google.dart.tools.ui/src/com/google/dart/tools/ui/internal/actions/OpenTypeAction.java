/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.actions;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.internal.dialogs.OpenTypeSelectionDialog;
import com.google.dart.tools.ui.internal.text.DartStatusConstants;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * Action to open the "Open Dart Type" dialog.
 */
public class OpenTypeAction extends Action implements IWorkbenchWindowActionDelegate,
    IActionDelegate2 {

  private static final int SEARCH_ELEMENT_KINDS = 0; /*
                                                      * IJavaSearchConstants.TYPE
                                                      */

  public OpenTypeAction() {
    super();
    setText(DartUIMessages.OpenTypeAction_label);
    setDescription(DartUIMessages.OpenTypeAction_description);
    setToolTipText(DartUIMessages.OpenTypeAction_tooltip);
    setImageDescriptor(DartPluginImages.DESC_TOOL_OPENTYPE);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_TYPE_ACTION);
  }

  @Override
  public void dispose() {
    // do nothing.
  }

  @Override
  public void init(IAction action) {
    // do nothing.
  }

  @Override
  public void init(IWorkbenchWindow window) {
    // do nothing.
  }

  @Override
  public void run() {
    runWithEvent(null);
  }

  @Override
  public void run(IAction action) {
    run();
  }

  /**
   * Opens a type selection dialog. If the user selects a type (and does not cancel), an editor is
   * opened on the selected type.
   * 
   * @see org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.Event)
   */
  @Override
  public void runWithEvent(Event e) {
    Shell parent = DartToolsPlugin.getActiveWorkbenchShell();
    if (!doCreateProjectFirstOnEmptyWorkspace(parent)) {
      return;
    }

    SelectionDialog dialog = new OpenTypeSelectionDialog(parent, true,
        PlatformUI.getWorkbench().getProgressService(), null, SEARCH_ELEMENT_KINDS);
    dialog.setTitle(DartUIMessages.OpenTypeAction_dialogTitle);
    dialog.setMessage(DartUIMessages.OpenTypeAction_dialogMessage);

    int result = dialog.open();
    if (result != IDialogConstants.OK_ID) {
      return;
    }

    Object[] types = dialog.getResult();
    if (types == null || types.length == 0) {
      return;
    }

    if (types.length == 1) {
      try {
        DartUI.openInEditor((DartElement) types[0], true, true);
      } catch (CoreException x) {
        ExceptionHandler.handle(x, DartUIMessages.OpenTypeAction_errorTitle,
            DartUIMessages.OpenTypeAction_errorMessage);
      }
      return;
    }

    final IWorkbenchPage workbenchPage = DartToolsPlugin.getActivePage();
    if (workbenchPage == null) {
      IStatus status = new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(),
          DartUIMessages.OpenTypeAction_no_active_WorkbenchPage);
      ExceptionHandler.handle(status, DartUIMessages.OpenTypeAction_errorTitle,
          DartUIMessages.OpenTypeAction_errorMessage);
      return;
    }

    MultiStatus multiStatus = new MultiStatus(DartToolsPlugin.getPluginId(),
        DartStatusConstants.INTERNAL_ERROR, DartUIMessages.OpenTypeAction_multiStatusMessage, null);

    for (int i = 0; i < types.length; i++) {
      Type type = (Type) types[i];
      try {
        DartUI.openInEditor(type, true, true);
      } catch (CoreException x) {
        multiStatus.merge(x.getStatus());
      }
    }

    if (!multiStatus.isOK()) {
      ExceptionHandler.handle(multiStatus, DartUIMessages.OpenTypeAction_errorTitle,
          DartUIMessages.OpenTypeAction_errorMessage);
    }
  }

  @Override
  public void runWithEvent(IAction action, Event event) {
    runWithEvent(event);
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    // do nothing. Action doesn't depend on selection.
  }

  /**
   * Opens the new project dialog if the workspace is empty.
   * 
   * @param parent the parent shell
   * @return returns <code>true</code> when a project has been created, or <code>false</code> when
   *         the new project has been canceled.
   */
  protected boolean doCreateProjectFirstOnEmptyWorkspace(Shell parent) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    if (workspaceRoot.getProjects().length == 0) {
      String title = DartUIMessages.OpenTypeAction_dialogTitle;
      String message = DartUIMessages.OpenTypeAction_createProjectFirst;
      if (MessageDialog.openQuestion(parent, title, message)) {
        new NewProjectAction().run();
        return workspaceRoot.getProjects().length != 0;
      }
      return false;
    }
    return true;
  }
}
