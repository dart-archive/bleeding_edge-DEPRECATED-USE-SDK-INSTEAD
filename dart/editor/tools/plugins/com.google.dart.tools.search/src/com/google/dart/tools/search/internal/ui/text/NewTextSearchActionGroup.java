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
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.ui.IContextMenuConstants;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenFileAction;

/**
 * Action group that adds the Text search actions to a context menu and the global menu bar.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class NewTextSearchActionGroup extends ActionGroup {

  private OpenFileAction fOpenAction;

  public NewTextSearchActionGroup(IViewPart part) {
    Assert.isNotNull(part);
    IWorkbenchPartSite site = part.getSite();
    fOpenAction = new OpenFileAction(site.getPage());

  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    ISelection selection = getContext().getSelection();
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection ss = (IStructuredSelection) selection;
      if (!ss.isEmpty()) {
        addOpenMenu(menu, ss);
      }
    }

  }

  private void addOpenMenu(IMenuManager menu, IStructuredSelection selection) {
    if (selection == null) {
      return;
    }

    fOpenAction.selectionChanged(selection);
    if (fOpenAction.isEnabled()) {
      menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpenAction);
    }
  }

}
