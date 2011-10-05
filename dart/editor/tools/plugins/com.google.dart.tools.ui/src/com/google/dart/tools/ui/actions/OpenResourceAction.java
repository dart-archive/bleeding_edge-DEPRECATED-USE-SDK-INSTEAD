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

import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.internal.ide.dialogs.OpenResourceDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the open resource action. Opens a dialog prompting for a file and opens the selected
 * file in an editor. This code was derived from
 * org.eclipse.ui.internal.ide.handler.OpenResourceHandler; it was modified to use our editor
 * selection logic instead of Eclipse's. We can get rid of this once we've modified Eclipse's
 * default editor to suit our needs.
 */
@SuppressWarnings("restriction")
public class OpenResourceAction extends Action implements IWorkbenchAction {

  public static final String ID = DartToolsPlugin.PLUGIN_ID + ".openResourceAction"; //$NON-NLS-1$
  public static final String ID_ORG_ECLIPSE_UI_OPEN_RESOURCE_ACTION = "org.eclipse.ui.navigate.openResource"; //$NON-NLS-1$

  private final IWorkbenchWindow window;

  /**
   * Creates a new instance of the class.
   */
  public OpenResourceAction(IWorkbenchWindow window) {
    this.window = window;
    setId(ID);
    setText(ActionMessages.OpenResourceAction_label);
    setImageDescriptor(null);
    setActionDefinitionId(ID_ORG_ECLIPSE_UI_OPEN_RESOURCE_ACTION);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IIDEHelpContextIds.OPEN_WORKSPACE_FILE_ACTION);
  }

  @Override
  public void dispose() {
    // Does nothing
  }

  @Override
  public void run() {
    final List<IFile> files = new ArrayList<IFile>();

    // Prompt the user for the resource to open.
    Object[] result = queryFileResource();

    if (result != null) {
      for (int i = 0; i < result.length; i++) {
        if (result[i] instanceof IFile) {
          files.add((IFile) result[i]);
        }
      }
    }

    if (files.size() > 0) {
      final IWorkbenchPage page = window.getActivePage();
      if (page == null) {
        MessageDialog.openError(window.getShell(), ActionMessages.OpenResourceAction_error_title,
            ActionMessages.OpenResourceAction_error_message);
        return;
      }

      try {
        for (IFile f : files) {
          EditorUtility.openInEditor(f, true);
        }
      } catch (final PartInitException e) {
        DartToolsPlugin.log(e);
        MessageDialog.openError(window.getShell(), ActionMessages.OpenResourceAction_error_title,
            ActionMessages.OpenResourceAction_error_message);
        return;
      } catch (final DartModelException e) {
        DartToolsPlugin.log(e);
        MessageDialog.openError(window.getShell(), ActionMessages.OpenResourceAction_error_title,
            ActionMessages.OpenResourceAction_error_message);
        return;
      }
    }
  }

  /**
   * Query the user for the resources that should be opened
   *
   * @return the resource that should be opened.
   */
  private final Object[] queryFileResource() {
    final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      return null;
    }
    final Shell parent = window.getShell();
    final IContainer input = ResourcesPlugin.getWorkspace().getRoot();

    final OpenResourceDialog dialog = new OpenResourceDialog(parent, input, IResource.FILE);
    final int resultCode = dialog.open();
    if (resultCode != Window.OK) {
      return null;
    }

    final Object[] result = dialog.getResult();

    return result;
  }

}
