/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring David Schneider,
 * david.schneider@unisys.com - [142500] WTP properties pages fonts don't follow Eclipse preferences
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.dialogs;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.util.XMLCommonUIContextIds;
import org.w3c.dom.DocumentType;

public class EditDoctypeDialog extends Dialog {
  protected boolean computeSystemId;
  protected String[] doctypeData;
  protected boolean errorChecking;
  protected Label errorMessageLabel;
  protected Button okButton;
  protected Button publicIdBrowseButton;
  protected Text publicIdField;
  protected IPath resourceLocation;
  protected Text rootElementNameField;
  protected Button systemIdBrowseButton;
  protected Text systemIdField;

  public EditDoctypeDialog(Shell parentShell, DocumentType doctype) {
    this(parentShell, doctype.getName(), doctype.getPublicId(), doctype.getSystemId());
  }

  public EditDoctypeDialog(Shell parentShell, String name, String publicId, String systemId) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    doctypeData = new String[3];
    doctypeData[0] = name;
    doctypeData[1] = publicId;
    doctypeData[2] = systemId;
  }

  protected void buttonPressed(int buttonId) {
    doctypeData[0] = getModelValue(rootElementNameField.getText());
    doctypeData[1] = getModelValue(publicIdField.getText());
    doctypeData[2] = getModelValue(systemIdField.getText());
    super.buttonPressed(buttonId);
  }

  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  protected Control createContents(Composite parent) {
    Control control = super.createContents(parent);
    updateErrorMessage();
    return control;
  }

  protected Control createDialogArea(Composite parent) {
    Composite dialogControl = (Composite) super.createDialogArea(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(dialogControl,
        XMLCommonUIContextIds.XCUI_DOCTYPE_DIALOG);

    Composite composite = new Composite(dialogControl, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    ModifyListener modifyListener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        if (e.widget == systemIdField) {
          computeSystemId = false;
        }
        updateErrorMessage();
      }
    };

    // row 1
    //
    Label rootElementNameLabel = new Label(composite, SWT.NONE);
    rootElementNameLabel.setText(XMLUIMessages._UI_LABEL_ROOT_ELEMENT_NAME_COLON);

    rootElementNameField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(rootElementNameField,
        XMLCommonUIContextIds.XCUI_DOCTYPE_ROOT);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 200;
    rootElementNameField.setLayoutData(gd);
    rootElementNameField.setText(getDisplayValue(doctypeData[0]));
    rootElementNameField.addModifyListener(modifyListener);

    Label placeHolder = new Label(composite, SWT.NONE);
    placeHolder.setLayoutData(new GridData());

    // row 2
    //
    Label publicIdLabel = new Label(composite, SWT.NONE);
    publicIdLabel.setText(XMLUIMessages._UI_LABEL_PUBLIC_ID_COLON);

    publicIdField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(publicIdField,
        XMLCommonUIContextIds.XCUI_DOCTYPE_PUBLIC);
    publicIdField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    publicIdField.setText(getDisplayValue(doctypeData[1]));

    SelectionListener selectionListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        EditEntityHelper helper = new EditEntityHelper();
        if (e.widget == publicIdBrowseButton) {
          helper.performBrowseForPublicId(getShell(), publicIdField, computeSystemId
              ? systemIdField : null);
        } else if (e.widget == systemIdBrowseButton) {
          helper.performBrowseForSystemId(getShell(), systemIdField, resourceLocation);
        }
      }
    };

    publicIdBrowseButton = new Button(composite, SWT.NONE);
    publicIdBrowseButton.setText(XMLUIMessages._UI_LABEL_BROWSE);
    publicIdBrowseButton.addSelectionListener(selectionListener);

    // row 3
    Label systemIdLabel = new Label(composite, SWT.NONE);
    systemIdLabel.setText(XMLUIMessages._UI_LABEL_SYSTEM_ID_COLON);

    systemIdField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(systemIdField,
        XMLCommonUIContextIds.XCUI_DOCTYPE_SYSTEM);
    systemIdField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    systemIdField.setText(getDisplayValue(doctypeData[2]));
    systemIdField.addModifyListener(modifyListener);

    systemIdBrowseButton = new Button(composite, SWT.NONE);
    systemIdBrowseButton.setText(XMLUIMessages._UI_LABEL_BROWSE_1);
    systemIdBrowseButton.addSelectionListener(selectionListener);

    // error message
    errorMessageLabel = new Label(dialogControl, SWT.NONE);
    errorMessageLabel.setText(""); //$NON-NLS-1$
    errorMessageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    Color color = new Color(errorMessageLabel.getDisplay(), 200, 0, 0);
    errorMessageLabel.setForeground(color);

    Dialog.applyDialogFont(parent);
    return dialogControl;
  }

  protected Label createMessageArea(Composite composite) {
    Label label = new Label(composite, SWT.NONE);
    // label.setText(message);
    return label;
  }

  protected String getDisplayValue(String string) {
    return string != null ? string : ""; //$NON-NLS-1$
  }

  public boolean getErrorChecking() {
    return errorChecking;
  }

  protected String getModelValue(String string) {
    String result = null;
    if ((string != null) && (string.trim().length() > 0)) {
      result = string;
    }
    return result;
  }

  public String getName() {
    return doctypeData[0];
  }

  public String getPublicId() {
    return doctypeData[1];
  }

  public String getSystemId() {
    return doctypeData[2];
  }

  public void setComputeSystemId(boolean computeSystemId) {
    this.computeSystemId = computeSystemId;
  }

  public void setErrorChecking(boolean errorChecking) {
    this.errorChecking = errorChecking;
  }

  public void setResourceLocation(IPath path) {
    resourceLocation = path;
  }

  public void updateErrorMessage() {
    if (errorChecking) {
      String errorMessage = null;
      if (getModelValue(systemIdField.getText()) == null) {
        errorMessage = XMLUIMessages._UI_WARNING_SYSTEM_ID_MUST_BE_SPECIFIED;
      } else if (getModelValue(rootElementNameField.getText()) == null) {
        errorMessage = XMLUIMessages._UI_WARNING_ROOT_ELEMENT_MUST_BE_SPECIFIED;
      }

      errorMessageLabel.setText(errorMessage != null ? errorMessage : ""); //$NON-NLS-1$
      okButton.setEnabled(errorMessage == null);
    }
  }
}
