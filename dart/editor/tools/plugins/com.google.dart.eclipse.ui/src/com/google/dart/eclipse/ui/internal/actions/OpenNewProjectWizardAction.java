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
package com.google.dart.eclipse.ui.internal.actions;

import com.google.dart.eclipse.wizards.DartProjectWizard;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractOpenWizardAction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.intro.IIntroPart;

/**
 * Open a wizard to create a new Dart project.
 */
public class OpenNewProjectWizardAction extends AbstractOpenWizardAction implements
    IWorkbenchAction {

  private static final String ACTION_ID = "com.google.dart.tools.ui.project.new"; //$NON-NLS-1$

  public OpenNewProjectWizardAction() {
    setText("New project"); //$NON-NLS-1$
    setDescription("Create a new Dart project"); //$NON-NLS-1$
    setToolTipText("Create a new Dart project"); //$NON-NLS-1$
    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/newprj_wiz.gif")); //$NON-NLS-1$
    setId(ACTION_ID);
  }

  @Override
  public void dispose() {

  }

  @Override
  public void run() {

    closeIntroPage();

    super.run();
  }

  @Override
  protected final INewWizard createWizard() throws CoreException {
    return new DartProjectWizard();
  }

  /**
   * 
   */
  private void closeIntroPage() {
    final IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
    PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
  }

}
