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

import com.google.dart.tools.ui.IContextMenuConstants;
import com.google.dart.tools.ui.internal.libraryview.DeleteAction;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

/**
 * Action group that adds copy, cut, paste, and delete actions to a view part's context menu and
 * installs handlers for the corresponding global menu actions.
 * <p>
 * This class was originally copied over from the JDT's org.eclipse.jdt.ui.actions.CCPActionGroup
 * class.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * <p>
 * The Copy and Paste actions have been removed from the this action group until after the initial
 * launch of the product.
 * <p>
 * We have commented out the Cut action below, but we may want to add it back in later if we choose
 * to have the CTRL-X keyboard command (or menu Edit>Cut) behave like it does in the JDT.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class CCPActionGroup extends ActionGroup {

  private final SelectionDispatchAction[] actions;

  private final SelectionDispatchAction deleteAction;
//  private final SelectionDispatchAction copyAction;
//  private final SelectionDispatchAction pasteAction;
//  private final SelectionDispatchAction cutAction;
  private final ISelectionProvider selectionProvider;

  /**
   * Creates a new <code>CCPActionGroup</code>. The group requires that the selection provided by
   * the view part's selection provider is of type
   * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param part the view part that owns this action group
   */
  public CCPActionGroup(IViewPart part) {
    this(part.getSite());
  }

  /**
   * Creates a new <code>CCPActionGroup</code>. The group requires that the selection provided by
   * the page's selection provider is of type
   * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param page the page that owns this action group
   */
  public CCPActionGroup(Page page) {
    this(page.getSite());
  }

  /**
   * Creates a new <code>CCPActionGroup</code>. The group requires that the selection provided by
   * the given selection provider is of type {@link IStructuredSelection}.
   * 
   * @param site the site that will own the action group.
   */
  private CCPActionGroup(IWorkbenchSite site) {
    selectionProvider = site.getSelectionProvider();

//    copyAction = new CopyToClipboardAction(site);
//    copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
//    pasteAction = new PasteAction(site);
//    pasteAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_PASTE);
    deleteAction = new DeleteAction(site);
    deleteAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);
//    cutAction = new CutAction(site);
//    cutAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_CUT);
    actions = new SelectionDispatchAction[] {/* cutAction,copyAction, pasteAction, */deleteAction};

    registerActionsAsSelectionChangeListeners();
  }

  @Override
  public void dispose() {
    super.dispose();
    deregisterActionsAsSelectionChangeListeners();
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    super.fillActionBars(actionBars);
    actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);
//    actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
//    actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), cutAction);
//    actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);
    for (int i = 0; i < actions.length; i++) {
      SelectionDispatchAction action = actions[i];
//      if (action == cutAction && !cutAction.isEnabled()) {
//        continue;
//      }
      menu.appendToGroup(IContextMenuConstants.GROUP_EDIT, action);
    }
  }

  /**
   * Returns the delete action managed by this action group.
   * 
   * @return the delete action. Returns <code>null</code> if the group doesn't provide any delete
   *         action
   */
  public IAction getDeleteAction() {
    return deleteAction;
  }

  /**
   * De-register the set of actions in this action group with the selection provider.
   * 
   * @see CCPActionGroup#registerActionsAsSelectionChangeListeners()
   */
  private void deregisterActionsAsSelectionChangeListeners() {
    ISelectionProvider provider = selectionProvider;
    for (SelectionDispatchAction action : actions) {
      provider.removeSelectionChangedListener(action);
    }
  }

  /**
   * Register the set of actions in this action group with the selection provider.
   * 
   * @see CCPActionGroup#deregisterActionsAsSelectionChangeListeners()
   */
  private void registerActionsAsSelectionChangeListeners() {
    ISelectionProvider provider = selectionProvider;
    ISelection selection = provider.getSelection();
    for (SelectionDispatchAction action : actions) {
      action.update(selection);
      provider.addSelectionChangedListener(action);
    }
  }
}
