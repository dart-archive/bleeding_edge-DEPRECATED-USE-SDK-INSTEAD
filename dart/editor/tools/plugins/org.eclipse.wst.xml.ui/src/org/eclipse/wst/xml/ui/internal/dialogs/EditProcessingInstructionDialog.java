/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.util.XMLCommonUIContextIds;
import org.w3c.dom.ProcessingInstruction;

public class EditProcessingInstructionDialog extends Dialog {
  protected String data;
  protected Text dataField;
  protected String target;
  protected Text targetField;

  public EditProcessingInstructionDialog(Shell parentShell, ProcessingInstruction pi) {
    this(parentShell, pi.getTarget(), pi.getData());
  }

  public EditProcessingInstructionDialog(Shell parentShell, String target, String data) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.target = target;
    this.data = data;
  }

  protected void buttonPressed(int buttonId) {
    target = getModelValue(targetField.getText());
    data = getModelValue(dataField.getText());
    super.buttonPressed(buttonId);
  }

  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  protected Control createDialogArea(Composite parent) {
    Composite dialogArea1 = (Composite) super.createDialogArea(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(dialogArea1,
        XMLCommonUIContextIds.XCUI_PROCESSING_DIALOG);

    Composite composite = new Composite(dialogArea1, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 250;

    Label targetLabel = new Label(composite, SWT.NONE);
    targetLabel.setText(XMLUIMessages._UI_LABEL_TARGET_COLON);

    targetField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    targetField.setLayoutData(gd);
    targetField.setText(getDisplayValue(target));

    Label dataLabel = new Label(composite, SWT.NONE);
    dataLabel.setText(XMLUIMessages._UI_LABEL_DATA_COLON);

    dataField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    dataField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    dataField.setText(getDisplayValue(data));

    return dialogArea1;
  }

  protected Label createMessageArea(Composite composite) {
    Label label = new Label(composite, SWT.NONE);
    // label.setText(message);
    return label;
  }

  public String getData() {
    return data;
  }

  protected String getDisplayValue(String string) {
    return string != null ? string : ""; //$NON-NLS-1$
  }

  protected String getModelValue(String string) {
    String result = null;
    if ((string != null) && (string.trim().length() > 0)) {
      result = string;
    }
    return result;
  }

  public String getTarget() {
    return target;
  }
}
