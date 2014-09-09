/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.engine.services.refactoring.Parameter;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.dialogs.TextFieldNavigationHandler;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ParameterEditDialog_OLD extends StatusDialog {

  private final Parameter fParameter;
  private final boolean fEditType;
  private final boolean fEditDefault;
  private Text fType;
  private Text fName;
  private Text fDefaultValue;

  /**
   * @param parentShell
   * @param parameter
   * @param canEditType
   * @param canEditDefault
   * @param context the <code>IPackageFragment</code> for type ContentAssist. Can be
   *          <code>null</code> if <code>canEditType</code> is <code>false</code>.
   */
  public ParameterEditDialog_OLD(Shell parentShell, Parameter parameter, boolean canEditType,
      boolean canEditDefault) {
    super(parentShell);
    fParameter = parameter;
    fEditType = canEditType;
    fEditDefault = canEditDefault;
//    fContext = context;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(RefactoringMessages.ParameterEditDialog_title);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite result = (Composite) super.createDialogArea(parent);
    GridLayout layout = (GridLayout) result.getLayout();
    layout.numColumns = 2;
    Label label;
    GridData gd;

    label = new Label(result, SWT.NONE);
    String newName = fParameter.getNewName();
    if (newName.length() == 0) {
      label.setText(RefactoringMessages.ParameterEditDialog_message_new);
    } else {
      label.setText(Messages.format(RefactoringMessages.ParameterEditDialog_message, newName));
    }
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    label.setLayoutData(gd);

    if (fEditType) {
      label = new Label(result, SWT.NONE);
      label.setText(RefactoringMessages.ParameterEditDialog_type);
      fType = new Text(result, SWT.BORDER);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      fType.setLayoutData(gd);
      fType.setText(fParameter.getNewTypeName());
      fType.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
          validate((Text) e.widget);
        }
      });
      TextFieldNavigationHandler.install(fType);
      // TODO(scheglov) would be nice to have
//      DartTypeCompletionProcessor processor = new DartTypeCompletionProcessor(true, false);
//      processor.setCompletionContext(
//          fContext.getCuHandle(),
//          fContext.getBeforeString(),
//          fContext.getAfterString());
//      ControlContentAssistHelper.createTextContentAssistant(fType, processor);
    }

    label = new Label(result, SWT.NONE);
    fName = new Text(result, SWT.BORDER);
    initializeDialogUnits(fName);
    label.setText(RefactoringMessages.ParameterEditDialog_name);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = convertWidthInCharsToPixels(45);
    fName.setLayoutData(gd);
    fName.setText(newName);
    fName.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        validate((Text) e.widget);
      }
    });
    TextFieldNavigationHandler.install(fName);

    if (fEditDefault && fParameter.isAdded()) {
      label = new Label(result, SWT.NONE);
      label.setText(RefactoringMessages.ParameterEditDialog_defaultValue);
      fDefaultValue = new Text(result, SWT.BORDER);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      fDefaultValue.setLayoutData(gd);
      fDefaultValue.setText(fParameter.getDefaultValue());
      fDefaultValue.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
          validate((Text) e.widget);
        }
      });
      TextFieldNavigationHandler.install(fDefaultValue);
    }
    applyDialogFont(result);
    return result;
  }

  @Override
  protected void okPressed() {
    if (fType != null) {
      fParameter.setNewTypeName(fType.getText());
    }
    fParameter.setNewName(fName.getText());
    if (fDefaultValue != null) {
      fParameter.setDefaultValue(fDefaultValue.getText());
    }
    super.okPressed();
  }

  private Status createErrorStatus(String message) {
    return new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(), IStatus.ERROR, message, null);
  }

  private Status createWarningStatus(String message) {
    return new Status(
        IStatus.WARNING,
        DartToolsPlugin.getPluginId(),
        IStatus.WARNING,
        message,
        null);
  }

  private void validate(Text first) {
    IStatus[] result = new IStatus[3];
    if (first == fType) {
      result[0] = validateType();
      result[1] = validateName();
      result[2] = validateDefaultValue();
    } else if (first == fName) {
      result[0] = validateName();
      result[1] = validateType();
      result[2] = validateDefaultValue();
    } else {
      result[0] = validateDefaultValue();
      result[1] = validateName();
      result[2] = validateType();
    }
    for (int i = 0; i < result.length; i++) {
      IStatus status = result[i];
      if (status != null && !status.isOK()) {
        updateStatus(status);
        return;
      }
    }
    updateStatus(Status.OK_STATUS);
  }

  private IStatus validateDefaultValue() {
    // TODO(scheglov) we don't need default values yet
    return null;
//    if (fDefaultValue == null) {
//      return null;
//    }
//    String defaultValue = fDefaultValue.getText();
//    if (defaultValue.length() == 0) {
//      return createErrorStatus(RefactoringMessages.ParameterEditDialog_defaultValue_error);
//    }
//    if (ChangeSignatureProcessor.isValidExpression(defaultValue)) {
//      return Status.OK_STATUS;
//    }
//    String msg = Messages.format(
//        RefactoringMessages.ParameterEditDialog_defaultValue_invalid,
//        new String[] {defaultValue});
//    return createErrorStatus(msg);

  }

  private IStatus validateName() {
    if (fName == null) {
      return null;
    }
    String text = fName.getText();
    if (text.length() == 0) {
      return createErrorStatus(RefactoringMessages.ParameterEditDialog_name_error);
    }
    IStatus status = DartConventions.validateFieldName(text);
    if (status.matches(IStatus.ERROR)) {
      return status;
    }
    if (!Checks.startsWithLowerCase(text)) {
      return createWarningStatus(RefactoringCoreMessages.ExtractTempRefactoring_convention);
    }
    return Status.OK_STATUS;
  }

  private IStatus validateType() {
    if (fType == null) {
      return null;
    }
    // TODO(scheglov)
    return Status.OK_STATUS;
//      String type = fType.getText();
//    RefactoringStatus status = TypeContextChecker.checkParameterTypeSyntax(
//        type,
//        fContext.getCuHandle());
//    if (status == null || status.isOK()) {
//      return Status.OK_STATUS;
//    }
//    if (status.hasError()) {
//      return createErrorStatus(status.getEntryWithHighestSeverity().getMessage());
//    } else {
//      return createWarningStatus(status.getEntryWithHighestSeverity().getMessage());
//    }
  }
}
