/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartArrayLiteral;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartBooleanLiteral;
import com.google.dart.compiler.ast.DartBreakStatement;
import com.google.dart.compiler.ast.DartCase;
import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartConditional;
import com.google.dart.compiler.ast.DartContinueStatement;
import com.google.dart.compiler.ast.DartDefault;
import com.google.dart.compiler.ast.DartDoWhileStatement;
import com.google.dart.compiler.ast.DartDoubleLiteral;
import com.google.dart.compiler.ast.DartEmptyStatement;
import com.google.dart.compiler.ast.DartExprStmt;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartForInStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartInitializer;
import com.google.dart.compiler.ast.DartIntegerLiteral;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartLabel;
import com.google.dart.compiler.ast.DartMapLiteral;
import com.google.dart.compiler.ast.DartMapLiteralEntry;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNativeBlock;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNullLiteral;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartParenthesizedExpression;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartStringInterpolation;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartSuperExpression;
import com.google.dart.compiler.ast.DartSwitchStatement;
import com.google.dart.compiler.ast.DartThisExpression;
import com.google.dart.compiler.ast.DartThrowStatement;
import com.google.dart.compiler.ast.DartTryStatement;
import com.google.dart.compiler.ast.DartTypeExpression;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartTypeParameter;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.ast.DartWhileStatement;

/**
 * Base class that can be extended to visit a single AST node, calling a specific method on it.
 */
public class NodeClassifier extends ASTVisitor<Void> {
  @Override
  public Void visitArrayAccess(DartArrayAccess node) {
    return null;
  }

  @Override
  public Void visitArrayLiteral(DartArrayLiteral node) {
    return null;
  }

  @Override
  public Void visitBinaryExpression(DartBinaryExpression node) {
    return null;
  }

  @Override
  public Void visitBlock(DartBlock node) {
    return null;
  }

  @Override
  public Void visitBooleanLiteral(DartBooleanLiteral node) {
    return null;
  }

  @Override
  public Void visitBreakStatement(DartBreakStatement node) {
    return null;
  }

  @Override
  public Void visitCase(DartCase node) {
    return null;
  }

  @Override
  public Void visitCatchBlock(DartCatchBlock node) {
    return null;
  }

  @Override
  public Void visitClass(DartClass node) {
    return null;
  }

  @Override
  public Void visitConditional(DartConditional node) {
    return null;
  }

  @Override
  public Void visitContinueStatement(DartContinueStatement node) {
    return null;
  }

  @Override
  public Void visitDefault(DartDefault node) {
    return null;
  }

  @Override
  public Void visitDoubleLiteral(DartDoubleLiteral node) {
    return null;
  }

  @Override
  public Void visitDoWhileStatement(DartDoWhileStatement node) {
    return null;
  }

  @Override
  public Void visitEmptyStatement(DartEmptyStatement node) {
    return null;
  }

  @Override
  public Void visitExprStmt(DartExprStmt node) {
    return null;
  }

  @Override
  public Void visitField(DartField node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitFieldDefinition(DartFieldDefinition node) {
    return null;
  }

  @Override
  public Void visitForInStatement(DartForInStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitForStatement(DartForStatement node) {
    return null;
  }

  @Override
  public Void visitFunction(DartFunction node) {
    return null;
  }

  @Override
  public Void visitFunctionExpression(DartFunctionExpression node) {
    return null;
  }

  @Override
  public Void visitFunctionObjectInvocation(DartFunctionObjectInvocation node) {
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitIdentifier(DartIdentifier node) {
    return null;
  }

  @Override
  public Void visitIfStatement(DartIfStatement node) {
    return null;
  }

  @Override
  public Void visitInitializer(DartInitializer node) {
    return null;
  }

  @Override
  public Void visitIntegerLiteral(DartIntegerLiteral node) {
    return null;
  }

  @Override
  public Void visitInvocation(DartInvocation node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitLabel(DartLabel node) {
    return null;
  }

  @Override
  public Void visitMapLiteral(DartMapLiteral node) {
    return null;
  }

  @Override
  public Void visitMapLiteralEntry(DartMapLiteralEntry node) {
    return null;
  }

  @Override
  public Void visitMethodDefinition(DartMethodDefinition node) {
    return null;
  }

  @Override
  public Void visitMethodInvocation(DartMethodInvocation node) {
    return null;
  }

  @Override
  public Void visitNativeBlock(DartNativeBlock node) {
    return null;
  }

  @Override
  public Void visitNewExpression(DartNewExpression node) {
    return null;
  }

  @Override
  public Void visitNullLiteral(DartNullLiteral node) {
    return null;
  }

  @Override
  public Void visitParameter(DartParameter node) {
    return null;
  }

  @Override
  public Void visitParenthesizedExpression(DartParenthesizedExpression node) {
    return null;
  }

  @Override
  public Void visitPropertyAccess(DartPropertyAccess node) {
    return null;
  }

  @Override
  public Void visitReturnStatement(DartReturnStatement node) {
    return null;
  }

  @Override
  public Void visitStringInterpolation(DartStringInterpolation node) {
    return null;
  }

  @Override
  public Void visitStringLiteral(DartStringLiteral node) {
    return null;
  }

  @Override
  public Void visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
    return null;
  }

  @Override
  public Void visitSuperExpression(DartSuperExpression node) {
    return null;
  }

  @Override
  public Void visitSwitchStatement(DartSwitchStatement node) {
    return null;
  }

  @Override
  public Void visitThisExpression(DartThisExpression node) {
    return null;
  }

  @Override
  public Void visitThrowStatement(DartThrowStatement node) {
    return null;
  }

  @Override
  public Void visitTryStatement(DartTryStatement node) {
    return null;
  }

  @Override
  public Void visitTypeExpression(DartTypeExpression node) {
    return null;
  }

  @Override
  public Void visitTypeNode(DartTypeNode node) {
    return null;
  }

  @Override
  public Void visitTypeParameter(DartTypeParameter node) {
    return null;
  }

  @Override
  public Void visitUnaryExpression(DartUnaryExpression node) {
    return null;
  }

  @Override
  public Void visitUnit(DartUnit node) {
    return null;
  }

  @Override
  public Void visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
    return null;
  }

  @Override
  public Void visitVariable(DartVariable node) {
    return null;
  }

  @Override
  public Void visitVariableStatement(DartVariableStatement node) {
    return null;
  }

  @Override
  public Void visitWhileStatement(DartWhileStatement node) {
    return null;
  }
}
