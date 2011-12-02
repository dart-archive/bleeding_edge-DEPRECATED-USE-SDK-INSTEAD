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
  func.visit(new AwaitProcessor(checker.nodesWithAwait));
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
  NodeSet nodesWithAwait;

  AwaitChecker() : nestedFunctions = [], nodesWithAwait = new NodeSet();

  visitVariableDefinition(VariableDefinition node) {
    bool awaitSeen = _visitList(node.values);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitFunctionDefinition(FunctionDefinition node) {
    if (_entryFunction) {
      _entryFunction = false;
      // TODO(sigmund) check that return type is Dynamic or a future.
      if (node.initializers != null) {
        for (Expression e in node.initializers) {
          if (e.visit(this)) {
            world.error('Await expressions are not allowed in initializers.',
                e.span);
          }
        }
      }
      if (node.body != null && node.body.visit(this)) {
        nodesWithAwait.add(node);
        return true;
      }
      return false;
    } else {
      // Do not analyze nested functions now, use a separate checker for that.
      nestedFunctions.add(node);
      return false;
    }
  }

  visitReturnStatement(ReturnStatement node) {
    bool awaitSeen = node.value != null && node.value.visit(this);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitThrowStatement(ThrowStatement node) {
    bool awaitSeen = node.value != null && node.value.visit(this);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitAssertStatement(AssertStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (awaitSeen) nodesWithAwait.add(node);
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
    if (node.falseBranch != null && node.falseBranch.visit(this)) {
      awaitSeen = true;
    }
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitWhileStatement(WhileStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (node.body != null && node.body.visit(this)) awaitSeen = true;
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitDoStatement(DoStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (node.body != null && node.body.visit(this)) awaitSeen = true;
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitForStatement(ForStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (node.body != null && node.body.visit(this)) awaitSeen = true;
    if (node.init != null && node.init.visit(this)) awaitSeen = true;
    if (_visitList(node.step)) awaitSeen = true;
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitForInStatement(ForInStatement node) {
    bool awaitSeen = node.list.visit(this);
    if (node.body != null && node.body.visit(this)) awaitSeen = true;
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitTryStatement(TryStatement node) {
    bool awaitSeen = (node.body != null && node.body.visit(this));
    if (_visitList(node.catches)) awaitSeen = true;
    if (node.finallyBlock != null && node.finallyBlock.visit(this)) {
      awaitSeen = true;
    }
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitSwitchStatement(SwitchStatement node) {
    bool awaitSeen = node.test.visit(this);
    if (_visitList(node.cases)) awaitSeen = true;
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitBlockStatement(BlockStatement node) {
    bool awaitSeen = _visitList(node.body);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitLabeledStatement(LabeledStatement node) {
    bool awaitSeen = node.body.visit(this);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitExpressionStatement(ExpressionStatement node) {
    bool awaitSeen = node.body.visit(this);
    if (awaitSeen) nodesWithAwait.add(node);
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
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitIndexExpression(IndexExpression node) {
    bool awaitSeen = node.target.visit(this);
    if (node.index.visit(this)) awaitSeen = true;
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitBinaryExpression(BinaryExpression node) {
    bool awaitSeen = node.x.visit(this);
    if (node.y.visit(this)) awaitSeen = true;
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitUnaryExpression(UnaryExpression node) {
    // TODO(sigmund): issue errors for ++/-- cases where we expect an l-value.
    bool awaitSeen = node.self.visit(this);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitPostfixExpression(PostfixExpression node) {
    // TODO(sigmund): issue errors for ++/-- cases where we expect an l-value.
    bool awaitSeen = node.body.visit(this);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitNewExpression(NewExpression node) {
    bool awaitSeen = _visitList(node.arguments);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitListExpression(ListExpression node) {
    bool awaitSeen = _visitList(node.values);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitMapExpression(MapExpression node) {
    bool awaitSeen = _visitList(node.items);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitConditionalExpression(ConditionalExpression node) {
    bool awaitSeen = node.test.visit(this);
    if (node.trueBranch.visit(this)) awaitSeen = true;
    if (node.falseBranch.visit(this)) awaitSeen = true;
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitIsExpression(IsExpression node) {
    bool awaitSeen = node.x.visit(this);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitParenExpression(ParenExpression node) {
    bool awaitSeen = node.body.visit(this);
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitAwaitExpression(AwaitExpression node) {
    nodesWithAwait.add(node);
    return true;
  }

  visitDotExpression(DotExpression node) {
    bool awaitSeen = node.self.visit(this);
    if (awaitSeen) nodesWithAwait.add(node);
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
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitCatchNode(CatchNode node) {
    bool awaitSeen = false;
    if (node.body != null && node.body.visit(this)) awaitSeen = true;
    if (awaitSeen) nodesWithAwait.add(node);
    return awaitSeen;
  }

  visitCaseNode(CaseNode node) {
    bool awaitSeen = false;
    for (Expression e in node.cases) {
      if (e != null && e.visit(this)) {
        world.error(
            'Await is not allowed in case expressions of switch statements.',
            e.span);
        awaitSeen = true;
      }
    }
    if (_visitList(node.statements)) awaitSeen = true;
    if (awaitSeen) nodesWithAwait.add(node);
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
        if (n != null && n.visit(this)) awaitSeen = true;
      }
    }
    return awaitSeen;
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
  static final _COMPLETER_NAME = '_a:res';
  static final _THEN_PARAM = '_a:v';
  static final _COMPLETE_METHOD = 'complete';
  static final _COMPLETE_EXCEPTION_METHOD = 'completeException';

  // TODO(sigmund): fix frog to make it possible to switch to '_a:after'. The
  // current mangling breaks across closure-boundaries.
  static final _CONTINUATION_PREFIX = '_a_after';

  /** The continuation when visiting a particular statement. */
  Queue<Statement> continuation;

  /** Counter to ensure created closure names are unique. */
  int continuationClosures = 0;

  /** Nodes containing await expressions (determined by [AwaitChecker]). */
  final NodeSet nodesWithAwait;

  AwaitProcessor(this.nodesWithAwait) : continuation = new Queue<Statement>();

  visitVariableDefinition(VariableDefinition node) {
    if (!nodesWithAwait.contains(node)) return node;
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
    if (!nodesWithAwait.contains(node)) return node;
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
    newList.addAll(newBody.dynamic.body);
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
    if (!nodesWithAwait.contains(node)) return node;
    // create a continuation closure if there are more than one statements
    // in the continuation
    Statement def = null;
    Statement afterIf = null;

     // TODO(sigmund): consider whether this optimization is worth it, or if we
     // should do it also on larger continuations.
     if (continuation.length > 1) {
      String afterIfName = '${_CONTINUATION_PREFIX}_if_$continuationClosures';
      continuationClosures++;
      def = _createContinuationClosure(afterIfName, node.span);
      afterIf = _makeCallNoArgs(afterIfName, node.span);
    } else if (continuation.length == 1) {
      afterIf = continuation.first();
    }

    final trueContinuation = new Queue();
    trueContinuation.addFirst(afterIf); // TODO(sigmund): create a deep copy
    continuation = trueContinuation;
    Statement tRes = node.trueBranch.visit(this);

    Statement fRes = null;
    if (node.falseBranch != null) {
      final falseContinuation = new Queue();
      falseContinuation.addFirst(afterIf); // TODO(sigmund): create a deep copy
      continuation = falseContinuation;
      fRes = node.falseBranch.visit(this);
      continuation = new Queue();
    } else {
      continuation = new Queue();
      continuation.addFirst(afterIf);
    }

    final newNode = new IfStatement(node.test, tRes, fRes, node.span);
    if (def != null) {
      continuation.addFirst(_returnFuture(node.span));
      continuation.addFirst(newNode);
      return def;
    }
    return newNode;
  }

  visitWhileStatement(WhileStatement node) {
    if (!nodesWithAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitDoStatement(DoStatement node) {
    if (!nodesWithAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitForStatement(ForStatement node) {
    if (!nodesWithAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitForInStatement(ForInStatement node) {
    if (!nodesWithAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitTryStatement(TryStatement node) {
    if (!nodesWithAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitSwitchStatement(SwitchStatement node) {
    if (!nodesWithAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitBlockStatement(BlockStatement node) {
    if (!nodesWithAwait.contains(node) && continuation.isEmpty()) return node;
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
    if (!nodesWithAwait.contains(node)) return node;
    // TODO(sigmund): implement
    return node;
  }

  visitExpressionStatement(ExpressionStatement node) {
    if (!nodesWithAwait.contains(node)) return node;
    // After normalization, expression statements with await can only occur at
    // the top-level or as the rhs of simple assignments (= but not +=).
    if (node.body is AwaitExpression) {
      return new ExpressionStatement(
          // TODO(sigmund): introduce temporary variable that produces no
          // shadowing to ignore the result
          _desugarAwaitCall(node.body, null), node.span);
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
    if (!nodesWithAwait.contains(node)) return node;
    return node;
  }

  visitArgumentNode(ArgumentNode node) {
    if (!nodesWithAwait.contains(node)) return node;
    return node;
  }

  visitCatchNode(CatchNode node) {
    if (!nodesWithAwait.contains(node)) return node;
    return node;
  }

  visitCaseNode(CaseNode node) {
    if (!nodesWithAwait.contains(node)) return node;
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
        ], null, null, null,
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

  /** Create a closure that contains the continuation statements. */
  _createContinuationClosure(String name, SourceSpan span) {
    List<Statement> continuationBlock = [];
    continuationBlock.addAll(continuation);
    return new FunctionDefinition([], null,
        new Identifier(name, span), [], null, null, null,
        new BlockStatement(continuationBlock, span), span);
  }

  /** Make a statement invoking a function in scope. */
  _makeCallNoArgs(String func, SourceSpan span) {
    return new ExpressionStatement(new CallExpression(
        new VarExpression(new Identifier(func, span), span), [], span), span);
  }
}

// TODO(sigmund): create the following tests:
// - await within the body of getter or setter properties
// - await in nested function
// - nested functions that are lambdas with or without name, top-level or within
// expressions
// - await in each valid AST construct
// - await in invalid locations (initializers, case expressions)
// - await in variable declarations with more than one name declared:
//     e.g. final x = 1, y = await 2, z = 3;
// - async function with a Future return type.
// - async function with incorrect return type.
// - apply await on an asynchronous function that contains awaits
//   (check propagation works)
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
