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
package com.google.dart.tools.core.internal.formatter;

import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartLiteral;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.compiler.ast.DartNullLiteral;
import com.google.dart.compiler.ast.DartStringInterpolation;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.parser.Token;

import java.util.ArrayList;
import java.util.List;

class BinaryExpressionFragmentBuilder extends DartNodeTraverser<DartNode> {
  private List<DartNode> fragmentsList = new ArrayList<DartNode>();
  private List<Token> operatorsList = new ArrayList<Token>();
  private int realFragmentsSize = 0;

  @Override
  public DartNode visitBinaryExpression(DartBinaryExpression binaryExpression) {
    final int numberOfParens = 0; // TODO preserve parens
    if (numberOfParens > 0) {
      addRealFragment(binaryExpression);
    } else {
      switch (binaryExpression.getOperator()) {
        case MUL:
        case ADD:
        case DIV:
        case TRUNC:
        case MOD:
        case BIT_XOR:
        case SUB:
        case OR:
        case AND:
        case BIT_AND:
        case BIT_OR:
          if (buildFragments(binaryExpression)) {
            accum(binaryExpression);
          }
          break;
        default:
          addRealFragment(binaryExpression);
          break;
      }
    }
    return null;
  }

  @Override
  public DartNode visitExpression(DartExpression node) {
    addRealFragment(node);
    return null;
  }

  @Override
  public DartNode visitLiteral(DartLiteral node) {
    addSmallFragment(node);
    return null;
  }

  @Override
  public DartNode visitNullLiteral(DartNullLiteral node) {
    addRealFragment(node);
    return null;
  }

  @Override
  public DartNode visitStringInterpolation(DartStringInterpolation node) {
    return visitExpression(node); // TODO
  }

  @Override
  public DartNode visitStringLiteral(DartStringLiteral node) {
    addRealFragment(node);
    return null;
  }

  void accum(DartBinaryExpression node) {
    node.getArg1().accept(this);
    operatorsList.add(node.getOperator());
    node.getArg2().accept(this);
  }

  DartNode[] fragments() {
    DartNode[] fragments = new DartNode[fragmentsList.size()];
    fragmentsList.toArray(fragments);
    return fragments;
  }

  Token[] operators() {
    Token[] fragments = new Token[operatorsList.size()];
    operatorsList.toArray(fragments);
    return fragments;
  }

  int realFragmentsSize() {
    return realFragmentsSize;
  }

  int size() {
    return fragmentsList.size();
  }

  private final void addRealFragment(DartNode node) {
    fragmentsList.add(node);
    realFragmentsSize++;
  }

  private final void addSmallFragment(DartNode node) {
    fragmentsList.add(node);
  }

  private boolean buildFragments(DartExpression expression) {
    int exprParenCount = 1; // TODO preserve parens
    if (exprParenCount != 0) {
      addRealFragment(expression);
      return false;
    }
    return true;
  }
}
