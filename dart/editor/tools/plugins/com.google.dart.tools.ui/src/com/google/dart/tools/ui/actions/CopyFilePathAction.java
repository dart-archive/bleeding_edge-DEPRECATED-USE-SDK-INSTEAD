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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.filesview.FilesView;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;

/**
 * An action for the Files view which allows users to copy the path of a {@link java.io.File} to
 * their clip board.
 * 
 * @see FilesView
 */
public class CopyFilePathAction extends SelectionDispatchAction {

  private static final String ACTION_ID = "com.google.dart.tools.ui.copyFilePath";

  /**
   * Creates an instance of the {@link CopyFilePathAction}.
   */
  public CopyFilePathAction(IWorkbenchSite site) {
    super(site);
    setText(ActionMessages.CopyFilePathAction_text);
    setDescription(ActionMessages.CopyFilePathAction_description);
    setToolTipText(ActionMessages.CopyFilePathAction_tooltip);
    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/elcl16/cpyqual_menu.gif"));
    setId(ACTION_ID);
    setEnabled(false);
  }

  @Override
  public void run(IStructuredSelection selection) {
    if (isEnabled()) {
      IResource selectedResource = (IResource) (selection.toArray()[0]);
      String path = selectedResource.getLocation().toOSString();
      copyToClipboard(path, getSite().getShell());
    }
  }

  /**
   * On each selection change event, call {@link #setEnabled(boolean)} iff a single element is
   * selected that is a {@link java.io.File}.
   */
  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    // if the selection is a structured selection (aka, from the Files view)
    if (event.getSelection() instanceof IStructuredSelection) {
      IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      // if there is one element selected
      if (selection.size() == 1) {
        Object firstElt = selection.getFirstElement();
        // if that element is a java.io.File element
        if (firstElt instanceof IResource) {
          setEnabled(true);
          return;
        }
      }
    }
    setEnabled(false);
  }

  private void copyToClipboard(Clipboard clipboard, String str, Shell shell) {
    try {
      clipboard.setContents(new String[] {str}, new Transfer[] {TextTransfer.getInstance()});
    } catch (SWTError ex) {
      if (ex.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
        throw ex;
      }
      String title = ActionMessages.CopyFilePathAction_dialogTitle;
      String message = ActionMessages.CopyFilePathAction_dialogMessage;
      if (MessageDialog.openQuestion(shell, title, message)) {
        copyToClipboard(clipboard, str, shell);
      }
    }
  }

  private void copyToClipboard(String text, Shell shell) {
    text = TextProcessor.deprocess(text);
    Clipboard clipboard = new Clipboard(shell.getDisplay());
    try {
      copyToClipboard(clipboard, text, shell);
    } finally {
      clipboard.dispose();
    }
  }

}
