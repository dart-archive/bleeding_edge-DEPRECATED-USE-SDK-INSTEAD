/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.preferences;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.sse.core.internal.encoding.CommonCharsetNames;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

/**
 * WorkbenchDefaultEncodingSettings is an extension of EncodingSettings. This composite contains
 * EncodingSettings for users to select the encoding they desire as well as a checkbox for users to
 * select to use the default workbench encoding instead.
 * 
 * @see org.eclipse.wst.xml.ui.internal.preferences.EncodingSettings
 */
public class WorkbenchDefaultEncodingSettings extends Composite {

  private final static int INDENT = 15;
  private static final String WORKBENCH_DEFAULT = ""; //$NON-NLS-1$
  private EncodingSettings fEncodingSettings;
  private String fNonDefaultIANA = null;
  private Button fUseDefaultButton;

  public WorkbenchDefaultEncodingSettings(Composite parent) {
    super(parent, SWT.NONE);
    createControls();
  }

  private void createControls() {
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginWidth = 0;
    setLayout(layout);
    GridData data = new GridData();
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    setLayoutData(data);

    Composite defaultEncodingComposite = new Composite(this, SWT.NONE);
    layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    data = new GridData(GridData.FILL_BOTH);
    defaultEncodingComposite.setLayout(layout);
    defaultEncodingComposite.setLayoutData(data);

    fUseDefaultButton = new Button(defaultEncodingComposite, SWT.CHECK);
    fUseDefaultButton.setText(XMLUIMessages.WorkbenchDefaultEncodingSettings_0);

    fUseDefaultButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        handleUseDefaultButtonSelected();
      }
    });

    fEncodingSettings = new EncodingSettings(this);
    ((GridLayout) fEncodingSettings.getLayout()).marginWidth = 0;
    ((GridData) fEncodingSettings.getLayoutData()).horizontalIndent = INDENT;

  }

  private Combo getEncodingCombo() {
    return fEncodingSettings.getEncodingCombo();
  }

  /**
   * <code>getIANATag</code> Get the IANA tag equivalent of the selected descriptive encoding name.
   * Returns empty string if using workbench encoding.
   * 
   * @return a <code>String</code> value
   */
  public String getIANATag() {
    if (!isDefault()) {
      return fEncodingSettings.getIANATag();
    }
    return WORKBENCH_DEFAULT;
  }

  private String getWorkbenchEncoding() {
    return ResourcesPlugin.getEncoding();
  }

  void handleUseDefaultButtonSelected() {
    if (fUseDefaultButton.getSelection()) {
      fNonDefaultIANA = fEncodingSettings.getIANATag();
      String workbenchValue = getWorkbenchEncoding();
      workbenchValue = CommonCharsetNames.getIanaPreferredCharsetName(workbenchValue);
      fEncodingSettings.setIANATag(workbenchValue);
    } else if (fNonDefaultIANA != null) {
      fEncodingSettings.setIANATag(fNonDefaultIANA);
    }
    getEncodingCombo().setEnabled(!fUseDefaultButton.getSelection());
    fEncodingSettings.setEnabled(!fUseDefaultButton.getSelection());
  }

  private boolean isDefault() {
    return fUseDefaultButton.getSelection();
  }

  /**
   * <code>setEncoding</code> Set the selection in the combo to the descriptive encoding name.
   * Selects use workbench encoding if ianaTag is null or empty string.
   */
  public void setIANATag(String ianaTag) {
    if ((ianaTag == null) || ianaTag.equals(WORKBENCH_DEFAULT)) {
      fUseDefaultButton.setSelection(true);
      handleUseDefaultButtonSelected();
    } else {
      fUseDefaultButton.setSelection(false);
      handleUseDefaultButtonSelected();
      if (!isDefault()) {
        fEncodingSettings.setIANATag(ianaTag);
      }
    }
  }
}
