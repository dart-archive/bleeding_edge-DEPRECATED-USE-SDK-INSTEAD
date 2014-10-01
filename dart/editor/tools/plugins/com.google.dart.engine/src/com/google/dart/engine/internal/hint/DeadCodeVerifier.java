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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.internal.constant.EvaluationResultImpl;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.object.BoolState;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;

/**
 * Instances of the class {@code DeadCodeVerifier} traverse an AST structure looking for cases of
 * {@link HintCode#DEAD_CODE}.
 * 
 * @coverage dart.engine.resolver
 */
public class DeadCodeVerifier extends RecursiveAstVisitor<Void> {

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
    if (isAmpAmp || isBarBar) {
      Expression lhsCondition = node.getLeftOperand();
      if (!isDebugConstant(lhsCondition)) {
        EvaluationResultImpl lhsResult = getConstantBooleanValue(lhsCondition);
        if (lhsResult != null) {
          if (lhsResult.getValue().isTrue() && isBarBar) {
            // report error on else block: true || !e!
            errorReporter.reportErrorForNode(HintCode.DEAD_CODE, node.getRightOperand());
            // only visit the LHS:
            safelyVisit(lhsCondition);
            return null;
          } else if (lhsResult.getValue().isFalse() && isAmpAmp) {
            // report error on if block: false && !e!
            errorReporter.reportErrorForNode(HintCode.DEAD_CODE, node.getRightOperand());
            // only visit the LHS:
            safelyVisit(lhsCondition);
            return null;
          }
        }
      }
      // How do we want to handle the RHS? It isn't dead code, but "pointless" or "obscure"...
//      Expression rhsCondition = node.getRightOperand();
//      ValidResult rhsResult = getConstantBooleanValue(rhsCondition);
//      if (rhsResult != null) {
//        if (rhsResult == ValidResult.RESULT_TRUE && isBarBar) {
//          // report error on else block: !e! || true
//          errorReporter.reportError(HintCode.DEAD_CODE, node.getRightOperand());
//          // only visit the RHS:
//          safelyVisit(rhsCondition);
//          return null;
//        } else if (rhsResult == ValidResult.RESULT_FALSE && isAmpAmp) {
//          // report error on if block: !e! && false
//          errorReporter.reportError(HintCode.DEAD_CODE, node.getRightOperand());
//          // only visit the RHS:
//          safelyVisit(rhsCondition);
//          return null;
//        }
//      }
    }
    return super.visitBinaryExpression(node);
  }

  /**
   * For each {@link Block}, this method reports and error on all statements between the end of the
   * block and the first return statement (assuming there it is not at the end of the block.)
   * 
   * @param node the block to evaluate
   */
  @Override
  public Void visitBlock(Block node) {
    NodeList<Statement> statements = node.getStatements();
    checkForDeadStatementsInNodeList(statements);
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpression node) {
    Expression conditionExpression = node.getCondition();
    safelyVisit(conditionExpression);
    if (!isDebugConstant(conditionExpression)) {
      EvaluationResultImpl result = getConstantBooleanValue(conditionExpression);
      if (result != null) {
        if (result.getValue().isTrue()) {
          // report error on else block: true ? 1 : !2!
          errorReporter.reportErrorForNode(HintCode.DEAD_CODE, node.getElseExpression());
          safelyVisit(node.getThenExpression());
          return null;
        } else {
          // report error on if block: false ? !1! : 2
          errorReporter.reportErrorForNode(HintCode.DEAD_CODE, node.getThenExpression());
          safelyVisit(node.getElseExpression());
          return null;
        }
      }
    }
    return super.visitConditionalExpression(node);
  }

  @Override
  public Void visitIfStatement(IfStatement node) {
    Expression conditionExpression = node.getCondition();
    safelyVisit(conditionExpression);
    if (!isDebugConstant(conditionExpression)) {
      EvaluationResultImpl result = getConstantBooleanValue(conditionExpression);
      if (result != null) {
        if (result.getValue().isTrue()) {
          // report error on else block: if(true) {} else {!}
          Statement elseStatement = node.getElseStatement();
          if (elseStatement != null) {
            errorReporter.reportErrorForNode(HintCode.DEAD_CODE, elseStatement);
            safelyVisit(node.getThenStatement());
            return null;
          }
        } else {
          // report error on if block: if (false) {!} else {}
          errorReporter.reportErrorForNode(HintCode.DEAD_CODE, node.getThenStatement());
          safelyVisit(node.getElseStatement());
          return null;
        }
      }
    }
    return super.visitIfStatement(node);
  }

  // Do we want to report "pointless" or "obscure" code such as do {} !while (false);!
//@Override
//public Void visitDoStatement(DoStatement node) {
//  Expression conditionExpression = node.getCondition();
//  ValidResult result = getConstantBooleanValue(conditionExpression);
//  if (result != null) {
//    if (result == ValidResult.RESULT_FALSE) {
//      // report error on if block: do {} !while (false);!
//      int whileOffset = node.getWhileKeyword().getOffset();
//      int semiColonOffset = node.getSemicolon().getOffset() + 1;
//      int length = semiColonOffset - whileOffset;
//      errorReporter.reportError(HintCode.DEAD_CODE, whileOffset, length);
//    }
//  }
//  return super.visitDoStatement(node);
//}

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    checkForDeadStatementsInNodeList(node.getStatements());
    return super.visitSwitchCase(node);
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    checkForDeadStatementsInNodeList(node.getStatements());
    return super.visitSwitchDefault(node);
  }

  @Override
  public Void visitTryStatement(TryStatement node) {
    safelyVisit(node.getBody());
    safelyVisit(node.getFinallyBlock());
    NodeList<CatchClause> catchClauses = node.getCatchClauses();
    int numOfCatchClauses = catchClauses.size();
    ArrayList<Type> visitedTypes = new ArrayList<Type>(numOfCatchClauses);
    for (int i = 0; i < numOfCatchClauses; i++) {
      CatchClause catchClause = catchClauses.get(i);
      if (catchClause.getOnKeyword() != null) {
        // on-catch clause found, verify that the exception type is not a subtype of a previous
        // on-catch exception type
        TypeName typeName = catchClause.getExceptionType();
        if (typeName != null && typeName.getType() != null) {
          Type currentType = typeName.getType();
          if (currentType.isObject()) {
            // Found catch clause clause that has Object as an exception type, this is equivalent to
            // having a catch clause that doesn't have an exception type, visit the block, but
            // generate an error on any following catch clauses (and don't visit them).
            safelyVisit(catchClause);
            if (i + 1 != numOfCatchClauses) {
              // this catch clause is not the last in the try statement
              CatchClause nextCatchClause = catchClauses.get(i + 1);
              CatchClause lastCatchClause = catchClauses.get(numOfCatchClauses - 1);
              int offset = nextCatchClause.getOffset();
              int length = lastCatchClause.getEnd() - offset;
              errorReporter.reportErrorForOffset(
                  HintCode.DEAD_CODE_CATCH_FOLLOWING_CATCH,
                  offset,
                  length);
              return null;
            }
          }
          for (Type type : visitedTypes) {
            if (currentType.isSubtypeOf(type)) {
              CatchClause lastCatchClause = catchClauses.get(numOfCatchClauses - 1);
              int offset = catchClause.getOffset();
              int length = lastCatchClause.getEnd() - offset;
              errorReporter.reportErrorForOffset(
                  HintCode.DEAD_CODE_ON_CATCH_SUBTYPE,
                  offset,
                  length,
                  currentType.getDisplayName(),
                  type.getDisplayName());
              return null;
            }
          }
          visitedTypes.add(currentType);
        }
        safelyVisit(catchClause);
      } else {
        // Found catch clause clause that doesn't have an exception type, visit the block, but
        // generate an error on any following catch clauses (and don't visit them).
        safelyVisit(catchClause);
        if (i + 1 != numOfCatchClauses) {
          // this catch clause is not the last in the try statement
          CatchClause nextCatchClause = catchClauses.get(i + 1);
          CatchClause lastCatchClause = catchClauses.get(numOfCatchClauses - 1);
          int offset = nextCatchClause.getOffset();
          int length = lastCatchClause.getEnd() - offset;
          errorReporter.reportErrorForOffset(
              HintCode.DEAD_CODE_CATCH_FOLLOWING_CATCH,
              offset,
              length);
          return null;
        }
      }
    }
    return null;
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    Expression conditionExpression = node.getCondition();
    safelyVisit(conditionExpression);
    if (!isDebugConstant(conditionExpression)) {
      EvaluationResultImpl result = getConstantBooleanValue(conditionExpression);
      if (result != null) {
        if (result.getValue().isFalse()) {
          // report error on if block: while (false) {!}
          errorReporter.reportErrorForNode(HintCode.DEAD_CODE, node.getBody());
          return null;
        }
      }
    }
    safelyVisit(node.getBody());
    return null;
  }

  /**
   * Given some {@link NodeList} of {@link Statement}s, from either a {@link Block} or
   * {@link SwitchMember}, this loops through the list in reverse order searching for statements
   * after a return, unlabeled break or unlabeled continue statement to mark them as dead code.
   * 
   * @param statements some ordered list of statements in a {@link Block} or {@link SwitchMember}
   */
  private void checkForDeadStatementsInNodeList(NodeList<Statement> statements) {
    int size = statements.size();
    for (int i = 0; i < size; i++) {
      Statement currentStatement = statements.get(i);
      safelyVisit(currentStatement);
      boolean returnOrBreakingStatement = currentStatement instanceof ReturnStatement
          || (currentStatement instanceof BreakStatement && ((BreakStatement) currentStatement).getLabel() == null)
          || (currentStatement instanceof ContinueStatement && ((ContinueStatement) currentStatement).getLabel() == null);
      if (returnOrBreakingStatement && i != size - 1) {
        Statement nextStatement = statements.get(i + 1);
        Statement lastStatement = statements.get(size - 1);
        int offset = nextStatement.getOffset();
        int length = lastStatement.getEnd() - offset;
        errorReporter.reportErrorForOffset(HintCode.DEAD_CODE, offset, length);
        return;
      }
    }
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
  private EvaluationResultImpl getConstantBooleanValue(Expression expression) {
    if (expression instanceof BooleanLiteral) {
      if (((BooleanLiteral) expression).getValue()) {
        return new EvaluationResultImpl(new DartObjectImpl(null, BoolState.from(true)));
      } else {
        return new EvaluationResultImpl(new DartObjectImpl(null, BoolState.from(false)));
      }
    }
    // Don't consider situations where we could evaluate to a constant boolean expression with the
    // ConstantVisitor
//    else {
//      EvaluationResultImpl result = expression.accept(new ConstantVisitor());
//      if (result == ValidResult.RESULT_TRUE) {
//        return ValidResult.RESULT_TRUE;
//      } else if (result == ValidResult.RESULT_FALSE) {
//        return ValidResult.RESULT_FALSE;
//      }
//      return null;
//    }
    return null;
  }

  /**
   * Return {@code true} if and only if the passed expression is resolved to a constant variable.
   * 
   * @param expression some conditional expression
   * @return {@code true} if and only if the passed expression is resolved to a constant variable
   */
  private boolean isDebugConstant(Expression expression) {
    Element element = null;
    if (expression instanceof Identifier) {
      Identifier identifier = (Identifier) expression;
      element = identifier.getStaticElement();
    } else if (expression instanceof PropertyAccess) {
      PropertyAccess propertyAccess = (PropertyAccess) expression;
      element = propertyAccess.getPropertyName().getStaticElement();
    }
    if (element instanceof PropertyAccessorElement) {
      PropertyAccessorElement pae = (PropertyAccessorElement) element;
      PropertyInducingElement variable = pae.getVariable();
      return variable != null && variable.isConst();
    }
    return false;
  }

  /**
   * If the given node is not {@code null}, visit this instance of the dead code verifier.
   * 
   * @param node the node to be visited
   */
  private void safelyVisit(AstNode node) {
    if (node != null) {
      node.accept(this);
    }
  }

}
