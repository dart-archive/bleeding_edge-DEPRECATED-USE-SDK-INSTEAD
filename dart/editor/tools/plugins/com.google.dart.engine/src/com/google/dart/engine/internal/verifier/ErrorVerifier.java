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

import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code ErrorVerifier} traverse an AST structure looking for additional
 * errors and warnings not covered by the parser and resolver.
 */
public class ErrorVerifier extends RecursiveASTVisitor<Void> {
  /**
   * The error reporter by which errors will be reported.
   */
  private ErrorReporter errorReporter;

  /**
   * The type representing the type 'dynamic'.
   */
  private Type dynamicType;

  /**
   * The object providing access to the types defined by the language.
   */
  private TypeProvider typeProvider;

  public ErrorVerifier(ErrorReporter errorReporter, TypeProvider typeProvider) {
    this.errorReporter = errorReporter;
    this.typeProvider = typeProvider;
    dynamicType = typeProvider.getDynamicType();
  }

  @Override
  public Void visitAssertStatement(AssertStatement node) {
    Expression expression = node.getCondition();
    Type type = getType(expression);
    if (type instanceof InterfaceType) {
      if (!type.isAssignableTo(typeProvider.getBoolType())) {
        errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_EXPRESSION, expression);
      }
    } else if (type instanceof FunctionType) {
      FunctionType functionType = (FunctionType) type;
      if (functionType.getTypeArguments().length == 0
          && !functionType.getReturnType().isAssignableTo(typeProvider.getBoolType())) {
        errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_EXPRESSION, expression);
      }
    }
    return super.visitAssertStatement(node);
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    Expression lhs = node.getLeftHandSide();
    Expression rhs = node.getRightHandSide();
    Type leftType = getType(lhs);
    Type rightType = getType(rhs);
    if (!rightType.isAssignableTo(leftType)) {
      errorReporter.reportError(
          StaticTypeWarningCode.INVALID_ASSIGNMENT,
          rhs,
          leftType.toString(),
          rightType.toString());
    }
    return super.visitAssignmentExpression(node);
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpression node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitConditionalExpression(node);
  }

  @Override
  public Void visitDoStatement(DoStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitDoStatement(node);
  }

  @Override
  public Void visitIfStatement(IfStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitIfStatement(node);
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    TypeName typeName = node.getConstructorName().getType();
    Type createdType = typeName.getType();
    if (createdType instanceof InterfaceType) {
      if (((InterfaceType) createdType).getElement().isAbstract()) {
        if (((KeywordToken) node.getKeyword()).getKeyword() == Keyword.CONST) {
          errorReporter.reportError(StaticWarningCode.CONST_WITH_ABSTRACT_CLASS, typeName);
        } else {
          errorReporter.reportError(StaticWarningCode.NEW_WITH_ABSTRACT_CLASS, typeName);
        }
      }
    } else {
      errorReporter.reportError(CompileTimeErrorCode.NON_CONSTANT_MAP_KEY, typeName);
    }
    return super.visitInstanceCreationExpression(node);
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitWhileStatement(node);
  }

  /**
   * Checks to ensure that the expressions that need to be of type bool, are. Otherwise an error is
   * reported on the expression.
   * 
   * @see StaticTypeWarningCode#NON_BOOL_CONDITION
   * @param condition the conditional expression to test
   */
  private void checkForNonBoolCondition(Expression condition) {
    Type conditionType = getType(condition);
    if (conditionType != null && !conditionType.isAssignableTo(typeProvider.getBoolType())) {
      errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_CONDITION, condition);
    }
  }

  /**
   * Return the type of the given expression that is to be used for type analysis.
   * 
   * @param expression the expression whose type is to be returned
   * @return the type of the given expression
   */
  private Type getType(Expression expression) {
    Type type = expression.getStaticType();
    return type == null ? dynamicType : type;
  }

}
