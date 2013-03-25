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
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.constant.ConstantVisitor;
import com.google.dart.engine.internal.constant.ErrorResult;
import com.google.dart.engine.internal.constant.EvaluationResultImpl;
import com.google.dart.engine.internal.constant.ValidResult;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.error.ErrorReporter;

import java.util.HashSet;

/**
 * Instances of the class {@code ConstantVerifier} traverse an AST structure looking for additional
 * errors and warnings not covered by the parser and resolver. In particular, it looks for errors
 * and warnings related to constant expressions.
 * 
 * @coverage dart.engine.resolver
 */
public class ConstantVerifier extends RecursiveASTVisitor<Void> {
  /**
   * The error reporter by which errors will be reported.
   */
  private ErrorReporter errorReporter;

  /**
   * Initialize a newly created constant verifier.
   * 
   * @param errorReporter the error reporter by which errors will be reported
   */
  public ConstantVerifier(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
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
      EvaluationResultImpl result = validate(key, CompileTimeErrorCode.NON_CONSTANT_MAP_KEY);
      if (result instanceof ValidResult && ((ValidResult) result).getValue() instanceof String) {
        String value = (String) ((ValidResult) result).getValue();
        if (keys.contains(value)) {
          errorReporter.reportError(StaticWarningCode.EQUAL_KEYS_IN_MAP, key);
        } else {
          keys.add(value);
        }
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
      VariableElementImpl element = (VariableElementImpl) node.getElement();
      EvaluationResultImpl result = element.getEvaluationResult();
      if (result == null) {
        //
        // Normally we don't need to visit const variable declarations because we have already
        // computed their values. But if we missed it for some reason, this gives us a second
        // chance.
        //
        result = validate(
            initializer,
            CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
        element.setEvaluationResult(result);
      } else if (result instanceof ErrorResult) {
        reportErrors(result, CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
      }
    }
    return null;
  }

  /**
   * If the given result represents one or more errors, report those errors. Except for special
   * cases, use the given error code rather than the one reported in the error.
   * 
   * @param result the result containing any errors that need to be reported
   * @param errorCode the error code to be used if the result represents an error
   */
  private void reportErrors(EvaluationResultImpl result, ErrorCode errorCode) {
    if (result instanceof ErrorResult) {
      for (ErrorResult.ErrorData data : ((ErrorResult) result).getErrorData()) {
        errorReporter.reportError(errorCode, data.getNode());
      }
    }
  }

  /**
   * Validate that the given expression is a compile time constant. Return the value of the compile
   * time constant, or {@code null} if the expression is not a compile time constant.
   * 
   * @param expression the expression to be validated
   * @param errorCode the error code to be used if the expression is not a compile time constant
   * @return the value of the compile time constant
   */
  private EvaluationResultImpl validate(Expression expression, ErrorCode errorCode) {
    EvaluationResultImpl result = expression.accept(new ConstantVisitor());
    reportErrors(result, errorCode);
    return result;
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
        DefaultFormalParameter defaultParameter = (DefaultFormalParameter) parameter;
        Expression defaultValue = defaultParameter.getDefaultValue();
        if (defaultValue != null) {
          EvaluationResultImpl result = validate(
              defaultValue,
              CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
          if (defaultParameter.isConst()) {
            VariableElementImpl element = (VariableElementImpl) parameter.getElement();
            element.setEvaluationResult(result);
          }
        }
      }
    }
  }
}
