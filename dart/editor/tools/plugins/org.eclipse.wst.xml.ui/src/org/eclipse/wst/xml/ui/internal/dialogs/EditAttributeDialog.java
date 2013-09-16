/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.xml.core.internal.document.JSPTag;
import org.eclipse.wst.xml.core.internal.provisional.NameValidator;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;
import org.eclipse.wst.xml.ui.internal.util.XMLCommonUIContextIds;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

public class EditAttributeDialog extends Dialog implements ModifyListener {
  protected Attr attribute;
  protected String attributeName;
  protected Text attributeNameField;
  protected String attributeValue;
  protected Text attributeValueField;
  protected Label errorMessageIcon;
  protected Label errorMessageLabel;
  protected Button okButton;
  protected Element ownerElement;

  public EditAttributeDialog(Shell parentShell, Element ownerElement, Attr attribute) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.ownerElement = ownerElement;
    this.attribute = attribute;
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      attributeName = getModelValue(attributeNameField.getText());
      attributeValue = attributeValueField.getText();
    }
    super.buttonPressed(buttonId);
  }

  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  protected Control createContents(Composite parent) {
    Control control = super.createContents(parent);
    attributeNameField.forceFocus();
    attributeNameField.selectAll();
    updateErrorMessage();
    return control;
  }

  protected Control createDialogArea(Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(dialogArea,
        XMLCommonUIContextIds.XCUI_ATTRIBUTE_DIALOG);

    Composite composite = new Composite(dialogArea, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    composite.setLayout(layout);

    //
    // Style convenience constants
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    Label attributeNameLabel = new Label(composite, SWT.NONE);
    attributeNameLabel.setText(XMLUIMessages._UI_LABEL_NAME_COLON);

    attributeNameField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 300;
    attributeNameField.setLayoutData(gd);
    attributeNameField.setText(getDisplayValue(attribute != null ? attribute.getName() : "")); //$NON-NLS-1$
    attributeNameField.addModifyListener(this);

    Label attributeValueLabel = new Label(composite, SWT.NONE);
    attributeValueLabel.setText(XMLUIMessages._UI_LABEL_VALUE_COLON);

    String value = attribute != null ? attribute.getValue() : ""; //$NON-NLS-1$
    int style = SWT.SINGLE | SWT.BORDER;
    if (value.indexOf("\n") != -1) { //$NON-NLS-1$
      style = SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL;
    }

    attributeValueField = new Text(composite, style);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 300;
    attributeValueField.setLayoutData(gd);
    attributeValueField.setText(getDisplayValue(attribute != null ? attribute.getValue() : "")); //$NON-NLS-1$

    // Error message
    Composite message = new Composite(composite, SWT.NONE);
    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    message.setLayout(layout);

    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    message.setLayoutData(gd);

    errorMessageIcon = new Label(message, SWT.NONE);
    gd = new GridData();
    gd.horizontalSpan = 1;
    gd.verticalAlignment = SWT.TOP;
    errorMessageIcon.setLayoutData(gd);
    errorMessageIcon.setImage(null);

    errorMessageLabel = new Label(message, SWT.WRAP);
    errorMessageLabel.setText(XMLUIMessages.error_message_goes_here);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 200;
    gd.heightHint = Math.max(30, errorMessageLabel.computeSize(0, 0, false).y * 2);
    gd.horizontalSpan = 1;
    gd.verticalAlignment = SWT.TOP;
    errorMessageLabel.setLayoutData(gd);

    return dialogArea;
  }

  public String getAttributeName() {
    return attributeName;
  }

  public String getAttributeValue() {
    return attributeValue;
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

  public void modifyText(ModifyEvent e) {
    updateErrorMessage();
  }

  protected void updateErrorMessage() {
    String errorMessage = null;
    Image image = null;
    String name = attributeNameField.getText().trim();
    if (name.length() > 0) {
      Attr matchingAttribute = ownerElement.getAttributeNode(name);
      if ((matchingAttribute != null) && (matchingAttribute != attribute)) {
        errorMessage = XMLUIMessages._ERROR_XML_ATTRIBUTE_ALREADY_EXISTS;
        image = XMLEditorPluginImageHelper.getInstance().getImage(
            XMLEditorPluginImages.IMG_OBJ_ERROR_OBJ);
      } else {
        if (!isValidName(name)) {
          errorMessage = NLS.bind(XMLUIMessages._ERROR_XML_ATTRIBUTE_IS_INVALID, name);
          image = XMLEditorPluginImageHelper.getInstance().getImage(
              XMLEditorPluginImages.IMG_OBJ_ERROR_OBJ);
        }
      }
    } else {
      errorMessage = ""; //$NON-NLS-1$
    }

    errorMessageLabel.setText(errorMessage != null ? errorMessage : ""); //$NON-NLS-1$
    errorMessageIcon.setImage(image);
    errorMessageLabel.getParent().layout();
    okButton.setEnabled(errorMessage == null);
  }

  private boolean isValidName(String name) {
    if (NameValidator.isValid(name))
      return true;
    // special for JSP tag in tag name
    if (name.startsWith(JSPTag.TAG_OPEN))
      return true;
    return false;
  }
}
