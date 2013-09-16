/**********************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.osgi.util.NLS;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Strings used by SSE UI
 * 
 * @plannedfor 1.0
 */
public class SSEUIMessages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.wst.sse.ui.internal.SSEUIPluginResources";//$NON-NLS-1$
  private static ResourceBundle fResourceBundle;

  public static ResourceBundle getResourceBundle() {
    try {
      if (fResourceBundle == null)
        fResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME);
    } catch (MissingResourceException x) {
      fResourceBundle = null;
    }
    return fResourceBundle;
  }

  private SSEUIMessages() {
    // cannot create new instance
  }

  static {
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, SSEUIMessages.class);
  }

  public static String Error_opening_file_UI_;
  public static String _UI_File_is_read_only;
  public static String _32concat_EXC_;
  public static String Multiple_errors;
  public static String _Undo_Text_Change__Ctrl_Z_UI_;
  public static String Undo_Text_Change__UI_;
  public static String _Undo__0___Ctrl_Z_UI_;
  public static String Undo___0___UI_;
  public static String _Redo_Text_Change__Ctrl_Y_UI_;
  public static String Redo_Text_Change__UI_;
  public static String _Redo__0___Ctrl_Y_UI_;
  public static String Redo___0___UI_;
  public static String Format_Document_UI_;
  public static String Format_Active_Elements_UI_;
  public static String Text_Cut_UI_;
  public static String Text_Paste_UI_;

  public static String Cleanup_Document_UI_;

  public static String Editor_Cut_label;
  public static String Editor_Cut_tooltip;
  public static String Editor_Cut_image;
  public static String Editor_Cut_description;
  public static String Editor_Copy_label;
  public static String Editor_Copy_tooltip;
  public static String Editor_Copy_image;
  public static String Editor_Copy_description;
  public static String Editor_Paste_label;
  public static String Editor_Paste_tooltip;
  public static String Editor_Paste_image;
  public static String Editor_Paste_description;
  public static String Editor_Delete_label;
  public static String Editor_Delete_tooltip;
  public static String Editor_Delete_image;
  public static String Editor_Delete_description;

  public static String ContentAssistProposals_label;
  public static String ContentAssistProposals_tooltip;
  public static String ContentAssistProposals_description;
  public static String QuickFix_label;
  public static String QuickFix_tooltip;
  public static String QuickFix_image;
  public static String QuickFix_description;
  public static String ToggleComment_label;
  public static String ToggleComment_tooltip;
  public static String ToggleComment_image;
  public static String ToggleComment_description;
  public static String ToggleComment_progress;
  public static String AddBlockComment_label;
  public static String AddBlockComment_tooltip;
  public static String AddBlockComment_image;
  public static String AddBlockComment_description;
  public static String RemoveBlockComment_label;
  public static String RemoveBlockComment_tooltip;
  public static String RemoveBlockComment_image;
  public static String RemoveBlockComment_description;
  public static String CleanupDocument_label;
  public static String CleanupDocument_tooltip;
  public static String CleanupDocument_image;
  public static String CleanupDocument_description;
  public static String FormatDocument_label;
  public static String FormatDocument_tooltip;
  public static String FormatDocument_image;
  public static String FormatDocument_description;
  public static String FormatActiveElements_label;
  public static String FormatActiveElements_tooltip;
  public static String FormatActiveElements_image;
  public static String FormatActiveElements_description;
  public static String OpenFileFromSource_label;
  public static String OpenFileFromSource_tooltip;
  public static String OpenFileFromSource_image;
  public static String OpenFileFromSource_description;
  public static String StructureSelectEnclosing_label;
  public static String StructureSelectEnclosing_tooltip;
  public static String StructureSelectEnclosing_description;
  public static String StructureSelectNext_label;
  public static String StructureSelectNext_tooltip;
  public static String StructureSelectNext_description;
  public static String StructureSelectPrevious_label;
  public static String StructureSelectPrevious_tooltip;
  public static String StructureSelectPrevious_description;
  public static String StructureSelectHistory_label;
  public static String StructureSelectHistory_tooltip;
  public static String StructureSelectHistory_description;
  public static String Text_Shift_Right_UI_;
  public static String Text_Shift_Left_UI_;

  public static String _4concat;
  public static String Content_type__UI_;
  public static String Foreground_UI_;
  public static String Foreground_Color_Selector_Button;
  public static String Background_UI_;
  public static String Background_Color_Selector_Button;
  public static String Bold_UI_;
  public static String Italics_UI;
  public static String Restore_Default_UI_;
  public static String Sample_text__UI_;

  public static String AddBreakpoint_label;
  public static String AddBreakpoint_tooltip;
  public static String AddBreakpoint_description;
  public static String AddBreakpoint_error_dialog_title;
  public static String AddBreakpoint_error_dialog_message;
  public static String ToggleBreakpoint_label;
  public static String ToggleBreakpoint_tooltip;
  public static String ToggleBreakpoint_description;
  public static String EnableBreakpoint_label;
  public static String EnableBreakpoint_tooltip;
  public static String EnableBreakpoint_description;
  public static String DisableBreakpoint_label;
  public static String DisableBreakpoint_tooltip;
  public static String DisableBreakpoint_description;
  public static String ManageBreakpoints_add_label;
  public static String ManageBreakpoints_remove_label;
  public static String ManageBreakpoints_tooltip;
  public static String ManageBreakpoints_error_adding_title1;
  public static String ManageBreakpoints_error_adding_message1;

  public static String AbstractColorPageDescription;
  public static String SyntaxColoring_Link;
  public static String SyntaxColoring_Description;
  public static String EditorModelUtil_0;
  public static String EditorModelUtil_1;

  // TODO: These should be removed when ContentSettingsPropertyPage is
  // deleted
  // web content settings
  public static String UI_Default_HTML_DOCTYPE_ID___1;
  public static String UI_CSS_profile___2;
  public static String UI_Target_Device___3;

  public static String Editor_ToggleInsertMode_label;

  //
  // These strings are used in Workbench menu bar
  //
  public static String ExpandSelectionToMenu_label;
  public static String SourceMenu_label;
  public static String RefactorMenu_label;

  public static String FindOccurrencesActionProvider_0;
  public static String RemoveAction_0;
  public static String ShowPropertiesAction_0;
  public static String ContentOutlineConfiguration_0;
  public static String ContentOutlineConfiguration_1;
  public static String AbstractOpenOn_0;
  public static String FormatActionDelegate_jobName;
  public static String FormatActionDelegate_errorStatusMessage;
  public static String FormatActionDelegate_3;
  public static String FormatActionDelegate_4;
  public static String FormatActionDelegate_5;

  public static String TranslucencyPreferenceTab_0;
  public static String TranslucencyPreferenceTab_1;
  public static String StructuredTextEditorPreferencePage_2;
  public static String StructuredTextEditorPreferencePage_6;
  public static String StructuredTextEditorPreferencePage_20;
  public static String StructuredTextEditorPreferencePage_23;
  public static String StructuredTextEditorPreferencePage_24;
  public static String StructuredTextEditorPreferencePage_30;
  public static String StructuredTextEditorPreferencePage_37;
  public static String StructuredTextEditorPreferencePage_38;
  public static String StructuredTextEditorPreferencePage_40;
  public static String StructuredTextEditorPreferencePage_41;
  public static String StructuredTextEditorPreferencePage_42;
  public static String StructuredTextEditorPreferencePage_43;
  public static String StructuredTextEditorPreferencePage_44;
  public static String TaskTagPreferenceTab_0;
  public static String TaskTagPreferenceTab_1;
  public static String TaskTagPreferenceTab_2;
  public static String TaskTagPreferenceTab_3;
  public static String TaskTagPreferenceTab_5;
  public static String TaskTagPreferenceTab_6;
  public static String TaskTagPreferenceTab_7;
  public static String TaskTagPreferenceTab_12;
  public static String TaskTagPreferenceTab_13;
  public static String TaskTagPreferenceTab_14;
  public static String TaskTagPreferenceTab_15;
  public static String TaskTagPreferenceTab_16;
  public static String TaskTagPreferenceTab_17;
  public static String TaskTagPreferenceTab_18;
  public static String TaskTagPreferenceTab_19;
  public static String TaskTagPreferenceTab_20;
  public static String TaskTagPreferenceTab_22;
  public static String TaskTagPreferenceTab_23;
  public static String TaskTagPreferenceTab_24;
  public static String TaskTagPreferenceTab_25;
  public static String TaskTagPreferenceTab_26;
  public static String TaskTagPreferenceTab_27;
  public static String TaskTagPreferenceTab_28;
  public static String TaskTagPreferenceTab_29;
  public static String TaskTagPreferenceTab_30;
  public static String TaskTagPreferenceTab_31;
  public static String TaskTagPreferencePage_32;
  public static String TaskTagPreferenceTab_33;
  public static String TaskTagExclusionTab_01;
  public static String TaskTagExclusionTab_02;
  public static String TaskTagExclusionTab_03;

  public static String PropertyPreferencePage_01;
  public static String PropertyPreferencePage_02;

  public static String FilePreferencePage_0;
  public static String NoModificationCompletionProposal_0;
  public static String ToggleBreakpointAction_0;
  public static String ManageBreakpointAction_0;
  public static String ManageBreakpointAction_1;
  public static String EditBreakpointAction_0;
  // Used in Structured Text Editor Preference Page / Hovers Tab
  public static String TextHoverPreferenceTab_title;
  public static String TextHoverPreferenceTab_hoverPreferences;
  public static String TextHoverPreferenceTab_keyModifier;
  public static String TextHoverPreferenceTab_description;
  public static String TextHoverPreferenceTab_modifierIsNotValid;
  public static String TextHoverPreferenceTab_modifierIsNotValidForHover;
  public static String TextHoverPreferenceTab_duplicateModifier;
  public static String TextHoverPreferenceTab_nameColumnTitle;
  public static String TextHoverPreferenceTab_modifierColumnTitle;
  public static String TextHoverPreferenceTab_delimiter;
  public static String TextHoverPreferenceTab_insertDelimiterAndModifierAndDelimiter;
  public static String TextHoverPreferenceTab_insertModifierAndDelimiter;
  public static String TextHoverPreferenceTab_insertDelimiterAndModifier;

  // used dynamically
  public static String combinationHover_label;
  public static String combinationHover_desc;
  public static String problemHover_label;
  public static String problemHover_desc;
  public static String documentationHover_label;
  public static String documentationHover_desc;
  public static String annotationHover_label;
  public static String annotationHover_desc;

  public static String EditStructuredTextEditorPreferencesAction_0;
  public static String StructuredTextEditorPreferencePage_0;
  public static String StructuredTextEditorPreferencePage_1;
  public static String PreferenceManager_0;

  public static String OccurrencesSearchQuery_0;
  public static String OccurrencesSearchQuery_2;
  public static String ShowView_errorTitle;
  public static String proc_dirty_regions_0;

  public static String textHoverMakeStickyHint;

  // Encoding
  public static String EncodingPreferencePage_0;
  public static String EncodingPreferencePage_1;

  public static String caret_update;
  public static String EmptyFilePreferencePage_0;

  public static String OffsetStatusLineContributionItem_0;
  public static String OffsetStatusLineContributionItem_2;
  public static String OffsetStatusLineContributionItem_3;
  public static String OffsetStatusLineContributionItem_4;
  public static String OffsetStatusLineContributionItem_5;
  public static String OffsetStatusLineContributionItem_6;
  public static String OffsetStatusLineContributionItem_7;
  public static String OffsetStatusLineContributionItem_8;
  public static String OffsetStatusLineContributionItem_9;
  public static String OffsetStatusLineContributionItem_10;
  public static String OffsetStatusLineContributionItem_11;
  public static String OffsetStatusLineContributionItem_12;
  public static String OffsetStatusLineContributionItem_13;
  public static String OffsetStatusLineContributionItem_14;
  public static String OffsetStatusLineContributionItem_15;
  public static String OffsetStatusLineContributionItem_16;
  public static String OffsetStatusLineContributionItem_17;
  public static String OffsetStatusLineContributionItem_18;
  public static String OffsetStatusLineContributionItem_19;
  public static String OffsetStatusLineContributionItem_20;

  /*
   * *****Below are possibly unused strings that may be removed *****
   */
  public static String Save_label;
  public static String An_error_has_occurred_when_ERROR_;
  public static String Problems_During_Save_As_UI_;
  public static String Save_could_not_be_complete_UI_;
  public static String SemanticHighlightingReconciler_0;
  public static String ManageBreakpoints_error_removing_title1;
  public static String ManageBreakpoints_error_removing_message1;
  public static String ManageBreakpoints_error_retrieving_message;
  public static String JSPSourcePreferencePageDescription;
  public static String Editor_error_save_message;
  public static String Editor_error_save_title;
  public static String Editor_warning_save_delete;

  public static String Previous_annotation;
  public static String Next_annotation;

  public static String AnnotationTypes_Errors;
  public static String AnnotationTypes_Warnings;
  public static String AnnotationTypes_Tasks;
  public static String AnnotationTypes_SearchResults;
  public static String AnnotationTypes_Bookmarks;
  public static String AnnotationTypes_Others;

  public static String Editor_ConvertToWindows_label;
  public static String Editor_ConvertToWindows_tooltip;
  public static String Editor_ConvertToWindows_image;
  public static String Editor_ConvertToWindows_description;

  public static String Editor_ConvertToUNIX_label;
  public static String Editor_ConvertToUNIX_tooltip;
  public static String Editor_ConvertToUNIX_image;
  public static String Editor_ConvertToUNIX_description;

  public static String Editor_ConvertToMac_label;
  public static String Editor_ConvertToMac_tooltip;
  public static String Editor_ConvertToMac_image;
  public static String Editor_ConvertToMac_description;
  public static String ConvertLineDelimitersMenu_label;
  public static String FindOccurrences_label;

  public static String ConvertLineDelimitersToCRLFActionDelegate_jobName;
  public static String ConvertLineDelimitersToCRLFActionDelegate_errorStatusMessage;
  public static String ConvertLineDelimitersToCRLFActionDelegate_3;
  public static String ConvertLineDelimitersToCRLFActionDelegate_4;

  public static String TextHoverPreferenceTab_annotationRollover;
  public static String TextHoverPreferenceTab_showAffordance;
  public static String TextHoverPreferenceTab_enabled;

  public static String BasicFindOccurrencesAction_0;
  public static String BasicSearchLabelProvider_0;
  public static String NavigationPreferenceTab_0;
  public static String OccurrencesSearchQuery_1;
  public static String FileModelProvider_0;
  public static String JFaceNodeAdapter_0;

  public static String ConfigureProjectSettings;
  public static String ConfigureWorkspaceSettings;
  public static String EnableProjectSettings;

  public static String GotoMatchingBracket_label;
  public static String GotoMatchingBracket_description;
  public static String GotoMatchingBracket_tooltip;
  public static String GotoMatchingBracket_error_invalidSelection;
  public static String GotoMatchingBracket_error_noMatchingBracket;
  public static String GotoMatchingBracket_error_bracketOutsideSelectedElement;

  public static String LoadingReferencedGrammars;

  public static String Folding;
  public static String StructuredTextEditorPreferencePage_3;
  public static String Projection_Toggle_label;
  public static String Projection_Toggle_tooltip;
  public static String Projection_Toggle_description;
  public static String Projection_Toggle_image;
  public static String Projection_ExpandAll_label;
  public static String Projection_ExpandAll_tooltip;
  public static String Projection_ExpandAll_description;
  public static String Projection_ExpandAll_image;
  public static String Projection_CollapseAll_label;
  public static String Projection_CollapseAll_tooltip;
  public static String Projection_CollapseAll_description;
  public static String Projection_CollapseAll_image;

  // These strings are accessed using resource bundle and in properties
  // file, need to use '.' instead of '_' in some keys
  public static String Editor_ManageBookmarks_tooltip;
  public static String Editor_ManageBookmarks_image;
  public static String Editor_ManageBookmarks_description;
  public static String Editor_ManageBookmarks_add_label;
  public static String Editor_ManageBookmarks_remove_label;
  public static String Editor_ManageBookmarks_add_dialog_title;
  public static String Editor_ManageBookmarks_add_dialog_message;
  public static String Editor_ManageBookmarks_error_dialog_title;
  public static String Editor_ManageBookmarks_error_dialog_message;
  public static String Editor_ManageTasks_tooltip;
  public static String Editor_ManageTasks_image;
  public static String Editor_ManageTasks_description;
  public static String Editor_ManageTasks_add_label;
  public static String Editor_ManageTasks_remove_label;
  public static String Editor_ManageTasks_add_dialog_title;
  public static String Editor_ManageTasks_add_dialog_message;
  public static String Editor_ManageTasks_error_dialog_title;
  public static String Editor_ManageTasks_error_dialog_message;
  /*
   * *****Above are possibly unused strings that may be removed*****
   */

  public static String StructuredTextEditorPreferencePage_39;
  public static String StructuredTextEditor_0;
  public static String UnknownContentTypeDialog_0;
  public static String UnknownContentTypeDialog_1;
  public static String UnknownContentTypeDialog_2;
  public static String StyledTextColorPicker_0;

  public static String TextSearchLabelProvider_matchCountFormat;

  //content assist messages
  public static String ContentAssist_computing_proposals;
  public static String ContentAssist_collecting_proposals;
  public static String ContentAssist_sorting_proposals;
  public static String ContentAssist_collecting_contexts;
  public static String ContentAssist_computing_contexts;
  public static String ContentAssist_sorting_contexts;
  public static String ContentAssist_no_completions;
  public static String ContentAssist_all_disabled_title;
  public static String ContentAssist_all_disabled_message;
  public static String ContentAssist_all_disabled_preference_link;
  public static String ContentAssist_no_message;
  public static String ContentAssist_toggle_affordance_update_message;
  public static String ContentAssist_defaultProposalCategory_title;
  public static String ContentAssist_press;
  public static String ContentAssist_click;
  public static String OptionalMessageDialog_dontShowAgain;

  //content assist preference messages
  public static String CodeAssistAdvancedConfigurationBlock_page_description;
  public static String CodeAssistAdvancedConfigurationBlock_no_shortcut;
  public static String CodeAssistAdvancedConfigurationBlock_Up;
  public static String CodeAssistAdvancedConfigurationBlock_Down;
  public static String CodeAssistAdvancedConfigurationBlock_separate_table_description;
  public static String CodeAssistAdvancedConfigurationBlock_default_table_description;
  public static String CodeAssistAdvancedConfigurationBlock_default_table_category_column_title;
  public static String CodeAssistAdvancedConfigurationBlock_PagesDown;
  public static String CodeAssistAdvancedConfigurationBlock_PagesUp;
  public static String CodeAssistAdvancedConfigurationBlock_separate_table_category_column_title;

  // Validation
  public static String Validation_Title;
  public static String Validation_Workspace;
  public static String Validation_Project;
  public static String Validation_jobName;

}
