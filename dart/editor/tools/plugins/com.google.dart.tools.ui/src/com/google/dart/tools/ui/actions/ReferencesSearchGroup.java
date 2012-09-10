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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Action group that adds the search for references actions to a context menu and the global menu
 * bar.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ReferencesSearchGroup extends ActionGroup {

  private IActionBars actionBars;
  private IWorkbenchSite site;

  private FindReferencesAction findReferencesAction;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor the Dart editor
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public ReferencesSearchGroup(DartEditor editor) {
    Assert.isNotNull(editor);
    site = editor.getSite();
    findReferencesAction = new FindReferencesAction(editor);
    findReferencesAction.setActionDefinitionId(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
    editor.setAction("SearchReferencesInWorkspace", findReferencesAction); //$NON-NLS-1$
  }

  /**
   * Creates a new <code>ReferencesSearchGroup</code>. The group requires that the selection
   * provided by the site's selection provider is of type <code>
   * org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param site the view part that owns this action group
   */
  public ReferencesSearchGroup(IWorkbenchSite site) {
    this(site, null);
  }

  /**
   * Creates a new <code>ReferencesSearchGroup</code>. The group requires that the selection
   * provided by the given selection provider is of type {@link IStructuredSelection}.
   * 
   * @param site the site that will own the action group.
   * @param specialSelectionProvider the selection provider used instead of the sites selection
   *          provider.
   */
  public ReferencesSearchGroup(IWorkbenchSite site, ISelectionProvider specialSelectionProvider) {
    this.site = site;

    findReferencesAction = new FindReferencesAction(site);
    findReferencesAction.setActionDefinitionId(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);

    // register the actions as selection listeners
    ISelectionProvider provider = specialSelectionProvider == null ? site.getSelectionProvider()
        : specialSelectionProvider;
    ISelection selection = provider.getSelection();
    registerAction(findReferencesAction, provider, selection, specialSelectionProvider);
  }

  @Override
  public void dispose() {
    ISelectionProvider provider = site.getSelectionProvider();
    if (provider != null) {
      disposeAction(findReferencesAction, provider);
    }
    findReferencesAction = null;
    updateGlobalActionHandlers();
    super.dispose();
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    Assert.isNotNull(actionBars);
    this.actionBars = actionBars;
    updateGlobalActionHandlers();
  }

  @Override
  public void fillContextMenu(IMenuManager mm) {
    appendToGroup(mm, findReferencesAction);
  }

  private void appendToGroup(IMenuManager menu, IAction action) {
    if (action.isEnabled()) {
      menu.prependToGroup(ITextEditorActionConstants.GROUP_OPEN, action);
    }
  }

  private void disposeAction(ISelectionChangedListener action, ISelectionProvider provider) {
    if (action != null) {
      provider.removeSelectionChangedListener(action);
    }
  }

  private void registerAction(SelectionDispatchAction action, ISelectionProvider provider,
      ISelection selection, ISelectionProvider specialSelectionProvider) {
    action.update(selection);
    provider.addSelectionChangedListener(action);
    if (specialSelectionProvider != null) {
      action.setSpecialSelectionProvider(specialSelectionProvider);
    }
  }

  private void updateGlobalActionHandlers() {
    if (actionBars != null) {
      actionBars.setGlobalActionHandler(
          JdtActionConstants.FIND_REFERENCES_IN_WORKSPACE,
          findReferencesAction);
    }
  }
}
