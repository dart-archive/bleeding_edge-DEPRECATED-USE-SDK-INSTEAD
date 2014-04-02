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
package com.google.dart.tools.core.model;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.util.Messages;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * The class <code>DartConventions</code> provides methods for checking Dart-specific conventions
 * such as name syntax.
 * 
 * @coverage dart.tools.core.model
 */
public final class DartConventions {
  @SuppressWarnings("unused")
  private static class MessageHolder {
    public static MessageHolder forClassTypeAlias() {
      return new MessageHolder().initForClassTypeAlias();
    }

    public static MessageHolder forCompilationUnit() {
      return new MessageHolder().initForCompilationUnit();
    }

    public static MessageHolder forField() {
      return new MessageHolder().initForField();
    }

    public static MessageHolder forFunction() {
      return new MessageHolder().initForFunction();
    }

    public static MessageHolder forFunctionTypeAlias() {
      return new MessageHolder().initForFunctionTypeAlias();
    }

    public static MessageHolder forMethod() {
      return new MessageHolder().initForMethod();
    }

    public static MessageHolder forParameter() {
      return new MessageHolder().initForParameter();
    }

    public static MessageHolder forPrefix() {
      return new MessageHolder().initForPrefix();
    }

    public static MessageHolder forType() {
      return new MessageHolder().initForType();
    }

    public static MessageHolder forTypeParameter() {
      return new MessageHolder().initForTypeParameter();
    }

    public static MessageHolder forVariable() {
      return new MessageHolder().initForVariable();
    }

    private String dollar;
    private String empty;
    private String initialCase;
    private String initialChar;
    private String internalChar;
    private String leadingOrTrailingBlanks;
    private String nullName;
    private String underscore;

    public String initialChar(String name) {
      return Messages.bind(initialChar, name);
    }

    public String internalChar(String name) {
      return Messages.bind(internalChar, name);
    }

    private MessageHolder initForClassTypeAlias() {
      dollar = Messages.convention_classTypeAliasName_dollar;
      empty = Messages.convention_classTypeAliasName_empty;
      initialCase = Messages.convention_classTypeAliasName_notLowercase;
      initialChar = Messages.convention_classTypeAliasName_initialChar;
      internalChar = Messages.convention_classTypeAliasName_internalChar;
      leadingOrTrailingBlanks = Messages.convention_classTypeAliasName_leadingOrTrailingBlanks;
      nullName = Messages.convention_classTypeAliasName_null;
      underscore = Messages.convention_classTypeAliasName_underscore;
      return this;
    }

    private MessageHolder initForCompilationUnit() {
      // dollar = Messages.convention_unitName_dollar;
      empty = Messages.convention_unitName_empty;
      // initialCase = Messages.convention_unitName_notUppercase;
      initialChar = Messages.convention_unitName_initialChar;
      internalChar = Messages.convention_unitName_internalChar;
      leadingOrTrailingBlanks = Messages.convention_unitName_leadingOrTrailingBlanks;
      nullName = Messages.convention_unitName_null;
      // underscore = Messages.convention_unitName_underscore;
      return this;
    }

    private MessageHolder initForField() {
      dollar = Messages.convention_fieldName_dollar;
      empty = Messages.convention_fieldName_empty;
      initialCase = Messages.convention_fieldName_notLowercase;
      initialChar = Messages.convention_fieldName_initialChar;
      internalChar = Messages.convention_fieldName_internalChar;
      leadingOrTrailingBlanks = Messages.convention_fieldName_leadingOrTrailingBlanks;
      nullName = Messages.convention_fieldName_null;
      underscore = Messages.convention_fieldName_underscore;
      return this;
    }

    private MessageHolder initForFunction() {
      dollar = Messages.convention_functionName_dollar;
      empty = Messages.convention_functionName_empty;
      initialCase = Messages.convention_functionName_notLowercase;
      initialChar = Messages.convention_functionName_initialChar;
      internalChar = Messages.convention_functionName_internalChar;
      leadingOrTrailingBlanks = Messages.convention_functionName_leadingOrTrailingBlanks;
      nullName = Messages.convention_functionName_null;
      underscore = Messages.convention_functionName_underscore;
      return this;
    }

    private MessageHolder initForFunctionTypeAlias() {
      dollar = Messages.convention_functionTypeAliasName_dollar;
      empty = Messages.convention_functionTypeAliasName_empty;
      initialCase = Messages.convention_functionTypeAliasName_notLowercase;
      initialChar = Messages.convention_functionTypeAliasName_initialChar;
      internalChar = Messages.convention_functionTypeAliasName_internalChar;
      leadingOrTrailingBlanks = Messages.convention_functionTypeAliasName_leadingOrTrailingBlanks;
      nullName = Messages.convention_functionTypeAliasName_null;
      underscore = Messages.convention_functionTypeAliasName_underscore;
      return this;
    }

    private MessageHolder initForMethod() {
      dollar = Messages.convention_methodName_dollar;
      empty = Messages.convention_methodName_empty;
      initialCase = Messages.convention_methodName_notLowercase;
      initialChar = Messages.convention_methodName_initialChar;
      internalChar = Messages.convention_methodName_internalChar;
      leadingOrTrailingBlanks = Messages.convention_methodName_leadingOrTrailingBlanks;
      nullName = Messages.convention_methodName_null;
      underscore = Messages.convention_methodName_underscore;
      return this;
    }

    private MessageHolder initForParameter() {
      dollar = Messages.convention_parameterName_dollar;
      empty = Messages.convention_parameterName_empty;
      initialCase = Messages.convention_parameterName_notLowercase;
      initialChar = Messages.convention_parameterName_initialChar;
      internalChar = Messages.convention_parameterName_internalChar;
      leadingOrTrailingBlanks = Messages.convention_parameterName_leadingOrTrailingBlanks;
      nullName = Messages.convention_parameterName_null;
      underscore = Messages.convention_parameterName_underscore;
      return this;
    }

    private MessageHolder initForPrefix() {
      // dollar = Messages.convention_prefix_dollar;
      empty = Messages.convention_prefix_empty;
      // initialCase = Messages.convention_prefix_notUppercase;
      initialChar = Messages.convention_prefix_initialChar;
      internalChar = Messages.convention_prefix_internalChar;
      leadingOrTrailingBlanks = Messages.convention_prefix_leadingOrTrailingBlanks;
      nullName = Messages.convention_prefix_null;
      // underscore = Messages.convention_prefix_underscore;
      return this;
    }

    private MessageHolder initForType() {
      dollar = Messages.convention_typeName_dollar;
      empty = Messages.convention_typeName_empty;
      initialCase = Messages.convention_typeName_notUppercase;
      initialChar = Messages.convention_typeName_initialChar;
      internalChar = Messages.convention_typeName_internalChar;
      leadingOrTrailingBlanks = Messages.convention_typeName_leadingOrTrailingBlanks;
      nullName = Messages.convention_typeName_null;
      return this;
    }

    private MessageHolder initForTypeParameter() {
      dollar = Messages.convention_typeParameterName_dollar;
      empty = Messages.convention_typeParameterName_empty;
      initialCase = Messages.convention_typeParameterName_notUppercase;
      initialChar = Messages.convention_typeParameterName_initialChar;
      internalChar = Messages.convention_typeParameterName_internalChar;
      leadingOrTrailingBlanks = Messages.convention_typeParameterName_leadingOrTrailingBlanks;
      nullName = Messages.convention_typeParameterName_null;
      return this;
    }

    private MessageHolder initForVariable() {
      dollar = Messages.convention_variableName_dollar;
      empty = Messages.convention_variableName_empty;
      initialCase = Messages.convention_variableName_notLowercase;
      initialChar = Messages.convention_variableName_initialChar;
      internalChar = Messages.convention_variableName_internalChar;
      leadingOrTrailingBlanks = Messages.convention_variableName_leadingOrTrailingBlanks;
      nullName = Messages.convention_variableName_null;
      underscore = Messages.convention_variableName_underscore;
      return this;
    }
  }

  /**
   * Validate the given field name. Return a status object indicating the validity of the name. The
   * status will have the code {@link IStatus.OK} if the name is valid as a field name, the code
   * {@link IStatus.WARNING} if the name is discouraged, or the code {@link IStatus.ERROR} if the
   * name is illegal. If the identifier is not valid then the status will have a message indicating
   * why.
   * 
   * @param name the field name being validated
   * @return a status object indicating the validity of the name
   */
  public static IStatus validateFieldName(String name) {
    return validateLowerCamelCase(name, MessageHolder.forField());
  }

  /**
   * Validate the given function name. Return a status object indicating the validity of the name.
   * The status will have the code {@link IStatus.OK} if the name is valid as a function name, the
   * code {@link IStatus.WARNING} if the name is discouraged, or the code {@link IStatus.ERROR} if
   * the name is illegal. If the identifier is not valid then the status will have a message
   * indicating why.
   * 
   * @param name the function name being validated
   * @return a status object indicating the validity of the name
   */
  public static IStatus validateFunctionName(String name) {
    return validateLowerCamelCase(name, MessageHolder.forFunction());
  }

  /**
   * Validate the given function type alias name. Return a status object indicating the validity of
   * the name. The status will have the code {@link IStatus.OK} if the name is valid as a function
   * type alias name, the code {@link IStatus.WARNING} if the name is discouraged, or the code
   * {@link IStatus.ERROR} if the name is illegal. If the identifier is not valid then the status
   * will have a message indicating why.
   * 
   * @param name the function name being validated
   * @return a status object indicating the validity of the name
   */
  public static IStatus validateFunctionTypeAliasName(String name) {
    return validateUpperCamelCase(name, MessageHolder.forFunctionTypeAlias());
  }

  /**
   * Validate the given method name. Return a status object indicating the validity of the name. The
   * status will have the code {@link IStatus.OK} if the name is valid as a method name, the code
   * {@link IStatus.WARNING} if the name is discouraged, or the code {@link IStatus.ERROR} if the
   * name is illegal. If the identifier is not valid then the status will have a message indicating
   * why.
   * 
   * @param name the method name being validated
   * @return a status object indicating the validity of the name
   */
  public static IStatus validateMethodName(String name) {
    return validateLowerCamelCase(name, MessageHolder.forMethod());
  }

  /**
   * Validate the given parameter name. Return a status object indicating the validity of the name.
   * The status will have the code {@link IStatus.OK} if the name is valid as a parameter name, the
   * code {@link IStatus.WARNING} if the name is discouraged, or the code {@link IStatus.ERROR} if
   * the name is illegal. If the identifier is not valid then the status will have a message
   * indicating why.
   * 
   * @param name the parameter name being validated
   * @return a status object indicating the validity of the name
   */
  public static IStatus validateParameterName(String name) {
    return validateLowerCamelCase(name, MessageHolder.forParameter());
  }

  /**
   * Validate the given Dart prefix. Return a status object indicating the validity of the name. The
   * status will have the code {@link IStatus.OK} if the given name is valid as a Dart prefix, the
   * code {@link IStatus.WARNING} if the given name is discouraged, or the code
   * {@link IStatus.ERROR} if the name is illegal. If the identifier is not valid then the status
   * will have a message indicating why.
   * 
   * @param prefix the prefix being validated
   * @return a status object indicating the validity of the name
   */
  public static IStatus validatePrefix(String prefix) {
    // null
    if (prefix == null) {
      return new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          -1,
          Messages.convention_prefix_null,
          null);
    }
    // leading or trailing space
    String trimmed = prefix.trim();
    if (!prefix.equals(trimmed)) {
      return new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          -1,
          Messages.convention_prefix_leadingOrTrailingBlanks,
          null);
    }
    // is not identifier
    IStatus status = validateIdentifier(prefix, MessageHolder.forPrefix());
    if (!status.isOK()) {
      return status;
    }
    // OK
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * Validate the given Dart type name, which can be either simple or qualified. Return a status
   * object indicating the validity of the name. The status will have the code {@link IStatus.OK} if
   * the given name is valid as a Dart type name, the code {@link IStatus.WARNING} if the given name
   * is discouraged, or the code {@link IStatus.ERROR} if the name is illegal. If the identifier is
   * not valid then the status will have a message indicating why.
   * <p>
   * For example, <code>"Object"</code> or <code>"goog$DB.Record"</code>.
   * 
   * @param name the type name being validated
   * @return a status object indicating the validity of the name
   */
  public static IStatus validateTypeName(String name) {
    return validateUpperCamelCase(name, MessageHolder.forType());
  }

  /**
   * Validate the given type parameter name. Return a status object indicating the validity of the
   * name. The status will have the code {@link IStatus.OK} if the name is valid as a type parameter
   * name, the code {@link IStatus.WARNING} if the name is discouraged, or the code
   * {@link IStatus.ERROR} if the name is illegal. If the identifier is not valid then the status
   * will have a message indicating why.
   * 
   * @param name the name being validated
   * @return a status object indicating the validity of the name
   */
  public static IStatus validateTypeParameterName(String name) {
    return validateUpperCamelCase(name, MessageHolder.forTypeParameter());
  }

  private static IStatus validateIdentifier(String identifier, MessageHolder messageHolder) {
    int length = identifier.length();
    if (length == 0) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, -1, messageHolder.empty, null);
    }
    char currentChar = identifier.charAt(0);
    if (!Character.isLetter(currentChar) && currentChar != '_' && currentChar != '$') {
      return new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          -1,
          messageHolder.initialChar(identifier),
          null);
    }
    for (int i = 1; i < length; i++) {
      currentChar = identifier.charAt(i);
      if (!Character.isLetterOrDigit(currentChar) && currentChar != '_' && currentChar != '$') {
        return new Status(
            IStatus.ERROR,
            DartCore.PLUGIN_ID,
            -1,
            messageHolder.internalChar(identifier),
            null);
      }
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * Validate the given identifier, which should be lower camel case. Return a status object
   * indicating the validity of the identifier. The status will have the code {@link IStatus.OK} if
   * the identifier is valid, {@link IStatus.WARNING} if the identifier is discouraged, or
   * {@link IStatus.ERROR} if the identifier is illegal. If the identifier is not valid then the
   * status will have a message indicating why.
   * 
   * @param identifier the identifier being validated
   * @param messageHolder a holder of messages explaining problems
   * @return a status object indicating the validity of the identifier
   */
  private static IStatus validateLowerCamelCase(String identifier, MessageHolder messageHolder) {
    // null
    if (identifier == null) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, -1, messageHolder.nullName, null);
    }
    // has leading/trailing spaces
    String trimmed = identifier.trim();
    if (!identifier.equals(trimmed)) {
      return new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          -1,
          messageHolder.leadingOrTrailingBlanks,
          null);
    }
    // is not identifier
    IStatus status = validateIdentifier(identifier, messageHolder);
    if (!status.isOK()) {
      return status;
    }
//    if (identifier.indexOf('$') >= 0) {
//      return new Status(IStatus.WARNING, DartCore.PLUGIN_ID, -1, messageHolder.dollar, null);
//    }
//    if (identifier.indexOf('_') >= 0) {
//      return new Status(IStatus.WARNING, DartCore.PLUGIN_ID, -1, messageHolder.underscore, null);
//    }
    // is private, OK
    if (identifier.charAt(0) == '_') {
      return DartModelStatusImpl.VERIFIED_OK;
    }
    // does not start with lower case
    if (!Character.isLowerCase(identifier.charAt(0))) {
      return new Status(IStatus.WARNING, DartCore.PLUGIN_ID, -1, messageHolder.initialCase, null);
    }
    // OK
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * Validate the given identifier, which should be upper camel case. Return a status object
   * indicating the validity of the identifier. The status will have the code {@link IStatus.OK} if
   * the identifier is valid, {@link IStatus.WARNING} if the identifier is discouraged, or
   * {@link IStatus.ERROR} if the identifier is illegal. If the identifier is not valid then the
   * status will have a message indicating why.
   * 
   * @param identifier the identifier being validated
   * @param messageHolder a holder of messages explaining problems
   * @return a status object indicating the validity of the identifier
   */
  private static IStatus validateUpperCamelCase(String identifier, MessageHolder messageHolder) {
    // null
    if (identifier == null) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, -1, messageHolder.nullName, null);
    }
    // leading or trailing spaces
    String trimmed = identifier.trim();
    if (!identifier.equals(trimmed)) {
      return new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          -1,
          messageHolder.leadingOrTrailingBlanks,
          null);
    }
    // is not identifier
    IStatus status = validateIdentifier(identifier, messageHolder);
    if (!status.isOK()) {
      return status;
    }
//    if (identifier.indexOf('$') >= 0) {
//      return new Status(IStatus.WARNING, DartCore.PLUGIN_ID, -1, messageHolder.dollar, null);
//    }
//    if (identifier.indexOf('_') >= 0) {
//      return new Status(IStatus.WARNING, DartCore.PLUGIN_ID, -1, messageHolder.underscore, null);
//    }
    // is private, OK
    if (identifier.charAt(0) == '_') {
      return DartModelStatusImpl.VERIFIED_OK;
    }
    // does not start with upper case
    if (!Character.isUpperCase(identifier.charAt(0))) {
      return new Status(IStatus.WARNING, DartCore.PLUGIN_ID, -1, messageHolder.initialCase, null);
    }
    // OK
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private DartConventions() {
    super();
  }
}
