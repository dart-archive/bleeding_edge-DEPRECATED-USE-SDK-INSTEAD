// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

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
  // mangling code currently breaks across closure-boundaries.
  static final _PREFIX = '_a_';
  static final _COMPLETER_NAME = _PREFIX + 'res';
  static final _THEN_PARAM = _PREFIX + 'v';
  static final _EXCEPTION_HANDLER_PARAM = _PREFIX + 'e';
  static final _IGNORED_THEN_PARAM = _PREFIX + 'ignored_param';
  static final _CONTINUATION_PREFIX = _PREFIX + 'after_';

  static final _COMPLETE_METHOD = 'complete';
  static final _COMPLETE_EXCEPTION_METHOD = 'completeException';


  /** The continuation when visiting a particular statement. */
  Queue<Statement> continuation;

  /** Closures to call when a future throws an exception (in reverse order). */
  List<Identifier> exceptionHandlers;

  /** All try-catch blocks enclosing the current statement. */
  List<TryStatement> enclosingTrys;

  /** Counter to ensure created closure names are unique. */
  int continuationClosures = 0;

  /** Nodes containing await expressions (determined by [AwaitChecker]). */
  final NodeSet haveAwait;

  AwaitProcessor(this.haveAwait)
      : continuation = new Queue<Statement>(),
        exceptionHandlers = [],
        enclosingTrys = [];

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
            _callCompleter(_makeNull(node.span), node.span));
      }
    }

    Statement newBody = node.body.visit(this);
    // TODO(sigmund): extract type arg and put it in completer
    // We update the body in-place to make it easier to update nested functions
    // without having to rewrite the containing function's AST.
    node.body = new BlockStatement([
        _declareCompleter(null, node.span),
        _wrapInTryCatch(newBody, node == _mainMethod.definition),
        _returnFuture(node.span)], newBody.span);
    return node;
  }

  visitReturnStatement(ReturnStatement node) {
    continuation.clear();
    return _callCompleter(node.value, node.span);
  }

  visitThrowStatement(ThrowStatement node) {
    // instead of calling the exception handler here, we take a different
    // approach and use throw directly. This helps make the try-catch
    // transformation simpler.
    continuation.clear();
    return node;
  }

  visitAssertStatement(AssertStatement node) {
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
    String afterIf = _newClosureName(_CONTINUATION_PREFIX + "_if");
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

    String afterWhile = _newClosureName(_CONTINUATION_PREFIX + "_while");
    Statement def = _makeContinuation(afterWhile, node.span);
    String repeatWhile = _newClosureName(_PREFIX + "_while");

    final bodyContinuation = new Queue();
    bodyContinuation.addFirst(_callNoArg(repeatWhile, node.span));
    continuation = bodyContinuation;
    Statement body = node.body.visit(this);

    continuation = new Queue();
    continuation.addFirst(_callNoArg(afterWhile, node.span));
    continuation.addFirst(new IfStatement(node.test, body, null, node.span));
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
    // TODO(sigmund): pending to do on try-catch blocks
    // - consider when await shows in catch blocks, but not in the try block
    // - support exceptions after the await
    // - handle nested try blocks
    // - support finally
    // - consider throws within catch blocks, e.g:
    //   try { a } catch (E e1) { throw new E2(); } catch (E2 e2) { -- }

    String afterTry = _newClosureName(_CONTINUATION_PREFIX + "_try");
    Statement afterTryDef = _makeContinuation(afterTry, node.span);

    final defs = []; // closures for each catch block (avoid duplicating code).
    final catches = []; // catch clauses of the transformed try-catch block

    // Catch blocks are passed as an exception handler on de-sugared awaits:
    final handlerName = new Identifier(
        _newClosureName(_PREFIX + "exception_handler"), node.span);
    // TODO(sigmund): add trace argument (library change in Future<T>)
    final handlerArg = new Identifier(_EXCEPTION_HANDLER_PARAM, node.span);
    final handlerBody = [];

    // Transform each catch-block internally.
    bool untypedCatch = false; // When true, the exception handler is smaller.
    for (CatchNode n in node.catches) {
      String fname = _newClosureName(_PREFIX + "catch");

      // Code in transformed catch-block:
      continuation = new Queue();
      continuation.addFirst(_callNoArg(afterTry, node.span));
      continuation.addFirst(n.body.visit(this));

      defs.add(_makeCatchFunction(n, fname));
      catches.add(new CatchNode(n.exception, n.trace,
          new BlockStatement(
            [_callCatchFunction(n, fname), _returnFuture(n.span)], n.span),
          n.span));

      // Code in exception handler:
      if (!untypedCatch) {
        final exceptionHandlerCases = [
            _callCatchFunctionHelper(fname, handlerArg, null, n.span),
            _returnBoolean(n.span, true)];
        if (n.exception.type == null) {
          handlerBody.addAll(exceptionHandlerCases);
          untypedCatch = true;
        } else {
          handlerBody.add(new IfStatement(
                new IsExpression(true,
                    new VarExpression(handlerArg, n.span),
                    n.exception.type, n.span),
                new BlockStatement(exceptionHandlerCases, n.span),
                null, n.span));
        }
      }
    }

    if (!untypedCatch) {
      handlerBody.add(_returnBoolean(node.span, false));
    }

    // Declare the exception handler
    final handlerDef = new FunctionDefinition([], null, handlerName,
        [new FormalNode(false, false, null, handlerArg, null, node.span)],
        null, null, new BlockStatement(handlerBody, node.span), node.span);

    // Transform the try body, tracking the enclosing try blocks and
    // exception handlers.
    enclosingTrys.addLast(new TryStatement(
          null /* this is replaced with code in [_desugarAwaitCall] */,
          catches, node.finallyBlock, node.span));
    exceptionHandlers.addLast(handlerName);
    continuation = new Queue();
    continuation.addFirst(_callNoArg(afterTry, node.span));
    Statement body = node.body.visit(this);
    exceptionHandlers.removeLast();
    enclosingTrys.removeLast();

    continuation = new Queue();
    continuation.addAll(defs);
    continuation.add(handlerDef);
    continuation.add(new TryStatement(body,
          catches, node.finallyBlock, node.span));
    continuation.add(_callNoArg(afterTry, node.span));
    return afterTryDef;
  }

  /**
   * Create a closure representing the catch block. The closure takes either 1
   * or 2 args, depending on whether [n] has a `trace` declaration.
   */
  Statement _makeCatchFunction(CatchNode n, String fname) {
    if (n.trace == null) {
      return _makeContinuation1(fname,
          n.exception.type, n.exception.name, n.span);
    } else {
      return _makeContinuation2(fname,
          n.exception.type, n.exception.name,
          n.trace.type, n.trace.name, n.span);
    }
  }

  /** Calls the catch function declared for [n] using [_makeCatchFunction]. */
  Statement _callCatchFunction(CatchNode n, String fname) {
    return _callCatchFunctionHelper(fname, n.exception.name,
        n.trace == null ?  null : n.trace.name, n.span);
  }

  /** Helper to call a catch function declared using [_makeCatchFunction]. */
  Statement _callCatchFunctionHelper(
      String fname, Identifier exception, Identifier trace, SourceSpan span) {
    if (trace == null) {
      return _call1Arg(fname,
          new VarExpression(exception, exception.span), span);
    } else {
      return _call2Args(fname, new VarExpression(exception, exception.span),
          new VarExpression(trace, trace.span), span);
    }
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
      // Note: we can't inline [res] in the line below because [continuation]
      // might be redefined and we want to use the latest reference after we
      // finish visiting [node.body].
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
    // shouldn't reach here.
    world.fatal("'catch' should be handled with the enclosing try statement",
        node.span);
  }

  visitCaseNode(CaseNode node) {
    if (!haveAwait.contains(node)) return node;
    return node;
  }

  /**
   * Converts an await expression into several statements: calling
   * [:Future.then:] and propatating errors. This implementation assumes that
   * await calls are within blocks (after normalization).
   */
  CallExpression _desugarAwaitCall(AwaitExpression node, Identifier param) {
    List<Statement> afterAwait = [];
    afterAwait.addAll(continuation);
    if (afterAwait.last() is ReturnStatement) {
      // The only reason there is a `return` is because there was another await
      // and we introduced it. Such `return` is not needed in the callback.
      afterAwait.removeLast();
    }
    // Within try-blocks, we wrap the continuation in a try-catch block:
    Statement thenBody = new BlockStatement(afterAwait, node.span);
    for (int i = enclosingTrys.length - 1; i >= 0; i--) {
      final templateTry = enclosingTrys[i];
      thenBody = new TryStatement(thenBody,
          templateTry.catches, templateTry.finallyBlock, templateTry.span);
    }

    // A lambda function that executes the continuation.
    final thenArg = new LambdaExpression(
        new FunctionDefinition([], null, null,
        [new FormalNode(
          false, false, null /* infer type from body? */,
          param, null, param.span)
        ], null, null,
        thenBody, node.span),
        node.span);

    continuation.clear();
    continuation.addFirst(_returnFuture(node.span));
    // Within try-blocks, we also add an exception handler to propagate errors.
    for (final handlerName in exceptionHandlers) {
      continuation.addFirst(new ExpressionStatement(new CallExpression(
          new DotExpression(node.body,
              new Identifier('handleException', node.span), node.span),
          [new ArgumentNode(null,
              new VarExpression(handlerName, node.span),
              node.span)],
          node.span), node.span));
    }
    return _callThen(node.body, thenArg, node.span);
  }

  /** Make the call expression: [: future.then(arg); :] */
  CallExpression _callThen(Expression future, Expression arg, SourceSpan span) {
    return new CallExpression(
        new DotExpression(future, new Identifier('then', span), span),
        [new ArgumentNode(null, arg, span)], span);
  }

  /** Make the statement: [: final Completer<T> v = new Completer<T>(); :]. */
  VariableDefinition _declareCompleter(Type argType, SourceSpan span) {
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

  /**
   * Wrap [s] in a try-catch block that propagates errors through the future
   * that is returned from the asynchronous function. If the function that we
   * are generating is `main`, we add a noop listener on the resulting future.
   * Without it, we would have to make it illegal to use `await` in main. This
   * is because futures swallow exceptions if their values are never used.
   */
  Statement _wrapInTryCatch(Statement s, bool isMain) {
    final ex = new Identifier("ex", s.span);
    Statement catchStatement =
        _callCompleterException(new VarExpression(ex, ex.span), s.span);
    if (isMain) {
      final future = new DotExpression(
          new VarExpression(new Identifier(_COMPLETER_NAME, s.span), s.span),
          new Identifier("future", s.span), s.span);
      final noopHandler = new LambdaExpression(
          new FunctionDefinition([], null, null,
            [new FormalNode(false, false, null,
                new Identifier(_IGNORED_THEN_PARAM, s.span), null, s.span)
            ], null, null,
            new BlockStatement([], s.span), s.span), s.span);
      catchStatement = new BlockStatement([
          // _a_res.future.then((_ignored_param) { });
          new ExpressionStatement(
              _callThen(future, noopHandler, s.span), s.span),
          catchStatement], s.span);
    }
    return new TryStatement(s, [
        new CatchNode(new DeclaredIdentifier(null, ex, ex.span), null,
            catchStatement, s.span)], null, s.span);
  }

  /** Make the statement: [: _a$res.complete(value); :]. */
  _callCompleter(Expression value, SourceSpan span) {
    return _callTarget1Arg(_COMPLETER_NAME, _COMPLETE_METHOD, value, span);
  }

  /** Make the statement: [: _a$res.completeException(value); :]. */
  _callCompleterException(Expression value, SourceSpan span) {
    return _callTarget1Arg(
        _COMPLETER_NAME, _COMPLETE_EXCEPTION_METHOD, value, span);
  }

  /** Make the statement: [: return _a$res.future; :]. */
  _returnFuture(SourceSpan span) {
    return new ReturnStatement(
        new DotExpression(
            new VarExpression(new Identifier(_COMPLETER_NAME, span), span),
            new Identifier("future", span), span), span);
  }

  /** Create a unique name for a continuation. */
  String _newClosureName(String name) {
    continuationClosures++;
    return '${name}_$continuationClosures';
  }

  /** Return the current continuation as a statement block for a method body. */
  Statement _continuationAsBody(SourceSpan span) {
    if (continuation.length == 1 && continuation.first() is BlockStatement) {
      // No need to wrap a single block statement within a block statement:
      return continuation.first();
    } else {
      List<Statement> continuationBlock = [];
      continuationBlock.addAll(continuation);
      return new BlockStatement(continuationBlock, span);
    }
  }

  /** Create a closure that contains the continuation statements. */
  FunctionDefinition _makeContinuation(String mName, SourceSpan span) {
    return new FunctionDefinition([], null,
        new Identifier(mName, span), [], null, null,
        _continuationAsBody(span), span);
  }

  /** Create a 1-arg closure that contains the continuation statements. */
  FunctionDefinition _makeContinuation1(String mName,
      TypeReference arg1Type, Identifier arg1Name, SourceSpan span) {
    return new FunctionDefinition([], null,
        new Identifier(mName, span),
        [new FormalNode(false, false, arg1Type, arg1Name, null, arg1Name.span)],
        null, null, _continuationAsBody(span), span);
  }

  /** Create a 2-arg closure that contains the continuation statements. */
  FunctionDefinition _makeContinuation2(
      String mName, TypeReference arg1Type, Identifier arg1Name,
      TypeReference arg2Type, Identifier arg2Name, SourceSpan span) {
    return new FunctionDefinition([], null,
        new Identifier(mName, span),
        [new FormalNode(false, false, arg1Type, arg1Name, null, arg1Name.span),
         new FormalNode(false, false, arg2Type, arg2Name, null, arg2Name.span)],
        null, null, _continuationAsBody(span), span);
  }

  /** Make a statement invoking a function in scope. */
  Statement _callNoArg(String mName, SourceSpan span) {
    return new ExpressionStatement(new CallExpression(
        new VarExpression(new Identifier(mName, span), span), [], span), span);
  }

  /** Make the statement: [: target.method(value); :]. */
  Statement _callTarget1Arg(
      String target, String method, Expression value, SourceSpan span) {
    if (value == null) value = _makeNull(span);
    return new ExpressionStatement(new CallExpression(
        new DotExpression(
            new VarExpression(new Identifier(target, span), span),
            new Identifier(method, span), span),
        [new ArgumentNode(null, value, value.span)], span), span);
  }

  /** Make the statement: [: f(value); :]. */
  Statement _call1Arg(String f, Expression value, SourceSpan span) {
    if (value == null) value = _makeNull(span);
    return new ExpressionStatement(new CallExpression(
        new VarExpression(new Identifier(f, span), span),
        [new ArgumentNode(null, value, value.span)], span), span);
  }

  /** Make the statement: [: f(a, b); :]. */
  Statement _call2Args(String f, Expression a, Expression b,
      SourceSpan span) {
    if (a == null) a = _makeNull(span);
    if (b == null) b = _makeNull(span);
    return new ExpressionStatement(new CallExpression(
        new VarExpression(new Identifier(f, span), span),
        [new ArgumentNode(null, a, a.span),
         new ArgumentNode(null, b, b.span)], span), span);
  }

  /** Make a return statement for a boolean value. */
  Statement _returnBoolean(SourceSpan span, bool value) {
    return new ReturnStatement(
      new LiteralExpression(Value.fromBool(value, span), span), span);
  }

  Expression _makeNull(SourceSpan span) {
    return new LiteralExpression(Value.fromNull(span), span);
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
