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
package com.google.dart.tools.ui.internal.cleanup;

import org.eclipse.osgi.util.NLS;

public class MultiFixMessages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.internal.cleanup.MultiFixMessages"; //$NON-NLS-1$

  public static String CleanUpRefactoringWizard_CleaningUp11_Title;

  public static String CleanUpRefactoringWizard_CleaningUpN1_Title;
  public static String CleanUpRefactoringWizard_CleaningUpNN_Title;
  public static String CleanUpRefactoringWizard_CleanUpConfigurationPage_title;
  public static String CleanUpRefactoringWizard_Configure_Button;
  public static String CleanUpRefactoringWizard_ConfigureCustomProfile_button;
  public static String CleanUpRefactoringWizard_CustomCleanUpsDialog_title;
  public static String CleanUpRefactoringWizard_EmptySelection_message;
  public static String CleanUpRefactoringWizard_HideWizard_Link;
  public static String CleanUpRefactoringWizard_Profile_TableHeader;
  public static String CleanUpRefactoringWizard_Project_TableHeader;
  public static String CleanUpRefactoringWizard_unknownProfile_Name;
  public static String CleanUpRefactoringWizard_UnmanagedProfileWithName_Name;
  public static String CleanUpRefactoringWizard_use_configured_radio;
  public static String CleanUpRefactoringWizard_use_custom_radio;
  public static String CleanUpRefactoringWizard_XofYCleanUpsSelected_message;
  public static String CodeFormatCleanUp_correctIndentation_description;
  public static String CodeFormatCleanUp_RemoveTrailingAll_description;
  public static String CodeFormatCleanUp_RemoveTrailingNoEmpty_description;
  public static String CodeFormatFix_correctIndentation_changeGroupLabel;
  public static String CodeFormatFix_RemoveTrailingWhitespace_changeDescription;
  public static String ImportsCleanUp_OrganizeImports_Description;
  public static String SortMembersCleanUp_AllMembers_description;
  public static String SortMembersCleanUp_Excluding_description;
  public static String SortMembersCleanUp_RemoveMarkersWarning0;
  public static String StringMultiFix_AddMissingNonNls_description;
  public static String StringMultiFix_RemoveUnnecessaryNonNls_description;
  public static String UnusedCodeMultiFix_RemoveUnusedVariable_description;

  public static String UnusedCodeMultiFix_RemoveUnusedField_description;
  public static String UnusedCodeMultiFix_RemoveUnusedType_description;
  public static String UnusedCodeMultiFix_RemoveUnusedConstructor_description;
  public static String UnusedCodeMultiFix_RemoveUnusedMethod_description;
  public static String UnusedCodeMultiFix_RemoveUnusedImport_description;
  public static String UnusedCodeCleanUp_RemoveUnusedCasts_description;
  public static String CodeStyleMultiFix_ChangeNonStaticAccess_description;

  public static String CodeStyleMultiFix_AddThisQualifier_description;
  public static String CodeStyleMultiFix_QualifyAccessToStaticField;
  public static String CodeStyleMultiFix_ChangeIndirectAccessToStaticToDirect;
  public static String CodeStyleMultiFix_ConvertSingleStatementInControlBodeyToBlock_description;
  public static String CodeStyleCleanUp_addDefaultSerialVersionId_description;
  public static String CodeStyleCleanUp_QualifyNonStaticMethod_description;
  public static String CodeStyleCleanUp_QualifyStaticMethod_description;
  public static String CodeStyleCleanUp_removeFieldThis_description;
  public static String CodeStyleCleanUp_removeMethodThis_description;
  public static String Java50MultiFix_AddMissingDeprecated_description;

  public static String Java50MultiFix_AddMissingOverride_description;
  public static String Java50MultiFix_AddMissingOverride_description2;
  public static String Java50CleanUp_ConvertToEnhancedForLoop_description;
  public static String Java50CleanUp_AddTypeParameters_description;
  public static String SerialVersionCleanUp_Generated_description;

  public static String CleanUpRefactoringWizard_WindowTitle;

  public static String CleanUpRefactoringWizard_PageTitle;
  public static String CleanUpRefactoringWizard_formatterException_errorMessage;
  public static String ControlStatementsCleanUp_RemoveUnnecessaryBlocks_description;

  public static String ControlStatementsCleanUp_RemoveUnnecessaryBlocksWithReturnOrThrow_description;
  public static String UnimplementedCodeCleanUp_AddUnimplementedMethods_description;

  public static String UnimplementedCodeCleanUp_MakeAbstract_description;
  public static String ExpressionsCleanUp_addParanoiac_description;

  public static String ExpressionsCleanUp_removeUnnecessary_description;
  public static String VariableDeclarationCleanUp_AddFinalField_description;

  public static String VariableDeclarationCleanUp_AddFinalParameters_description;
  public static String VariableDeclarationCleanUp_AddFinalLocals_description;
  public static String CodeFormatCleanUp_description;

  public static String CodeFormatFix_description;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, MultiFixMessages.class);
  }

  private MultiFixMessages() {
  }

}
