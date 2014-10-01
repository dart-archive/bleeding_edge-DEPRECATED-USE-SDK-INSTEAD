/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.error;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.AnalysisErrorWithProperties;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;

import java.util.HashSet;

/**
 * Instances of the class {@code ErrorReporter} wrap an error listener with utility methods used to
 * create the errors being reported.
 * 
 * @coverage dart.engine.error
 */
public class ErrorReporter {
  /**
   * The error listener to which errors will be reported.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The default source to be used when reporting errors.
   */
  private Source defaultSource;

  /**
   * The source to be used when reporting errors.
   */
  private Source source;

  /**
   * Initialize a newly created error reporter that will report errors to the given listener.
   * 
   * @param errorListener the error listener to which errors will be reported
   * @param defaultSource the default source to be used when reporting errors
   */
  public ErrorReporter(AnalysisErrorListener errorListener, Source defaultSource) {
    if (errorListener == null) {
      throw new IllegalArgumentException("An error listener must be provided");
    } else if (defaultSource == null) {
      throw new IllegalArgumentException("A default source must be provided");
    }
    this.errorListener = errorListener;
    this.defaultSource = defaultSource;
    this.source = defaultSource;
  }

  public Source getSource() {
    return source;
  }

  /**
   * Creates an error with properties with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param node the node specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  public AnalysisErrorWithProperties newErrorWithProperties(ErrorCode errorCode, AstNode node,
      Object... arguments) {
    return new AnalysisErrorWithProperties(
        source,
        node.getOffset(),
        node.getLength(),
        errorCode,
        arguments);
  }

  /**
   * Report a passed error.
   * 
   * @param error the error to report
   */
  public void reportError(AnalysisError error) {
    errorListener.onError(error);
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param element the element which name should be used as the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  public void reportErrorForElement(ErrorCode errorCode, Element element, Object... arguments) {
    reportErrorForOffset(
        errorCode,
        element.getNameOffset(),
        element.getDisplayName().length(),
        arguments);
  }

  /**
   * Report an error with the given error code and arguments.
   * <p>
   * If the arguments contain the names of two or more types, the method
   * {@link #reportTypeErrorForNode(ErrorCode, AstNode, Object...)} should be used and the types
   * themselves (rather than their names) should be passed as arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param node the node specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  public void reportErrorForNode(ErrorCode errorCode, AstNode node, Object... arguments) {
    reportErrorForOffset(errorCode, node.getOffset(), node.getLength(), arguments);
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param offset the offset of the location of the error
   * @param length the length of the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  public void reportErrorForOffset(ErrorCode errorCode, int offset, int length, Object... arguments) {
    errorListener.onError(new AnalysisError(source, offset, length, errorCode, arguments));
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param token the token specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  public void reportErrorForToken(ErrorCode errorCode, Token token, Object... arguments) {
    reportErrorForOffset(errorCode, token.getOffset(), token.getLength(), arguments);
  }

  /**
   * Report an error with the given error code and arguments. The arguments are expected to contain
   * two or more types. Convert the types into strings by using the display names of the types,
   * unless there are two or more types with the same names, in which case the extended display
   * names of the types will be used in order to clarify the message.
   * <p>
   * If there are not two or more types in the argument list, the method
   * {@link #reportErrorForNode(ErrorCode, AstNode, Object...)} should be used instead.
   * 
   * @param errorCode the error code of the error to be reported
   * @param node the node specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  public void reportTypeErrorForNode(ErrorCode errorCode, AstNode node, Object... arguments) {
    convertTypeNames(arguments);
    reportErrorForOffset(errorCode, node.getOffset(), node.getLength(), arguments);
  }

  /**
   * Set the source to be used when reporting errors. Setting the source to {@code null} will cause
   * the default source to be used.
   * 
   * @param source the source to be used when reporting errors
   */
  public void setSource(Source source) {
    this.source = source == null ? defaultSource : source;
  }

  /**
   * Given an array of arguments that is expected to contain two or more types, convert the types
   * into strings by using the display names of the types, unless there are two or more types with
   * the same names, in which case the extended display names of the types will be used in order to
   * clarify the message.
   * 
   * @param arguments the arguments that are to be converted
   */
  private void convertTypeNames(Object[] arguments) {
    if (hasEqualTypeNames(arguments)) {
      int count = arguments.length;
      for (int i = 0; i < count; i++) {
        Object argument = arguments[i];
        if (argument instanceof Type) {
          Type type = (Type) argument;
          Element element = type.getElement();
          if (element == null) {
            arguments[i] = type.getDisplayName();
          } else {
            arguments[i] = element.getExtendedDisplayName(type.getDisplayName());
          }
        }
      }
    } else {
      int count = arguments.length;
      for (int i = 0; i < count; i++) {
        Object argument = arguments[i];
        if (argument instanceof Type) {
          arguments[i] = ((Type) argument).getDisplayName();
        }
      }
    }
  }

  /**
   * Return {@code true} if the given array of arguments contains two or more types with the same
   * display name.
   * 
   * @param arguments the arguments being tested
   * @return {@code true} if the array of arguments contains two or more types with the same display
   *         name
   */
  private boolean hasEqualTypeNames(Object[] arguments) {
    int count = arguments.length;
    HashSet<String> typeNames = new HashSet<String>(count);
    for (int i = 0; i < count; i++) {
      if (arguments[i] instanceof Type && !typeNames.add(((Type) arguments[i]).getDisplayName())) {
        return true;
      }
    }
    return false;
  }
}
