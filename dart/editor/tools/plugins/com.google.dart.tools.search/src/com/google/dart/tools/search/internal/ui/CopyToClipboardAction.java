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
package com.google.dart.tools.search.internal.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import java.util.Collections;
import java.util.Iterator;

public class CopyToClipboardAction extends Action {

  private StructuredViewer fViewer;

  public CopyToClipboardAction() {
    setText(SearchMessages.CopyToClipboardAction_label);
    setToolTipText(SearchMessages.CopyToClipboardAction_tooltip);
    ISharedImages workbenchImages = PlatformUI.getWorkbench().getSharedImages();
    setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
    setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
    setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));

  }

  public CopyToClipboardAction(StructuredViewer viewer) {
    this();
    Assert.isNotNull(viewer);
    fViewer = viewer;
  }

  /*
   * Implements method from IAction
   */
  @Override
  public void run() {
    Shell shell = SearchPlugin.getActiveWorkbenchShell();
    if (shell == null || fViewer == null) {
      return;
    }

    ILabelProvider labelProvider = (ILabelProvider) fViewer.getLabelProvider();
    String lineDelim = System.getProperty("line.separator"); //$NON-NLS-1$
    StringBuffer buf = new StringBuffer();
    @SuppressWarnings("rawtypes")
    Iterator iter = getSelection();
    while (iter.hasNext()) {
      if (buf.length() > 0) {
        buf.append(lineDelim);
      }
      buf.append(labelProvider.getText(iter.next()));
    }

    if (buf.length() > 0) {
      copyToClipboard(buf.toString(), shell);
    }
  }

  @Override
  public void runWithEvent(Event event) {
    // bugzilla 126062: allow combos and text fields of the view to fill
    // the clipboard
    Shell shell = SearchPlugin.getActiveWorkbenchShell();
    if (shell != null) {
      String sel = null;
      if (event.widget instanceof Combo) {
        Combo combo = (Combo) event.widget;
        sel = combo.getText();
        Point selection = combo.getSelection();
        sel = sel.substring(selection.x, selection.y);
      } else if (event.widget instanceof Text) {
        Text text = (Text) event.widget;
        sel = text.getSelectionText();
      }
      if (sel != null) {
        if (sel.length() > 0) {
          copyToClipboard(sel, shell);
        }
        return;
      }
    }

    run();
  }

  /**
   * @param viewer The viewer to set.
   */
  public void setViewer(StructuredViewer viewer) {
    fViewer = viewer;
  }

  private void copyToClipboard(Clipboard clipboard, String str, Shell shell) {
    try {
      clipboard.setContents(new String[] {str}, new Transfer[] {TextTransfer.getInstance()});
    } catch (SWTError ex) {
      if (ex.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
        throw ex;
      }
      String title = SearchMessages.CopyToClipboardAction_error_title;
      String message = SearchMessages.CopyToClipboardAction_error_message;
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

  @SuppressWarnings("rawtypes")
  private Iterator getSelection() {
    ISelection s = fViewer.getSelection();
    if (s instanceof IStructuredSelection) {
      return ((IStructuredSelection) s).iterator();
    }
    return Collections.EMPTY_LIST.iterator();
  }
}
