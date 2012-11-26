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
import com.google.dart.tools.ui.internal.projects.CreateFileWizard;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * Action that opens the new file wizard.
 */
public class OpenNewFileWizardAction extends AbstractOpenWizardAction implements IWorkbenchAction,
    ISelectionListener, ISelectionChangedListener {

  private static final String ACTION_ID = "com.google.dart.tools.ui.file.new";

  /**
   * Creates an instance of the <code>OpenNewFileWizardAction</code>.
   */
  public OpenNewFileWizardAction(IWorkbenchWindow window) {
    setText(ActionMessages.OpenNewFileWizardAction_text);
    setDescription(ActionMessages.OpenNewFileWizardAction_description);
    setToolTipText(ActionMessages.OpenNewFileWizardAction_tooltip);
    setImageDescriptor(DartPluginImages.DESC_TOOL_NEW_FILE);
    setActionDefinitionId(ACTION_ID);
    setId(ACTION_ID); //$NON-NLS-N$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.OPEN_ACTION);
    window.getSelectionService().addSelectionListener(this);
  }

  @Override
  public void dispose() {

  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {

  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {

  }

  @Override
  protected final INewWizard createWizard() {
    return new CreateFileWizard();
  }

}
