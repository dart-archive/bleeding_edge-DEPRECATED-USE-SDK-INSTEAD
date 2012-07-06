package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.internal.corext.refactoring.code.InlineLocalRefactoring;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class InlineLocalWizard extends RefactoringWizard {

  private static class InlineLocalInputPage extends MessageWizardPage {

    public static final String PAGE_NAME = "InlineTempInputPage"; //$NON-NLS-1$

    public InlineLocalInputPage() {
      super(PAGE_NAME, true, MessageWizardPage.STYLE_QUESTION);
    }

    @Override
    public void createControl(Composite parent) {
      super.createControl(parent);
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          getControl(),
          DartHelpContextIds.INLINE_TEMP_WIZARD_PAGE);
    }

    @Override
    protected String getMessageString() {
      InlineLocalRefactoring refactoring = (InlineLocalRefactoring) getRefactoring();
      int occurrences = refactoring.getReferences().size();
      final String name = refactoring.getVariableElement().getName();
      switch (occurrences) {
        case 0:
          return Messages.format(RefactoringMessages.InlineLocalInputPage_message_zero, name);

        case 1:
          return Messages.format(RefactoringMessages.InlineLocalInputPage_message_one, name);

        default:
          return Messages.format(
              RefactoringMessages.InlineLocalInputPage_message_multi,
              new Object[] {new Integer(occurrences), name});
      }
    }
  }

  public InlineLocalWizard(InlineLocalRefactoring ref) {
    super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE
        | NO_BACK_BUTTON_ON_STATUS_DIALOG);
    setDefaultPageTitle(RefactoringMessages.InlineLocalWizard_defaultPageTitle);
  }

  @Override
  public int getMessageLineWidthInChars() {
    return 0;
  }

  @Override
  protected void addUserInputPages() {
    addPage(new InlineLocalInputPage());
  }
}
