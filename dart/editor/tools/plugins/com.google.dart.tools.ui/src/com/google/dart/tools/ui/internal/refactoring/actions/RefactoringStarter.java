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
package com.google.dart.tools.ui.internal.refactoring.actions;

import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;

/**
 * A helper class to activate the UI of a refactoring
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RefactoringStarter {

  private RefactoringStatus fStatus;

  public boolean activate(RefactoringWizard wizard, Shell parent, String dialogTitle, int saveMode) {
    RefactoringSaveHelper saveHelper = new RefactoringSaveHelper(saveMode);
    if (!canActivate(saveHelper, parent)) {
      return false;
    }

    try {
      RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
      int result = op.run(parent, dialogTitle);
      fStatus = op.getInitialConditionCheckingStatus();
      if (result == IDialogConstants.CANCEL_ID
          || result == RefactoringWizardOpenOperation.INITIAL_CONDITION_CHECKING_FAILED) {
        saveHelper.triggerIncrementalBuild();
        return false;
      } else {
        return true;
      }
    } catch (InterruptedException e) {
      return false; // User action got cancelled
    }
  }

  public RefactoringStatus getInitialConditionCheckingStatus() {
    return fStatus;
  }

  private boolean canActivate(RefactoringSaveHelper saveHelper, Shell shell) {
    return saveHelper.saveEditors(shell);
  }
}
