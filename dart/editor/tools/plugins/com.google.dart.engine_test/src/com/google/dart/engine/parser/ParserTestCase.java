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
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.parser.CommentAndMetadata;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.TestSource;

import junit.framework.AssertionFailedError;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ParserTestCase extends EngineTestCase {
  /**
   * An empty array of objects used as arguments to zero-argument methods.
   */
  private static final Object[] EMPTY_ARGUMENTS = new Object[0];

  /**
   * A flag indicating whether parser is to parse function bodies.
   */
  protected static boolean parseFunctionBodies = true;

  /**
   * Invoke a parse method in {@link Parser}. The method is assumed to have the given number and
   * type of parameters and will be invoked with the given arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the parse method is invoked.
   * 
   * @param methodName the name of the parse method that should be invoked to parse the source
   * @param objects the values of the arguments to the method
   * @param source the source to be parsed by the parse method
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   * @throws AssertionFailedError if the result is {@code null} or if any errors are produced
   */
  public static <E> E parse(String methodName, Object[] objects, String source) throws Exception {
    return parse(methodName, objects, source, new AnalysisError[0]);
  }

  /**
   * Invoke a parse method in {@link Parser}. The method is assumed to have the given number and
   * type of parameters and will be invoked with the given arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the parse method is invoked.
   * 
   * @param methodName the name of the parse method that should be invoked to parse the source
   * @param objects the values of the arguments to the method
   * @param source the source to be parsed by the parse method
   * @param errors the errors that should be generated
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   * @throws AssertionFailedError if the result is {@code null} or the errors produced while
   *           scanning and parsing the source do not match the expected errors
   */
  public static <E> E parse(String methodName, Object[] objects, String source,
      AnalysisError... errors) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    E result = invokeParserMethod(methodName, objects, source, listener);
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
   * @param objects the values of the arguments to the method
   * @param source the source to be parsed by the parse method
   * @param errorCodes the error codes of the errors that should be generated
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   * @throws AssertionFailedError if the result is {@code null} or the errors produced while
   *           scanning and parsing the source do not match the expected errors
   */
  public static <E> E parse(String methodName, Object[] objects, String source,
      ErrorCode... errorCodes) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    E result = invokeParserMethod(methodName, objects, source, listener);
    listener.assertErrorsWithCodes(errorCodes);
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
    return parse(methodName, EMPTY_ARGUMENTS, source, errorCodes);
  }

  /**
   * Parse the given source as a compilation unit.
   * 
   * @param source the source to be parsed
   * @param errorCodes the error codes of the errors that are expected to be found
   * @return the compilation unit that was parsed
   * @throws Exception if the source could not be parsed, if the compilation errors in the source do
   *           not match those that are expected, or if the result would have been {@code null}
   */
  public static CompilationUnit parseCompilationUnit(String source, ErrorCode... errorCodes)
      throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scanner scanner = new Scanner(null, new CharSequenceReader(source), listener);
    listener.setLineInfo(new TestSource(), scanner.getLineStarts());
    Token token = scanner.tokenize();
    Parser parser = new Parser(null, listener);
    parser.setParseAsync(true);
    parser.setParseDeferredLibraries(true);
    parser.setParseEnum(true);
    CompilationUnit unit = parser.parseCompilationUnit(token);
    assertNotNull(unit);
    listener.assertErrorsWithCodes(errorCodes);
    return unit;
  }

  /**
   * Parse the given source as an expression.
   * 
   * @param source the source to be parsed
   * @param errorCodes the error codes of the errors that are expected to be found
   * @return the expression that was parsed
   * @throws Exception if the source could not be parsed, if the compilation errors in the source do
   *           not match those that are expected, or if the result would have been {@code null}
   */
  @SuppressWarnings("unchecked")
  public static <E extends Expression> E parseExpression(String source, ErrorCode... errorCodes)
      throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scanner scanner = new Scanner(null, new CharSequenceReader(source), listener);
    listener.setLineInfo(new TestSource(), scanner.getLineStarts());
    Token token = scanner.tokenize();
    Parser parser = new Parser(null, listener);
    Expression expression = parser.parseExpression(token);
    assertNotNull(expression);
    listener.assertErrorsWithCodes(errorCodes);
    return (E) expression;
  }

  /**
   * Parse the given source as a statement.
   * 
   * @param source the source to be parsed
   * @param errorCodes the error codes of the errors that are expected to be found
   * @return the statement that was parsed
   * @throws Exception if the source could not be parsed, if the compilation errors in the source do
   *           not match those that are expected, or if the result would have been {@code null}
   */
  @SuppressWarnings("unchecked")
  public static <E extends Statement> E parseStatement(String source, ErrorCode... errorCodes)
      throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scanner scanner = new Scanner(null, new CharSequenceReader(source), listener);
    listener.setLineInfo(new TestSource(), scanner.getLineStarts());
    Token token = scanner.tokenize();
    Parser parser = new Parser(null, listener);
    Statement statement = parser.parseStatement(token);
    assertNotNull(statement);
    listener.assertErrorsWithCodes(errorCodes);
    return (E) statement;
  }

  /**
   * Parse the given source as a sequence of statements.
   * 
   * @param source the source to be parsed
   * @param expectedCount the number of statements that are expected
   * @param errorCodes the error codes of the errors that are expected to be found
   * @return the statements that were parsed
   * @throws Exception if the source could not be parsed, if the number of statements does not match
   *           the expected count, if the compilation errors in the source do not match those that
   *           are expected, or if the result would have been {@code null}
   */
  public static List<Statement> parseStatements(String source, int expectedCount,
      ErrorCode... errorCodes) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scanner scanner = new Scanner(null, new CharSequenceReader(source), listener);
    listener.setLineInfo(new TestSource(), scanner.getLineStarts());
    Token token = scanner.tokenize();
    Parser parser = new Parser(null, listener);
    List<Statement> statements = parser.parseStatements(token);
    assertSizeOfList(expectedCount, statements);
    listener.assertErrorsWithCodes(errorCodes);
    return statements;
  }

  /**
   * Invoke a method in {@link Parser}. The method is assumed to have the given number and type of
   * parameters and will be invoked with the given arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the method is invoked.
   * 
   * @param methodName the name of the method that should be invoked
   * @param objects the values of the arguments to the method
   * @param source the source to be processed by the parse method
   * @param listener the error listener that will be used for both scanning and parsing
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   * @throws AssertionFailedError if the result is {@code null} or the errors produced while
   *           scanning and parsing the source do not match the expected errors
   */
  @SuppressWarnings("unchecked")
  protected static <E> E invokeParserMethod(String methodName, Object[] objects, String source,
      GatheringErrorListener listener) throws Exception {
    //
    // Scan the source.
    //
    Scanner scanner = new Scanner(null, new CharSequenceReader(source), listener);
    Token tokenStream = scanner.tokenize();
    listener.setLineInfo(new TestSource(), scanner.getLineStarts());
    //
    // Parse the source.
    //
    Parser parser = new Parser(null, listener);
    parser.setParseFunctionBodies(parseFunctionBodies);
    parser.setParseDeferredLibraries(true);
    parser.setParseAsync(true);
    Object result = invokeParserMethodImpl(parser, methodName, objects, tokenStream);
    //
    // Partially test the results.
    //
    if (!listener.hasErrors()) {
      assertNotNull(result);
    }
    return (E) result;
  }

  /**
   * Invoke a method in {@link Parser}. The method is assumed to have no arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the method is invoked.
   * 
   * @param methodName the name of the method that should be invoked
   * @param source the source to be processed by the parse method
   * @param listener the error listener that will be used for both scanning and parsing
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   * @throws AssertionFailedError if the result is {@code null} or the errors produced while
   *           scanning and parsing the source do not match the expected errors
   */
  protected static <E> E invokeParserMethod(String methodName, String source,
      GatheringErrorListener listener) throws Exception {
    return invokeParserMethod(methodName, EMPTY_ARGUMENTS, source, listener);
  }

  /**
   * Invokes {@link Parser} method with given name and valid number of parameters for given
   * arguments.
   */
  protected static Object invokeParserMethodImpl(Parser parser, String methodName,
      Object[] objects, Token tokenStream) throws Exception {
    Field currentTokenField = Parser.class.getDeclaredField("currentToken");
    currentTokenField.setAccessible(true);
    currentTokenField.set(parser, tokenStream);
    Method parseMethod = findParserMethod(methodName, objects.length);
    return parseMethod.invoke(parser, objects);
  }

  /**
   * @return the {@link Method} with given name in {@link Parser}, not {@code null}. Fails if not
   *         found.
   */
  private static Method findParserMethod(String name, int numParameters) {
    Method[] methods = Parser.class.getDeclaredMethods();
    for (Method method : methods) {
      if (method.getName().equals(name) && method.getParameterTypes().length == numParameters) {
        method.setAccessible(true);
        return method;
      }
    }
    fail("Cannot find method Parser." + name);
    return null;
  }

  /**
   * Return a CommentAndMetadata object with the given values that can be used for testing.
   * 
   * @param comment the comment to be wrapped in the object
   * @param annotations the annotations to be wrapped in the object
   * @return a CommentAndMetadata object that can be used for testing
   */
  protected CommentAndMetadata commentAndMetadata(Comment comment, Annotation... annotations) {
    ArrayList<Annotation> metadata = new ArrayList<Annotation>();
    for (Annotation annotation : annotations) {
      metadata.add(annotation);
    }
    return new CommentAndMetadata(comment, metadata);
  }

  /**
   * Return an empty CommentAndMetadata object that can be used for testing.
   * 
   * @return an empty CommentAndMetadata object that can be used for testing
   */
  protected CommentAndMetadata emptyCommentAndMetadata() {
    return new CommentAndMetadata(null, new ArrayList<Annotation>());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    parseFunctionBodies = true;
  }
}
