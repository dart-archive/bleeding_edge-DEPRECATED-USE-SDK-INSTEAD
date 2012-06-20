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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.util.SelectionUtil;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

class CopyCallHierarchyAction extends Action {

  private static final char INDENTATION = '\t';

  static String convertLineTerminators(String in) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    StringReader stringReader = new StringReader(in);
    BufferedReader bufferedReader = new BufferedReader(stringReader);
    try {
      String line = bufferedReader.readLine();
      while (line != null) {
        printWriter.print(line);
        line = bufferedReader.readLine();
        if (line != null && line.length() != 0) {
          printWriter.println();
        }

      }
    } catch (IOException e) {
      return in; // return the call hierarchy unfiltered
    }
    return stringWriter.toString();
  }

  private CallHierarchyViewPart view;
  private CallHierarchyViewer viewer;
  private final Clipboard clipboard;

  public CopyCallHierarchyAction(CallHierarchyViewPart view, Clipboard clipboard,
      CallHierarchyViewer viewer) {
    super(CallHierarchyMessages.CopyCallHierarchyAction_label);
    Assert.isNotNull(clipboard);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CALL_HIERARCHY_COPY_ACTION);
    this.view = view;
    this.clipboard = clipboard;
    this.viewer = viewer;
  }

  public boolean canActionBeAdded() {
    Object element = SelectionUtil.getSingleElement(getSelection());
    return element != null;
  }

  @Override
  public void run() {
    StringBuffer buf = new StringBuffer();
    addCalls(viewer.getTree().getSelection()[0], 0, buf);

    TextTransfer plainTextTransfer = TextTransfer.getInstance();
    try {
      clipboard.setContents(
          new String[] {convertLineTerminators(buf.toString())},
          new Transfer[] {plainTextTransfer});
    } catch (SWTError e) {
      if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
        throw e;
      }
      if (MessageDialog.openQuestion(
          view.getViewSite().getShell(),
          CallHierarchyMessages.CopyCallHierarchyAction_problem,
          CallHierarchyMessages.CopyCallHierarchyAction_clipboard_busy)) {
        run();
      }
    }
  }

  /**
   * Adds the specified {@link TreeItem}'s text to the StringBuffer.
   * 
   * @param item the tree item
   * @param indent the indent size
   * @param buf the string buffer
   */
  private void addCalls(TreeItem item, int indent, StringBuffer buf) {
    for (int i = 0; i < indent; i++) {
      buf.append(INDENTATION);
    }

    buf.append(TextProcessor.deprocess(item.getText()));
    buf.append('\n');

    if (item.getExpanded()) {
      TreeItem[] items = item.getItems();
      for (int i = 0; i < items.length; i++) {
        addCalls(items[i], indent + 1, buf);
      }
    }
  }

  private ISelection getSelection() {
    ISelectionProvider provider = view.getSite().getSelectionProvider();
    if (provider != null) {
      return provider.getSelection();
    }
    return null;
  }
}
