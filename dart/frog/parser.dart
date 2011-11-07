// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(jimhug): Error recovery needs major work!
/**
 * A simple recursive descent parser for the dart language.
 *
 * This parser is designed to be more permissive than the official
 * Dart grammar.  It is expected that many grammar errors would be
 * reported by a later compiler phase.  For example, a class is allowed
 * to extend an arbitrary number of base classes - this can be
 * very clearly detected and is reported in a later compiler phase.
 */
class Parser {
  Tokenizer tokenizer;

  final SourceFile source;
  /** Enables diet parse, which skips function bodies. */
  final bool diet;
  /**
   * Throw an IncompleteSourceException if the parser encounters a premature end
   * of file or an incomplete multiline string.
   */
  final bool throwOnIncomplete;
  /**
   * Allow semicolons to be omitted at the end of lines.
   * // TODO(nweiz): make this work for more than just end-of-file
   */
  final bool optionalSemicolons;

  // TODO(jimhug): Is it possible to handle initializers cleanly?
  bool _inInitializers;

  Token _previousToken;
  Token _peekToken;

  Parser(this.source, [this.diet = false, this.throwOnIncomplete = false,
      this.optionalSemicolons = false, int startOffset = 0]) {
    tokenizer = new Tokenizer(source, true, startOffset);
    _peekToken = tokenizer.next();
    _previousToken = null;
    _inInitializers = false;
  }

  /** Generate an error if [source] has not been completely consumed. */
  void checkEndOfFile() {
    _eat(TokenKind.END_OF_FILE);
  }

  /** Guard to break out of parser when an unexpected end of file is found. */
  // TODO(jimhug): Failure to call this method can lead to inifinite parser
  //   loops.  Consider embracing exceptions for more errors to reduce
  //   the danger here.
  bool isPrematureEndOfFile() {
    if (throwOnIncomplete && _maybeEat(TokenKind.END_OF_FILE) ||
        _maybeEat(TokenKind.INCOMPLETE_MULTILINE_STRING_DQ) ||
        _maybeEat(TokenKind.INCOMPLETE_MULTILINE_STRING_SQ)) {
      throw new IncompleteSourceException(_previousToken);
    } else if (_maybeEat(TokenKind.END_OF_FILE)) {
      _error('unexpected end of file', _peekToken.span);
      return true;
    } else {
      return false;
    }
  }

  ///////////////////////////////////////////////////////////////////
  // Basic support methods
  ///////////////////////////////////////////////////////////////////
  int _peek() {
    return _peekToken.kind;
  }

  Token _next() {
    _previousToken = _peekToken;
    _peekToken = tokenizer.next();
    return _previousToken;
  }

  bool _peekKind(int kind) {
    return _peekToken.kind == kind;
  }

  /* Is the next token a legal identifier?  This includes pseudo-keywords. */
  bool _peekIdentifier() {
    return TokenKind.isIdentifier(_peekToken.kind);
  }

  bool _maybeEat(int kind) {
    if (_peekToken.kind == kind) {
      _previousToken = _peekToken;
      _peekToken = tokenizer.next();
      return true;
    } else {
      return false;
    }
  }

  void _eat(int kind) {
    if (!_maybeEat(kind)) {
      _errorExpected(TokenKind.kindToString(kind));
    }
  }

  void _eatSemicolon() {
    if (optionalSemicolons && _peekKind(TokenKind.END_OF_FILE)) return;
    _eat(TokenKind.SEMICOLON);
  }

  void _errorExpected(String expected) {
    // Throw an IncompleteSourceException if that's the problem and
    // throwOnIncomplete is true
    if (throwOnIncomplete) isPrematureEndOfFile();
    var tok = _next();
    var message = 'expected $expected, but found $tok';
    _error(message, tok.span);
  }

  void _error(String message, [SourceSpan location=null]) {
    if (location === null) {
      location = _peekToken.span;
    }
    world.fatal(message, location); // syntax errors are fatal for now
  }

  /** Skips from an opening '{' to the syntactically matching '}'. */
  void _skipBlock() {
    int depth = 1;
    _eat(TokenKind.LBRACE);
    while (true) {
      var tok = _next();
      if (tok.kind == TokenKind.LBRACE) {
        depth += 1;
      } else if (tok.kind == TokenKind.RBRACE) {
        depth -= 1;
        if (depth == 0) return;
      } else if (tok.kind == TokenKind.END_OF_FILE) {
        _error('unexpected end of file during diet parse', tok.span);
        return;
      }
    }
  }

  SourceSpan _makeSpan(int start) {
    return new SourceSpan(source, start, _previousToken.end);
  }

  ///////////////////////////////////////////////////////////////////
  // Top level productions
  ///////////////////////////////////////////////////////////////////

  List<Definition> compilationUnit() {
    var ret = [];
    _maybeEat(TokenKind.HASHBANG);
    while (_peekKind(TokenKind.HASH)) {
      ret.add(directive());
    }
    while (!_maybeEat(TokenKind.END_OF_FILE)) {
      ret.add(topLevelDefinition());
    }
    return ret;
  }

  directive() {
    int start = _peekToken.start;
    _eat(TokenKind.HASH);
    var name = identifier();
    var args = arguments();
    _eatSemicolon();
    return new DirectiveDefinition(name, args, _makeSpan(start));
  }

  topLevelDefinition() {
    switch (_peek()) {
      case TokenKind.CLASS:
        return classDefinition(TokenKind.CLASS);
      case TokenKind.INTERFACE:
        return classDefinition(TokenKind.INTERFACE);
      case TokenKind.TYPEDEF:
        return functionTypeAlias();
      default:
        return declaration();
    }
  }

  evalUnit() {
    switch (_peek()) {
      case TokenKind.CLASS:
        return classDefinition(TokenKind.CLASS);
      case TokenKind.INTERFACE:
        return classDefinition(TokenKind.INTERFACE);
      case TokenKind.TYPEDEF:
        return functionTypeAlias();
      default:
        return statement();
    }
  }

  ///////////////////////////////////////////////////////////////////
  // Definition productions
  ///////////////////////////////////////////////////////////////////

  classDefinition(int kind) {
    int start = _peekToken.start;
    _eat(kind);
    var name = identifier();

    var typeParams = null;
    if (_peekKind(TokenKind.LT)) {
      typeParams = typeParameters();
    }

    var _extends = null;
    if (_maybeEat(TokenKind.EXTENDS)) {
      _extends = typeList();
    }

    var _implements = null;
    if (_maybeEat(TokenKind.IMPLEMENTS)) {
      _implements = typeList();
    }

    var _native = null;
    if (_maybeEat(TokenKind.NATIVE)) {
      _native = maybeStringLiteral();
    }

    var _factory = null;
    if (_maybeEat(TokenKind.FACTORY)) {
      _factory = type();
    }

    var body = [];
    if (_maybeEat(TokenKind.LBRACE)) {
      while (!_maybeEat(TokenKind.RBRACE)) {
        if (isPrematureEndOfFile()) break;
        body.add(declaration());
      }
    } else {
      _errorExpected('block starting with "{" or ";"');
    }
    return new TypeDefinition(kind == TokenKind.CLASS, name, typeParams,
      _extends, _implements, _native, _factory, body, _makeSpan(start));
  }

  functionTypeAlias() {
    int start = _peekToken.start;
    _eat(TokenKind.TYPEDEF);

    var di = declaredIdentifier(false);
    var typeParams = null;
    if (_peekKind(TokenKind.LT)) {
      typeParams = typeParameters();
    }
    var formals = formalParameterList();
    _eatSemicolon();

    var func = new FunctionDefinition(null, di.type, di.name, formals,
                                       null, null, _makeSpan(start));

    return new FunctionTypeDefinition(func, typeParams, _makeSpan(start));
  }

  initializers() {
    _inInitializers = true;
    var ret = [];
    do {
      ret.add(expression());
    } while (_maybeEat(TokenKind.COMMA));
    _inInitializers = false;
    return ret;
  }

  functionBody(bool inExpression) {
    int start = _peekToken.start;
    if (_maybeEat(TokenKind.ARROW)) {
      var expr = expression();
      if (!inExpression) {
        _eatSemicolon();
      }
      return new ReturnStatement(expr, _makeSpan(start));
    } else if (_peekKind(TokenKind.LBRACE)) {
      if (diet) {
        _skipBlock();
        return new DietStatement(_makeSpan(start));
      } else {
        return block();
      }
    } else if (!inExpression) {
      if (_maybeEat(TokenKind.SEMICOLON)) {
        return null;
      } else if (_maybeEat(TokenKind.NATIVE)) {
        var nativeBody = maybeStringLiteral();
        if (_peekKind(TokenKind.SEMICOLON)) {
          _eatSemicolon();
          return new NativeStatement(nativeBody, _makeSpan(start));
        } else {
          // TODO(jimhug): This is just to get isolate.dart to parse - while
          //   we figure out what's really going on in there.
          return functionBody(inExpression);
        }
      }
    }

    _error('Expected function body (neither { nor => found)');
  }

  finishField(start, modifiers, type, name, value) {
    var names = [name];
    var values = [value];

    while (_maybeEat(TokenKind.COMMA)) {
      names.add(identifier());
      if (_maybeEat(TokenKind.ASSIGN)) {
        values.add(expression());
      } else {
        values.add(null);
      }
    }

    _eatSemicolon();
    return new VariableDefinition(modifiers, type, names, values,
                                   _makeSpan(start));
  }

  finishDefinition(start, modifiers, di) {
    switch(_peek()) {
      case TokenKind.LPAREN:
        var formals = formalParameterList();
        var inits = null;
        if (_maybeEat(TokenKind.COLON)) {
          inits = initializers();
        }
        var body = functionBody(/*inExpression:*/false);
        if (di.name == null) {
          // TODO(jimhug): Must be named constructor - verify how?
          di.name = di.type.name;
        }
        return new FunctionDefinition(modifiers, di.type, di.name, formals,
                                       inits, body, _makeSpan(start));

      case TokenKind.ASSIGN:
        _eat(TokenKind.ASSIGN);
        var value = expression();
        return finishField(start, modifiers, di.type, di.name, value);

      case TokenKind.COMMA:
      case TokenKind.SEMICOLON:
        return finishField(start, modifiers, di.type, di.name, null);

      default:
        // TODO(jimhug): This error message sucks.
        _errorExpected('declaration');

        return null;
    }
  }

  declaration([bool includeOperators=true]) {
    int start = _peekToken.start;
    if (_peekKind(TokenKind.FACTORY)) {
      return factoryConstructorDeclaration();
    }

    var modifiers = _readModifiers();
    return finishDefinition(start, modifiers,
        declaredIdentifier(includeOperators));
  }

  factoryConstructorDeclaration() {
    int start = _peekToken.start;
    var factoryToken = _next();

    var names = [identifier()];
    while (_maybeEat(TokenKind.DOT)) {
      names.add(identifier());
    }
    var typeParams = null;
    if (_peekKind(TokenKind.LT)) {
      typeParams = typeParameters();
    }

    var name = null;
    var type = null;
    if (_maybeEat(TokenKind.DOT)) {
      name = identifier();
    } else if (typeParams == null) {
      if (names.length > 1) {
        name = names.removeLast();
      } else {
        name = new Identifier('', names[0].span);
      }
    } else {
      name = new Identifier('', names[0].span);
    }

    if (names.length > 1) {
      // TODO(jimhug): This is nasty to support and currently unused.
      _error('unsupported qualified name for factory', names[0].span);
    }
    type = new NameTypeReference(false, names[0], null, names[0].span);
    var di = new DeclaredIdentifier(type, name, _makeSpan(start));
    return finishDefinition(start, [factoryToken], di);
  }

  ///////////////////////////////////////////////////////////////////
  // Statement productions
  ///////////////////////////////////////////////////////////////////
  Statement statement() {
    switch (_peek()) {
      case TokenKind.BREAK:
        return breakStatement();
      case TokenKind.CONTINUE:
        return continueStatement();
      case TokenKind.RETURN:
        return returnStatement();
      case TokenKind.THROW:
        return throwStatement();
      case TokenKind.ASSERT:
        return assertStatement();

      case TokenKind.WHILE:
        return whileStatement();
      case TokenKind.DO:
        return doStatement();
      case TokenKind.FOR:
        return forStatement();

      case TokenKind.IF:
        return ifStatement();
      case TokenKind.SWITCH:
        return switchStatement();

      case TokenKind.TRY:
        return tryStatement();

      case TokenKind.LBRACE:
        return block();
      case TokenKind.SEMICOLON:
        return emptyStatement();

      case TokenKind.FINAL:
        return declaration(false);
      case TokenKind.VAR:
        return declaration(false);

      default:
        // Covers var decl, func decl, labeled stmt and real expressions.
        return finishExpressionAsStatement(expression());
    }
  }

  finishExpressionAsStatement(expr) {
    // TODO(jimhug): This method looks very inefficient - bundle tests.
    int start = expr.span.start;

    if (_maybeEat(TokenKind.COLON)) {
      var label = _makeLabel(expr);
      return new LabeledStatement(label, statement(), _makeSpan(start));
    }

    if (expr is LambdaExpression) {
      if (expr.func.body is! BlockStatement) {
        _eatSemicolon();
        expr.func.span = _makeSpan(start);
      }
      return expr.func;
    } else if (expr is DeclaredIdentifier) {
      var value = null;
      if (_maybeEat(TokenKind.ASSIGN)) {
        value = expression();
      }
      return finishField(start, null, expr.type, expr.name, value);
    } else if (_isBin(expr, TokenKind.ASSIGN) &&
               (expr.x is DeclaredIdentifier)) {
      DeclaredIdentifier di = expr.x; // TODO(jimhug): inference should handle!
      return finishField(start, null, di.type, di.name, expr.y);
    } else if (_isBin(expr, TokenKind.LT) && _maybeEat(TokenKind.COMMA)) {
      var baseType = _makeType(expr.x);
      var typeArgs = [_makeType(expr.y)];
      var gt = _finishTypeArguments(baseType, 0, typeArgs);
      var name = identifier();
      var value = null;
      if (_maybeEat(TokenKind.ASSIGN)) {
        value = expression();
      }
      return finishField(expr.span.start, null, gt, name, value);
    } else {
      _eatSemicolon();
      return new ExpressionStatement(expr, _makeSpan(expr.span.start));
    }
  }

  Expression testCondition() {
    _eat(TokenKind.LPAREN);
    var ret = expression();
    _eat(TokenKind.RPAREN);
    return ret;
  }

  BlockStatement block() {
    int start = _peekToken.start;
    _eat(TokenKind.LBRACE);
    var stmts = [];
    while (!_maybeEat(TokenKind.RBRACE)) {
      if (isPrematureEndOfFile()) break;
      stmts.add(statement());
    }
    return new BlockStatement(stmts, _makeSpan(start));
  }

  EmptyStatement emptyStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.SEMICOLON);
    return new EmptyStatement(_makeSpan(start));
  }


  IfStatement ifStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.IF);
    var test = testCondition();
    var trueBranch = statement();
    var falseBranch = null;
    if (_maybeEat(TokenKind.ELSE)) {
      falseBranch = statement();
    }
    return new IfStatement(test, trueBranch, falseBranch, _makeSpan(start));
  }

  WhileStatement whileStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.WHILE);
    var test = testCondition();
    var body = statement();
    return new WhileStatement(test, body, _makeSpan(start));
  }

  DoStatement doStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.DO);
    var body = statement();
    _eat(TokenKind.WHILE);
    var test = testCondition();
    _eatSemicolon();
    return new DoStatement(body, test, _makeSpan(start));
  }

  forStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.FOR);
    _eat(TokenKind.LPAREN);

    var init = forInitializerStatement(start);
    if (init is ForInStatement) {
      return init;
    }
    var test = null;
    if (!_maybeEat(TokenKind.SEMICOLON)) {
      test = expression();
      _eatSemicolon();
    }
    var step = [];
    if (!_maybeEat(TokenKind.RPAREN)) {
      step.add(expression());
      while (_maybeEat(TokenKind.COMMA)) {
        step.add(expression());
      }
      _eat(TokenKind.RPAREN);
    }

    var body = statement();

    return new ForStatement(init, test, step, body, _makeSpan(start));
  }

  forInitializerStatement(int start) {
    if (_maybeEat(TokenKind.SEMICOLON)) {
      return null;
    } else {
      var init = expression();
      // Weird code here is needed to handle generic type and for in
      // TODO(jmesserly): unify with block in finishExpressionAsStatement
      if (_peekKind(TokenKind.COMMA) && _isBin(init, TokenKind.LT)) {
        _eat(TokenKind.COMMA);
        var baseType = _makeType(init.x);
        var typeArgs = [_makeType(init.y)];
        var gt = _finishTypeArguments(baseType, 0, typeArgs);
        var name = identifier();
        init = new DeclaredIdentifier(gt, name, _makeSpan(init.span.start));
      }

      if (_maybeEat(TokenKind.IN)) {
        return _finishForIn(start, _makeDeclaredIdentifier(init));
      } else {
        return finishExpressionAsStatement(init);
      }
    }
  }

  _finishForIn(int start, DeclaredIdentifier di) {
    var expr = expression();
    _eat(TokenKind.RPAREN);
    var body = statement();
    return new ForInStatement(di, expr, body,
      _makeSpan(start));
  }

  tryStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.TRY);
    var body = block();
    var catches = [];

    while (_peekKind(TokenKind.CATCH)) {
      catches.add(catchNode());
    }

    var finallyBlock = null;
    if (_maybeEat(TokenKind.FINALLY)) {
      finallyBlock = block();
    }
    return new TryStatement(body, catches, finallyBlock, _makeSpan(start));
  }

  catchNode() {
    int start = _peekToken.start;
    _eat(TokenKind.CATCH);
    _eat(TokenKind.LPAREN);
    var exc = declaredIdentifier();
    var trace = null;
    if (_maybeEat(TokenKind.COMMA)) {
      trace = declaredIdentifier();
    }
    _eat(TokenKind.RPAREN);
    var body = block();
    return new CatchNode(exc, trace, body, _makeSpan(start));
  }

  switchStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.SWITCH);
    var test = testCondition();
    var cases = [];
    _eat(TokenKind.LBRACE);
    while (!_maybeEat(TokenKind.RBRACE)) {
      cases.add(caseNode());
    }
    return new SwitchStatement(test, cases, _makeSpan(start));
  }

  _peekCaseEnd() {
    var kind = _peek();
    //TODO(efortuna): also if the first is an identifier followed by a colon, we
    //have a label for the case statement.
    return kind == TokenKind.RBRACE || kind == TokenKind.CASE ||
      kind == TokenKind.DEFAULT;
  }

  caseNode() {
    int start = _peekToken.start;
    var label = null;
    if (_peekIdentifier()) {
      label = identifier();
      _eat(TokenKind.COLON);
    }
    var cases = [];
    while (true) {
      if (_maybeEat(TokenKind.CASE)) {
        cases.add(expression());
        _eat(TokenKind.COLON);
      } else if (_maybeEat(TokenKind.DEFAULT)) {
        cases.add(null);
        _eat(TokenKind.COLON);
      } else {
        break;
      }
    }
    if (cases.length == 0) {
      _error('case or default');
    }
    var stmts = [];
    while (!_peekCaseEnd() ) {
      if (isPrematureEndOfFile()) break;
      stmts.add(statement());
    }
    return new CaseNode(label, cases, stmts, _makeSpan(start));
  }

  returnStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.RETURN);
    var expr;
    if (_maybeEat(TokenKind.SEMICOLON)) {
      expr = null;
    } else {
      expr = expression();
      _eatSemicolon();
    }
    return new ReturnStatement(expr, _makeSpan(start));
  }

  throwStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.THROW);
    var expr;
    if (_maybeEat(TokenKind.SEMICOLON)) {
      expr = null;
    } else {
      expr = expression();
      _eatSemicolon();
    }
    return new ThrowStatement(expr, _makeSpan(start));
  }

  assertStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.ASSERT);
    _eat(TokenKind.LPAREN);
    var expr = expression();
    _eat(TokenKind.RPAREN);
    _eatSemicolon();
    return new AssertStatement(expr, _makeSpan(start));
  }

  breakStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.BREAK);
    var name = null;
    if (_peekIdentifier()) {
      name = identifier();
    }
    _eatSemicolon();
    return new BreakStatement(name, _makeSpan(start));
  }

  continueStatement() {
    int start = _peekToken.start;
    _eat(TokenKind.CONTINUE);
    var name = null;
    if (_peekIdentifier()) {
      name = identifier();
    }
    _eatSemicolon();
    return new ContinueStatement(name, _makeSpan(start));
  }


  ///////////////////////////////////////////////////////////////////
  // Expression productions
  ///////////////////////////////////////////////////////////////////
  expression() {
    return infixExpression(0);
  }

  _makeType(expr) {
    if (expr is VarExpression) {
      return new NameTypeReference(false, expr.name, null, expr.span);
    } else if (expr is DotExpression) {
      var type = _makeType(expr.self);
      if (type.names === null) {
        type.names = [expr.name];
      } else {
        type.names.add(expr.name);
      }
      type.span = expr.span;
      return type;
    } else {
      _error('expected type reference');
      return null;
    }
  }

  infixExpression(int precedence) {
    return finishInfixExpression(unaryExpression(), precedence);
  }

  _finishDeclaredId(type) {
    var name = identifier();
    return finishPostfixExpression(
      new DeclaredIdentifier(type, name, _makeSpan(type.span.start)));
  }

  /**
   * Takes an initial binary expression of A < B and turns it into a
   * declared identifier included the A < B piece in the type.
   */
  _fixAsType(BinaryExpression x) {
    assert(_isBin(x, TokenKind.LT));
    // TODO(jimhug): good errors when expectations are violated
    if (_maybeEat(TokenKind.GT)) {
      // The simple case of A < B > just becomes a generic type
      var base = _makeType(x.x);
      var typeParam = _makeType(x.y);
      var type = new GenericTypeReference(base, [typeParam], 0,
        _makeSpan(x.span.start));
      return _finishDeclaredId(type);
    } else {
      // The case of A < B < kicks off a lot more parsing.
      assert(_peekKind(TokenKind.LT));

      var base = _makeType(x.x);
      var paramBase = _makeType(x.y);
      var firstParam = addTypeArguments(paramBase, 1);

      var type;
      if (firstParam.depth <= 0) {
        type = new GenericTypeReference(base, [firstParam], 0,
          _makeSpan(x.span.start));
      } else if (_maybeEat(TokenKind.COMMA)) {
        type = _finishTypeArguments(base, 0, [firstParam]);
      } else {
        _eat(TokenKind.GT);
        type = new GenericTypeReference(base, [firstParam], 0,
          _makeSpan(x.span.start));
      }
      return _finishDeclaredId(type);
    }
  }

  finishInfixExpression(Expression x, int precedence) {
    while (true) {
      int kind = _peek();
      var prec = TokenKind.infixPrecedence(_peek());
      if (prec >= precedence) {
        if (kind == TokenKind.LT || kind == TokenKind.GT) {
          if (_isBin(x, TokenKind.LT)) {
            // This must be a generic type according the the Dart grammar.
            // This rule is in the grammar to forbid A < B < C and
            // A < B > C as expressions both because they don't make sense
            // and to make it easier to disambiguate the generic types.
            // There are a number of other comparison operators that are
            // also unallowed to nest in this way, but in the spirit of this
            // "friendly" parser, those will be allowed until a later phase.
            return _fixAsType(x);
          }
        }
        var op = _next();
        if (op.kind == TokenKind.IS) {
          var isTrue = !_maybeEat(TokenKind.NOT);
          var typeRef = type();
          x = new IsExpression(isTrue, x, typeRef, _makeSpan(x.span.start));
          continue;
        }
        // Using prec + 1 ensures that a - b - c will group correctly.
        // Using prec for ASSIGN ops ensures that a = b = c groups correctly.
        var y = infixExpression(prec == 2 ? prec: prec+1);
        if (op.kind == TokenKind.CONDITIONAL) {
          _eat(TokenKind.COLON);
          var z = infixExpression(prec+1);
          x = new ConditionalExpression(x, y, z, _makeSpan(x.span.start));
        } else {
          x = new BinaryExpression(op, x, y, _makeSpan(x.span.start));
        }
      } else {
        break;
      }
    }
    return x;
  }

  _isPrefixUnaryOperator(int kind) {
    switch(kind) {
      case TokenKind.ADD:
      case TokenKind.SUB:
      case TokenKind.NOT:
      case TokenKind.BIT_NOT:
      case TokenKind.INCR:
      case TokenKind.DECR:
        return true;
      default:
        return false;
    }
  }

  unaryExpression() {
    int start = _peekToken.start;
    // peek for prefixOperators and incrementOperators
    if (_isPrefixUnaryOperator(_peek())) {
      var tok = _next();
      var expr = unaryExpression();
      return new UnaryExpression(tok, expr, _makeSpan(start));
    }

    return finishPostfixExpression(primary());
  }

  argument() {
    int start = _peekToken.start;
    var expr;
    var label = null;
    if (_maybeEat(TokenKind.ELLIPSIS)) {
      label = new Identifier('...', _makeSpan(start));
    }
    expr = expression();
    if (label === null && _maybeEat(TokenKind.COLON)) {
      label = _makeLabel(expr);
      expr = expression();
    }
    return new ArgumentNode(label, expr, _makeSpan(start));
  }

  arguments() {
    var args = [];
    // TODO(jimhug): switch to forced formals when get a DeclaredId
    _eat(TokenKind.LPAREN);
    if (!_maybeEat(TokenKind.RPAREN)) {
      do {
        args.add(argument());
      } while (_maybeEat(TokenKind.COMMA));
      _eat(TokenKind.RPAREN);
    }
    return args;
  }

  finishPostfixExpression(expr) {
    switch(_peek()) {
      case TokenKind.LPAREN:
        return finishPostfixExpression(new CallExpression(expr, arguments(),
          _makeSpan(expr.span.start)));
      case TokenKind.LBRACK:
        _eat(TokenKind.LBRACK);
        var index = expression();
        _eat(TokenKind.RBRACK);
        return finishPostfixExpression(new IndexExpression(expr, index,
          _makeSpan(expr.span.start)));
      case TokenKind.DOT:
        _eat(TokenKind.DOT);
        var name = identifier();
        var ret = new DotExpression(expr, name, _makeSpan(expr.span.start));
        return finishPostfixExpression(ret);

      case TokenKind.INCR:
      case TokenKind.DECR:
        var tok = _next();
        return new PostfixExpression(expr, tok, _makeSpan(expr.span.start));

      // These are pseudo-expressions supported for cover grammar
      // must be forbidden when parsing initializers.
      case TokenKind.ARROW:
      case TokenKind.LBRACE:
        if (_inInitializers) return expr;
        var body = functionBody(true);
        return _makeFunction(expr, body);

      default:
        if (_peekIdentifier()) {
          return finishPostfixExpression(
            new DeclaredIdentifier(_makeType(expr), identifier(),
              _makeSpan(expr.span.start)));
        } else {
          return expr;
        }
    }
  }

  /** Checks if the given expression is a binary op of the given kind. */
  _isBin(expr, kind) {
    return expr is BinaryExpression && expr.op.kind == kind;
  }

  _boolTypeRef(SourceSpan span) {
    return new TypeReference(span, world.boolType);
  }

  _intTypeRef(SourceSpan span) {
    return new TypeReference(span, world.intType);
  }

  _doubleTypeRef(SourceSpan span) {
    return new TypeReference(span, world.doubleType);
  }

  _stringTypeRef(SourceSpan span) {
    return new TypeReference(span, world.stringType);
  }

  primary() {
    int start = _peekToken.start;
    switch (_peek()) {
      case TokenKind.THIS:
        _eat(TokenKind.THIS);
        return new ThisExpression(_makeSpan(start));

      case TokenKind.SUPER:
        _eat(TokenKind.SUPER);
        return new SuperExpression(_makeSpan(start));

      case TokenKind.CONST:
        _eat(TokenKind.CONST);
        if (_peekKind(TokenKind.LBRACK) || _peekKind(TokenKind.INDEX)) {
          return finishListLiteral(start, true, null);
        } else if (_peekKind(TokenKind.LBRACE)) {
          return finishMapLiteral(start, true, null);
        } else if (_peekKind(TokenKind.LT)) {
          return finishTypedLiteral(start, true);
        } else {
          return finishNewExpression(start, true);
        }

      case TokenKind.NEW:
        _eat(TokenKind.NEW);
        return finishNewExpression(start, false);

      case TokenKind.LPAREN:
        return _parenOrLambda();

      case TokenKind.LBRACK:
      case TokenKind.INDEX:
        return finishListLiteral(start, false, null);
      case TokenKind.LBRACE:
        return finishMapLiteral(start, false, null);

      // Literals
      case TokenKind.NULL:
        _eat(TokenKind.NULL);
        return new NullExpression(_makeSpan(start));

      // TODO(jimhug): Make Literal creation less wasteful - no dup span/text.
      case TokenKind.TRUE:
        _eat(TokenKind.TRUE);
        return new LiteralExpression(true, _boolTypeRef(_makeSpan(start)),
          'true', _makeSpan(start));

      case TokenKind.FALSE:
        _eat(TokenKind.FALSE);
        return new LiteralExpression(false,_boolTypeRef(_makeSpan(start)),
          'false', _makeSpan(start));

      case TokenKind.HEX_INTEGER:
        var t = _next();
        // Remove the 0x or 0X before parsing the hex number.
        return new LiteralExpression(parseHex(t.text.substring(2)),
          _intTypeRef(_makeSpan(start)), t.text, _makeSpan(start));

      case TokenKind.INTEGER:
        var t = _next();
        return new LiteralExpression(Math.parseInt(t.text),
          _intTypeRef(_makeSpan(start)), t.text, _makeSpan(start));

      case TokenKind.DOUBLE:
        var t = _next();
        return new LiteralExpression(Math.parseDouble(t.text),
          _doubleTypeRef(_makeSpan(start)), t.text, _makeSpan(start));

      case TokenKind.STRING:
        return stringLiteralExpr();

      case TokenKind.INCOMPLETE_STRING:
        return stringInterpolation();

      case TokenKind.LT:
        return finishTypedLiteral(start, false);

      case TokenKind.VOID:
      case TokenKind.VAR:
      case TokenKind.FINAL:
        return declaredIdentifier(false);

      default:
        if (!_peekIdentifier()) {
          // TODO(jimhug): Better error message.
          _errorExpected('expression');
        }
        return new VarExpression(identifier(), _makeSpan(start));
    }
  }

  stringInterpolation() {
    int start = _peekToken.start;
    var lits = [];
    var startQuote = null, endQuote = null;
    while(_peekKind(TokenKind.INCOMPLETE_STRING)) {
      var token = _next();
      var text = token.text;
      if (startQuote == null) {
        if (isMultilineString(text)) {
          endQuote = text.substring(0, 3);
          // TODO(jmesserly): HACK add a newline to everything that's not
          // the first multiline string, so we don't incorrectly strip off any
          // real newlines later in the interpolated string.
          startQuote = endQuote + '\n';
        } else {
          startQuote = endQuote = text[0];
        }
        text = text.substring(0, text.length-1) + endQuote; // fix trailing $
      } else {
        text = startQuote + text.substring(0, text.length-1) + endQuote;
      }
      lits.add(makeStringLiteral(text, token.span));
      if (_maybeEat(TokenKind.LBRACE)) {
        lits.add(expression());
        _eat(TokenKind.RBRACE);
      } else {
        var id = identifier();
        lits.add(new VarExpression(id, id.span));
      }
    }
    var tok = _next();
    if (tok.kind != TokenKind.STRING) {
      _errorExpected('interpolated string');
    }
    var text = startQuote + tok.text;
    lits.add(makeStringLiteral(text, tok.span));
    var span = _makeSpan(start);
    return new LiteralExpression(lits, _stringTypeRef(span), '\$\$\$', span);
  }

  makeStringLiteral(String text, SourceSpan span) {
    return new LiteralExpression(text, _stringTypeRef(span), text, span);
  }

  stringLiteralExpr() {
    var token = _next();
    return makeStringLiteral(token.text, token.span);
  }

  String maybeStringLiteral() {
    var kind = _peek();
    if (kind == TokenKind.STRING) {
      return parseStringLiteral(_next().text);
    } else if (kind == TokenKind.STRING_PART) {
      _next();
      _errorExpected('string literal, but found interpolated string start');
    } else if (kind == TokenKind.INCOMPLETE_STRING) {
      _next();
      _errorExpected('string literal, but found incomplete string');
    }
    return null;
  }

  _parenOrLambda() {
    int start = _peekToken.start;
    var args = arguments();
    if (!_inInitializers &&
        (_peekKind(TokenKind.ARROW) || _peekKind(TokenKind.LBRACE))) {
      var body = functionBody(true);
      var formals = _makeFormals(args);
      var func = new FunctionDefinition(null, null, null, formals, null,
        body, _makeSpan(start));
      return new LambdaExpression(func, func.span);
    } else {
      if (args.length == 1) {
        return new ParenExpression(args[0].value, _makeSpan(start));
      } else {
        _error('unexpected comma expression');
        return args[0].value;
      }
    }
  }


  _typeAsIdentifier(type) {
    // TODO(jimhug): lots of errors to check for
    return type.name;
  }

  _specialIdentifier(bool includeOperators) {
    int start = _peekToken.start;
    String name;

    switch (_peek()) {
      case TokenKind.ELLIPSIS:
        _eat(TokenKind.ELLIPSIS);
        _error('rest no longer supported', _previousToken.span);
        name = identifier().name;
        break;
      case TokenKind.THIS:
        _eat(TokenKind.THIS);
        _eat(TokenKind.DOT);
        name = 'this.${identifier().name}';
        break;
      case TokenKind.GET:
        if (!includeOperators) return null;
        _eat(TokenKind.GET);
        if (_peekIdentifier()) {
          name = 'get\$${identifier().name}';
        } else {
          name = 'get';
        }
        break;
      case TokenKind.SET:
        if (!includeOperators) return null;
        _eat(TokenKind.SET);
        if (_peekIdentifier()) {
          name = 'set\$${identifier().name}';
        } else {
          name = 'set';
        }
        break;
      case TokenKind.OPERATOR:
        if (!includeOperators) return null;
        _eat(TokenKind.OPERATOR);
        var kind = _peek();
        if (kind == TokenKind.NEGATE) {
          name = '\$negate';
          _next();
        } else {
          name = TokenKind.binaryMethodName(kind);
          if (name == null) {
            // TODO(jimhug): This is a very useful error, but we have to
            //   lose it because operator is a pseudo-keyword...
            //_errorExpected('legal operator name, but found: ${tok}');
            name = 'operator';
          } else {
            _next();
          }
        }
        break;
      default:
        return null;
    }
    return new Identifier(name, _makeSpan(start));
  }

  // always includes this and ... as legal names to simplify other code.
  declaredIdentifier([bool includeOperators=false]) {
    int start = _peekToken.start;
    var myType = null;
    var name = _specialIdentifier(includeOperators);
    if (name === null) {
      myType = type();
      name = _specialIdentifier(includeOperators);
      if (name === null) {
        if (_peekIdentifier()) {
          name = identifier();
        } else if (myType is NameTypeReference && myType.names == null) {
          name = _typeAsIdentifier(myType);
          myType = null;
        } else {
          // TODO(jimhug): Where do these errors get handled?
        }
      }
    }
    return new DeclaredIdentifier(myType, name, _makeSpan(start));
  }

  // TODO(jimhug): Move this to base <= 36 and into shared code.
  static int _hexDigit(int c) {
    if(c >= 48/*0*/ && c <= 57/*9*/) {
      return c - 48;
    } else if (c >= 97/*a*/ && c <= 102/*f*/) {
      return c - 87;
    } else if (c >= 65/*A*/ && c <= 70/*F*/) {
      return c - 55;
    } else {
      return -1;
    }
  }

  static int parseHex(String hex) {
    var result = 0;

    for (int i=0; i < hex.length; i++) {
      var digit = _hexDigit(hex.charCodeAt(i));
      assert(digit != -1);
      result = (result << 4) + digit;
    }

    return result;
  }

  finishNewExpression(int start, bool isConst) {
    var type = type();
    var name = null;
    if (_maybeEat(TokenKind.DOT)) {
      name = identifier();
    }
    var args = arguments();
    return new NewExpression(isConst, type, name, args, _makeSpan(start));
  }

  finishListLiteral(int start, bool isConst, TypeReference type) {
    if (_maybeEat(TokenKind.INDEX)) {
      // This is an empty array.
      return new ListExpression(isConst, type, [], _makeSpan(start));
    }

    var values = [];
    _eat(TokenKind.LBRACK);
    while (!_maybeEat(TokenKind.RBRACK)) {
      if (isPrematureEndOfFile()) break;
      values.add(expression());
      if (!_maybeEat(TokenKind.COMMA)) {
        _eat(TokenKind.RBRACK);
        break;
      }
    }
    return new ListExpression(isConst, type, values, _makeSpan(start));
  }

  finishMapLiteral(int start, bool isConst, TypeReference type) {
    var items = [];
    _eat(TokenKind.LBRACE);
    while (!_maybeEat(TokenKind.RBRACE)) {
      if (isPrematureEndOfFile()) break;
      // This is deliberately overly permissive - checked in later pass.
      items.add(expression());
      _eat(TokenKind.COLON);
      items.add(expression());
      if (!_maybeEat(TokenKind.COMMA)) {
        _eat(TokenKind.RBRACE);
        break;
      }
    }
    return new MapExpression(isConst, type, items, _makeSpan(start));
  }

  finishTypedLiteral(int start, bool isConst) {
    var span = _makeSpan(start);
    var typeToBeNamedLater = new NameTypeReference(false, null, null, span);
    var genericType = addTypeArguments(typeToBeNamedLater, 0);

    // TODO(jimhug): Fill in correct typeToBeNamedLater details...
    if (_peekKind(TokenKind.LBRACK) || _peekKind(TokenKind.INDEX)) {
      return finishListLiteral(start, isConst, genericType);
    } else if (_peekKind(TokenKind.LBRACE)) {
      return finishMapLiteral(start, isConst, genericType);
    } else {
      _errorExpected('array or map literal');
    }
  }

  ///////////////////////////////////////////////////////////////////
  // Some auxilary productions.
  ///////////////////////////////////////////////////////////////////
  _readModifiers() {
    var modifiers = null;
    while (true) {
      switch(_peek()) {
        case TokenKind.STATIC:
        case TokenKind.FINAL:
        case TokenKind.CONST:
        case TokenKind.ABSTRACT:
        case TokenKind.FACTORY:
          if (modifiers === null) modifiers = [];
          modifiers.add(_next());
          break;
        default:
          return modifiers;
      }
    }

    return null;
  }

  typeParameter() {
    // non-recursive - so always starts from zero depth
    int start = _peekToken.start;
    var name = identifier();
    var myType = null;
    if (_maybeEat(TokenKind.EXTENDS)) {
      myType = type(1);
    }
    return new TypeParameter(name, myType, _makeSpan(start));
  }

  typeParameters() {
    // always starts from zero depth
    _eat(TokenKind.LT);

    bool closed = false;
    var ret = [];
    do {
      // inlining typeParameter to handle scope issues?
      var tp = typeParameter();
      ret.add(tp);
      if (tp.extendsType is GenericTypeReference && tp.extendsType.depth == 0) {
        closed = true;
        break;
      }
    } while (_maybeEat(TokenKind.COMMA));
    if (!closed) {
      _eat(TokenKind.GT);
    }
    return ret;
  }

  int _eatClosingAngle(int depth) {
    if (_maybeEat(TokenKind.GT)) {
      return depth;
    } else if (depth > 0 && _maybeEat(TokenKind.SAR)) {
      return depth-1;
    } else if (depth > 1 && _maybeEat(TokenKind.SHR)) {
      return depth-2;
    } else {
      _errorExpected('>');
      return depth;
    }
  }

  addTypeArguments(TypeReference baseType, int depth) {
    _eat(TokenKind.LT);
    return _finishTypeArguments(baseType, depth, []);
  }

  _finishTypeArguments(TypeReference baseType, int depth, types) {
    var delta = -1;
    do {
      var myType = type(depth+1);
      types.add(myType);
      if (myType is GenericTypeReference && myType.depth <= depth) {
        // TODO(jimhug): Friendly error if peek(COMMA).
        delta = depth - myType.depth;
        break;
      }
    } while (_maybeEat(TokenKind.COMMA));
    if (delta >= 0) {
      depth -= delta;
    } else {
      depth = _eatClosingAngle(depth);
    }

    var span = _makeSpan(baseType.span.start);
    return new GenericTypeReference(baseType, types, depth, span);
  }

  typeList() {
    var types = [];
    do {
      types.add(type());
    } while (_maybeEat(TokenKind.COMMA));

    return types;
  }

  type([int depth = 0]) {
    int start = _peekToken.start;
    var name;
    var names = null;
    var typeArgs = null;
    var isFinal = false;

    switch (_peek()) {
      case TokenKind.VOID:
        return new TypeReference(_next().span, world.voidType);
      case TokenKind.VAR:
        return new TypeReference(_next().span, world.varType);
      case TokenKind.FINAL:
        _eat(TokenKind.FINAL);
        isFinal = true;
        name = identifier();
        break;
      default:
        name = identifier();
        break;
    }

    while (_maybeEat(TokenKind.DOT)) {
      if (names === null) names = [];
      names.add(identifier());
    }

    var typeRef = new NameTypeReference(isFinal, name, names,
      _makeSpan(start));

    if (_peekKind(TokenKind.LT)) {
      return addTypeArguments(typeRef, depth);
    } else {
      return typeRef;
    }
  }

  formalParameter(bool inOptionalBlock) {
    int start = _peekToken.start;
    var isThis = false;
    var isRest = false;
    var di = declaredIdentifier(false);
    var type = di.type;
    var name = di.name;

    var value = null;
    if (_maybeEat(TokenKind.ASSIGN)) {
      if (!inOptionalBlock) {
        _error('default values only allowed inside [optional] section');
      }
      value = expression();
    } else if (_peekKind(TokenKind.LPAREN)) {
      var formals = formalParameterList();
      var func = new FunctionDefinition(null, type, name, formals,
                                         null, null, _makeSpan(start));
      type = new FunctionTypeReference(false, func, func.span);
    }
    if (inOptionalBlock && value == null) {
      value = new NullExpression(_makeSpan(start));
    }

    return new FormalNode(isThis, isRest, type, name, value, _makeSpan(start));
  }

  formalParameterList() {
    _eat(TokenKind.LPAREN);
    var formals = [];
    var inOptionalBlock = false;
    if (!_maybeEat(TokenKind.RPAREN)) {
      if (_maybeEat(TokenKind.LBRACK)) {
        inOptionalBlock = true;
      }
      formals.add(formalParameter(inOptionalBlock));
      while (_maybeEat(TokenKind.COMMA)) {
        if (_maybeEat(TokenKind.LBRACK)) {
          if (inOptionalBlock) {
            _error('already inside an optional block', _previousToken.span);
          }
          inOptionalBlock = true;
        }
        formals.add(formalParameter(inOptionalBlock));
      }
      if (inOptionalBlock) {
        _eat(TokenKind.RBRACK);
      }
      _eat(TokenKind.RPAREN);
    }
    return formals;
  }

  identifier() {
    var tok = _next();
    if (!TokenKind.isIdentifier(tok.kind)) {
      _error('expected identifier, but found $tok', tok.span);
    }

    return new Identifier(tok.text, _makeSpan(tok.start));
  }

  ///////////////////////////////////////////////////////////////////
  // These last productions handle most ambiguities in grammar
  // They will convert expressions into other types.
  ///////////////////////////////////////////////////////////////////

  /**
   * Converts an [Expression] and a [Statment] body into a
   * [FunctionDefinition].
   */
  _makeFunction(expr, body) {
    var name, type;
    if (expr is CallExpression) {
      if (expr.target is VarExpression) {
        name = expr.target.name;
        type = null;
      } else if (expr.target is DeclaredIdentifier) {
        name = expr.target.name;
        type = expr.target.type;
      } else {
        _error('bad function');
      }
      var formals = _makeFormals(expr.arguments);
      var span =
        new SourceSpan(expr.span.file, expr.span.start, body.span.end);
      var func =
        new FunctionDefinition(null, type, name, formals, null, body, span);
      return new LambdaExpression(func, func.span);
    } else {
      _error('expected function');
    }
  }

  /** Converts a single expression into a formal or list of formals. */
  _makeFormal(expr) {
    if (expr is VarExpression) {
      return new FormalNode(false, false, null, expr.name, null, expr.span);
    } else if (expr is DeclaredIdentifier) {
      return new FormalNode(false, false, expr.type, expr.name, null,
        expr.span);
    } else if (_isBin(expr, TokenKind.ASSIGN) &&
               (expr.x is DeclaredIdentifier)) {
      DeclaredIdentifier di = expr.x; // TODO(jimhug): inference should handle!
      return new FormalNode(false, false, di.type, di.name, expr.y,
        expr.span);
    } else if (_isBin(expr, TokenKind.LT)) {
      // special signaling value to merge with next arg.
      return null;
    } else if (expr is ListExpression) {
      return _makeFormalsFromList(expr);
    } else {
      _error('expected formal', expr.span);
    }
  }

  _makeFormalsFromList(expr) {
    if (expr.isConst) {
      _error('expected formal, but found "const"', expr.span);
    } else if (expr.type != null) {
      _error('expected formal, but found generic type arguments',
         expr.type.span);
    }

    return _makeFormalsFromExpressions(expr.values, allowOptional:false);
  }

  /** Converts a list of arguments into a list of formals. */
  _makeFormals(arguments) {
    var expressions = [];
    for (int i = 0; i < arguments.length; i++) {
      final arg = arguments[i];
      if (arg.label != null) {
        _error('expected formal, but found ":"');
      }
      expressions.add(arg.value);
    }
    return _makeFormalsFromExpressions(expressions, allowOptional:true);
  }

  /** Converts a list of expressions into a list of formals. */
  _makeFormalsFromExpressions(expressions, [bool allowOptional]) {
    var formals = [];
    for (int i = 0; i < expressions.length; i++) {
      var formal = _makeFormal(expressions[i]);
      if (formal == null) {
        // special signal that we have the A<C case
        var baseType = _makeType(expressions[i].x);
        var typeParams = [_makeType(expressions[i].y)];
        i++;
        while (i < expressions.length) {
          var expr = expressions[i++];
          // Looking for D > m closer
          if (_isBin(expr, TokenKind.GT)) {
            typeParams.add(_makeType(expr.x));
            var type = new GenericTypeReference(baseType, typeParams, 0,
              _makeSpan(baseType.span.start));
            var name = null;
            if (expr.y is VarExpression) {
              // TODO(jimhug): Should be handled by inference!
              VarExpression ve = expr.y;
              name = ve.name;
            } else {
              _error('expected formal', expr.span);
            }
            formal = new FormalNode(false, false, type, name, null,
              _makeSpan(expressions[0].span.start));
            break;
          } else {
            typeParams.add(_makeType(expr));
          }
        }
        formals.add(formal);

      } else if (formal is List) {
        formals.addAll(formal);
        if (!allowOptional) {
          _error('unexpected nested optional formal', expressions[i].span);
        }

      } else {
        formals.add(formal);
      }
    }
    return formals;
  }

  /** Converts an expression to a [DeclaredIdentifier]. */
  _makeDeclaredIdentifier(e) {
    if (e is VarExpression) {
      return new DeclaredIdentifier(null, e.name, e.span);
    } else if (e is DeclaredIdentifier) {
      return e;
    } else {
      _error('expected declared identifier');
      return new DeclaredIdentifier(null, null, e.span);
    }
  }

  /** Converts an expression into a label. */
  _makeLabel(expr) {
    if (expr is VarExpression) {
      return expr.name;
    } else {
      _errorExpected('label');
      return null;
    }
  }
}

class IncompleteSourceException implements Exception {
  final Token token;

  IncompleteSourceException(this.token);

  String toString() {
    if (token.span == null) return 'Unexpected $token';
    return token.span.toMessageString('Unexpected $token');
  }
}
