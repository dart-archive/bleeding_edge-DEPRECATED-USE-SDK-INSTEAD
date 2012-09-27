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
package com.google.dart.tools.core.dom;

import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartArrayLiteral;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartBooleanLiteral;
import com.google.dart.compiler.ast.DartBreakStatement;
import com.google.dart.compiler.ast.DartCase;
import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartComment;
import com.google.dart.compiler.ast.DartConditional;
import com.google.dart.compiler.ast.DartContinueStatement;
import com.google.dart.compiler.ast.DartDefault;
import com.google.dart.compiler.ast.DartDoWhileStatement;
import com.google.dart.compiler.ast.DartDoubleLiteral;
import com.google.dart.compiler.ast.DartEmptyStatement;
import com.google.dart.compiler.ast.DartExprStmt;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartInitializer;
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
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartStringInterpolation;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartSuperExpression;
import com.google.dart.compiler.ast.DartSwitchMember;
import com.google.dart.compiler.ast.DartSwitchStatement;
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
import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.tools.core.DartCore;

import java.util.ArrayList;

/**
 * Instances of the class <code>AST</code>
 */
public class AST {
  /**
   * Create a new node with the given type.
   * 
   * @param nodeClass the class of the node to be created
   * @return the node that was created
   */
  @SuppressWarnings("unchecked")
  public <N extends DartNode> N createInstance(Class<N> nodeClass) {
    if (nodeClass == DartArrayAccess.class) {
      return (N) new DartArrayAccess(null, null);
    } else if (nodeClass == DartArrayLiteral.class) {
      return (N) new DartArrayLiteral(false, null, new ArrayList<DartExpression>());
    } else if (nodeClass == DartBinaryExpression.class) {
      return (N) new DartBinaryExpression(null, 0, null, null);
    } else if (nodeClass == DartBlock.class) {
      return (N) new DartBlock(new ArrayList<DartStatement>());
    } else if (nodeClass == DartBooleanLiteral.class) {
      return (N) DartBooleanLiteral.get(false);
    } else if (nodeClass == DartBreakStatement.class) {
      return (N) new DartBreakStatement(null);
    } else if (nodeClass == DartCase.class) {
      return (N) new DartCase(null, new ArrayList<DartLabel>(), new ArrayList<DartStatement>());
    } else if (nodeClass == DartCatchBlock.class) {
      return (N) new DartCatchBlock(null, null, null);
    } else if (nodeClass == DartClass.class) {
      return (N) new DartClass(
          null,
          null,
          null,
          new ArrayList<DartTypeNode>(),
          -1,
          -1,
          new ArrayList<DartNode>(),
          new ArrayList<DartTypeParameter>(),
          null,
          false,
          Modifiers.NONE);
    } else if (nodeClass == DartComment.class) {
      return (N) new DartComment(null, 0, 0, null);
    } else if (nodeClass == DartConditional.class) {
      return (N) new DartConditional(null, null, null);
    } else if (nodeClass == DartContinueStatement.class) {
      return (N) new DartContinueStatement(null);
    } else if (nodeClass == DartDefault.class) {
      return (N) new DartDefault(new ArrayList<DartLabel>(), new ArrayList<DartStatement>());
    } else if (nodeClass == DartDoubleLiteral.class) {
      return (N) DartDoubleLiteral.get(0.0);
    } else if (nodeClass == DartDoWhileStatement.class) {
      return (N) new DartDoWhileStatement(null, null);
    } else if (nodeClass == DartEmptyStatement.class) {
      return (N) new DartEmptyStatement();
    } else if (nodeClass == DartExprStmt.class) {
      return (N) new DartExprStmt(null);
    } else if (nodeClass == DartField.class) {
      return (N) new DartField(null, null, null, null);
    } else if (nodeClass == DartFieldDefinition.class) {
      return (N) new DartFieldDefinition(null, new ArrayList<DartField>());
    } else if (nodeClass == DartForStatement.class) {
      return (N) new DartForStatement(null, null, null, 0, null);
    } else if (nodeClass == DartFunction.class) {
      return (N) new DartFunction(new ArrayList<DartParameter>(), -1, -1, 0, null, null);
    } else if (nodeClass == DartFunctionExpression.class) {
      return (N) new DartFunctionExpression(null, null, false);
    } else if (nodeClass == DartFunctionObjectInvocation.class) {
      return (N) new DartFunctionObjectInvocation(null, new ArrayList<DartExpression>());
    } else if (nodeClass == DartFunctionTypeAlias.class) {
      return (N) new DartFunctionTypeAlias(
          null,
          null,
          new ArrayList<DartParameter>(),
          new ArrayList<DartTypeParameter>());
    } else if (nodeClass == DartIdentifier.class) {
      return (N) new DartIdentifier("");
    } else if (nodeClass == DartIfStatement.class) {
      return (N) new DartIfStatement(null, 0, null, 0, null);
    } else if (nodeClass == DartInitializer.class) {
      return (N) new DartInitializer(null, null);
    } else if (nodeClass == DartLabel.class) {
      return (N) new DartLabel(null, null);
    } else if (nodeClass == DartMapLiteral.class) {
      return (N) new DartMapLiteral(false, null, new ArrayList<DartMapLiteralEntry>());
    } else if (nodeClass == DartMapLiteralEntry.class) {
      return (N) new DartMapLiteralEntry(null, null);
    } else if (nodeClass == DartMethodDefinition.class) {
      return (N) DartMethodDefinition.create(null, null, null, null);
    } else if (nodeClass == DartMethodInvocation.class) {
      return (N) new DartMethodInvocation(null, false, null, new ArrayList<DartExpression>());
    } else if (nodeClass == DartNativeBlock.class) {
      return (N) new DartNativeBlock(null);
    } else if (nodeClass == DartNewExpression.class) {
      return (N) new DartNewExpression(null, new ArrayList<DartExpression>(), false);
    } else if (nodeClass == DartNullLiteral.class) {
      return (N) DartNullLiteral.get();
    } else if (nodeClass == DartParameter.class) {
      return (N) new DartParameter(null, null, new ArrayList<DartParameter>(), null, Modifiers.NONE);
    } else if (nodeClass == DartParenthesizedExpression.class) {
      return (N) new DartParenthesizedExpression(null);
    } else if (nodeClass == DartPropertyAccess.class) {
      return (N) new DartPropertyAccess(null, null);
    } else if (nodeClass == DartReturnStatement.class) {
      return (N) new DartReturnStatement(null);
    } else if (nodeClass == DartStringInterpolation.class) {
      return (N) new DartStringInterpolation(
          new ArrayList<DartStringLiteral>(),
          new ArrayList<DartExpression>());
    } else if (nodeClass == DartStringLiteral.class) {
      return (N) DartStringLiteral.get("");
    } else if (nodeClass == DartSuperConstructorInvocation.class) {
      return (N) new DartSuperConstructorInvocation(null, new ArrayList<DartExpression>());
    } else if (nodeClass == DartSuperExpression.class) {
      return (N) DartSuperExpression.get();
    } else if (nodeClass == DartSwitchStatement.class) {
      return (N) new DartSwitchStatement(null, new ArrayList<DartSwitchMember>());
    } else if (nodeClass == DartThisExpression.class) {
      return (N) DartThisExpression.get();
    } else if (nodeClass == DartThrowExpression.class) {
      return (N) new DartThrowExpression(null);
    } else if (nodeClass == DartTryStatement.class) {
      return (N) new DartTryStatement(null, new ArrayList<DartCatchBlock>(), null);
    } else if (nodeClass == DartTypeExpression.class) {
      return (N) new DartTypeExpression(null);
    } else if (nodeClass == DartTypeNode.class) {
      return (N) new DartTypeNode(null, new ArrayList<DartTypeNode>());
    } else if (nodeClass == DartTypeParameter.class) {
      return (N) new DartTypeParameter(null, null);
    } else if (nodeClass == DartUnaryExpression.class) {
      return (N) new DartUnaryExpression(null, 0, null, true);
    } else if (nodeClass == DartUnit.class) {
      return (N) new DartUnit(null, false);
    } else if (nodeClass == DartUnqualifiedInvocation.class) {
      return (N) new DartUnqualifiedInvocation(null, new ArrayList<DartExpression>());
    } else if (nodeClass == DartVariable.class) {
      return (N) new DartVariable(null, null);
    } else if (nodeClass == DartVariableStatement.class) {
      return (N) new DartVariableStatement(new ArrayList<DartVariable>(), null);
    } else if (nodeClass == DartWhileStatement.class) {
      return (N) new DartWhileStatement(null, 0, null);
    }
    return null;
  }

  /**
   * Create a new node with the given type.
   * 
   * @param nodeType the type of the node to be created
   * @return the node that was created
   */
  public DartNode createInstance(int nodeType) {
    DartCore.notYetImplemented();
    return null;
  }

  /**
   * Create a new block with an empty list of statements.
   * 
   * @return the block that was created
   */
  public DartBlock newBlock() {
    return new DartBlock(new ArrayList<DartStatement>());
  }
}
