/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.preferences.ui;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceNames;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.sse.core.internal.validate.ValidationMessage;
import org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractValidationSettingsPage;
import org.eclipse.wst.sse.ui.internal.preferences.ui.ScrolledPageContent;

public class HTMLValidationPreferencePage extends AbstractValidationSettingsPage {

  private static final int[] SEVERITIES = {
      ValidationMessage.ERROR, ValidationMessage.WARNING, ValidationMessage.IGNORE};

  private static final String SETTINGS_SECTION_NAME = "HTMLValidationSeverities";//$NON-NLS-1$

  public HTMLValidationPreferencePage() {
    super();
  }

  private PixelConverter fPixelConverter;

  protected Control createCommonContents(Composite parent) {
    final Composite page = new Composite(parent, SWT.NULL);

    //GridLayout
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    page.setLayout(layout);

    fPixelConverter = new PixelConverter(parent);

    final Composite content = createValidationSection(page);

    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
    gridData.heightHint = fPixelConverter.convertHeightInCharsToPixels(20);
    content.setLayoutData(gridData);

    return page;
  }

  private Composite createValidationSection(Composite page) {
    int nColumns = 3;

    final ScrolledPageContent spContent = new ScrolledPageContent(page);

    Composite composite = spContent.getBody();

    GridLayout layout = new GridLayout(nColumns, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);

    Label description = new Label(composite, SWT.NONE);
    description.setText(HTMLUIMessages.Validation_description);
    description.setFont(page.getFont());

    ExpandableComposite ec;
    Composite inner;
    String label;

    String[] errorWarningIgnoreLabel = new String[] {
        HTMLUIMessages.Validation_Error, HTMLUIMessages.Validation_Warning,
        HTMLUIMessages.Validation_Ignore};

    // Element section

    ec = createStyleSection(composite, HTMLUIMessages.Expandable_label_elements, nColumns);

    inner = new Composite(ec, SWT.NONE);
    inner.setFont(composite.getFont());
    inner.setLayout(new GridLayout(nColumns, false));
    ec.setClient(inner);

    label = HTMLUIMessages.HTMLValidationPreferencePage_8;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_UNKNOWN_NAME, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_9;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_INVALID_NAME, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_10;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_START_INVALID_CASE, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_11;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_END_INVALID_CASE, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_12;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_MISSING_START, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_13;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_MISSING_END, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_14;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_UNNECESSARY_END, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_15;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_INVALID_DIRECTIVE, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_16;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_INVALID_CONTENT, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_17;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_DUPLICATE, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_18;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_COEXISTENCE, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_19;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_UNCLOSED_START_TAG, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_20;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_UNCLOSED_END_TAG, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_21;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_INVALID_EMPTY_TAG, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_36;
    addComboBox(inner, label, HTMLCorePreferenceNames.ELEM_INVALID_TEXT, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    // End Element Section

    // The Attribute validation section

    ec = createStyleSection(composite, HTMLUIMessages.Expandable_label_attributes, nColumns);
    inner = new Composite(ec, SWT.NONE);
    inner.setFont(composite.getFont());
    inner.setLayout(new GridLayout(nColumns, false));
    ec.setClient(inner);

    label = HTMLUIMessages.HTMLValidationPreferencePage_0;
    addComboBox(inner, label, HTMLCorePreferenceNames.ATTRIBUTE_UNDEFINED_NAME, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_1;
    addComboBox(inner, label, HTMLCorePreferenceNames.ATTRIBUTE_UNDEFINED_VALUE, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_2;
    addComboBox(inner, label, HTMLCorePreferenceNames.ATTRIBUTE_NAME_MISMATCH, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_3;
    addComboBox(inner, label, HTMLCorePreferenceNames.ATTRIBUTE_INVALID_NAME, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_4;
    addComboBox(inner, label, HTMLCorePreferenceNames.ATTRIBUTE_INVALID_VALUE, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_5;
    addComboBox(inner, label, HTMLCorePreferenceNames.ATTRIBUTE_DUPLICATE, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_6;
    addComboBox(inner, label, HTMLCorePreferenceNames.ATTRIBUTE_VALUE_MISMATCH, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_7;
    addComboBox(inner, label, HTMLCorePreferenceNames.ATTRIBUTE_VALUE_UNCLOSED, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_37;
    addComboBox(inner, label, HTMLCorePreferenceNames.ATTRIBUTE_VALUE_EQUALS_MISSING, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_35;
    addComboBox(inner, label, HTMLCorePreferenceNames.ATTRIBUTE_VALUE_RESOURCE_NOT_FOUND,
        SEVERITIES, errorWarningIgnoreLabel, 0);

    // End Attribute section

    // Document Type
    ec = createStyleSection(composite, HTMLUIMessages.Expandable_label_document_type, nColumns);

    inner = new Composite(ec, SWT.NONE);
    inner.setFont(composite.getFont());
    inner.setLayout(new GridLayout(nColumns, false));
    ec.setClient(inner);

    label = HTMLUIMessages.HTMLValidationPreferencePage_22;
    addComboBox(inner, label, HTMLCorePreferenceNames.DOC_DUPLICATE, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_23;
    addComboBox(inner, label, HTMLCorePreferenceNames.DOC_INVALID_CONTENT, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_24;
    addComboBox(inner, label, HTMLCorePreferenceNames.DOC_DOCTYPE_UNCLOSED, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    // End Document Type

    // Comments
    ec = createStyleSection(composite, HTMLUIMessages.Expandable_label_comment, nColumns);

    inner = new Composite(ec, SWT.NONE);
    inner.setFont(composite.getFont());
    inner.setLayout(new GridLayout(nColumns, false));
    ec.setClient(inner);

    label = HTMLUIMessages.HTMLValidationPreferencePage_27;
    addComboBox(inner, label, HTMLCorePreferenceNames.COMMENT_INVALID_CONTENT, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_28;
    addComboBox(inner, label, HTMLCorePreferenceNames.COMMENT_UNCLOSED, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    // End Comments

    // CDATA Sections
    ec = createStyleSection(composite, HTMLUIMessages.Expandable_label_cdata, nColumns);

    inner = new Composite(ec, SWT.NONE);
    inner.setFont(composite.getFont());
    inner.setLayout(new GridLayout(nColumns, false));
    ec.setClient(inner);

    label = HTMLUIMessages.HTMLValidationPreferencePage_29;
    addComboBox(inner, label, HTMLCorePreferenceNames.CDATA_INVALID_CONTENT, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_30;
    addComboBox(inner, label, HTMLCorePreferenceNames.CDATA_UNCLOSED, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    // End CDATA Sections

    // Processing Instructions
    ec = createStyleSection(composite, HTMLUIMessages.Expandable_label_pi, nColumns);

    inner = new Composite(ec, SWT.NONE);
    inner.setFont(composite.getFont());
    inner.setLayout(new GridLayout(nColumns, false));
    ec.setClient(inner);

    label = HTMLUIMessages.HTMLValidationPreferencePage_31;
    addComboBox(inner, label, HTMLCorePreferenceNames.PI_INVALID_CONTENT, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_32;
    addComboBox(inner, label, HTMLCorePreferenceNames.PI_UNCLOSED, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    // End Processing Instructions

    // Entity References
    ec = createStyleSection(composite, HTMLUIMessages.Expandable_label_entity_ref, nColumns);

    inner = new Composite(ec, SWT.NONE);
    inner.setFont(composite.getFont());
    inner.setLayout(new GridLayout(nColumns, false));
    ec.setClient(inner);

    label = HTMLUIMessages.HTMLValidationPreferencePage_33;
    addComboBox(inner, label, HTMLCorePreferenceNames.REF_INVALID_CONTENT, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_34;
    addComboBox(inner, label, HTMLCorePreferenceNames.REF_UNDEFINED, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    // End Entity References

    // Text Content
    ec = createStyleSection(composite, HTMLUIMessages.Expandable_label_text, nColumns);

    inner = new Composite(ec, SWT.NONE);
    inner.setFont(composite.getFont());
    inner.setLayout(new GridLayout(nColumns, false));
    ec.setClient(inner);

    label = HTMLUIMessages.HTMLValidationPreferencePage_25;
    addComboBox(inner, label, HTMLCorePreferenceNames.TEXT_INVALID_CONTENT, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    label = HTMLUIMessages.HTMLValidationPreferencePage_26;
    addComboBox(inner, label, HTMLCorePreferenceNames.TEXT_INVALID_CHAR, SEVERITIES,
        errorWarningIgnoreLabel, 0);

    // End Text Content

    restoreSectionExpansionStates(getDialogSettings().getSection(SETTINGS_SECTION_NAME));

    return spContent;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  protected void performDefaults() {
    resetSeverities();
    super.performDefaults();
  }

  protected IDialogSettings getDialogSettings() {
    return HTMLUIPlugin.getDefault().getDialogSettings();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.dialogs.DialogPage#dispose()
   */
  public void dispose() {
    storeSectionExpansionStates(getDialogSettings().addNewSection(SETTINGS_SECTION_NAME));
    super.dispose();
  }

  protected String getQualifier() {
    return HTMLCorePlugin.getDefault().getBundle().getSymbolicName();
  }

  protected String getPreferenceNodeQualifier() {
    return HTMLCorePlugin.getDefault().getBundle().getSymbolicName();
  }

  protected String getPreferencePageID() {
    return "org.eclipse.wst.html.ui.preferences.validation";//$NON-NLS-1$
  }

  protected String getProjectSettingsKey() {
    return HTMLCorePreferenceNames.USE_PROJECT_SETTINGS;
  }

  protected String getPropertyPageID() {
    return "org.eclipse.wst.html.ui.propertyPage.project.validation";//$NON-NLS-1$
  }

  public void init(IWorkbench workbench) {
  }
}
