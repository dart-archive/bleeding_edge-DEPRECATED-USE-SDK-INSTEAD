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

import com.google.dart.tools.ui.internal.callhierarchy.CallLocation;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchCommandConstants;

import java.util.Iterator;

/**
 * Copies the selection from the location viewer.
 */
class LocationCopyAction extends Action {
  private final Clipboard clipboard;
  private final IViewSite viewSite;
  private final LocationViewer locationViewer;

  LocationCopyAction(IViewSite viewSite, Clipboard clipboard, LocationViewer locationViewer) {
    this.clipboard = clipboard;
    this.viewSite = viewSite;
    this.locationViewer = locationViewer;

    setText(CallHierarchyMessages.LocationCopyAction_copy);
    setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
    setEnabled(!locationViewer.getSelection().isEmpty());

    locationViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        setEnabled(!event.getSelection().isEmpty());
      }
    });
  }

  @Override
  public void run() {
    IStructuredSelection selection = (IStructuredSelection) locationViewer.getSelection();
    StringBuffer buf = new StringBuffer();
    for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
      CallLocation location = (CallLocation) iterator.next();
      buf.append(location.getLineNumber()).append('\t').append(location.getCallText());
      buf.append('\n');
    }
    TextTransfer plainTextTransfer = TextTransfer.getInstance();
    try {
      clipboard.setContents(
          new String[] {CopyCallHierarchyAction.convertLineTerminators(buf.toString())},
          new Transfer[] {plainTextTransfer});
    } catch (SWTError e) {
      if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
        throw e;
      }
      if (MessageDialog.openQuestion(
          viewSite.getShell(),
          CallHierarchyMessages.CopyCallHierarchyAction_problem,
          CallHierarchyMessages.CopyCallHierarchyAction_clipboard_busy)) {
        run();
      }
    }
  }
}
