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
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.TestSource;

import junit.framework.AssertionFailedError;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ParserTestCase extends EngineTestCase {
  /**
   * An empty array of classes used as parameter types for zero-argument methods.
   */
  private static final Class<?>[] EMPTY_PARAMETERS = new Class[0];

  /**
   * An empty array of objects used as arguments to zero-argument methods.
   */
  private static final Object[] EMPTY_ARGUMENTS = new Object[0];

  /**
   * Invoke a parse method in {@link Parser}. The method is assumed to have the given number and
   * type of parameters and will be invoked with the given arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the parse method is invoked.
   * 
   * @param methodName the name of the parse method that should be invoked to parse the source
   * @param classes the types of the arguments to the method
   * @param objects the values of the arguments to the method
   * @param source the source to be parsed by the parse method
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   * @throws AssertionFailedError if the result is {@code null} or if any errors are produced
   */
  public static <E> E parse(String methodName, Class<?>[] classes, Object[] objects, String source)
      throws Exception {
    return parse(methodName, classes, objects, source, new AnalysisError[0]);
  }

  /**
   * Invoke a parse method in {@link Parser}. The method is assumed to have the given number and
   * type of parameters and will be invoked with the given arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the parse method is invoked.
   * 
   * @param methodName the name of the parse method that should be invoked to parse the source
   * @param classes the types of the arguments to the method
   * @param objects the values of the arguments to the method
   * @param source the source to be parsed by the parse method
   * @param errorCodes the error codes of the errors that should be generated
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   * @throws AssertionFailedError if the result is {@code null} or the errors produced while
   *           scanning and parsing the source do not match the expected errors
   */
  public static <E> E parse(String methodName, Class<?>[] classes, Object[] objects, String source,
      AnalysisError... errors) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    E result = parse(methodName, classes, objects, source, listener);
    listener.assertErrors(errors);
    return result;
  }

  /**
   * Invoke a parse method in {@link Parser}. The method is assumed to have the given number and
   * type of parameters and will be invoked with the given arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the parse method is invoked.
   * 
   * @param methodName the name of the parse method that should be invoked to parse the source
   * @param classes the types of the arguments to the method
   * @param objects the values of the arguments to the method
   * @param source the source to be parsed by the parse method
   * @param errorCodes the error codes of the errors that should be generated
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   * @throws AssertionFailedError if the result is {@code null} or the errors produced while
   *           scanning and parsing the source do not match the expected errors
   */
  public static <E> E parse(String methodName, Class<?>[] classes, Object[] objects, String source,
      ErrorCode... errorCodes) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    E result = parse(methodName, classes, objects, source, listener);
    listener.assertErrors(errorCodes);
    return result;
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
   * @throws AssertionFailedError if the result is {@code null} or the errors produced while
   *           scanning and parsing the source do not match the expected errors
   */
  public static <E> E parse(String methodName, String source, ErrorCode... errorCodes)
      throws Exception {
    return parse(methodName, EMPTY_PARAMETERS, EMPTY_ARGUMENTS, source, errorCodes);
  }

  /**
   * Invoke a parse method in {@link Parser}. The method is assumed to have the given number and
   * type of parameters and will be invoked with the given arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the parse method is invoked.
   * 
   * @param methodName the name of the parse method that should be invoked to parse the source
   * @param classes the types of the arguments to the method
   * @param objects the values of the arguments to the method
   * @param source the source to be parsed by the parse method
   * @param listener the error listener that will be used for both scanning and parsing
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   * @throws AssertionFailedError if the result is {@code null} or the errors produced while
   *           scanning and parsing the source do not match the expected errors
   */
  @SuppressWarnings("unchecked")
  private static <E> E parse(String methodName, Class<?>[] classes, Object[] objects,
      String source, GatheringErrorListener listener) throws Exception {
    if (classes.length != objects.length) {
      fail("Invalid test: number of parameters specified (" + classes.length
          + ") does not match number of arguments provided (" + objects.length + ")");
    }
    //
    // Scan the source.
    //
    StringScanner scanner = new StringScanner(null, source, listener);
    Token tokenStream = scanner.tokenize();
    listener.setLineInfo(new TestSource(), scanner.getLineStarts());
    //
    // Parse the source.
    //
    Parser parser = new Parser(null, listener);
    Field currentTokenField = Parser.class.getDeclaredField("currentToken");
    currentTokenField.setAccessible(true);
    currentTokenField.set(parser, tokenStream);
    Method parseMethod = Parser.class.getDeclaredMethod(methodName, classes);
    parseMethod.setAccessible(true);
    Object result = parseMethod.invoke(parser, objects);
    //
    // Partially test the results.
    //
    assertNotNull(result);
    return (E) result;
  }
}
