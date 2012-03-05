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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;

/**
 * Action group to add the filter actions to a view part's toolbar menu.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 */
public class CallHierarchyFiltersActionGroup extends ActionGroup {

//  class ShowExpandWithConstructorsDialogAction extends Action {
//    ShowExpandWithConstructorsDialogAction() {
//      setText(CallHierarchyMessages.ShowExpandWithConstructorsDialogAction_text);
//    }
//
//    @Override
//    public void run() {
//      openExpandWithConstructorsDialog();
//    }
//  }

  class ShowFilterDialogAction extends Action {
    ShowFilterDialogAction() {
      setText(CallHierarchyMessages.ShowFilterDialogAction_text);
      setImageDescriptor(DartPluginImages.DESC_ELCL_FILTER);
      setDisabledImageDescriptor(DartPluginImages.DESC_DLCL_FILTER);
    }

    @Override
    public void run() {
      openFiltersDialog();
    }
  }

  private IViewPart viewPart;

  /**
   * Creates a new <code>CustomFiltersActionGroup</code>.
   * 
   * @param part the view part that owns this action group
   * @param viewer the viewer to be filtered
   */
  public CallHierarchyFiltersActionGroup(IViewPart part, StructuredViewer viewer) {
    Assert.isNotNull(part);
    Assert.isNotNull(viewer);
    viewPart = part;
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    fillViewMenu(actionBars.getMenuManager());
  }

  private void fillViewMenu(IMenuManager viewMenu) {
    viewMenu.add(new Separator("filters")); //$NON-NLS-1$
    viewMenu.add(new ShowFilterDialogAction());
//    viewMenu.add(new ShowExpandWithConstructorsDialogAction());
  }

//  private void openExpandWithConstructorsDialog() {
//    Shell parentShell = viewPart.getViewSite().getShell();
//    new ExpandWithConstructorsDialog(parentShell).open();
//  }

  private void openFiltersDialog() {
    FiltersDialog dialog = new FiltersDialog(viewPart.getViewSite().getShell());
    dialog.open();
  }
}
