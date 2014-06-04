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
import com.google.dart.tools.ui.actions.CreateAndRevealProjectAction;
import com.google.dart.tools.ui.internal.refactoring.MoveSupport;
import com.google.dart.tools.ui.internal.refactoring.RefactoringUtils;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
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
    // project drop
    if (target instanceof IWorkspaceRoot) {
      IStatus status = performProjectDrop(data);
      return status.isOK();
    }
    // DROP_COPY
    if (getCurrentOperation() == DND.DROP_COPY) {
      return super.performDrop(data);
    }
    // DROP_MOVE, move IResource(s) to IContainer
    if (target instanceof IContainer && data instanceof IStructuredSelection) {
      final IContainer destination = (IContainer) target;
      // prepare resources
      final IResource[] resources;
      {
        Object[] selectionObjects = ((IStructuredSelection) data).toArray();
        resources = new IResource[selectionObjects.length];
        for (int i = 0; i < selectionObjects.length; i++) {
          Object o = selectionObjects[i];
          if (o instanceof IResource && !(o instanceof IFolder)) {
            resources[i] = (IResource) o;
          } else {
            return false;
          }
        }
      }
      // wait for background analysis
      if (!RefactoringUtils.waitReadyForRefactoring2()) {
        return false;
      }
      // execute MoveRefactoring
      try {
        RefactoringStatus status = new RefactoringStatus();
        MoveSupport.performMove(status, resources, destination);
      } catch (InterruptedException e) {
        // ignore
      } catch (Throwable e) {
        DartToolsPlugin.log(e);
      }
    }
    // we don't know how to move this
    return false;
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
