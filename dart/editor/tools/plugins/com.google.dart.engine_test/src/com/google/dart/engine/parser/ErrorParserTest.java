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

/**
 * The class {@code ErrorParserTest} defines parser tests that test the parsing of invalid code
 * sequences to ensure that errors are correctly reported.
 */
public class ErrorParserTest extends ParserTestCase {
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
