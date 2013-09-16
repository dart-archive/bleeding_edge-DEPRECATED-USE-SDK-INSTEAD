/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.preferences.ui;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
import org.eclipse.wst.css.ui.internal.CSSUIMessages;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.css.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage;

/**
 */
public class CSSSourcePreferencePage extends AbstractPreferencePage {
  // Content Assist
  protected Button fAutoPropose;
  protected Label fAutoProposeLabel;

  protected Text fAutoProposeText;
  protected Button fClearAllBlankLines;
  protected Button fIdentLower;
  // case of output character
  // case of identifier
  protected Button fIdentUpper;
  private Spinner fIndentationSize;
  private Button fIndentUsingSpaces;

  private Button fIndentUsingTabs;
  // Formatting
  protected Label fLineWidthLabel;

  protected Text fLineWidthText;
  // prohibit wrapping if style attribute
  protected Button fNowrapAttr;
  // one property per one line
  protected Button fPropertyPerLine;
  protected Button fPropNameLower;

  // case of property name
  protected Button fPropNameUpper;
  protected Button fPropValueLower;
  // case of property value
  protected Button fPropValueUpper;

  // Selector case
  protected Button fSelectorUpper;
  protected Button fSelectorLower;

  protected Button fSplitMultiAttrs;
  private final int MAX_INDENTATION_SIZE = 16;
  private final int MIN_INDENTATION_SIZE = 0;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  protected Control createContents(Composite parent) {
    final Composite composite = super.createComposite(parent, 1);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
        IHelpContextIds.CSS_PREFWEBX_SOURCE_HELPID);

    new PreferenceLinkArea(
        composite,
        SWT.WRAP | SWT.MULTI,
        "org.eclipse.wst.sse.ui.preferences.editor", CSSUIMessages._UI_STRUCTURED_TEXT_EDITOR_PREFS_LINK,//$NON-NLS-1$
        (IWorkbenchPreferenceContainer) getContainer(), null).getControl().setLayoutData(
        GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).create());
    new Label(composite, SWT.NONE).setLayoutData(GridDataFactory.swtDefaults().create());

    createContentsForFormattingGroup(composite);
    createContentsForContentAssistGroup(composite);
    setSize(composite);
    loadPreferences();

    return composite;
  }

  private void createContentsForContentAssistGroup(Composite parent) {
    // not content assist, but preferred case
    Group caseGroup = createGroup(parent, 4);
    caseGroup.setText(CSSUIMessages.PrefsLabel_CaseGroup);

    // use group for radio buttons so that associated label is read
    Group identGroup = createGroup(caseGroup, 1);
    identGroup.setText(CSSUIMessages.PrefsLabel_CaseIdent);
    fIdentUpper = createRadioButton(identGroup, CSSUIMessages.PrefsLabel_CaseIdentUpper);
    fIdentLower = createRadioButton(identGroup, CSSUIMessages.PrefsLabel_CaseIdentLower);

    // use group for radio buttons so that associated label is read
    Group selectorGroup = createGroup(caseGroup, 1);
    selectorGroup.setText(CSSUIMessages.PrefsLabel_SelectorTagName);
    fSelectorUpper = createRadioButton(selectorGroup, CSSUIMessages.PrefsLabel_SelectorTagNameUpper);
    fSelectorLower = createRadioButton(selectorGroup, CSSUIMessages.PrefsLabel_SelectorTagNameLower);

    // use group for radio buttons so that associated label is read
    Group propNameGroup = createGroup(caseGroup, 1);
    propNameGroup.setText(CSSUIMessages.PrefsLabel_CasePropName);
    fPropNameUpper = createRadioButton(propNameGroup, CSSUIMessages.PrefsLabel_CasePropNameUpper);
    fPropNameLower = createRadioButton(propNameGroup, CSSUIMessages.PrefsLabel_CasePropNameLower);

    // use group for radio buttons so that associated label is read
    Group propValueGroup = createGroup(caseGroup, 1);
    propValueGroup.setText(CSSUIMessages.PrefsLabel_CasePropValue);
    fPropValueUpper = createRadioButton(propValueGroup, CSSUIMessages.PrefsLabel_CasePropValueUpper);
    fPropValueLower = createRadioButton(propValueGroup, CSSUIMessages.PrefsLabel_CasePropValueLower);
  }

  private void createContentsForFormattingGroup(Composite parent) {
    Group formattingGroup = createGroup(parent, 2);
    formattingGroup.setText(CSSUIMessages.Formatting_UI_);

    fLineWidthLabel = createLabel(formattingGroup, CSSUIMessages.Line_width__UI_);
    fLineWidthText = new Text(formattingGroup, SWT.SINGLE | SWT.BORDER);
    GridData gData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.BEGINNING);
    gData.widthHint = 25;
    fLineWidthText.setLayoutData(gData);
    fLineWidthText.addModifyListener(this);

    fPropertyPerLine = createCheckBox(formattingGroup,
        CSSUIMessages.PrefsLabel_WrappingInsertLineBreak);
    ((GridData) fPropertyPerLine.getLayoutData()).horizontalSpan = 2;

    fNowrapAttr = createCheckBox(formattingGroup, CSSUIMessages.PrefsLabel_WrappingWithoutAttr);
    ((GridData) fNowrapAttr.getLayoutData()).horizontalSpan = 2;

    // [269224] - Place the indent controls in their own composite for proper tab ordering
    Composite indentComposite = createComposite(formattingGroup, 1);
    ((GridData) indentComposite.getLayoutData()).horizontalSpan = 2;
    ((GridLayout) indentComposite.getLayout()).marginWidth = 0;
    ((GridLayout) indentComposite.getLayout()).marginHeight = 0;

    fIndentUsingTabs = createRadioButton(indentComposite, CSSUIMessages.Indent_using_tabs_);
    ((GridData) fIndentUsingTabs.getLayoutData()).horizontalSpan = 1;
    fIndentUsingSpaces = createRadioButton(indentComposite, CSSUIMessages.Indent_using_spaces);
    ((GridData) fIndentUsingSpaces.getLayoutData()).horizontalSpan = 1;

    createLabel(formattingGroup, CSSUIMessages.Indentation_size);
    fIndentationSize = new Spinner(formattingGroup, SWT.READ_ONLY | SWT.BORDER);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    fIndentationSize.setLayoutData(gd);
    fIndentationSize.setToolTipText(CSSUIMessages.Indentation_size_tip);
    fIndentationSize.setMinimum(MIN_INDENTATION_SIZE);
    fIndentationSize.setMaximum(MAX_INDENTATION_SIZE);
    fIndentationSize.setIncrement(1);
    fIndentationSize.setPageIncrement(4);
    fIndentationSize.addModifyListener(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
   */
  protected IPreferenceStore doGetPreferenceStore() {
    return CSSUIPlugin.getDefault().getPreferenceStore();
  }

  private void doSavePreferenceStore() {
    CSSUIPlugin.getDefault().savePluginPreferences();
    CSSCorePlugin.getDefault().savePluginPreferences(); // model
  }

  protected Preferences getModelPreferences() {
    return CSSCorePlugin.getDefault().getPluginPreferences();
  }

  protected void initializeValues() {
    initializeValuesForFormattingGroup();
    initializeValuesForContentAssistGroup();
  }

  private void initializeValuesForContentAssistGroup() {
    // not content assist, but preferred case
    Preferences prefs = getModelPreferences();
    fIdentUpper.setSelection(prefs.getInt(CSSCorePreferenceNames.CASE_IDENTIFIER) == CSSCorePreferenceNames.UPPER);
    fIdentLower.setSelection(prefs.getInt(CSSCorePreferenceNames.CASE_IDENTIFIER) == CSSCorePreferenceNames.LOWER);
    fSelectorUpper.setSelection(prefs.getInt(CSSCorePreferenceNames.CASE_SELECTOR) == CSSCorePreferenceNames.UPPER);
    fSelectorLower.setSelection(prefs.getInt(CSSCorePreferenceNames.CASE_SELECTOR) == CSSCorePreferenceNames.LOWER);
    fPropNameUpper.setSelection(prefs.getInt(CSSCorePreferenceNames.CASE_PROPERTY_NAME) == CSSCorePreferenceNames.UPPER);
    fPropNameLower.setSelection(prefs.getInt(CSSCorePreferenceNames.CASE_PROPERTY_NAME) == CSSCorePreferenceNames.LOWER);
    fPropValueUpper.setSelection(prefs.getInt(CSSCorePreferenceNames.CASE_PROPERTY_VALUE) == CSSCorePreferenceNames.UPPER);
    fPropValueLower.setSelection(prefs.getInt(CSSCorePreferenceNames.CASE_PROPERTY_VALUE) == CSSCorePreferenceNames.LOWER);
  }

  private void initializeValuesForFormattingGroup() {
    // Formatting
    Preferences prefs = getModelPreferences();
    fLineWidthText.setText(prefs.getString(CSSCorePreferenceNames.LINE_WIDTH));
    fPropertyPerLine.setSelection(prefs.getBoolean(CSSCorePreferenceNames.WRAPPING_ONE_PER_LINE));
    fNowrapAttr.setSelection(prefs.getBoolean(CSSCorePreferenceNames.WRAPPING_PROHIBIT_WRAP_ON_ATTR));

    if (CSSCorePreferenceNames.TAB.equals(getModelPreferences().getString(
        CSSCorePreferenceNames.INDENTATION_CHAR))) {
      fIndentUsingTabs.setSelection(true);
      fIndentUsingSpaces.setSelection(false);
    } else {
      fIndentUsingSpaces.setSelection(true);
      fIndentUsingTabs.setSelection(false);
    }

    fIndentationSize.setSelection(getModelPreferences().getInt(
        CSSCorePreferenceNames.INDENTATION_SIZE));
  }

  protected void performDefaults() {
    performDefaultsForFormattingGroup();
    performDefaultsForContentAssistGroup();

    validateValues();
    enableValues();

    super.performDefaults();
  }

  private void performDefaultsForContentAssistGroup() {
    // not content assist, but preferred case
    Preferences prefs = getModelPreferences();
    fIdentUpper.setSelection(prefs.getDefaultInt(CSSCorePreferenceNames.CASE_IDENTIFIER) == CSSCorePreferenceNames.UPPER);
    fIdentLower.setSelection(prefs.getDefaultInt(CSSCorePreferenceNames.CASE_IDENTIFIER) == CSSCorePreferenceNames.LOWER);
    fSelectorUpper.setSelection(prefs.getDefaultInt(CSSCorePreferenceNames.CASE_SELECTOR) == CSSCorePreferenceNames.UPPER);
    fSelectorLower.setSelection(prefs.getDefaultInt(CSSCorePreferenceNames.CASE_SELECTOR) == CSSCorePreferenceNames.LOWER);
    fPropNameUpper.setSelection(prefs.getDefaultInt(CSSCorePreferenceNames.CASE_PROPERTY_NAME) == CSSCorePreferenceNames.UPPER);
    fPropNameLower.setSelection(prefs.getDefaultInt(CSSCorePreferenceNames.CASE_PROPERTY_NAME) == CSSCorePreferenceNames.LOWER);
    fPropValueUpper.setSelection(prefs.getDefaultInt(CSSCorePreferenceNames.CASE_PROPERTY_VALUE) == CSSCorePreferenceNames.UPPER);
    fPropValueLower.setSelection(prefs.getDefaultInt(CSSCorePreferenceNames.CASE_PROPERTY_VALUE) == CSSCorePreferenceNames.LOWER);
  }

  private void performDefaultsForFormattingGroup() {
    // Formatting
    Preferences prefs = getModelPreferences();
    fLineWidthText.setText(prefs.getDefaultString(CSSCorePreferenceNames.LINE_WIDTH));
    fPropertyPerLine.setSelection(prefs.getDefaultBoolean(CSSCorePreferenceNames.WRAPPING_ONE_PER_LINE));
    fNowrapAttr.setSelection(prefs.getDefaultBoolean(CSSCorePreferenceNames.WRAPPING_PROHIBIT_WRAP_ON_ATTR));

    if (CSSCorePreferenceNames.TAB.equals(getModelPreferences().getDefaultString(
        CSSCorePreferenceNames.INDENTATION_CHAR))) {
      fIndentUsingTabs.setSelection(true);
      fIndentUsingSpaces.setSelection(false);
    } else {
      fIndentUsingSpaces.setSelection(true);
      fIndentUsingTabs.setSelection(false);
    }
    fIndentationSize.setSelection(getModelPreferences().getDefaultInt(
        CSSCorePreferenceNames.INDENTATION_SIZE));
  }

  public boolean performOk() {
    boolean result = super.performOk();

    doSavePreferenceStore();

    return result;
  }

  protected void storeValues() {
    storeValuesForFormattingGroup();
    storeValuesForContentAssistGroup();
  }

  private void storeValuesForContentAssistGroup() {
    // not content assist, but preferred case
    Preferences prefs = getModelPreferences();
    prefs.setValue(CSSCorePreferenceNames.CASE_IDENTIFIER, (fIdentUpper.getSelection())
        ? CSSCorePreferenceNames.UPPER : CSSCorePreferenceNames.LOWER);
    prefs.setValue(CSSCorePreferenceNames.CASE_SELECTOR, (fSelectorUpper.getSelection())
        ? CSSCorePreferenceNames.UPPER : CSSCorePreferenceNames.LOWER);
    prefs.setValue(CSSCorePreferenceNames.CASE_PROPERTY_NAME, (fPropNameUpper.getSelection())
        ? CSSCorePreferenceNames.UPPER : CSSCorePreferenceNames.LOWER);
    prefs.setValue(CSSCorePreferenceNames.CASE_PROPERTY_VALUE, (fPropValueUpper.getSelection())
        ? CSSCorePreferenceNames.UPPER : CSSCorePreferenceNames.LOWER);
  }

  private void storeValuesForFormattingGroup() {
    // Formatting
    Preferences prefs = getModelPreferences();
    prefs.setValue(CSSCorePreferenceNames.LINE_WIDTH, fLineWidthText.getText());
    prefs.setValue(CSSCorePreferenceNames.WRAPPING_ONE_PER_LINE, fPropertyPerLine.getSelection());
    prefs.setValue(CSSCorePreferenceNames.WRAPPING_PROHIBIT_WRAP_ON_ATTR,
        fNowrapAttr.getSelection());

    if (fIndentUsingTabs.getSelection()) {
      getModelPreferences().setValue(CSSCorePreferenceNames.INDENTATION_CHAR,
          CSSCorePreferenceNames.TAB);
    } else {
      getModelPreferences().setValue(CSSCorePreferenceNames.INDENTATION_CHAR,
          CSSCorePreferenceNames.SPACE);
    }
    getModelPreferences().setValue(CSSCorePreferenceNames.INDENTATION_SIZE,
        fIndentationSize.getSelection());
  }

  protected void validateValues() {
    boolean isError = false;
    String widthText = null;

    if (fLineWidthText != null) {
      try {
        widthText = fLineWidthText.getText();
        int formattingLineWidth = Integer.parseInt(widthText);
        if ((formattingLineWidth < WIDTH_VALIDATION_LOWER_LIMIT)
            || (formattingLineWidth > WIDTH_VALIDATION_UPPER_LIMIT)) {
          throw new NumberFormatException();
        }
      } catch (NumberFormatException nfexc) {
        setInvalidInputMessage(widthText);
        setValid(false);
        isError = true;
      }
    }

    if (!isError) {
      setErrorMessage(null);
      setValid(true);
    }
  }
}
