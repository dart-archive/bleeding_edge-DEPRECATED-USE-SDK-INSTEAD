/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.preferences.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.css.ui.internal.preferences.CSSUIPreferenceNames;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.contentassist.CompletionProposoalCatigoriesConfigurationRegistry;
import org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage;
import org.eclipse.wst.sse.ui.preferences.CodeAssistCyclingConfigurationBlock;
import org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

/**
 * <p>
 * Defines the preference page for allowing the user to change the content assist preferences
 * </p>
 */
public class CSSContentAssistPreferencePage extends AbstractPreferencePage implements
    IWorkbenchPreferencePage {

  private static final String CSS_CONTENT_TYPE_ID = "org.eclipse.wst.css.core.csssource"; //$NON-NLS-1$

  /** configuration block for changing preference having to do with the content assist categories */
  private CodeAssistCyclingConfigurationBlock fConfigurationBlock;

  private Button fInsertSingleProposals;

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  protected Control createContents(Composite parent) {
    final Composite composite = super.createComposite(parent, 1);

    createContentsForInsertionGroup(composite);
    createContentsForCyclingGroup(composite);

    setSize(composite);
    loadPreferences();

    return composite;
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#performDefaults()
   */
  protected void performDefaults() {
    performDefaultsForCyclingGroup();
    performDefaultsForInsertionGroup();

    validateValues();
    enableValues();

    super.performDefaults();
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#initializeValues()
   */
  protected void initializeValues() {
    initializeValuesForCyclingGroup();
    initializeValuesForInsertionGroup();
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#storeValues()
   */
  protected void storeValues() {
    storeValuesForCyclingGroup();
    storeValuesForInsertionGroup();
  }

  /**
   * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
   */
  protected IPreferenceStore doGetPreferenceStore() {
    return CSSUIPlugin.getDefault().getPreferenceStore();
  }

  private void createContentsForInsertionGroup(Composite composite) {
    Group group = createGroup(composite, 2);

    group.setText(XMLUIMessages.Group_label_Insertion);

    fInsertSingleProposals = createCheckBox(group, XMLUIMessages.Insert_single_proposals);
    ((GridData) fInsertSingleProposals.getLayoutData()).horizontalSpan = 2;
  }

  /**
   * <p>
   * Create the contents for the content assist cycling preference group
   * </p>
   * 
   * @param parent {@link Composite} parent of the group
   */
  private void createContentsForCyclingGroup(Composite parent) {
    ICompletionProposalCategoriesConfigurationWriter configurationWriter = CompletionProposoalCatigoriesConfigurationRegistry.getDefault().getWritableConfiguration(
        CSS_CONTENT_TYPE_ID);

    if (configurationWriter != null) {
      fConfigurationBlock = new CodeAssistCyclingConfigurationBlock(CSS_CONTENT_TYPE_ID,
          configurationWriter);
      fConfigurationBlock.createContents(parent,
          XMLUIMessages.XMLContentAssistPreferencePage_Cycling_UI_); //$NON-NLS-1$
    } else {
      Logger.log(Logger.ERROR,
          "There should be an ICompletionProposalCategoriesConfigurationWriter" + //$NON-NLS-1$
              " specified for the CSS content type, but can't fine it, thus can't create user" + //$NON-NLS-1$
              " preference block for editing proposal categories preferences."); //$NON-NLS-1$
    }
  }

  /**
   * <p>
   * Store the values for the cycling group
   * </p>
   */
  private void storeValuesForCyclingGroup() {
    if (fConfigurationBlock != null) {
      fConfigurationBlock.storeValues();
    }
  }

  private void storeValuesForInsertionGroup() {
    getPreferenceStore().setValue(CSSUIPreferenceNames.INSERT_SINGLE_SUGGESTION,
        (fInsertSingleProposals != null) ? fInsertSingleProposals.getSelection() : false);
  }

  /**
   * <p>
   * Initialize the values for the cycling group
   * </p>
   */
  private void initializeValuesForCyclingGroup() {
    if (fConfigurationBlock != null) {
      fConfigurationBlock.initializeValues();
    }
  }

  private void initializeValuesForInsertionGroup() {
    initCheckbox(fInsertSingleProposals, CSSUIPreferenceNames.INSERT_SINGLE_SUGGESTION);
  }

  /**
   * <p>
   * Load the defaults of the cycling group
   * </p>
   */
  private void performDefaultsForCyclingGroup() {
    if (fConfigurationBlock != null) {
      fConfigurationBlock.performDefaults();
    }
  }

  private void performDefaultsForInsertionGroup() {
    defaultCheckbox(fInsertSingleProposals, CSSUIPreferenceNames.INSERT_SINGLE_SUGGESTION);
  }
}
