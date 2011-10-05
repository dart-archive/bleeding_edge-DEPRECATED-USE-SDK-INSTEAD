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
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenWithMenu;

/**
 * Action group that adds the actions opening a new editor to the context menu and the action bar's
 * navigate menu.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OpenEditorActionGroup extends ActionGroup {

  private boolean fIsEditorOwner;
  private OpenAction fOpen;
  private ISelectionProvider fSelectionProvider;
  private IWorkbenchSite fSite;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor the Dart editor
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public OpenEditorActionGroup(DartEditor editor) {
    fIsEditorOwner = true;
    fOpen = new OpenAction(editor);
    fOpen.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
    editor.setAction("OpenEditor", fOpen); //$NON-NLS-1$
    fSite = editor.getEditorSite();
    fSelectionProvider = fSite.getSelectionProvider();
    initialize();
  }

  /**
   * Creates a new <code>OpenActionGroup</code>. The group requires that the selection provided by
   * the part's selection provider is of type <code>
   * org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param part the view part that owns this action group
   */
  public OpenEditorActionGroup(IViewPart part) {
    this(part.getSite(), null);
  }

  /**
   * Creates a new <code>OpenEditorActionGroup</code>. The group requires that the selection
   * provided by the given selection provider is of type {@link IStructuredSelection}.
   * 
   * @param site the site that will own the action group.
   * @param specialSelectionProvider the selection provider used instead of the site's selection
   *          provider.
   */
  public OpenEditorActionGroup(IWorkbenchPartSite site, ISelectionProvider specialSelectionProvider) {
    fSite = site;
    fOpen = new OpenAction(fSite);
    fOpen.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
    fSelectionProvider = specialSelectionProvider == null ? fSite.getSelectionProvider()
        : specialSelectionProvider;
    initialize();
    if (specialSelectionProvider != null) {
      fOpen.setSpecialSelectionProvider(specialSelectionProvider);
    }
  }

  /*
   * @see ActionGroup#dispose()
   */
  @Override
  public void dispose() {
    fSelectionProvider.removeSelectionChangedListener(fOpen);
    super.dispose();
  }

  /*
   * (non-Javadoc) Method declared in ActionGroup
   */
  @Override
  public void fillActionBars(IActionBars actionBar) {
    super.fillActionBars(actionBar);
    setGlobalActionHandlers(actionBar);
  }

  /*
   * (non-Javadoc) Method declared in ActionGroup
   */
  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);
    appendToGroup(menu, fOpen);
    if (!fIsEditorOwner) {
      addOpenWithMenu(menu);
    }
  }

  /**
   * Returns the open action managed by this action group.
   * 
   * @return the open action. Returns <code>null</code> if the group doesn't provide any open action
   */
  public IAction getOpenAction() {
    return fOpen;
  }

  private void addOpenWithMenu(IMenuManager menu) {
    ISelection selection = getContext().getSelection();
    if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
      return;
    }
    IStructuredSelection ss = (IStructuredSelection) selection;
    if (ss.size() != 1) {
      return;
    }

    Object o = ss.getFirstElement();
    if (!(o instanceof IAdaptable)) {
      return;
    }

    IAdaptable element = (IAdaptable) o;
    Object resource = element.getAdapter(IResource.class);
    if (!(resource instanceof IFile)) {
      return;
    }

    // Create a menu.
    IMenuManager submenu = new MenuManager(ActionMessages.OpenWithMenu_label);
    submenu.add(new OpenWithMenu(fSite.getPage(), (IFile) resource));

    // Add the submenu.
    menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);
  }

  private void appendToGroup(IMenuManager menu, IAction action) {
    if (action.isEnabled()) {
      menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, action);
    }
  }

  private void initialize() {
    ISelection selection = fSelectionProvider.getSelection();
    fOpen.update(selection);
    if (!fIsEditorOwner) {
      fSelectionProvider.addSelectionChangedListener(fOpen);
    }
  }

  private void setGlobalActionHandlers(IActionBars actionBars) {
    actionBars.setGlobalActionHandler(JdtActionConstants.OPEN, fOpen);
  }
}
