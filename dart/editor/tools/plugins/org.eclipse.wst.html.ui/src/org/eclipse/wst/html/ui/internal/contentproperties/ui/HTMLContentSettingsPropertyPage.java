/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentproperties.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.html.ui.internal.Logger;
import org.eclipse.wst.html.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.internal.contentproperties.IContentSettings;
import org.eclipse.wst.sse.ui.internal.contentproperties.ui.ComboListOnPropertyPage;
import org.eclipse.wst.sse.ui.internal.contentproperties.ui.ContentSettingsPropertyPage;

import java.util.Map;

/**
 * @deprecated Use WebContentSettingsPropertyPage instead
 */
public final class HTMLContentSettingsPropertyPage extends ContentSettingsPropertyPage implements
    org.eclipse.swt.events.SelectionListener {

  private final int N_DOCUMENT_TYPE = 0;
  private final int N_CSS_PROFILE = 1;
  private final int N_TARGET_DEVICE = 2;

  private Text publicIdText;
  private Text systemIdText;

  public HTMLContentSettingsPropertyPage() {
    super();
    numberOfCombo = 3;
    numCols = 2;
    numRows = 8;
    combo = new ComboListOnPropertyPage[super.numberOfCombo];

  }

  protected void createDocumentTypeComboBox() {

    // create description of implecit DOCTYPE	
    Label label = new Label(propertyPage, SWT.LEFT);
    label.setText(HTMLUIMessages.UI_Description_of_role_of_following_DOCTYPE);
    GridData data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.horizontalSpan = numCols;
    label.setLayoutData(data);

    // create combobox
    super.combo[N_DOCUMENT_TYPE] = super.createComboBoxOf(DOCUMENT_LABEL);
    super.combo[this.N_DOCUMENT_TYPE].addSelectionListener(this);
    // set entry list into Combo
    ContentSettingsRegistry.setHTMLDocumentTypeRegistryInto(combo[N_DOCUMENT_TYPE]);
    //	create TextField
    createIDTextField();
    if (combo[N_DOCUMENT_TYPE].getItemCount() <= 0)
      return;

    String initValue = contentSettings.getProperty((IResource) super.getElement(),
        IContentSettings.HTML_DOCUMENT_TYPE);
    // when either .contentsettings or element doesn't exist
    // when attribute doesn't exists,getProperty returns empty string.
    if (initValue == null)
      initValue = ""; //$NON-NLS-1$
    // set init selectionItem in Combo
    super.setSelectionItem(combo[N_DOCUMENT_TYPE], initValue);
    this.publicIdText.setText(initValue);
    if (!initValue.equals("")) {//$NON-NLS-1$
      // toro D210260
      if (ContentSettingsRegistry.getSystemIdFrom(initValue) != null)
        this.systemIdText.setText(ContentSettingsRegistry.getSystemIdFrom(initValue));
      else
        this.systemIdText.setText("");//$NON-NLS-1$
    } else
      this.systemIdText.setText("");//$NON-NLS-1$

    // create separator
    label = new Label(propertyPage, SWT.SEPARATOR | SWT.HORIZONTAL);
    data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.horizontalSpan = numCols;
    data.verticalSpan = 8;
    label.setLayoutData(data);

  }

  protected void createCSSComboBox() {
    super.combo[N_CSS_PROFILE] = super.createComboBoxOf(CSS_LABEL);

    ContentSettingsRegistry.setCSSMetaModelRegistryInto(combo[N_CSS_PROFILE]);
    if (combo[N_CSS_PROFILE].getItemCount() <= 0)
      return;
    String initValue = contentSettings.getProperty((IResource) super.getElement(),
        IContentSettings.CSS_PROFILE);
    // when either .contentsettings or element doesn't exist
    // when attribute doesn't exists,getProperty returns empty string.
    if (initValue == null)
      initValue = ""; //$NON-NLS-1$
    // set init selectionItem in Combo
    super.setSelectionItem(combo[N_CSS_PROFILE], initValue);
  }

  protected void createDeviceComboBox() {
    super.combo[N_TARGET_DEVICE] = super.createComboBoxOf(DEVICE_LABEL);

    ContentSettingsRegistry.setDeviceProfileRegistryInto(combo[N_TARGET_DEVICE]);
    if (combo[N_TARGET_DEVICE].getItemCount() <= 0)
      return;
    String initValue = contentSettings.getProperty((IResource) super.getElement(),
        IContentSettings.DEVICE_PROFILE);
    // when either .contentsettings or element doesn't exist
    // when attribute doesn't exists,getProperty returns empty string.
    if (initValue == null)
      initValue = ""; //$NON-NLS-1$
    // set init selectionItem in Combo
    super.setSelectionItem(combo[N_TARGET_DEVICE], initValue);
  }

  protected void createSettingsPageGUI() {
    int type = ((IResource) getElement()).getType();
    switch (type) {
      case IResource.FILE:
        //	composite = createComposite(propertyPage,numCols,numRows);
        createDocumentTypeComboBox();
        createCSSComboBox();
        createDeviceComboBox();
        computeMaxWidthHint();
        PlatformUI.getWorkbench().getHelpSystem().setHelp(propertyPage,
            IHelpContextIds.WEB_CONTENT_SETTINGS_HELPID);
        break;

      default:
        Logger.log(Logger.WARNING,
            "HTMLContentSettingsPropertyPage is instantiated by resource except FILE");//$NON-NLS-1$
        break;
    }

  }

  protected void putSelectedPropertyInto(Map properties, String valueInCombo, int index) {

    switch (index) {
      case N_DOCUMENT_TYPE:
        // doc type
        properties.put(IContentSettings.HTML_DOCUMENT_TYPE, valueInCombo);
        break;
      case N_CSS_PROFILE:
        // css
        properties.put(IContentSettings.CSS_PROFILE, valueInCombo);
        break;
      case N_TARGET_DEVICE:
        // device
        properties.put(IContentSettings.DEVICE_PROFILE, valueInCombo);
        break;
      default:
        Logger.log(Logger.ERROR,
            "Index is out of range in putSelectedPropertyInto() in class HTMLContentSettingsPropertyPage");//$NON-NLS-1$
        break;
    }

  }

  protected void deleteNoneProperty(int index) {
    switch (index) {
      case N_DOCUMENT_TYPE:
        // doc type
        contentSettings.deleteProperty((IResource) super.getElement(),
            IContentSettings.HTML_DOCUMENT_TYPE);
        break;
      case N_CSS_PROFILE:
        // css
        contentSettings.deleteProperty((IResource) super.getElement(), IContentSettings.CSS_PROFILE);
        break;
      case N_TARGET_DEVICE:
        // device
        contentSettings.deleteProperty((IResource) super.getElement(),
            IContentSettings.DEVICE_PROFILE);
        break;
      default:
        Logger.log(Logger.ERROR,
            "Index is out of range in deleteNoneProperty() in class HTMLContentSettingsPropertyPage");//$NON-NLS-1$
        break;
    }
  }

  private void createIDTextField() {
    // public ID & System ID
    Label publicLabel = new Label(super.propertyPage, SWT.NONE);
    GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
    data.horizontalIndent = 10;
    publicLabel.setLayoutData(data);
    publicLabel.setText(HTMLUIMessages.UI_Public_ID);
    publicIdText = new Text(super.propertyPage, SWT.BORDER | SWT.READ_ONLY);
    data = new GridData();

    publicIdText.setLayoutData(data);

    Label systemLabel = new Label(super.propertyPage, SWT.NONE);
    data = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
    data.horizontalIndent = 10;
    systemLabel.setLayoutData(data);
    systemLabel.setText(HTMLUIMessages.UI_System_ID);
    systemIdText = new Text(super.propertyPage, SWT.BORDER | SWT.READ_ONLY);
    data = new GridData();

    systemIdText.setLayoutData(data);
  }

  private void computeMaxWidthHint() {
    // maxLengthString was set when HTMLDocumentTypeEntry was set in class ContentSettingsRegistry.
    String maxLengthString = ContentSettingsRegistry.maxLengthStringInHTMLDocumentTypeRegistry;
    String backup = this.systemIdText.getText();
    this.systemIdText.setText(maxLengthString);
    int maxWidthHint = this.systemIdText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    this.systemIdText.setText(backup);

    if (this.combo[this.N_DOCUMENT_TYPE].getLayoutData() != null)
      ((GridData) this.combo[this.N_DOCUMENT_TYPE].getLayoutData()).widthHint = maxWidthHint;
    if (this.publicIdText.getLayoutData() != null)
      ((GridData) this.publicIdText.getLayoutData()).widthHint = maxWidthHint;
    if (this.systemIdText.getLayoutData() != null)
      ((GridData) this.systemIdText.getLayoutData()).widthHint = maxWidthHint;
    if (this.combo[this.N_CSS_PROFILE].getLayoutData() != null)
      ((GridData) this.combo[this.N_CSS_PROFILE].getLayoutData()).widthHint = maxWidthHint;
    if (this.combo[this.N_TARGET_DEVICE].getLayoutData() != null)
      ((GridData) this.combo[this.N_TARGET_DEVICE].getLayoutData()).widthHint = maxWidthHint;

  }

  public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
  }

  public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
    Widget source = event.widget;

    if (this.combo[this.N_DOCUMENT_TYPE].equals(source)) {
      ComboListOnPropertyPage combo = this.combo[this.N_DOCUMENT_TYPE];
      if (combo.getSelectionIndex() < 0)
        return;
      if (!combo.getSelectedValue().equals("")) {//$NON-NLS-1$
        this.publicIdText.setText(combo.getSelectedValue());
        if (ContentSettingsRegistry.getSystemIdFrom(combo.getSelectedValue()) != null)
          this.systemIdText.setText(ContentSettingsRegistry.getSystemIdFrom(combo.getSelectedValue()));
        else
          this.systemIdText.setText("");//$NON-NLS-1$
      } else {
        this.publicIdText.setText("");//$NON-NLS-1$
        this.systemIdText.setText(""); //$NON-NLS-1$
      }

    }
  }

  protected void performDefaults() {
    super.performDefaults();
    this.publicIdText.setText("");//$NON-NLS-1$
    this.systemIdText.setText(""); //$NON-NLS-1$

  }

}
