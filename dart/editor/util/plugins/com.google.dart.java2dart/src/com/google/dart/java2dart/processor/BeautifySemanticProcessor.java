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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;

import static com.google.dart.java2dart.util.TokenFactory.token;

/**
 * {@link SemanticProcessor} for making Dart AST cleaner and nicer.
 */
public class BeautifySemanticProcessor extends SemanticProcessor {
  private static boolean canRemovePathenthesis(ParenthesizedExpression node) {
    AstNode parent = node.getParent();
    // argument of invocation
    if (parent instanceof ArgumentList) {
      return true;
    }
    // list literal element
    if (parent instanceof ListLiteral) {
      return true;
    }
    // RHS of assignment
    if (parent instanceof AssignmentExpression) {
      AssignmentExpression assignment = (AssignmentExpression) parent;
      if (assignment.getRightHandSide() == node) {
        return true;
      }
    }
    // initializer
    if (parent instanceof VariableDeclaration) {
      VariableDeclaration declaration = (VariableDeclaration) parent;
      if (declaration.getInitializer() == node) {
        return true;
      }
    }
    // return statement
    if (parent instanceof ReturnStatement) {
      ReturnStatement returnStatement = (ReturnStatement) parent;
      if (returnStatement.getExpression() == node) {
        return true;
      }
    }
    if (parent instanceof ExpressionFunctionBody) {
      ExpressionFunctionBody body = (ExpressionFunctionBody) parent;
      if (body.getExpression() == node) {
        return true;
      }
    }
    // 'if' condition
    if (parent instanceof IfStatement) {
      return true;
    }
    // no
    return false;
  }

  public BeautifySemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
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
        // dig into () until found something different
        {
          ParenthesizedExpression node2 = node;
          while (node2.getExpression() instanceof ParenthesizedExpression) {
            node2 = (ParenthesizedExpression) node2.getExpression();
          }
          if (node2 != node) {
            replaceNode(node, node2);
            node = node2;
          }
        }
        // may be remove this last ()
        if (canRemovePathenthesis(node)) {
          replaceNode(node, node.getExpression());
        }
        // process expression
        return super.visitParenthesizedExpression(node);
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
