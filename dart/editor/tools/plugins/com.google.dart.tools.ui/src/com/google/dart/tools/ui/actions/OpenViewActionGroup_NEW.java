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

import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;

/**
 * Action group that with "Open..." actions
 */
public class OpenViewActionGroup_NEW extends AbstractDartSelectionActionGroup {
  private AbstractDartSelectionAction_NEW typeHierarchyAction;

  public OpenViewActionGroup_NEW(DartEditor editor) {
    super(editor);
    typeHierarchyAction = new OpenTypeHierarchyAction_NEW(editor);
    initActions();
    addActions(typeHierarchyAction);
    addActionDartSelectionListeners();
  }

  @Override
  public void dispose() {
    super.dispose();
    typeHierarchyAction = null;
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    super.fillActionBars(actionBars);
    actionBars.setGlobalActionHandler(DartActionConstants.OPEN_TYPE_HIERARCHY, typeHierarchyAction);
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
  }

  /**
   * Initializes definition attributes of actions.
   */
  private void initActions() {
    typeHierarchyAction.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_TYPE_HIERARCHY);
    typeHierarchyAction.setId(DartEditorActionDefinitionIds.OPEN_TYPE_HIERARCHY);
  }
}
