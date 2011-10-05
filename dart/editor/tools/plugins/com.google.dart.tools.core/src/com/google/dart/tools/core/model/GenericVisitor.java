/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.model;

import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartArrayLiteral;
import com.google.dart.compiler.ast.DartAssertion;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartBooleanLiteral;
import com.google.dart.compiler.ast.DartBreakStatement;
import com.google.dart.compiler.ast.DartCase;
import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartConditional;
import com.google.dart.compiler.ast.DartContext;
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
import com.google.dart.compiler.ast.DartNode;
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
import com.google.dart.compiler.ast.DartVisitor;
import com.google.dart.compiler.ast.DartWhileStatement;

/**
 * A visitor for Dart AST nodes that performs the same action on each node.
 */
public class GenericVisitor extends DartVisitor {
  @Override
  public void endVisit(DartArrayAccess x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartArrayLiteral x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartAssertion x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartBinaryExpression x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartBlock x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartBooleanLiteral x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartBreakStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartCase x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartCatchBlock x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartClass x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartConditional x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartContinueStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartDefault x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartDoubleLiteral x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartDoWhileStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartEmptyStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartExprStmt x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartField x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartFieldDefinition x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartForInStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartForStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartFunction x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartFunctionExpression x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartFunctionObjectInvocation x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartFunctionTypeAlias x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartIdentifier x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartIfStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartInitializer x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartIntegerLiteral x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartInvocation x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartLabel x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartMapLiteral x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartMapLiteralEntry x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartMethodDefinition x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartMethodInvocation x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartNativeBlock x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartNewExpression x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartNullLiteral x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartParameter x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartParenthesizedExpression x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartPropertyAccess x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartReturnStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartStringInterpolation x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartStringLiteral x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartSuperConstructorInvocation x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartSuperExpression x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartSwitchStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartThisExpression x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartThrowStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartTryStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartTypeExpression x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartTypeNode x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartTypeParameter x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartUnaryExpression x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartUnit x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartUnqualifiedInvocation x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartVariable x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartVariableStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public void endVisit(DartWhileStatement x, DartContext ctx) {
    endVisitNode(x, ctx);
  }

  @Override
  public boolean visit(DartArrayAccess x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartArrayLiteral x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartAssertion x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartBinaryExpression x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartBlock x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartBooleanLiteral x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartBreakStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartCase x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartCatchBlock x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartClass x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartConditional x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartContinueStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartDefault x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartDoubleLiteral x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartDoWhileStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartEmptyStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartExprStmt x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartField x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartFieldDefinition x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartForInStatement x, DartContext ctx) {
    return true;
  }

  @Override
  public boolean visit(DartForStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartFunction x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartFunctionExpression x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartFunctionObjectInvocation x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartFunctionTypeAlias x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartIdentifier x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartIfStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartInitializer x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartIntegerLiteral x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartInvocation x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartLabel x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartMapLiteral x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartMapLiteralEntry x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartMethodDefinition x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartMethodInvocation x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartNativeBlock x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartNewExpression x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartNullLiteral x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartParameter x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartParenthesizedExpression x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartPropertyAccess x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartReturnStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartStringInterpolation x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartStringLiteral x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartSuperConstructorInvocation x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartSuperExpression x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartSwitchStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartThisExpression x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartThrowStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartTryStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartTypeExpression x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartTypeNode x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartTypeParameter x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartUnaryExpression x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartUnit x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartUnqualifiedInvocation x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartVariable x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartVariableStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  @Override
  public boolean visit(DartWhileStatement x, DartContext ctx) {
    return visitNode(x, ctx);
  }

  /**
   * Subclasses should override this method to perform a generic end-of-visit operation for each
   * node.
   * 
   * @param node the node that is done being visited
   * @param context the context associated with the visit
   */
  protected void endVisitNode(DartNode node, DartContext context) {
  }

  /**
   * Subclasses should override this method to perform a generic operation for each node.
   * 
   * @param node the node that is now being visited
   * @param context the context associated with the visit
   * @return <code>true</code> if the children of the node should be visited
   */
  protected boolean visitNode(DartNode node, DartContext context) {
    return true;
  }

}
