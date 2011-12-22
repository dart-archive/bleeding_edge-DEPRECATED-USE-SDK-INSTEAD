// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A phase in the compilation process that processes the entire AST and
 * desugars await expressions.
 */
awaitTransformation() {
  for (var lib in world.libraries.getValues()) {
    for (var type in lib.types.getValues()) {
      for (var member in type.members.getValues()) {
        _process(member);
      }
      for (var member in type.constructors.getValues()) {
        _process(member);
      }
    }
  }
}

/** Transform a single member (method or property). */
_process(Member member) {
  if (member.isConstructor || member.isMethod) {
    _processFunction(member.definition);
  } else if (member.isProperty) {
    PropertyMember p = member;
    if (p.getter != null) _process(p.getter);
    if (p.setter != null) _process(p.setter);
  }
}

/** Transform a function definition, and internally any nested functions. */
_processFunction(FunctionDefinition func) {
  AwaitChecker checker = new AwaitChecker();

  // Run checker that collects nested functions and which nodes may contain
  // await expressions.
  func.visit(checker);

  // Rewrite nested asynchronous functions first.
  for (FunctionDefinition f in checker.nestedFunctions) {
    _processFunction(f);
  }

  // Then rewrite the function, if necessary.
  func.visit(new AwaitProcessor(checker.haveAwait));
}

/** Weak set of AST nodes. */
// TODO(sigmund): delete this. This is a temporary workaround to keep 'frog'
// as independent as possible of the await experimental feature. Ideally we
// should either make [Node] hashable or store information collected by analyses
// in the nodes themselves.
class NodeSet {
  Map<String, List<Node>> _hashset;
  NodeSet() : _hashset = {};

  bool add(Node n) {
    if (contains(n)) return false;
    String key = n.span.locationText;
    List<Node> nodes = _hashset[key];
    if (nodes == null) {
      _hashset[key] = [n];
    } else {
      nodes.add(n);
    }
    return true;
  }

  bool contains(Node n) {
    String key = n.span.locationText;
    List<Node> nodes = _hashset[key];
    if (nodes == null) {
      return false;
    }
    for (Node member in nodes) {
      if (n === member) return true;
    }
    return false;
  }
}


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
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupportedStmt("return", node);
    }
    return awaitSeen;
  }

  visitThrowStatement(ThrowStatement node) {
    bool awaitSeen = _visit(node.value);
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupportedStmt("throw", node);
    }
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
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupportedStmt("try", node);
    }
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
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("call expressions", node);
    }
    return awaitSeen;
  }

  visitIndexExpression(IndexExpression node) {
    bool awaitSeen = node.target.visit(this);
    if (node.index.visit(this)) awaitSeen = true;
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("index expressions", node);
    }
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
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("paren expressions", node);
    }
    return awaitSeen;
  }

  visitAwaitExpression(AwaitExpression node) {
    haveAwait.add(node);
    return true;
  }

  visitDotExpression(DotExpression node) {
    bool awaitSeen = node.self.visit(this);
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("dot expressions", node);
    }
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

  visitNullExpression(NullExpression node) {
    return false;
  }

  visitLiteralExpression(LiteralExpression node) {
    return false;
  }

  visitArgumentNode(ArgumentNode node) {
    bool awaitSeen = node.value.visit(this);
    if (awaitSeen) {
      haveAwait.add(node);
      _notSupported("call arguments", node);
    }
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

/** Normalizes the AST to make the translation in [AwaitProcessor] simpler. */
class AwaitNormalizer implements TreeVisitor {
  // TODO(sigmund): implement normalization. [AwaitProcessor] assumes that:
  //
  // - await only occurs in top-level assignments. For example:
  //      if (await t) return;
  //   after normalization should become:
  //      final $t = await t;
  //      if ($t) return;
  //
  // - await in declarations are split in multiple declarations:
  //      int x = 1, y = await t, z = 3, w = y;
  //   becomes:
  //      int x = 1;
  //      int y = await t;
  //      int z = 3, w = y;
  //
  // - await cannot occur on complex assignments:
  //      x += await t
  //   becomes:
  //      $t = await t
  //      x += $t
  //
  // - await cannot occur outside statement blocks:
  //      if (...) x = await t
  //   becomes:
  //      if (...) { x = await t }
}


/**
 * Desugarizes all await calls in a single function. This visitor assumes that
 * the tree is already normalized with [AwaitNormalizer]. For this reason it
 * only needs to recurse on statements and not on expressions. Like in
 * [AwaitChecker], nested functions have to be processed separately.
 */
class AwaitProcessor implements TreeVisitor {

  /**
   * Name of the variable introduced on asynchronous functions (of type
   * [Completer] used to create a future of the function's result.
   */
  // TODO(sigmund): fix frog to make it possible to switch to '_a:res'. The
  // current mangling breaks across closure-boundaries.
  static final _COMPLETER_NAME = '_a_res';
  static final _THEN_PARAM = '_a_v';
  static final _IGNORED_THEN_PARAM = '_a_ignored_param';
  static final _COMPLETE_METHOD = 'complete';
  static final _COMPLETE_EXCEPTION_METHOD = 'completeException';

  static final _CONTINUATION_PREFIX = '_a_after_';
  static final _LOOP_CONTINUATION_PREFIX = '_a_';

  /** The continuation when visiting a particular statement. */
  Queue<Statement> continuation;

  /** Counter to ensure created closure names are unique. */
  int continuationClosures = 0;

  /** Nodes containing await expressions (determined by [AwaitChecker]). */
  final NodeSet haveAwait;

  AwaitProcessor(this.haveAwait) : continuation = new Queue<Statement>();

  visitVariableDefinition(VariableDefinition node) {
    if (!haveAwait.contains(node)) return node;
    final values = node.values;
    // After normalization, variable declarations with await can only occur at
    // the top-level and there is no other declaration.
    assert(values != null && values.length == 1
        && values[0] is AwaitExpression);
    final param = new Identifier(_THEN_PARAM, node.span);
    // T x = await t; ... becomes:
    // t.then((v) { T x = v; ... }
    continuation.addFirst(new VariableDefinition(
          node.modifiers, node.type, node.names,
          [new VarExpression(param, node.span)], node.span));
    return new ExpressionStatement(
        _desugarAwaitCall(values[0], param), node.span);
  }

  visitFunctionDefinition(FunctionDefinition node) {
    if (!haveAwait.contains(node)) return node;
    // TODO(sigmund): consider making this part of the normalizer
    // make implicit return explicit:
    if (node.body is BlockStatement) {
      BlockStatement block = node.body;
      if (block.body.last() is! ReturnStatement) {
        continuation.addFirst(
            _callCompleter(new NullExpression(node.span), node.span));
      }
    }

    Statement newBody = node.body.visit(this);
    // TODO(sigmund): extract type arg and put it in completer
    final newList = [_declareCompleter(null, node.span)];
    if (newBody is BlockStatement) {
      BlockStatement block = newBody;
      newList.addAll(block.body);
    } else {
      newList.add(newBody);
    }
    // We update the body in-place to make it easier to update nested functions
    // without having to rewrite the containing function's AST.
    node.body = new BlockStatement(newList, newBody.span);
    return node;
  }

  visitReturnStatement(ReturnStatement node) {
    continuation.clear();
    return _callCompleter(node.value, node.span);
  }

  visitThrowStatement(ThrowStatement node) {
    continuation.clear();
    return _callCompleterException(node.value, node.span);
  }

  visitAssertStatement(AssertStatement node) {
    // TODO(sigmund): implement. This should be normalized into a conditional
    // and call completeException only when the assertion fails.
    return node;
  }

  visitBreakStatement(BreakStatement node) {
    // TODO(sigmund): implement
    return node;
  }

  visitContinueStatement(ContinueStatement node) {
    // TODO(sigmund): implement
    return node;
  }

  visitIfStatement(IfStatement node) {
    if (!haveAwait.contains(node)) return node;
    // TODO(sigmund): consider whether we should create this continuation
    // closure when there are few statements following (e.g a simple expression
    // statement, no loops, etc).
    String afterIf = _newClosureName("if", false);
    Statement def = _makeContinuation(afterIf, node.span);

    final trueContinuation = new Queue();
    trueContinuation.addFirst(_callNoArg(afterIf, node.span));
    continuation = trueContinuation;
    Statement tRes = node.trueBranch.visit(this);

    Statement fRes = null;
    if (node.falseBranch != null) {
      final falseContinuation = new Queue();
      falseContinuation.addFirst(_callNoArg(afterIf, node.span));
      continuation = falseContinuation;
      fRes = node.falseBranch.visit(this);
      continuation = new Queue();
    } else {
      continuation = new Queue();
      continuation.addFirst(_callNoArg(afterIf, node.span));
    }

    continuation.addFirst(new IfStatement(node.test, tRes, fRes, node.span));
    return def;
  }

  visitWhileStatement(WhileStatement node) {
    if (!haveAwait.contains(node)) return node;

    String afterWhile = _newClosureName("while", false);
    Statement def = _makeContinuation(afterWhile, node.span);
    String repeatWhile = _newClosureName("while", true);

    final bodyContinuation = new Queue();
    bodyContinuation.addFirst(_callNoArg(repeatWhile, node.span));
    continuation = bodyContinuation;
    Statement tRes = node.body.visit(this);

    continuation = new Queue();
    continuation.addFirst(_callNoArg(afterWhile, node.span));
    continuation.addFirst(new IfStatement(node.test, tRes, null, node.span));
    Statement defLoop = _makeContinuation(repeatWhile, node.span);

    continuation = new Queue();
    continuation.addFirst(_callNoArg(repeatWhile, node.span));
    continuation.addFirst(defLoop);
    return def;
  }

  visitDoStatement(DoStatement node) {
    if (!haveAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitForStatement(ForStatement node) {
    if (!haveAwait.contains(node)) return node;
    // TODO(sigmund): implement
    // Note: this is harder than while loops because of dart's special semantics
    // capturing the loop variable.
    return node;
  }

  visitForInStatement(ForInStatement node) {
    if (!haveAwait.contains(node)) return node;
    // TODO(sigmund): implement
    // Note: this is harder than while loops because of dart's special semantics
    // capturing the loop variable.
    return node;
  }

  visitTryStatement(TryStatement node) {
    if (!haveAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitSwitchStatement(SwitchStatement node) {
    if (!haveAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitBlockStatement(BlockStatement node) {
    if (!haveAwait.contains(node) && continuation.isEmpty()) return node;
    // TODO(sigmund): test also when !continuation.isEmpty();
    for (int i = node.body.length - 1; i >= 0; i--) {
      final res = node.body[i].visit(this);
      // Note: we can't inline [res] below because, visit might redefine
      // 'continuation' and we want to use it here.
      continuation.addFirst(res);
    }

    List<Statement> newBody = [];
    newBody.addAll(continuation);
    continuation.clear();
    return new BlockStatement(newBody, node.span);
  }

  visitLabeledStatement(LabeledStatement node) {
    if (!haveAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitExpressionStatement(ExpressionStatement node) {
    if (!haveAwait.contains(node)) return node;
    // After normalization, expression statements with await can only occur at
    // the top-level or as the rhs of simple assignments (= but not +=).
    if (node.body is AwaitExpression) {
      return new ExpressionStatement(
          _desugarAwaitCall(node.body,
            new Identifier(_IGNORED_THEN_PARAM, node.span)), node.span);
    } else {
      assert(node.body is BinaryExpression);
      BinaryExpression bin = node.body;
      assert(bin.op.kind == TokenKind.ASSIGN);
      assert(bin.y is AwaitExpression);
      final param = new Identifier(_THEN_PARAM, node.span);
      continuation.addFirst(new ExpressionStatement(
          new BinaryExpression(
              bin.op, bin.x, new VarExpression(param, node.span), node.span),
          node.span));
      return new ExpressionStatement(
          _desugarAwaitCall(bin.y, param), node.span);
    }
  }

  visitEmptyStatement(EmptyStatement node) {
    if (!haveAwait.contains(node)) return node;
    return node;
  }

  visitArgumentNode(ArgumentNode node) {
    if (!haveAwait.contains(node)) return node;
    return node;
  }

  visitCatchNode(CatchNode node) {
    if (!haveAwait.contains(node)) return node;
    return node;
  }

  visitCaseNode(CaseNode node) {
    if (!haveAwait.contains(node)) return node;
    return node;
  }

  /**
   * Converts an await expression into several statements: calling
   * [:Future.then:] and propatating errors.
   */
  _desugarAwaitCall(AwaitExpression node, Identifier param) {
    final thenMethod = new DotExpression(node.body,
            new Identifier('then', node.span), node.span);
    List<Statement> afterAwait = [];
    afterAwait.addAll(continuation);
    final thenArg = new LambdaExpression(
        new FunctionDefinition([], null, null,
        [new FormalNode(
          false, false, null /* infer type from body? */,
          param, null, param.span)
        ], null, null,
        new BlockStatement(afterAwait, node.span), node.span),
        node.span);
    continuation.clear();
    // TODO(sigmund): insert in new continuation all additional statements that
    // propagate errors.
    // this assumes that the normalization ensures await calls are within blocks
    continuation.addFirst(_returnFuture(node.span));
    return new CallExpression(thenMethod,
        [new ArgumentNode(null, thenArg, node.span)], node.span);
  }


  /** Make the statement: [: final Completer<T> v = new Completer<T>(); :]. */
  _declareCompleter(Type argType, SourceSpan span) {
    final name = new Identifier('Completer', span);
    final ctorName = new Identifier('', span);
    var typeRef = new NameTypeReference(false, name, [ctorName], span);
    var type = world.corelib.types['Completer'];
    if (argType != null) {
      typeRef = new GenericTypeReference(typeRef,
          new TypeReference(span, argType), 0, span);
      typeRef.type = type.getOrMakeConcreteType([argType]);
    } else {
      typeRef.type = type;
    }
    final def = new VariableDefinition([new Token.fake(TokenKind.FINAL, span)],
        typeRef,
        [new Identifier(_COMPLETER_NAME, span)],
        [new NewExpression(false, typeRef, null, [], span)],
        span);
    return def;
  }

  /** Make the statement: [: _a$res.complete(value); :]. */
  _callCompleter(Expression value, SourceSpan span) {
    return _makeCall(_COMPLETER_NAME, _COMPLETE_METHOD, value, span);
  }

  /** Make the statement: [: _a$res.completeException(value); :]. */
  _callCompleterException(Expression value, SourceSpan span) {
    return _makeCall(_COMPLETER_NAME, _COMPLETE_EXCEPTION_METHOD, value, span);
  }

  /** Make the statement: [: target.method(value); :]. */
  _makeCall(String target, String method, Expression value, SourceSpan span) {
    if (value == null) value = new NullExpression(span);
    return new ExpressionStatement(new CallExpression(
        new DotExpression(
            new VarExpression(new Identifier(target, span), span),
            new Identifier(method, span), span),
        [new ArgumentNode(null, value, value.span)], span), span);
  }

  /** Make the statement: [: return _a$res.future; :]. */
  _returnFuture(SourceSpan span) {
    return new ReturnStatement(
        new DotExpression(
            new VarExpression(new Identifier(_COMPLETER_NAME, span), span),
            new Identifier("future", span), span), span);
  }

  /** Create a unique name for a continuation. */
  String _newClosureName(String name, bool isLoop) {
    String mName = (isLoop ? _LOOP_CONTINUATION_PREFIX : _CONTINUATION_PREFIX)
        + '${name}_$continuationClosures';
    continuationClosures++;
    return mName;
  }

  /** Create a closure that contains the continuation statements. */
  _makeContinuation(String mName, SourceSpan span) {
    List<Statement> continuationBlock = [];
    continuationBlock.addAll(continuation);
    return new FunctionDefinition([], null,
        new Identifier(mName, span), [], null, null,
        new BlockStatement(continuationBlock, span), span);
  }

  /** Make a statement invoking a function in scope. */
  _callNoArg(String mName, SourceSpan span) {
    return new ExpressionStatement(new CallExpression(
        new VarExpression(new Identifier(mName, span), span), [], span), span);
  }
}

// TODO(sigmund): create the following tests:
// - await within the body of getter or setter properties
// - await in each valid AST construct
// - exceptions - make some of these fail and propagate errors.
// - methods with and without returns (are returns added implicitly)
// - await within assignmetns, but not declarations (see what happens with
//      x = await t;
//      x = await y;
//   or
//      x += await t;
//      x += await y;
//   (does it matter that ExpressionStatement will shadow the variable in the
//   callback function to then?)
// - floating ifs/for loops, etc - make sure we don't need to normalize the ast
// to disallow 'floating' ifs or to insert a block statement when returning from
// ifs (rather than appending code to the existing continuation list). for
// instance:
//   if (a) if (b) await t; 2;
//  becomes:
//   if (a) { if (b) { await t; } } 2;
//  which becomes becomes:
//    _2() { 2; } if (a) { if (b) { t.then((_) { _2(); } } } _2();
//  (seems that 'a' doesn't need { })
// - 'if/while' with only one statement in the continuation (check the
// optimization that copies code rather than adding a continuation closure).
// - await not inside a block (could break error propagation if normalization is
// done incorrectly).
