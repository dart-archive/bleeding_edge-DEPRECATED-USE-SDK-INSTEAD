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

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchSite;

/**
 * Action group that with "Open..." actions
 */
public class OpenViewActionGroup extends AbstractDartSelectionActionGroup {
  private AbstractDartSelectionAction_OLD typeHierarchyAction;

  public OpenViewActionGroup(DartEditor editor) {
    super(editor);
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      typeHierarchyAction = new OpenTypeHierarchyAction_NEW(editor);
    } else {
      typeHierarchyAction = new OpenTypeHierarchyAction_OLD(editor);
    }
    initActions();
    addActions(typeHierarchyAction);
    addActionDartSelectionListeners();
  }

  public OpenViewActionGroup(IWorkbenchSite site) {
    super(site);
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      typeHierarchyAction = new OpenTypeHierarchyAction_NEW(site);
    } else {
      typeHierarchyAction = new OpenTypeHierarchyAction_OLD(site);
    }
    initActions();
    addActions(typeHierarchyAction);
    addActionSelectionListeners();
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
    // TODO(scheglov)
  }

  /**
   * Initializes definition attributes of actions.
   */
  private void initActions() {
    typeHierarchyAction.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_TYPE_HIERARCHY);
    typeHierarchyAction.setId(DartEditorActionDefinitionIds.OPEN_TYPE_HIERARCHY);
    // TODO(scheglov)
//    part.setAction("OpenTypeHierarchy", fOpenTypeHierarchy); //$NON-NLS-1$
  }
}
