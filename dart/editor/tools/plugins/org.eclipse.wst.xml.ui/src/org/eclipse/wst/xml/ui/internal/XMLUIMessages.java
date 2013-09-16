/**********************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM - Initial API and implementation
 * Benjamin Muskalla, b.muskalla@gmx.net - [158660] character entities should have their own syntax
 * highlighting preference David Carver - STAR - [205989] - [validation] validate XML after XInclude
 * resolution
 **********************************************************************/
package org.eclipse.wst.xml.ui.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.osgi.util.NLS;

/**
 * Strings used by XML UI
 * 
 * @plannedfor 1.0
 */
public class XMLUIMessages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.wst.xml.ui.internal.XMLUIPluginResources";//$NON-NLS-1$
  private static ResourceBundle fResourceBundle;

  public static String Sample_XML_doc;
  public static String Comment_Delimiters_UI_;
  public static String Comment_Content_UI_;
  public static String Tag_Delimiters_UI_;
  public static String Tag_Names_UI_;
  public static String Attribute_Names_UI_;
  public static String Attribute_Equals_UI_;
  public static String Attribute_Values_UI_;
  public static String Declaration_Delimiters_UI_;
  public static String Content_UI_;
  public static String CDATA_Delimiters_UI_;
  public static String CDATA_Content_UI_;
  public static String Processing_Instruction_Del_UI_;
  public static String Processing_Instruction_Con_UI__UI_;
  public static String DOCTYPE_Name_UI_;
  public static String DOCTYPE_SYSTEM_PUBLIC_Keyw_UI_;
  public static String DOCTYPE_Public_Reference_UI_;
  public static String DOCTYPE_System_Reference_UI_;
  public static String Entity_Reference_UI_;
  public static String DELETE;
  public static String ADD_PROCESSING_INSTRUCTION;
  public static String _UI_MENU_ADD_AFTER;
  public static String _UI_MENU_ADD_ATTRIBUTE;
  public static String _UI_MENU_ADD_BEFORE;
  public static String _UI_MENU_ADD_CHILD;
  public static String _UI_MENU_REMOVE;
  public static String _UI_MENU_REPLACE_WITH;
  public static String _UI_MENU_EDIT_DOCTYPE;
  public static String _UI_LABEL_UNDO_REPLACE_DESCRIPTION;
  public static String _UI_LABEL_EDIT_DOCTYPE;
  public static String _UI_INFO_EXPAND_ALL;
  public static String _UI_INFO_COLLAPSE_ALL;
  public static String _UI_MENU_ADD_DTD_INFORMATION;
  public static String _UI_MENU_ADD_SCHEMA_INFORMATION;
  public static String _UI_MENU_EDIT_PROCESSING_INSTRUCTION;
  public static String _UI_MENU_EDIT_NAMESPACES;
  public static String _UI_MENU_ADD_DTD_INFORMATION_TITLE;
  public static String _UI_MENU_EDIT_PROCESSING_INSTRUCTION_TITLE;
  public static String _UI_MENU_EDIT_SCHEMA_INFORMATION_TITLE;
  public static String _UI_MENU_NEW_ATTRIBUTE;
  public static String _UI_MENU_NEW_ATTRIBUTE_TITLE;
  public static String _UI_MENU_EDIT_ATTRIBUTE;
  public static String _UI_MENU_EDIT_ATTRIBUTE_TITLE;
  public static String _UI_MENU_NEW_ELEMENT;
  public static String _UI_MENU_NEW_ELEMENT_TITLE;
  public static String _UI_MENU_RENAME;
  public static String _UI_MENU_RENAME_TITLE;
  public static String _UI_LABEL_ELEMENT_NAME;
  public static String _UI_MENU_ADD_COMMENT;
  public static String _UI_MENU_ADD_PROCESSING_INSTRUCTION;
  public static String _UI_MENU_ADD_CDATA_SECTION;
  public static String _UI_MENU_ADD_PCDATA;
  public static String _UI_MENU_COMMENT;
  public static String _UI_MENU_PROCESSING_INSTRUCTION;
  public static String _UI_MENU_CDATA_SECTION;
  public static String _UI_MENU_PCDATA;
  public static String _UI_MENU_ADD;
  public static String _UI_COMMENT_VALUE;
  public static String _UI_PI_TARGET_VALUE;
  public static String _UI_PI_DATA_VALUE;
  public static String _UI_LABEL_ROOT_ELEMENT_VALUE;
  public static String _UI_LABEL_TARGET_COLON;
  public static String _UI_LABEL_DATA_COLON;
  public static String _UI_LABEL_ROOT_ELEMENT_NAME_COLON;
  public static String _UI_LABEL_PUBLIC_ID_COLON;
  public static String _UI_LABEL_SYSTEM_ID_COLON;
  public static String _UI_LABEL_BROWSE;
  public static String _UI_LABEL_BROWSE_1;
  public static String _UI_LABEL_SELECT_XML_CATALOG_ENTRY;
  public static String _UI_LABEL_SPECIFY_SYSTEM_ID;
  public static String _UI_LABEL_SELECT_FILE;
  public static String _UI_LABEL_KEY;
  public static String _UI_LABEL_URI;
  public static String _UI_LABEL_XML_CATALOG_COLON;
  public static String _UI_LABEL_NAMESPACE_NAME;
  public static String _UI_LABEL_LOCATION_HINT;
  public static String _UI_LABEL_PREFIX;
  public static String _UI_LABEL_NAMESPACE_NAME_COLON;
  public static String _UI_LABEL_LOCATION_HINT_COLON;
  public static String _UI_LABEL_PREFIX_COLON;
  public static String _UI_NO_NAMESPACE_NAME;
  public static String _UI_NO_PREFIX;
  public static String _UI_LABEL_XML_SCHEMA_INFORMATION;
  public static String _UI_LABEL_NAME_COLON;
  public static String _UI_LABEL_VALUE_COLON;
  public static String _UI_BUTTON_DELETE;
  public static String _UI_BUTTON_NEW;
  public static String _UI_BUTTON_EDIT;
  public static String _UI_LABEL_NEW_NAMESPACE_INFORMATION;
  public static String _UI_RADIO_BUTTON_SELECT_FROM_WORKSPACE;
  public static String _UI_RADIO_BUTTON_SELECT_FROM_CATALOG;
  public static String _UI_WARNING_MORE_THAN_ONE_NS_WITH_NAME;
  public static String _UI_WARNING_MORE_THAN_ONE_NS_WITHOUT_NAME;
  public static String _UI_WARNING_MORE_THAN_ONE_NS_WITHOUT_PREFIX;
  public static String _UI_WARNING_MORE_THAN_ONE_NS_WITH_PREFIX;
  public static String _UI_WARNING_SCHEMA_CAN_NOT_BE_LOCATED;
  public static String _UI_WARNING_LOCATION_HINT_NOT_SPECIFIED;
  public static String _UI_WARNING_NAMESPACE_NAME_NOT_SPECIFIED;
  public static String _UI_WARNING_PREFIX_NOT_SPECIFIED;
  public static String _UI_WARNING_ROOT_ELEMENT_MUST_BE_SPECIFIED;
  public static String _UI_WARNING_SYSTEM_ID_MUST_BE_SPECIFIED;
  public static String _UI_INVALID_NAME;
  public static String _UI_ENTER_REQ_PREFIX_AND_NAMESPACE;
  public static String _UI_SELECT_REGISTERED_NAMESPACES;
  public static String _UI_SPECIFY_NEW_NAMESPACE;
  public static String _UI_SELECT_NAMESPACE_TO_ADD;
  public static String _UI_ADD_NAMESPACE_DECLARATIONS;
  public static String _UI_NAMESPACE_DECLARATIONS;
  public static String _UI_TARGET_NAMESPACE;
  public static String _ERROR_XML_ATTRIBUTE_ALREADY_EXISTS;
  public static String _ERROR_XML_ATTRIBUTE_IS_INVALID;
  public static String error_message_goes_here;
  public static String SurroundWithNewElementQuickAssistProposal_0;
  public static String SurroundWithNewElementQuickAssistProposal_1;
  public static String RenameInFileQuickAssistProposal_0;
  public static String RenameInFileQuickAssistProposal_1;
  public static String InsertRequiredAttrsQuickAssistProposal_0;
  public static String InsertRequiredAttrsQuickAssistProposal_1;
  public static String EncodingSettings_0;
  public static String EncodingSettings_1;
  public static String DragNodeCommand_0;
  public static String DragNodeCommand_1;
  public static String CommonEditNamespacesDialog_0;
  public static String JFaceNodeAdapter_1;
  public static String QuickFixProcessorXML_0;
  public static String QuickFixProcessorXML_1;
  public static String QuickFixProcessorXML_2;
  public static String QuickFixProcessorXML_3;
  public static String QuickFixProcessorXML_4;
  public static String QuickFixProcessorXML_5;
  public static String QuickFixProcessorXML_6;
  public static String QuickFixProcessorXML_7;
  public static String QuickFixProcessorXML_8;
  public static String QuickFixProcessorXML_9;
  public static String QuickFixProcessorXML_10;
  public static String QuickFixProcessorXML_11;
  public static String QuickFixProcessorXML_12;
  public static String QuickFixProcessorXML_13;
  public static String QuickFixProcessorXML_14;
  public static String QuickFixProcessorXML_15;
  public static String XMLPropertySourceAdapter_0;
  public static String WorkbenchDefaultEncodingSettings_0;
  public static String refreshoutline_0;
  public static String Creating_files_encoding;
  public static String End_tag_has_attributes;
  public static String Attribute__is_missing_a_value;
  public static String Attribute__has_no_value;
  public static String Missing_end_tag_;
  public static String Missing_start_tag_;
  public static String ReconcileStepForMarkup_0;
  public static String ReconcileStepForMarkup_1;
  public static String ReconcileStepForMarkup_2;
  public static String ReconcileStepForMarkup_3;
  public static String ReconcileStepForMarkup_4;
  public static String ReconcileStepForMarkup_5;
  public static String ReconcileStepForMarkup_6;
  public static String End_with_;
  public static String SEVERE_internal_error_occu_UI_;
  public static String No_known_attribute__UI_;
  public static String Content_Assist_not_availab_UI_;
  public static String Element__is_unknown;
  public static String Comment__;
  public static String Close_with__;
  public static String End_with__;
  public static String Close_with___;
  public static String Close_with____;
  public static String _Has_no_available_child;
  public static String No_known_child_tag;
  public static String __Has_no_known_child;
  public static String No_known_child_tag_names;
  public static String The_document_element__;
  public static String No_definition_for_in;
  public static String No_definition_for;
  public static String No_content_model_for;
  public static String No_content_model_found_UI_;
  public static String Cleanup_UI_;
  public static String Compress_empty_element_tags_UI_;
  public static String Insert_required_attributes_UI_;
  public static String Insert_missing_tags_UI_;
  public static String Insert_single_proposals;
  public static String Quote_attribute_values_UI_;
  public static String Format_source_UI_;
  public static String Convert_EOL_codes_UI_;
  public static String Insert_XML_decl;
  public static String EOL_Windows_UI;
  public static String EOL_Unix_UI;
  public static String EOL_Mac_UI;
  public static String Creating_files;
  public static String Encoding_desc;
  public static String Encoding;
  public static String Creating_or_saving_files;
  public static String End_of_line_code_desc;
  public static String End_of_line_code;
  public static String EOL_Windows;
  public static String EOL_Unix;
  public static String EOL_Mac;
  public static String EOL_NoTranslation;
  public static String XMLFilesPreferencePage_ExtensionLabel;
  public static String XMLFilesPreferencePage_ExtensionError;
  public static String XMLContentAssistPreferencePage_Auto_Activation_UI_;
  public static String XMLContentAssistPreferencePage_Cycling_UI_;
  public static String Automatically_make_suggest_UI_;
  public static String Auto_Activation_Delay;
  public static String Not_an_integer;
  public static String Missing_integer;
  public static String Prompt_when_these_characte_UI_;
  public static String Formatting_UI_;
  public static String Line_width__UI_;
  public static String Split_multiple_attributes;
  public static String Align_final_bracket;
  public static String Preserve_PCDATA_Content;
  public static String Space_before_empty_close_tag;
  public static String Indent_using_tabs;
  public static String Indent_using_spaces;
  public static String Indentation_size;
  public static String Indentation_size_tip;
  public static String Clear_all_blank_lines_UI_;
  public static String Format_comments;
  public static String Format_comments_join_lines;
  public static String Grammar_Constraints;
  public static String Group_label_Insertion;
  public static String Use_inferred_grammar_in_absence_of;
  public static String Suggestion_Strategy;
  public static String Suggestion_Strategy_Lax;
  public static String Suggestion_Strategy_Strict;
  public static String Element____1;
  public static String Content_Model____2;
  public static String Attribute____3;
  public static String Data_Type____4;
  public static String Enumerated_Values____5;
  public static String Default_Value____6;
  public static String Documentation_view_default_msg;
  public static String SourceMenu_label;
  public static String Comment_label; // Resource bundle
  public static String Comment_tooltip; // Resource bundle
  public static String Comment_description; // Resource bundle
  public static String Uncomment_label; // Resource bundle
  public static String Uncomment_tooltip; // Resource bundle
  public static String Uncomment_description; // Resource bundle
  public static String ToggleComment_label; // Resource bundle
  public static String ToggleComment_tooltip; // Resource bundle
  public static String ToggleComment_description; // Resource bundle
  public static String AddBlockComment_label; // Resource bundle
  public static String AddBlockComment_tooltip; // Resource bundle
  public static String AddBlockComment_description; // Resource bundle
  public static String RemoveBlockComment_label; // Resource bundle
  public static String RemoveBlockComment_tooltip; // Resource bundle
  public static String RemoveBlockComment_description; // Resource bundle
  public static String CleanupDocument_label; // Resource bundle
  public static String CleanupDocument_tooltip; // Resource bundle
  public static String CleanupDocument_description; // Resource bundle
  public static String FindOccurrences_label; // Resource bundle
  public static String OpenFileFromSource_label; // Resource bundle
  public static String OpenFileFromSource_tooltip; // Resource bundle
  public static String OpenFileFromSource_description; // Resource bundle
  public static String XMLContentOutlineConfiguration_0;
  public static String XMLTyping_Auto_Complete;
  public static String XMLTyping_Auto_Remove;
  public static String XMLTyping_Complete_Comments;
  public static String XMLTyping_Close_Strings;
  public static String XMLTyping_Close_Brackets;
  public static String XMLTyping_Complete_End_Tags;
  public static String XMLTyping_Complete_Elements;
  public static String XMLTyping_Remove_End_Tags;
  public static String XMLTyping_Start_Tag;
  public static String XMLTyping_End_Tag;
  public static String StructureSelectEnclosing_label;
  public static String StructureSelectEnclosing_tooltip;
  public static String StructureSelectEnclosing_description;
  public static String StructureSelectNext_label;
  public static String StructureSelectNext_tooltip;
  public static String StructureSelectNext_description;
  public static String StructureSelectPrevious_label;
  public static String StructureSelectPrevious_tooltip;
  public static String StructureSelectPrevious_description;
  public static String MESSAGE_XML_VALIDATION_MESSAGE_UI_;
  public static String Indicate_no_grammar_specified;
  public static String Indicate_no_document_element;
  public static String Indicate_no_grammar_specified_severities_error;
  public static String Indicate_no_grammar_specified_severities_warning;
  public static String Indicate_no_grammar_specified_severities_ignore;
  public static String Validating_files;
  public static String SyntaxColoringPage_0;
  public static String SyntaxColoringPage_2;
  public static String SyntaxColoringPage_3;
  public static String SyntaxColoringPage_4;
  public static String SyntaxColoringPage_5;
  public static String SyntaxColoringPage_6;
  public static String EmptyFilePreferencePage_0;
  public static String _UI_STRUCTURED_TEXT_EDITOR_PREFS_LINK;
  public static String gotoMatchingTag_label;
  public static String gotoMatchingTag_description;
  public static String gotoMatchingTag_start;
  public static String gotoMatchingTag_end;
  public static String nextSibling_label;
  public static String nextSibling_description;
  public static String previousSibling_label;
  public static String previousSibling_description;
  public static String Use_XInclude;
  public static String Honour_all_schema_locations;
  public static String Open;
  public static String Open_With;
  public static String _UI_BUTTON_SORT;
  public static String MarkupValidation_files_label;
  public static String MarkupValidation_files;
  public static String Severity_error;
  public static String Severity_warning;
  public static String Severity_ignore;
  public static String Empty_element_tag;
  public static String End_tag_with_attributes;
  public static String Invalid_whitespace_before_tagname;
  public static String Missing_closing_bracket;
  public static String Missing_closing_quote;
  public static String Missing_end_tag;
  public static String Missing_start_tag;
  public static String Missing_quotes;
  public static String Namespace_in_pi_target;
  public static String Tag_name_missing;
  public static String Whitespace_at_start;

  static {
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, XMLUIMessages.class);
  }

  private XMLUIMessages() {
    // cannot create new instance
  }

  public static ResourceBundle getResourceBundle() {
    try {
      if (fResourceBundle == null) {
        fResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME);
      }
    } catch (MissingResourceException x) {
      fResourceBundle = null;
    }
    return fResourceBundle;
  }
}
