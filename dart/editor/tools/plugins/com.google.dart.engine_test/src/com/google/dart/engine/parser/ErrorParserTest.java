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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypedLiteral;
import com.google.dart.engine.scanner.Token;

/**
 * The class {@code ErrorParserTest} defines parser tests that test the parsing of code to ensure
 * that errors are correctly reported, and in some cases, not reported.
 */
public class ErrorParserTest extends ParserTestCase {
  public void fail_expectedListOrMapLiteral() throws Exception {
    TypedLiteral literal = parse(
        "parseListOrMapLiteral",
        new Class[] {Token.class},
        new Object[] {null},
        "1",
        ParserErrorCode.EXPECTED_LIST_OR_MAP_LITERAL);
    assertTrue(literal.isSynthetic());
  }

  public void test_breakOutsideOfLoop_breakInDoStatement() throws Exception {
    parse("parseDoStatement", "do {break;} while (x);");
  }

  public void test_breakOutsideOfLoop_breakInForStatement() throws Exception {
    parse("parseForStatement", "for (; x;) {break;}");
  }

  public void test_breakOutsideOfLoop_breakInIfStatement() throws Exception {
    parse("parseIfStatement", "if (x) {break;}", ParserErrorCode.BREAK_OUTSIDE_OF_LOOP);
  }

  public void test_breakOutsideOfLoop_breakInSwitchStatement() throws Exception {
    parse("parseSwitchStatement", "switch (x) {case 1: break;}");
  }

  public void test_breakOutsideOfLoop_breakInWhileStatement() throws Exception {
    parse("parseWhileStatement", "while (x) {break;}");
  }

  public void test_breakOutsideOfLoop_functionExpression_inALoop() throws Exception {
    parse("parseStatement", "for(; x;) {() {break;}}", ParserErrorCode.BREAK_OUTSIDE_OF_LOOP);
  }

  public void test_breakOutsideOfLoop_functionExpression_withALoop() throws Exception {
    parse("parseStatement", "() {for (; x;) {break;}}");
  }

  public void test_builtInIdentifierAsFunctionName_constConstructor() throws Exception {
    parse(
        "parseClassMember",
        "const C.as() {}",
        ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
  }

  public void test_builtInIdentifierAsFunctionName_constructor() throws Exception {
    parse("parseClassMember", "C.as() {}", ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
  }

  public void test_builtInIdentifierAsFunctionName_functionExpression() throws Exception {
    parse(
        "parseFunctionExpression",
        "as() {}",
        ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
  }

  public void test_builtInIdentifierAsFunctionName_getter() throws Exception {
    parse("parseClassMember", "get as {}", ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
  }

  public void test_builtInIdentifierAsFunctionName_method() throws Exception {
    parse("parseClassMember", "void as() {}", ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
  }

  public void test_builtInIdentifierAsFunctionName_setter() throws Exception {
    parse("parseClassMember", "set as(v) {}", ParserErrorCode.BUILT_IN_IDENTIFIER_AS_FUNCTION_NAME);
  }

  public void test_builtInIdentifierAsLabel_statement() throws Exception {
    parse("parseStatement", "as: m();", ParserErrorCode.BUILT_IN_IDENTIFIER_AS_LABEL);
  }

  public void test_builtInIdentifierAsLabel_switchMember() throws Exception {
    parse(
        "parseSwitchStatement",
        "switch (e) {as: case 0: break;}",
        ParserErrorCode.BUILT_IN_IDENTIFIER_AS_LABEL);
  }

  public void test_builtInIdentifierAsTypeDefName() throws Exception {
    parse("parseTypeAlias", "typedef as();", ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
  }

  public void test_builtInIdentifierAsTypeName() throws Exception {
    parse("parseClassDeclaration", "class as {}", ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME);
  }

  public void test_builtInIdentifierAsTypeVariableName() throws Exception {
    parse("parseTypeParameter", "as", ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME);
  }

  public void test_builtInIdentifierAsVariableName_for() throws Exception {
    parse(
        "parseForStatement",
        "for (as in list) {}",
        ParserErrorCode.BUILT_IN_IDENTIFIER_AS_VARIABLE_NAME);
  }

  public void test_builtInIdentifierAsVariableName_variable() throws Exception {
    parse("parseVariableDeclaration", "as", ParserErrorCode.BUILT_IN_IDENTIFIER_AS_VARIABLE_NAME);
  }

  public void test_catchOrFinallyExpected() throws Exception {
    TryStatement statement = parse(
        "parseTryStatement",
        "try {}",
        ParserErrorCode.CATCH_OR_FINALLY_EXPECTED);
    assertNotNull(statement);
  }

  public void test_continueInCaseMustHaveLabel_error() throws Exception {
    parse(
        "parseSwitchStatement",
        "switch (x) {case 1: continue;}",
        ParserErrorCode.CONTINUE_IN_CASE_MUST_HAVE_LABEL);
  }

  public void test_continueInCaseMustHaveLabel_noError() throws Exception {
    parse("parseSwitchStatement", "switch (x) {case 1: continue a;}");
  }

  public void test_continueInCaseMustHaveLabel_noError_switchInLoop() throws Exception {
    parse("parseWhileStatement", "while (a) { switch (b) {default: continue;}}");
  }

  public void test_continueOutsideOfLoop_continueInDoStatement() throws Exception {
    parse("parseDoStatement", "do {continue;} while (x);");
  }

  public void test_continueOutsideOfLoop_continueInForStatement() throws Exception {
    parse("parseForStatement", "for (; x;) {continue;}");
  }

  public void test_continueOutsideOfLoop_continueInIfStatement() throws Exception {
    parse("parseIfStatement", "if (x) {continue;}", ParserErrorCode.CONTINUE_OUTSIDE_OF_LOOP);
  }

  public void test_continueOutsideOfLoop_continueInSwitchStatement() throws Exception {
    parse("parseSwitchStatement", "switch (x) {case 1: continue a;}");
  }

  public void test_continueOutsideOfLoop_continueInWhileStatement() throws Exception {
    parse("parseWhileStatement", "while (x) {continue;}");
  }

  public void test_continueOutsideOfLoop_functionExpression_inALoop() throws Exception {
    parse("parseStatement", "for(; x;) {() {continue;}}", ParserErrorCode.CONTINUE_OUTSIDE_OF_LOOP);
  }

  public void test_continueOutsideOfLoop_functionExpression_withALoop() throws Exception {
    parse("parseStatement", "() {for (; x;) {continue;}}");
  }

  public void test_directiveOutOfOrder_classBeforeDirective() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "class Foo{}\nlibrary l;",
        ParserErrorCode.DIRECTIVE_OUT_OF_ORDER);
    assertNotNull(unit);
  }

  public void test_directiveOutOfOrder_classBetweenDirectives() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "library l;\nclass Foo{}\npart 'a.dart';",
        ParserErrorCode.DIRECTIVE_OUT_OF_ORDER);
    assertNotNull(unit);
  }

  public void test_expectedCaseOrDefault() throws Exception {
    parse("parseSwitchStatement", "switch (e) {break;}", ParserErrorCode.EXPECTED_CASE_OR_DEFAULT);
  }

  public void test_expectedIdentifier_number() throws Exception {
    SimpleIdentifier expression = parse(
        "parseSimpleIdentifier",
        "1",
        ParserErrorCode.EXPECTED_IDENTIFIER);
    assertTrue(expression.isSynthetic());
  }

  public void test_expectedStringLiteral() throws Exception {
    StringLiteral expression = parse(
        "parseStringLiteral",
        "1",
        ParserErrorCode.EXPECTED_STRING_LITERAL);
    assertTrue(expression.isSynthetic());
  }

  public void test_noUnaryPlusOperator() throws Exception {
    parse("parseUnaryExpression", "+x", ParserErrorCode.NO_UNARY_PLUS_OPERATOR);
  }

  public void test_onlyOneLibraryDirective() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "library l;library m;",
        ParserErrorCode.ONLY_ONE_LIBRARY_DIRECTIVE);
    assertNotNull(unit);
  }

  public void test_operatorCannotBeStatic_noReturnType() throws Exception {
    parse(
        "parseClassMember",
        "static operator +(int x) => x + 1",
        ParserErrorCode.OPERATOR_CANNOT_BE_STATIC);
  }

  public void test_operatorCannotBeStatic_returnType() throws Exception {
    parse(
        "parseClassMember",
        "static int operator +(int x) => x + 1",
        ParserErrorCode.OPERATOR_CANNOT_BE_STATIC);
  }

  public void test_operatorIsNotUserDefinable() throws Exception {
    parse(
        "parseClassMember",
        "operator +=(int x) => x + 1",
        ParserErrorCode.OPERATOR_IS_NOT_USER_DEFINABLE);
  }

  public void test_positionalAfterNamedArgument() throws Exception {
    parse("parseArgumentList", "(x: 1, 2)", ParserErrorCode.POSITIONAL_AFTER_NAMED_ARGUMENT);
  }

  public void test_topLevelCannotBeStatic() throws Exception {
    parse("parseCompilationUnitMember", "static var x;", ParserErrorCode.TOP_LEVEL_CANNOT_BE_STATIC);
  }
}
