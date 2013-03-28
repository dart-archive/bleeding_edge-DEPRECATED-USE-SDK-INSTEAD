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
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchSite;

/**
 * Action group that with "Open..." actions
 */
public class OpenViewActionGroup extends AbstractDartSelectionActionGroup {
  private OpenTypeHierarchyAction thAction;

  public OpenViewActionGroup(DartEditor editor) {
    super(editor);
    thAction = new OpenTypeHierarchyAction(editor);
    initActions();
    addActions(thAction);
    addActionDartSelectionListeners();
  }

  public OpenViewActionGroup(IWorkbenchSite site) {
    super(site);
    thAction = new OpenTypeHierarchyAction(site);
    initActions();
    addActions(thAction);
    addActionSelectionListeners();
  }

  @Override
  public void dispose() {
    super.dispose();
    thAction = null;
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    super.fillActionBars(actionBars);
    actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_TYPE_HIERARCHY, thAction);
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    // TODO(scheglov)
  }

  /**
   * Initializes definition attributes of actions.
   */
  private void initActions() {
    thAction.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_TYPE_HIERARCHY);
    thAction.setId(DartEditorActionDefinitionIds.OPEN_TYPE_HIERARCHY);
    // TODO(scheglov)
//    part.setAction("OpenTypeHierarchy", fOpenTypeHierarchy); //$NON-NLS-1$
  }
}
