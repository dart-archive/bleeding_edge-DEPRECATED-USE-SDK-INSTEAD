/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentassist;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.html.ui.internal.preferences.HTMLUIPreferenceNames;
import org.eclipse.wst.sse.ui.contentassist.StructuredContentAssistProcessor;
import org.eclipse.wst.xml.ui.internal.contentassist.AttributeContextInformationPresenter;

/**
 * <p>
 * Implementation of {@link StructuredContentAssistProcessor} for HTML documents
 * </p>
 * <p>
 * This implementation will react to user preference changes for auto activation characters for HTML
 * pages
 * </p>
 */
public class HTMLStructuredContentAssistProcessor extends StructuredContentAssistProcessor {
  /** the auto activation characters for this processor, set by user preference */
  private char[] fCompletionProposalAutoActivationCharacters;

  /** the context information validator for this processor */
  private IContextInformationValidator fContextInformationValidator;

  /**
   * <p>
   * Constructor
   * </p>
   * 
   * @param assistant {@link ContentAssistant} to use
   * @param partitionTypeID the partition type this processor is for
   * @param viewer {@link ITextViewer} this processor is acting in
   */
  public HTMLStructuredContentAssistProcessor(ContentAssistant assistant, String partitionTypeID,
      ITextViewer viewer) {

    super(assistant, partitionTypeID, viewer, HTMLUIPlugin.getDefault().getPreferenceStore());
    getAutoActivationCharacterPreferences();
    updateAutoActivationDelay();
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.StructuredContentAssistProcessor#getContextInformationValidator()
   */
  public IContextInformationValidator getContextInformationValidator() {
    if (this.fContextInformationValidator == null) {
      this.fContextInformationValidator = new AttributeContextInformationPresenter();
    }
    return this.fContextInformationValidator;
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.StructuredContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
   */
  public char[] getCompletionProposalAutoActivationCharacters() {
    return super.getCompletionProposalAutoActivationCharacters() != null
        ? super.getCompletionProposalAutoActivationCharacters()
        : this.fCompletionProposalAutoActivationCharacters;
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.StructuredContentAssistProcessor#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getProperty().equals(HTMLUIPreferenceNames.AUTO_PROPOSE)
        || event.getProperty().equals(HTMLUIPreferenceNames.AUTO_PROPOSE_CODE)) {
      getAutoActivationCharacterPreferences();
    } else if (event.getProperty().equals(HTMLUIPreferenceNames.AUTO_PROPOSE_DELAY)) {
      updateAutoActivationDelay();
    }
  }

  /**
   * <p>
   * Sets the auto activation delay in Content Assist
   * </p>
   */
  private void updateAutoActivationDelay() {
    IPreferenceStore store = getPreferenceStore();
    boolean doAuto = store.getBoolean(HTMLUIPreferenceNames.AUTO_PROPOSE);
    if (doAuto) {
      setAutoActivationDelay(store.getInt(HTMLUIPreferenceNames.AUTO_PROPOSE_DELAY));
    }
  }

  /**
   * <p>
   * Gets the auto activation character user preferences and stores them for later use
   * </p>
   */
  private void getAutoActivationCharacterPreferences() {
    String key = HTMLUIPreferenceNames.AUTO_PROPOSE;
    boolean doAuto = getPreferenceStore().getBoolean(key);
    if (doAuto) {
      key = HTMLUIPreferenceNames.AUTO_PROPOSE_CODE;
      this.fCompletionProposalAutoActivationCharacters = getPreferenceStore().getString(key).toCharArray();
    } else {
      this.fCompletionProposalAutoActivationCharacters = null;
    }
  }
}
