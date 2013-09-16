/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.editor;

import org.eclipse.wst.css.ui.internal.CSSUIPlugin;

/**
 * Help context ids for the CSS Source Editor.
 * <p>
 * This interface contains constants only; it is not intended to be implemented.
 * </p>
 */
public interface IHelpContextIds {
  // org.eclipse.wst.css.ui.
  public static final String PREFIX = CSSUIPlugin.ID + "."; //$NON-NLS-1$

  // // figured out on the fly
  // // CSS Source page editor
  // public static final String CSS_SOURCEVIEW_HELPID =
  // ContentTypeIdForCSS.ContentTypeID_CSS +"_source_HelpId"; //$NON-NLS-1$

  // CSS Files Preference page
  public static final String CSS_PREFWEBX_FILES_HELPID = PREFIX + "webx0010"; //$NON-NLS-1$
  // CSS Source Preference page
  public static final String CSS_PREFWEBX_SOURCE_HELPID = PREFIX + "webx0011"; //$NON-NLS-1$
  // CSS Styles Preference page
  public static final String CSS_PREFWEBX_STYLES_HELPID = PREFIX + "webx0012"; //$NON-NLS-1$
  // CSS Template Preference page
  public static final String CSS_PREFWEBX_TEMPLATES_HELPID = PREFIX + "webx0013"; //$NON-NLS-1$

  // CSS Cleanup dialog
  public static final String CSS_CLEANUP_HELPID = PREFIX + "xmlm1300"; //$NON-NLS-1$

  // CSS Content Settings
  public static final String CSS_CONTENT_SETTINGS_HELPID = PREFIX + "misc0180"; //$NON-NLS-1$

  // CSS New File Wizard - Template Page
  public static final String CSS_NEWWIZARD_TEMPLATE_HELPID = PREFIX + "cssw0010"; //$NON-NLS-1$
}
