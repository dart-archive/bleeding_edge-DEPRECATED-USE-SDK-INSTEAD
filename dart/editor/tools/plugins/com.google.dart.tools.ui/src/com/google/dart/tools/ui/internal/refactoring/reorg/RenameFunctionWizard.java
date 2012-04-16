package com.google.dart.tools.ui.internal.refactoring.reorg;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.ltk.core.refactoring.Refactoring;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameFunctionWizard extends RenameRefactoringWizard {

  public RenameFunctionWizard(Refactoring refactoring) {
    super(
        refactoring,
        RefactoringMessages.RenameFunctionWizard_defaultPageTitle,
        RefactoringMessages.RenameFunctionWizardInputPage_description,
        DartPluginImages.DESC_WIZBAN_REFACTOR_TYPE,
        DartHelpContextIds.RENAME_TYPE_WIZARD_PAGE);
  }
}
