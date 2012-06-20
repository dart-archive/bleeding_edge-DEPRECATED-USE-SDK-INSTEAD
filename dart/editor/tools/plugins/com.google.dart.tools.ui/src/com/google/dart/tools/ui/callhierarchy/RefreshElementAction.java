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

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Action class to refresh a single element in the call hierarchy.
 */
public class RefreshElementAction extends Action {

  /**
   * The call hierarchy viewer.
   */
  private final CallHierarchyViewer viewer;

  /**
   * Creates the action to refresh a single element in the call hierarchy.
   * 
   * @param viewer the call hierarchy viewer
   */
  public RefreshElementAction(CallHierarchyViewer viewer) {
    this.viewer = viewer;
    setText(CallHierarchyMessages.RefreshSingleElementAction_text);
    setToolTipText(CallHierarchyMessages.RefreshSingleElementAction_tooltip);
    setDescription(CallHierarchyMessages.RefreshSingleElementAction_description);
    DartPluginImages.setLocalImageDescriptors(this, "refresh.gif");//$NON-NLS-1$
    setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CALL_HIERARCHY_REFRESH_SINGLE_ELEMENT_ACTION);
    setEnabled(true);
  }

  @Override
  public void run() {
    IStructuredSelection selection = (IStructuredSelection) getSelection();
    if (selection.isEmpty()) {
      viewer.getPart().refresh();
      return;
    }
    List<MethodWrapper> toExpand = new ArrayList<MethodWrapper>();
    for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
      MethodWrapper element = (MethodWrapper) iter.next();
      boolean isExpanded = viewer.getExpandedState(element);
      element.removeFromCache();
      if (isExpanded) {
        viewer.setExpandedState(element, false);
        toExpand.add(element);
      }
      viewer.refresh(element);
    }
    for (Iterator<MethodWrapper> iter = toExpand.iterator(); iter.hasNext();) {
      MethodWrapper elem = iter.next();
      viewer.setExpandedState(elem, true);
    }
  }

  /**
   * Gets the selection from the call hierarchy view part.
   * 
   * @return the current selection
   */
  private ISelection getSelection() {
    return viewer.getSelection();
  }
}
