package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class InlineLocalWizard_OLD extends ServiceRefactoringWizard {

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
      ServiceInlineLocalRefactoring refactoring = (ServiceInlineLocalRefactoring) getRefactoring();
      int occurrences = refactoring.getReferenceCount();
      final String name = refactoring.getVariableName();
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

  public InlineLocalWizard_OLD(ServiceInlineLocalRefactoring ref) {
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
