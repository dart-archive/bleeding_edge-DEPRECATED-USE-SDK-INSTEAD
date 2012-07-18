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

/**
 * The class {@code ErrorParserTest} defines parser tests that test the parsing of invalid code
 * sequences to ensure that errors are correctly reported.
 */
public class ErrorParserTest extends ParserTestCase {

  public void fail_expectedListOrMapLiteral() throws Exception {
    TypedLiteral literal = parse(
        "parseListOrMapLiteral",
        "1",
        ParserErrorCode.EXPECTED_LIST_OR_MAP_LITERAL);
    assertTrue(literal.isSynthetic());
  }

  public void test_directiveOutOfOrder_classBeforeDirective() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "class Foo{}\n#library('LibraryName');",
        ParserErrorCode.DIRECTIVE_OUT_OF_ORDER);
    assertNotNull(unit);
  }

  public void test_directiveOutOfOrder_classBetweenDirectives() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "#library('LibraryName');\nclass Foo{}\n#source('a.dart');",
        ParserErrorCode.DIRECTIVE_OUT_OF_ORDER);
    assertNotNull(unit);
  }

  public void test_expectedCatchOrFinallyExpected() throws Exception {
    TryStatement statement = parse(
        "parseTryStatement",
        "try {}",
        ParserErrorCode.CATCH_OR_FINALLY_EXPECTED);
    assertNotNull(statement);
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

  public void test_onlyOneLibraryDirective() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "#library('LibraryName');#library('LibraryName2');",
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

}
