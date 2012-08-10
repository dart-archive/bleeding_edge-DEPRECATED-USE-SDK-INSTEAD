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
package com.google.dart.engine.parser;

import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.error.SubSystem;

/**
 * The enumeration {@code ParserErrorCode} defines the error codes used for errors detected by the
 * parser.
 */
public enum ParserErrorCode implements ErrorCode {
//  ABSTRACT_METHOD_WITH_BODY("Abstract method cannot have a body"),
//  ABSTRACT_TOP_LEVEL_ELEMENT("Only class can be abstract top-level element"),
  BREAK_OUTSIDE_OF_LOOP("'break' used outside of loop or case statement"),
  BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME(ErrorSeverity.WARNING,
      "Built-in identifier cannot be used as a function name"),
  BUILT_IN_IDENTIFIER_AS_LABEL(ErrorSeverity.WARNING,
      "Built-in identifier cannot be used as a label"),
  BUILT_IN_IDENTIFIER_AS_TYPE_NAME("Built-in identifier cannot be used as a type name"),
  BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME("Built-in identifier cannot be used as a type alias name"),
  BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME(
      "Built-in identifier cannot be used as a type variable name"),
  BUILT_IN_IDENTIFIER_AS_VARIABLE_NAME(ErrorSeverity.WARNING,
      "Built-in identifier cannot be used as a variable name"),
  CATCH_OR_FINALLY_EXPECTED("catch or finally clause expected"),
  CONTINUE_IN_CASE_MUST_HAVE_LABEL("'continue' in case statement must have a label as a target"),
  CONTINUE_OUTSIDE_OF_LOOP("'continue' used outside of loop or case statement"),
//  DEFAULT_VALUE_CAN_NOT_BE_SPECIFIED_IN_CLOSURE(
//      "Default values can not be specified in closure parameter"),
//  DEFAULT_VALUE_CAN_NOT_BE_SPECIFIED_IN_TYPEDEF(
//      "Default values can not be specified in closure type definition"),
//  DEFAULT_POSITIONAL_PARAMETER("Positional parameters cannot have default values"),
//  DEPRECATED_USE_OF_FACTORY_KEYWORD(ErrorSeverity.WARNING,
//      "Deprecated use of the 'factory' keyword: use 'default' instead"),
  DIRECTIVE_OUT_OF_ORDER("Directive out of order"),
//  DISALLOWED_ABSTRACT_KEYWORD("Abstract keyword not allowed here"),
//  DISALLOWED_FACTORY_KEYWORD("Factory keyword not allowed here"),
//  DISALLOWED_STATIC_KEYWORD("Static keyword not allowed here"),
  DUPLICATE_LABEL_IN_SWITCH_STATEMENT("Duplicate label in switch statement"),
//  EXPECTED_ARRAY_OR_MAP_LITERAL("Expected array or map literal"),
  EXPECTED_CASE_OR_DEFAULT("Expected 'case' or 'default'"),
//  EXPECTED_CLASS_DECLARATION_LBRACE("Expected '{' in class declaration"),
//  EXPECTED_COMMA_OR_RIGHT_BRACE("Expected ',' or '}'"),
//  EXPECTED_COMMA_OR_RIGHT_PAREN("Expected ',' or ')', but got '%s'"),
//  EXPECTED_EOS("Unexpected token '%s' (expected end of file)"),
//  EXPECTED_EXPRESSION_AFTER_COMMA("Expected expression after comma"),
//  EXPECTED_EXTENDS("Expected 'extends'"),
//  EXPECTED_FUNCTION_STATEMENT_BODY("Expected function statement body"),
  EXPECTED_IDENTIFIER("Expected identifier"),
//  EXPECTED_LEFT_PAREN("'(' expected"),
  EXPECTED_LIST_OR_MAP_LITERAL("Expected list or map literal"),
//  EXPECTED_PREFIX_KEYWORD("Expected 'prefix' after comma"),
//  EXPECTED_PREFIX_IDENTIFIER("Prefix string can only contain valid identifier characters"),
//  EXPECTED_SEMICOLON("Expected ';'"),
  EXPECTED_STRING_LITERAL("Expected string literal"),
//  EXPECTED_STRING_LITERAL_MAP_ENTRY_KEY("Expected string literal for map entry key"),
//  EXPECTED_TOKEN("Unexpected token '%s' (expected '%s')"),
//  EXPECTED_VAR_FINAL_OR_TYPE("Expected 'var', 'final' or type"),
//  NATIVE_ONLY_CLASS("Native keyword can be specified only for classes"),
//  NATIVE_ONLY_CORE_LIB("Native keyword can be used only in corelib"),
//  FACTORY_CANNOT_BE_ABSTRACT("A factory cannot be abstract"),
//  FACTORY_CANNOT_BE_STATIC("A factory cannot be static"),
//  FACTORY_CANNOT_BE_TOP_LEVEL("A factory cannot be top level, it must be a member of a class"),
//  FINAL_IS_NOT_ALLOWED_ON_A_METHOD_DEFINITION("'final' is not allowed on a method definition"),
//  FOR_IN_WITH_COMPLEX_VARIABLE("Only simple variables can be assigned to in a for-in construct"),
//  FOR_IN_WITH_MULTIPLE_VARIABLES("Too many variable declarations in a for-in construct"),
//  FOR_IN_WITH_VARIABLE_INITIALIZER("Cannot initialize for-in variables"),
//  FUNCTION_TYPED_PARAMETER_IS_FINAL("Formal parameter with a function type cannot be const"),
//  FUNCTION_TYPED_PARAMETER_IS_VAR("Formal parameter with a function type cannot be var"),
//  FUNCTION_NAME_EXPECTED_IDENTIFIER("Function name expected to be an identifier"),
//  ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE("Illegal assignment to non-assignable expression"),
//  ILLEGAL_NUMBER_OF_PARAMETERS("Illegal number of parameters"),
//  INCOMPLETE_STRING_LITERAL("Incomplete string literal"),
//  INVALID_FIELD_DECLARATION("Wrong syntax for field declaration"),
//  INVALID_IDENTIFIER("The token '%s' cannot be used as an identifier"),
//  INVALID_OPERATOR_CHAINING("Cannot chain '%s'"),
//  LABEL_NOT_FOLLOWED_BY_CASE_OR_DEFAULT("Label not followed by 'case', 'default', or statement"),
//  LOCAL_CANNOT_BE_STATIC("Local function can not be static"),
//  MISSING_FUNCTION_NAME("a function name is required for a declaration"),
//  NAMED_PARAMETER_NOT_ALLOWED("Named parameter is not allowed for operator or setter method"),
//  NO_SPACE_AFTER_PLUS("Cannot have space between plus and numeric literal"),
//  NO_SOUP_FOR_YOU("Too many errors"),
  NO_UNARY_PLUS_OPERATOR("No unary plus operator in Dart"),
  ONLY_ONE_LIBRARY_DIRECTIVE("Only one library directive may be declared in a file"),
  OPERATOR_CANNOT_BE_STATIC("Operators cannot be static"),
  OPERATOR_IS_NOT_USER_DEFINABLE("Operator is not user definable"),
  POSITIONAL_AFTER_NAMED_ARGUMENT("Positional argument after named argument"),
//  REDIRECTING_CONSTRUCTOR_CANNOT_HAVE_A_BODY("Redirecting constructor cannot have a body"),
//  REDIRECTING_CONSTRUCTOR_PARAM("Redirecting constructor can not have initializers"),
//  REDIRECTING_CONSTRUCTOR_ITSELF("Redirecting constructor can not have initializers"),
//  REDIRECTING_CONSTRUCTOR_MULTIPLE("Multiple redirecting constructor invocations"),
//  REDIRECTING_CONSTRUCTOR_OTHER("Redirecting constructor can not have initializers"),
//  STATIC_MEMBERS_CANNOT_BE_ABSTRACT("Static members cannot be abstract"),
//  SUPER_CONSTRUCTOR_MULTIPLE("'super' must be called only once in the initialization list"),
//  SUPER_CANNOT_BE_USED_AS_THE_SECOND_OPERAND(
//      "'super' cannot be used as the second operand in a binary expression."),
//  SUPER_IS_NOT_VALID_AS_A_BOOLEAN_OPERAND("'super' is not valid as a boolean operand"),
//  SUPER_IS_NOT_VALID_ALONE_OR_AS_A_BOOLEAN_OPERAND(
//      "'super' is not valid alone or as a boolean operand"),
  TOP_LEVEL_CANNOT_BE_STATIC("Top-level field or method cannot be static"),
//  UNREACHABLE_CODE_IN_CASE(ErrorSeverity.WARNING, "Unreachable code in case statement"),
  UNEXPECTED_TOKEN("Unexpected token '%s'");
//  UNEXPECTED_TOKEN_IN_STRING_INTERPOLATION("Unexpected token in string interpolation: %s"),
//  VAR_IS_NOT_ALLOWED_ON_A_METHOD_DEFINITION("'var' is not allowed on a method definition"),
//  VOID_FIELD("Field cannot be of type void"),
//  VOID_PARAMETER("Parameter cannot be of type void");

  /**
   * The severity of this error.
   */
  private final ErrorSeverity severity;

  /**
   * The message template used to create the message to be displayed for this error.
   */
  private final String message;

  /**
   * Initialize a newly created error code to have the given severity and message.
   * 
   * @param severity the severity of the error
   * @param message the message template used to create the message to be displayed for the error
   */
  private ParserErrorCode(ErrorSeverity severity, String message) {
    this.severity = severity;
    this.message = message;
  }

  /**
   * Initialize a newly created error code to have the given message and a severity of ERROR.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private ParserErrorCode(String message) {
    this(ErrorSeverity.ERROR, message);
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return severity;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public SubSystem getSubSystem() {
    return SubSystem.PARSER;
  }

  @Override
  public boolean needsRecompilation() {
    return true;
  }
}
