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
package com.google.dart.tools.ui.internal.actions;

import com.google.dart.tools.ui.actions.SelectionDispatchAction;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.tasklist.TaskPropertiesDialog;

public class AddTaskAction extends SelectionDispatchAction {

  public AddTaskAction(IWorkbenchSite site) {
    super(site);
    setEnabled(false);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_TASK_ACTION);
  }

  @Override
  public void run(IStructuredSelection selection) {
    IResource resource = getElement(selection);
    if (resource == null) {
      return;
    }

    TaskPropertiesDialog dialog = new TaskPropertiesDialog(getShell());
    dialog.setResource(resource);
    dialog.open();
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(getElement(selection) != null);
  }

  private IResource getElement(IStructuredSelection selection) {
    if (selection.size() != 1) {
      return null;
    }

    Object element = selection.getFirstElement();
    if (!(element instanceof IAdaptable)) {
      return null;
    }
    return (IResource) ((IAdaptable) element).getAdapter(IResource.class);
  }
}
