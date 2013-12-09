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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.visitor.UnifyingASTVisitor;

/**
 * Instances of the class {@code ReturnDetector} determine whether the visited AST node is
 * guaranteed (modulo exceptions) to terminate by executing a return statement.
 */
public class ReturnDetector extends UnifyingASTVisitor<Boolean> {
  /**
   * Initialize a newly created return detector.
   */
  public ReturnDetector() {
    super();
  }

  @Override
  public Boolean visitBlock(Block node) {
    return visitStatements(node.getStatements());
  }

  @Override
  public Boolean visitBlockFunctionBody(BlockFunctionBody node) {
    return node.getBlock().accept(this);
  }

  @Override
  public Boolean visitIfStatement(IfStatement node) {
    Statement thenStatement = node.getThenStatement();
    Statement elseStatement = node.getElseStatement();
    if (thenStatement == null || elseStatement == null) {
      return false;
    }
    return thenStatement.accept(this) && elseStatement.accept(this);
  }

  @Override
  public Boolean visitNode(ASTNode node) {
    return false;
  }

  @Override
  public Boolean visitReturnStatement(ReturnStatement node) {
    return true;
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
    boolean hasDefault = false;
    for (SwitchMember member : node.getMembers()) {
      if (!member.accept(this)) {
        return false;
      }
      if (member instanceof SwitchDefault) {
        hasDefault = true;
      }
    }
    return hasDefault;
  }

  private boolean visitStatements(NodeList<Statement> statements) {
    for (int i = statements.size() - 1; i >= 0; i--) {
      if (statements.get(i).accept(this)) {
        return true;
      }
    }
    return false;
  }
}
