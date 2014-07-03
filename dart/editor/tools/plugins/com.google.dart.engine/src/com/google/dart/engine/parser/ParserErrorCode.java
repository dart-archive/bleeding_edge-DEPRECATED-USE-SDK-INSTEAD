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
import com.google.dart.engine.error.ErrorType;

/**
 * The enumeration {@code ParserErrorCode} defines the error codes used for errors detected by the
 * parser. The convention for this class is for the name of the error code to indicate the problem
 * that caused the error to be generated and for the error message to explain what is wrong and,
 * when appropriate, how the problem can be corrected.
 * 
 * @coverage dart.engine.parser
 */
public enum ParserErrorCode implements ErrorCode {
  ABSTRACT_CLASS_MEMBER("Members of classes cannot be declared to be 'abstract'"),
  ABSTRACT_STATIC_METHOD("Static methods cannot be declared to be 'abstract'"),
  ABSTRACT_TOP_LEVEL_FUNCTION("Top-level functions cannot be declared to be 'abstract'"),
  ABSTRACT_TOP_LEVEL_VARIABLE("Top-level variables cannot be declared to be 'abstract'"),
  ABSTRACT_TYPEDEF("Type aliases cannot be declared to be 'abstract'"),
  ASSERT_DOES_NOT_TAKE_ASSIGNMENT("Assert cannot be called on an assignment"),
  ASSERT_DOES_NOT_TAKE_CASCADE("Assert cannot be called on cascade"),
  ASSERT_DOES_NOT_TAKE_THROW("Assert cannot be called on throws"),
  ASSERT_DOES_NOT_TAKE_RETHROW("Assert cannot be called on rethrows"),
  BREAK_OUTSIDE_OF_LOOP("A break statement cannot be used outside of a loop or switch statement"),
  CONST_AND_FINAL("Members cannot be declared to be both 'const' and 'final'"),
  CONST_AND_VAR("Members cannot be declared to be both 'const' and 'var'"),
  CONST_CLASS("Classes cannot be declared to be 'const'"),
  CONST_CONSTRUCTOR_WITH_BODY("'const' constructors cannot have a body"),
  CONST_FACTORY("Only redirecting factory constructors can be declared to be 'const'"),
  CONST_METHOD("Getters, setters and methods cannot be declared to be 'const'"),
  CONST_TYPEDEF("Type aliases cannot be declared to be 'const'"),
  CONSTRUCTOR_WITH_RETURN_TYPE("Constructors cannot have a return type"),
  CONTINUE_OUTSIDE_OF_LOOP(
      "A continue statement cannot be used outside of a loop or switch statement"),
  CONTINUE_WITHOUT_LABEL_IN_CASE(
      "A continue statement in a switch statement must have a label as a target"),
  DEFERRED_IMPORTS_NOT_SUPPORTED("Deferred imports are not supported by default"),
  DEPRECATED_CLASS_TYPE_ALIAS("The 'typedef' mixin application was replaced with 'class'"),
  DIRECTIVE_AFTER_DECLARATION("Directives must appear before any declarations"),
  DUPLICATE_LABEL_IN_SWITCH_STATEMENT("The label %s was already used in this switch statement"),
  DUPLICATED_MODIFIER("The modifier '%s' was already specified."),
  EQUALITY_CANNOT_BE_EQUALITY_OPERAND(
      "Equality expression cannot be operand of another equality expression."),
  EXPECTED_CASE_OR_DEFAULT("Expected 'case' or 'default'"),
  EXPECTED_CLASS_MEMBER("Expected a class member"),
  EXPECTED_EXECUTABLE("Expected a method, getter, setter or operator declaration"),
  EXPECTED_LIST_OR_MAP_LITERAL("Expected a list or map literal"),
  EXPECTED_STRING_LITERAL("Expected a string literal"),
  EXPECTED_TOKEN("Expected to find '%s'"),
  EXPECTED_TYPE_NAME("Expected a type name"),
  EXPORT_DIRECTIVE_AFTER_PART_DIRECTIVE("Export directives must preceed part directives"),
  EXTERNAL_AFTER_CONST("The modifier 'external' should be before the modifier 'const'"),
  EXTERNAL_AFTER_FACTORY("The modifier 'external' should be before the modifier 'factory'"),
  EXTERNAL_AFTER_STATIC("The modifier 'external' should be before the modifier 'static'"),
  EXTERNAL_CLASS("Classes cannot be declared to be 'external'"),
  EXTERNAL_CONSTRUCTOR_WITH_BODY("External constructors cannot have a body"),
  EXTERNAL_FIELD("Fields cannot be declared to be 'external'"),
  EXTERNAL_GETTER_WITH_BODY("External getters cannot have a body"),
  EXTERNAL_METHOD_WITH_BODY("External methods cannot have a body"),
  EXTERNAL_OPERATOR_WITH_BODY("External operators cannot have a body"),
  EXTERNAL_SETTER_WITH_BODY("External setters cannot have a body"),
  EXTERNAL_TYPEDEF("Type aliases cannot be declared to be 'external'"),
  FACTORY_TOP_LEVEL_DECLARATION("Top-level declarations cannot be declared to be 'factory'"),
  FACTORY_WITHOUT_BODY("A non-redirecting 'factory' constructor must have a body"),
  FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR("Field initializers can only be used in a constructor"),
  FINAL_AND_VAR("Members cannot be declared to be both 'final' and 'var'"),
  FINAL_CLASS("Classes cannot be declared to be 'final'"),
  FINAL_CONSTRUCTOR("A constructor cannot be declared to be 'final'"),
  FINAL_METHOD("Getters, setters and methods cannot be declared to be 'final'"),
  FINAL_TYPEDEF("Type aliases cannot be declared to be 'final'"),
  FUNCTION_TYPED_PARAMETER_VAR(
      "Function typed parameters cannot specify 'const', 'final' or 'var' instead of return type"),
  GETTER_IN_FUNCTION("Getters cannot be defined within methods or functions"),
  GETTER_WITH_PARAMETERS("Getter should be declared without a parameter list"),
  ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE("Illegal assignment to non-assignable expression"),
  IMPLEMENTS_BEFORE_EXTENDS("The extends clause must be before the implements clause"),
  IMPLEMENTS_BEFORE_WITH("The with clause must be before the implements clause"),
  IMPORT_DIRECTIVE_AFTER_PART_DIRECTIVE("Import directives must preceed part directives"),
  INITIALIZED_VARIABLE_IN_FOR_EACH("The loop variable in a for-each loop cannot be initialized"),
  INVALID_AWAIT_IN_FOR("The modifier 'await' is not allowed for a normal 'for' statement",
      "Remove the keyword or use a for-each statement."),
  INVALID_CODE_POINT("The escape sequence '%s' is not a valid code point"),
  INVALID_COMMENT_REFERENCE(
      "Comment references should contain a possibly prefixed identifier and can start with 'new', but should not contain anything else"),
  INVALID_HEX_ESCAPE(
      "An escape sequence starting with '\\x' must be followed by 2 hexidecimal digits"),
  INVALID_OPERATOR("The string '%s' is not a valid operator"),
  INVALID_OPERATOR_FOR_SUPER("The operator '%s' cannot be used with 'super'"),
  INVALID_STAR_AFTER_ASYNC("The modifier 'async*' is not allowed for an expression function body",
      "Convert the body to a block."),
  INVALID_SYNC("The modifier 'sync' is not allowed for an exrpression function body",
      "Convert the body to a block."),
  INVALID_UNICODE_ESCAPE(
      "An escape sequence starting with '\\u' must be followed by 4 hexidecimal digits or from 1 to 6 digits between '{' and '}'"),
  LIBRARY_DIRECTIVE_NOT_FIRST("The library directive must appear before all other directives"),
  LOCAL_FUNCTION_DECLARATION_MODIFIER("Local function declarations cannot specify any modifier"),
  // TODO(brianwilkerson) Improve this message. We probably need to know the context in which we are
  // parsing the assignable selector in order to get decent messages.
  MISSING_ASSIGNABLE_SELECTOR("Missing selector such as \".<identifier>\" or \"[0]\""),
  MISSING_CATCH_OR_FINALLY("A try statement must have either a catch or finally clause"),
  MISSING_CLASS_BODY("A class definition must have a body, even if it is empty"),
  MISSING_CLOSING_PARENTHESIS("The closing parenthesis is missing"),
  MISSING_CONST_FINAL_VAR_OR_TYPE(
      "Variables must be declared using the keywords 'const', 'final', 'var' or a type name"),
  MISSING_EXPRESSION_IN_THROW("Throw expressions must compute the object to be thrown"),
  MISSING_FUNCTION_BODY("A function body must be provided"),
  MISSING_FUNCTION_PARAMETERS("Functions must have an explicit list of parameters"),
  MISSING_GET("Getters must have the keyword 'get' before the getter name"),
  MISSING_IDENTIFIER("Expected an identifier"),
  MISSING_KEYWORD_OPERATOR("Operator declarations must be preceeded by the keyword 'operator'"),
  MISSING_NAME_IN_LIBRARY_DIRECTIVE("Library directives must include a library name"),
  MISSING_NAME_IN_PART_OF_DIRECTIVE("Library directives must include a library name"),
  MISSING_PREFIX_IN_DEFERRED_IMPORT("Deferred imports must have a prefix"),
  MISSING_STAR_AFTER_SYNC("The modifier 'sync' must be followed by a star ('*')",
      "Remove the modifier or add a star."),
  MISSING_STATEMENT("Expected a statement"),
  MISSING_TERMINATOR_FOR_PARAMETER_GROUP("There is no '%s' to close the parameter group"),
  MISSING_TYPEDEF_PARAMETERS("Type aliases for functions must have an explicit list of parameters"),
  MISSING_VARIABLE_IN_FOR_EACH(
      "A loop variable must be declared in a for-each loop before the 'in', but none were found"),
  MIXED_PARAMETER_GROUPS(
      "Cannot have both positional and named parameters in a single parameter list"),
  MULTIPLE_EXTENDS_CLAUSES("Each class definition can have at most one extends clause"),
  MULTIPLE_IMPLEMENTS_CLAUSES("Each class definition can have at most one implements clause"),
  MULTIPLE_LIBRARY_DIRECTIVES("Only one library directive may be declared in a file"),
  MULTIPLE_NAMED_PARAMETER_GROUPS(
      "Cannot have multiple groups of named parameters in a single parameter list"),
  MULTIPLE_PART_OF_DIRECTIVES("Only one part-of directive may be declared in a file"),
  MULTIPLE_POSITIONAL_PARAMETER_GROUPS(
      "Cannot have multiple groups of positional parameters in a single parameter list"),
  MULTIPLE_VARIABLES_IN_FOR_EACH(
      "A single loop variable must be declared in a for-each loop before the 'in', but %s were found"),
  MULTIPLE_WITH_CLAUSES("Each class definition can have at most one with clause"),
  NAMED_FUNCTION_EXPRESSION("Function expressions cannot be named"),
  NAMED_PARAMETER_OUTSIDE_GROUP("Named parameters must be enclosed in curly braces ('{' and '}')"),
  NATIVE_CLAUSE_IN_NON_SDK_CODE(
      "Native clause can only be used in the SDK and code that is loaded through native extensions"),
  NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE(
      "Native functions can only be declared in the SDK and code that is loaded through native extensions"),
  NON_CONSTRUCTOR_FACTORY("Only constructors can be declared to be a 'factory'"),
  NON_IDENTIFIER_LIBRARY_NAME("The name of a library must be an identifier"),
  NON_PART_OF_DIRECTIVE_IN_PART("The part-of directive must be the only directive in a part"),
  NON_USER_DEFINABLE_OPERATOR("The operator '%s' is not user definable"),
  NORMAL_BEFORE_OPTIONAL_PARAMETERS("Normal parameters must occur before optional parameters"),
  POSITIONAL_AFTER_NAMED_ARGUMENT("Positional arguments must occur before named arguments"),
  POSITIONAL_PARAMETER_OUTSIDE_GROUP(
      "Positional parameters must be enclosed in square brackets ('[' and ']')"),
  REDIRECTION_IN_NON_FACTORY_CONSTRUCTOR("Only factory constructor can specify '=' redirection."),
  SETTER_IN_FUNCTION("Setters cannot be defined within methods or functions"),
  STATIC_AFTER_CONST("The modifier 'static' should be before the modifier 'const'"),
  STATIC_AFTER_FINAL("The modifier 'static' should be before the modifier 'final'"),
  STATIC_AFTER_VAR("The modifier 'static' should be before the modifier 'var'"),
  STATIC_CONSTRUCTOR("Constructors cannot be static"),
  STATIC_GETTER_WITHOUT_BODY("A 'static' getter must have a body"),
  STATIC_OPERATOR("Operators cannot be static"),
  STATIC_SETTER_WITHOUT_BODY("A 'static' setter must have a body"),
  STATIC_TOP_LEVEL_DECLARATION("Top-level declarations cannot be declared to be 'static'"),
  SWITCH_HAS_CASE_AFTER_DEFAULT_CASE(
      "The 'default' case should be the last case in a switch statement"),
  SWITCH_HAS_MULTIPLE_DEFAULT_CASES("The 'default' case can only be declared once"),
  TOP_LEVEL_OPERATOR("Operators must be declared within a class"),
  UNEXPECTED_TERMINATOR_FOR_PARAMETER_GROUP("There is no '%s' to open a parameter group"),
  UNEXPECTED_TOKEN("Unexpected token '%s'"),
  WITH_BEFORE_EXTENDS("The extends clause must be before the with clause"),
  WITH_WITHOUT_EXTENDS("The with clause cannot be used without an extends clause"),
  WRONG_SEPARATOR_FOR_NAMED_PARAMETER(
      "The default value of a named parameter should be preceeded by ':'"),
  WRONG_SEPARATOR_FOR_POSITIONAL_PARAMETER(
      "The default value of a positional parameter should be preceeded by '='"),
  WRONG_TERMINATOR_FOR_PARAMETER_GROUP("Expected '%s' to close parameter group"),
  VAR_AND_TYPE("Variables cannot be declared using both 'var' and a type name; remove the 'var'"),
  VAR_AS_TYPE_NAME("The keyword 'var' cannot be used as a type name"),
  VAR_CLASS("Classes cannot be declared to be 'var'"),
  VAR_RETURN_TYPE("The return type cannot be 'var'"),
  VAR_TYPEDEF("Type aliases cannot be declared to be 'var'"),
  VOID_PARAMETER("Parameters cannot have a type of 'void'"),
  VOID_VARIABLE("Variables cannot have a type of 'void'");

  /**
   * The severity of this error.
   */
  private final ErrorSeverity errorSeverity;

  /**
   * The template used to create the message to be displayed for this error.
   */
  private final String message;

  /**
   * The template used to create the correction to be displayed for this error, or {@code null} if
   * there is no correction information for this error.
   */
  public String correction;

  /**
   * Initialize a newly created error code to have the given severity and message.
   * 
   * @param errorSeverity the severity of the error
   * @param message the message template used to create the message to be displayed for the error
   */
  private ParserErrorCode(ErrorSeverity errorSeverity, String message) {
    this(errorSeverity, message, null);
  }

  /**
   * Initialize a newly created error code to have the given severity, message and correction.
   * 
   * @param errorSeverity the severity of the error
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private ParserErrorCode(ErrorSeverity errorSeverity, String message, String correction) {
    this.errorSeverity = errorSeverity;
    this.message = message;
    this.correction = correction;
  }

  /**
   * Initialize a newly created error code to have the given message and a severity of ERROR.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private ParserErrorCode(String message) {
    this(ErrorSeverity.ERROR, message, null);
  }

  /**
   * Initialize a newly created error code to have the given message and a severity of ERROR.
   * 
   * @param message the message template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private ParserErrorCode(String message, String correction) {
    this(ErrorSeverity.ERROR, message, correction);
  }

  @Override
  public String getCorrection() {
    return correction;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return errorSeverity;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorType getType() {
    return ErrorType.SYNTACTIC_ERROR;
  }

  @Override
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
