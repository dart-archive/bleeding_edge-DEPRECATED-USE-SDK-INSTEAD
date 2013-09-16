/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.editor;

import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

/**
 * Help context ids for the XML Source Editor.
 * <p>
 * This interface contains constants only; it is not intended to be implemented.
 * </p>
 */
public interface IHelpContextIds {
  // org.eclipse.wst.xml.ui.
  public static final String PREFIX = XMLUIPlugin.ID + "."; //$NON-NLS-1$

  // figured out on the fly
  // // XML Source page editor
  // public static final String XML_SOURCEVIEW_HELPID =
  // ContentTypeIdForXML.ContentTypeID_XML +"_source_HelpId"; //$NON-NLS-1$

  // XML Files Preference page
  public static final String XML_PREFWEBX_FILES_HELPID = PREFIX + "webx0060"; //$NON-NLS-1$
  // XML Source Preference page
  public static final String XML_PREFWEBX_SOURCE_HELPID = PREFIX + "webx0061"; //$NON-NLS-1$
  // XML Styles Preference page
  public static final String XML_PREFWEBX_STYLES_HELPID = PREFIX + "webx0062"; //$NON-NLS-1$
  // XML Templates Preference page
  public static final String XML_PREFWEBX_TEMPLATES_HELPID = PREFIX + "webx0063"; //$NON-NLS-1$
  // XML Validator Preference page
  public static final String XML_PREFWEBX_VALIDATOR_HELPID = PREFIX + "webx0064"; //$NON-NLS-1$

  // XML Cleanup dialog
  public static final String CLEANUP_XML_HELPID = PREFIX + "xmlm1200"; //$NON-NLS-1$

  // XML New File Wizard - Template Page
  public static final String XML_NEWWIZARD_TEMPLATE_HELPID = PREFIX + "xmlw0010"; //$NON-NLS-1$
}
