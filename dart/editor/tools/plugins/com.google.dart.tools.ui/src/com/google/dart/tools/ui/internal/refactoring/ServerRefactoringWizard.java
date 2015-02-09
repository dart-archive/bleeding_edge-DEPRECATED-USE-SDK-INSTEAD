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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public abstract class ServerRefactoringWizard extends RefactoringWizard {
  protected final ServerRefactoring refactoring;

  public ServerRefactoringWizard(ServerRefactoring refactoring, int flags) {
    super(refactoring, flags);
    this.refactoring = refactoring;
    setDialogSettings(DartToolsPlugin.getDefault().getDialogSettings());
  }

  @Override
  public boolean needsProgressMonitor() {
    setForcePreviewReview(refactoring.requiresPreview());
    return super.needsProgressMonitor();
  }

  @Override
  public boolean performFinish() {
    boolean valid = super.performFinish();
    // if not testing, proceed by default
    if (!ErrorDialog.AUTOMATED_MODE) {
      return valid;
    }
    // log error if not valid
    if (!valid) {
      RefactoringStatus status = null;
      try {
        status = ReflectionUtils.getFieldObject(this, "fConditionCheckingStatus");
      } catch (Throwable e) {
      }
      DartToolsPlugin.logErrorMessage("Refactoring operation failed: " + status);
    }
    // don't show error page/dialog
    return true;
  }
}
