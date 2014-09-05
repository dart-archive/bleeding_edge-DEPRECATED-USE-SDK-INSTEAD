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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Action group that adds refactoring actions to a context menu and the global menu bar.
 */
public class RefactorActionGroup_OLD extends AbstractDartSelectionActionGroup {
  public static final String GROUP_REORG = "reorgGroup";

  private RenameAction_OLD renameAction;
  private ExtractLocalAction_OLD extractLocalAction;
  private ExtractMethodAction extractMethodAction;
  private InlineAction inlineAction;
  private ConvertMethodToGetterAction convertMethodToGetterAction;
  private ConvertGetterToMethodAction convertGetterToMethodAction;

  public RefactorActionGroup_OLD(DartEditor editor) {
    super(editor);
    renameAction = new RenameAction_OLD(editor);
    extractLocalAction = new ExtractLocalAction_OLD(editor);
    extractMethodAction = new ExtractMethodAction(editor);
    inlineAction = new InlineAction(editor);
    convertMethodToGetterAction = new ConvertMethodToGetterAction(editor);
    convertGetterToMethodAction = new ConvertGetterToMethodAction(editor);
    initActions();
    editor.setAction("RenameElement", renameAction);
    addActions(
        renameAction,
        extractLocalAction,
        extractMethodAction,
        inlineAction,
        convertMethodToGetterAction,
        convertGetterToMethodAction);
    addActionDartSelectionListeners();
  }

  public RefactorActionGroup_OLD(IWorkbenchSite site) {
    super(site);
    renameAction = new RenameAction_OLD(site);
    convertMethodToGetterAction = new ConvertMethodToGetterAction(site);
    convertGetterToMethodAction = new ConvertGetterToMethodAction(site);
    initActions();
    addActions(renameAction, convertMethodToGetterAction, convertGetterToMethodAction);
    addActionSelectionListeners();
  }

  @Override
  public void dispose() {
    super.dispose();
    renameAction = null;
    extractLocalAction = null;
    extractMethodAction = null;
    inlineAction = null;
    convertMethodToGetterAction = null;
    convertGetterToMethodAction = null;
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    super.fillActionBars(actionBars);
    actionBars.setGlobalActionHandler(DartActionConstants.RENAME, renameAction);
    actionBars.setGlobalActionHandler(DartActionConstants.EXTRACT_LOCAL, extractLocalAction);
    actionBars.setGlobalActionHandler(DartActionConstants.EXTRACT_METHOD, extractMethodAction);
    actionBars.setGlobalActionHandler(DartActionConstants.INLINE, inlineAction);
    actionBars.setGlobalActionHandler(
        DartActionConstants.CONVERT_METHOD_TO_GETTER,
        convertMethodToGetterAction);
    actionBars.setGlobalActionHandler(
        DartActionConstants.CONVERT_GETTER_TO_METHOD,
        convertGetterToMethodAction);
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, new Separator(GROUP_REORG));
    // append refactoring actions
    ISelection selection = getContext().getSelection();
    updateActions(selection);
    appendToGroup(menu, GROUP_REORG);
  }

  /**
   * Initializes definition attributes of actions.
   */
  private void initActions() {
    renameAction.setActionDefinitionId(DartEditorActionDefinitionIds.RENAME_ELEMENT);
    renameAction.setId(DartEditorActionDefinitionIds.RENAME_ELEMENT);
  }
}
