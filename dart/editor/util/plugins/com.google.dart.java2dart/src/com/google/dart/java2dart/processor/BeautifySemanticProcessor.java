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

package com.google.dart.java2dart.processor;

import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;

import static com.google.dart.java2dart.util.TokenFactory.token;

/**
 * {@link SemanticProcessor} for making Dart AST cleaner and nicer.
 */
public class BeautifySemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new BeautifySemanticProcessor();

  private static boolean canRemovePathenthesis(ParenthesizedExpression node) {
    // argument of invocation
    if (node.getParent() instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) node.getParent();
      if (invocation.getArgumentList().getArguments().contains(node)) {
        return true;
      }
    }
    // RHS of assignment
    if (node.getParent() instanceof AssignmentExpression) {
      AssignmentExpression assignment = (AssignmentExpression) node.getParent();
      if (assignment.getRightHandSide() == node) {
        return true;
      }
    }
    // initializer
    if (node.getParent() instanceof VariableDeclaration) {
      VariableDeclaration declaration = (VariableDeclaration) node.getParent();
      if (declaration.getInitializer() == node) {
        return true;
      }
    }
    // return statement
    if (node.getParent() instanceof ReturnStatement) {
      ReturnStatement returnStatement = (ReturnStatement) node.getParent();
      if (returnStatement.getExpression() == node) {
        return true;
      }
    }
    if (node.getParent() instanceof ExpressionFunctionBody) {
      ExpressionFunctionBody body = (ExpressionFunctionBody) node.getParent();
      if (body.getExpression() == node) {
        return true;
      }
    }
    // no
    return false;
  }

  @Override
  public void process(final Context context, CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitAsExpression(AsExpression node) {
        super.visitAsExpression(node);
        if (node.getExpression() instanceof IntegerLiteral
            && node.getType().getName().getName().equals("int")) {
          replaceNode(node, node.getExpression());
        }
        return null;
      }

      @Override
      public Void visitParenthesizedExpression(ParenthesizedExpression node) {
        super.visitParenthesizedExpression(node);
        if (canRemovePathenthesis(node)) {
          replaceNode(node, node.getExpression());
          return null;
        }
        return null;
      }

      @Override
      public Void visitPrefixExpression(PrefixExpression node) {
        super.visitPrefixExpression(node);
        if (node.getOperator().getType() == TokenType.BANG) {
          if (node.getOperand() instanceof ParenthesizedExpression) {
            ParenthesizedExpression parExpression = (ParenthesizedExpression) node.getOperand();
            if (parExpression.getExpression() instanceof IsExpression) {
              IsExpression isExpression = (IsExpression) parExpression.getExpression();
              if (isExpression.getNotOperator() == null) {
                isExpression.setNotOperator(token(TokenType.BANG));
                replaceNode(node, isExpression);
              }
            }
          }
        }
        return null;
      }
    });
  }
}
