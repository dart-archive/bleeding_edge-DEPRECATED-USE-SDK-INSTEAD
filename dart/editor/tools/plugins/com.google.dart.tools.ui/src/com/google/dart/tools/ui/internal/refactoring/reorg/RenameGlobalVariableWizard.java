package com.google.dart.tools.ui.internal.refactoring.reorg;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.ltk.core.refactoring.Refactoring;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameGlobalVariableWizard extends RenameRefactoringWizard {

  public RenameGlobalVariableWizard(Refactoring refactoring) {
    super(
        refactoring,
        RefactoringMessages.RenameGlobalVariableWizard_defaultPageTitle,
        RefactoringMessages.RenameGlobalVariableWizardInputPage_description,
        DartPluginImages.DESC_WIZBAN_REFACTOR_TYPE,
        DartHelpContextIds.RENAME_LOCAL_VARIABLE_WIZARD_PAGE);
  }
}
