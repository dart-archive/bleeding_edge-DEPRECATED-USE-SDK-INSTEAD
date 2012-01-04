// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Normalizes the AST to make the translation in [AwaitProcessor] simpler. This
 * normalization provides the following guarantees:
 * - await only occurs in top-level assignments. For example:
 *      if (await t) return;
 *   after normalization should become:
 *      final $t = await t;
 *      if ($t) return;
 *
 * - await in declarations are split in multiple declarations:
 *      int x = 1, y = await t, z = 3, w = y;
 *   becomes:
 *      int x = 1;
 *      int y = await t;
 *      int z = 3, w = y;
 *
 * - await cannot occur on complex assignments:
 *      x += await t
 *   becomes:
 *      $t = await t
 *      x += $t
 *
 * - await cannot occur outside statement blocks:
 *      if (...) x = await t
 *   becomes:
 *      if (...) { x = await t }
 */
class AwaitNormalizer implements TreeVisitor {
  // TODO(sigmund): fix frog to make it possible to switch to '_a:tmp'. The
  // mangling code currently breaks across closure-boundaries.
  static final _PREFIX = '_a_tmp';
  int _tmpVarCounter = 0;

  /** AST nodes that contain await expressions. */
  NodeSet haveAwait;

  AwaitNormalizer(this.haveAwait);

  NormalizerResult visitVariableDefinition(VariableDefinition node) {
    // split variable declarations in chuncks:
    List<Statement> res = [];
    List<Identifier> names = [];
    List<Expression> values = [];
    for (int i = 0; i < node.names.length; i++) {
      final val = node.values[i];
      if (val == null || !haveAwait.contains(val)) {
        names.add(node.names[i]);
        values.add(val);
      } else {
        // an await was found, declare previous vars first:
        if (names.length > 0) {
          res.add(new VariableDefinition(
              node.modifiers, node.type, names, values, node.span));
          names = [];
          values = [];
        }
        final valRes = _visit(val);
        res.addAll(valRes.stmts);
        // add the declaration directly, since all following vars should
        // be in a separate declaration
        final newDecl = new VariableDefinition(node.modifiers, node.type,
              [node.names[i]], [valRes.exp], node.span);
        res.add(newDecl);
      }
    }
    if (names.length > 0) {
      res.add(new VariableDefinition(
            node.modifiers, node.type, names, values, node.span));
    }
    return new NormalizerResult(res, null, null);
  }

  NormalizerResult visitFunctionDefinition(FunctionDefinition node) {
    if (!haveAwait.contains(node)) return null;
    node.body = _visit(node.body).asStatement(); // normalize and replace body.
  }

  NormalizerResult visitReturnStatement(ReturnStatement node) {
    final expRes = _visit(node.value);
    final res = new NormalizerResult([], null, null);
    res.stmts.addAll(expRes.stmts);
    res.stmts.add(new ReturnStatement(expRes.exp, node.span));
    return res;
  }

  NormalizerResult visitThrowStatement(ThrowStatement node) {
    final expRes = _visit(node.value);
    final res = new NormalizerResult([], null, null);
    res.stmts.addAll(expRes.stmts);
    res.stmts.add(new ThrowStatement(expRes.exp, node.span));
    return res;
  }

  NormalizerResult visitIfStatement(IfStatement node) {
    final testRes = _visit(node.test);
    final trueRes = _visit(node.trueBranch).asStatement();
    final falseRes = _visit(node.falseBranch).asStatement();
    final res = new NormalizerResult([], null, null);
    res.stmts.addAll(testRes.stmts);
    final exp = _liftExp(testRes, res.stmts);
    res.stmts.add(new IfStatement(exp, trueRes, falseRes, node.span));
    return res;
  }

  NormalizerResult visitWhileStatement(WhileStatement node) {
    final testRes = _visit(node.test);
    final bodyRes = _visit(node.body).asStatement();
    final res = new NormalizerResult([], null, null);
    res.stmts.addAll(testRes.stmts);
    final exp = _liftExp(testRes, res.stmts);
    res.stmts.add(new WhileStatement(exp, bodyRes, node.span));
    return res;
  }

  NormalizerResult visitDoStatement(DoStatement node) {
    final bodyRes = _visit(node.body);
    final testRes = _visit(node.test);
    final bodyList = [bodyRes.asStatement()];
    bodyList.addAll(testRes.stmts);
    final exp = _liftExp(testRes, bodyList);
    final body = new BlockStatement(bodyList, node.span);
    return new NormalizerResult.fromNode(
        new DoStatement(body, exp, node.span));
  }

  NormalizerResult visitForStatement(ForStatement node) {
    // TODO(sigmund): implement
    _notSupported("for loops", node);
    return null;
  }

  NormalizerResult visitForInStatement(ForInStatement node) {
    _notSupported("for-in loops", node);
    return null;
  }

  NormalizerResult visitTryStatement(TryStatement node) {
    final bodyRes = _visit(node.body);
    List<CatchNode> catchesRes = [];
    for (NormalizerResult r in _visitList(node.catches)) {
      assert (r.node != null && r.node is CatchNode);
      catchesRes.add(r.node);
    }
    final finallyRes = _visit(node.finallyBlock);

    return new NormalizerResult.fromNode(
        new TryStatement(bodyRes.asStatement(),
          catchesRes, finallyRes.asStatement(), node.span));
  }

  NormalizerResult visitSwitchStatement(SwitchStatement node) {
    _notSupported("switch statements", node);
    return null;
  }

  NormalizerResult visitBlockStatement(BlockStatement node) {
    final bodyRes = _visitList(node.body);
    final newBlock = [];
    for (NormalizerResult b in bodyRes) {
      newBlock.addAll(b.stmts);
      if (b.exp != null) {
        world.fatal(
            "unexpected: expression result from normalizing block", node.span);

      }
    }
    return new NormalizerResult.fromNode(
        new BlockStatement(newBlock, node.span));
  }

  NormalizerResult visitLabeledStatement(LabeledStatement node) {
    return new NormalizerResult.fromNode(new LabeledStatement(
        node.name, _visit(node.body).asStatement(), node.span));
  }

  visitAssertStatement(AssertStatement node) {
    // We can normalize
    //    assert await M;
    // as:
    //    var x = false;
    //    assert (x = true); // ensure await is only evaluated when assertions
    //                       // are enabled
    //    if (x) {
    //      var t = await M;
    //      assert t;
    //    }
    _notSupported("assert statements", node);
    return null;
  }

  NormalizerResult visitExpressionStatement(ExpressionStatement node) {
    final bodyRes = _visit(node.body);
    final res = new NormalizerResult([], null, null);
    res.stmts.addAll(bodyRes.stmts);
    res.stmts.add(new ExpressionStatement(bodyRes.exp, node.span));
    return res;
  }

  NormalizerResult visitCallExpression(CallExpression node) {
    final targetRes = _visit(node.target);
    final argsRes = _visitList(node.arguments);
    final extraStmts = [];
    final newArgs = [];
    extraStmts.addAll(targetRes.stmts);
    final target = _liftExp(targetRes, extraStmts);
    for (final arg in argsRes) {
      extraStmts.addAll(arg.stmts);
      newArgs.add(arg.node);
    }
    return new NormalizerResult(extraStmts,
        new CallExpression(target, newArgs, node.span), null);
  }

  NormalizerResult visitIndexExpression(IndexExpression node) {
    final targetRes = _visit(node.target);
    final indexRes = _visit(node.index);
    final extraStmts = [];
    extraStmts.addAll(targetRes.stmts);
    final target = _liftExp(targetRes, extraStmts);
    extraStmts.addAll(indexRes.stmts);
    final index = _liftExp(indexRes, extraStmts);
    return new NormalizerResult(extraStmts,
        new IndexExpression(target, index, node.span), null);
  }

  NormalizerResult visitBinaryExpression(BinaryExpression node) {
    final xRes = _visit(node.x);
    final yRes = _visit(node.y);
    final extraStmts = [];
    extraStmts.addAll(xRes.stmts);
    final x = _liftExp(xRes, extraStmts);
    if (node.op.kind == TokenKind.OR || node.op.kind == TokenKind.AND) {
      // AND and OR require short-circuiting code:
      final otherStmts = [];
      otherStmts.addAll(yRes.stmts);
      final y = _liftExp(yRes, otherStmts);
      final tmpId = _newTmp(node.span);
      extraStmts.add(_declareVar(tmpId, x, false));
      final tmpVar = new VarExpression(tmpId, node.span);
      Expression test = tmpVar;
      if (node.op.kind == TokenKind.OR) {
        // (a && b) becomes t = a; if (t) t = b; (t)
        // (a || b) becomes t = a; if (!t) t = b; (t)
        test = new UnaryExpression(
            new Token.fake(TokenKind.NOT, node.op.span), test, node.op.span);
      }
      otherStmts.add(new ExpressionStatement(
          new BinaryExpression(new Token.fake(TokenKind.ASSIGN, node.span),
              tmpVar, y, y.span), y.span));
      extraStmts.add(new IfStatement(test,
            new BlockStatement(otherStmts, node.y.span), null, node.y.span));
      return new NormalizerResult(extraStmts, tmpVar, null);
    } else {
      extraStmts.addAll(yRes.stmts);
      // Other operators require a var-lifting the rhs if they contain an await:
      final y = _liftExp(yRes, extraStmts);
      return new NormalizerResult(extraStmts,
          new BinaryExpression(node.op, x, y, node.span), null);
    }
  }

  NormalizerResult visitUnaryExpression(UnaryExpression node) {
    // TODO(sigmund): implement
    _notSupported("unary expressions", node);
    return null;
  }

  NormalizerResult visitPostfixExpression(PostfixExpression node) {
    // TODO(sigmund): implement
    _notSupported("postfix expressions", node);
    return null;
  }

  NormalizerResult visitNewExpression(NewExpression node) {
    // TODO(sigmund): implement
    _notSupported("new expressions", node);
    return null;
  }

  NormalizerResult visitListExpression(ListExpression node) {
    // TODO(sigmund): implement
    _notSupported("list literals", node);
    return null;
  }

  NormalizerResult visitMapExpression(MapExpression node) {
    // TODO(sigmund): implement
    _notSupported("map literals", node);
    return null;
  }

  NormalizerResult visitConditionalExpression(ConditionalExpression node) {
    // TODO(sigmund): implement
    _notSupported("ternary expressions", node);
    return null;
  }

  NormalizerResult visitIsExpression(IsExpression node) {
    // TODO(sigmund): implement
    _notSupported("is expressions", node);
    return null;
  }

  NormalizerResult visitParenExpression(ParenExpression node) {
    final res = _visit(node.body);
    final extraStmts = [];
    extraStmts.addAll(res.stmts);
    final exp = _liftExp(res, extraStmts);
    return new NormalizerResult(extraStmts,
        new ParenExpression(exp, node.span), res.node);
  }

  NormalizerResult visitAwaitExpression(AwaitExpression node) {
    final stmts = [];
    final bodyRes = _visit(node.body);
    stmts.addAll(bodyRes.stmts);
    final val = _asVariable(bodyRes, stmts);
    return new NormalizerResult(stmts,
        val != node.body ? new AwaitExpression(val, node.span) : node, null);
  }

  NormalizerResult visitDotExpression(DotExpression node) {
    final selfRes = _visit(node.self);
    final extraStmts = [];
    extraStmts.addAll(selfRes.stmts);
    final self = _liftExp(selfRes, extraStmts);
    return new NormalizerResult(extraStmts,
        new DotExpression(self, node.name, node.span), null);
  }

  NormalizerResult visitArgumentNode(ArgumentNode node) {
    final valueRes = _visit(node.value);
    final extraStmts = [];
    extraStmts.addAll(valueRes.stmts);
    final value = _liftExp(valueRes, extraStmts);
    return new NormalizerResult(extraStmts, null,
        new ArgumentNode(node.label, value, node.span));
  }

  CatchNode visitCatchNode(CatchNode node) {
    // TODO(sigmund): implement
    _notSupported("catch node", node);
    return node;
  }

  NormalizerResult visitCaseNode(CaseNode node) {
    // TODO(sigmund): implement
    _notSupported("case node", node);
    return null;
  }

  List _visitList(List nodes) {
    if (nodes == null) return null;
    List res = [];
    for (final n in nodes) {
      res.add(_visit(n));
    }
    return res;
  }

  _visit(node) {
    if (node == null || !haveAwait.contains(node)) {
      return new NormalizerResult.fromNode(node);
    }
    return node.visit(this);
  }

  Identifier _newTmp(SourceSpan span) {
    return new Identifier(_PREFIX + _tmpVarCounter++, span);
  }

  VariableDefinition _declareVar(name, value, bool isFinal) {
    return new VariableDefinition(
        isFinal ? [new Token.fake(TokenKind.FINAL, value.span)] : null,
        null, [name], [value], value.span);
  }

  Expression _liftExp(NormalizerResult r, List<Statement> extraStmts) {
    if (r.exp is! AwaitExpression) return r.exp;
    Identifier name = _newTmp(r.exp.span);
    extraStmts.add(_declareVar(name, r.exp, true));
    return new VarExpression(name, r.exp.span);
  }

  Expression _asVariable(NormalizerResult r, List<Statement> extraStmts) {
    if (r.exp is VarExpression) return r.exp;
    Identifier name = _newTmp(r.exp.span);
    extraStmts.add(_declareVar(name, r.exp, true));
    return new VarExpression(name, r.exp.span);
  }

  _notSupported(what, node) {
    world.error("Normalization of $what is not supported yet.", node.span);
  }
}

/** A temporary result of the normalization process. */
class NormalizerResult {
  /** A list of statements, in order. */
  List<Statement> stmts;

  /** Normalized expression (null for normalized statements). */
  Expression exp;

  /** Resulting node for AST nodes that are not stmts or expressions. */
  Node node;

  NormalizerResult(this.stmts, this.exp, this.node);

  factory NormalizerResult.fromNode(node) {
    if (node == null) {
      return new NormalizerResult(null, null, null);
    } else if (node is Expression) {
      return new NormalizerResult(const [], node, null);
    } else if (node is Statement) {
      return new NormalizerResult([node], null, null);
    } else {
      return new NormalizerResult(const [], null, node);
    }
  }

  Statement asStatement() {
    if (exp != null) {
      world.fatal("asStatement only supported on statement results.", exp.span);
    }
    if (stmts == null) {
      return null;
    }
    if (stmts.length == 1) {
      // Return the underlying statement without wrapping it in a block, unless
      // the statement contains an await.
      Statement stmt = stmts.last();

      if (stmt is! ExpressionStatement) return stmt;
      ExpressionStatement expStmt = stmt;
      Expression body = expStmt.body;

      if (body is! AwaitExpression || body is! BinaryExpression) return stmt;

      if (body is BinaryExpression) {
        BinaryExpression binExp = body;
        if (binExp.y is! AwaitExpression) return stmt;
      }
    }
    return new BlockStatement(stmts, stmts.last().span);
  }
}
