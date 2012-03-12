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

import com.google.dart.tools.ui.actions.CreateAndRevealProjectAction;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.navigator.NavigatorDropAdapter;

/**
 * Implements drop behavior for drag and drop operations that land on the files view.
 */
@SuppressWarnings("deprecation")
public class FilesViewDropAdapter extends NavigatorDropAdapter {

  /**
   * Constructs a new drop adapter.
   * 
   * @param viewer the viewer
   */
  public FilesViewDropAdapter(StructuredViewer viewer) {
    super(viewer);
  }

  @Override
  public boolean performDrop(Object data) {
    Object target = getCurrentTarget();
    if (target instanceof IWorkspaceRoot) {
      IStatus status = performProjectDrop(data);
      return status.isOK();
    }

    return super.performDrop(data);
  }

  @Override
  public boolean validateDrop(Object target, int dragOperation, TransferData transferType) {
    if (target instanceof IWorkspaceRoot) {
      //note: does not update lastValidLocation field...
      if (FileTransfer.getInstance().isSupportedType(transferType)
          && (dragOperation != DND.DROP_COPY)) {
        return false;
      }

      return true;
    }

    return super.validateDrop(target, dragOperation, transferType);
  }

  @Override
  protected Object determineTarget(DropTargetEvent event) {
    return event.item == null ? ResourcesPlugin.getWorkspace().getRoot() : event.item.getData();
  }

  private IWorkbenchWindow getWindow() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
  }

  private IStatus performProjectDrop(Object data) {
    if (data instanceof String[]) {
      String[] path = (String[]) data;
      for (String p : path) {
        CreateAndRevealProjectAction action = new CreateAndRevealProjectAction(getWindow(), p);
        action.run();
        if (!action.getStatus().isOK()) {
          return Status.CANCEL_STATUS;
        }
      }

      return Status.OK_STATUS;
    }

    return Status.CANCEL_STATUS;
  }

}
