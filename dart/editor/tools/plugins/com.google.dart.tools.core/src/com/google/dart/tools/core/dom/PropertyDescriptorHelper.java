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
package com.google.dart.tools.core.dom;

import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartArrayLiteral;
import com.google.dart.compiler.ast.DartAssertion;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartBooleanLiteral;
import com.google.dart.compiler.ast.DartBreakStatement;
import com.google.dart.compiler.ast.DartCase;
import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartClassMember;
import com.google.dart.compiler.ast.DartComment;
import com.google.dart.compiler.ast.DartConditional;
import com.google.dart.compiler.ast.DartContinueStatement;
import com.google.dart.compiler.ast.DartDeclaration;
import com.google.dart.compiler.ast.DartDefault;
import com.google.dart.compiler.ast.DartDoWhileStatement;
import com.google.dart.compiler.ast.DartDoubleLiteral;
import com.google.dart.compiler.ast.DartEmptyStatement;
import com.google.dart.compiler.ast.DartExprStmt;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartForInStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartGotoStatement;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartInitializer;
import com.google.dart.compiler.ast.DartIntegerLiteral;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartLabel;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartLiteral;
import com.google.dart.compiler.ast.DartMapLiteral;
import com.google.dart.compiler.ast.DartMapLiteralEntry;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNamedExpression;
import com.google.dart.compiler.ast.DartNativeBlock;
import com.google.dart.compiler.ast.DartNativeDirective;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNullLiteral;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartParameterizedTypeNode;
import com.google.dart.compiler.ast.DartParenthesizedExpression;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartRedirectConstructorInvocation;
import com.google.dart.compiler.ast.DartResourceDirective;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartStringInterpolation;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartSuperExpression;
import com.google.dart.compiler.ast.DartSwitchMember;
import com.google.dart.compiler.ast.DartSwitchStatement;
import com.google.dart.compiler.ast.DartSyntheticErrorExpression;
import com.google.dart.compiler.ast.DartSyntheticErrorStatement;
import com.google.dart.compiler.ast.DartThisExpression;
import com.google.dart.compiler.ast.DartThrowStatement;
import com.google.dart.compiler.ast.DartTryStatement;
import com.google.dart.compiler.ast.DartTypeExpression;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartTypeParameter;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.ast.DartWhileStatement;
import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.DartCore;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * The class <code>PropertyDescriptorHelper</code> defines constants and methods that allow clients
 * to access the properties of AST nodes through {@link StructuralPropertyDescriptor}s.
 */
public class PropertyDescriptorHelper {
  /**
   * The expression computing the value being used to access the array.
   */
  public static final StructuralPropertyDescriptor DART_ARRAY_ACCESS_KEY = new ChildPropertyDescriptor(
      DartArrayAccess.class,
      "key",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression computing the array being accessed.
   */
  public static final StructuralPropertyDescriptor DART_ARRAY_ACCESS_TARGET = new ChildPropertyDescriptor(
      DartArrayAccess.class,
      "target",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The list of expressions computing the values in the array.
   */
  public static final StructuralPropertyDescriptor DART_ARRAY_LITERAL_EXPRESSIONS = new ChildListPropertyDescriptor(
      DartArrayLiteral.class,
      "expressions",
      DartExpression.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression in an assertion statement.
   */
  public static final StructuralPropertyDescriptor DART_ASSERTION_EXPRESSION = new ChildPropertyDescriptor(
      DartAssertion.class,
      "expression",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The expression computing the left operand of the binary operator.
   */
  public static final StructuralPropertyDescriptor DART_BINARY_EXPRESSION_LEFT_OPERAND = new ChildPropertyDescriptor(
      DartBinaryExpression.class,
      "arg1",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The binary operator.
   */
  public static final StructuralPropertyDescriptor DART_BINARY_EXPRESSION_OPERATOR = new SimplePropertyDescriptor(
      DartBinaryExpression.class,
      "op",
      Token.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The expression computing the right operand of the binary operator.
   */
  public static final StructuralPropertyDescriptor DART_BINARY_EXPRESSION_RIGHT_OPERAND = new ChildPropertyDescriptor(
      DartBinaryExpression.class,
      "arg2",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The list of statements in the block.
   */
  public static final StructuralPropertyDescriptor DART_BLOCK_STATEMENTS = new ChildListPropertyDescriptor(
      DartBlock.class,
      "stmts",
      DartStatement.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The value of the literal.
   */
  public static final StructuralPropertyDescriptor DART_BOOLEAN_LITERAL_VALUE = new SimplePropertyDescriptor(
      DartBooleanLiteral.class,
      "value",
      boolean.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The expression computing the value to be compared with the switch value.
   */
  public static final StructuralPropertyDescriptor DART_CASE_EXPRESSION = new ChildPropertyDescriptor(
      DartCase.class,
      "expr",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * 
   */
  public static final StructuralPropertyDescriptor DART_CASE_LABEL = new ChildPropertyDescriptor(
      DartCase.class,
      "label",
      DartLabel.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The statements executed in the enclosing statement's expression evaluates to the case
   * expression's value.
   */
  public static final StructuralPropertyDescriptor DART_CASE_STATEMENTS = new ChildListPropertyDescriptor(
      DartCase.class,
      "statements",
      DartStatement.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The declaration of the variable to which the exception will be assigned.
   */
  public static final StructuralPropertyDescriptor DART_CATCH_BLOCK_EXCEPTION = new ChildPropertyDescriptor(
      DartCatchBlock.class,
      "exception",
      DartParameter.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The declaration of the variable to which the stack trace will be assigned.
   */
  public static final StructuralPropertyDescriptor DART_CATCH_BLOCK_STACK_TRACE = new ChildPropertyDescriptor(
      DartCatchBlock.class,
      "stackTrace",
      DartParameter.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The block containing the statements that will be executed.
   */
  public static final StructuralPropertyDescriptor DART_CATCH_BLOCK_BODY = new ChildPropertyDescriptor(
      DartCatchBlock.class,
      "block",
      DartBlock.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The name of the default implementation class of the interface being defined.
   */
  public static final StructuralPropertyDescriptor DART_CLASS_DEFAULT_CLASS = new ChildPropertyDescriptor(
      DartClass.class,
      "defaultClass",
      DartTypeNode.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * A list of the interfaces implemented or extended by the class or interface being defined.
   */
  public static final StructuralPropertyDescriptor DART_CLASS_INTERFACES = new ChildListPropertyDescriptor(
      DartClass.class,
      "interfaces",
      DartTypeNode.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * A list of the interfaces implemented or extended by the class or interface being defined.
   */
  public static final StructuralPropertyDescriptor DART_CLASS_IS_INTERFACE = new SimplePropertyDescriptor(
      DartClass.class,
      "isInterface",
      boolean.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The list of members of the class or interface being defined.
   */
  public static final StructuralPropertyDescriptor DART_CLASS_MEMBERS = new ChildListPropertyDescriptor(
      DartClass.class,
      "members",
      DartClassMember.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The name of the class or interface being defined.
   */
  public static final StructuralPropertyDescriptor DART_CLASS_NAME = new ChildPropertyDescriptor(
      DartClass.class,
      "name",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The superclass of the class or interface being defined.
   */
  public static final StructuralPropertyDescriptor DART_CLASS_SUPERCLASS = new ChildPropertyDescriptor(
      DartClass.class,
      "superclass",
      DartTypeNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * A list of the type parameters associated with this class.
   */
  public static final StructuralPropertyDescriptor DART_CLASS_TYPE_PARAMETERS = new ChildListPropertyDescriptor(
      DartClass.class,
      "typeParameters",
      DartTypeParameter.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The modifiers associated with the member being defined.
   */
  public static final StructuralPropertyDescriptor DART_CLASS_MEMBER_MODIFIERS = new SimplePropertyDescriptor(
      DartClassMember.class,
      "modifiers",
      Modifiers.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The name of the member being defined.
   */
  // TODO(brianwilkerson) Lobby to get the type of this member changed!
  public static final StructuralPropertyDescriptor DART_CLASS_MEMBER_NAME = new ChildPropertyDescriptor(
      DartClassMember.class,
      "name",
      DartNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The condition determining which of the other expressions will be evaluated.
   */
  public static final StructuralPropertyDescriptor DART_CONDITIONAL_CONDITION = new ChildPropertyDescriptor(
      DartConditional.class,
      "condition",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression that will be evaluated if the condition is false.
   */
  public static final StructuralPropertyDescriptor DART_CONDITIONAL_ELSE = new ChildPropertyDescriptor(
      DartConditional.class,
      "Expr",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression that will be evaluated if the condition is true.
   */
  public static final StructuralPropertyDescriptor DART_CONDITIONAL_THEN = new ChildPropertyDescriptor(
      DartConditional.class,
      "thenExpr",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The value of the literal.
   */
  public static final StructuralPropertyDescriptor DART_DOUBLE_LITERAL_VALUE = new SimplePropertyDescriptor(
      DartDoubleLiteral.class,
      "value",
      double.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The body of the loop.
   */
  public static final StructuralPropertyDescriptor DART_DO_WHILE_STATEMENT_BODY = new ChildPropertyDescriptor(
      DartDoWhileStatement.class,
      "body",
      DartStatement.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression that will be evaluated to determine whether the loop should repeat.
   */
  public static final StructuralPropertyDescriptor DART_DO_WHILE_STATEMENT_CONDITION = new ChildPropertyDescriptor(
      DartDoWhileStatement.class,
      "condition",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The expression being represented as a statement.
   */
  public static final StructuralPropertyDescriptor DART_EXPRESSION_STATEMENT_EXPRESSION = new ChildPropertyDescriptor(
      DartExprStmt.class,
      "expr",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The initial value of the field.
   */
  public static final StructuralPropertyDescriptor DART_FIELD_VALUE = new ChildPropertyDescriptor(
      DartField.class,
      "value",
      DartExpression.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The fields contained within a field definition.
   */
  public static final StructuralPropertyDescriptor DART_FIELD_DEFINITION_FIELDS = new ChildListPropertyDescriptor(
      DartFieldDefinition.class,
      "fields",
      DartField.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The type associated with a field definition.
   */
  public static final StructuralPropertyDescriptor DART_FIELD_DEFINITION_TYPE = new ChildPropertyDescriptor(
      DartFieldDefinition.class,
      "type",
      DartTypeNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The body of the loop.
   */
  public static final StructuralPropertyDescriptor DART_FOR_IN_STATEMENT_BODY = new ChildPropertyDescriptor(
      DartForInStatement.class,
      "body",
      DartStatement.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression that computes the iterable object being looped over.
   */
  public static final StructuralPropertyDescriptor DART_FOR_IN_STATEMENT_ITERABLE = new ChildPropertyDescriptor(
      DartForInStatement.class,
      "iterable",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The variable of the loop.
   */
  public static final StructuralPropertyDescriptor DART_FOR_IN_STATEMENT_VARIABLE = new ChildPropertyDescriptor(
      DartForInStatement.class,
      "variable",
      DartVariableStatement.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The body of the loop.
   */
  public static final StructuralPropertyDescriptor DART_FOR_STATEMENT_BODY = new ChildPropertyDescriptor(
      DartForStatement.class,
      "body",
      DartStatement.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression evaluated to determine whether the body should be executed.
   */
  public static final StructuralPropertyDescriptor DART_FOR_STATEMENT_CONDITION = new ChildPropertyDescriptor(
      DartForStatement.class,
      "condition",
      DartExpression.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression evaluated after the execution of the body.
   */
  public static final StructuralPropertyDescriptor DART_FOR_STATEMENT_INCREMENT = new ChildPropertyDescriptor(
      DartForStatement.class,
      "increment",
      DartExpression.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The statement executed before executing the condition for the first time.
   */
  public static final StructuralPropertyDescriptor DART_FOR_STATEMENT_INIT = new ChildPropertyDescriptor(
      DartForStatement.class,
      "init",
      DartStatement.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The body of the function.
   */
  public static final StructuralPropertyDescriptor DART_FUNCTION_BODY = new ChildPropertyDescriptor(
      DartFunction.class,
      "body",
      DartStatement.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The list of the parameters of the function being defined.
   */
  public static final StructuralPropertyDescriptor DART_FUNCTION_PARAMETERS = new ChildListPropertyDescriptor(
      DartFunction.class,
      "parameters",
      DartParameter.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The body of the function.
   */
  public static final StructuralPropertyDescriptor DART_FUNCTION_RETURN_TYPE = new ChildPropertyDescriptor(
      DartFunction.class,
      "returnTypeNode",
      DartTypeNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The body of the function being defined.
   */
  public static final StructuralPropertyDescriptor DART_FUNCTION_EXPRESSION_FUNCTION = new ChildPropertyDescriptor(
      DartFunctionExpression.class,
      "function",
      DartFunction.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * A flag indicating whether this expression is a statement.
   */
  public static final StructuralPropertyDescriptor DART_FUNCTION_EXPRESSION_IS_STATEMENT = new SimplePropertyDescriptor(
      DartFunctionExpression.class,
      "isStmt",
      boolean.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The name of the function being defined.
   */
  public static final StructuralPropertyDescriptor DART_FUNCTION_EXPRESSION_NAME = new ChildPropertyDescriptor(
      DartFunctionExpression.class,
      "name",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The function object being invoked.
   */
  public static final StructuralPropertyDescriptor DART_FUNCTION_OBJECT_INVOCATION_TARGET = new ChildPropertyDescriptor(
      DartFunctionObjectInvocation.class,
      "target",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The name of the function.
   */
  public static final StructuralPropertyDescriptor DART_FUNCTION_TYPE_ALIAS_NAME = new ChildPropertyDescriptor(
      DartFunctionTypeAlias.class,
      "name",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The return type of the function.
   */
  public static final StructuralPropertyDescriptor DART_FUNCTION_TYPE_ALIAS_RETURN_TYPE = new ChildPropertyDescriptor(
      DartFunctionTypeAlias.class,
      "returnTypeNode",
      DartTypeNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The parameters of the function.
   */
  public static final StructuralPropertyDescriptor DART_FUNCTION_TYPE_ALIAS_PARAMETERS = new ChildListPropertyDescriptor(
      DartFunctionTypeAlias.class,
      "parameters",
      DartParameter.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The label of a break or continue statement.
   */
  public static final StructuralPropertyDescriptor DART_GOTO_STATEMENT_LABEL = new SimplePropertyDescriptor(
      DartGotoStatement.class,
      "label",
      String.class,
      StructuralPropertyDescriptor.OPTIONAL);

  /**
   * The target name of an identifier.
   */
  public static final StructuralPropertyDescriptor DART_IDENTIFIER_TARGET_NAME = new SimplePropertyDescriptor(
      DartIdentifier.class,
      "targetName",
      String.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The condition that will be evaluated to choose between the else and then statements.
   */
  public static final StructuralPropertyDescriptor DART_IF_STATEMENT_CONDITION = new ChildPropertyDescriptor(
      DartIfStatement.class,
      "condition",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The statement that will be executed if the condition evaluates to false.
   */
  public static final StructuralPropertyDescriptor DART_IF_STATEMENT_ELSE = new ChildPropertyDescriptor(
      DartIfStatement.class,
      "elseStmt",
      DartStatement.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The statement that will be executed if the condition evaluates to true.
   */
  public static final StructuralPropertyDescriptor DART_IF_STATEMENT_THEN = new ChildPropertyDescriptor(
      DartIfStatement.class,
      "thenStmt",
      DartStatement.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The prefix used when referencing names defined in the library being imported.
   */
  public static final StructuralPropertyDescriptor DART_IMPORT_DIRECTIVE_PREFIX = new ChildPropertyDescriptor(
      DartImportDirective.class,
      "prefix",
      DartStringLiteral.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The URI of the library being imported.
   */
  public static final StructuralPropertyDescriptor DART_IMPORT_DIRECTIVE_URI = new ChildPropertyDescriptor(
      DartImportDirective.class,
      "libraryUri",
      DartStringLiteral.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The name of the field being initialized.
   */
  public static final StructuralPropertyDescriptor DART_INITIALIZER_NAME = new ChildPropertyDescriptor(
      DartInitializer.class,
      "name",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The value to which the field is to be initialized.
   */
  public static final StructuralPropertyDescriptor DART_INITIALIZER_VALUE = new ChildPropertyDescriptor(
      DartInitializer.class,
      "value",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The value of the literal.
   */
  public static final StructuralPropertyDescriptor DART_INTEGER_LITERAL_VALUE = new SimplePropertyDescriptor(
      DartIntegerLiteral.class,
      "value",
      BigInteger.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The arguments passed to the invocation.
   */
  public static final StructuralPropertyDescriptor DART_INVOCATION_ARGS = new ChildListPropertyDescriptor(
      DartInvocation.class,
      "args",
      DartExpression.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The label of the labeled statement.
   */
  public static final StructuralPropertyDescriptor DART_LABELED_STATEMENT_LABEL = new ChildPropertyDescriptor(
      DartLabel.class,
      "label",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The statement of the labeled statement.
   */
  public static final StructuralPropertyDescriptor DART_LABELED_STATEMENT_STATEMENT = new ChildPropertyDescriptor(
      DartLabel.class,
      "statement",
      DartStatement.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The name of the library being defined.
   */
  public static final StructuralPropertyDescriptor DART_LIBRARY_DIRECTIVE_NAME = new ChildPropertyDescriptor(
      DartLibraryDirective.class,
      "name",
      DartStringLiteral.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The list of the entries in the map being defined.
   */
  public static final StructuralPropertyDescriptor DART_MAP_LITERAL_ENTRIES = new ChildListPropertyDescriptor(
      DartMapLiteral.class,
      "entries",
      DartMapLiteralEntry.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The key of the entry.
   */
  public static final StructuralPropertyDescriptor DART_MAP_LITERAL_ENTRY_KEY = new ChildPropertyDescriptor(
      DartMapLiteralEntry.class,
      "key",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The value of the entry.
   */
  public static final StructuralPropertyDescriptor DART_MAP_LITERAL_ENTRY_VALUE = new ChildPropertyDescriptor(
      DartMapLiteralEntry.class,
      "value",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The function associated with the method.
   */
  public static final StructuralPropertyDescriptor DART_METHOD_DEFINITION_FUNCTION = new ChildPropertyDescriptor(
      DartMethodDefinition.class,
      "function",
      DartFunction.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The list of members of the class or interface being defined.
   */
  public static final StructuralPropertyDescriptor DART_METHOD_DEFINITION_INITIALIZERS = new ChildListPropertyDescriptor(
      DartMethodDefinition.class,
      "initializers",
      DartInitializer.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The name of the method or function being called.
   */
  public static final StructuralPropertyDescriptor DART_METHOD_INVOCATION_FUNCTION_NAME = new ChildPropertyDescriptor(
      DartMethodInvocation.class,
      "functionName",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The expression computing the target of the method or function call.
   */
  public static final StructuralPropertyDescriptor DART_METHOD_INVOCATION_TARGET = new ChildPropertyDescriptor(
      DartMethodInvocation.class,
      "target",
      DartExpression.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression that is being named.
   */
  public static final StructuralPropertyDescriptor DART_NAMED_EXPRESSION_EXPRESSION = new ChildPropertyDescriptor(
      DartNamedExpression.class,
      "expression",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The name of the expression.
   */
  public static final StructuralPropertyDescriptor DART_NAMED_EXPRESSION_NAME = new ChildPropertyDescriptor(
      DartNamedExpression.class,
      "name",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The name of the constructor being invoked.
   */
  public static final StructuralPropertyDescriptor DART_NEW_EXPRESSION_CONSTRUCTOR_NAME = new ChildPropertyDescriptor(
      DartNewExpression.class,
      "constructor",
      DartNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The URI of the native file being referenced.
   */
  public static final StructuralPropertyDescriptor DART_NATIVE_DIRECTIVE_URI = new ChildPropertyDescriptor(
      DartNativeDirective.class,
      "nativeUri",
      DartStringLiteral.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The arguments to the constructor being invoked.
   */
  public static final StructuralPropertyDescriptor DART_NEW_EXPRESSION_ARGUMENTS = new ChildListPropertyDescriptor(
      DartNewExpression.class,
      "args",
      DartExpression.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * 
   */
  public static final StructuralPropertyDescriptor DART_PARAMETER_DEFAULT_EXPRESSION = new ChildPropertyDescriptor(
      DartParameter.class,
      "defaultExpr",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * 
   */
  public static final StructuralPropertyDescriptor DART_PARAMETER_FUNCTION_PARAMETERS = new ChildListPropertyDescriptor(
      DartParameter.class,
      "functionParameters",
      DartParameter.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * A flag indicating whether the parameter is const.
   */
  public static final StructuralPropertyDescriptor DART_PARAMETER_IS_CONST = new SimplePropertyDescriptor(
      DartParameter.class,
      "isConst",
      boolean.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The name of the parameter being defined.
   */
  public static final StructuralPropertyDescriptor DART_PARAMETER_NAME = new ChildPropertyDescriptor(
      DartParameter.class,
      "name",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The name of the type of the parameter being defined.
   */
  public static final StructuralPropertyDescriptor DART_PARAMETER_TYPE_NAME = new ChildPropertyDescriptor(
      DartParameter.class,
      "typeNode",
      DartTypeNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The expression being parameterized.
   */
  public static final StructuralPropertyDescriptor DART_PARAMETERIZED_NODE_EXPRESSION = new ChildPropertyDescriptor(
      DartParameterizedTypeNode.class,
      "expression",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The type parameters used to parameterize the expression.
   */
  public static final StructuralPropertyDescriptor DART_PARAMETERIZED_NODE_TYPE_PARAMETERS = new ChildListPropertyDescriptor(
      DartParameterizedTypeNode.class,
      "typeParameters",
      DartTypeParameter.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression within the parentheses.
   */
  public static final StructuralPropertyDescriptor DART_PARENTHESIZED_EXPRESSION_EXPRESSION = new ChildPropertyDescriptor(
      DartParenthesizedExpression.class,
      "expression",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression used to compute the object containing the property.
   */
  public static final StructuralPropertyDescriptor DART_PROPERTY_ACCESS_QUALIFIER = new ChildPropertyDescriptor(
      DartPropertyAccess.class,
      "qualifier",
      DartNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The name of the property being accessed.
   */
  public static final StructuralPropertyDescriptor DART_PROPERTY_ACCESS_NAME = new ChildPropertyDescriptor(
      DartPropertyAccess.class,
      "name",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The name of the constructor being invoked.
   */
  public static final StructuralPropertyDescriptor DART_REDIRECT_CONSTRUCTOR_INVOCATION_NAME = new ChildPropertyDescriptor(
      DartRedirectConstructorInvocation.class,
      "name",
      DartInitializer.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The URI of the resource being referenced.
   */
  public static final StructuralPropertyDescriptor DART_RESOURCE_DIRECTIVE_URI = new ChildPropertyDescriptor(
      DartResourceDirective.class,
      "resourceUri",
      DartStringLiteral.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The expression computing the value to be returned by a return statement.
   */
  public static final StructuralPropertyDescriptor DART_RETURN_STATEMENT_VALUE = new ChildPropertyDescriptor(
      DartReturnStatement.class,
      "value",
      DartExpression.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The URI of the source being included.
   */
  public static final StructuralPropertyDescriptor DART_SOURCE_DIRECTIVE_URI = new ChildPropertyDescriptor(
      DartSourceDirective.class,
      "sourceUri",
      DartStringLiteral.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * 
   */
  public static final StructuralPropertyDescriptor DART_STRING_INTERPOLATION_EXPRESSIONS = new ChildListPropertyDescriptor(
      DartStringInterpolation.class,
      "expressions",
      DartExpression.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * 
   */
  public static final StructuralPropertyDescriptor DART_STRING_INTERPOLATION_STRINGS = new ChildListPropertyDescriptor(
      DartStringInterpolation.class,
      "strings",
      DartStringLiteral.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The value of the literal.
   */
  public static final StructuralPropertyDescriptor DART_STRING_LITERAL_VALUE = new SimplePropertyDescriptor(
      DartStringLiteral.class,
      "value",
      String.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The name of the constructor to invoke.
   */
  public static final StructuralPropertyDescriptor DART_SUPER_CONSTRUCTOR_INVOCATION_NAME = new ChildPropertyDescriptor(
      DartSuperConstructorInvocation.class,
      "name",
      DartIdentifier.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The label associated with the switch member.
   */
  public static final StructuralPropertyDescriptor DART_SWITCH_MEMBER_LABEL = new ChildPropertyDescriptor(
      DartSwitchMember.class,
      "label",
      DartLabel.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The statements associated with the switch member.
   */
  public static final StructuralPropertyDescriptor DART_SWITCH_MEMBER_STATEMENTS = new ChildListPropertyDescriptor(
      DartSwitchMember.class,
      "statements",
      DartStatement.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The expression controlling which member will be executed.
   */
  public static final StructuralPropertyDescriptor DART_SWITCH_STATEMENT_EXPRESSION = new ChildPropertyDescriptor(
      DartSwitchStatement.class,
      "expression",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The members in the switch statement.
   */
  public static final StructuralPropertyDescriptor DART_SWITCH_STATEMENT_MEMBERS = new ChildListPropertyDescriptor(
      DartSwitchStatement.class,
      "members",
      DartSwitchMember.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression creating the exception to be thrown.
   */
  public static final StructuralPropertyDescriptor DART_THROW_STATEMENT_EXCEPTION = new ChildPropertyDescriptor(
      DartThrowStatement.class,
      "exception",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The string representing the invalid content of the source file.
   */
  public static final StructuralPropertyDescriptor DART_SYNTHETIC_ERROR_EXPRESSION_TOKEN_STRING = new SimplePropertyDescriptor(
      DartSyntheticErrorExpression.class,
      "tokenString",
      String.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The string representing the invalid content of the source file.
   */
  public static final StructuralPropertyDescriptor DART_SYNTHETIC_ERROR_STATEMENT_TOKEN_STRING = new SimplePropertyDescriptor(
      DartSyntheticErrorStatement.class,
      "tokenString",
      String.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The catch blocks controlling which exceptions (if any) will be handled.
   */
  public static final StructuralPropertyDescriptor DART_TRY_STATEMENT_CATCH_BLOCKS = new ChildListPropertyDescriptor(
      DartTryStatement.class,
      "catchBlocks",
      DartCatchBlock.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The block to be executed after the try block even if an exception is thrown.
   */
  public static final StructuralPropertyDescriptor DART_TRY_STATEMENT_FINALY_BLOCK = new ChildPropertyDescriptor(
      DartTryStatement.class,
      "finallyBlock",
      DartBlock.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The block being protected by the try statement.
   */
  public static final StructuralPropertyDescriptor DART_TRY_STATEMENT_TRY_BLOCK = new ChildPropertyDescriptor(
      DartTryStatement.class,
      "tryBlock",
      DartBlock.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The type arguments associated with the type.
   */
  public static final StructuralPropertyDescriptor DART_TYPE_TYPE_ARGUMENTS = new ChildListPropertyDescriptor(
      DartTypeNode.class,
      "typeArguments",
      DartTypeNode.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The name of the type.
   */
  public static final StructuralPropertyDescriptor DART_TYPE_IDENTIFIER = new ChildPropertyDescriptor(
      DartTypeNode.class,
      "identifier",
      DartNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The type being represented as an expression.
   */
  public static final StructuralPropertyDescriptor DART_TYPE_EXPRESSION_TYPE = new ChildPropertyDescriptor(
      DartTypeExpression.class,
      "typeNode",
      DartTypeNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The bound applied to the type parameter.
   */
  public static final StructuralPropertyDescriptor DART_TYPE_PARAMETER_BOUND = new ChildPropertyDescriptor(
      DartTypeParameter.class,
      "bound",
      DartTypeNode.class,
      StructuralPropertyDescriptor.OPTIONAL,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The name of the type parameter.
   */
  public static final StructuralPropertyDescriptor DART_TYPE_PARAMETER_NAME = new ChildPropertyDescriptor(
      DartTypeParameter.class,
      "name",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * A flag indicating whether this is a prefix operator.
   */
  public static final StructuralPropertyDescriptor DART_UNARY_EXPRESSION_IS_PREFIX = new SimplePropertyDescriptor(
      DartUnaryExpression.class,
      "isPrefix",
      boolean.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The operand of the expression.
   */
  public static final StructuralPropertyDescriptor DART_UNARY_EXPRESSION_OPERAND = new ChildPropertyDescriptor(
      DartUnaryExpression.class,
      "arg",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The operator of the expression.
   */
  public static final StructuralPropertyDescriptor DART_UNARY_EXPRESSION_OPERATOR = new SimplePropertyDescriptor(
      DartUnaryExpression.class,
      "operator",
      Token.class,
      StructuralPropertyDescriptor.MANDATORY);

  /**
   * The comments defined within the compilation unit.
   */
  public static final StructuralPropertyDescriptor DART_UNIT_COMMENTS = new ChildListPropertyDescriptor(
      DartUnit.class,
      "comments",
      DartComment.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The types defined within the compilation unit.
   */
  public static final StructuralPropertyDescriptor DART_UNIT_MEMBERS = new ChildListPropertyDescriptor(
      DartUnit.class,
      "members",
      DartNode.class,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The target of the invocation.
   */
  public static final StructuralPropertyDescriptor DART_UNQUALIFIED_INVOCATION_TARGET = new ChildPropertyDescriptor(
      DartUnqualifiedInvocation.class,
      "target",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The name of the variable.
   */
  public static final StructuralPropertyDescriptor DART_VARIABLE_NAME = new ChildPropertyDescriptor(
      DartVariable.class,
      "name",
      DartIdentifier.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.NO_CYCLE_RISK);

  /**
   * The expression computing the initial value of the variable.
   */
  public static final StructuralPropertyDescriptor DART_VARIABLE_VALUE = new ChildPropertyDescriptor(
      DartVariable.class,
      "value",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The type of the variables defined in this statement.
   */
  public static final StructuralPropertyDescriptor DART_VARIABLE_STATEMENT_TYPE = new ChildPropertyDescriptor(
      DartVariableStatement.class,
      "typeNode",
      DartTypeNode.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The variables defined in this statement.
   */
  public static final StructuralPropertyDescriptor DART_VARIABLE_STATEMENT_VARIABLES = new ChildListPropertyDescriptor(
      DartVariableStatement.class,
      "vars",
      DartVariable.class,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The body of the while statement.
   */
  public static final StructuralPropertyDescriptor DART_WHILE_STATEMENT_BODY = new ChildPropertyDescriptor(
      DartWhileStatement.class,
      "body",
      DartStatement.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * The expression evaluated to determine whether the body will be evaluated.
   */
  public static final StructuralPropertyDescriptor DART_WHILE_STATEMENT_CONDITION = new ChildPropertyDescriptor(
      DartWhileStatement.class,
      "condition",
      DartExpression.class,
      StructuralPropertyDescriptor.MANDATORY,
      StructuralPropertyDescriptor.CYCLE_RISK);

  /**
   * Return the descriptor for the property that defines the given node's location in it's parent
   * node.
   * 
   * @param node the node whose location is to be returned
   * @return the location of the given node in its parent
   */
  public static StructuralPropertyDescriptor getLocationInParent(DartNode node) {
    if (node == null) {
      return null;
    }
    DartNode parent = node.getParent();
    if (parent == null) {
      return null;
    }
    PropertyLocator locator = new PropertyLocator(node);
    return parent.accept(locator);
  }

  /**
   * Return a list containing the structural properties defined by the given class of node.
   * 
   * @param nodeClass the node class for which properties are to be returned
   * @return a list containing the structural properties defined by the given class of node
   */
  public static List<StructuralPropertyDescriptor> getStructuralPropertiesForType(
      Class<? extends DartNode> nodeClass) {
    List<StructuralPropertyDescriptor> properties = new ArrayList<StructuralPropertyDescriptor>();
    Class<?> type = nodeClass;
    while (type != DartNode.class) {
      if (type == DartArrayAccess.class) {
        properties.add(DART_ARRAY_ACCESS_KEY);
        properties.add(DART_ARRAY_ACCESS_TARGET);
      } else if (type == DartArrayLiteral.class) {
        properties.add(DART_ARRAY_LITERAL_EXPRESSIONS);
      } else if (type == DartAssertion.class) {
        properties.add(DART_ASSERTION_EXPRESSION);
      } else if (type == DartBinaryExpression.class) {
        properties.add(DART_BINARY_EXPRESSION_LEFT_OPERAND);
        properties.add(DART_BINARY_EXPRESSION_OPERATOR);
        properties.add(DART_BINARY_EXPRESSION_RIGHT_OPERAND);
      } else if (type == DartBlock.class) {
        properties.add(DART_BLOCK_STATEMENTS);
      } else if (type == DartBooleanLiteral.class) {
        properties.add(DART_BOOLEAN_LITERAL_VALUE);
      } else if (type == DartBreakStatement.class) {
        // Nothing added
      } else if (type == DartCase.class) {
        properties.add(DART_CASE_EXPRESSION);
        properties.add(DART_CASE_LABEL);
        properties.add(DART_CASE_STATEMENTS);
      } else if (type == DartCatchBlock.class) {
        properties.add(DART_CATCH_BLOCK_EXCEPTION);
        properties.add(DART_CATCH_BLOCK_STACK_TRACE);
        properties.add(DART_CATCH_BLOCK_BODY);
      } else if (type == DartClass.class) {
        properties.add(DART_CLASS_DEFAULT_CLASS);
        properties.add(DART_CLASS_INTERFACES);
        properties.add(DART_CLASS_IS_INTERFACE);
        properties.add(DART_CLASS_MEMBERS);
        properties.add(DART_CLASS_NAME);
        properties.add(DART_CLASS_SUPERCLASS);
        properties.add(DART_CLASS_TYPE_PARAMETERS);
      } else if (type == DartClassMember.class) {
        properties.add(DART_CLASS_MEMBER_MODIFIERS);
        properties.add(DART_CLASS_MEMBER_NAME);
      } else if (type == DartComment.class) {
        // Nothing added
      } else if (type == DartConditional.class) {
        properties.add(DART_CONDITIONAL_CONDITION);
        properties.add(DART_CONDITIONAL_ELSE);
        properties.add(DART_CONDITIONAL_THEN);
      } else if (type == DartContinueStatement.class) {
        // Nothing added
      } else if (type == DartDeclaration.class) {
        DartCore.notYetImplemented();
        // I'm not sure how to represent this because the type of the name is
        // a type parameter. We might have to ignore the existence of this
        // class.
        // properties.add(DART_DECLARATION_NAME);
      } else if (type == DartDefault.class) {
        // Nothing added
      } else if (type == DartDoubleLiteral.class) {
        properties.add(DART_DOUBLE_LITERAL_VALUE);
      } else if (type == DartDoWhileStatement.class) {
        properties.add(DART_DO_WHILE_STATEMENT_BODY);
        properties.add(DART_DO_WHILE_STATEMENT_CONDITION);
      } else if (type == DartEmptyStatement.class) {
        // Nothing added
      } else if (type == DartExpression.class) {
        // Nothing added
      } else if (type == DartExprStmt.class) {
        properties.add(DART_EXPRESSION_STATEMENT_EXPRESSION);
      } else if (type == DartField.class) {
        properties.add(DART_FIELD_VALUE);
      } else if (type == DartFieldDefinition.class) {
        properties.add(DART_FIELD_DEFINITION_FIELDS);
        properties.add(DART_FIELD_DEFINITION_TYPE);
      } else if (type == DartForStatement.class) {
        properties.add(DART_FOR_STATEMENT_BODY);
        properties.add(DART_FOR_STATEMENT_CONDITION);
        properties.add(DART_FOR_STATEMENT_INCREMENT);
        properties.add(DART_FOR_STATEMENT_INIT);
      } else if (type == DartFunction.class) {
        properties.add(DART_FUNCTION_BODY);
        properties.add(DART_FUNCTION_PARAMETERS);
        properties.add(DART_FUNCTION_RETURN_TYPE);
      } else if (type == DartFunctionExpression.class) {
        properties.add(DART_FUNCTION_EXPRESSION_FUNCTION);
        properties.add(DART_FUNCTION_EXPRESSION_IS_STATEMENT);
        properties.add(DART_FUNCTION_EXPRESSION_NAME);
      } else if (type == DartFunctionObjectInvocation.class) {
        DartCore.notYetImplemented();
      } else if (type == DartGotoStatement.class) {
        properties.add(DART_GOTO_STATEMENT_LABEL);
      } else if (type == DartIdentifier.class) {
        properties.add(DART_IDENTIFIER_TARGET_NAME);
      } else if (type == DartIfStatement.class) {
        properties.add(DART_IF_STATEMENT_CONDITION);
        properties.add(DART_IF_STATEMENT_ELSE);
        properties.add(DART_IF_STATEMENT_THEN);
      } else if (type == DartInitializer.class) {
        properties.add(DART_INITIALIZER_NAME);
        properties.add(DART_INITIALIZER_VALUE);
      } else if (type == DartIntegerLiteral.class) {
        properties.add(DART_INTEGER_LITERAL_VALUE);
      } else if (type == DartInvocation.class) {
        properties.add(DART_INVOCATION_ARGS);
      } else if (type == DartLabel.class) {
        properties.add(DART_LABELED_STATEMENT_LABEL);
        properties.add(DART_LABELED_STATEMENT_STATEMENT);
      } else if (type == DartLiteral.class) {
        // Nothing added
      } else if (type == DartMapLiteral.class) {
        properties.add(DART_MAP_LITERAL_ENTRIES);
      } else if (type == DartMapLiteralEntry.class) {
        properties.add(DART_MAP_LITERAL_ENTRY_KEY);
        properties.add(DART_MAP_LITERAL_ENTRY_VALUE);
      } else if (type == DartMethodDefinition.class) {
        properties.add(DART_METHOD_DEFINITION_FUNCTION);
        properties.add(DART_METHOD_DEFINITION_INITIALIZERS);
      } else if (type == DartMethodInvocation.class) {
        properties.add(DART_METHOD_INVOCATION_TARGET);
        properties.add(DART_METHOD_INVOCATION_FUNCTION_NAME);
      } else if (type == DartNativeBlock.class) {
        // Nothing added
      } else if (type == DartNewExpression.class) {
        properties.add(DART_NEW_EXPRESSION_CONSTRUCTOR_NAME);
        properties.add(DART_NEW_EXPRESSION_ARGUMENTS);
      } else if (type == DartNode.class) {
        // Nothing added
      } else if (type == DartNullLiteral.class) {
        // Nothing added
      } else if (type == DartParameter.class) {
        properties.add(DART_PARAMETER_DEFAULT_EXPRESSION);
        properties.add(DART_PARAMETER_FUNCTION_PARAMETERS);
        properties.add(DART_PARAMETER_IS_CONST);
        properties.add(DART_PARAMETER_NAME);
        properties.add(DART_PARAMETER_TYPE_NAME);
      } else if (type == DartParenthesizedExpression.class) {
        properties.add(DART_PARENTHESIZED_EXPRESSION_EXPRESSION);
      } else if (type == DartPropertyAccess.class) {
        properties.add(DART_PROPERTY_ACCESS_QUALIFIER);
        properties.add(DART_PROPERTY_ACCESS_NAME);
      } else if (type == DartReturnStatement.class) {
        properties.add(DART_RETURN_STATEMENT_VALUE);
      } else if (type == DartStatement.class) {
        // Nothing added
      } else if (type == DartStringInterpolation.class) {
        properties.add(DART_STRING_INTERPOLATION_EXPRESSIONS);
        properties.add(DART_STRING_INTERPOLATION_STRINGS);
      } else if (type == DartStringLiteral.class) {
        properties.add(DART_STRING_LITERAL_VALUE);
      } else if (type == DartSuperConstructorInvocation.class) {
        properties.add(DART_SUPER_CONSTRUCTOR_INVOCATION_NAME);
      } else if (type == DartSuperExpression.class) {
        // Nothing added
      } else if (type == DartSwitchMember.class) {
        properties.add(DART_SWITCH_MEMBER_LABEL);
        properties.add(DART_SWITCH_MEMBER_STATEMENTS);
      } else if (type == DartSwitchStatement.class) {
        properties.add(DART_SWITCH_STATEMENT_EXPRESSION);
        properties.add(DART_SWITCH_STATEMENT_MEMBERS);
      } else if (type == DartThisExpression.class) {
        // Nothing added
      } else if (type == DartThrowStatement.class) {
        properties.add(DART_THROW_STATEMENT_EXCEPTION);
      } else if (type == DartTryStatement.class) {
        properties.add(DART_TRY_STATEMENT_CATCH_BLOCKS);
        properties.add(DART_TRY_STATEMENT_FINALY_BLOCK);
        properties.add(DART_TRY_STATEMENT_TRY_BLOCK);
      } else if (type == DartTypeNode.class) {
        properties.add(DART_TYPE_TYPE_ARGUMENTS);
        properties.add(DART_TYPE_IDENTIFIER);
      } else if (type == DartTypeExpression.class) {
        properties.add(DART_TYPE_EXPRESSION_TYPE);
      } else if (type == DartTypeParameter.class) {
        properties.add(DART_TYPE_PARAMETER_BOUND);
        properties.add(DART_TYPE_PARAMETER_NAME);
      } else if (type == DartUnaryExpression.class) {
        properties.add(DART_UNARY_EXPRESSION_IS_PREFIX);
        properties.add(DART_UNARY_EXPRESSION_OPERAND);
        properties.add(DART_UNARY_EXPRESSION_OPERATOR);
      } else if (type == DartUnit.class) {
        properties.add(DART_UNIT_COMMENTS);
        properties.add(DART_UNIT_MEMBERS);
      } else if (type == DartUnqualifiedInvocation.class) {
        properties.add(DART_UNQUALIFIED_INVOCATION_TARGET);
      } else if (type == DartVariable.class) {
        properties.add(DART_VARIABLE_NAME);
        properties.add(DART_VARIABLE_VALUE);
      } else if (type == DartVariableStatement.class) {
        properties.add(DART_VARIABLE_STATEMENT_TYPE);
        properties.add(DART_VARIABLE_STATEMENT_VARIABLES);
      } else if (type == DartWhileStatement.class) {
        properties.add(DART_WHILE_STATEMENT_BODY);
        properties.add(DART_WHILE_STATEMENT_CONDITION);
      } else {
        throw new IllegalArgumentException("Unknown subclass of DartNode: " + type.getName());
      }
      type = type.getSuperclass();
    }
    return properties;
  }

  /**
   * Return a list containing the structural properties defined for the given node.
   * 
   * @param node the node for which properties are to be returned
   * @return a list containing the structural properties defined for the given node
   */
  public static List<StructuralPropertyDescriptor> getStructuralPropertiesForType(DartNode node) {
    return getStructuralPropertiesForType(node.getClass());
  }

  /**
   * Return the value of the given property of the given node.
   * 
   * @param node the node whose property is to be accessed
   * @param property the property whose value is to be returned
   * @return the value of the given property for the given node
   * @throws RuntimeException if the node does not have the given property or if the value of the
   *           property cannot be accessed
   */
  public static Object getStructuralProperty(DartNode node, StructuralPropertyDescriptor property) {
    PropertyGetter getter = new PropertyGetter(property);
    return node.accept(getter);
  }

  /**
   * Set the value of the given property of the given node to the given value.
   * 
   * @param node the node whose property is to be set
   * @param property the property whose value is to be set
   * @param newValue the new value of the property
   * @throws RuntimeException if the node does not have the given property or if the property cannot
   *           be set to the given value
   */
  public static void setStructuralProperty(DartNode node, StructuralPropertyDescriptor property,
      Object newValue) {
    PropertySetter setter = new PropertySetter(property, newValue);
    node.accept(setter);
  }
}
