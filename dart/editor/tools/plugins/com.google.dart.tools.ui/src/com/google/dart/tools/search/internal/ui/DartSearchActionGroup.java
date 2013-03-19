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
  private FindDeclarationsAction_OLD findDeclarationsAction;

  public DartSearchActionGroup(DartEditor editor) {
    this.site = editor.getSite();
    findReferencesAction = new FindReferencesAction(editor);
    findReferencesAction.setActionDefinitionId(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
    findReferencesAction.setId(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
  }

  public DartSearchActionGroup(Page page) {
    this(page.getSite());
  }

  private DartSearchActionGroup(IWorkbenchSite site) {
    this.site = site;
    findReferencesAction = new FindReferencesAction(site);
    findReferencesAction.setActionDefinitionId(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
    findReferencesAction.setId(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
    // TODO(scheglov)
    site.getSelectionProvider().addSelectionChangedListener(findReferencesAction);
  }

  @Override
  public void dispose() {
    disposeAction(findReferencesAction);
    // TODO(scheglov) remove check for null
    if (findDeclarationsAction != null) {
      disposeAction(findDeclarationsAction);
    }
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
      // TODO(scheglov) remove check for null
      if (findReferencesAction != null) {
        findReferencesAction.update(selection);
        appendToGroup(menu, findReferencesAction);
      }
      // TODO(scheglov) remove check for null
      if (findDeclarationsAction != null) {
        findDeclarationsAction.update(selection);
        appendToGroup(menu, findDeclarationsAction);
      }
//      if (sel instanceof DartElementSelection) {
//        DartElementSelection selection = (DartElementSelection) sel;
//        if (ActionUtil.isFindOverridesAvailable(selection)) {
//          findOverridesAction.update(selection);
//          appendToGroup(mm, findOverridesAction);
//        }
//        if (ActionUtil.isFindDeclarationsAvailable(selection)) {
//          findDeclarationsAction.update(selection);
//          appendToGroup(mm, findDeclarationsAction);
//        }
//        if (ActionUtil.isFindUsesAvailable(selection)) {
//          findReferencesAction.update(selection);
//          appendToGroup(mm, findReferencesAction);
//        }
//      } else {
//        // TODO(messick): Remove this branch.
//        appendToGroup(mm, findDeclarationsAction);
//        appendToGroup(mm, findReferencesAction);
//      }
    }
  }

  private void appendToGroup(IMenuManager menu, IAction action) {
    if (action.isEnabled()) {
      menu.prependToGroup(ITextEditorActionConstants.GROUP_OPEN, action);
    }
  }

  private void disposeAction(ISelectionChangedListener action) {
    ISelectionProvider provider = site.getSelectionProvider();
    if (provider != null) {
      provider.removeSelectionChangedListener(action);
    }
  }
}
