/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.preferences;

import java.util.Vector;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.contentassist.CompletionProposoalCatigoriesConfigurationRegistry;
import org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage;
import org.eclipse.wst.sse.ui.preferences.CodeAssistCyclingConfigurationBlock;
import org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

/**
 * <p>
 * Defines the preference page for allowing the user to change the content assist preferences
 * </p>
 */
public class XMLContentAssistPreferencePage extends AbstractPreferencePage implements
    IWorkbenchPreferencePage {

  private static final String XML_CONTENT_TYPE_ID = "org.eclipse.core.runtime.xml"; //$NON-NLS-1$

  // Auto Activation
  private Button fAutoPropose;
  private Label fAutoProposeDelayLabel;
  private Text fAutoProposeDelay;
  private Label fAutoProposeLabel;
  private Text fAutoProposeText;
  private Combo fSuggestionStrategyCombo;
  private Vector fSuggestionStrategies = null;

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

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#initializeValues()
   */
  protected void initializeValues() {
    initializeValuesForAutoActivationGroup();
    initializeValuesForCyclingGroup();
    initializeValuesForInsertionGroup();
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage#storeValues()
   */
  protected void storeValues() {
    storeValuesForAutoActivationGroup();
    storeValuesForCyclingGroup();
    storeValuesForInsertionGroup();
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
    return XMLUIPlugin.getDefault().getPreferenceStore();
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
    contentAssistGroup.setText(XMLUIMessages.XMLContentAssistPreferencePage_Auto_Activation_UI_);

    fAutoPropose = createCheckBox(contentAssistGroup, XMLUIMessages.Automatically_make_suggest_UI_);
    ((GridData) fAutoPropose.getLayoutData()).horizontalSpan = 2;
    fAutoPropose.addSelectionListener(this);

    fAutoProposeDelayLabel = createLabel(contentAssistGroup, XMLUIMessages.Auto_Activation_Delay);
    fAutoProposeDelay = createTextField(contentAssistGroup);
    fAutoProposeDelay.setTextLimit(4);
    fAutoProposeDelay.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        verifyDelay();
      }
    });

    fAutoProposeLabel = createLabel(contentAssistGroup,
        XMLUIMessages.Prompt_when_these_characte_UI_);
    fAutoProposeText = createTextField(contentAssistGroup);

    createLabel(contentAssistGroup, XMLUIMessages.Suggestion_Strategy);
    fSuggestionStrategyCombo = new Combo(contentAssistGroup, SWT.READ_ONLY);
    fSuggestionStrategies = new Vector();
    fSuggestionStrategyCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fSuggestionStrategyCombo.add(XMLUIMessages.Suggestion_Strategy_Lax);
    fSuggestionStrategies.add(XMLUIPreferenceNames.SUGGESTION_STRATEGY_VALUE_LAX);
    fSuggestionStrategyCombo.add(XMLUIMessages.Suggestion_Strategy_Strict);
    fSuggestionStrategies.add(XMLUIPreferenceNames.SUGGESTION_STRATEGY_VALUE_STRICT);
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
        XML_CONTENT_TYPE_ID);

    if (configurationWriter != null) {
      fConfigurationBlock = new CodeAssistCyclingConfigurationBlock(XML_CONTENT_TYPE_ID,
          configurationWriter);
      fConfigurationBlock.createContents(parent,
          XMLUIMessages.XMLContentAssistPreferencePage_Cycling_UI_);
    } else {
      Logger.log(Logger.ERROR,
          "There should be an ICompletionProposalCategoriesConfigurationWriter" + //$NON-NLS-1$
              " specified for the XML content type, but can't fine it, thus can't create user" + //$NON-NLS-1$
              " preference block for editing proposal categories preferences."); //$NON-NLS-1$
    }
  }

  /**
   * <p>
   * Store the values for the auto activation group
   * </p>
   */
  private void storeValuesForAutoActivationGroup() {
    getPreferenceStore().setValue(XMLUIPreferenceNames.AUTO_PROPOSE, fAutoPropose.getSelection());
    getPreferenceStore().setValue(XMLUIPreferenceNames.AUTO_PROPOSE_CODE,
        fAutoProposeText.getText());
    getPreferenceStore().setValue(XMLUIPreferenceNames.SUGGESTION_STRATEGY,
        getCurrentAutoActivationSuggestionStrategy());
    getPreferenceStore().setValue(XMLUIPreferenceNames.AUTO_PROPOSE_DELAY,
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

  private void storeValuesForInsertionGroup() {
    getPreferenceStore().setValue(XMLUIPreferenceNames.INSERT_SINGLE_SUGGESTION,
        (fInsertSingleProposals != null) ? fInsertSingleProposals.getSelection() : false);
  }

  /**
   * <p>
   * Initialize the values for the auto activation group
   * </p>
   */
  private void initializeValuesForAutoActivationGroup() {
    fAutoPropose.setSelection(getPreferenceStore().getBoolean(XMLUIPreferenceNames.AUTO_PROPOSE));
    fAutoProposeText.setText(getPreferenceStore().getString(XMLUIPreferenceNames.AUTO_PROPOSE_CODE));
    fAutoProposeDelay.setText(Integer.toString(getPreferenceStore().getInt(
        XMLUIPreferenceNames.AUTO_PROPOSE_DELAY)));
    String suggestionStrategy = getPreferenceStore().getString(
        XMLUIPreferenceNames.SUGGESTION_STRATEGY);
    if (suggestionStrategy.length() > 0) {
      setCurrentAutoActivationSuggestionStrategy(suggestionStrategy);
    } else {
      setCurrentAutoActivationSuggestionStrategy(XMLUIPreferenceNames.SUGGESTION_STRATEGY_VALUE_LAX);
    }
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
    initCheckbox(fInsertSingleProposals, XMLUIPreferenceNames.INSERT_SINGLE_SUGGESTION);
  }

  /**
   * <p>
   * Load the defaults for the auto activation group
   * </p>
   */
  private void performDefaultsForAutoActivationGroup() {
    fAutoPropose.setSelection(getPreferenceStore().getDefaultBoolean(
        XMLUIPreferenceNames.AUTO_PROPOSE));
    fAutoProposeText.setText(getPreferenceStore().getDefaultString(
        XMLUIPreferenceNames.AUTO_PROPOSE_CODE));
    fAutoProposeDelay.setText(Integer.toString(getPreferenceStore().getDefaultInt(
        XMLUIPreferenceNames.AUTO_PROPOSE_DELAY)));
    String suggestionStrategy = getPreferenceStore().getDefaultString(
        XMLUIPreferenceNames.SUGGESTION_STRATEGY);
    if (suggestionStrategy.length() > 0) {
      setCurrentAutoActivationSuggestionStrategy(suggestionStrategy);
    } else {
      setCurrentAutoActivationSuggestionStrategy(XMLUIPreferenceNames.SUGGESTION_STRATEGY_VALUE_LAX);
    }
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
    defaultCheckbox(fInsertSingleProposals, XMLUIPreferenceNames.INSERT_SINGLE_SUGGESTION);
  }

  /**
   * Return the currently selected suggestion strategy preference
   * 
   * @return a suggestion strategy constant from XMLUIPreferenceNames
   */
  private String getCurrentAutoActivationSuggestionStrategy() {
    int i = fSuggestionStrategyCombo.getSelectionIndex();
    if (i >= 0) {
      return (String) (fSuggestionStrategies.elementAt(i));
    }
    return ""; //$NON-NLS-1$
  }

  /**
   * Set a suggestion strategy in suggestion strategy combo box
   * 
   * @param strategy
   */
  private void setCurrentAutoActivationSuggestionStrategy(String strategy) {
    // Clear the current selection.
    fSuggestionStrategyCombo.clearSelection();
    fSuggestionStrategyCombo.deselectAll();

    int i = fSuggestionStrategies.indexOf(strategy);
    if (i >= 0) {
      fSuggestionStrategyCombo.select(i);
    }
  }
}
