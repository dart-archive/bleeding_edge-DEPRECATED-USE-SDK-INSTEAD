/*
 * Copyright (c) 2015, the Dart project authors.
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

package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.ui.internal.refactoring.ServerRefactoring.ServerRefactoringListener;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.widgets.Display;

/**
 * A helper to track status of response for {@link ServerRefactoring} inputs.
 */
public class WizardPageOptionsHelper implements ServerRefactoringListener {
  private final UserInputWizardPage page;

  public boolean hasPendingRequests = false;
  public RefactoringStatus optionsStatus = new RefactoringStatus();

  public WizardPageOptionsHelper(ServerRefactoring refactoring, UserInputWizardPage page) {
    this.page = page;
    refactoring.setListener(this);
  }

  @Override
  public void requestStateChanged(boolean hasPendingRequests, RefactoringStatus optionsStatus) {
    if (optionsStatus == null) {
      optionsStatus = new RefactoringStatus();
    }
    this.hasPendingRequests = hasPendingRequests;
    this.optionsStatus = optionsStatus;
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        runInUI();
      }
    });
  }

  private void runInUI() {
    IWizard wizard = page.getWizard();
    if (wizard != null) {
      IWizardContainer container = wizard.getContainer();
      if (container != null) {
        page.setPageComplete(optionsStatus);
        container.updateButtons();
      }
    }
  }
}
