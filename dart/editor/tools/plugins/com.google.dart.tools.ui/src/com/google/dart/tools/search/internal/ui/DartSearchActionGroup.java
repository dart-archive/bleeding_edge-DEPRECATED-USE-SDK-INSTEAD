/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.search.internal.ui;

import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * {@link ActionGroup} that adds the Dart search actions.
 */
public class DartSearchActionGroup extends ActionGroup {
  private IWorkbenchSite site;
  private FindReferencesAction findReferencesAction;
  private FindDeclarationsAction findDeclarationsAction;

  public DartSearchActionGroup(DartEditor editor) {
    this.site = editor.getSite();
    findReferencesAction = new FindReferencesAction(editor);
    findDeclarationsAction = new FindDeclarationsAction(editor);
    initActions();
  }

  public DartSearchActionGroup(Page page) {
    this(page.getSite());
  }

  private DartSearchActionGroup(IWorkbenchSite site) {
    this.site = site;
    findReferencesAction = new FindReferencesAction(site);
    findDeclarationsAction = new FindDeclarationsAction(site);
    initActions();
    // TODO(scheglov)
//    site.getSelectionProvider().addSelectionChangedListener(findReferencesAction);
  }

  @Override
  public void dispose() {
    disposeAction(findReferencesAction);
    disposeAction(findDeclarationsAction);
    findReferencesAction = null;
    findDeclarationsAction = null;
    super.dispose();
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);
    menu.add(new Separator());
    {
      ISelection selection = getContext().getSelection();
      findReferencesAction.update(selection);
      findDeclarationsAction.update(selection);
      appendToGroup(menu, findReferencesAction);
      appendToGroup(menu, findDeclarationsAction);
    }
  }

  private void appendToGroup(IMenuManager menu, IAction action) {
    if (action.isEnabled()) {
      menu.appendToGroup(ITextEditorActionConstants.GROUP_OPEN, action);
    }
  }

  private void disposeAction(ISelectionChangedListener action) {
    ISelectionProvider provider = site.getSelectionProvider();
    if (provider != null) {
      provider.removeSelectionChangedListener(action);
    }
  }

  private void initActions() {
    findReferencesAction.setActionDefinitionId(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
    findReferencesAction.setId(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
    findDeclarationsAction.setActionDefinitionId(DartEditorActionDefinitionIds.SEARCH_DECLARATIONS_IN_WORKSPACE);
    findDeclarationsAction.setId(DartEditorActionDefinitionIds.SEARCH_DECLARATIONS_IN_WORKSPACE);
  }
}
