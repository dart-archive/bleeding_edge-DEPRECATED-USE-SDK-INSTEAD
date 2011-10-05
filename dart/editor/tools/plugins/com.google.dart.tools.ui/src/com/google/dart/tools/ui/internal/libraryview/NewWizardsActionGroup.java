/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.libraryview;

import com.google.dart.tools.ui.IContextMenuConstants;
import com.google.dart.tools.ui.actions.OpenNewApplicationWizardAction;
import com.google.dart.tools.ui.actions.OpenNewFileWizardAction;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

/**
 * Action group that adds the new file and new application actions to a context menu.
 */

public class NewWizardsActionGroup extends ActionGroup {

  private OpenNewFileWizardAction openNewFileWizardAction;
  private OpenNewApplicationWizardAction openNewApplicationWizardAction;

  /**
   * Creates a new <code>NavigateActionGroup</code>. The group requires that the selection provided
   * by the part's selection provider is of type
   * {@link org.eclipse.jface.viewers.IStructuredSelection}.
   *
   * @param part the view part that owns this action group
   */
  public NewWizardsActionGroup(IViewPart part) {
    openNewFileWizardAction = new OpenNewFileWizardAction();
    openNewApplicationWizardAction = new OpenNewApplicationWizardAction();
  }

  /**
   * Creates a new <code>NavigateActionGroup</code>. The group requires that the selection provided
   * by the given selection provider is of type {@link IStructuredSelection}.
   *
   * @param site the site that will own the action group.
   * @param specialSelectionProvider the selection provider used instead of the sites selection
   *          provider.
   */
  public NewWizardsActionGroup(IWorkbenchPartSite site, ISelectionProvider specialSelectionProvider) {
    openNewFileWizardAction = new OpenNewFileWizardAction();
    openNewApplicationWizardAction = new OpenNewApplicationWizardAction();
  }

  @Override
  public void dispose() {
    super.dispose();

  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    super.fillActionBars(actionBars);

  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);

    ISelection currentSel = getContext().getSelection();

    if (currentSel == null || currentSel.isEmpty()) {
      menu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, openNewFileWizardAction);
      menu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, openNewApplicationWizardAction);
      menu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, new Separator());
    }
  }

  @Override
  public void setContext(ActionContext context) {
    super.setContext(context);

  }

  @Override
  public void updateActionBars() {
    super.updateActionBars();

  }

}
