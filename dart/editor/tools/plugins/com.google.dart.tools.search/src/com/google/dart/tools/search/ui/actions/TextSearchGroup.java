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
package com.google.dart.tools.search.ui.actions;

import com.google.dart.tools.search2.internal.ui.SearchMessages;
import com.google.dart.tools.search2.internal.ui.text2.FindInFileActionDelegate;
import com.google.dart.tools.search2.internal.ui.text2.FindInRecentScopeActionDelegate;
import com.google.dart.tools.search2.internal.ui.text2.FindInWorkspaceActionDelegate;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Action group that adds a sub-menu with text search actions to a context menu.
 */
public class TextSearchGroup extends ActionGroup {

  private static final String CTX_MENU_ID = "com.google.dart.tools.search.text.ctxmenu"; //$NON-NLS-1$

  private String fAppendToGroup = ITextEditorActionConstants.GROUP_FIND;
  private String fMenuText = SearchMessages.TextSearchGroup_submenu_text;
  private FindInRecentScopeActionDelegate[] fActions;

  /**
   * Constructs a TextSearchGroup for adding actions to the context menu of the editor provided. The
   * editor will be accessed for the purpose of determining the search string.
   * 
   * @param editor the editor
   */
  public TextSearchGroup(IEditorPart editor) {
    createActions(editor);
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    MenuManager textSearchMM = new MenuManager(fMenuText, CTX_MENU_ID);
    int i = 0;
    for (i = 0; i < fActions.length - 1; i++) {
      textSearchMM.add(fActions[i]);
    }
    textSearchMM.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    textSearchMM.add(new Separator());
    textSearchMM.add(fActions[i]);

    menu.appendToGroup(fAppendToGroup, textSearchMM);
  }

  /**
   * Changes the group where the submenu is appended to. The default is
   * ITextEditorActionConstants.GROUP_FIND.
   * 
   * @param groupID the group id to append to
   */
  public void setAppendToGroup(String groupID) {
    fAppendToGroup = groupID;
  }

  /**
   * Changes the text that is used for the submenu label. The default is "Search Text".
   * 
   * @param text the text for the menu label.
   */
  public void setMenuText(String text) {
    fMenuText = text;
  }

  private void createActions(IEditorPart editor) {
    fActions = new FindInRecentScopeActionDelegate[] {
        new FindInWorkspaceActionDelegate(), new FindInFileActionDelegate()};
    for (int i = 0; i < fActions.length; i++) {
      FindInRecentScopeActionDelegate action = fActions[i];
      action.setActiveEditor(action, editor);
    }
  }
}
