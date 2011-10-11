/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

/**
 * Action group that adds the open and show actions to a context menu and the action bar's navigate
 * menu. This action group reuses the <code>
 * OpenEditorActionGroup</code> and <code>OpenViewActionGroup</code>.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * <p>
 * The OpenViewActionGroup has been commented out but not deleted, in case we want the action group
 * in the future.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class NavigateActionGroup extends ActionGroup {

  private OpenEditorActionGroup openEditorActionGroup;

  //private OpenViewActionGroup openViewActionGroup;

  /**
   * Creates a new <code>NavigateActionGroup</code>. The group requires that the selection provided
   * by the part's selection provider is of type
   * {@link org.eclipse.jface.viewers.IStructuredSelection}.
   *
   * @param part the view part that owns this action group
   */
  public NavigateActionGroup(IViewPart part) {
    openEditorActionGroup = new OpenEditorActionGroup(part);
    //openViewActionGroup = new OpenViewActionGroup(part);
  }

  /**
   * Creates a new <code>NavigateActionGroup</code>. The group requires that the selection provided
   * by the given selection provider is of type {@link IStructuredSelection}.
   *
   * @param site the site that will own the action group.
   * @param specialSelectionProvider the selection provider used instead of the sites selection
   *          provider.
   */
  public NavigateActionGroup(IWorkbenchPartSite site, ISelectionProvider specialSelectionProvider) {
    openEditorActionGroup = new OpenEditorActionGroup(site, specialSelectionProvider);
    //openViewActionGroup = new OpenViewActionGroup(site, specialSelectionProvider);
  }

  @Override
  public void dispose() {
    super.dispose();
    openEditorActionGroup.dispose();
    //openViewActionGroup.dispose();
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    super.fillActionBars(actionBars);
    openEditorActionGroup.fillActionBars(actionBars);
    //openViewActionGroup.fillActionBars(actionBars);
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);

    openEditorActionGroup.fillContextMenu(menu);
    //openViewActionGroup.fillContextMenu(menu);
  }

  /**
   * Returns the edit action managed by this action group.
   *
   * @return the edit action. Returns <code>null</code> if the group doesn't provide any edit action
   */
  public IAction getEditAction() {
    IAction editAction = openEditorActionGroup.getOpenAction();
    editAction.setText(ActionMessages.EditAction_label);
    editAction.setDescription(ActionMessages.EditAction_description);
    return editAction;
  }

  @Override
  public void setContext(ActionContext context) {
    super.setContext(context);
    openEditorActionGroup.setContext(context);
    //openViewActionGroup.setContext(context);
  }

  @Override
  public void updateActionBars() {
    super.updateActionBars();
    openEditorActionGroup.updateActionBars();
    //openViewActionGroup.updateActionBars();
  }
}
