/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.editor;

import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;

/**
 * Help context ids for the HTML Source Editor.
 * <p>
 * This interface contains constants only; it is not intended to be implemented.
 * </p>
 */
public interface IHelpContextIds {
  // org.eclipse.wst.html.ui.
  public static final String PREFIX = HTMLUIPlugin.ID + "."; //$NON-NLS-1$

  // // figured out on the fly
  // // HTML Source page editor
  // public static final String HTML_SOURCEVIEW_HELPID =
  // ContentTypeIdForHTML.ContentTypeID_HTML +"_source_HelpId";
  // //$NON-NLS-1$

  // HTML Files Preference page
  public static final String HTML_PREFWEBX_FILES_HELPID = PREFIX + "webx0030"; //$NON-NLS-1$
  // HTML Source Preference page
  public static final String HTML_PREFWEBX_SOURCE_HELPID = PREFIX + "webx0031"; //$NON-NLS-1$
  // HTML Styles Preference page
  public static final String HTML_PREFWEBX_STYLES_HELPID = PREFIX + "webx0032"; //$NON-NLS-1$
  // HTML Templates Preference page
  public static final String HTML_PREFWEBX_TEMPLATES_HELPID = PREFIX + "webx0033"; //$NON-NLS-1$

  // HTML Cleanup dialog
  public static final String CLEANUP_HTML_HELPID = PREFIX + "xmlm1100"; //$NON-NLS-1$

  // HTML Content Settings
  public static final String WEB_CONTENT_SETTINGS_HELPID = PREFIX + "misc0170"; //$NON-NLS-1$

  // HTML New File Wizard - Template Page
  public static final String HTML_NEWWIZARD_TEMPLATE_HELPID = PREFIX + "htmlw0010"; //$NON-NLS-1$
}
