package com.google.dart.tools.ui.internal.refactoring.reorg;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.ltk.core.refactoring.Refactoring;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public final class RenameLocalVariableWizard extends RenameRefactoringWizard {

  public RenameLocalVariableWizard(Refactoring refactoring) {
    super(refactoring, RefactoringMessages.RenameLocalVariableWizard_defaultPageTitle,
        RefactoringMessages.RenameTypeParameterWizard_inputPage_description,
        DartPluginImages.DESC_WIZBAN_REFACTOR, DartHelpContextIds.RENAME_LOCAL_VARIABLE_WIZARD_PAGE);
  }
}
