/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.edit.ui;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceNames;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.html.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.core.internal.encoding.CommonEncodingPreferenceNames;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

public class CleanupDialogHTML extends Dialog implements SelectionListener {

  protected Button fRadioButtonTagNameCaseAsis;
  protected Button fRadioButtonTagNameCaseLower;
  protected Button fRadioButtonTagNameCaseUpper;
  protected Button fRadioButtonAttrNameCaseAsis;
  protected Button fRadioButtonAttrNameCaseLower;
  protected Button fRadioButtonAttrNameCaseUpper;
  protected Button fCheckBoxCompressEmptyElementTags;
  protected Button fCheckBoxInsertRequiredAttrs;
  protected Button fCheckBoxInsertMissingTags;
  protected Button fCheckBoxQuoteAttrValues;
  protected Button fCheckBoxFormatSource;
  protected Button fCheckBoxConvertEOLCodes;
  protected Button fRadioButtonConvertEOLWindows;
  protected Button fRadioButtonConvertEOLUnix;
  protected Button fRadioButtonConvertEOLMac;
  protected Preferences fPreferences = null;
  private boolean fIsXHTML = false;
  private Group fTagNameCase;
  private Group fAttrNameCase;

  public CleanupDialogHTML(Shell shell) {
    super(shell);
  }

  protected Control createDialogArea(Composite parent) {
    getShell().setText(HTMLUIMessages.Cleanup_UI_);
    Composite composite = new Composite(parent, SWT.NULL);

    createDialogAreaInComposite(composite);
    initializeOptions();

    return composite;
  }

  protected Control createButtonBar(Composite parent) {
    Control c = super.createButtonBar(parent);
    okButtonEnablement();
    return c;
  }

  protected void createDialogAreaInCompositeForHTML(Composite composite) {
    // Convert tag name case
    // d257064 need to associate group w/ radio buttons so radio buttons
    // header can be read
    fTagNameCase = new Group(composite, SWT.NONE);
    fTagNameCase.setText(HTMLUIMessages.Tag_name_case_for_HTML_UI_);
    GridLayout hLayout = new GridLayout();
    hLayout.numColumns = 3;
    fTagNameCase.setLayout(hLayout);
    fRadioButtonTagNameCaseAsis = new Button(fTagNameCase, SWT.RADIO);
    fRadioButtonTagNameCaseAsis.setText(HTMLUIMessages.Tag_name_case_As_is_UI_);
    fRadioButtonTagNameCaseAsis.addSelectionListener(this);
    fRadioButtonTagNameCaseLower = new Button(fTagNameCase, SWT.RADIO);
    fRadioButtonTagNameCaseLower.setText(HTMLUIMessages.Tag_name_case_Lower_UI_);
    fRadioButtonTagNameCaseLower.addSelectionListener(this);
    fRadioButtonTagNameCaseUpper = new Button(fTagNameCase, SWT.RADIO);
    fRadioButtonTagNameCaseUpper.setText(HTMLUIMessages.Tag_name_case_Upper_UI_);
    fRadioButtonTagNameCaseUpper.addSelectionListener(this);

    // Convert attr name case
    // d257064 need to associate group w/ radio buttons so radio buttons
    // header can be read
    fAttrNameCase = new Group(composite, SWT.NONE);
    fAttrNameCase.setText(HTMLUIMessages.Attribute_name_case_for_HTML_UI_);
    fAttrNameCase.setLayout(hLayout);
    fRadioButtonAttrNameCaseAsis = new Button(fAttrNameCase, SWT.RADIO);
    fRadioButtonAttrNameCaseAsis.setText(HTMLUIMessages.Attribute_name_case_As_is_UI_);
    fRadioButtonAttrNameCaseAsis.addSelectionListener(this);
    fRadioButtonAttrNameCaseLower = new Button(fAttrNameCase, SWT.RADIO);
    fRadioButtonAttrNameCaseLower.setText(HTMLUIMessages.Attribute_name_case_Lower_UI_);
    fRadioButtonAttrNameCaseLower.addSelectionListener(this);
    fRadioButtonAttrNameCaseUpper = new Button(fAttrNameCase, SWT.RADIO);
    fRadioButtonAttrNameCaseUpper.setText(HTMLUIMessages.Attribute_name_case_Upper_UI_);
    fRadioButtonAttrNameCaseUpper.addSelectionListener(this);
  }

  protected void createDialogAreaInComposite(Composite composite) {
    createDialogAreaInCompositeForHTML(composite);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
        IHelpContextIds.CLEANUP_HTML_HELPID); // use
    // HTML
    // specific
    // help

    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.makeColumnsEqualWidth = true;
    composite.setLayout(layout);

    // Compress empty element tags
    fCheckBoxCompressEmptyElementTags = new Button(composite, SWT.CHECK);
    fCheckBoxCompressEmptyElementTags.setText(XMLUIMessages.Compress_empty_element_tags_UI_);
    fCheckBoxCompressEmptyElementTags.addSelectionListener(this);

    // Insert missing required attrs
    fCheckBoxInsertRequiredAttrs = new Button(composite, SWT.CHECK);
    fCheckBoxInsertRequiredAttrs.setText(HTMLUIMessages.Insert_required_attributes_UI_);
    fCheckBoxInsertRequiredAttrs.addSelectionListener(this);

    // Insert missing begin/end tags
    fCheckBoxInsertMissingTags = new Button(composite, SWT.CHECK);
    fCheckBoxInsertMissingTags.setText(HTMLUIMessages.Insert_missing_tags_UI_);
    fCheckBoxInsertMissingTags.addSelectionListener(this);

    // Quote attribute values
    fCheckBoxQuoteAttrValues = new Button(composite, SWT.CHECK);
    fCheckBoxQuoteAttrValues.setText(HTMLUIMessages.Quote_attribute_values_UI_);
    fCheckBoxQuoteAttrValues.addSelectionListener(this);

    // Format source
    fCheckBoxFormatSource = new Button(composite, SWT.CHECK);
    fCheckBoxFormatSource.setText(HTMLUIMessages.Format_source_UI_);
    fCheckBoxFormatSource.addSelectionListener(this);

    // Convert EOL code
    fCheckBoxConvertEOLCodes = new Button(composite, SWT.CHECK);
    fCheckBoxConvertEOLCodes.setText(HTMLUIMessages.Convert_EOL_codes_UI_);
    fCheckBoxConvertEOLCodes.addSelectionListener(this);
    Composite EOLCodes = new Composite(composite, SWT.NULL);
    GridLayout hLayout = new GridLayout();
    hLayout.numColumns = 3;
    EOLCodes.setLayout(hLayout);
    fRadioButtonConvertEOLWindows = new Button(EOLCodes, SWT.RADIO);
    fRadioButtonConvertEOLWindows.setText(HTMLUIMessages.EOL_Windows_UI);
    fRadioButtonConvertEOLWindows.addSelectionListener(this);
    fRadioButtonConvertEOLUnix = new Button(EOLCodes, SWT.RADIO);
    fRadioButtonConvertEOLUnix.setText(HTMLUIMessages.EOL_Unix_UI);
    fRadioButtonConvertEOLUnix.addSelectionListener(this);
    fRadioButtonConvertEOLMac = new Button(EOLCodes, SWT.RADIO);
    fRadioButtonConvertEOLMac.setText(HTMLUIMessages.EOL_Mac_UI);
    fRadioButtonConvertEOLMac.addSelectionListener(this);
  }

  protected void okPressed() {
    storeOptions();

    super.okPressed();
  }

  protected void initializeOptionsForHTML() {
    boolean caseSensitive = isXHTMLType();

    if (caseSensitive) {
      fRadioButtonTagNameCaseLower.setSelection(true);
    } else {
      int tagNameCase = getModelPreferences().getInt(HTMLCorePreferenceNames.CLEANUP_TAG_NAME_CASE);
      if (tagNameCase == HTMLCorePreferenceNames.UPPER) {
        fRadioButtonTagNameCaseUpper.setSelection(true);
      } else if (tagNameCase == HTMLCorePreferenceNames.LOWER)
        fRadioButtonTagNameCaseLower.setSelection(true);
      else
        fRadioButtonTagNameCaseAsis.setSelection(true);
    }

    if (caseSensitive) {
      fRadioButtonAttrNameCaseLower.setSelection(true);
    } else {
      int attrNameCase = getModelPreferences().getInt(
          HTMLCorePreferenceNames.CLEANUP_ATTR_NAME_CASE);
      if (attrNameCase == HTMLCorePreferenceNames.UPPER) {
        fRadioButtonAttrNameCaseUpper.setSelection(true);
      } else if (attrNameCase == HTMLCorePreferenceNames.LOWER)
        fRadioButtonAttrNameCaseLower.setSelection(true);
      else
        fRadioButtonAttrNameCaseAsis.setSelection(true);
    }

    enableCaseControls(!caseSensitive);
  }

  protected void initializeOptions() {
    initializeOptionsForHTML();

    fCheckBoxCompressEmptyElementTags.setSelection(getModelPreferences().getBoolean(
        HTMLCorePreferenceNames.COMPRESS_EMPTY_ELEMENT_TAGS));
    fCheckBoxInsertRequiredAttrs.setSelection(getModelPreferences().getBoolean(
        HTMLCorePreferenceNames.INSERT_REQUIRED_ATTRS));
    fCheckBoxInsertMissingTags.setSelection(getModelPreferences().getBoolean(
        HTMLCorePreferenceNames.INSERT_MISSING_TAGS));
    fCheckBoxQuoteAttrValues.setSelection(getModelPreferences().getBoolean(
        HTMLCorePreferenceNames.QUOTE_ATTR_VALUES));
    fCheckBoxFormatSource.setSelection(getModelPreferences().getBoolean(
        HTMLCorePreferenceNames.FORMAT_SOURCE));
    fCheckBoxConvertEOLCodes.setSelection(getModelPreferences().getBoolean(
        HTMLCorePreferenceNames.CONVERT_EOL_CODES));
    if (fCheckBoxConvertEOLCodes.getSelection()) {
      String EOLCode = getModelPreferences().getString(HTMLCorePreferenceNames.CLEANUP_EOL_CODE);
      if (EOLCode == CommonEncodingPreferenceNames.LF)
        fRadioButtonConvertEOLUnix.setSelection(true);
      else if (EOLCode == CommonEncodingPreferenceNames.CR)
        fRadioButtonConvertEOLMac.setSelection(true);
      else
        fRadioButtonConvertEOLWindows.setSelection(true);
    }
    enableEOLCodeRadios(fCheckBoxConvertEOLCodes.getSelection());
  }

  protected void storeOptionsForHTML() {
    if (!isXHTMLType() && fRadioButtonTagNameCaseUpper.getSelection())
      getModelPreferences().setValue(HTMLCorePreferenceNames.CLEANUP_TAG_NAME_CASE,
          HTMLCorePreferenceNames.UPPER);
    else if (fRadioButtonTagNameCaseLower.getSelection())
      getModelPreferences().setValue(HTMLCorePreferenceNames.CLEANUP_TAG_NAME_CASE,
          HTMLCorePreferenceNames.LOWER);
    else
      getModelPreferences().setValue(HTMLCorePreferenceNames.CLEANUP_TAG_NAME_CASE,
          HTMLCorePreferenceNames.ASIS);

    if (!isXHTMLType() && fRadioButtonAttrNameCaseUpper.getSelection())
      getModelPreferences().setValue(HTMLCorePreferenceNames.CLEANUP_ATTR_NAME_CASE,
          HTMLCorePreferenceNames.UPPER);
    else if (fRadioButtonAttrNameCaseLower.getSelection())
      getModelPreferences().setValue(HTMLCorePreferenceNames.CLEANUP_ATTR_NAME_CASE,
          HTMLCorePreferenceNames.LOWER);
    else
      getModelPreferences().setValue(HTMLCorePreferenceNames.CLEANUP_ATTR_NAME_CASE,
          HTMLCorePreferenceNames.ASIS);

    // explicitly save plugin preferences so values are stored
    HTMLCorePlugin.getDefault().savePluginPreferences();
  }

  protected void storeOptions() {
    storeOptionsForHTML();

    getModelPreferences().setValue(HTMLCorePreferenceNames.COMPRESS_EMPTY_ELEMENT_TAGS,
        fCheckBoxCompressEmptyElementTags.getSelection());
    getModelPreferences().setValue(HTMLCorePreferenceNames.INSERT_REQUIRED_ATTRS,
        fCheckBoxInsertRequiredAttrs.getSelection());
    getModelPreferences().setValue(HTMLCorePreferenceNames.INSERT_MISSING_TAGS,
        fCheckBoxInsertMissingTags.getSelection());
    getModelPreferences().setValue(HTMLCorePreferenceNames.QUOTE_ATTR_VALUES,
        fCheckBoxQuoteAttrValues.getSelection());
    getModelPreferences().setValue(HTMLCorePreferenceNames.FORMAT_SOURCE,
        fCheckBoxFormatSource.getSelection());
    getModelPreferences().setValue(HTMLCorePreferenceNames.CONVERT_EOL_CODES,
        fCheckBoxConvertEOLCodes.getSelection());

    if (fCheckBoxConvertEOLCodes.getSelection()) {
      if (fRadioButtonConvertEOLUnix.getSelection()) {
        getModelPreferences().setValue(HTMLCorePreferenceNames.CLEANUP_EOL_CODE,
            CommonEncodingPreferenceNames.LF);
      } else if (fRadioButtonConvertEOLMac.getSelection()) {
        getModelPreferences().setValue(HTMLCorePreferenceNames.CLEANUP_EOL_CODE,
            CommonEncodingPreferenceNames.CR);
      } else {
        getModelPreferences().setValue(HTMLCorePreferenceNames.CLEANUP_EOL_CODE,
            CommonEncodingPreferenceNames.CRLF);
      }
    }

    // explicitly save plugin preferences so values are stored
    HTMLCorePlugin.getDefault().savePluginPreferences();
  }

  public void widgetDefaultSelected(SelectionEvent e) {
    widgetSelected(e);
  }

  public void widgetSelected(SelectionEvent e) {
    if (e.widget == fCheckBoxConvertEOLCodes)
      enableEOLCodeRadios(fCheckBoxConvertEOLCodes.getSelection());

    okButtonEnablement();
  }

  protected Preferences getModelPreferences() {
    return HTMLCorePlugin.getDefault().getPluginPreferences();
  }

  private boolean isXHTMLType() {
    return fIsXHTML;
  }

  protected void enableEOLCodeRadios(boolean enable) {
    if ((fRadioButtonConvertEOLWindows != null) && (fRadioButtonConvertEOLUnix != null)
        && (fRadioButtonConvertEOLMac != null)) {
      fRadioButtonConvertEOLWindows.setEnabled(enable);
      fRadioButtonConvertEOLUnix.setEnabled(enable);
      fRadioButtonConvertEOLMac.setEnabled(enable);

      if (!fRadioButtonConvertEOLWindows.getSelection()
          && !fRadioButtonConvertEOLUnix.getSelection()
          && !fRadioButtonConvertEOLMac.getSelection())
        fRadioButtonConvertEOLWindows.setSelection(true);
    }
  }

  /**
   * Enables/disables the tag/attr case radio buttons
   */
  private void enableCaseControls(boolean enable) {
    fTagNameCase.setEnabled(enable);
    fRadioButtonTagNameCaseAsis.setEnabled(enable);
    fRadioButtonTagNameCaseLower.setEnabled(enable);
    fRadioButtonTagNameCaseUpper.setEnabled(enable);

    fAttrNameCase.setEnabled(enable);
    fRadioButtonAttrNameCaseAsis.setEnabled(enable);
    fRadioButtonAttrNameCaseLower.setEnabled(enable);
    fRadioButtonAttrNameCaseUpper.setEnabled(enable);
  }

  /**
   * Enables/disables OK button
   */
  private void okButtonEnablement() {
    boolean tagNameCaseCheck = ((fRadioButtonTagNameCaseUpper != null && fRadioButtonTagNameCaseUpper.getSelection()) || fRadioButtonTagNameCaseLower.getSelection());
    boolean attrNameCaseCheck = ((fRadioButtonAttrNameCaseUpper != null && fRadioButtonAttrNameCaseUpper.getSelection()) || fRadioButtonAttrNameCaseLower.getSelection());
    boolean eolCheck = fCheckBoxConvertEOLCodes.getSelection()
        && (fRadioButtonConvertEOLUnix.getSelection() || fRadioButtonConvertEOLMac.getSelection() || fRadioButtonConvertEOLWindows.getSelection());
    boolean buttonEnabled = false;
    if (isXHTMLType()) {
      buttonEnabled = fCheckBoxInsertRequiredAttrs.getSelection()
          || fCheckBoxInsertMissingTags.getSelection() || fCheckBoxQuoteAttrValues.getSelection()
          || fCheckBoxFormatSource.getSelection() || eolCheck;
    } else {
      buttonEnabled = tagNameCaseCheck || attrNameCaseCheck
          || fCheckBoxInsertRequiredAttrs.getSelection()
          || fCheckBoxInsertMissingTags.getSelection() || fCheckBoxQuoteAttrValues.getSelection()
          || fCheckBoxFormatSource.getSelection() || eolCheck;
    }
    getButton(IDialogConstants.OK_ID).setEnabled(buttonEnabled);
  }

  void setisXHTMLType(boolean isXHTML) {
    fIsXHTML = isXHTML;
  }
}
