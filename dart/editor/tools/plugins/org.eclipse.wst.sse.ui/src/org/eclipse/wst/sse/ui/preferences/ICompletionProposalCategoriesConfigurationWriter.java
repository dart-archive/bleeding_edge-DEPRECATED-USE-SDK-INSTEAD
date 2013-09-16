/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.preferences;

import org.eclipse.wst.sse.ui.internal.contentassist.CompletionProposalCategory;

import java.util.List;

/**
 * <p>
 * Implementers of the <code>org.eclipse.wst.sse.ui.completionProposalCategoriesConfiguration</code>
 * extension can implement this interface if their configuration should be user edible and thus
 * needs writing capabilities
 * </p>
 * <p>
 * <b>NOTE: </b>Implementers must have a 0 argument constructor so class can be instantiated by
 * extension.
 * </p>
 * 
 * @see ICompletionProposalCategoriesConfigurationReader
 */
public interface ICompletionProposalCategoriesConfigurationWriter extends
    ICompletionProposalCategoriesConfigurationReader {

  /**
   * <p>
   * If a writer has a known associated properties page then that properties page ID can be used so
   * that error messages during the content assist process can link to that preference page to allow
   * the user to change the settings
   * </p>
   * 
   * @return <code>true</code> if this writer has a known associated properties preference page,
   *         <code>false</code> otherwise
   */
  boolean hasAssociatedPropertiesPage();

  /**
   * @return If {@link #hasAssociatedPropertiesPage()} returns <code>true</code> then this method
   *         must return a valid properties page ID where the user can edit the content assist
   *         configuration, else it can return <code>null</code>
   */
  String getPropertiesPageID();

  /**
   * <p>
   * Sets whether or not the given category should be displayed on its own content assist page.
   * <p>
   * <p>
   * <b>NOTE: </b>This preference should <b>NOT</b> be saved permanently here, that action should
   * wait until {@link #saveConfiguration()} is called
   * </p>
   * 
   * @param categoryID the category that should either be displayed on its own content assist page
   *          or not
   * @param shouldDisplay <code>true</code> if the given category should be displayed on its own
   *          content assist page, <code>false</code> otherwise
   */
  void setShouldDisplayOnDefaultPage(String categoryID, boolean shouldDisplay);

  /**
   * <p>
   * Sets whether or not the given category should be displayed on the default content assist page.
   * <p>
   * <p>
   * <b>NOTE: </b>This preference should <b>NOT</b> be saved permanently here, that action should
   * wait until {@link #saveConfiguration()} is called
   * </p>
   * 
   * @param categoryID the category that should either be displayed on the default content assist
   *          page or not
   * @param shouldDisplay <code>true</code> if the given category should be displayed on the default
   *          content assist page, <code>false</code> otherwise
   */
  void setShouldDisplayOnOwnPage(String categoryID, boolean shouldDisplay);

  /**
   * <p>
   * Sets the order in which the categories should be cycled when invoking content assist multiple
   * times. Event categories that are not activated to display on their own content assist page can
   * be listed here so that when activated to display on their own page they have a rank. The entire
   * order needs to be re-set each time one category moves because the writer has no way of knowing
   * how to move just one category in the order
   * </p>
   * <p>
   * <b>NOTE: </b>This preference should <b>NOT</b> be saved permanently here, that action should
   * wait until {@link #saveConfiguration()} is called
   * </p>
   * 
   * @param order <code>{@link List}<{@link String}></code>
   *          <ul>
   *          <li><b>values:</b> {@link CompletionProposalCategory} IDs</li>
   *          </ul>
   */
  void setPageOrder(List order);

  /**
   * <p>
   * Sets the order in which the categories should be listed on the default page. Event categories
   * that are not activated to display on the default content assist page can be listed here so that
   * when activated to display on the default page they have a rank. The entire order needs to be
   * re-set each time one category moves because the writer has no way of knowing how to move just
   * one category in the order
   * </p>
   * <p>
   * <b>NOTE: </b>This preference should <b>NOT</b> be saved permanently here, that action should
   * wait until {@link #saveConfiguration()} is called
   * </p>
   * 
   * @param order <code>{@link List}<{@link String}></code>
   *          <ul>
   *          <li><b>values:</b> {@link CompletionProposalCategory} IDs</li>
   *          </ul>
   */
  void setDefaultPageOrder(List order);

  /**
   * <p>
   * Should load the default settings from wherever they are being stored
   * </p>
   */
  void loadDefaults();

  /**
   * <p>
   * Should save the configuration permanently. Typically called after the user changes some
   * preferences using a preference page and then applies them, but if they do not apply the changes
   * then this function should not be called. This is the reason why the various <code>set*</code>
   * methods should not permanently save the configuration
   * </p>
   * 
   * @return <code>true</code> if the save was successful, <code>false</code> otherwise
   */
  boolean saveConfiguration();
}
