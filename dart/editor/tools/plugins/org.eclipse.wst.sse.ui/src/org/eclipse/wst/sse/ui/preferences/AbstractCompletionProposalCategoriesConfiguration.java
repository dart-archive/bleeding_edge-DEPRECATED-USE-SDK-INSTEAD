/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.preferences;

import com.ibm.icu.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.sse.ui.internal.contentassist.CompletionProposalCategory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Implements a completion proposal categories configuration reader and writer using an
 * {@link IPreferenceStore}
 * </p>
 * <p>
 * This class is meant to be subclasses by implementers of the
 * <code>org.eclipse.wst.sse.ui.completionProposalCategoriesConfiguration</code> extension point as
 * a convince rather then implementing {@link ICompletionProposalCategoriesConfigurationReader} and
 * {@link ICompletionProposalCategoriesConfigurationWriter} from scratch.
 * </p>
 */
public abstract class AbstractCompletionProposalCategoriesConfiguration implements
    ICompletionProposalCategoriesConfigurationReader,
    ICompletionProposalCategoriesConfigurationWriter {

  /** Separator used between preferences stored in the same key */
  private static final String PREFERENCE_CATEGORY_SEPERATOR = "\0"; //$NON-NLS-1$

  /**
   * <p>
   * {@link CompletionProposalCategory} IDs sorted by the order they should be listed on the default
   * page if they are being displayed there
   * </p>
   * <code>{@link List}<{@link String}></code>
   * <ul>
   * <li><b>values:</b> {@link CompletionProposalCategory} IDs</li>
   * </ul>
   */
  private List fDefaultPageSortOrder;

  /**
   * <p>
   * {@link CompletionProposalCategory} IDs sorted by the order they should be cycled through
   * </p>
   * <code>{@link List}<{@link String}></code>
   * <ul>
   * <li><b>values:</b> {@link CompletionProposalCategory} IDs</li>
   * </ul>
   */
  private List fOwnPageSortOrder;

  /**
   * <p>
   * {@link CompletionProposalCategory} IDs that should not be displayed on their own page
   * </p>
   * <code>{@link Set}<{@link String}></code>
   * <ul>
   * <li><b>values:</b> {@link CompletionProposalCategory} IDs</li>
   * </ul>
   */
  private Set fShouldNotDisplayOnOwnPage;

  /**
   * <p>
   * {@link CompletionProposalCategory} IDs that should not be displayed on on the default page
   * </p>
   * <code>{@link Set}<{@link String}></code>
   * <ul>
   * <li><b>values:</b> {@link CompletionProposalCategory} IDs</li>
   * </ul>
   */
  private Set fShouldNotDisplayOnDefaultPage;

  /**
   * <p>
   * Create a new configuration by loading from the associated {@link IPreferenceStore}
   * </p>
   */
  public AbstractCompletionProposalCategoriesConfiguration() {
    this.fOwnPageSortOrder = new ArrayList();
    this.fDefaultPageSortOrder = new ArrayList();
    this.fShouldNotDisplayOnOwnPage = new HashSet();
    this.fShouldNotDisplayOnDefaultPage = new HashSet();

    this.loadUserConfiguration();
  }

  /**
   * @return {@link IPreferenceStore} to read and write the configuration to and from
   */
  protected abstract IPreferenceStore getPreferenceStore();

  /**
   * @return Preference key to use in the {@link IPreferenceStore} for the category sort order when
   *         cycling through the pages
   */
  protected abstract String getPageSortOrderPrefKey();

  /**
   * @return Preference key to use in the {@link IPreferenceStore} for the category sort order on
   *         the default page
   */
  protected abstract String getDefaultPageSortOrderPrefKey();

  /**
   * @return Preference key to use in the {@link IPreferenceStore} for which categories not to
   *         display on their own page
   */
  protected abstract String getShouldNotDisplayOnOwnPagePrefKey();

  /**
   * @return Preference key to use in the {@link IPreferenceStore} for which categories not to
   *         display on the default page
   */
  protected abstract String getShouldNotDisplayOnDefaultPagePrefKey();

  /**
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationReader#getPageSortOrder(java.lang.String)
   */
  public int getPageSortOrder(String categoryID) {
    int sortOrder = this.fOwnPageSortOrder.indexOf(categoryID);
    if (sortOrder == -1) {
      sortOrder = DEFAULT_SORT_ORDER;
    }

    return sortOrder;
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationReader#getDefaultPageSortOrder(java.lang.String)
   */
  public int getDefaultPageSortOrder(String categoryID) {
    int sortOrder = this.fDefaultPageSortOrder.indexOf(categoryID);
    if (sortOrder == -1) {
      sortOrder = DEFAULT_SORT_ORDER;
    }

    return sortOrder;
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationReader#shouldDisplayOnDefaultPage(java.lang.String)
   */
  public boolean shouldDisplayOnDefaultPage(String categoryID) {
    return !fShouldNotDisplayOnDefaultPage.contains(categoryID);
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationReader#shouldDisplayOnOwnPage(java.lang.String)
   */
  public boolean shouldDisplayOnOwnPage(String categoryID) {
    return !this.fShouldNotDisplayOnOwnPage.contains(categoryID);
  }

  /**
   * <p>
   * Loads defaults from the associated {@link IPreferenceStore}
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter#loadDefaults()
   */
  public void loadDefaults() {
    this.loadDefaultPagePreference(true);
    this.loadPageSortOrder(true);
    this.loadDefaultPageSortOrder(true);
    this.loadShouldNotDisplayOnOwnPage(true);
  }

  /**
   * <p>
   * Saves to the associated {@link IPreferenceStore}
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter#saveConfiguration()
   */
  public boolean saveConfiguration() {
    this.saveShouldDisplayOnDefaultPageConfiguration();
    this.saveShouldDisplayOnOwnPageConfiguration();
    this.saveDefaultPageSortOrderConfiguration();
    this.savePageSortOrderConfiguration();

    return true;
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter#setShouldDisplayOnDefaultPage(java.lang.String,
   *      boolean)
   */
  public void setShouldDisplayOnDefaultPage(String categoryID, boolean shouldDisplay) {

    if (shouldDisplay) {
      this.fShouldNotDisplayOnDefaultPage.remove(categoryID);
    } else {
      this.fShouldNotDisplayOnDefaultPage.add(categoryID);
    }
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter#setShouldDisplayOnOwnPage(java.lang.String,
   *      boolean)
   */
  public void setShouldDisplayOnOwnPage(String categoryID, boolean shouldDisplay) {

    if (shouldDisplay) {
      this.fShouldNotDisplayOnOwnPage.remove(categoryID);
    } else {
      this.fShouldNotDisplayOnOwnPage.add(categoryID);
    }
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter#setPageOrder(java.util.List)
   */
  public void setPageOrder(List order) {
    this.fOwnPageSortOrder = order;
  }

  /**
   * @see org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter#setDefaultPageOrder(java.util.List)
   */
  public void setDefaultPageOrder(List order) {
    this.fDefaultPageSortOrder = order;
  }

  /**
   * <p>
   * Loads the user configuration from the associated {@link IPreferenceStore}
   * </p>
   */
  private void loadUserConfiguration() {
    this.loadDefaultPagePreference(false);
    this.loadPageSortOrder(false);
    this.loadDefaultPageSortOrder(false);
    this.loadShouldNotDisplayOnOwnPage(false);
  }

  /**
   * <p>
   * Loads the user preferences for which categories to display on the default content assist page
   * </p>
   * 
   * @param useDefaults if <code>true</code> then use the {@link IPreferenceStore} defaults,
   *          otherwise use the user specified preferences
   */
  private void loadDefaultPagePreference(boolean useDefaults) {
    //clear the current display on default page configuration
    this.fShouldNotDisplayOnDefaultPage.clear();

    //parse either the default or user configuration preference
    String displayOnDefaultPage;
    if (useDefaults) {
      displayOnDefaultPage = getPreferenceStore().getDefaultString(
          getShouldNotDisplayOnDefaultPagePrefKey());
    } else {
      displayOnDefaultPage = getPreferenceStore().getString(
          getShouldNotDisplayOnDefaultPagePrefKey());
    }
    StringTokenizer defaultPageTokenizer = new StringTokenizer(displayOnDefaultPage,
        PREFERENCE_CATEGORY_SEPERATOR);
    while (defaultPageTokenizer.hasMoreTokens()) {
      fShouldNotDisplayOnDefaultPage.add(defaultPageTokenizer.nextToken());
    }
  }

  /**
   * <p>
   * Loads the user preferences for the sort order of the content assist pages
   * </p>
   * 
   * @param useDefaults if <code>true</code> then use the {@link IPreferenceStore} defaults,
   *          otherwise use the user specified preferences
   */
  private void loadPageSortOrder(boolean useDefaults) {
    //clear the current sort order
    this.fOwnPageSortOrder.clear();

    //parse either the default or user configuration preference
    String sortOrder;
    if (useDefaults) {
      sortOrder = getPreferenceStore().getDefaultString(getPageSortOrderPrefKey());
    } else {
      sortOrder = getPreferenceStore().getString(getPageSortOrderPrefKey());
    }
    StringTokenizer tokenizer = new StringTokenizer(sortOrder, PREFERENCE_CATEGORY_SEPERATOR);
    while (tokenizer.hasMoreTokens()) {
      String categoryID = tokenizer.nextToken();
      this.fOwnPageSortOrder.add(categoryID);
    }
  }

  /**
   * <p>
   * Loads the user preferences for the sort order of the content assist pages
   * </p>
   * 
   * @param useDefaults if <code>true</code> then use the {@link IPreferenceStore} defaults,
   *          otherwise use the user specified preferences
   */
  private void loadDefaultPageSortOrder(boolean useDefaults) {
    //clear the current sort order
    this.fDefaultPageSortOrder.clear();

    //parse either the default or user configuration preference
    String sortOrder;
    if (useDefaults) {
      sortOrder = getPreferenceStore().getDefaultString(getDefaultPageSortOrderPrefKey());
    } else {
      sortOrder = getPreferenceStore().getString(getDefaultPageSortOrderPrefKey());
    }
    StringTokenizer tokenizer = new StringTokenizer(sortOrder, PREFERENCE_CATEGORY_SEPERATOR);
    while (tokenizer.hasMoreTokens()) {
      String categoryID = tokenizer.nextToken();
      this.fDefaultPageSortOrder.add(categoryID);
    }
  }

  /**
   * <p>
   * Loads the user preferences for which categories should not be displayed on their own content
   * assist page
   * </p>
   * 
   * @param useDefaults if <code>true</code> then use the {@link IPreferenceStore} defaults,
   *          otherwise use the user specified preferences
   */
  private void loadShouldNotDisplayOnOwnPage(boolean useDefaults) {
    //clear the current sort order
    this.fShouldNotDisplayOnOwnPage.clear();

    //parse either the default or user configuration preference
    String preference;
    if (useDefaults) {
      preference = getPreferenceStore().getDefaultString(getShouldNotDisplayOnOwnPagePrefKey());
    } else {
      preference = getPreferenceStore().getString(getShouldNotDisplayOnOwnPagePrefKey());
    }
    StringTokenizer tokenizer = new StringTokenizer(preference, PREFERENCE_CATEGORY_SEPERATOR);
    while (tokenizer.hasMoreTokens()) {
      String categoryID = tokenizer.nextToken();
      this.fShouldNotDisplayOnOwnPage.add(categoryID);
    }
  }

  /**
   * <p>
   * Saves the user preferences for which categories to display on the default content assist page
   * </p>
   */
  private void saveShouldDisplayOnDefaultPageConfiguration() {
    //create the preference string
    StringBuffer defaultPageBuff = new StringBuffer();
    Iterator defaultPageIter = this.fShouldNotDisplayOnDefaultPage.iterator();
    while (defaultPageIter.hasNext()) {
      String categoryID = (String) defaultPageIter.next();
      defaultPageBuff.append(categoryID + PREFERENCE_CATEGORY_SEPERATOR);
    }

    //save the preference
    this.getPreferenceStore().setValue(this.getShouldNotDisplayOnDefaultPagePrefKey(),
        defaultPageBuff.toString());
  }

  /**
   * <p>
   * Saves the user preferences for the sort order of the content assist pages
   * </p>
   */
  private void savePageSortOrderConfiguration() {
    //create the preference string
    StringBuffer orderBuff = new StringBuffer();
    for (int i = 0; i < this.fOwnPageSortOrder.size(); ++i) {
      if (this.fOwnPageSortOrder.get(i) != null) {
        orderBuff.append(this.fOwnPageSortOrder.get(i) + PREFERENCE_CATEGORY_SEPERATOR);
      }
    }

    //save the preference
    this.getPreferenceStore().setValue(this.getPageSortOrderPrefKey(), orderBuff.toString());
  }

  /**
   * <p>
   * Saves the user preferences for the sort order of the categories on the default page
   * </p>
   */
  private void saveDefaultPageSortOrderConfiguration() {
    //create the preference string
    StringBuffer orderBuff = new StringBuffer();
    for (int i = 0; i < this.fDefaultPageSortOrder.size(); ++i) {
      if (this.fDefaultPageSortOrder.get(i) != null) {
        orderBuff.append(this.fDefaultPageSortOrder.get(i) + PREFERENCE_CATEGORY_SEPERATOR);
      }
    }

    //save the preference
    this.getPreferenceStore().setValue(this.getDefaultPageSortOrderPrefKey(), orderBuff.toString());
  }

  /**
   * <p>
   * Saves the user preferences for which categories should not be displayed on their own content
   * assist page
   * </p>
   */
  private void saveShouldDisplayOnOwnPageConfiguration() {
    //create the preference string
    StringBuffer buff = new StringBuffer();
    Iterator iter = this.fShouldNotDisplayOnOwnPage.iterator();
    while (iter.hasNext()) {
      String categoryID = (String) iter.next();
      buff.append(categoryID + PREFERENCE_CATEGORY_SEPERATOR);
    }

    //save the preference
    this.getPreferenceStore().setValue(this.getShouldNotDisplayOnOwnPagePrefKey(), buff.toString());
  }
}
