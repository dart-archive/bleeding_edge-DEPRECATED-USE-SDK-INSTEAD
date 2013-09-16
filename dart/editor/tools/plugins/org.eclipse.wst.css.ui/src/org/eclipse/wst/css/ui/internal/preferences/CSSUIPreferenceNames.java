/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.preferences;

/**
 * Preference keys for CSS UI
 */
public class CSSUIPreferenceNames {
  private CSSUIPreferenceNames() {
    // cannot create instance
  }

  /**
   * The key to store customized templates.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public static final String TEMPLATES_KEY = "org.eclipse.wst.sse.ui.custom_templates"; //$NON-NLS-1$

  /**
   * The key to store the last template name used in new CSS file wizard. Template name is stored
   * instead of template id because user-created templates do not have template ids.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public static final String NEW_FILE_TEMPLATE_NAME = "newFileTemplateName"; //$NON-NLS-1$

  /**
   * The initial template ID to be used in the new CSS file wizard. In the absence of
   * {@link NEW_FILE_TEMPLATE_NAME}, this ID is used to find a template name
   */
  public static final String NEW_FILE_TEMPLATE_ID = "newFileTemplateId"; //$NON-NLS-1$

  /**
   * <p>
   * preference key used for saving which categories should not display on the default page
   * </p>
   * <p>
   * Value is of type {@link String} consisting of
   * <tt>org.eclipse.wst.sse.ui.completionProposal/proposalCategory/@id</tt>s separated by the null
   * character (<tt>\0</tt>), ordered is ignored
   * </p>
   */
  public static final String CONTENT_ASSIST_DO_NOT_DISPLAY_ON_DEFAULT_PAGE = "css_content_assist_display_on_default_page"; //$NON-NLS-1$

  /**
   * <p>
   * preference key used for saving which categories should not display on their own page
   * </p>
   * <p>
   * Value is of type {@link String} consisting of
   * <tt>org.eclipse.wst.sse.ui.completionProposal/proposalCategory/@id</tt>s separated by the null
   * character (<tt>\0</tt>), order is ignored
   * </p>
   */
  public static final String CONTENT_ASSIST_DO_NOT_DISPLAY_ON_OWN_PAGE = "css_content_assist_display_on_own_page"; //$NON-NLS-1$

  /**
   * <p>
   * preference key for saving the sort order of the categories when displaying them on their own
   * page
   * </p>
   * <p>
   * Value is of type {@link String} consisting of
   * <tt>org.eclipse.wst.sse.ui.completionProposal/proposalCategory/@id</tt>s separated by the null
   * character (<tt>\0</tt>) in the desired sort order.
   * </p>
   */
  public static final String CONTENT_ASSIST_OWN_PAGE_SORT_ORDER = "css_content_assist_own_page_sort_order"; //$NON-NLS-1$

  /**
   * <p>
   * preference key for saving the sort order of the categories when displaying them on the default
   * page
   * </p>
   * <p>
   * Value is of type {@link String} consisting of
   * <tt>org.eclipse.wst.sse.ui.completionProposal/proposalCategory/@id</tt>s separated by the null
   * character (<tt>\0</tt>) in the desired sort order.
   * </p>
   */
  public static final String CONTENT_ASSIST_DEFAULT_PAGE_SORT_ORDER = "css_content_assist_default_page_sort_order"; //$NON-NLS-1$

  /**
   * <p>
   * preference key to store the option for auto insertion of single suggestions
   * </p>
   * <p>
   * Value is of type <code>boolean</code>
   * </p>
   */
  public static final String INSERT_SINGLE_SUGGESTION = "insertSingleSuggestion"; //$NON-NLS-1$
}
