/*
 * Copyright 2012 Google Inc.
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

package com.google.dart.tools.ui.internal.projects;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractOpenWizardAction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * Open a wizard to create a new application or import an existing application.
 */
public class OpenNewApplicationWizardAction extends AbstractOpenWizardAction implements
    IWorkbenchAction {

  private static final String ACTION_ID = "com.google.dart.tools.ui.project.new"; //$NON-NLS-1$

  public OpenNewApplicationWizardAction() {
    setText(ProjectMessages.OpenNewApplicationWizardAction_text);
    setDescription(ProjectMessages.OpenNewApplicationWizardAction_desc);
    setToolTipText(ProjectMessages.OpenNewApplicationWizardAction_tooltip);
    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/package_obj_new.png")); //$NON-NLS-1$
    setId(ACTION_ID);
  }

  @Override
  public void dispose() {

  }

  @Override
  protected final INewWizard createWizard() throws CoreException {
    return new CreateApplicationWizard();
  }

}
