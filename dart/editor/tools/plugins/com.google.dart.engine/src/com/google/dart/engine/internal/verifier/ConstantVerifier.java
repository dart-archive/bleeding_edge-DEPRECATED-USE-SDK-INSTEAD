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
package com.google.dart.engine.internal.verifier;

import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.ConstantEvaluator;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.error.ErrorReporter;

import java.util.HashSet;

/**
 * Instances of the class {@code ConstantVerifier} traverse an AST structure looking for additional
 * errors and warnings not covered by the parser and resolver. In particular, it looks for errors
 * and warnings related to constant expressions.
 */
public class ConstantVerifier extends RecursiveASTVisitor<Void> {
  /**
   * The error reporter by which errors will be reported.
   */
  private ErrorReporter errorReporter;

  /**
   * The constant evaluator used to evaluate constants.
   */
  private ConstantEvaluator evaluator;

  /**
   * Initialize a newly created constant verifier.
   * 
   * @param errorReporter the error reporter by which errors will be reported
   */
  public ConstantVerifier(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
    evaluator = new ConstantEvaluator(errorReporter);
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    super.visitFunctionExpression(node);
    validateDefaultValues(node.getParameters());
    return null;
  }

  @Override
  public Void visitListLiteral(ListLiteral node) {
    super.visitListLiteral(node);
    if (node.getModifier() != null) {
      for (Expression element : node.getElements()) {
        validate(element, CompileTimeErrorCode.NON_CONSTANT_LIST_ELEMENT);
      }
    }
    return null;
  }

  @Override
  public Void visitMapLiteral(MapLiteral node) {
    super.visitMapLiteral(node);
    boolean isConst = node.getModifier() != null;
    HashSet<String> keys = new HashSet<String>();
    for (MapLiteralEntry entry : node.getEntries()) {
      StringLiteral key = entry.getKey();
      Object value = validate(key, CompileTimeErrorCode.NON_CONSTANT_MAP_KEY);
      if (value instanceof String) {
        if (keys.contains(value)) {
          errorReporter.reportError(StaticWarningCode.EQUAL_KEYS_IN_MAP, key);
        } else {
          keys.add((String) value);
        }
      } else if (value != null) {
        // TODO(brianwilkerson) If this can ever happen, report this error.
      }
      if (isConst) {
        validate(entry.getValue(), CompileTimeErrorCode.NON_CONSTANT_MAP_VALUE);
      }
    }
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    super.visitMethodDeclaration(node);
    validateDefaultValues(node.getParameters());
    return null;
  }

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    super.visitSwitchCase(node);
    validate(node.getExpression(), CompileTimeErrorCode.NON_CONSTANT_CASE_EXPRESSION);
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    super.visitVariableDeclaration(node);
    Expression initializer = node.getInitializer();
    if (initializer != null && node.isConst()) {
      validate(initializer, CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
    }
    return null;
  }

  /**
   * Validate that the given expression is a compile time constant. Return the value of the compile
   * time constant, or {@code null} if the expression is not a compile time constant.
   * 
   * @param expression the expression to be validated
   * @param errorCode the error code to be used if the expression is not a compile time constant
   * @return the value of the compile time constant
   */
  private Object validate(Expression expression, ErrorCode errorCode) {
    Object value = expression.accept(evaluator);
    if (value == ConstantEvaluator.NOT_A_CONSTANT) {
      errorReporter.reportError(errorCode, expression);
      return null;
    }
    // TODO(brianwilkerson) Decide what value will be returned to represent that the expression
    // would throw an exception and test for it.
    if (value == errorCode) {
      errorReporter.reportError(
          CompileTimeErrorCode.COMPILE_TIME_CONSTANT_RAISES_EXCEPTION,
          expression);
      return null;
    }
    // TODO(brianwilkerson) Decide what value will be returned to represent that the expression
    // is recursively defined in terms of itself.
    if (value == errorCode) {
      errorReporter.reportError(CompileTimeErrorCode.RECURSIVE_COMPILE_TIME_CONSTANT, expression);
      return null;
    }
    return value;
  }

  /**
   * Validate that the default value associated with each of the parameters in the given list is a
   * compile time constant.
   * 
   * @param parameters the list of parameters to be validated
   */
  private void validateDefaultValues(FormalParameterList parameters) {
    if (parameters == null) {
      return;
    }
    for (FormalParameter parameter : parameters.getParameters()) {
      if (parameter instanceof DefaultFormalParameter) {
        Expression defaultValue = ((DefaultFormalParameter) parameter).getDefaultValue();
        if (defaultValue != null) {
          validate(defaultValue, CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
        }
      }
    }
  }
}
