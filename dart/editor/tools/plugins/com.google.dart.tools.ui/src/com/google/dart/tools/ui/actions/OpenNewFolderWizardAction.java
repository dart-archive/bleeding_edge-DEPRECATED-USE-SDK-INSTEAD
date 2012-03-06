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

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.projects.CreateFolderWizard;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * Action that opens the New Folder Wizard
 */
public class OpenNewFolderWizardAction extends AbstractOpenWizardAction implements
    IWorkbenchAction, ISelectionListener, ISelectionChangedListener {

  private static final String ACTION_ID = "com.google.dart.tools.ui.folder.new";

  public OpenNewFolderWizardAction(IWorkbenchWindow window) {
    setText(ActionMessages.OpenNewFolderWizardAction_text);
    setDescription(ActionMessages.OpenNewFolderWizardAction_description);
    setToolTipText(ActionMessages.OpenNewFolderWizardAction_tooltip);
    setImageDescriptor(DartPluginImages.DESC_TOOL_NEWPACKAGE);
    setId(ACTION_ID); //$NON-NLS-N$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.OPEN_ACTION);
    window.getSelectionService().addSelectionListener(this);

  }

  @Override
  public void dispose() {

  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    updateEnablement(selection);
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    ISelection selection = event.getSelection();
    updateEnablement(selection);
  }

  @Override
  protected INewWizard createWizard() throws CoreException {
    return new CreateFolderWizard();
  }

  private void updateEnablement(ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      if (structuredSelection.size() == 1) {
        if (structuredSelection.getFirstElement() instanceof IResource) {
          IResource element = (IResource) structuredSelection.getFirstElement();
          if (element.getType() == IResource.FOLDER || element.getType() == IResource.PROJECT) {
            setEnabled(true);
            return;
          }
        }
      }
    }
    setEnabled(false);
  }

}
