package com.google.dart.tools.ui.internal.refactoring.reorg;

import com.google.dart.tools.internal.corext.refactoring.tagging.INameUpdating;
import com.google.dart.tools.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import com.google.dart.tools.internal.corext.refactoring.tagging.ITextUpdating;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.TextInputWizardPage;
import com.google.dart.tools.ui.internal.util.RowLayouter;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.swt.SWT;
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
abstract class RenameInputWizardPage extends TextInputWizardPage {

  private final String fHelpContextID;

  /**
   * Creates a new text input page.
   * 
   * @param description the page description
   * @param contextHelpId the context help ID
   * @param isLastUserPage <code>true</code> if this page is the wizard's last user input page.
   *          Otherwise <code>false</code>.
   * @param initialValue the initial value
   */
  public RenameInputWizardPage(String description, String contextHelpId, boolean isLastUserPage,
      String initialValue) {
    super(description, isLastUserPage, initialValue);
    fHelpContextID = contextHelpId;
  }

  @Override
  public void createControl(Composite parent) {
    Composite superComposite = new Composite(parent, SWT.NONE);
    setControl(superComposite);
    initializeDialogUnits(superComposite);
    superComposite.setLayout(new GridLayout());
    Composite composite = new Composite(superComposite, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    layout.marginWidth = 0;

    composite.setLayout(layout);
    RowLayouter layouter = new RowLayouter(2);

    Label label = new Label(composite, SWT.NONE);
    label.setText(getLabelText());

    Text text = createTextInputField(composite);
    text.selectAll();
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = convertWidthInCharsToPixels(25);
    text.setLayoutData(gd);

    layouter.perform(label, text, 1);

    Label separator = new Label(composite, SWT.NONE);
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false);
    gridData.heightHint = 2;
    separator.setLayoutData(gridData);

    addAdditionalOptions(composite, layouter);
    updateForcePreview();

    Dialog.applyDialogFont(superComposite);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), fHelpContextID);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      INameUpdating nameUpdating = (INameUpdating) getRefactoring().getAdapter(INameUpdating.class);
      if (nameUpdating != null) {
        String newName = getNewName(nameUpdating);
        if (newName != null && newName.length() > 0 && !newName.equals(getInitialValue())) {
          Text textField = getTextField();
          textField.setText(newName);
          textField.setSelection(0, newName.length());
        }
      }
    }
    super.setVisible(visible);
  }

  /**
   * Clients can override this method to provide more UI elements. By default, does nothing
   * 
   * @param composite the parent composite
   * @param layouter the row layouter to use
   */
  protected void addAdditionalOptions(Composite composite, RowLayouter layouter) {
    // none by default
  }

  protected boolean getBooleanSetting(String key, boolean defaultValue) {
    String update = getRefactoringSettings().get(key);
    if (update != null) {
      return Boolean.valueOf(update).booleanValue();
    } else {
      return defaultValue;
    }
  }

  protected String getLabelText() {
    return RefactoringMessages.RenameInputWizardPage_new_name;
  }

  /**
   * Returns the new name for the Dart element or <code>null</code> if no new name is provided
   * 
   * @param nameUpdating the name updating
   * @return the new name or <code>null</code>
   */
  protected String getNewName(INameUpdating nameUpdating) {
    return nameUpdating.getNewElementName();
  }

  protected void saveBooleanSetting(String key, Button checkBox) {
    if (checkBox != null) {
      getRefactoringSettings().put(key, checkBox.getSelection());
    }
  }

  private void updateForcePreview() {
    boolean forcePreview = false;
    Refactoring refactoring = getRefactoring();
    ITextUpdating tu = (ITextUpdating) refactoring.getAdapter(ITextUpdating.class);
    IQualifiedNameUpdating qu = (IQualifiedNameUpdating) refactoring.getAdapter(IQualifiedNameUpdating.class);
    if (tu != null) {
      forcePreview = tu.getUpdateTextualMatches();
    }
    if (qu != null) {
      forcePreview |= qu.getUpdateQualifiedNames();
    }
    getRefactoringWizard().setForcePreviewReview(forcePreview);
  }

  private void updateQulifiedNameUpdating(final IQualifiedNameUpdating ref, boolean enabled) {
//    fQualifiedNameComponent.setEnabled(enabled);
    ref.setUpdateQualifiedNames(enabled);
    updateForcePreview();
  }
}
