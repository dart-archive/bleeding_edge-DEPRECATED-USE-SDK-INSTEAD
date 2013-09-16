/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.validation;

import org.eclipse.osgi.util.NLS;

/**
 * Strings used by XML Validation
 */
public class XMLValidationUIMessages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.wst.xml.ui.internal.validation.xmlvalidation"; //$NON-NLS-1$

  private XMLValidationUIMessages() {
    // cannot create new instance
  }

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, XMLValidationUIMessages.class);
  }
  public static String _UI_REF_FILE_ERROR_DESCRIPTION;
  public static String _UI_REF_FILE_ERROR_MESSAGE;
  public static String _UI_REF_FILE_ERROR_DETAILS;
  public static String _UI_DETAILS_INFORMATION_UNAVAILABLE;
  public static String _UI_DETAILS_INFO_REVALIDATE_TO_REGENERATE;
  public static String _UI_SAVE_DIRTY_FILE_MESSAGE;
  public static String _UI_SAVE_DIRTY_FILE_TITLE;
  public static String TaskListTableViewer_0;
  public static String TaskListTableViewer_1;
  public static String TaskListTableViewer_2;
  public static String TaskListTableViewer_3;
}
