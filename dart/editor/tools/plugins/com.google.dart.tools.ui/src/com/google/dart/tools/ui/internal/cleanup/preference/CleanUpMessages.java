/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.cleanup.preference;

import org.eclipse.osgi.util.NLS;

public class CleanUpMessages extends NLS {

  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.internal.cleanup.preference.CleanUpMessages"; //$NON-NLS-1$

  public static String CleanUpConfigurationBlock_SelectedCleanUps_label;
  public static String CleanUpConfigurationBlock_ShowCleanUpWizard_checkBoxLabel;

  public static String CleanUpModifyDialog_SelectOne_Error;
  public static String CleanUpModifyDialog_XofYSelected_Label;

  public static String CleanUpProfileManager_ProfileName_EclipseBuildIn;

  public static String CodeFormatingTabPage_CheckboxName_FormatSourceCode;

  public static String CodeFormatingTabPage_correctIndentation_checkbox_text;
  public static String CodeFormatingTabPage_FormatterSettings_Description;
  public static String CodeFormatingTabPage_GroupName_Formatter;
  public static String CodeFormatingTabPage_Imports_GroupName;
  public static String CodeFormatingTabPage_OrganizeImports_CheckBoxLable;
  public static String CodeFormatingTabPage_OrganizeImportsSettings_Description;
  public static String CodeFormatingTabPage_SortMembers_GroupName;
  public static String CodeFormatingTabPage_SortMembers_CheckBoxLabel;
  public static String CodeFormatingTabPage_SortMembers_Description;

  public static String CodeFormatingTabPage_SortMembersExclusive_radio0;
  public static String CodeFormatingTabPage_SortMembersFields_CheckBoxLabel;

  public static String CodeFormatingTabPage_RemoveTrailingWhitespace_all_radio;

  public static String CodeFormatingTabPage_RemoveTrailingWhitespace_checkbox_text;

  public static String CodeFormatingTabPage_RemoveTrailingWhitespace_ignoreEmpty_radio;

  public static String CodeFormatingTabPage_SortMembersSemanticChange_warning;

  public static String CodeStyleTabPage_CheckboxName_ConvertForLoopToEnhanced;
  public static String CodeStyleTabPage_CheckboxName_UseBlocks;
  public static String CodeStyleTabPage_CheckboxName_UseFinal;
  public static String CodeStyleTabPage_CheckboxName_UseFinalForFields;
  public static String CodeStyleTabPage_CheckboxName_UseFinalForLocals;
  public static String CodeStyleTabPage_CheckboxName_UseFinalForParameters;
  public static String CodeStyleTabPage_CheckboxName_UseParentheses;
  public static String CodeStyleTabPage_GroupName_ControlStatments;
  public static String CodeStyleTabPage_GroupName_Expressions;
  public static String CodeStyleTabPage_GroupName_VariableDeclarations;
  public static String CodeStyleTabPage_RadioName_AlwaysUseBlocks;
  public static String CodeStyleTabPage_RadioName_AlwaysUseParantheses;
  public static String CodeStyleTabPage_RadioName_NeverUseBlocks;
  public static String CodeStyleTabPage_RadioName_NeverUseParantheses;
  public static String CodeStyleTabPage_RadioName_UseBlocksSpecial;

  public static String ContributedCleanUpTabPage_ErrorPage_message;

  public static String MemberAccessesTabPage_CheckboxName_ChangeAccessesThroughInstances;
  public static String MemberAccessesTabPage_CheckboxName_ChangeAccessesThroughSubtypes;
  public static String MemberAccessesTabPage_CheckboxName_FieldQualifier;
  public static String MemberAccessesTabPage_CheckboxName_MethodQualifier;
  public static String MemberAccessesTabPage_CheckboxName_QualifyFieldWithDeclaringClass;
  public static String MemberAccessesTabPage_CheckboxName_QualifyMethodWithDeclaringClass;
  public static String MemberAccessesTabPage_CheckboxName_QualifyWithDeclaringClass;
  public static String MemberAccessesTabPage_GroupName_NonStaticAccesses;
  public static String MemberAccessesTabPage_GroupName_StaticAccesses;
  public static String MemberAccessesTabPage_RadioName_AlwaysThisForFields;
  public static String MemberAccessesTabPage_RadioName_AlwaysThisForMethods;
  public static String MemberAccessesTabPage_RadioName_NeverThisForFields;
  public static String MemberAccessesTabPage_RadioName_NeverThisForMethods;

  public static String MissingCodeTabPage_CheckboxName_AddMissingAnnotations;
  public static String MissingCodeTabPage_CheckboxName_AddMissingDeprecatedAnnotations;
  public static String MissingCodeTabPage_CheckboxName_AddMissingOverrideAnnotations;

  public static String MissingCodeTabPage_CheckboxName_AddMissingOverrideInterfaceAnnotations;
  public static String MissingCodeTabPage_CheckboxName_AddMethods;
  public static String MissingCodeTabPage_CheckboxName_AddSUID;
  public static String MissingCodeTabPage_GroupName_Annotations;
  public static String MissingCodeTabPage_GroupName_UnimplementedCode;
  public static String MissingCodeTabPage_GroupName_PotentialProgrammingProblems;
  public static String MissingCodeTabPage_Label_CodeTemplatePreferencePage;
  public static String MissingCodeTabPage_RadioName_AddDefaultSUID;
  public static String MissingCodeTabPage_RadioName_AddGeneratedSUID;

  public static String UnnecessaryCodeTabPage_CheckboxName_UnnecessaryCasts;
  public static String UnnecessaryCodeTabPage_CheckboxName_UnnecessaryNLSTags;
  public static String UnnecessaryCodeTabPage_CheckboxName_UnusedConstructors;
  public static String UnnecessaryCodeTabPage_CheckboxName_UnusedFields;
  public static String UnnecessaryCodeTabPage_CheckboxName_UnusedImports;
  public static String UnnecessaryCodeTabPage_CheckboxName_UnusedLocalVariables;
  public static String UnnecessaryCodeTabPage_CheckboxName_UnusedMembers;
  public static String UnnecessaryCodeTabPage_CheckboxName_UnusedMethods;
  public static String UnnecessaryCodeTabPage_CheckboxName_UnusedTypes;
  public static String UnnecessaryCodeTabPage_GroupName_UnnecessaryCode;
  public static String UnnecessaryCodeTabPage_GroupName_UnusedCode;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, CleanUpMessages.class);
  }

  private CleanUpMessages() {
  }
}
