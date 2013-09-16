/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.sse.ui.preferences.AbstractCompletionProposalCategoriesConfiguration;

/**
 * <p>
 * The readable and writable completion proposal categories configuration for the CSS content type
 * </p>
 */
public class CSSCompletionProposalCategoriesConfiguration extends
    AbstractCompletionProposalCategoriesConfiguration {

  /** the ID of the preference page where users can change the preferences */
  private static final String PREFERENCES_PAGE_ID = "org.eclipse.wst.css.ui.preferences.contentassist"; //$NON-NLS-1$

  /**
   * <p>
   * Creates the configuration
   * </p>
   */
  public CSSCompletionProposalCategoriesConfiguration() {
    //nothing to do.
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.AbstractCompletionProposalCategoriesConfiguration#getPreferenceStore()
   */
  protected IPreferenceStore getPreferenceStore() {
    return CSSUIPlugin.getDefault().getPreferenceStore();
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter#hasAssociatedPropertiesPage()
   */
  public boolean hasAssociatedPropertiesPage() {
    return true;
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter#getPropertiesPageID()
   */
  public String getPropertiesPageID() {
    return PREFERENCES_PAGE_ID;
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.AbstractCompletionProposalCategoriesConfiguration#getShouldNotDisplayOnDefaultPagePrefKey()
   */
  protected String getShouldNotDisplayOnDefaultPagePrefKey() {
    return CSSUIPreferenceNames.CONTENT_ASSIST_DO_NOT_DISPLAY_ON_DEFAULT_PAGE;
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.AbstractCompletionProposalCategoriesConfiguration#getShouldNotDisplayOnOwnPagePrefKey()
   */
  protected String getShouldNotDisplayOnOwnPagePrefKey() {
    return CSSUIPreferenceNames.CONTENT_ASSIST_DO_NOT_DISPLAY_ON_OWN_PAGE;
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.AbstractCompletionProposalCategoriesConfiguration#getPageSortOrderPrefKey()
   */
  protected String getPageSortOrderPrefKey() {
    return CSSUIPreferenceNames.CONTENT_ASSIST_OWN_PAGE_SORT_ORDER;
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.AbstractCompletionProposalCategoriesConfiguration#getDefaultPageSortOrderPrefKey()
   */
  protected String getDefaultPageSortOrderPrefKey() {
    return CSSUIPreferenceNames.CONTENT_ASSIST_DEFAULT_PAGE_SORT_ORDER;
  }
}
