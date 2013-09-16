/**********************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.wst.html.ui.internal;

import org.eclipse.osgi.util.NLS;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Strings used by HTML UI
 * 
 * @plannedfor 1.0
 */
public class HTMLUIMessages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.wst.html.ui.internal.HTMLUIPluginResources";//$NON-NLS-1$
  private static ResourceBundle fResourceBundle;

  static {
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, HTMLUIMessages.class);
  }

  private HTMLUIMessages() {
    // cannot create new instance of this class
  }

  public static ResourceBundle getResourceBundle() {
    try {
      if (fResourceBundle == null)
        fResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME);
    } catch (MissingResourceException x) {
      fResourceBundle = null;
    }
    return fResourceBundle;
  }

  public static String Sample_HTML_doc;
  public static String HTMLFilesPreferencePage_0;
  public static String _UI_WIZARD_NEW_TITLE;
  public static String _UI_WIZARD_NEW_HEADING;
  public static String _UI_WIZARD_NEW_DESCRIPTION;
  public static String _ERROR_FILENAME_MUST_END_HTML;
  public static String _WARNING_FOLDER_MUST_BE_INSIDE_WEB_CONTENT;
  public static String ResourceGroup_nameExists;
  public static String NewHTMLTemplatesWizardPage_0;
  public static String NewHTMLTemplatesWizardPage_1;
  public static String NewHTMLTemplatesWizardPage_2;
  public static String NewHTMLTemplatesWizardPage_3;
  public static String NewHTMLTemplatesWizardPage_4;
  public static String NewHTMLTemplatesWizardPage_5;
  public static String NewHTMLTemplatesWizardPage_6;
  public static String NewHTMLTemplatesWizardPage_7;
  public static String Creating_files_encoding;
  public static String CleanupDocument_label; // resource bundle
  public static String CleanupDocument_tooltip; // resource bundle
  public static String CleanupDocument_description; // resource bundle
  public static String ToggleComment_label; // resource bundle
  public static String ToggleComment_tooltip; // resource bundle
  public static String ToggleComment_description; // resource bundle
  public static String Add_inline;
  public static String AddBlockComment_label; // resource bundle
  public static String AddBlockComment_tooltip; // resource bundle
  public static String AddBlockComment_description; // resource bundle
  public static String Remove_inline;
  public static String RemoveBlockComment_label; // resource bundle
  public static String RemoveBlockComment_tooltip; // resource bundle
  public static String RemoveBlockComment_description; // resource bundle
  public static String FindOccurrences_label; // resource bundle
  public static String Creating_files;
  public static String Elements_Dialog_message;
  public static String Elements_Dialog_title;
  public static String Encoding_desc;
  public static String UI_Description_of_role_of_following_DOCTYPE;
  public static String UI_Default_HTML_DOCTYPE_ID___1;
  public static String UI_Public_ID;
  public static String UI_System_ID;
  public static String UI_none;
  public static String UI_CSS_profile___2;
  public static String WebContentSettingsPropertyPage_0;
  public static String ProjectWebContentSettingsPropertyPage_0;

  public static String Auto_Activation_UI_;
  public static String Auto_Activation_Delay;
  public static String Automatically_make_suggest_UI_;
  public static String Prompt_when_these_characte_UI_;
  public static String Cycling_UI_;
  public static String Formatting_UI_;
  public static String Line_width__UI_;
  public static String Split_multiple_attributes;
  public static String Align_final_bracket;
  public static String Indent_using_tabs;
  public static String Indent_using_spaces;
  public static String Indentation_size;
  public static String Indentation_size_tip;
  public static String Inline_elements_table_label;
  public static String Clear_all_blank_lines_UI_;
  public static String Preferred_markup_case_UI_;
  public static String Tag_names__UI_;
  public static String Tag_names_Upper_case_UI_;
  public static String Tag_names_Lower_case_UI_;
  public static String Attribute_names__UI_;
  public static String Attribute_names_Upper_case_UI_;
  public static String Attribute_names_Lower_case_UI_;
  public static String Cleanup_UI_;
  public static String Tag_name_case_for_HTML_UI_;
  public static String Tag_name_case_As_is_UI_;
  public static String Tag_name_case_Lower_UI_;
  public static String Tag_name_case_Upper_UI_;
  public static String Attribute_name_case_for_HTML_UI_;
  public static String Attribute_name_case_As_is_UI_;
  public static String Attribute_name_case_Lower_UI_;
  public static String Attribute_name_case_Upper_UI_;
  public static String Insert_required_attributes_UI_;
  public static String Insert_missing_tags_UI_;
  public static String Quote_attribute_values_UI_;
  public static String Format_source_UI_;
  public static String Convert_EOL_codes_UI_;
  public static String EOL_Windows_UI;
  public static String EOL_Unix_UI;
  public static String EOL_Mac_UI;
  public static String SyntaxColoringPage_0;
  public static String SyntaxColoringPage_2;
  public static String SyntaxColoringPage_3;
  public static String SyntaxColoringPage_4;
  public static String SyntaxColoringPage_5;
  public static String SyntaxColoringPage_6;

  // below are possibly unused strings that may be deleted
  public static String HTMLFilesPreferencePage_1;
  public static String HTMLFilesPreferencePage_2;
  public static String HTMLFilesPreferencePage_3;
  // above are possibly unused strings that may be deleted
  public static String EmptyFilePreferencePage_0;
  public static String _UI_STRUCTURED_TEXT_EDITOR_PREFS_LINK;

  // HTML Typing Preferences
  public static String HTMLTyping_Auto_Complete;
  public static String HTMLTyping_Auto_Remove;
  public static String HTMLTyping_Complete_Comments;
  public static String HTMLTyping_Complete_End_Tags;
  public static String HTMLTyping_Remove_End_Tags;
  public static String HTMLTyping_Close_Strings;
  public static String HTMLTyping_Close_Brackets;

  // below are the strings for the validation page
  public static String Validation_description;
  public static String Validation_Warning;
  public static String Validation_Error;
  public static String Validation_Ignore;
  public static String Expandable_label_attributes;
  public static String Expandable_label_elements;
  public static String Expandable_label_document_type;
  public static String Expandable_label_text;
  public static String Expandable_label_comment;
  public static String Expandable_label_cdata;
  public static String Expandable_label_pi;
  public static String Expandable_label_entity_ref;

  public static String HTMLValidationPreferencePage_0;
  public static String HTMLValidationPreferencePage_1;
  public static String HTMLValidationPreferencePage_10;
  public static String HTMLValidationPreferencePage_11;
  public static String HTMLValidationPreferencePage_12;
  public static String HTMLValidationPreferencePage_13;
  public static String HTMLValidationPreferencePage_14;
  public static String HTMLValidationPreferencePage_15;
  public static String HTMLValidationPreferencePage_16;
  public static String HTMLValidationPreferencePage_17;
  public static String HTMLValidationPreferencePage_18;
  public static String HTMLValidationPreferencePage_19;
  public static String HTMLValidationPreferencePage_2;
  public static String HTMLValidationPreferencePage_20;
  public static String HTMLValidationPreferencePage_21;
  public static String HTMLValidationPreferencePage_22;
  public static String HTMLValidationPreferencePage_23;
  public static String HTMLValidationPreferencePage_24;
  public static String HTMLValidationPreferencePage_25;
  public static String HTMLValidationPreferencePage_26;
  public static String HTMLValidationPreferencePage_27;
  public static String HTMLValidationPreferencePage_28;
  public static String HTMLValidationPreferencePage_29;
  public static String HTMLValidationPreferencePage_3;
  public static String HTMLValidationPreferencePage_30;
  public static String HTMLValidationPreferencePage_31;
  public static String HTMLValidationPreferencePage_32;
  public static String HTMLValidationPreferencePage_33;
  public static String HTMLValidationPreferencePage_34;
  public static String HTMLValidationPreferencePage_35;
  public static String HTMLValidationPreferencePage_36;
  public static String HTMLValidationPreferencePage_37;
  public static String HTMLValidationPreferencePage_4;
  public static String HTMLValidationPreferencePage_5;
  public static String HTMLValidationPreferencePage_6;
  public static String HTMLValidationPreferencePage_7;
  public static String HTMLValidationPreferencePage_8;
  public static String HTMLValidationPreferencePage_9;

  // Hyperlinks
  public static String Hyperlink_line;
  public static String Open;
}
