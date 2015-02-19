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

import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.AwaitExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CascadeExpression;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.EmptyStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FunctionDeclarationStatement;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.Literal;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RethrowExpression;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code ExitDetector} determine whether the visited AST node is guaranteed
 * to terminate by executing a {@code return} statement, {@code throw} expression, {@code rethrow}
 * expression, or simple infinite loop such as {@code while(true)}.
 */
public class ExitDetector extends GeneralizingAstVisitor<Boolean> {
  /**
   * Return {@code true} if the given {@link AstNode} exits.
   */
  public static boolean exits(AstNode node) {
    return new ExitDetector().nodeExits(node);
  }

  /**
   * Set to {@code true} when a {@code break} is encountered, and reset to {@code false} when a
   * {@code do}, {@code while}, {@code for} or {@code switch} block is entered.
   */
  // TODO (jwren) This support only covers a subset of cases.  Labeled breaks and continues aren't
  // covered.
  private boolean enclosingBlockContainsBreak = false;

  /**
   * Initialize a newly created return detector.
   */
  public ExitDetector() {
    super();
  }

  @Override
  public Boolean visitArgumentList(ArgumentList node) {
    return visitExpressions(node.getArguments());
  }

  @Override
  public Boolean visitAsExpression(AsExpression node) {
    return nodeExits(node.getExpression());
  }

  @Override
  public Boolean visitAssertStatement(AssertStatement node) {
    return nodeExits(node.getCondition());
  }

  @Override
  public Boolean visitAssignmentExpression(AssignmentExpression node) {
    return nodeExits(node.getLeftHandSide()) || nodeExits(node.getRightHandSide());
  }

  @Override
  public Boolean visitAwaitExpression(AwaitExpression node) {
    return nodeExits(node.getExpression());
  }

  @Override
  public Boolean visitBinaryExpression(BinaryExpression node) {
    Expression lhsExpression = node.getLeftOperand();
    TokenType operatorType = node.getOperator().getType();
    // If the operator is || and the left hand side is false literal, don't consider the RHS of the
    // binary expression.
    // TODO(jwren) Do we want to take constant expressions into account, evaluate if(false) {}
    // differently than if(<condition>), when <condition> evaluates to a constant false value?
    if (operatorType == TokenType.BAR_BAR) {
      if (lhsExpression instanceof BooleanLiteral) {
        BooleanLiteral booleanLiteral = (BooleanLiteral) lhsExpression;
        if (!booleanLiteral.getValue()) {
          return false;
        }
      }
    }
    // If the operator is && and the left hand side is true literal, don't consider the RHS of the
    // binary expression.
    if (operatorType == TokenType.AMPERSAND_AMPERSAND) {
      if (lhsExpression instanceof BooleanLiteral) {
        BooleanLiteral booleanLiteral = (BooleanLiteral) lhsExpression;
        if (booleanLiteral.getValue()) {
          return false;
        }
      }
    }
    Expression rhsExpression = node.getRightOperand();
    return nodeExits(lhsExpression) || nodeExits(rhsExpression);
  }

  @Override
  public Boolean visitBlock(Block node) {
    return visitStatements(node.getStatements());
  }

  @Override
  public Boolean visitBlockFunctionBody(BlockFunctionBody node) {
    return nodeExits(node.getBlock());
  }

  @Override
  public Boolean visitBreakStatement(BreakStatement node) {
    enclosingBlockContainsBreak = true;
    return false;
  }

  @Override
  public Boolean visitCascadeExpression(CascadeExpression node) {
    return nodeExits(node.getTarget()) || visitExpressions(node.getCascadeSections());
  }

  @Override
  public Boolean visitConditionalExpression(ConditionalExpression node) {
    Expression conditionExpression = node.getCondition();
    Expression thenStatement = node.getThenExpression();
    Expression elseStatement = node.getElseExpression();
    // TODO(jwren) Do we want to take constant expressions into account, evaluate if(false) {}
    // differently than if(<condition>), when <condition> evaluates to a constant false value?
    if (nodeExits(conditionExpression)) {
      return true;
    }
    if (thenStatement == null || elseStatement == null) {
      return false;
    }
    return thenStatement.accept(this) && elseStatement.accept(this);
  }

  @Override
  public Boolean visitContinueStatement(ContinueStatement node) {
    return false;
  }

  @Override
  public Boolean visitDoStatement(DoStatement node) {
    boolean outerBreakValue = enclosingBlockContainsBreak;
    enclosingBlockContainsBreak = false;
    try {
      Expression conditionExpression = node.getCondition();
      if (nodeExits(conditionExpression)) {
        return true;
      }
      // TODO(jwren) Do we want to take all constant expressions into account?
      if (conditionExpression instanceof BooleanLiteral) {
        BooleanLiteral booleanLiteral = (BooleanLiteral) conditionExpression;
        // If do {} while (true), and the body doesn't return or the body doesn't have a break, then
        // return true.
        boolean blockReturns = nodeExits(node.getBody());
        if (booleanLiteral.getValue() && (blockReturns || !enclosingBlockContainsBreak)) {
          return true;
        }
      }
      return false;
    } finally {
      enclosingBlockContainsBreak = outerBreakValue;
    }
  }

  @Override
  public Boolean visitEmptyStatement(EmptyStatement node) {
    return false;
  }

  @Override
  public Boolean visitExpressionStatement(ExpressionStatement node) {
    return nodeExits(node.getExpression());
  }

  @Override
  public Boolean visitForEachStatement(ForEachStatement node) {
    boolean outerBreakValue = enclosingBlockContainsBreak;
    enclosingBlockContainsBreak = false;
    try {
      return nodeExits(node.getIterator());
    } finally {
      enclosingBlockContainsBreak = outerBreakValue;
    }
  }

  @Override
  public Boolean visitForStatement(ForStatement node) {
    boolean outerBreakValue = enclosingBlockContainsBreak;
    enclosingBlockContainsBreak = false;
    try {
      if (node.getVariables() != null
          && visitVariableDeclarations(node.getVariables().getVariables())) {
        return true;
      }
      if (node.getInitialization() != null && nodeExits(node.getInitialization())) {
        return true;
      }
      Expression conditionExpression = node.getCondition();
      if (conditionExpression != null && nodeExits(conditionExpression)) {
        return true;
      }
      if (visitExpressions(node.getUpdaters())) {
        return true;
      }
      // TODO(jwren) Do we want to take all constant expressions into account?
      // If for(; true; ) (or for(;;)), and the body doesn't return or the body doesn't have a
      // break, then return true.
      boolean implicitOrExplictTrue = conditionExpression == null
          || (conditionExpression instanceof BooleanLiteral && ((BooleanLiteral) conditionExpression).getValue());
      if (implicitOrExplictTrue) {
        boolean blockReturns = nodeExits(node.getBody());
        if (blockReturns || !enclosingBlockContainsBreak) {
          return true;
        }
      }
      return false;
    } finally {
      enclosingBlockContainsBreak = outerBreakValue;
    }
  }

  @Override
  public Boolean visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    return false;
  }

  @Override
  public Boolean visitFunctionExpression(FunctionExpression node) {
    return false;
  }

  @Override
  public Boolean visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    if (nodeExits(node.getFunction())) {
      return true;
    }
    return node.getArgumentList().accept(this);
  }

  @Override
  public Boolean visitIdentifier(Identifier node) {
    return false;
  }

  @Override
  public Boolean visitIfStatement(IfStatement node) {
    Expression conditionExpression = node.getCondition();
    Statement thenStatement = node.getThenStatement();
    Statement elseStatement = node.getElseStatement();
    if (nodeExits(conditionExpression)) {
      return true;
    }
    // TODO(jwren) Do we want to take all constant expressions into account?
    if (conditionExpression instanceof BooleanLiteral) {
      BooleanLiteral booleanLiteral = (BooleanLiteral) conditionExpression;
      if (booleanLiteral.getValue()) {
        // if(true) ...
        return nodeExits(thenStatement);
      } else if (elseStatement != null) {
        // if (false) ...
        return nodeExits(elseStatement);
      }
    }
    if (thenStatement == null || elseStatement == null) {
      return false;
    }
    return nodeExits(thenStatement) && nodeExits(elseStatement);
  }

  @Override
  public Boolean visitIndexExpression(IndexExpression node) {
    Expression target = node.getRealTarget();
    if (nodeExits(target)) {
      return true;
    }
    if (nodeExits(node.getIndex())) {
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitInstanceCreationExpression(InstanceCreationExpression node) {
    return nodeExits(node.getArgumentList());
  }

  @Override
  public Boolean visitIsExpression(IsExpression node) {
    return node.getExpression().accept(this);
  }

  @Override
  public Boolean visitLabel(Label node) {
    return false;
  }

  @Override
  public Boolean visitLabeledStatement(LabeledStatement node) {
    return node.getStatement().accept(this);
  }

  @Override
  public Boolean visitLiteral(Literal node) {
    return false;
  }

  @Override
  public Boolean visitMethodInvocation(MethodInvocation node) {
    Expression target = node.getRealTarget();
    if (target != null && target.accept(this)) {
      return true;
    }
    return nodeExits(node.getArgumentList());
  }

  @Override
  public Boolean visitNamedExpression(NamedExpression node) {
    return node.getExpression().accept(this);
  }

  @Override
  public Boolean visitParenthesizedExpression(ParenthesizedExpression node) {
    return node.getExpression().accept(this);
  }

  @Override
  public Boolean visitPostfixExpression(PostfixExpression node) {
    return false;
  }

  @Override
  public Boolean visitPrefixExpression(PrefixExpression node) {
    return false;
  }

  @Override
  public Boolean visitPropertyAccess(PropertyAccess node) {
    Expression target = node.getRealTarget();
    if (target != null && target.accept(this)) {
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitRethrowExpression(RethrowExpression node) {
    return true;
  }

  @Override
  public Boolean visitReturnStatement(ReturnStatement node) {
    return true;
  }

  @Override
  public Boolean visitSuperExpression(SuperExpression node) {
    return false;
  }

  @Override
  public Boolean visitSwitchCase(SwitchCase node) {
    return visitStatements(node.getStatements());
  }

  @Override
  public Boolean visitSwitchDefault(SwitchDefault node) {
    return visitStatements(node.getStatements());
  }

  @Override
  public Boolean visitSwitchStatement(SwitchStatement node) {
    boolean outerBreakValue = enclosingBlockContainsBreak;
    enclosingBlockContainsBreak = false;
    try {
      boolean hasDefault = false;
      NodeList<SwitchMember> memberList = node.getMembers();
      SwitchMember[] members = memberList.toArray(new SwitchMember[memberList.size()]);
      for (int i = 0; i < members.length; i++) {
        SwitchMember switchMember = members[i];
        if (switchMember instanceof SwitchDefault) {
          hasDefault = true;
          // If this is the last member and there are no statements, return false
          if (switchMember.getStatements().isEmpty() && i + 1 == members.length) {
            return false;
          }
        }
        // For switch members with no statements, don't visit the children, otherwise, return false if
        // no return is found in the children statements
        if (!switchMember.getStatements().isEmpty() && !switchMember.accept(this)) {
          return false;
        }
      }
      // All of the members exit, determine whether there are possible cases
      // that are not caught by the members.
      Type type = node.getExpression() == null ? null : node.getExpression().getBestType();
      if (type instanceof InterfaceType) {
        InterfaceType interfaceType = (InterfaceType) type;
        ClassElement element = interfaceType.getElement();
        if (element != null && element.isEnum()) {
          // If some of the enum values are not covered, then a warning will
          // have already been generated, so there's no point in generating a
          // hint.
          return true;
        }
      }
      return hasDefault;
    } finally {
      enclosingBlockContainsBreak = outerBreakValue;
    }
  }

  @Override
  public Boolean visitThisExpression(ThisExpression node) {
    return false;
  }

  @Override
  public Boolean visitThrowExpression(ThrowExpression node) {
    return true;
  }

  @Override
  public Boolean visitTryStatement(TryStatement node) {
    if (nodeExits(node.getBody())) {
      return true;
    }
    Block finallyBlock = node.getFinallyBlock();
    if (nodeExits(finallyBlock)) {
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitTypeName(TypeName node) {
    return false;
  }

  @Override
  public Boolean visitVariableDeclaration(VariableDeclaration node) {
    Expression initializer = node.getInitializer();
    if (initializer != null) {
      return initializer.accept(this);
    }
    return false;
  }

  @Override
  public Boolean visitVariableDeclarationList(VariableDeclarationList node) {
    return visitVariableDeclarations(node.getVariables());
  }

  @Override
  public Boolean visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    NodeList<VariableDeclaration> variables = node.getVariables().getVariables();
    for (int i = 0; i < variables.size(); i++) {
      if (variables.get(i).accept(this)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitWhileStatement(WhileStatement node) {
    boolean outerBreakValue = enclosingBlockContainsBreak;
    enclosingBlockContainsBreak = false;
    try {
      Expression conditionExpression = node.getCondition();
      if (conditionExpression.accept(this)) {
        return true;
      }
      // TODO(jwren) Do we want to take all constant expressions into account?
      if (conditionExpression instanceof BooleanLiteral) {
        BooleanLiteral booleanLiteral = (BooleanLiteral) conditionExpression;
        // If while(true), and the body doesn't return or the body doesn't have a break, then return
        // true.
        boolean blockReturns = node.getBody().accept(this);
        if (booleanLiteral.getValue() && (blockReturns || !enclosingBlockContainsBreak)) {
          return true;
        }
      }
      return false;
    } finally {
      enclosingBlockContainsBreak = outerBreakValue;
    }
  }

  /**
   * Return {@code true} if the given node exits.
   * 
   * @param node the node being tested
   * @return {@code true} if the given node exits
   */
  private boolean nodeExits(AstNode node) {
    if (node == null) {
      return false;
    }
    return node.accept(this);
  }

  private boolean visitExpressions(NodeList<Expression> expressions) {
    for (int i = expressions.size() - 1; i >= 0; i--) {
      if (expressions.get(i).accept(this)) {
        return true;
      }
    }
    return false;
  }

  private boolean visitStatements(NodeList<Statement> statements) {
    for (int i = statements.size() - 1; i >= 0; i--) {
      if (statements.get(i).accept(this)) {
        return true;
      }
    }
    return false;
  }

  private boolean visitVariableDeclarations(NodeList<VariableDeclaration> variableDeclarations) {
    for (int i = variableDeclarations.size() - 1; i >= 0; i--) {
      if (variableDeclarations.get(i).accept(this)) {
        return true;
      }
    }
    return false;
  }
}
