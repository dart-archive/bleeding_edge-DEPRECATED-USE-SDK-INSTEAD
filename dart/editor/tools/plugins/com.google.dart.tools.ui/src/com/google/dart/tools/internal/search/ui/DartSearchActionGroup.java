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
package com.google.dart.tools.internal.search.ui;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction;
import com.google.dart.tools.ui.actions.AbstractDartSelectionActionGroup;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.actions.OpenAction;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * {@link ActionGroup} that adds the Dart search actions.
 * 
 * @coverage dart.editor.ui.search
 */
public class DartSearchActionGroup extends AbstractDartSelectionActionGroup {
  private AbstractDartSelectionAction findReferencesAction;
  private AbstractDartSelectionAction findDeclarationsAction;
  private OpenAction openAction;

  public DartSearchActionGroup(DartEditor editor) {
    super(editor);
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      findReferencesAction = new FindReferencesAction_NEW(editor);
    } else {
      findReferencesAction = new FindReferencesAction(editor);
    }
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      findDeclarationsAction = new FindDeclarationsAction_NEW(editor);
    } else {
      findDeclarationsAction = new FindDeclarationsAction(editor);
    }
    openAction = new OpenAction(editor);
    initActions();
    editor.setAction(findReferencesAction.getActionDefinitionId(), findReferencesAction);
    editor.setAction(findDeclarationsAction.getActionDefinitionId(), findDeclarationsAction);
    editor.setAction("OpenEditor", openAction);
    addActions(findReferencesAction, findDeclarationsAction, openAction);
    addActionDartSelectionListeners();
  }

  public DartSearchActionGroup(IViewPart part) {
    this(part.getViewSite());
  }

  public DartSearchActionGroup(IWorkbenchSite site) {
    super(site);
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      findReferencesAction = new FindReferencesAction_NEW(site);
    } else {
      findReferencesAction = new FindReferencesAction(site);
    }
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      findDeclarationsAction = new FindDeclarationsAction_NEW(site);
    } else {
      findDeclarationsAction = new FindDeclarationsAction(site);
    }
    initActions();
    addActions(findReferencesAction, findDeclarationsAction);
    addActionSelectionListeners();
  }

  @Override
  public void dispose() {
    super.dispose();
    findReferencesAction = null;
    findDeclarationsAction = null;
    openAction = null;
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    ISelection selection = getContext().getSelection();
    updateActions(selection);
    appendToGroup(menu, ITextEditorActionConstants.GROUP_OPEN);
  }

  /**
   * Initializes definition attributes of actions.
   */
  private void initActions() {
    findReferencesAction.setActionDefinitionId(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
    findReferencesAction.setId(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
    findDeclarationsAction.setActionDefinitionId(DartEditorActionDefinitionIds.SEARCH_DECLARATIONS_IN_WORKSPACE);
    findDeclarationsAction.setId(DartEditorActionDefinitionIds.SEARCH_DECLARATIONS_IN_WORKSPACE);
    if (openAction != null) {
      openAction.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_EDITOR);
      openAction.setId(DartEditorActionDefinitionIds.OPEN_EDITOR);
    }
  }
}
