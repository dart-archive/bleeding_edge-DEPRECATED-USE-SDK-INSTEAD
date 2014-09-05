package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.ui.internal.refactoring.contentassist.VariableNamesProcessor;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.util.ControlContentAssistHelper;
import com.google.dart.tools.ui.internal.util.RowLayouter;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ExtractLocalWizard_NEW extends ServerRefactoringWizard {

  private static class ExtractLocalInputPage extends TextInputWizardPage {
    private final boolean initialValid;
    private static final String DESCRIPTION = RefactoringMessages.ExtractLocalInputPage_enter_name;
    private final String[] names;

    public ExtractLocalInputPage(String[] names) {
      super(DESCRIPTION, true, names.length == 0 ? "" : names[0]); //$NON-NLS-1$
      Assert.isNotNull(names);
      this.names = names;
      initialValid = names.length > 0;
    }

    @Override
    public void createControl(Composite parent) {
      Composite result = new Composite(parent, SWT.NONE);
      setControl(result);
      GridLayout layout = new GridLayout();
      layout.numColumns = 2;
      layout.verticalSpacing = 8;
      result.setLayout(layout);
      RowLayouter layouter = new RowLayouter(2);

      Label label = new Label(result, SWT.NONE);
      label.setText(RefactoringMessages.ExtractLocalInputPage_variable_name);

      Text text = createTextInputField(result);
      text.selectAll();
      text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      ControlContentAssistHelper.createTextContentAssistant(text, new VariableNamesProcessor(names));

      layouter.perform(label, text, 1);

      addReplaceAllCheckbox(result, layouter);

      validateTextField(text.getText());

      Dialog.applyDialogFont(result);
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          getControl(),
          DartHelpContextIds.EXTRACT_LOCAL_WIZARD_PAGE);
    }

    @Override
    protected boolean isInitialInputValid() {
      return initialValid;
    }

    @Override
    protected void textModified(String text) {
      getExtractLocalRefactoring().setName(text);
      super.textModified(text);
    }

    @Override
    protected RefactoringStatus validateTextField(String text) {
      return getExtractLocalRefactoring().setName(text);
    }

    private void addReplaceAllCheckbox(Composite result, RowLayouter layouter) {
      final ServerExtractLocalRefactoring refactoring = getExtractLocalRefactoring();
      final Button checkBox = new Button(result, SWT.CHECK);
      layouter.perform(checkBox);
      checkBox.setText(RefactoringMessages.ExtractLocalInputPage_replace_all);
      checkBox.setEnabled(refactoring.hasSeveralOccurrences());
      if (refactoring.hasSeveralOccurrences()) {
        checkBox.setSelection(true);
        refactoring.setExtractAll(true);
      }
      checkBox.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          refactoring.setExtractAll(checkBox.getSelection());
        }
      });
    }

    private ServerExtractLocalRefactoring getExtractLocalRefactoring() {
      return (ServerExtractLocalRefactoring) getRefactoring();
    }
  }

  static final String DIALOG_SETTING_SECTION = "ExtractLocalWizard"; //$NON-NLS-1$

  public ExtractLocalWizard_NEW(ServerExtractLocalRefactoring ref) {
    super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
    setDefaultPageTitle(RefactoringMessages.ExtractLocalWizard_defaultPageTitle);
  }

  @Override
  protected void addUserInputPages() {
    addPage(new ExtractLocalInputPage(getExtractLocalRefactoring().getNames()));
  }

  private ServerExtractLocalRefactoring getExtractLocalRefactoring() {
    return (ServerExtractLocalRefactoring) getRefactoring();
  }
}
