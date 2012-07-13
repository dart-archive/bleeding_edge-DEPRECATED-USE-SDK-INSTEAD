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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * The class {@code ErrorParserTest} defines parser tests that test the parsing of invalid code
 * sequences to ensure that errors are correctly reported.
 */
public class ErrorParserTest extends EngineTestCase {
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

  /**
   * Invoke a parse method in {@link Parser}. The method is assumed to have no arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the parse method is invoked.
   * 
   * @param methodName the name of the parse method that should be invoked to parse the source
   * @param source the source to be parsed by the parse method
   * @param errorCodes the error codes of the errors that should be generated
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  @SuppressWarnings("unchecked")
  private <E> E parse(String methodName, String source, ErrorCode... errorCodes) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    StringScanner scanner = new StringScanner(null, source, listener);
    Token tokenStream = scanner.tokenize();
    Parser parser = new Parser(null, listener);
    Field currentTokenField = Parser.class.getDeclaredField("currentToken");
    currentTokenField.setAccessible(true);
    currentTokenField.set(parser, tokenStream);
    Method parseMethod = Parser.class.getDeclaredMethod(methodName);
    parseMethod.setAccessible(true);
    Object result = parseMethod.invoke(parser);
    assertNotNull(result);
    listener.assertErrors(errorCodes);
    return (E) result;
  }
}
