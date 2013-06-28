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

import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.error.AuditCode;
import com.google.dart.engine.internal.constant.ConstantVisitor;
import com.google.dart.engine.internal.constant.EvaluationResultImpl;
import com.google.dart.engine.internal.constant.ValidResult;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

/**
 * Instances of the class {@code DeadCodeVerifier} traverse an AST structure looking for cases of
 * {@link AuditCode#DEAD_CODE}.
 * 
 * @coverage dart.engine.resolver
 */
public class DeadCodeVerifier extends RecursiveASTVisitor<Void> {

  /**
   * The error reporter by which errors will be reported.
   */
  private ErrorReporter errorReporter;

  /**
   * Create a new instance of the {@link DeadCodeVerifier}.
   * 
   * @param errorReporter the error reporter
   */
  public DeadCodeVerifier(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    Token operator = node.getOperator();
    boolean isAmpAmp = operator.getType() == TokenType.AMPERSAND_AMPERSAND;
    boolean isBarBar = operator.getType() == TokenType.BAR_BAR;
    boolean foundError = false;
    if (isAmpAmp || isBarBar) {
      Expression lhsCondition = node.getLeftOperand();
      ValidResult lhsResult = getConstantBooleanValue(lhsCondition);
      if (lhsResult != null) {
        if (lhsResult == ValidResult.RESULT_TRUE && isBarBar) {
          // report error on else block: true || !e!
          errorReporter.reportError(AuditCode.DEAD_CODE, node.getRightOperand());
          foundError = true;
        } else if (lhsResult == ValidResult.RESULT_FALSE && isAmpAmp) {
          // report error on if block: false && !e!
          errorReporter.reportError(AuditCode.DEAD_CODE, node.getRightOperand());
          foundError = true;
        }
      }
      // How do we want to handle the RHS? It isn't dead code, but "pointless" or "obscure"...
//      Expression rhsCondition = node.getRightOperand();
//      ValidResult rhsResult = getConstantBooleanValue(rhsCondition);
//      if (rhsResult != null) {
//        if (rhsResult == ValidResult.RESULT_TRUE && isBarBar) {
//          // report error on else block: !e! || true
//          errorReporter.reportError(AuditCode.DEAD_CODE, node.getRightOperand());
//          foundError = false;
//        } else if (rhsResult == ValidResult.RESULT_FALSE && isAmpAmp) {
//          // report error on if block: !e! && false
//          errorReporter.reportError(AuditCode.DEAD_CODE, node.getRightOperand());
//          foundError = false;
//        }
//      }
    }
    if (foundError) {
      return null;
    }
    return super.visitBinaryExpression(node);
  }

  @Override
  public Void visitBlock(Block node) {
    checkForDeadCodeStatementsAfterReturn(node);
    return super.visitBlock(node);
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpression node) {
    Expression conditionExpression = node.getCondition();
    ValidResult result = getConstantBooleanValue(conditionExpression);
    if (result != null) {
      if (result == ValidResult.RESULT_TRUE) {
        // report error on else block: true ? 1 : !2!
        errorReporter.reportError(AuditCode.DEAD_CODE, node.getElseExpression());
      } else {
        // report error on if block: false ? !1! : 2
        errorReporter.reportError(AuditCode.DEAD_CODE, node.getThenExpression());
      }
    }
    return super.visitConditionalExpression(node);
  }

  // Do we want to report "pointless" or "obscure" code such as do {} !while (false);!
//  @Override
//  public Void visitDoStatement(DoStatement node) {
//    Expression conditionExpression = node.getCondition();
//    ValidResult result = getConstantBooleanValue(conditionExpression);
//    if (result != null) {
//      if (result == ValidResult.RESULT_FALSE) {
//        // report error on if block: do {} !while (false);!
//        int whileOffset = node.getWhileKeyword().getOffset();
//        int semiColonOffset = node.getSemicolon().getOffset() + 1;
//        int length = semiColonOffset - whileOffset;
//        errorReporter.reportError(AuditCode.DEAD_CODE, whileOffset, length);
//      }
//    }
//    return super.visitDoStatement(node);
//  }

  @Override
  public Void visitIfStatement(IfStatement node) {
    Expression conditionExpression = node.getCondition();
    ValidResult result = getConstantBooleanValue(conditionExpression);
    if (result != null) {
      if (result == ValidResult.RESULT_TRUE) {
        // report error on else block: if(true) {} else {!}
        Statement elseStatement = node.getElseStatement();
        if (elseStatement != null) {
          errorReporter.reportError(AuditCode.DEAD_CODE, elseStatement);
        }
      } else {
        // report error on if block: if (false) {!} else {}
        errorReporter.reportError(AuditCode.DEAD_CODE, node.getThenStatement());
      }
    }
    return super.visitIfStatement(node);
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    Expression conditionExpression = node.getCondition();
    ValidResult result = getConstantBooleanValue(conditionExpression);
    if (result != null) {
      if (result == ValidResult.RESULT_FALSE) {
        // report error on if block: while (false) {!}
        errorReporter.reportError(AuditCode.DEAD_CODE, node.getBody());
      }
    }
    return super.visitWhileStatement(node);
  }

  /**
   * Given some {@link Block}, this method reports and error on all statements between the end of
   * the block and the first return statement (assuming there it is not at the end of the block.)
   * 
   * @param node the block to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   */
  private boolean checkForDeadCodeStatementsAfterReturn(Block node) {
    NodeList<Statement> statements = node.getStatements();
    int size = statements.size();
    if (size == 0) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      Statement currentStatement = statements.get(i);
      if (currentStatement instanceof ReturnStatement && i != size - 1) {
        Statement nextStatement = statements.get(i + 1);
        Statement lastStatement = statements.get(size - 1);
        int offset = nextStatement.getOffset();
        int length = lastStatement.getEnd() - offset;
        errorReporter.reportError(AuditCode.DEAD_CODE, offset, length);
        return true;
      }
    }
    return false;
  }

  /**
   * Given some {@link Expression}, this method returns {@link ValidResult#RESULT_TRUE} if it is
   * {@code true}, {@link ValidResult#RESULT_FALSE} if it is {@code false}, or {@code null} if the
   * expression is not a constant boolean value.
   * 
   * @param expression the expression to evaluate
   * @return {@link ValidResult#RESULT_TRUE} if it is {@code true}, {@link ValidResult#RESULT_FALSE}
   *         if it is {@code false}, or {@code null} if the expression is not a constant boolean
   *         value
   */
  private ValidResult getConstantBooleanValue(Expression expression) {
    if (expression instanceof BooleanLiteral) {
      if (((BooleanLiteral) expression).getValue()) {
        return ValidResult.RESULT_TRUE;
      } else {
        return ValidResult.RESULT_FALSE;
      }
    } else {
      EvaluationResultImpl result = expression.accept(new ConstantVisitor());
      if (result == ValidResult.RESULT_TRUE) {
        return ValidResult.RESULT_TRUE;
      } else if (result == ValidResult.RESULT_FALSE) {
        return ValidResult.RESULT_FALSE;
      }
      return null;
    }
  }

}
