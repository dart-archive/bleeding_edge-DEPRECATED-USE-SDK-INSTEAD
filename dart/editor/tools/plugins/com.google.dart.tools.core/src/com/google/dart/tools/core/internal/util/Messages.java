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
package com.google.dart.tools.core.internal.util;

import org.eclipse.osgi.util.NLS;

import java.text.MessageFormat;

/**
 * Instances of the class <code>Messages</code>
 */
public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.core.internal.util.messages";//$NON-NLS-1$

  public static String code_assist_internal_error;
  public static String convention_fieldName_dollar;
  public static String convention_fieldName_empty;
  public static String convention_fieldName_notLowercase;
  public static String convention_fieldName_initialChar;
  public static String convention_fieldName_internalChar;
  public static String convention_fieldName_leadingOrTrailingBlanks;
  public static String convention_fieldName_null;
  public static String convention_fieldName_underscore;

  public static String convention_methodName_dollar;
  public static String convention_methodName_empty;
  public static String convention_methodName_notLowercase;
  public static String convention_methodName_initialChar;
  public static String convention_methodName_internalChar;
  public static String convention_methodName_leadingOrTrailingBlanks;
  public static String convention_methodName_null;
  public static String convention_methodName_underscore;

  public static String convention_functionName_dollar;
  public static String convention_functionName_empty;
  public static String convention_functionName_notLowercase;
  public static String convention_functionName_initialChar;
  public static String convention_functionName_internalChar;
  public static String convention_functionName_leadingOrTrailingBlanks;
  public static String convention_functionName_null;
  public static String convention_functionName_underscore;

  public static String convention_functionTypeAliasName_dollar;
  public static String convention_functionTypeAliasName_empty;
  public static String convention_functionTypeAliasName_notLowercase;
  public static String convention_functionTypeAliasName_initialChar;
  public static String convention_functionTypeAliasName_internalChar;
  public static String convention_functionTypeAliasName_leadingOrTrailingBlanks;
  public static String convention_functionTypeAliasName_null;
  public static String convention_functionTypeAliasName_underscore;

  public static String convention_parameterName_dollar;
  public static String convention_parameterName_empty;
  public static String convention_parameterName_notLowercase;
  public static String convention_parameterName_initialChar;
  public static String convention_parameterName_internalChar;
  public static String convention_parameterName_leadingOrTrailingBlanks;
  public static String convention_parameterName_null;
  public static String convention_parameterName_underscore;
  public static String convention_prefix_empty;
  public static String convention_prefix_initialChar;
  public static String convention_prefix_internalChar;
  public static String convention_prefix_leadingOrTrailingBlanks;
  public static String convention_prefix_null;

  public static String convention_typeName_dollar;
  public static String convention_typeName_empty;
  public static String convention_typeName_initialChar;
  public static String convention_typeName_internalChar;
  public static String convention_typeName_leadingOrTrailingBlanks;
  public static String convention_typeName_notUppercase;
  public static String convention_typeName_null;

  public static String convention_typeParameterName_dollar;
  public static String convention_typeParameterName_empty;
  public static String convention_typeParameterName_initialChar;
  public static String convention_typeParameterName_internalChar;
  public static String convention_typeParameterName_leadingOrTrailingBlanks;
  public static String convention_typeParameterName_notUppercase;
  public static String convention_typeParameterName_null;

  public static String convention_unitName_empty;
  public static String convention_unitName_initialChar;
  public static String convention_unitName_internalChar;
  public static String convention_unitName_leadingOrTrailingBlanks;
  public static String convention_unitName_notDartName;
  public static String convention_unitName_null;
  public static String convention_variableName_dollar;
  public static String convention_variableName_empty;
  public static String convention_variableName_notLowercase;
  public static String convention_variableName_initialChar;
  public static String convention_variableName_internalChar;
  public static String convention_variableName_leadingOrTrailingBlanks;
  public static String convention_variableName_null;
  public static String convention_variableName_underscore;

  public static String correction_nullRequestor;
  public static String correction_nullUnit;

  public static String buffer_closed;
  public static String element_nullName;
  public static String engine_completing;
  public static String engine_searching;
  public static String engine_searching_indexing;
  public static String engine_searching_matching;
  public static String file_notFound;
  public static String path_nullPath;
  public static String savedState_jobName;

  public static String hierarchy_creating;
  public static String hierarchy_creatingOnType;

  public static String operation_cancelled;
  public static String operation_copyElementProgress;
  public static String operation_copyResourceProgress;
  public static String operation_createFieldProgress;
  public static String operation_createMethodProgress;
  public static String operation_createTypeProgress;
  public static String operation_createUnitProgress;
  public static String operation_deleteElementProgress;
  public static String operation_deleteResourceProgress;
  public static String operation_moveElementProgress;
  public static String operation_moveResourceProgress;
  public static String operation_nullContainer;
  public static String operation_nullName;
  public static String operation_reconcilingWorkingCopy;
  public static String operation_renameElementProgress;
  public static String operation_renameResourceProgress;
  public static String operation_workingCopy_commit;

  public static String problem_atLine;
  public static String problem_noSourceInformation;

  public static String status_nameCollision;

  public static String change_library_has_no_file;

  static {
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  /**
   * Bind the given message's substitution locations with the given string values.
   * 
   * @param message the message to be manipulated
   * @return the manipulated String
   */
  public static String bind(String message) {
    return bind(message, null);
  }

  /**
   * Bind the given message's substitution locations with the given string values.
   * 
   * @param message the message to be manipulated
   * @param binding the object to be inserted into the message
   * @return the manipulated String
   */
  public static String bind(String message, Object binding) {
    return bind(message, new Object[] {binding});
  }

  /**
   * Bind the given message's substitution locations with the given string values.
   * 
   * @param message the message to be manipulated
   * @param binding1 An object to be inserted into the message
   * @param binding2 A second object to be inserted into the message
   * @return the manipulated String
   */
  public static String bind(String message, Object binding1, Object binding2) {
    return bind(message, new Object[] {binding1, binding2});
  }

  /**
   * Bind the given message's substitution locations with the given string values.
   * 
   * @param message the message to be manipulated
   * @param bindings An array of objects to be inserted into the message
   * @return the manipulated String
   */
  public static String bind(String message, Object[] bindings) {
    return MessageFormat.format(message, bindings);
  }

  private Messages() {
    // Do not instantiate
  }
}
