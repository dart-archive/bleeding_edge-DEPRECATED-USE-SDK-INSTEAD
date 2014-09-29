/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.IContextMenuConstants;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchSite;

/**
 * Action group that adds the actions opening a new editor to the context menu and the action bar's
 * navigate menu.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OpenEditorActionGroup_NEW extends AbstractDartSelectionActionGroup {
  private OpenAction_NEW fOpen;
  private ISelectionProvider fSelectionProvider;
  private IWorkbenchSite fSite;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor the Dart editor
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public OpenEditorActionGroup_NEW(DartEditor editor) {
    super(editor);
    fOpen = new OpenAction_NEW(editor);
    fOpen.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_EDITOR);
    fOpen.setId(DartEditorActionDefinitionIds.OPEN_EDITOR);
    editor.setAction("OpenEditor", fOpen); //$NON-NLS-1$
    fSite = editor.getEditorSite();
    fSelectionProvider = fSite.getSelectionProvider();
    initialize();
    addActions(fOpen);
  }

  @Override
  public void dispose() {
    fSelectionProvider.removeSelectionChangedListener(fOpen);
    super.dispose();
  }

  @Override
  public void fillActionBars(IActionBars actionBar) {
    super.fillActionBars(actionBar);
    setGlobalActionHandlers(actionBar);
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);
    appendToGroup(menu, fOpen);
  }

  /**
   * Returns the open action managed by this action group.
   * 
   * @return the open action. Returns <code>null</code> if the group doesn't provide any open action
   */
  public IAction getOpenAction() {
    return fOpen;
  }

  private void appendToGroup(IMenuManager menu, IAction action) {
    if (action.isEnabled()) {
      menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, action);
    }
  }

  private void initialize() {
    ISelection selection = fSelectionProvider.getSelection();
    fOpen.selectionChanged(selection);
  }

  private void setGlobalActionHandlers(IActionBars actionBars) {
    actionBars.setGlobalActionHandler(DartActionConstants.OPEN, fOpen);
  }
}
