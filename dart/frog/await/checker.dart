// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Traverses the entire code of a function to find whether it contains awaits,
 * ensuring they occur only in valid locations. This visitor will *not* recurse
 * inside nested function declarations.
 */
class AwaitChecker implements TreeVisitor {

  /** Functions found within the body of the entry function. */
  List<FunctionDefinition> nestedFunctions;

  /** Helper bool to ensure that only the top-level function is analyzed. */
  bool _entryFunction = true;

  /** AST nodes that contain await expressions. */
  NodeSet haveAwait;

  // TODO: track haveExit (return/throw) and haveBreak (break/continue) to
  // property transform nodes that don't contain await, but may be affected
  AwaitChecker() : nestedFunctions = [], haveAwait = new NodeSet();

  visitVariableDefinition(VariableDefinition node) {
    bool awaitSeen = _visitList(node.values);
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitFunctionDefinition(FunctionDefinition node) {
    if (_entryFunction) {
      _entryFunction = false;
      if (node.initializers != null) {
        for (Expression e in node.initializers) {
          if (e.visit(this)) {
            world.error('Await expressions are not allowed in initializers.',
                e.span);
          }
        }
      }
      if (_visit(node.body)) {
        haveAwait.add(node);
        // TODO(sigmund) check that return type is Dynamic or a future.
        return true;
      }
      return false;
    } else {
      // Do not analyze nested functions now, use a separate checker for that.
      nestedFunctions.add(node);
      return false;
    }
  }

  _notSupportedStmt(name, node) {
    world.error("Await is not supported in '$name' statements yet.", node.span);
  }

  _notSupported(name, node) {
    world.error(
        "Await is not supported in $name yet, try pulling into a tmp var.",
        node.span);
  }

  visitReturnStatement(ReturnStatement node) {
    bool awaitSeen = _visit(node.value);
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitThrowStatement(ThrowStatement node) {
    bool awaitSeen = _visit(node.value);
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitAssertStatement(AssertStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupportedStmt("assert", node);
    }
    return awaitSeen;
  }

  visitBreakStatement(BreakStatement node) {
    return false;
  }

  visitContinueStatement(ContinueStatement node) {
    return false;
  }

  visitIfStatement(IfStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (node.trueBranch.visit(this)) {
      awaitSeen = true;
    }
    if (_visit(node.falseBranch)) {
      awaitSeen = true;
    }
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitWhileStatement(WhileStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (_visit(node.body)) awaitSeen = true;
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitDoStatement(DoStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (_visit(node.body)) awaitSeen = true;
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupportedStmt("do while", node);
    }
    return awaitSeen;
  }

  visitForStatement(ForStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (_visit(node.body)) awaitSeen = true;
    if (_visit(node.init)) awaitSeen = true;
    if (_visitList(node.step)) awaitSeen = true;
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupportedStmt("for", node);
    }
    return awaitSeen;
  }

  visitForInStatement(ForInStatement node) {
    bool awaitSeen = node.list.visit(this);
    if (_visit(node.body)) awaitSeen = true;
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupportedStmt("for-in", node);
    }
    return awaitSeen;
  }

  visitTryStatement(TryStatement node) {
    bool awaitSeen = (_visit(node.body));
    if (_visitList(node.catches)) awaitSeen = true;
    if (_visit(node.finallyBlock)) {
      awaitSeen = true;
    }
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitSwitchStatement(SwitchStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (_visitList(node.cases)) awaitSeen = true;
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupportedStmt("switch", node);
    }
    return awaitSeen;
  }

  visitBlockStatement(BlockStatement node) {
    bool awaitSeen = _visitList(node.body);
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitLabeledStatement(LabeledStatement node) {
    bool awaitSeen = node.body.visit(this);
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitExpressionStatement(ExpressionStatement node) {
    bool awaitSeen = node.body.visit(this);
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitEmptyStatement(EmptyStatement node) {
    return false;
  }

  visitLambdaExpression(LambdaExpression node) {
    _entryFunction = false;
    return node.func.visit(this);
  }

  visitCallExpression(CallExpression node) {
    bool awaitSeen = node.target.visit(this);
    if (_visitList(node.arguments)) awaitSeen = true;
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitIndexExpression(IndexExpression node) {
    bool awaitSeen = node.target.visit(this);
    if (node.index.visit(this)) awaitSeen = true;
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitBinaryExpression(BinaryExpression node) {
    bool awaitSeen = node.x.visit(this);
    if (node.y.visit(this)) awaitSeen = true;
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitUnaryExpression(UnaryExpression node) {
    // TODO(sigmund): issue errors for ++/-- cases where we expect an l-value.
    bool awaitSeen = node.self.visit(this);
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("unary expressions", node);
    }
    return awaitSeen;
  }

  visitPostfixExpression(PostfixExpression node) {
    // TODO(sigmund): issue errors for ++/-- cases where we expect an l-value.
    bool awaitSeen = node.body.visit(this);
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("postfix expressions", node);
    }
    return awaitSeen;
  }

  visitNewExpression(NewExpression node) {
    bool awaitSeen = _visitList(node.arguments);
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("new expressions", node);
    }
    return awaitSeen;
  }

  visitListExpression(ListExpression node) {
    bool awaitSeen = _visitList(node.values);
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("list literals", node);
    }
    return awaitSeen;
  }

  visitMapExpression(MapExpression node) {
    bool awaitSeen = _visitList(node.items);
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("map literals", node);
    }
    return awaitSeen;
  }

  visitConditionalExpression(ConditionalExpression node) {
    bool awaitSeen = node.test.visit(this);
    if (node.trueBranch.visit(this)) awaitSeen = true;
    if (node.falseBranch.visit(this)) awaitSeen = true;
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("ternary expressions", node);
    }
    return awaitSeen;
  }

  visitIsExpression(IsExpression node) {
    bool awaitSeen = node.x.visit(this);
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("'is' checks", node);
    }
    return awaitSeen;
  }

  visitParenExpression(ParenExpression node) {
    bool awaitSeen = node.body.visit(this);
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitAwaitExpression(AwaitExpression node) {
    haveAwait.add(node);
    return true;
  }

  visitDotExpression(DotExpression node) {
    bool awaitSeen = node.self.visit(this);
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitVarExpression(VarExpression node) {
    return false;
  }

  visitThisExpression(ThisExpression node) {
    return false;
  }

  visitSuperExpression(SuperExpression node) {
    return false;
  }

  visitStringInterpExpression(StringInterpExpression node) {
    return false;
  }

  visitLiteralExpression(LiteralExpression node) {
    return false;
  }

  visitArgumentNode(ArgumentNode node) {
    bool awaitSeen = node.value.visit(this);
    if (awaitSeen) haveAwait.add(node);
    return awaitSeen;
  }

  visitCatchNode(CatchNode node) {
    bool awaitSeen = false;
    if (_visit(node.body)) awaitSeen = true;
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("catch blocks", node);
    }
    return awaitSeen;
  }

  visitCaseNode(CaseNode node) {
    bool awaitSeen = false;
    for (Expression e in node.cases) {
      if (_visit(e)) {
        world.error(
            'Await is not allowed in case expressions of switch statements.',
            e.span);
        awaitSeen = true;
      }
    }
    if (_visitList(node.statements)) awaitSeen = true;
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("case blocks", node);
    }
    return awaitSeen;
  }

  /**
   * Recurses on every element in the list, returns whether any of them has an
   * await.
   */
  _visitList(List nodes) {
    bool awaitSeen = false;
    if (nodes != null) {
      for (final n in nodes) {
        if (_visit(n)) awaitSeen = true;
      }
    }
    return awaitSeen;
  }

  _visit(node) {
    if (node == null) return false;
    return node.visit(this);
  }
}
