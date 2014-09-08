/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class InlineMethodInputPage_NEW extends UserInputWizardPage {

  public static final String PAGE_NAME = "InlineMethodInputPage";//$NON-NLS-1$
  private static final String DESCRIPTION = RefactoringMessages.InlineMethodInputPage_description;

  private ServerInlineMethodRefactoring fRefactoring;
  private Button fRemove;

  public InlineMethodInputPage_NEW() {
    super(PAGE_NAME);
    setImageDescriptor(DartPluginImages.DESC_WIZBAN_REFACTOR_CU);
    setDescription(DESCRIPTION);
  }

  @Override
  public void createControl(Composite parent) {
    initializeDialogUnits(parent);
    fRefactoring = (ServerInlineMethodRefactoring) getRefactoring();

    Composite result = new Composite(parent, SWT.NONE);
    setControl(result);
    GridLayout layout = new GridLayout();
    result.setLayout(layout);
    GridData gd = null;

    boolean isDeclaration = fRefactoring.isDeclaration();
    Label label = new Label(result, SWT.NONE);
    String methodLabel = fRefactoring.getFullName();
    label.setText(Messages.format(
        RefactoringMessages.InlineMethodInputPage_inline_method,
        methodLabel));
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Composite separator = new Composite(result, SWT.NONE);
    separator.setLayoutData(new GridData(0, 0));

    Button radioAll = new Button(result, SWT.RADIO);
    radioAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    radioAll.setText(RefactoringMessages.InlineMethodInputPage_all_invocations);
    radioAll.setSelection(isDeclaration);
    radioAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        if (((Button) event.widget).getSelection()) {
          fRemove.setEnabled(true);
          fRefactoring.setInlineAll(true);
        }
      }
    });
    if (isDeclaration) {
      fRefactoring.setInlineAll(true);
      fRefactoring.setDeleteSource(true);
    }

    fRemove = new Button(result, SWT.CHECK);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalIndent = convertWidthInCharsToPixels(3);
    fRemove.setLayoutData(gd);
    fRemove.setText(RefactoringMessages.InlineMethodInputPage_delete_declaration);
    fRemove.setSelection(isDeclaration);
    fRemove.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fRefactoring.setDeleteSource(((Button) e.widget).getSelection());
      }
    });

    Button radioSelected = new Button(result, SWT.RADIO);
    radioSelected.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    radioSelected.setText(RefactoringMessages.InlineMethodInputPage_only_selected);
    radioSelected.setSelection(!isDeclaration);
    if (isDeclaration) {
      radioSelected.setEnabled(false);
      radioAll.setFocus();
    } else {
      radioSelected.setFocus();
    }
    radioSelected.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        if (((Button) event.widget).getSelection()) {
          fRemove.setEnabled(false);
          fRefactoring.setInlineAll(false);
        }
      }
    });

    Dialog.applyDialogFont(result);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        getControl(),
        DartHelpContextIds.INLINE_METHOD_WIZARD_PAGE);
  }
}
