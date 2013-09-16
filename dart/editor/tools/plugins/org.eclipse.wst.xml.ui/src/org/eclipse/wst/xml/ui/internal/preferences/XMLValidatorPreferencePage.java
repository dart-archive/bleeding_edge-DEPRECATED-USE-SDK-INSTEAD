/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring David Carver - STAR -
 * [205989] - [validation] validate XML after XInclude resolution
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.preferences;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.wst.sse.core.internal.validate.ValidationMessage;
import org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractValidationSettingsPage;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

public class XMLValidatorPreferencePage extends AbstractValidationSettingsPage {
  private static final String SETTINGS_SECTION_NAME = "XMLValidationSeverities";//$NON-NLS-1$

  boolean fOriginalUseXIncludeButtonSelected;

  boolean fOriginalUseHonourAllButtonSelected;

  boolean fOriginalUseExtendedMarkupValidation;

  private Combo fIndicateNoGrammar = null;

  private Combo fIndicateNoDocumentElement = null;

  private Button fHonourAllSchemaLocations = null;

  private Button fUseXinclude = null;

  private Button fExtendedMarkupValidation;

  private Combo fMissingStartTag;

  private Combo fMissingEndTag;

  private Combo fMissingTagName;

  private Combo fEmptyElementTag;

  private Combo fEndTagWithAttributes;

  private Combo fInvalidWhitespaceBeforeTagname;

  private Combo fMissingClosingBracket;

  private Combo fMissingClosingQuote;

  private Combo fMissingQuotes;

  private Combo fInvalidNamespaceInPI;

  private Combo fInvalidWhitespaceAtStart;

  private Group fMarkupValidationGroup;
  private ControlEnableState fMarkupState;

  private static final int[] XML_SEVERITIES = {
      ValidationMessage.WARNING, ValidationMessage.ERROR, ValidationMessage.IGNORE};

  private static final String[] MARKUP_SEVERITIES = {
      XMLUIMessages.Severity_error, XMLUIMessages.Severity_warning, XMLUIMessages.Severity_ignore};

  protected void createContentsForValidatingGroup(Composite validatingGroup) {

    if (fIndicateNoGrammar == null)
      fIndicateNoGrammar = addComboBox(validatingGroup,
          XMLUIMessages.Indicate_no_grammar_specified, XMLCorePreferenceNames.INDICATE_NO_GRAMMAR,
          XML_SEVERITIES, MARKUP_SEVERITIES, 0);

    if (fIndicateNoDocumentElement == null) {
      fIndicateNoDocumentElement = addComboBox(validatingGroup,
          XMLUIMessages.Indicate_no_document_element,
          XMLCorePreferenceNames.INDICATE_NO_DOCUMENT_ELEMENT, XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    }

    if (fUseXinclude == null) {
      fUseXinclude = createCheckBox(validatingGroup, XMLUIMessages.Use_XInclude);
      ((GridData) fUseXinclude.getLayoutData()).horizontalSpan = 2;
    }

    if (fHonourAllSchemaLocations == null) {
      fHonourAllSchemaLocations = createCheckBox(validatingGroup,
          XMLUIMessages.Honour_all_schema_locations);
      ((GridData) fHonourAllSchemaLocations.getLayoutData()).horizontalSpan = 2;
    }

    IScopeContext[] contexts = createPreferenceScopes();
    fOriginalUseXIncludeButtonSelected = contexts[0].getNode(getPreferenceNodeQualifier()).getBoolean(
        XMLCorePreferenceNames.USE_XINCLUDE, true);

    if (fUseXinclude != null) {
      fUseXinclude.setSelection(fOriginalUseXIncludeButtonSelected);
    }
    fOriginalUseHonourAllButtonSelected = contexts[0].getNode(getPreferenceNodeQualifier()).getBoolean(
        XMLCorePreferenceNames.HONOUR_ALL_SCHEMA_LOCATIONS, true);
    if (fHonourAllSchemaLocations != null) {
      fHonourAllSchemaLocations.setSelection(fOriginalUseHonourAllButtonSelected);
    }

  }

  private void handleMarkupSeveritySelection(boolean selection) {
    if (selection) {
      if (fMarkupState != null) {
        fMarkupState.restore();
        fMarkupState = null;
      }
    } else {
      if (fMarkupState == null)
        fMarkupState = ControlEnableState.disable(fMarkupValidationGroup);
    }
  }

  protected void createContentsForMarkupValidationGroup(Composite parent) {

    IScopeContext[] contexts = createPreferenceScopes();

    fOriginalUseExtendedMarkupValidation = contexts[0].getNode(getPreferenceNodeQualifier()).getBoolean(
        XMLCorePreferenceNames.MARKUP_VALIDATION, false);
    fExtendedMarkupValidation = createCheckBox(parent, XMLUIMessages.MarkupValidation_files);

    ((GridData) fExtendedMarkupValidation.getLayoutData()).horizontalSpan = 2;
    fExtendedMarkupValidation.setSelection(fOriginalUseExtendedMarkupValidation);

    fExtendedMarkupValidation.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        handleMarkupSeveritySelection(fExtendedMarkupValidation.getSelection());
      }
    });

    fMarkupValidationGroup = createGroup(parent, 3);
    ((GridLayout) fMarkupValidationGroup.getLayout()).makeColumnsEqualWidth = false;
    fMarkupValidationGroup.setText(XMLUIMessages.MarkupValidation_files_label);
    GridLayout layout = new GridLayout(3, false);
    fMarkupValidationGroup.setLayout(layout);

    if (fMissingStartTag == null)
      fMissingStartTag = addComboBox(fMarkupValidationGroup, XMLUIMessages.Missing_start_tag,
          XMLCorePreferenceNames.MISSING_START_TAG, XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    if (fMissingEndTag == null)
      fMissingEndTag = addComboBox(fMarkupValidationGroup, XMLUIMessages.Missing_end_tag,
          XMLCorePreferenceNames.MISSING_END_TAG, XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    if (fMissingTagName == null)
      fMissingTagName = addComboBox(fMarkupValidationGroup, XMLUIMessages.Tag_name_missing,
          XMLCorePreferenceNames.MISSING_TAG_NAME, XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    if (fMissingQuotes == null)
      fMissingQuotes = addComboBox(fMarkupValidationGroup, XMLUIMessages.Missing_quotes,
          XMLCorePreferenceNames.MISSING_QUOTES, XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    if (fMissingClosingBracket == null)
      fMissingClosingBracket = addComboBox(fMarkupValidationGroup,
          XMLUIMessages.Missing_closing_bracket, XMLCorePreferenceNames.MISSING_CLOSING_BRACKET,
          XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    if (fMissingClosingQuote == null)
      fMissingClosingQuote = addComboBox(fMarkupValidationGroup,
          XMLUIMessages.Missing_closing_quote, XMLCorePreferenceNames.MISSING_CLOSING_QUOTE,
          XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    if (fEmptyElementTag == null)
      fEmptyElementTag = addComboBox(fMarkupValidationGroup, XMLUIMessages.Empty_element_tag,
          XMLCorePreferenceNames.ATTRIBUTE_HAS_NO_VALUE, XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    if (fEndTagWithAttributes == null)
      fEndTagWithAttributes = addComboBox(fMarkupValidationGroup,
          XMLUIMessages.End_tag_with_attributes, XMLCorePreferenceNames.END_TAG_WITH_ATTRIBUTES,
          XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    if (fInvalidWhitespaceBeforeTagname == null)
      fInvalidWhitespaceBeforeTagname = addComboBox(fMarkupValidationGroup,
          XMLUIMessages.Invalid_whitespace_before_tagname,
          XMLCorePreferenceNames.WHITESPACE_BEFORE_TAGNAME, XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    if (fInvalidNamespaceInPI == null)
      fInvalidNamespaceInPI = addComboBox(fMarkupValidationGroup,
          XMLUIMessages.Namespace_in_pi_target, XMLCorePreferenceNames.NAMESPACE_IN_PI_TARGET,
          XML_SEVERITIES, MARKUP_SEVERITIES, 0);
    if (fInvalidWhitespaceAtStart == null)
      fInvalidWhitespaceAtStart = addComboBox(fMarkupValidationGroup,
          XMLUIMessages.Whitespace_at_start, XMLCorePreferenceNames.WHITESPACE_AT_START,
          XML_SEVERITIES, MARKUP_SEVERITIES, 0);

    handleMarkupSeveritySelection(fOriginalUseExtendedMarkupValidation);

  }

  protected void performDefaultsForValidatingGroup() {
    IEclipsePreferences modelPreferences = new DefaultScope().getNode(getPreferenceNodeQualifier());
    boolean useXIncludeButtonSelected = modelPreferences.getBoolean(
        XMLCorePreferenceNames.USE_XINCLUDE, true);

    if (fUseXinclude != null) {
      fUseXinclude.setSelection(useXIncludeButtonSelected);
    }
    boolean useHonourAllButtonSelected = modelPreferences.getBoolean(
        XMLCorePreferenceNames.HONOUR_ALL_SCHEMA_LOCATIONS, true);
    if (fHonourAllSchemaLocations != null) {
      fHonourAllSchemaLocations.setSelection(useHonourAllButtonSelected);
    }
  }

  protected void performDefaultsForMarkupValidationGroup() {
    IEclipsePreferences modelPreferences = new DefaultScope().getNode(getPreferenceNodeQualifier());
    boolean useExtendedMarkupValidation = modelPreferences.getBoolean(
        XMLCorePreferenceNames.MARKUP_VALIDATION, false);

    if (fExtendedMarkupValidation != null) {
      if (fExtendedMarkupValidation.getSelection() != useExtendedMarkupValidation) {
        handleMarkupSeveritySelection(useExtendedMarkupValidation);
      }
      fExtendedMarkupValidation.setSelection(useExtendedMarkupValidation);
    }
  }

  protected void storeValuesForValidatingGroup(IScopeContext[] contexts) {
    if (fUseXinclude != null) {
      boolean useXIncludeButtonSelected = fUseXinclude.getSelection();
      contexts[0].getNode(getPreferenceNodeQualifier()).putBoolean(
          XMLCorePreferenceNames.USE_XINCLUDE, useXIncludeButtonSelected);
    }
    if (fHonourAllSchemaLocations != null) {
      boolean honourAllButtonSelected = fHonourAllSchemaLocations.getSelection();
      contexts[0].getNode(getPreferenceNodeQualifier()).putBoolean(
          XMLCorePreferenceNames.HONOUR_ALL_SCHEMA_LOCATIONS, honourAllButtonSelected);
    }
  }

  protected void storeValuesForMarkupValidationGroup(IScopeContext[] contexts) {
    if (fExtendedMarkupValidation != null) {
      boolean extendedMarkupValidation = fExtendedMarkupValidation.getSelection();
      contexts[0].getNode(getPreferenceNodeQualifier()).putBoolean(
          XMLCorePreferenceNames.MARKUP_VALIDATION, extendedMarkupValidation);
    }
  }

  protected void performDefaults() {
    resetSeverities();
    performDefaultsForValidatingGroup();
    performDefaultsForMarkupValidationGroup();
    super.performDefaults();
  }

  protected Preferences getModelPreferences() {
    return XMLCorePlugin.getDefault().getPluginPreferences();
  }

  protected void doSavePreferenceStore() {
    XMLCorePlugin.getDefault().savePluginPreferences(); // model
  }

  protected void storeValues() {
    super.storeValues();
    IScopeContext[] contexts = createPreferenceScopes();

    storeValuesForValidatingGroup(contexts);
    storeValuesForMarkupValidationGroup(contexts);
  }

  protected Control createCommonContents(Composite parent) {
    final Composite page = new Composite(parent, SWT.NULL);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    page.setLayout(layout);

    Group validatingGroup = createGroup(page, 3);
    validatingGroup.setText(XMLUIMessages.Validating_files);
    createContentsForValidatingGroup(validatingGroup);

    createContentsForMarkupValidationGroup(page);

    return page;
  }

  protected String getPreferenceNodeQualifier() {
    return XMLCorePlugin.getDefault().getBundle().getSymbolicName();
  }

  protected String getPreferencePageID() {
    return "org.eclipse.wst.sse.ui.preferences.xml.validation";//$NON-NLS-1$
  }

  protected String getProjectSettingsKey() {
    return XMLCorePreferenceNames.USE_PROJECT_SETTINGS;
  }

  protected String getPropertyPageID() {
    return "org.eclipse.wst.xml.ui.propertyPage.project.validation";//$NON-NLS-1$
  }

  public void init(IWorkbench workbench) {
  }

  private Group createGroup(Composite parent, int numColumns) {

    Group group = new Group(parent, SWT.NULL);

    // GridLayout
    GridLayout layout = new GridLayout();
    layout.numColumns = numColumns;
    group.setLayout(layout);

    // GridData
    GridData data = new GridData(GridData.FILL);
    data.horizontalIndent = 0;
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    group.setLayoutData(data);

    return group;
  }

  private Button createCheckBox(Composite group, String label) {
    Button button = new Button(group, SWT.CHECK | SWT.LEFT);
    button.setText(label);

    // button.setLayoutData(GridDataFactory.fillDefaults().create());

    // GridData
    GridData data = new GridData(GridData.FILL);
    data.verticalAlignment = GridData.CENTER;
    data.horizontalAlignment = GridData.FILL;
    button.setLayoutData(data);

    return button;
  }

  public void dispose() {
    storeSectionExpansionStates(getDialogSettings().addNewSection(SETTINGS_SECTION_NAME));
    super.dispose();
  }

  protected IDialogSettings getDialogSettings() {
    return XMLUIPlugin.getDefault().getDialogSettings();
  }

  protected boolean shouldRevalidateOnSettingsChange() {
    return fOriginalUseExtendedMarkupValidation != fExtendedMarkupValidation.getSelection()
        || fOriginalUseXIncludeButtonSelected != fUseXinclude.getSelection()
        || fOriginalUseHonourAllButtonSelected != fHonourAllSchemaLocations.getSelection()
        || super.shouldRevalidateOnSettingsChange();
  }
}
