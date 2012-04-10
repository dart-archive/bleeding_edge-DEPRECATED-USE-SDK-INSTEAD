package com.google.dart.tools.ui.internal.refactoring.reorg;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.ltk.core.refactoring.Refactoring;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameTypeWizard extends RenameRefactoringWizard {

  public RenameTypeWizard(Refactoring refactoring) {
    super(
        refactoring,
        RefactoringMessages.RenameTypeWizard_defaultPageTitle,
        RefactoringMessages.RenameTypeWizardInputPage_description,
        DartPluginImages.DESC_WIZBAN_REFACTOR_TYPE,
        DartHelpContextIds.RENAME_TYPE_WIZARD_PAGE);
  }
}
