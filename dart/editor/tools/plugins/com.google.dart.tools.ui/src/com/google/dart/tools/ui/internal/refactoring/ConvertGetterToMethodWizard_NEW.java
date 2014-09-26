package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.ui.DartToolsPlugin;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ConvertGetterToMethodWizard_NEW extends ServerRefactoringWizard {

  static final String DIALOG_SETTING_SECTION = "ConvertGetterToMethodWizard"; //$NON-NLS-1$

  public ConvertGetterToMethodWizard_NEW(ServerConvertGetterToMethodRefactoring ref) {
    super(ref, DIALOG_BASED_USER_INTERFACE);
    setDefaultPageTitle(RefactoringMessages.ConvertGetterToMethodWizard_page_title);
    setDialogSettings(DartToolsPlugin.getDefault().getDialogSettings());
  }

  @Override
  protected void addUserInputPages() {
    // no input page
  }
}
