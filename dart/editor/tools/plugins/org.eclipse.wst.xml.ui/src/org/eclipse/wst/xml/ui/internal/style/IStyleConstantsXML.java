/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring Benjamin Muskalla,
 * b.muskalla@gmx.net - [158660] character entities should have their own syntax highlighting
 * preference
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.style;

/**
 * Contains the symbolic name of styles used by LineStyleProvider, ColorManager, and any others who
 * may be interested
 */
public interface IStyleConstantsXML {
  public static final String BACKGROUND = "background"; //$NON-NLS-1$
  public static final String CDATA_BORDER = "cdataBorder";//$NON-NLS-1$
  public static final String CDATA_TEXT = "cdataText";//$NON-NLS-1$
  public static final String COMMENT_BORDER = "commentBorder";//$NON-NLS-1$
  public static final String COMMENT_TEXT = "commentText";//$NON-NLS-1$
  public static final String DECL_BORDER = "declBoder";//$NON-NLS-1$
  public static final String DOCTYPE_EXTERNAL_ID = "doctypeExternalId";//$NON-NLS-1$
  public static final String DOCTYPE_EXTERNAL_ID_PUBREF = "doctypeExternalPubref";//$NON-NLS-1$
  public static final String DOCTYPE_EXTERNAL_ID_SYSREF = "doctypeExtrenalSysref";//$NON-NLS-1$
  public static final String DOCTYPE_NAME = "doctypeName";//$NON-NLS-1$

  public static final String FOREGROUND = "foreground"; //$NON-NLS-1$
  public static final String PI_BORDER = "piBorder";//$NON-NLS-1$
  public static final String PI_CONTENT = "piContent";//$NON-NLS-1$

  public static final String TAG_ATTRIBUTE_EQUALS = "tagAttributeEquals"; //$NON-NLS-1$
  public static final String TAG_ATTRIBUTE_NAME = "tagAttributeName";//$NON-NLS-1$
  public static final String TAG_ATTRIBUTE_VALUE = "tagAttributeValue";//$NON-NLS-1$
  public static final String TAG_BORDER = "tagBorder";//$NON-NLS-1$
  public static final String TAG_NAME = "tagName";//$NON-NLS-1$
  public static final String XML_CONTENT = "xmlContent";//$NON-NLS-1$
  public static final String ENTITY_REFERENCE = "entityReference";//$NON-NLS-1$
}
