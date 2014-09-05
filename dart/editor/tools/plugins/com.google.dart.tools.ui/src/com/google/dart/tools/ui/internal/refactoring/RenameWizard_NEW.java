package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.ui.internal.util.GridLayoutFactory;
import com.google.dart.tools.ui.internal.util.RowLayouter;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameWizard_NEW extends ServerRefactoringWizard {

  private class RenameInputPage extends TextInputWizardPage {
    public RenameInputPage() {
      super(null, true, refactoring.getOldName());
    }

    @Override
    public void createControl(Composite parent) {
      Composite result = new Composite(parent, SWT.NONE);
      GridLayoutFactory.create(result).columns(2).spacingVertical(8);
      setControl(result);

      RowLayouter layouter = new RowLayouter(2);

      Label label = new Label(result, SWT.NONE);
      label.setText("New &name:");

      Text text = createTextInputField(result);
      text.selectAll();
      text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      layouter.perform(label, text, 1);

      Dialog.applyDialogFont(result);
    }

    @Override
    protected boolean isEmptyInputValid() {
      return true;
    }

    @Override
    protected void textModified(String text) {
      refactoring.setNewName(text);
      super.textModified(text);
    }

    @Override
    protected RefactoringStatus validateTextField(String text) {
      return refactoring.setNewName(text);
    }
  }

  static final String DIALOG_SETTING_SECTION = "RenameWizard";
  private final ServerRenameRefactoring refactoring;

  public RenameWizard_NEW(ServerRenameRefactoring refactoring) {
    super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
    this.refactoring = refactoring;
    setDefaultPageTitle("Rename");
  }

  @Override
  protected void addUserInputPages() {
    setDefaultPageTitle("Rename " + refactoring.getElementKindName());
    addPage(new RenameInputPage());
  }
}
