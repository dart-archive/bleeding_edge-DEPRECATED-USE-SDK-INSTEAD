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
package com.google.dart.engine.internal.hint;

import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code Dart2JSVerifier} traverse an AST structure looking for hints for
 * code that will be compiled to JS, such as {@link HintCode#IS_DOUBLE}.
 * 
 * @coverage dart.engine.resolver
 */
public class Dart2JSVerifier extends RecursiveAstVisitor<Void> {

  /**
   * The error reporter by which errors will be reported.
   */
  private ErrorReporter errorReporter;

  /**
   * The name of the {@code double} type.
   */
  private static final String DOUBLE_TYPE_NAME = "double";

  /**
   * Create a new instance of the {@link Dart2JSVerifier}.
   * 
   * @param errorReporter the error reporter
   */
  public Dart2JSVerifier(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  @Override
  public Void visitIsExpression(IsExpression node) {
    checkForIsDoubleHints(node);
    return super.visitIsExpression(node);
  }

  /**
   * Check for instances of {@code x is double}, {@code x is int}, {@code x is! double} and
   * {@code x is! int}.
   * 
   * @param node the is expression to check
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#IS_DOUBLE
   * @see HintCode#IS_INT
   * @see HintCode#IS_NOT_DOUBLE
   * @see HintCode#IS_NOT_INT
   */
  private boolean checkForIsDoubleHints(IsExpression node) {
    TypeName typeName = node.getType();
    Type type = typeName.getType();
    if (type != null && type.getElement() != null) {
      Element element = type.getElement();
      String typeNameStr = element.getName();
      LibraryElement libraryElement = element.getLibrary();
//      if (typeNameStr.equals(INT_TYPE_NAME) && libraryElement != null
//          && libraryElement.isDartCore()) {
//        if (node.getNotOperator() == null) {
//          errorReporter.reportError(HintCode.IS_INT, node);
//        } else {
//          errorReporter.reportError(HintCode.IS_NOT_INT, node);
//        }
//        return true;
//      } else
      if (typeNameStr.equals(DOUBLE_TYPE_NAME) && libraryElement != null
          && libraryElement.isDartCore()) {
        if (node.getNotOperator() == null) {
          errorReporter.reportErrorForNode(HintCode.IS_DOUBLE, node);
        } else {
          errorReporter.reportErrorForNode(HintCode.IS_NOT_DOUBLE, node);
        }
        return true;
      }
    }
    return false;
  }

}
