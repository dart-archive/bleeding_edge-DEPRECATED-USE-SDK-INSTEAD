/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.refactoring;

import org.eclipse.osgi.util.NLS;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public final class RefactoringMessages extends NLS {
  ///////////////////////////////////////////////////////////////////////////////////////
  // Rename refactoring
  ///////////////////////////////////////////////////////////////////////////////////////
  // RenameAction
  public static String RenameAction_text;
  public static String RenameAction_rename;
  public static String RenameAction_unavailable;
  // RenameDartElementAction
  public static String RenameDartElementAction_name;
  public static String RenameDartElementAction_exception;
  public static String RenameDartElementAction_not_available;
  // RefactorActionGroup
  public static String RefactorActionGroup_no_refactoring_available;
  // DartStatusContextViewer
  public static String DartStatusContextViewer_no_source_available;
  // RefactoringExecutionHelper
  public static String RefactoringExecutionHelper_cannot_execute;
  public static String RefactoringStarter_saving;
  public static String RefactoringStarter_unexpected_exception;
  // RenameInputWizardPage
  public static String RenameRefactoringWizard_internal_error;
  public static String RenameInputWizardPage_new_name;
  // RenameFieldWizard
  public static String RenameFieldWizard_defaultPageTitle;
  public static String RenameFieldWizard_inputPage_description;
  // RenameFunctionTypeAliasWizard
  public static String RenameFunctionTypeAliasWizard_defaultPageTitle;
  public static String RenameFunctionTypeAliasWizardInputPage_description;
  // RenameFunctionWizard
  public static String RenameFunctionWizard_defaultPageTitle;
  public static String RenameFunctionWizardInputPage_description;
  // RenameGlobalVariableWizard
  public static String RenameGlobalVariableWizard_defaultPageTitle;
  public static String RenameGlobalVariableWizardInputPage_description;
  // RenameLocalVariableWizard
  public static String RenameLocalVariableWizard_defaultPageTitle;
  public static String RenameLocalVariableWizard_inputPage_description;
  // RenameMethodWizard
  public static String RenameMethodWizard_defaultPageTitle;
  public static String RenameMethodWizard_inputPage_description;
  // RenameTypeParameterWizard
  public static String RenameTypeParameterWizard_defaultPageTitle;
  public static String RenameTypeParameterWizard_inputPage_description;
  // RenameTypeWizard
  public static String RenameTypeWizard_defaultPageTitle;
  public static String RenameTypeWizardInputPage_description;
  // RenameProjectParticipant
  public static String RenameProjectParticipant_name;
  // RenameResourceParticipant
  public static String RenameResourceParticipant_name;
  // DeleteResourceParticipant
  public static String DeleteResourceParticipant_name;
  public static String DeleteResourceParticipant_remove_reference;

  ///////////////////////////////////////////////////////////////////////////////////////
  // Extract local refactoring
  ///////////////////////////////////////////////////////////////////////////////////////
  public static String ExtractLocalAction_label;
  public static String ExtractLocalAction_dialog_title;
  public static String ExtractLocalInputPage_enter_name;
  public static String ExtractLocalInputPage_variable_name;
  public static String ExtractLocalInputPage_replace_all;
  public static String ExtractLocalWizard_defaultPageTitle;

  ///////////////////////////////////////////////////////////////////////////////////////
  // Inline action
  ///////////////////////////////////////////////////////////////////////////////////////
  public static String InlineAction_dialog_title;
  public static String InlineAction_Inline;
  public static String InlineAction_select;

  ///////////////////////////////////////////////////////////////////////////////////////
  // Inline local refactoring
  ///////////////////////////////////////////////////////////////////////////////////////
  public static String InlineLocalAction_dialog_title;
  public static String InlineLocalAction_label;
  public static String InlineLocalInputPage_message_multi;
  public static String InlineLocalInputPage_message_zero;
  public static String InlineLocalInputPage_message_one;
  public static String InlineLocalWizard_defaultPageTitle;

  ///////////////////////////////////////////////////////////////////////////////////////
  // Inline method refactoring
  ///////////////////////////////////////////////////////////////////////////////////////
  public static String InlineMethodAction_dialog_title;
  public static String InlineMethodAction_inline_Method;
  public static String InlineMethodAction_no_method_invocation_or_declaration_selected;
  public static String InlineMethodAction_unexpected_exception;
  public static String InlineMethodWizard_page_title;
  public static String InlineMethodInputPage_all_invocations;
  public static String InlineMethodInputPage_delete_declaration;
  public static String InlineMethodInputPage_description;
  public static String InlineMethodInputPage_inline_method;
  public static String InlineMethodInputPage_only_selected;

  ///////////////////////////////////////////////////////////////////////////////////////
  // Convert method <=> getter
  ///////////////////////////////////////////////////////////////////////////////////////
  public static String ConvertMethodToGetterAction_title;
  public static String ConvertMethodToGetterAction_dialog_title;
  public static String ConvertMethodToGetterAction_select;
  public static String ConvertMethodToGetterAction_already_getter;
  public static String ConvertMethodToGetterAction_only_without_arguments;
  public static String ConvertMethodToGetterWizard_page_title;
  public static String ConvertOptionalParametersToNamedWizard_page_title;
  public static String ConvertOptionalParametersToNamedAction_dialog_title;
  public static String ConvertOptionalParametersToNamedAction_noOptionalPositional;

  // XXX
  // These constants are not used yet in Dart refactoring.
  //

  private static final String BUNDLE_NAME = RefactoringMessages.class.getName();

  public static String ChangeExceptionHandler_abort_button;

  public static String ChangeExceptionHandler_dialog_message;

  public static String ChangeExceptionHandler_dialog_title;

  public static String ChangeExceptionHandler_message;

  public static String ChangeExceptionHandler_status_without_detail;

  public static String ChangeExceptionHandler_undo_button;

  public static String ChangeExceptionHandler_undo_dialog_message;

  public static String ChangeExceptionHandler_undo_dialog_title;

  public static String ChangeExceptionsControl_buttons_add;

  public static String ChangeExceptionsControl_buttons_remove;

  public static String ChangeExceptionsControl_choose_message;

  public static String ChangeExceptionsControl_choose_title;

  public static String ChangeExceptionsControl_not_exception;

  public static String ChangeParametersControl_buttons_add;

  public static String ChangeParametersControl_buttons_edit;

  public static String ChangeParametersControl_buttons_move_down;

  public static String ChangeParametersControl_buttons_move_up;

  public static String ChangeParametersControl_buttons_remove;

  public static String ChangeParametersControl_new_parameter_default_name;

  public static String ChangeParametersControl_table_defaultValue;

  public static String ChangeParametersControl_table_name;

  public static String ChangeParametersControl_table_type;

  public static String ChangeSignatureInputPage_access_modifier;

  public static String ChangeSignatureInputPage_change;

  public static String ChangeSignatureInputPage_Change_Signature;

  public static String ChangeSignatureInputPage_default;

  public static String ChangeSignatureInputPage_exception;

  public static String ChangeSignatureInputPage_exceptions;

  public static String ChangeSignatureInputPage_Internal_Error;

  public static String ChangeSignatureInputPage_method_name;

  public static String ChangeSignatureInputPage_method_Signature_Preview;

  public static String ChangeSignatureInputPage_parameters;

  public static String ChangeSignatureInputPage_return_type;

  public static String ChangeSignatureInputPage_unchanged;

  public static String ChangeSignatureRefactoring_modify_Parameters;

  public static String ChangeTypeAction_description;

  public static String ChangeTypeAction_dialog_title;

  public static String ChangeTypeAction_exception;

  public static String ChangeTypeAction_label;

  public static String ChangeTypeAction_tooltipText;

  public static String ChangeTypeInputPage_Select_Type;

  public static String ChangeTypeWizard_analyzing;

  public static String ChangeTypeWizard_computationInterrupted;

  public static String ChangeTypeWizard_declCannotBeChanged;

  public static String ChangeTypeWizard_grayed_types;

  public static String ChangeTypeWizard_internalError;

  public static String ChangeTypeWizard_pleaseChooseType;

  public static String ChangeTypeWizard_title;

  public static String ChangeTypeWizard_with_itself;

  public static String ConvertAnonymousToNestedAction_Convert_Anonymous;

  public static String ConvertAnonymousToNestedAction_dialog_title;

  public static String ConvertAnonymousToNestedAction_wizard_title;

  public static String ConvertAnonymousToNestedInputPage_class_name;

  public static String ConvertAnonymousToNestedInputPage_declare_final;

  public static String ConvertAnonymousToNestedInputPage_declare_static;

  public static String ConvertAnonymousToNestedInputPage_description;

  public static String ConvertLocalToField_label;

  public static String ConvertLocalToField_title;

  public static String ConvertNestedToTopAction_Convert;

  public static String ConvertNestedToTopAction_To_activate;

  public static String DelegateCreator_deprecate_delegates;

  public static String DeleteWizard_1;

  public static String DeleteWizard_10;

  public static String DeleteWizard_11;

  public static String DeleteWizard_12_singular;

  public static String DeleteWizard_12_plural;

  public static String DeleteWizard_2;

  public static String DeleteWizard_3;

  public static String DeleteWizard_4;

  public static String DeleteWizard_5;

  public static String DeleteWizard_6;

  public static String DeleteWizard_7;

  public static String DeleteWizard_8;

  public static String DeleteWizard_9;

  public static String DeleteWizard_also_delete_sub_packages;

  public static String ExtractClassAction_action_text;

  public static String ExtractClassWizard_button_edit;

  public static String ExtractClassWizard_checkbox_create_gettersetter;

  public static String ExtractClassWizard_column_name;

  public static String ExtractClassWizard_column_type;

  public static String ExtractClassWizard_dialog_message;

  public static String ExtractClassWizard_dialog_title;

  public static String ExtractClassWizard_field_name;

  public static String ExtractClassWizard_label_class_name;

  public static String ExtractClassWizard_label_destination;

  public static String ExtractClassWizard_label_select_fields;

  public static String ExtractClassWizard_not_available;

  public static String ExtractClassWizard_page_title;

  public static String ExtractClassWizard_radio_nested;

  public static String ExtractClassWizard_radio_top_level;

  public static String ExtractConstantAction_extract_constant;

  public static String ExtractConstantAction_label;

  public static String ExtractConstantInputPage_access_modifiers;

  public static String ExtractConstantInputPage_constant_name;

  public static String ExtractConstantInputPage_enter_name;

  public static String ExtractConstantInputPage_exception;

  public static String ExtractConstantInputPage_Internal_Error;

  public static String ExtractConstantInputPage_qualify_constant_references_with_class_name;

  public static String ExtractConstantInputPage_replace_all;

  public static String ExtractConstantInputPage_selection_refers_to_nonfinal_fields;

  public static String ExtractConstantInputPage_signature_preview;

  public static String ExtractConstantWizard_defaultPageTitle;

  public static String ExtractInterfaceAction_Extract_Interface;

  public static String ExtractInterfaceAction_To_activate;

  public static String ExtractInterfaceInputPage_change_references;

  public static String ExtractInterfaceInputPage_description;

  public static String ExtractInterfaceInputPage_Deselect_All;

  public static String ExtractInterfaceInputPage_Extract_Interface;

  public static String ExtractInterfaceInputPage_Interface_name;

  public static String ExtractInterfaceInputPage_Internal_Error;

  public static String ExtractInterfaceInputPage_Members;

  public static String ExtractInterfaceInputPage_Select_All;

  public static String ExtractInterfaceWizard_12;

  public static String ExtractInterfaceWizard_abstract_label;

  public static String ExtractInterfaceWizard_Extract_Interface;

  public static String ExtractInterfaceWizard_generate_annotations;

  public static String ExtractInterfaceWizard_generate_comments;

  public static String ExtractInterfaceWizard_public_label;

  public static String ExtractInterfaceWizard_use_supertype;

  public static String ExtractMethodAction_dialog_title;

  public static String ExtractMethodAction_label;

  public static String ExtractMethodInputPage_access_Modifiers;

  public static String ExtractMethodInputPage_anonymous_type_label;

  public static String ExtractMethodInputPage_default;

  public static String ExtractMethodInputPage_description;

  public static String ExtractMethodInputPage_destination_type;

  public static String ExtractMethodInputPage_duplicates_multi;

  public static String ExtractMethodInputPage_duplicates_none;

  public static String ExtractMethodInputPage_duplicates_single;

  public static String ExtractMethodInputPage_generateJavadocComment;

  public static String ExtractMethodInputPage_label_text;

  public static String ExtractMethodInputPage_parameters;

  public static String ExtractMethodInputPage_private;

  public static String ExtractMethodInputPage_protected;

  public static String ExtractMethodInputPage_public;

  public static String ExtractMethodInputPage_signature_preview;

  public static String ExtractMethodInputPage_throwRuntimeExceptions;

  public static String ExtractMethodInputPage_validation_emptyMethodName;

  public static String ExtractMethodInputPage_validation_emptyParameterName;

  public static String ExtractMethodWizard_extract_method;

  public static String ExtractSuperTypeAction_label;

  public static String ExtractSuperTypeAction_unavailable;

  public static String ExtractSupertypeMemberPage_add_button_label;

  public static String ExtractSupertypeMemberPage_choose_type_caption;

  public static String ExtractSupertypeMemberPage_choose_type_message;

  public static String ExtractSupertypeMemberPage_create_stubs_label;

  public static String ExtractSupertypeMemberPage_declare_abstract;

  public static String ExtractSupertypeMemberPage_extract;

  public static String ExtractSupertypeMemberPage_extract_supertype;

  public static String ExtractSupertypeMemberPage_name_label;

  public static String ExtractSupertypeMemberPage_no_members_selected;

  public static String ExtractSupertypeMemberPage_page_title;

  public static String ExtractSupertypeMemberPage_remove_button_label;

  public static String ExtractSupertypeMemberPage_types_list_caption;

  public static String ExtractSupertypeMemberPage_use_instanceof_label;

  public static String ExtractSupertypeMemberPage_use_supertype_label;

  public static String ExtractSupertypeWizard_defaultPageTitle;

  public static String ExtractTempInputPage_declare_final;

  public static String ExtractTempInputPage_extract_local;

  public static String InferTypeArgumentsAction_dialog_title;

  public static String InferTypeArgumentsAction_label;

  public static String InferTypeArgumentsAction_unavailable;

  public static String InferTypeArgumentsInputPage_description;

  public static String InferTypeArgumentsWizard_assumeCloneSameType;

  public static String InferTypeArgumentsWizard_defaultPageTitle;

  public static String InferTypeArgumentsWizard_leaveUnconstrainedRaw;

  public static String InferTypeArgumentsWizard_lengthyDescription;

  public static String InlineConstantAction_dialog_title;

  public static String InlineConstantAction_inline_Constant;

  public static String InlineConstantAction_no_constant_reference_or_declaration;

  public static String InlineConstantAction_unexpected_exception;

  public static String InlineConstantInputPage_All_references;

  public static String InlineConstantInputPage_Delete_constant;

  public static String InlineConstantInputPage_Inline_constant;

  public static String InlineConstantInputPage_Only_selected;

  public static String InlineConstantWizard_initializer_refers_to_fields;

  public static String InlineConstantWizard_Inline_Constant;

  public static String InlineConstantWizard_message;

  public static String IntroduceFactoryAction_description;

  public static String IntroduceFactoryAction_dialog_title;

  public static String IntroduceFactoryAction_exception;

  public static String IntroduceFactoryAction_label;

  public static String IntroduceFactoryAction_tooltipText;

  public static String IntroduceFactoryAction_use_factory;

  public static String IntroduceFactoryInputPage_browseLabel;

  public static String IntroduceFactoryInputPage_chooseFactoryClass_message;

  public static String IntroduceFactoryInputPage_chooseFactoryClass_title;

  public static String IntroduceFactoryInputPage_factoryClassLabel;

  public static String IntroduceFactoryInputPage_method_name;

  public static String IntroduceFactoryInputPage_name_factory;

  public static String IntroduceFactoryInputPage_protectConstructorLabel;

  public static String IntroduceIndirectionAction_description;

  public static String IntroduceIndirectionAction_dialog_title;

  public static String IntroduceIndirectionAction_title;

  public static String IntroduceIndirectionAction_tooltip;

  public static String IntroduceIndirectionAction_unknown_exception;

  public static String IntroduceIndirectionInputPage_browse;

  public static String IntroduceIndirectionInputPage_declaring_class;

  public static String IntroduceIndirectionInputPage_dialog_choose_declaring_class;

  public static String IntroduceIndirectionInputPage_dialog_choose_declaring_class_long;

  public static String IntroduceIndirectionInputPage_new_method_name;

  public static String IntroduceIndirectionInputPage_update_references;

  public static String IntroduceParameterAction_dialog_title;

  public static String IntroduceParameterAction_label;

  public static String IntroduceParameterInputPage_description;

  public static String IntroduceParameterObjectWizard_classnamefield_label;

  public static String IntroduceParameterObjectWizard_createasnestedclass_radio;

  public static String IntroduceParameterObjectWizard_createastoplevel_radio;

  public static String IntroduceParameterObjectWizard_creategetter_checkbox;

  public static String IntroduceParameterObjectWizard_createsetter_checkbox;

  public static String IntroduceParameterObjectWizard_destination_label;

  public static String IntroduceParameterObjectWizard_dot_not_allowed_error;

  public static String IntroduceParameterObjectWizard_edit_button;

  public static String IntroduceParameterObjectWizard_error_description;

  public static String IntroduceParameterObjectWizard_error_title;

  public static String IntroduceParameterObjectWizard_fieldname_message;

  public static String IntroduceParameterObjectWizard_fieldname_title;

  public static String IntroduceParameterObjectWizard_fields_selection_label;

  public static String IntroduceParameterObjectWizard_type_already_exists_in_package_info;

  public static String IntroduceParameterObjectWizard_method_group;

  public static String IntroduceParameterObjectWizard_moveentryup_button;

  public static String IntroduceParameterObjectWizard_moventrydown_button;

  public static String IntroduceParameterObjectWizard_name_column;

  public static String IntroduceParameterObjectWizard_parameterfield_label;

  public static String IntroduceParameterObjectWizard_parametername_check_alreadyexists;

  public static String IntroduceParameterObjectWizard_parametername_check_atleastoneparameter;

  public static String IntroduceParameterObjectWizard_parametername_check_notunique;

  public static String IntroduceParameterObjectWizard_signaturepreview_label;

  public static String IntroduceParameterObjectWizard_type_column;

  public static String IntroduceParameterObjectWizard_type_group;

  public static String IntroduceParameterObjectWizard_wizardpage_description;

  public static String IntroduceParameterObjectWizard_wizardpage_name;

  public static String IntroduceParameterObjectWizard_wizardpage_title;

  public static String IntroduceParameterWizard_defaultPageTitle;

  public static String IntroduceParameterWizard_parameters;

  public static String JavaStatusContextViewer_no_source_found0;

  public static String JavaTypeCompletionProcessor_no_completion;

  public static String ModifyParametersAction_unavailable;

  public static String MoveAction_Move;

  public static String MoveAction_select;

  public static String MoveAction_text;

  public static String MoveInnerToToplnputPage_description;

  public static String MoveInnerToToplnputPage_enter_name;

  public static String MoveInnerToToplnputPage_enter_name_mandatory;

  public static String MoveInnerToToplnputPage_instance_final;

  public static String MoveInnerToToplnputPage_mandatory_info;

  public static String MoveInnerToToplnputPage_optional_info;

  public static String MoveInnerToTopWizard_Move_Inner;

  public static String MoveInstanceMethodAction_dialog_title;

  public static String MoveInstanceMethodAction_Move_Method;

  public static String MoveInstanceMethodAction_No_reference_or_declaration;

  public static String MoveInstanceMethodAction_unexpected_exception;

  public static String MoveInstanceMethodPage_invalid_target;

  public static String MoveInstanceMethodPage_Method_name;

  public static String MoveInstanceMethodPage_Name;

  public static String MoveInstanceMethodPage_New_receiver;

  public static String MoveInstanceMethodPage_Target_name;

  public static String MoveInstanceMethodPage_Type;

  public static String MoveInstanceMethodWizard_Move_Method;

  public static String MoveMembersAction_unavailable;

  public static String MoveMembersInputPage_browse;

  public static String MoveMembersInputPage_choose_Type;

  public static String MoveMembersInputPage_descriptionKey_singular;

  public static String MoveMembersInputPage_descriptionKey_plural;

  public static String MoveMembersInputPage_destination_multi;

  public static String MoveMembersInputPage_destination_single;

  public static String MoveMembersInputPage_dialogMessage;

  public static String MoveMembersInputPage_exception;

  public static String MoveMembersInputPage_invalid_name;

  public static String MoveMembersInputPage_Invalid_selection;

  public static String MoveMembersInputPage_move_Member;

  public static String MoveMembersInputPage_no_binary;

  public static String MoveMembersInputPage_not_found;

  public static String MoveMembersWizard_page_title;

  public static String NewTextRefactoringAction_exception;

  public static String OpenRefactoringWizardAction_exception;

  public static String OpenRefactoringWizardAction_refactoring;

  public static String OpenRefactoringWizardAction_unavailable;

  public static String ParameterEditDialog_defaultValue;

  public static String ParameterEditDialog_defaultValue_error;

  public static String ParameterEditDialog_defaultValue_invalid;

  public static String ParameterEditDialog_message;

  public static String ParameterEditDialog_message_new;

  public static String ParameterEditDialog_name;

  public static String ParameterEditDialog_name_error;

  public static String ParameterEditDialog_title;

  public static String ParameterEditDialog_type;

  public static String PromoteTempInputPage_constructors;

  public static String PromoteTempInputPage_Current_method;

  public static String PromoteTempInputPage_declare_final;

  public static String PromoteTempInputPage_declare_static;

  public static String PromoteTempInputPage_description;

  public static String PromoteTempInputPage_Field_declaration;

  public static String PromoteTempInputPage_Field_name;

  public static String PromoteTempInputPage_Initialize;

  public static String PullUpAction_unavailable;

  public static String PullUpInputPage_exception;

  public static String PullUpInputPage_hierarchyLabal_singular;

  public static String PullUpInputPage_hierarchyLabal_plural;

  public static String PullUpInputPage_pull_Up;

  public static String PullUpInputPage_pull_up1;

  public static String PullUpInputPage_see_log;

  public static String PullUpInputPage_select_methods;

  public static String PullUpInputPage_subtypes;

  public static String PullUpInputPage1_Action;

  public static String PullUpInputPage1_Add_Required;

  public static String PullUpInputPage1_Create_stubs;

  public static String PullUpInputPage1_declare_abstract;

  public static String PullUpInputPage1_Edit;

  public static String PullUpInputPage1_Edit_members;

  public static String PullUpInputPage1_label_use_destination;

  public static String PullUpInputPage1_label_use_in_instanceof;

  public static String PullUpInputPage1_Mark_selected_members_singular;

  public static String PullUpInputPage1_Mark_selected_members_plural;

  public static String PullUpInputPage1_Member;

  public static String PullUpInputPage1_page_message;

  public static String PullUpInputPage1_pull_up;

  public static String PullUpInputPage1_Select_destination;

  public static String PullUpInputPage1_Select_members_to_pull_up;

  public static String PullUpInputPage1_Specify_actions;

  public static String PullUpInputPage1_status_line_singular;

  public static String PullUpInputPage1_status_line_plural;

  public static String PullUpInputPage2_Select;

  public static String PullUpInputPage2_Source;

  public static String PullUpWizard_defaultPageTitle;

  public static String PullUpWizard_deselect_all_label;

  public static String PullUpWizard_select_all_label;

  public static String PushDownAction_Push_Down;

  public static String PushDownAction_To_activate;

  public static String PushDownInputPage_Action;

  public static String PushDownInputPage_Add_Required;

  public static String PushDownInputPage_Edit;

  public static String PushDownInputPage_Edit_members;

  public static String PushDownInputPage_Internal_Error;

  public static String PushDownInputPage_leave_abstract;

  public static String PushDownInputPage_Mark_selected_members_singular;

  public static String PushDownInputPage_Mark_selected_members_plural;

  public static String PushDownInputPage_Member;

  public static String PushDownInputPage_push_down;

  public static String PushDownInputPage_Push_Down;

  public static String PushDownInputPage_Select_members_to_push_down;

  public static String PushDownInputPage_Specify_actions;

  public static String PushDownInputPage_status_line_singular;

  public static String PushDownInputPage_status_line_plural;

  public static String PushDownWizard_defaultPageTitle;

  public static String QualifiedNameComponent_patterns_description;

  public static String QualifiedNameComponent_patterns_label;

  public static String RefactoringErrorDialogUtil_okToPerformQuestion;

  public static String RefactoringExecutionStarter_IntroduceParameterObject_problem_description;

  public static String RefactoringExecutionStarter_IntroduceParameterObject_problem_title;

  public static String RefactoringGroup_modify_Parameters_label;

  public static String RefactoringGroup_move_label;

  public static String RefactoringGroup_pull_Up_label;

  public static String RefactoringStarter_always_save;

  public static String RefactoringStarter_must_save;

  public static String RefactoringStarter_save_all_resources;

  public static String ReferencesInBinaryStatusContextViewer_show_as_search_button;

  public static String ReferencesInBinaryStatusContextViewer_title;

  public static String RenameCuWizard_defaultPageTitle;

  public static String RenameCuWizard_inputPage_description;

  public static String RenameEnumConstWizard_defaultPageTitle;

  public static String RenameEnumConstWizard_inputPage_description;

  public static String RenameFieldInputWizardPage_getter_label;

  public static String RenameFieldInputWizardPage_rename_getter;

  public static String RenameFieldInputWizardPage_rename_getter_to;

  public static String RenameFieldInputWizardPage_rename_setter;

  public static String RenameFieldInputWizardPage_rename_setter_to;

  public static String RenameFieldInputWizardPage_setter_label;

  public static String RenameInputWizardPage_update_qualified_names;

  public static String RenameInputWizardPage_update_references;

  public static String RenameInputWizardPage_update_textual_matches;

  public static String RenameJavaElementAction_started_rename_in_file;

  public static String RenameJavaProject_defaultPageTitle;

  public static String RenameJavaProject_inputPage_description;

  public static String RenamePackageWizard_defaultPageTitle;

  public static String RenamePackageWizard_inputPage_description;

  public static String RenamePackageWizard_rename_subpackages;

  public static String RenameSourceFolder_defaultPageTitle;

  public static String RenameSourceFolder_inputPage_description;

  public static String RenameTypeWizard_unexpected_exception;
  public static String RenameTypeWizardInputPage_update_similar_elements;
  public static String RenameTypeWizardInputPage_update_similar_elements_configure;
  public static String RenameTypeWizardSimilarElementsOptionsDialog_select_strategy;
  public static String RenameTypeWizardSimilarElementsOptionsDialog_strategy_1;
  public static String RenameTypeWizardSimilarElementsOptionsDialog_strategy_2;
  public static String RenameTypeWizardSimilarElementsOptionsDialog_strategy_3;
  public static String RenameTypeWizardSimilarElementsOptionsDialog_title;
  public static String RenameTypeWizardSimilarElementsOptionsDialog_warning_short_names;
  public static String RenameTypeWizardSimilarElementsPage_change_element_name;
  public static String RenameTypeWizardSimilarElementsPage_change_name;
  public static String RenameTypeWizardSimilarElementsPage_enter_new_name;
  public static String RenameTypeWizardSimilarElementsPage_field_exists;
  public static String RenameTypeWizardSimilarElementsPage_method_exists;
  public static String RenameTypeWizardSimilarElementsPage_name_empty;
  public static String RenameTypeWizardSimilarElementsPage_name_should_start_lowercase;
  public static String RenameTypeWizardSimilarElementsPage_rename_to;
  public static String RenameTypeWizardSimilarElementsPage_restore_defaults;
  public static String RenameTypeWizardSimilarElementsPage_review_similar_elements;
  public static String RenameTypeWizardSimilarElementsPage_select_element_to_view_source;

  public static String ReplaceInvocationsAction_dialog_title;

  public static String ReplaceInvocationsAction_label;

  public static String ReplaceInvocationsAction_unavailable;

  public static String ReplaceInvocationsInputPage_replaceAll;

  public static String ReplaceInvocationsInputPage_replaceInvocationsBy;

  public static String ReplaceInvocationsWizard_title;

  public static String SelfEncapsulateField_sef;

  public static String SelfEncapsulateFieldInputPage_access_Modifiers;

  public static String SelfEncapsulateFieldInputPage_configure_link;

  public static String SelfEncapsulateFieldInputPage_keep_references;

  public static String SelfEncapsulateFieldInputPage_default;

  public static String SelfEncapsulateFieldInputPage_description;

  public static String SelfEncapsulateFieldInputPage_field_access;

  public static String SelfEncapsulateFieldInputPage_first_method;

  public static String SelfEncapsulateFieldInputPage_generateJavadocComment;

  public static String SelfEncapsulateFieldInputPage_getter_name;

  public static String SelfEncapsulateFieldInputPage_insert_after;

  public static String SelfEncapsulateFieldInputPage_private;

  public static String SelfEncapsulateFieldInputPage_protected;

  public static String SelfEncapsulateFieldInputPage_public;

  public static String SelfEncapsulateFieldInputPage_setter_name;

  public static String SelfEncapsulateFieldInputPage_use_setter_getter;

  public static String SelfEncapsulateFieldInputPage_usenewgetter_label;

  public static String SelfEncapsulateFieldInputPage_usenewsetter_label;

  public static String SurroundWithTryCatchAction_dialog_title;

  public static String SurroundWithTryMultiCatchAction_dialog_title;

  public static String SurroundWithTryCatchAction_exception;

  public static String SurroundWithTryCatchAction_label;

  public static String SurroundWithTryMultiCatchAction_label;

  public static String SurroundWithTryMultiCatchAction_not17;

  public static String UseSupertypeAction_to_activate;

  public static String UseSupertypeAction_use_Supertype;

  public static String UseSupertypeInputPage_no_possible_updates;

  public static String UseSupertypeInputPage_No_updates;

  public static String UseSupertypeInputPage_Select_supertype;

  public static String UseSupertypeInputPage_Select_supertype_to_use;

  public static String UseSupertypeInputPage_updates_possible_in_file;

  public static String UseSupertypeInputPage_updates_possible_in_files;

  public static String UseSupertypeInputPage_Use_in_instanceof;

  public static String UseSupertypeWizard_10;

  public static String UseSupertypeWizard_Use_Super_Type_Where_Possible;

  public static String VisibilityControlUtil_Access_modifier;

  public static String VisibilityControlUtil_defa_ult_4;

  public static String SelfEncapsulateFieldInputPage_useexistingsetter_label;

  public static String SelfEncapsulateFieldInputPage_useexistinggetter_label;

  static {
    reloadMessages();
  }

  public static void reloadMessages() {
    NLS.initializeMessages(BUNDLE_NAME, RefactoringMessages.class);
  }

  private RefactoringMessages() {
    // Do not instantiate
  }
}
