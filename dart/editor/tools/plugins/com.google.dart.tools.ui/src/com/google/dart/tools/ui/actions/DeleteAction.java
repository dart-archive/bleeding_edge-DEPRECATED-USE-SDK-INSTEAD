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

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;

/**
 * Standard action for deleting the currently selected resources.
 */
public final class DeleteAction extends SelectionDispatchAction {

  /**
   * The id of this action.
   */
  public static final String ID = DartToolsPlugin.PLUGIN_ID + ".DeleteAction"; //$NON-NLS-1$

  public DeleteAction(IWorkbenchSite site) {
    super(site);
    setId(ID);
    setText(CCPMessages.DeleteAction_text);
    setDescription(CCPMessages.DeleteAction_description);
  }

  @Override
  public void run(IStructuredSelection selection) {
    createWorkbenchAction(selection).run();
    return;
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(createWorkbenchAction(selection).isEnabled());
    return;
  }

  private IAction createWorkbenchAction(IStructuredSelection selection) {
    DeleteResourceAction action = new DeleteResourceAction(getSite());
    action.selectionChanged(selection);
    return action;
  }
}
