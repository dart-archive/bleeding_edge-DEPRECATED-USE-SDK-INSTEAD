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
package com.google.dart.tools.core.dom.visitor;

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
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartInitializer;
import com.google.dart.compiler.ast.DartIntegerLiteral;
import com.google.dart.compiler.ast.DartLabel;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartMapLiteral;
import com.google.dart.compiler.ast.DartMapLiteralEntry;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNamedExpression;
import com.google.dart.compiler.ast.DartNativeBlock;
import com.google.dart.compiler.ast.DartNativeDirective;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNullLiteral;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartParameterizedTypeNode;
import com.google.dart.compiler.ast.DartParenthesizedExpression;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartRedirectConstructorInvocation;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringInterpolation;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartSuperExpression;
import com.google.dart.compiler.ast.DartSwitchStatement;
import com.google.dart.compiler.ast.DartSyntheticErrorExpression;
import com.google.dart.compiler.ast.DartSyntheticErrorStatement;
import com.google.dart.compiler.ast.DartThisExpression;
import com.google.dart.compiler.ast.DartThrowExpression;
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

import java.util.List;

/**
 * Instances of the class <code>WrappedDartVisitorAdaptor</code> adapt a {@link WrappedDartVisitor} to be a plain visitor,
 * causing the {@link WrappedDartVisitor#preVisit(DartNode)} and
 * {@link WrappedDartVisitor#postVisit(DartNode)} methods to be invoked.
 */
public class WrappedDartVisitorAdaptor<R> extends ASTVisitor<R> {
  /**
   * The wrapped visitor being adapted to a plain visitor.
   */
  private WrappedDartVisitor<R> baseVisitor;

  /**
   * Initialize a newly created adapter to adapt the given wrapped visitor to be a plain visitor.
   * 
   * @param baseVisitor the wrapped visitor being adapted to a plain visitor
   */
  public WrappedDartVisitorAdaptor(WrappedDartVisitor<R> baseVisitor) {
    this.baseVisitor = baseVisitor;
  }

  @Override
  public void visit(List<? extends DartNode> nodes) {
    if (nodes != null) {
      for (DartNode node : nodes) {
        node.accept(this);
      }
    }
  }

  @Override
  public R visitArrayAccess(DartArrayAccess node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitArrayAccess(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitArrayLiteral(DartArrayLiteral node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitArrayLiteral(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitBinaryExpression(DartBinaryExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitBinaryExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitBlock(DartBlock node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitBlock(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitBooleanLiteral(DartBooleanLiteral node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitBooleanLiteral(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitBreakStatement(DartBreakStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitBreakStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitCase(DartCase node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitCase(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitCatchBlock(DartCatchBlock node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitCatchBlock(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitClass(DartClass node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitClass(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitConditional(DartConditional node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitConditional(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitContinueStatement(DartContinueStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitContinueStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitDefault(DartDefault node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitDefault(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitDoubleLiteral(DartDoubleLiteral node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitDoubleLiteral(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitDoWhileStatement(DartDoWhileStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitDoWhileStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitEmptyStatement(DartEmptyStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitEmptyStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitExprStmt(DartExprStmt node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitExprStmt(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitField(DartField node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitField(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitFieldDefinition(DartFieldDefinition node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitFieldDefinition(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitForInStatement(DartForInStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitForInStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitForStatement(DartForStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitForStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitFunction(DartFunction node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitFunction(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitFunctionExpression(DartFunctionExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitFunctionExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitFunctionObjectInvocation(DartFunctionObjectInvocation node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitFunctionObjectInvocation(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitFunctionTypeAlias(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitIdentifier(DartIdentifier node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitIdentifier(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitIfStatement(DartIfStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitIfStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitImportDirective(DartImportDirective node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitImportDirective(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitInitializer(DartInitializer node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitInitializer(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitIntegerLiteral(DartIntegerLiteral node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitIntegerLiteral(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitLabel(DartLabel node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitLabel(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitLibraryDirective(DartLibraryDirective node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitLibraryDirective(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitMapLiteral(DartMapLiteral node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitMapLiteral(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitMapLiteralEntry(DartMapLiteralEntry node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitMapLiteralEntry(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitMethodDefinition(DartMethodDefinition node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitMethodDefinition(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitMethodInvocation(DartMethodInvocation node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitMethodInvocation(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitNamedExpression(DartNamedExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitNamedExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitNativeBlock(DartNativeBlock node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitNativeBlock(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitNativeDirective(DartNativeDirective node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitNativeDirective(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitNewExpression(DartNewExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitNewExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitNullLiteral(DartNullLiteral node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitNullLiteral(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitParameter(DartParameter node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitParameter(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitParameterizedTypeNode(DartParameterizedTypeNode node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitParameterizedTypeNode(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitParenthesizedExpression(DartParenthesizedExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitParenthesizedExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitPropertyAccess(DartPropertyAccess node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitPropertyAccess(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitRedirectConstructorInvocation(DartRedirectConstructorInvocation node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitRedirectConstructorInvocation(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitReturnStatement(DartReturnStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitReturnStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitSourceDirective(DartSourceDirective node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitSourceDirective(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitStringInterpolation(DartStringInterpolation node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitStringInterpolation(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitStringLiteral(DartStringLiteral node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitStringLiteral(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitSuperConstructorInvocation(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitSuperExpression(DartSuperExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitSuperExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitSwitchStatement(DartSwitchStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitSwitchStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitSyntheticErrorExpression(DartSyntheticErrorExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitSyntheticErrorExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitSyntheticErrorStatement(DartSyntheticErrorStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitSyntheticErrorStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitThisExpression(DartThisExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitThisExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitThrowExpression(DartThrowExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitThrowExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitTryStatement(DartTryStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitTryStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitTypeExpression(DartTypeExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitTypeExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitTypeNode(DartTypeNode node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitTypeNode(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitTypeParameter(DartTypeParameter node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitTypeParameter(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitUnaryExpression(DartUnaryExpression node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitUnaryExpression(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitUnit(DartUnit node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitUnit(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitUnqualifiedInvocation(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitVariable(DartVariable node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitVariable(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitVariableStatement(DartVariableStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitVariableStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }

  @Override
  public R visitWhileStatement(DartWhileStatement node) {
    baseVisitor.preVisit(node);
    R result = baseVisitor.visitWhileStatement(node);
    baseVisitor.postVisit(node);
    return result;
  }
}
