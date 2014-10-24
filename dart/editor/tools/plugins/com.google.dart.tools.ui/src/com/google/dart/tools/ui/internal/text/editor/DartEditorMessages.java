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
package com.google.dart.tools.ui.internal.text.editor;

import org.eclipse.osgi.util.NLS;

import java.util.ResourceBundle;

/**
 * Helper class to get NLSed messages.
 */
final class DartEditorMessages extends NLS {

  private static final String BUNDLE_FOR_CONSTRUCTED_KEYS = "com.google.dart.tools.ui.internal.text.editor.ConstructedJavaEditorMessages";//$NON-NLS-1$
  private static ResourceBundle fgBundleForConstructedKeys = ResourceBundle.getBundle(BUNDLE_FOR_CONSTRUCTED_KEYS);

  private static final String BUNDLE_NAME = DartEditorMessages.class.getName();

  public static String AddImportOnSelection_label;

  public static String AddImportOnSelection_tooltip;

  public static String AddImportOnSelection_description;
  public static String AddImportOnSelection_error_title;
  public static String AddImportOnSelection_dialog_title;
  public static String AddImportOnSelection_dialog_message;
  public static String ClassFileEditor_error_classfile_not_on_classpath;
  public static String ClassFileEditor_error_invalid_input_message;
  public static String ClassFileEditor_error_title;
  public static String ClassFileEditor_error_message;
  // public static String ClassFileEditor_SourceAttachmentForm_cannotconfigure;
  // public static String ClassFileEditor_SourceAttachmentForm_notsupported;
  // public static String ClassFileEditor_SourceAttachmentForm_readonly;
  public static String ClassFileMarkerAnnotationModel_error_isAcceptable;
  public static String ClassFileMarkerAnnotationModel_error_isAffected;
  public static String ClassFileMarkerAnnotationModel_error_resourceChanged;
  public static String CompilationUnitDocumentProvider_progressNotifyingSaveParticipants;
  public static String CompilationUnitDocumentProvider_error_saveParticipantProblem;
  public static String CompilationUnitDocumentProvider_error_saveParticipantSavedFile;
  public static String CompilationUnitDocumentProvider_error_saveParticipantFailed;
  public static String CompilationUnitEditor_error_saving_message1;
  public static String CompilationUnitEditor_error_saving_title1;
  public static String CompilationUnitEditor_error_saving_saveParticipant;
  public static String EditorUtility_cantFindEditor;
  public static String EditorUtility_could_not_find_editorId;
  public static String EditorUtility_file_must_not_be_null;
  public static String EditorUtility_no_active_WorkbenchPage;
  public static String EditorUtility_no_editorInput;
  public static String JavaOutlinePage_Sort_label;
  public static String JavaOutlinePage_Sort_tooltip;
  public static String JavaOutlinePage_Sort_description;
  public static String JavaOutlinePage_GoIntoTopLevelType_label;
  public static String JavaOutlinePage_GoIntoTopLevelType_tooltip;
  public static String JavaOutlinePage_GoIntoTopLevelType_description;
  public static String JavaOutlinePage_error_NoTopLevelType;
  public static String ToggleComment_error_title;
  public static String ToggleComment_error_message;
  public static String ContentAssistProposal_label;
  public static String Editor_FoldingMenu_name;
  public static String CompilationUnitDocumentProvider_saveAsTargetOpenInEditor;
  public static String ClassFileDocumentProvider_error_createElementInfo;
  public static String ExpandSelectionMenu_label;
  public static String GotoMatchingBracket_label;
  public static String GotoMatchingBracket_error_invalidSelection;
  public static String GotoMatchingBracket_error_noMatchingBracket;
  public static String GotoMatchingBracket_error_bracketOutsideSelectedElement;
  public static String SourceAttachmentForm_title;
  public static String SourceAttachmentForm_heading;
  public static String SourceAttachmentForm_message_noSource;
  // public static String SourceAttachmentForm_message_noSourceAttachment;
  // public static String SourceAttachmentForm_message_pressButtonToAttach;
  // public static String SourceAttachmentForm_message_noSourceInAttachment;
  // public static String SourceAttachmentForm_message_pressButtonToChange;
  // public static String SourceAttachmentForm_button_attachSource;
  // public static String SourceAttachmentForm_button_changeAttachedSource;
  // public static String SourceAttachmentForm_error_title;
  // public static String SourceAttachmentForm_error_message;
  public static String SourceAttachmentForm_attach_error_title;
  public static String SourceAttachmentForm_attach_error_message;
  public static String EditorUtility_concatModifierStrings;
  public static String OverrideIndicatorManager_implements;
  public static String OverrideIndicatorManager_intallJob;
  public static String OverrideIndicatorManager_overrides;
  public static String OverrideIndicatorManager_open_error_title;
  public static String OverrideIndicatorManager_open_error_message;
  public static String OverrideIndicatorManager_open_error_messageHasLogEntry;
  public static String SemanticHighlighting_annotation;
  public static String SemanticHighlighting_constructor;
  public static String SemanticHighlighting_field;
  public static String SemanticHighlighting_function;
  public static String SemanticHighlighting_getter;
  public static String SemanticHighlighting_importPrefix;
  public static String SemanticHighlighting_job;
  public static String SemanticHighlighting_methodDeclaration;
  public static String SemanticHighlighting_localVariableDeclaration;
  public static String SemanticHighlighting_localVariable;
  public static String SemanticHighlighting_dynamicType;
  public static String SemanticHighlighting_enum;
  public static String SemanticHighlighting_enumConstant;
  public static String SemanticHighlighting_parameterVariable;
  public static String SemanticHighlighting_deprecatedElement;
  public static String SemanticHighlighting_method;
  public static String SemanticHighlighting_number;
  public static String SemanticHighlighting_objectInitializer;
  public static String SemanticHighlighting_setter;
  public static String SemanticHighlighting_staticField;
  public static String SemanticHighlighting_staticMethod;
  public static String SemanticHighlighting_staticMethodDeclaration;
  public static String SemanticHighlighting_class;
  public static String SemanticHighlighting_functionTypeAlias;
  public static String SemanticHighlighting_typeVariable;
  public static String SemanticHighlighting_topLevelMember;
  public static String JavaEditor_markOccurrences_job_name;
  public static String Editor_OpenPropertiesFile_error_keyNotFound;
  public static String Editor_OpenPropertiesFile_error_fileNotFound_dialogMessage;
  public static String Editor_OpenPropertiesFile_error_openEditor_dialogMessage;
  public static String Editor_MoveLines_IllegalMove_status;
  static {
    NLS.initializeMessages(BUNDLE_NAME, DartEditorMessages.class);
  }
  public static String BasicEditorActionContributor_specific_content_assist_menu;
  public static String SemanticHighlighting_directive;

  /**
   * Returns the message bundle which contains constructed keys.
   * 
   * @return the message bundle
   */
  public static ResourceBundle getBundleForConstructedKeys() {
    return fgBundleForConstructedKeys;
  }

  private DartEditorMessages() {
    // Do not instantiate
  }
}
