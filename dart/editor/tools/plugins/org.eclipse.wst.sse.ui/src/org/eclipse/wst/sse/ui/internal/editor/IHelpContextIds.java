/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.editor;

import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

/**
 * Help context ids for the Source Editor.
 * <p>
 * This interface contains constants only; it is not intended to be implemented.
 * </p>
 */
public interface IHelpContextIds {

  // org.eclipse.wst.sse.ui.
  public static final String PREFIX = SSEUIPlugin.ID + "."; //$NON-NLS-1$
  // Abstract Source Editor Context Menu
  // Content Assist
  public static final String CONTMNU_CONTENTASSIST_HELPID = PREFIX + "xmlm1010"; //$NON-NLS-1$
  // Format Document
  public static final String CONTMNU_FORMAT_DOC_HELPID = PREFIX + "xmlm1030"; //$NON-NLS-1$
  // Format Active Elements
  public static final String CONTMNU_FORMAT_ELEMENTS_HELPID = PREFIX + "xmlm1040"; //$NON-NLS-1$
  // Cleanup Document
  // public static final String CONTMNU_CLEANUP_DOC_HELPID = PREFIX +
  // "xmlm1050"; //$NON-NLS-1$

  // Properties
  public static final String CONTMNU_PROPERTIES_HELPID = PREFIX + "xmlm1070"; //$NON-NLS-1$

  // Structured Text Editor Preference Page
  // Appearance
  public static final String PREFSTE_APPEARANCE_HELPID = PREFIX + "sted0001"; //$NON-NLS-1$
  // Hovers
  public static final String PREFSTE_HOVERS_HELPID = PREFIX + "sted0003"; //$NON-NLS-1$

//	// Read-Only Text Style
//	public static final String PREFWEBX_READONLY_HELPID = PREFIX + "webx0001"; //$NON-NLS-1$

  // Web and XML Preference Page
  // Task Tags
  public static final String PREFWEBX_TASKTAGS_HELPID = PREFIX + "webx0000"; //$NON-NLS-1$

  // Source Editor View
  public static final String XML_SOURCE_VIEW_HELPID = PREFIX + "xmlm2000"; //$NON-NLS-1$
}
