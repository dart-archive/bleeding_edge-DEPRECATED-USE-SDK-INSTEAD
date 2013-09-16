/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.encoding.ui;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.wst.sse.core.internal.encoding.CommonEncodingPreferenceNames;
import org.eclipse.wst.sse.core.internal.encoding.util.CodedResourcePlugin;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;

/**
 * @deprecated - this should not be handled by the platform and not WTP
 */

public class EncodingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  Button fUse3ByteBOMWithUTF8CheckBox = null;

  private Button createCheckBox(Composite parent, String label) {
    Button button = new Button(parent, SWT.CHECK | SWT.LEFT | SWT.WRAP);
    button.setText(label);

    boolean selection = CodedResourcePlugin.getDefault().getPluginPreferences().getBoolean(
        CommonEncodingPreferenceNames.USE_3BYTE_BOM_WITH_UTF8);
    button.setSelection(selection);

    return button;
  }

  private Composite createComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    //GridLayout
    GridLayout layout = new GridLayout(1, true);
    composite.setLayout(layout);

    //GridData
    GridData data = new GridData(GridData.FILL_BOTH);
    composite.setLayoutData(data);

    return composite;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  protected Control createContents(Composite parent) {
    Composite composite = createComposite(parent);

    String description = SSEUIMessages.EncodingPreferencePage_0; //$NON-NLS-1$

    // ISSUE: the following to lines look redundant, 
    // not sure what was trying to be accomplished. 
    // May be dead code. 
    createLabel(composite, description);
    createLabel(composite, ""); //$NON-NLS-1$

    String checkBoxLabel = SSEUIMessages.EncodingPreferencePage_1; //$NON-NLS-1$
    fUse3ByteBOMWithUTF8CheckBox = createCheckBox(composite, checkBoxLabel);

    return composite;
  }

  private Label createLabel(Composite parent, String text) {
    Label label = new Label(parent, SWT.LEFT);
    label.setText(text);

    return label;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {
  }

  public void performDefaults() {
    boolean defaultSelection = CodedResourcePlugin.getDefault().getPluginPreferences().getDefaultBoolean(
        CommonEncodingPreferenceNames.USE_3BYTE_BOM_WITH_UTF8);
    fUse3ByteBOMWithUTF8CheckBox.setSelection(defaultSelection);
  }

  public boolean performOk() {
    CodedResourcePlugin.getDefault().getPluginPreferences().setValue(
        CommonEncodingPreferenceNames.USE_3BYTE_BOM_WITH_UTF8,
        fUse3ByteBOMWithUTF8CheckBox.getSelection());
    CodedResourcePlugin.getDefault().savePluginPreferences();

    return true;
  }
}
