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

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This action removes a single node from the Call Hierarchy view.
 */
class RemoveFromViewAction extends Action {

  /**
   * The Call Hierarchy view part.
   */
  private CallHierarchyViewPart chvPart;

  /**
   * The Call Hierarchy viewer.
   */
  private CallHierarchyViewer callHierarchyViewer;

  /**
   * Creates the hide single node action.
   * 
   * @param part the call hierarchy view part
   * @param viewer the call hierarchy viewer
   */
  public RemoveFromViewAction(CallHierarchyViewPart part, CallHierarchyViewer viewer) {
    chvPart = part;
    callHierarchyViewer = viewer;
    setText(CallHierarchyMessages.RemoveFromViewAction_removeFromView_text);
    setDescription(CallHierarchyMessages.RemoveFromViewAction_removeFromView_description);
    setToolTipText(CallHierarchyMessages.RemoveFromViewAction_removeFromView_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CALL_HIERARCHY_REMOVE_FROM_VIEW_ACTION);

    ISharedImages workbenchImages = DartToolsPlugin.getDefault().getWorkbench().getSharedImages();
    setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE_DISABLED));
    setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));
    setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));
  }

  @Override
  public void run() {
    DartElement[] inputElements = chvPart.getInputElements();
    List<DartElement> inputList = new ArrayList<DartElement>(Arrays.asList(inputElements));
    DartElement[] selection = getSelectedElements();
    for (int i = 0; i < selection.length; i++) {
      if (inputList.contains(selection[i])) {
        inputList.remove(selection[i]);
      }
    }
    if (inputList.size() > 0) {
      chvPart.updateInputHistoryAndDescription(
          inputElements,
          inputList.toArray(new TypeMember[inputList.size()]));
    }
    TreeItem[] items = callHierarchyViewer.getTree().getSelection();
    for (int i = 0; i < items.length; i++) {
      items[i].dispose();
    }
  }

  /**
   * Checks whether this action can be added for the selected element in the call hierarchy.
   * 
   * @return <code> true</code> if the action can be added, <code>false</code> otherwise
   */
  protected boolean canActionBeAdded() {
    IStructuredSelection selection = (IStructuredSelection) getSelection();
    if (selection.isEmpty()) {
      return false;
    }

    Iterator<?> iter = selection.iterator();
    while (iter.hasNext()) {
      Object element = iter.next();
      if (!(element instanceof MethodWrapper)) {
        return false;
      }
    }

    TreeItem[] items = callHierarchyViewer.getTree().getSelection();
    for (int k = 0; k < items.length; k++) {
      if (!checkForChildren(items[k])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks whether the children are being fetched for a node recursively.
   * 
   * @param item the parent node
   * @return <code>false</code> when children are currently being fetched for a node,
   *         <code>true</code> otherwise
   */
  private boolean checkForChildren(TreeItem item) {
    TreeItem[] children = item.getItems();
    if (children.length == 1) {
      Object data = children[0].getData();
      if (!(data instanceof MethodWrapper) && data != null) {
        return false; // Do not add action if children are still being fetched for that node or if it's only JFace's dummy node.
      }
    }
    for (int i = 0; i < children.length; i++) {
      if (!checkForChildren(children[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Gets the elements selected in the call hierarchy view part.
   * 
   * @return the elements
   */
  private DartElement[] getSelectedElements() {
    ISelection selection = getSelection();
    if (selection instanceof IStructuredSelection) {
      List<DartElement> members = new ArrayList<DartElement>();
      List<?> elements = ((IStructuredSelection) selection).toList();
      for (Iterator<?> iter = elements.iterator(); iter.hasNext();) {
        Object obj = iter.next();
        if (obj instanceof MethodWrapper) {
          MethodWrapper wrapper = (MethodWrapper) obj;
          members.add((wrapper).getMember());
        }
      }
      return members.toArray(new TypeMember[members.size()]);
    }
    return null;
  }

  /**
   * Gets the selection from the call hierarchy view part.
   * 
   * @return the current selection
   */
  private ISelection getSelection() {
    return chvPart.getSelection();
  }
}
