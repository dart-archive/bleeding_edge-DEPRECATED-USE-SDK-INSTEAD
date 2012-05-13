/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.actions;

/**
 * Action ids for standard actions, for groups in the menu bar, and for actions in context menus of
 * JDT views.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class JdtActionConstants {

  // Navigate menu

  /**
   * Navigate menu: name of standard Goto Type global action (value
   * <code>"com.google.dart.tools.ui.actions.GoToType"</code>).
   */
  public static final String GOTO_TYPE = "com.google.dart.tools.ui.actions.GoToType"; //$NON-NLS-1$

  /**
   * Navigate menu: name of standard Goto Package global action (value
   * <code>"com.google.dart.tools.ui.actions.GoToPackage"</code>).
   */
  public static final String GOTO_PACKAGE = "com.google.dart.tools.ui.actions.GoToPackage"; //$NON-NLS-1$

  /**
   * Navigate menu: name of standard Open global action (value
   * <code>"com.google.dart.tools.ui.actions.Open"</code>).
   */
  public static final String OPEN = "com.google.dart.tools.ui.actions.Open"; //$NON-NLS-1$

  /**
   * Navigate menu: name of standard Open Super Implementation global action (value
   * <code>"com.google.dart.tools.ui.actions.OpenSuperImplementation"</code>).
   */
  public static final String OPEN_SUPER_IMPLEMENTATION = "com.google.dart.tools.ui.actions.OpenSuperImplementation"; //$NON-NLS-1$

  /**
   * Navigate menu: name of standard Open Type Hierarchy global action (value
   * <code>"com.google.dart.tools.ui.actions.OpenTypeHierarchy"</code>).
   */
  public static final String OPEN_TYPE_HIERARCHY = "com.google.dart.tools.ui.actions.OpenTypeHierarchy"; //$NON-NLS-1$

  /**
   * Navigate menu: name of standard Open Call Hierarchy global action (value
   * <code>"com.google.dart.tools.ui.actions.OpenCallHierarchy"</code>).
   */
  public static final String OPEN_CALL_HIERARCHY = "com.google.dart.tools.ui.actions.OpenCallHierarchy"; //$NON-NLS-1$

  /**
   * Navigate menu: name of standard Open External Javadoc global action (value
   * <code>"com.google.dart.tools.ui.actions.OpenExternalJavaDoc"</code>).
   */
  public static final String OPEN_EXTERNAL_JAVA_DOC = "com.google.dart.tools.ui.actions.OpenExternalJavaDoc"; //$NON-NLS-1$

  /**
   * Navigate menu: name of standard Show in Packages View global action (value
   * <code>"com.google.dart.tools.ui.actions.ShowInPackagesView"</code>).
   */
  public static final String SHOW_IN_PACKAGE_VIEW = "com.google.dart.tools.ui.actions.ShowInPackagesView"; //$NON-NLS-1$

  /**
   * Navigate menu: name of standard Show in Navigator View global action (value
   * <code>"com.google.dart.tools.ui.actions.ShowInNaviagtorView"</code>).
   */
  public static final String SHOW_IN_NAVIGATOR_VIEW = "com.google.dart.tools.ui.actions.ShowInNaviagtorView"; //$NON-NLS-1$

  // Edit menu

  /**
   * Edit menu: name of standard Code Assist global action (value
   * <code>"com.google.dart.tools.ui.actions.ContentAssist"</code>).
   */
  public static final String CONTENT_ASSIST = "com.google.dart.tools.ui.actions.ContentAssist"; //$NON-NLS-1$

  // Source menu

  /**
   * Source menu: name of standard Comment global action (value
   * <code>"com.google.dart.tools.ui.actions.Comment"</code>).
   */
  public static final String COMMENT = "com.google.dart.tools.ui.actions.Comment"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Uncomment global action (value
   * <code>"com.google.dart.tools.ui.actions.Uncomment"</code>).
   */
  public static final String UNCOMMENT = "com.google.dart.tools.ui.actions.Uncomment"; //$NON-NLS-1$

  /**
   * Source menu: name of standard ToggleComment global action (value
   * <code>"com.google.dart.tools.ui.actions.ToggleComment"</code>).
   */
  public static final String TOGGLE_COMMENT = "com.google.dart.tools.ui.actions.ToggleComment"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Block Comment global action (value
   * <code>"com.google.dart.tools.ui.actions.AddBlockComment"</code>).
   */
  public static final String ADD_BLOCK_COMMENT = "com.google.dart.tools.ui.actions.AddBlockComment"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Block Uncomment global action (value
   * <code>"com.google.dart.tools.ui.actions.RemoveBlockComment"</code>).
   */
  public static final String REMOVE_BLOCK_COMMENT = "com.google.dart.tools.ui.actions.RemoveBlockComment"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Indent global action (value
   * <code>"com.google.dart.tools.ui.actions.Indent"</code>).
   */
  public static final String INDENT = "com.google.dart.tools.ui.actions.Indent"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Shift Right action (value
   * <code>"com.google.dart.tools.ui.actions.ShiftRight"</code>).
   */
  public static final String SHIFT_RIGHT = "com.google.dart.tools.ui.actions.ShiftRight"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Shift Left global action (value
   * <code>"com.google.dart.tools.ui.actions.ShiftLeft"</code>).
   */
  public static final String SHIFT_LEFT = "com.google.dart.tools.ui.actions.ShiftLeft"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Format global action (value
   * <code>"com.google.dart.tools.ui.actions.Format"</code>).
   */
  public static final String FORMAT = "com.google.dart.tools.ui.actions.Format"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Format Element global action (value
   * <code>"com.google.dart.tools.ui.actions.FormatElement"</code>).
   */
  public static final String FORMAT_ELEMENT = "com.google.dart.tools.ui.actions.FormatElement"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Add Import global action (value
   * <code>"com.google.dart.tools.ui.actions.AddImport"</code>).
   */
  public static final String ADD_IMPORT = "com.google.dart.tools.ui.actions.AddImport"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Organize Imports global action (value
   * <code>"com.google.dart.tools.ui.actions.OrganizeImports"</code>).
   */
  public static final String ORGANIZE_IMPORTS = "com.google.dart.tools.ui.actions.OrganizeImports"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Sort Members global action (value
   * <code>"com.google.dart.tools.ui.actions.SortMembers"</code>).
   */
  public static final String SORT_MEMBERS = "com.google.dart.tools.ui.actions.SortMembers"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Surround with try/catch block global action (value
   * <code>"com.google.dart.tools.ui.actions.SurroundWithTryCatch"</code> ).
   */
  public static final String SURROUND_WITH_TRY_CATCH = "com.google.dart.tools.ui.actions.SurroundWithTryCatch"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Override Methods global action (value
   * <code>"com.google.dart.tools.ui.actions.OverrideMethods"</code>).
   */
  public static final String OVERRIDE_METHODS = "com.google.dart.tools.ui.actions.OverrideMethods"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Generate Getter and Setter global action (value
   * <code>"com.google.dart.tools.ui.actions.GenerateGetterSetter"</code> ).
   */
  public static final String GENERATE_GETTER_SETTER = "com.google.dart.tools.ui.actions.GenerateGetterSetter"; //$NON-NLS-1$

  /**
   * Source menu: name of standard delegate methods global action (value
   * <code>"com.google.dart.tools.ui.actions.GenerateDelegateMethods"</code>).
   */
  public static final String GENERATE_DELEGATE_METHODS = "com.google.dart.tools.ui.actions.GenerateDelegateMethods"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Add Constructor From Superclass global action (value
   * <code>"com.google.dart.tools.ui.actions.AddConstructorFromSuperclass"</code> ).
   */
  public static final String ADD_CONSTRUCTOR_FROM_SUPERCLASS = "com.google.dart.tools.ui.actions.AddConstructorFromSuperclass"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Generate Constructor using Fields global action (value
   * <code>"com.google.dart.tools.ui.actions.GenerateConstructorUsingFields"</code> ).
   */
  public static final String GENERATE_CONSTRUCTOR_USING_FIELDS = "com.google.dart.tools.ui.actions.GenerateConstructorUsingFields"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Generate hashCode() and equals() global action (value
   * <code>"com.google.dart.tools.ui.actions.GenerateHashCodeEquals"</code>).
   */
  public static final String GENERATE_HASHCODE_EQUALS = "com.google.dart.tools.ui.actions.GenerateHashCodeEquals"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Add Javadoc Comment global action (value
   * <code>"com.google.dart.tools.ui.actions.AddJavaDocComment"</code>).
   */
  public static final String ADD_JAVA_DOC_COMMENT = "com.google.dart.tools.ui.actions.AddJavaDocComment"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Externalize Strings global action (value
   * <code>"com.google.dart.tools.ui.actions.ExternalizeStrings"</code>).
   */
  public static final String EXTERNALIZE_STRINGS = "com.google.dart.tools.ui.actions.ExternalizeStrings"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Convert Line Delimiters To Windows global action (value
   * <code>"com.google.dart.tools.ui.actions.ConvertLineDelimitersToWindows"</code> ).
   */
  public static final String CONVERT_LINE_DELIMITERS_TO_WINDOWS = "com.google.dart.tools.ui.actions.ConvertLineDelimitersToWindows"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Convert Line Delimiters To UNIX global action (value
   * <code>"com.google.dart.tools.ui.actions.ConvertLineDelimitersToUNIX"</code> ).
   */
  public static final String CONVERT_LINE_DELIMITERS_TO_UNIX = "com.google.dart.tools.ui.actions.ConvertLineDelimitersToUNIX"; //$NON-NLS-1$

  /**
   * Source menu: name of standardConvert Line Delimiters To Mac global action (value
   * <code>"com.google.dart.tools.ui.actions.ConvertLineDelimitersToMac"</code> ).
   */
  public static final String CONVERT_LINE_DELIMITERS_TO_MAC = "com.google.dart.tools.ui.actions.ConvertLineDelimitersToMac"; //$NON-NLS-1$

  /**
   * Source menu: name of standard Clean up global action (value
   * <code>"com.google.dart.tools.ui.actions.CleanUp"</code>).
   */
  public static final String CLEAN_UP = "com.google.dart.tools.ui.actions.CleanUp"; //$NON-NLS-1$

  // Refactor menu

  /**
   * Refactor menu: name of standard Self Encapsulate Field global action (value
   * <code>"com.google.dart.tools.ui.actions.SelfEncapsulateField"</code>).
   */
  public static final String SELF_ENCAPSULATE_FIELD = "com.google.dart.tools.ui.actions.SelfEncapsulateField"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Modify Parameters global action (value
   * <code>"com.google.dart.tools.ui.actions.ModifyParameters"</code>).
   */
  public static final String MODIFY_PARAMETERS = "com.google.dart.tools.ui.actions.ModifyParameters"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Pull Up global action (value
   * <code>"com.google.dart.tools.ui.actions.PullUp"</code>).
   */
  public static final String PULL_UP = "com.google.dart.tools.ui.actions.PullUp"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Push Down global action (value
   * <code>"com.google.dart.tools.ui.actions.PushDown"</code>).
   */
  public static final String PUSH_DOWN = "com.google.dart.tools.ui.actions.PushDown"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Move Element global action (value
   * <code>"com.google.dart.tools.ui.actions.Move"</code>).
   */
  public static final String MOVE = "com.google.dart.tools.ui.actions.Move"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Rename Element global action (value
   * <code>"com.google.dart.tools.ui.actions.Rename"</code>).
   */
  public static final String RENAME = "com.google.dart.tools.ui.actions.Rename"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Extract Local global action (value
   * <code>"com.google.dart.tools.ui.actions.ExtractLocal"</code>).
   */
  public static final String EXTRACT_LOCAL = "com.google.dart.tools.ui.actions.ExtractLocal"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Extract Constant global action (value
   * <code>"com.google.dart.tools.ui.actions.ExtractConstant"</code>).
   */
  public static final String EXTRACT_CONSTANT = "com.google.dart.tools.ui.actions.ExtractConstant"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Introduce Parameter global action (value
   * <code>"com.google.dart.tools.ui.actions.IntroduceParameter"</code>).
   */
  public static final String INTRODUCE_PARAMETER = "com.google.dart.tools.ui.actions.IntroduceParameter"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Introduce Factory global action (value
   * <code>"com.google.dart.tools.ui.actions.IntroduceFactory"</code>).
   */
  public static final String INTRODUCE_FACTORY = "com.google.dart.tools.ui.actions.IntroduceFactory"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Extract Method global action (value
   * <code>"com.google.dart.tools.ui.actions.ExtractMethod"</code>).
   */
  public static final String EXTRACT_METHOD = "com.google.dart.tools.ui.actions.ExtractMethod"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Replace Invocations global action (value
   * <code>"com.google.dart.tools.ui.actions.ReplaceInvocations"</code>).
   */
  public static final String REPLACE_INVOCATIONS = "com.google.dart.tools.ui.actions.ReplaceInvocations"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Introduce Indirection global action (value
   * <code>"com.google.dart.tools.ui.actions.IntroduceIndirection"</code>).
   */
  public static final String INTRODUCE_INDIRECTION = "com.google.dart.tools.ui.actions.IntroduceIndirection"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Inline global action (value
   * <code>"com.google.dart.tools.ui.actions.Inline"</code>).
   */
  public static final String INLINE = "com.google.dart.tools.ui.actions.Inline"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Extract Interface global action (value
   * <code>"com.google.dart.tools.ui.actions.ExtractInterface"</code>).
   */
  public static final String EXTRACT_INTERFACE = "com.google.dart.tools.ui.actions.ExtractInterface"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Generalize Declared Type global action (value
   * <code>"com.google.dart.tools.ui.actions.ChangeType"</code>).
   */
  public static final String CHANGE_TYPE = "com.google.dart.tools.ui.actions.ChangeType"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard global action to convert a nested type to a top level type
   * (value <code>"com.google.dart.tools.ui.actions.MoveInnerToTop"</code>).
   */
  public static final String CONVERT_NESTED_TO_TOP = "com.google.dart.tools.ui.actions.ConvertNestedToTop"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Use Supertype global action (value
   * <code>"com.google.dart.tools.ui.actions.UseSupertype"</code>).
   */
  public static final String USE_SUPERTYPE = "com.google.dart.tools.ui.actions.UseSupertype"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Infer Generic Type Arguments global action (value
   * <code>"com.google.dart.tools.ui.actions.InferTypeArguments"</code>).
   */
  public static final String INFER_TYPE_ARGUMENTS = "com.google.dart.tools.ui.actions.InferTypeArguments"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard global action to convert a local variable to a field (value
   * <code>"com.google.dart.tools.ui.actions.ConvertLocalToField"</code>).
   */
  public static final String CONVERT_LOCAL_TO_FIELD = "com.google.dart.tools.ui.actions.ConvertLocalToField"; //$NON-NLS-1$

  /**
   * Refactor menu: name of standard Covert Anonymous to Nested global action (value
   * <code>"com.google.dart.tools.ui.actions.ConvertAnonymousToNested"</code>).
   */
  public static final String CONVERT_ANONYMOUS_TO_NESTED = "com.google.dart.tools.ui.actions.ConvertAnonymousToNested"; //$NON-NLS-1$

  // Search Menu

  /**
   * Search menu: name of standard Find References in Workspace global action (value
   * <code>"com.google.dart.tools.ui.actions.ReferencesInWorkspace"</code> ).
   */
  public static final String FIND_REFERENCES_IN_WORKSPACE = "com.google.dart.tools.ui.actions.ReferencesInWorkspace"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find References in Project global action (value
   * <code>"com.google.dart.tools.ui.actions.ReferencesInProject"</code> ).
   */
  public static final String FIND_REFERENCES_IN_PROJECT = "com.google.dart.tools.ui.actions.ReferencesInProject"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find References in Hierarchy global action (value
   * <code>"com.google.dart.tools.ui.actions.ReferencesInHierarchy"</code> ).
   */
  public static final String FIND_REFERENCES_IN_HIERARCHY = "com.google.dart.tools.ui.actions.ReferencesInHierarchy"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find References in Working Set global action (value
   * <code>"com.google.dart.tools.ui.actions.ReferencesInWorkingSet"</code>).
   */
  public static final String FIND_REFERENCES_IN_WORKING_SET = "com.google.dart.tools.ui.actions.ReferencesInWorkingSet"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Declarations in Workspace global action (value
   * <code>"com.google.dart.tools.ui.actions.DeclarationsInWorkspace"</code>).
   */
  public static final String FIND_DECLARATIONS_IN_WORKSPACE = "com.google.dart.tools.ui.actions.DeclarationsInWorkspace"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Declarations in Project global action (value
   * <code>"com.google.dart.tools.ui.actions.DeclarationsInProject"</code> ).
   */
  public static final String FIND_DECLARATIONS_IN_PROJECT = "com.google.dart.tools.ui.actions.DeclarationsInProject"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Declarations in Hierarchy global action (value
   * <code>"com.google.dart.tools.ui.actions.DeclarationsInHierarchy"</code>).
   */
  public static final String FIND_DECLARATIONS_IN_HIERARCHY = "com.google.dart.tools.ui.actions.DeclarationsInHierarchy"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Declarations in Working Set global action (value
   * <code>"com.google.dart.tools.ui.actions.DeclarationsInWorkingSet"</code>).
   */
  public static final String FIND_DECLARATIONS_IN_WORKING_SET = "com.google.dart.tools.ui.actions.DeclarationsInWorkingSet"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Implementors in Workspace global action (value
   * <code>"com.google.dart.tools.ui.actions.ImplementorsInWorkspace"</code>).
   */
  public static final String FIND_IMPLEMENTORS_IN_WORKSPACE = "com.google.dart.tools.ui.actions.ImplementorsInWorkspace"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Implementors in Project global action (value
   * <code>"com.google.dart.tools.ui.actions.ImplementorsInProject"</code> ).
   */
  public static final String FIND_IMPLEMENTORS_IN_PROJECT = "com.google.dart.tools.ui.actions.ImplementorsInProject"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Implementors in Working Set global action (value
   * <code>"com.google.dart.tools.ui.actions.ImplementorsInWorkingSet"</code>).
   */
  public static final String FIND_IMPLEMENTORS_IN_WORKING_SET = "com.google.dart.tools.ui.actions.ImplementorsInWorkingSet"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Read Access in Workspace global action (value
   * <code>"com.google.dart.tools.ui.actions.ReadAccessInWorkspace"</code> ).
   */
  public static final String FIND_READ_ACCESS_IN_WORKSPACE = "com.google.dart.tools.ui.actions.ReadAccessInWorkspace"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Read Access in Project global action (value
   * <code>"com.google.dart.tools.ui.actions.ReadAccessInProject"</code> ).
   */
  public static final String FIND_READ_ACCESS_IN_PROJECT = "com.google.dart.tools.ui.actions.ReadAccessInProject"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Read Access in Hierarchy global action (value
   * <code>"com.google.dart.tools.ui.actions.ReadAccessInHierarchy"</code> ).
   */
  public static final String FIND_READ_ACCESS_IN_HIERARCHY = "com.google.dart.tools.ui.actions.ReadAccessInHierarchy"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Read Access in Working Set global action (value
   * <code>"com.google.dart.tools.ui.actions.ReadAccessInWorkingSet"</code>).
   */
  public static final String FIND_READ_ACCESS_IN_WORKING_SET = "com.google.dart.tools.ui.actions.ReadAccessInWorkingSet"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Write Access in Workspace global action (value
   * <code>"com.google.dart.tools.ui.actions.WriteAccessInWorkspace"</code>).
   */
  public static final String FIND_WRITE_ACCESS_IN_WORKSPACE = "com.google.dart.tools.ui.actions.WriteAccessInWorkspace"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Write Access in Project global action (value
   * <code>"com.google.dart.tools.ui.actions.WriteAccessInProject"</code> ).
   */
  public static final String FIND_WRITE_ACCESS_IN_PROJECT = "com.google.dart.tools.ui.actions.WriteAccessInProject"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Read Access in Hierarchy global action (value
   * <code>"com.google.dart.tools.ui.actions.WriteAccessInHierarchy"</code>).
   */
  public static final String FIND_WRITE_ACCESS_IN_HIERARCHY = "com.google.dart.tools.ui.actions.WriteAccessInHierarchy"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find Read Access in Working Set global action (value
   * <code>"com.google.dart.tools.ui.actions.WriteAccessInWorkingSet"</code>).
   */
  public static final String FIND_WRITE_ACCESS_IN_WORKING_SET = "com.google.dart.tools.ui.actions.WriteAccessInWorkingSet"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Occurrences in File global action (value
   * <code>"com.google.dart.tools.ui.actions.OccurrencesInFile"</code>).
   */
  public static final String FIND_OCCURRENCES_IN_FILE = "com.google.dart.tools.ui.actions.OccurrencesInFile"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find exception occurrences global action (value
   * <code>"com.google.dart.tools.ui.actions.ExceptionOccurrences"</code> ).
   */
  public static final String FIND_EXCEPTION_OCCURRENCES = "com.google.dart.tools.ui.actions.ExceptionOccurrences"; //$NON-NLS-1$

  /**
   * Search menu: name of standard Find implement occurrences global action (value
   * <code>"com.google.dart.tools.ui.actions.ImplementOccurrences"</code> ).
   */
  public static final String FIND_IMPLEMENT_OCCURRENCES = "com.google.dart.tools.ui.actions.ImplementOccurrences"; //$NON-NLS-1$		

}
