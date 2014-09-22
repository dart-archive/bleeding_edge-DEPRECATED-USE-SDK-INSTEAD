package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.ui.DartToolsPlugin;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ConvertMethodToGetterWizard_NEW extends ServerRefactoringWizard {

  static final String DIALOG_SETTING_SECTION = "ConvertMethodToGetterWizard"; //$NON-NLS-1$

  public ConvertMethodToGetterWizard_NEW(ServerConvertMethodToGetterRefactoring ref) {
    super(ref, DIALOG_BASED_USER_INTERFACE);
    setDefaultPageTitle(RefactoringMessages.ConvertMethodToGetterWizard_page_title);
    setDialogSettings(DartToolsPlugin.getDefault().getDialogSettings());
  }

  @Override
  protected void addUserInputPages() {
    // no input page
  }
}
