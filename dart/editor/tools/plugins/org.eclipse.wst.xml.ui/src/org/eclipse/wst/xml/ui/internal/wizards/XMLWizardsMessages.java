/**********************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM - Initial API and implementation
 * David Carver/STAR Standard - d_a_carver@yahoo.com - bug 192568
 **********************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import org.eclipse.osgi.util.NLS;

/**
 * Strings used by XML Wizards
 * 
 * @plannedfor 1.0
 */
public class XMLWizardsMessages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.wst.xml.ui.internal.wizards.wizardResource";//$NON-NLS-1$

  public static String _UI_DIALOG_NEW_TITLE;
  public static String _UI_DIALOG_TITLE_INVALID_GRAMMAR;
  public static String _UI_DIALOG_MESSAGE_INVALID_GRAMMAR;
  public static String _UI_WIZARD_CREATE_NEW_TITLE;
  public static String _UI_RADIO_XML_FROM_DTD;
  public static String _UI_RADIO_XML_FROM_SCHEMA;
  public static String _UI_RADIO_XML_FROM_SCRATCH;
  public static String _UI_WIZARD_CREATE_XML_HEADING;
  public static String _UI_WIZARD_CREATE_XML_EXPL;
  public static String _UI_WIZARD_CREATE_XML_FILE_HEADING;
  public static String _UI_WIZARD_CREATE_XML_FILE_EXPL;
  public static String _UI_WIZARD_SELECT_DTD_FILE_DESC;
  public static String _UI_WIZARD_SELECT_DTD_FILE_TITLE;
  public static String _UI_WIZARD_SELECT_XSD_FILE_DESC;
  public static String _UI_WIZARD_SELECT_XSD_FILE_TITLE;
  public static String _UI_WIZARD_SELECT_ROOT_HEADING;
  public static String _UI_WIZARD_SELECT_ROOT_EXPL;
  public static String _UI_LABEL_ROOT_ELEMENT;
  public static String _UI_WARNING_TITLE_NO_ROOT_ELEMENTS;
  public static String _UI_WARNING_MSG_NO_ROOT_ELEMENTS;
  public static String _UI_LABEL_NO_LOCATION_HINT;
  public static String _UI_WARNING_MSG_NO_LOCATION_HINT_1;
  public static String _UI_WARNING_MSG_NO_LOCATION_HINT_2;
  public static String _UI_WARNING_MSG_NO_LOCATION_HINT_3;
  public static String _UI_WIZARD_CONTENT_OPTIONS;
  public static String _UI_WIZARD_CREATE_REQUIRED;// commented out
  public static String _UI_WIZARD_CREATE_OPTIONAL;// commented out
  public static String _UI_WIZARD_CREATE_OPTIONAL_ATTRIBUTES;
  public static String _UI_WIZARD_CREATE_OPTIONAL_ELEMENTS;
  public static String _UI_WIZARD_CREATE_FIRST_CHOICE;
  public static String _UI_WIZARD_FILL_ELEMENTS_AND_ATTRIBUTES;
  public static String _UI_LABEL_DOCTYPE_INFORMATION;
  public static String _UI_LABEL_SYSTEM_ID;
  public static String _UI_LABEL_PUBLIC_ID;
  public static String _UI_WARNING_URI_NOT_FOUND_COLON;
  public static String _UI_INVALID_GRAMMAR_ERROR;
  public static String _ERROR_BAD_FILENAME_EXTENSION;
  public static String _ERROR_FILE_ALREADY_EXISTS;
  public static String _ERROR_ROOT_ELEMENT_MUST_BE_SPECIFIED;
  public static String _UI_LABEL_ERROR_SCHEMA_INVALID_INFO;
  public static String _UI_LABEL_ERROR_DTD_INVALID_INFO;
  public static String _UI_LABEL_ERROR_CATALOG_ENTRY_INVALID;
  public static String _UI_LABEL_NAMESPACE_INFORMATION;
  public static String Validation_Plugins_Unavailable;
  public static String Validation_cannot_be_performed;
  public static String ExampleProjectCreationOperation_op_desc;
  public static String ExampleProjectCreationOperation_op_desc_proj;
  public static String ExampleProjectCreationWizard_title;
  public static String ExampleProjectCreationWizard_op_error_title;
  public static String ExampleProjectCreationWizard_op_error_message;
  public static String ExampleProjectCreationWizard_overwritequery_title;
  public static String ExampleProjectCreationWizard_overwritequery_message;
  public static String ExampleProjectCreationWizardPage_error_alreadyexists;

  public static String NewXMLTemplatesWizardPage_0;
  public static String NewXMLTemplatesWizardPage_1;
  public static String NewXMLTemplatesWizardPage_2;
  public static String NewXMLTemplatesWizardPage_3;
  public static String NewXMLTemplatesWizardPage_4;
  public static String NewXMLTemplatesWizardPage_5;
  public static String NewXMLTemplatesWizardPage_6;
  public static String NewXMLTemplatesWizardPage_7;

  public static String _UI_DIALOG_XMLCATALOG_IMPORT_TITLE;
  public static String _UI_DIALOG_XMLCATALOG_EXPORT_TITLE;
  public static String _UI_DIALOG_XMLCATALOG_EXPORT_DESCRIPTION;
  public static String _UI_DIALOG_XMLCATALOG_IMPORT_DESCRIPTION;
  public static String _UI_WIZARD_LIMIT_OPTIONAL_ELEMENT_DEPTH;
  public static String _UI_WIZARD_GENERATING_XML_DOCUMENT;

  static {
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, XMLWizardsMessages.class);
  }

  private XMLWizardsMessages() {
    // cannot create new instance
  }
}
