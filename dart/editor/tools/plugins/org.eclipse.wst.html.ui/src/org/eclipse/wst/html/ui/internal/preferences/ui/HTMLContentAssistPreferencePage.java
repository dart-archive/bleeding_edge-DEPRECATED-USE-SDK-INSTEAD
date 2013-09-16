/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.preferences.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.html.ui.internal.preferences.HTMLUIPreferenceNames;
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
public class HTMLContentAssistPreferencePage extends AbstractPreferencePage implements
    IWorkbenchPreferencePage {

  private static final String HTML_CONTENT_TYPE_ID = "org.eclipse.wst.html.core.htmlsource"; //$NON-NLS-1$

  // Auto Activation
  private Button fAutoPropose;
  private Label fAutoProposeDelayLabel;
  private Text fAutoProposeDelay;
  private Label fAutoProposeLabel;
  private Text fAutoProposeText;

  /** configuration block for changing preference having to do with the content assist categories */
  private CodeAssistCyclingConfigurationBlock fConfigurationBlock;

  private Button fInsertSingleProposals;

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  protected Control createContents(Composite parent) {
    final Composite composite = super.createComposite(parent, 1);

    createContentsForInsertionGroup(composite);
    createContentsForAutoActivationGroup(composite);
    createContentsForCyclingGroup(composite);

    setSize(composite);
    loadPreferences();

    return composite;
  }

  private void createContentsForInsertionGroup(Composite composite) {
    Group group = createGroup(composite, 2);

    group.setText(XMLUIMessages.Group_label_Insertion);

    fInsertSingleProposals = createCheckBox(group, XMLUIMessages.Insert_single_proposals);
    ((GridData) fInsertSingleProposals.getLayoutData()).horizontalSpan = 2;
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#performDefaults()
   */
  protected void performDefaults() {
    performDefaultsForAutoActivationGroup();
    performDefaultsForCyclingGroup();
    performDefaultsForInsertionGroup();

    validateValues();
    enableValues();

    super.performDefaults();
  }

  private void performDefaultsForInsertionGroup() {
    defaultCheckbox(fInsertSingleProposals, HTMLUIPreferenceNames.INSERT_SINGLE_SUGGESTION);
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#initializeValues()
   */
  protected void initializeValues() {
    initializeValuesForAutoActivationGroup();
    initializeValuesForCyclingGroup();
    initializeValuesForInsertionGroup();
  }

  private void initializeValuesForInsertionGroup() {
    initCheckbox(fInsertSingleProposals, HTMLUIPreferenceNames.INSERT_SINGLE_SUGGESTION);
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#storeValues()
   */
  protected void storeValues() {
    storeValuesForAutoActivationGroup();
    storeValuesForCyclingGroup();
    storeValuesForInsertionGroup();
  }

  private void storeValuesForInsertionGroup() {
    getPreferenceStore().setValue(HTMLUIPreferenceNames.INSERT_SINGLE_SUGGESTION,
        (fInsertSingleProposals != null) ? fInsertSingleProposals.getSelection() : false);
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#enableValues()
   */
  protected void enableValues() {
    if (fAutoPropose != null) {
      if (fAutoPropose.getSelection()) {
        fAutoProposeDelayLabel.setEnabled(true);
        fAutoProposeDelay.setEnabled(true);
        fAutoProposeLabel.setEnabled(true);
        fAutoProposeText.setEnabled(true);
      } else {
        fAutoProposeDelayLabel.setEnabled(false);
        fAutoProposeDelay.setEnabled(false);
        fAutoProposeLabel.setEnabled(false);
        fAutoProposeText.setEnabled(false);
      }
    }
  }

  /**
   * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
   */
  protected IPreferenceStore doGetPreferenceStore() {
    return HTMLUIPlugin.getDefault().getPreferenceStore();
  }

  /**
   * <p>
   * Create contents for the auto activation preference group
   * </p>
   * 
   * @param parent {@link Composite} parent of the group
   */
  private void createContentsForAutoActivationGroup(Composite parent) {
    Group contentAssistGroup = createGroup(parent, 2);
    contentAssistGroup.setText(HTMLUIMessages.Auto_Activation_UI_);

    fAutoPropose = createCheckBox(contentAssistGroup, HTMLUIMessages.Automatically_make_suggest_UI_);
    ((GridData) fAutoPropose.getLayoutData()).horizontalSpan = 2;
    fAutoPropose.addSelectionListener(this);

    fAutoProposeDelayLabel = createLabel(contentAssistGroup, HTMLUIMessages.Auto_Activation_Delay);
    fAutoProposeDelay = createTextField(contentAssistGroup);
    fAutoProposeDelay.setTextLimit(4);
    fAutoProposeDelay.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        verifyDelay();
      }
    });

    fAutoProposeLabel = createLabel(contentAssistGroup,
        HTMLUIMessages.Prompt_when_these_characte_UI_);
    fAutoProposeText = createTextField(contentAssistGroup);
  }

  private void verifyDelay() {
    final String text = fAutoProposeDelay.getText();
    boolean valid = true;
    try {
      final int delay = Integer.parseInt(text);
      if (delay < 0) {
        valid = false;
      }
    } catch (NumberFormatException e) {
      valid = false;
    }
    if (!valid) {
      if (text.trim().length() > 0)
        setErrorMessage(NLS.bind(XMLUIMessages.Not_an_integer, text));
      else
        setErrorMessage(XMLUIMessages.Missing_integer);
      setValid(false);
    } else {
      setErrorMessage(null);
      setValid(true);
    }
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
        HTML_CONTENT_TYPE_ID);

    if (configurationWriter != null) {
      fConfigurationBlock = new CodeAssistCyclingConfigurationBlock(HTML_CONTENT_TYPE_ID,
          configurationWriter);
      fConfigurationBlock.createContents(parent, HTMLUIMessages.Cycling_UI_);
    } else {
      Logger.log(Logger.ERROR,
          "There should be an ICompletionProposalCategoriesConfigurationWriter" + //$NON-NLS-1$
              " specified for the HTML content type, but can't fine it, thus can't create user" + //$NON-NLS-1$
              " preference block for editing proposal categories preferences."); //$NON-NLS-1$
    }
  }

  /**
   * <p>
   * Store the values for the auto activation group
   * </p>
   */
  private void storeValuesForAutoActivationGroup() {
    getPreferenceStore().setValue(HTMLUIPreferenceNames.AUTO_PROPOSE, fAutoPropose.getSelection());
    getPreferenceStore().setValue(HTMLUIPreferenceNames.AUTO_PROPOSE_CODE,
        fAutoProposeText.getText());
    getPreferenceStore().setValue(HTMLUIPreferenceNames.AUTO_PROPOSE_DELAY,
        Integer.parseInt(fAutoProposeDelay.getText()));
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

  /**
   * <p>
   * Initialize the values for the auto activation group
   * </p>
   */
  private void initializeValuesForAutoActivationGroup() {
    // Content Assist
    fAutoPropose.setSelection(getPreferenceStore().getBoolean(HTMLUIPreferenceNames.AUTO_PROPOSE));
    fAutoProposeText.setText(getPreferenceStore().getString(HTMLUIPreferenceNames.AUTO_PROPOSE_CODE));
    fAutoProposeDelay.setText(Integer.toString(getPreferenceStore().getInt(
        HTMLUIPreferenceNames.AUTO_PROPOSE_DELAY)));
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

  /**
   * <p>
   * Load the defaults for the auto activation group
   * </p>
   */
  private void performDefaultsForAutoActivationGroup() {
    // Content Assist
    fAutoPropose.setSelection(getPreferenceStore().getDefaultBoolean(
        HTMLUIPreferenceNames.AUTO_PROPOSE));
    fAutoProposeText.setText(getPreferenceStore().getDefaultString(
        HTMLUIPreferenceNames.AUTO_PROPOSE_CODE));
    fAutoProposeDelay.setText(Integer.toString(getPreferenceStore().getDefaultInt(
        HTMLUIPreferenceNames.AUTO_PROPOSE_DELAY)));
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
}
