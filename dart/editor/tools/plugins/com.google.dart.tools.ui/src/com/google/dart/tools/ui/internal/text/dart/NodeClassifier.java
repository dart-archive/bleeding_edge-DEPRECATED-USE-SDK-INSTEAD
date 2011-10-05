/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

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
 * Base class that can be extended to visit a single AST node, calling a specific method on it.
 */
public class NodeClassifier extends DartVisitor {

  @Override
  public boolean visit(DartArrayAccess x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartArrayLiteral x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartAssertion node, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartBinaryExpression x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartBlock x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartBooleanLiteral x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartBreakStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartCase x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartCatchBlock x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartClass x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartConditional x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartContinueStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartDefault x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartDoubleLiteral x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartDoWhileStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartEmptyStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartExprStmt x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartField x, DartContext ctx) {
    return true;
  }

  @Override
  public boolean visit(DartFieldDefinition x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartForInStatement x, DartContext ctx) {
    return true;
  }

  @Override
  public boolean visit(DartForStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartFunction x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartFunctionExpression x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartFunctionObjectInvocation node, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartFunctionTypeAlias node, DartContext ctx) {
    return true;
  }

  @Override
  public boolean visit(DartIdentifier x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartIfStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartInitializer x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartIntegerLiteral x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartInvocation x, DartContext ctx) {
    return true;
  }

  @Override
  public boolean visit(DartLabel x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartMapLiteral x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartMapLiteralEntry x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartMethodDefinition x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartMethodInvocation node, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartNativeBlock x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartNewExpression x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartNullLiteral x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartParameter x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartParenthesizedExpression x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartPropertyAccess x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartReturnStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartStringInterpolation node, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartStringLiteral x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartSuperConstructorInvocation node, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartSuperExpression x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartSwitchStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartThisExpression x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartThrowStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartTryStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartTypeExpression x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartTypeNode x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartTypeParameter x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartUnaryExpression x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartUnit x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartUnqualifiedInvocation node, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartVariable x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartVariableStatement x, DartContext ctx) {
    return false;
  }

  @Override
  public boolean visit(DartWhileStatement x, DartContext ctx) {
    return false;
  }
}
