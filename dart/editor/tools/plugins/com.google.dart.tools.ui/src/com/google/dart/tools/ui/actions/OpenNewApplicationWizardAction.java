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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.wizard.NewApplicationWizard;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * Action that opens the new application wizard.
 * 
 * @see NewApplicationWizard
 */
public class OpenNewApplicationWizardAction extends AbstractOpenWizardAction implements
    IWorkbenchAction {

  private static final String ACTION_ID = "com.google.dart.tools.ui.app.new";

  /**
   * Creates an instance of the <code>OpenNewApplicationWizardAction</code>.
   */
  public OpenNewApplicationWizardAction() {
    setText(ActionMessages.OpenNewApplication2WizardAction_text);
    setDescription(ActionMessages.OpenNewApplication2WizardAction_description);
    setToolTipText(ActionMessages.OpenNewApplication2WizardAction_tooltip);
    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/library_new.png"));
    setId(ACTION_ID);
  }

  @Override
  public void dispose() {

  }

  @Override
  protected final INewWizard createWizard() throws CoreException {
    return new NewApplicationWizard();
  }

}
