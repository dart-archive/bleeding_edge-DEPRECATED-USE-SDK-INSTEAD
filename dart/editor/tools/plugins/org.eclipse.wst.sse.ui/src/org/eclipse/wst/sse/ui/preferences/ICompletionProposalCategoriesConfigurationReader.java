/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.preferences;

/**
 * <p>
 * Implementers of the <code>org.eclipse.wst.sse.ui.completionProposalCategoriesConfiguration</code>
 * extension will need an implementation of this interface for their extension
 * </p>
 * <p>
 * <b>NOTE: </b>Implementers must have a 0 argument constructor so class can be instantiated by
 * extension.
 * </p>
 * 
 * @see ICompletionProposalCategoriesConfigurationWriter
 */
public interface ICompletionProposalCategoriesConfigurationReader {
  /**
   * The default is to display a category on its own page if not otherwise defined by a property.
   */
  boolean DEFAULT_DISPLAY_ON_OWN_PAGE = true;

  /** The default is to display a category on the default content assist page. */
  boolean DEFAULT_INCLUDE_ON_DEFAULTS_PAGE = true;

  /** the default sort order if none is defined by a properties extension */
  int DEFAULT_SORT_ORDER = Integer.MAX_VALUE;

  /**
   * <p>
   * Determines if the given category should be displayed on its own content assist page
   * </p>
   * 
   * @param categoryID determine if this category should be displayed on its own content assist page
   * @return <code>true</code> if the given category should be displayed on its own content assist
   *         page, <code>false</code> otherwise
   */
  boolean shouldDisplayOnOwnPage(String categoryID);

  /**
   * <p>
   * Determines if the given category should be displayed on the default content assist page
   * </p>
   * 
   * @param categoryID determine if this category should be displayed on the default content assist
   *          page
   * @return <code>true</code> if the given category should be displayed on the default content
   *         assist page, <code>false</code> otherwise
   */
  boolean shouldDisplayOnDefaultPage(String categoryID);

  /**
   * <p>
   * Determines the sort order ranking of the given category when compared to the other categories,
   * this is used to determine the order in which the separate content assist pages should be
   * displayed
   * </p>
   * 
   * @param categoryID determine the sort order ranking of this category
   * @return the sort order ranking of the given category when compared to the other categories
   */
  int getPageSortOrder(String categoryID);

  /**
   * <p>
   * Determines the sort order ranking of the given category when compared to the other categories,
   * this is used to determine the order in which categories should be listed on the default page
   * </p>
   * 
   * @param categoryID determine the sort order ranking of this category
   * @return the sort order ranking of the given category when compared to the other categories
   */
  int getDefaultPageSortOrder(String categoryID);
}
