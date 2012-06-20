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

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.internal.views.navigator.ResourceNavigatorMessages;
import org.eclipse.ui.part.ResourceTransfer;

import java.util.Iterator;
import java.util.List;

/**
 * Standard action for copying the currently selected resources to the clipboard.
 * <p>
 * Based on the package private <code>org.eclipse.ui.views.navigator.CopyAction</code>.
 * </p>
 */
@SuppressWarnings({"restriction", "rawtypes"})
public class CopyAction extends SelectionListenerAction {

  /**
   * The id of this action.
   */
  public static final String ID = DartToolsPlugin.PLUGIN_ID + ".CopyAction"; //$NON-NLS-1$

  /**
   * The shell in which to show any dialogs.
   */
  private Shell shell;

  /**
   * System clipboard
   */
  private Clipboard clipboard;

  /**
   * Associated paste action. May be <code>null</code>
   */
  private PasteAction pasteAction;

  /**
   * Creates a new action.
   * 
   * @param shell the shell for any dialogs
   * @param clipboard a platform clipboard
   */
  public CopyAction(Shell shell, Clipboard clipboard) {
    super(ResourceNavigatorMessages.CopyAction_title);
    Assert.isNotNull(shell);
    Assert.isNotNull(clipboard);
    this.shell = shell;
    this.clipboard = clipboard;
    setToolTipText(ResourceNavigatorMessages.CopyAction_toolTip);
    setId(CopyAction.ID);
    setAccelerator(SWT.MOD1 | 'c');
    setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
        ISharedImages.IMG_TOOL_COPY));
    setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
        ISharedImages.IMG_TOOL_COPY_DISABLED));

  }

  /**
   * Creates a new action.
   * 
   * @param shell the shell for any dialogs
   * @param clipboard a platform clipboard
   * @param pasteAction a paste action
   */
  public CopyAction(Shell shell, Clipboard clipboard, PasteAction pasteAction) {
    this(shell, clipboard);
    this.pasteAction = pasteAction;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void run() {

    List selectedResources = getSelectedResources();
    IResource[] resources = (IResource[]) selectedResources.toArray(new IResource[selectedResources.size()]);

    // Get the file names and a string representation
    final int length = resources.length;
    int actualLength = 0;
    String[] fileNames = new String[length];
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < length; i++) {
      IPath location = resources[i].getLocation();
      // location may be null. See bug 29491.
      if (location != null) {
        fileNames[actualLength++] = location.toOSString();
      }
      if (i > 0) {
        buf.append("\n"); //$NON-NLS-1$
      }
      buf.append(resources[i].getName());
    }
    // was one or more of the locations null?
    if (actualLength < length) {
      String[] tempFileNames = fileNames;
      fileNames = new String[actualLength];
      for (int i = 0; i < actualLength; i++) {
        fileNames[i] = tempFileNames[i];
      }
    }
    setClipboard(resources, fileNames, buf.toString());

    // update the enablement of the paste action
    // workaround since the clipboard does not suppot callbacks
    if (pasteAction != null && pasteAction.getStructuredSelection() != null) {
      pasteAction.selectionChanged(pasteAction.getStructuredSelection());
    }
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {

    /**
     * The <code>CopyAction</code> implementation of this <code>SelectionListenerAction</code>
     * method enables this action if one or more resources of compatible types are selected.
     */

    if (!super.updateSelection(selection)) {
      return false;
    }

    if (getSelectedNonResources().size() > 0) {
      return false;
    }

    List selectedResources = getSelectedResources();
    if (selectedResources.size() == 0) {
      return false;
    }

    boolean projSelected = selectionIsOfType(IResource.PROJECT);
    boolean fileFoldersSelected = selectionIsOfType(IResource.FILE | IResource.FOLDER);
    if (!projSelected && !fileFoldersSelected) {
      return false;
    }

    // selection must be homogeneous
    if (projSelected && fileFoldersSelected) {
      return false;
    }

    // must have a common parent  
    IContainer firstParent = ((IResource) selectedResources.get(0)).getParent();
    if (firstParent == null) {
      return false;
    }

    Iterator resourcesEnum = selectedResources.iterator();
    while (resourcesEnum.hasNext()) {
      IResource currentResource = (IResource) resourcesEnum.next();
      if (!currentResource.getParent().equals(firstParent)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Set the clipboard contents. Prompt to retry if clipboard is busy.
   * 
   * @param resources the resources to copy to the clipboard
   * @param fileNames file names of the resources to copy to the clipboard
   * @param names string representation of all names
   */
  private void setClipboard(IResource[] resources, String[] fileNames, String names) {
    try {
      // set the clipboard contents
      if (fileNames.length > 0) {
        clipboard.setContents(new Object[] {resources, fileNames, names}, new Transfer[] {
            ResourceTransfer.getInstance(), FileTransfer.getInstance(), TextTransfer.getInstance()});
      } else {
        clipboard.setContents(
            new Object[] {resources, names},
            new Transfer[] {ResourceTransfer.getInstance(), TextTransfer.getInstance()});
      }
    } catch (SWTError e) {
      if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
        throw e;
      }
      if (MessageDialog.openQuestion(
          shell,
          ResourceNavigatorMessages.CopyToClipboardProblemDialog_title,
          ResourceNavigatorMessages.CopyToClipboardProblemDialog_message)) {
        setClipboard(resources, fileNames, names);
      }
    }
  }

}
