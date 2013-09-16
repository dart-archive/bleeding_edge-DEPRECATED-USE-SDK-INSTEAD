/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

/**
 * Help context ids for the xml wizard.
 * <p>
 * This interface contains constants only; it is not intended to be implemented.
 * </p>
 */
interface IXMLWizardHelpContextIds {
  // org.eclipse.wst.xml.ui.
  public static final String PREFIX = XMLUIPlugin.ID + "."; //$NON-NLS-1$
  // Create XML file from a DTD file radio
  public static final String XML_NEWWIZARD_CREATEXML1_HELPID = PREFIX + "xmlc0101"; //$NON-NLS-1$
  // Create XML file from an XML schema file radio button
  public static final String XML_NEWWIZARD_CREATEXML2_HELPID = PREFIX + "xmlc0102"; //$NON-NLS-1$
  // Create XML file from scratch radio button
  public static final String XML_NEWWIZARD_CREATEXML3_HELPID = PREFIX + "xmlc0103"; //$NON-NLS-1$
  // Select Source file page
  public static final String XML_NEWWIZARD_SELECTSOURCE_HELPID = PREFIX + "xmlc0500"; //$NON-NLS-1$
  // Root element for both DTD and XML schema
  public static final String XML_NEWWIZARD_SELECTROOTELEMENT_HELPID = PREFIX + "xmlc0410"; //$NON-NLS-1$
  // Create Optional Attributes
  public static final String XML_NEWWIZARD_SELECTROOTELEMENT1_HELPID = PREFIX + "xmlc0441"; //$NON-NLS-1$
  // Create Optional Elements
  public static final String XML_NEWWIZARD_SELECTROOTELEMENT2_HELPID = PREFIX + "xmlc0442"; //$NON-NLS-1$
  // Create first choice of required choice
  public static final String XML_NEWWIZARD_SELECTROOTELEMENT3_HELPID = PREFIX + "xmlc0443"; //$NON-NLS-1$
  // Fill elements and attribute with data
  public static final String XML_NEWWIZARD_SELECTROOTELEMENT4_HELPID = PREFIX + "xmlc0444"; //$NON-NLS-1$
  // System ID for DTD
  public static final String XML_NEWWIZARD_SELECTROOTELEMENT5_HELPID = PREFIX + "xmlc0210"; //$NON-NLS-1$
  // Public ID for DTD
  public static final String XML_NEWWIZARD_SELECTROOTELEMENT6_HELPID = PREFIX + "xmlc0220"; //$NON-NLS-1$
}
