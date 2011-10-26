/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;
import com.google.dart.tools.ui.wizard.NewFileWizard;
import com.google.dart.tools.ui.wizard.NewFileWizardPage;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * Action that opens the new file wizard.
 * 
 * @see NewFileWizard
 * @see NewFileWizardPage
 */
public class OpenNewFileWizardAction extends AbstractOpenWizardAction implements IWorkbenchAction {

  private static final String ACTION_ID = "com.google.dart.tools.ui.file.new";

  /**
   * Creates an instance of the <code>OpenNewFileWizardAction</code>.
   */
  public OpenNewFileWizardAction() {
    setText(ActionMessages.OpenNewFileWizardAction_text);
    setDescription(ActionMessages.OpenNewFileWizardAction_description);
    setToolTipText(ActionMessages.OpenNewFileWizardAction_tooltip);
    //TODO (pquitslund) add an image
    setId(ACTION_ID); //$NON-NLS-N$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_ACTION);
  }

  @Override
  public void dispose() {

  }

  @Override
  protected final INewWizard createWizard() throws CoreException {
    return new NewFileWizard();
  }

}
