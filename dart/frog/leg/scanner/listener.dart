// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A listener for parser events.
 */
class Listener {
  int classCount = 0;
  int aliasCount = 0;
  int interfaceCount = 0;
  int libraryTagCount = 0;
  int topLevelMemberCount = 0;
  Identifier previousIdentifier = null;
  final Canceler canceler;

  Link<DeclarationBuilder> builders;
  Link<Element> topLevelElements;

  Listener(Canceler this.canceler)
    : builders = const EmptyLink<DeclarationBuilder>(),
      topLevelElements = const EmptyLink<Element>();

  void beginLibraryTag(Token token) {
    canceler.cancel("Cannot handle library tags");
    libraryTagCount++;
  }

  void beginClass(Token token) {
    classCount++;
    push(token, buildClassElement);
  }

  Element buildClassElement(DeclarationBuilder declaration) {
    canceler.cancel("Cannot handle classes");
  }

  void endClass(Token token) {
    handleDeclaration(pop(), token);
  }

  void beginInterface(Token token) {
    interfaceCount++;
    push(token, buildInterfaceElement);
  }

  Element buildInterfaceElement(DeclarationBuilder declaration) {
    canceler.cancel("Cannot handle interfaces");
  }

  void endInterface(Token token) {
    handleDeclaration(pop(), token);
  }

  void beginFunctionTypeAlias(Token token) {
    aliasCount++;
    push(token, buildFunctionTypeAliasElement);
  }

  Element buildFunctionTypeAliasElement(DeclarationBuilder declaration) {
    canceler.cancel("Cannot handle typedefs");
  }

  void endFunctionTypeAlias(Token token) {
    handleDeclaration(pop(), token);
  }

  void beginTopLevelMember(Token token) {
    topLevelMemberCount++;
  }

  void topLevelMethod(Token token) {
    push(token, buildMethod);
    builders.head.name = previousIdentifier.source;
  }

  Element buildMethod(DeclarationBuilder declaration) {
    return new PartialFunctionElement(declaration.name,
                                      declaration.beginToken,
                                      declaration.endToken);
  }

  void topLevelField(Token token) {
    push(token, buildField);
    builders.head.name = previousIdentifier.source;
  }

  Element buildField(DeclarationBuilder declaration) {
    canceler.cancel("Cannot handle fields");
  }

  void endTopLevelMember(Token token) {
    handleDeclaration(pop(), token);
  }

  void beginTypeVariable(Token token) {
  }

  void endTypeVariable(Token token) {
  }

  void beginTypeVariables(Token token) {
  }

  void identifier(Token token) {
    previousIdentifier = new Identifier(token);
  }

  void beginTypeArguments(Token token) {
  }

  Token expected(SourceString string, Token token) {
    canceler.cancel("Expected '$string', but got '$token' " +
                    "@ ${token.charOffset}");
  }

  void unexpectedEof() {
    canceler.cancel("Unexpected end of file");
  }

  void notIdentifier(Token token) {
    canceler.cancel("Expected identifier, but got '$token' " +
                    "@ ${token.charOffset}");
  }

  Token expectedType(Token token) {
    canceler.cancel("Expected a type, but got '$token' @ ${token.charOffset}");
  }

  Token expectedBlock(Token token) {
    canceler.cancel("Expected a block, but got '$token' @ ${token.charOffset}");
  }

  Token unexpected(Token token) {
    canceler.cancel("Unexpected token '$token' @ ${token.charOffset}");
  }

  void push(Token token, ElementBuilder builder) {
    builders = builders.prepend(new DeclarationBuilder(token, builder));
  }

  void addElement(Element element) {
    topLevelElements = topLevelElements.prepend(element);
  }

  DeclarationBuilder pop() {
    DeclarationBuilder declaration = builders.head;
    builders = builders.tail;
    return declaration;
  }

  void handleDeclaration(DeclarationBuilder declaration, Token token) {
    declaration.endToken = token;
    declaration.endToken = token;
    addElement(declaration.build());
  }

  void voidType(Token token) {
    // Ignored.
  }
}

typedef Element ElementBuilder(DeclarationBuilder declaration);

/**
 * Builder of elements.
 *
 * Instance of this class are not supposed to outlive the parser phase
 * and are used to collect data during parsing.
 */
class DeclarationBuilder {
  Token beginToken;
  Token endToken;
  SourceString name;
  ElementBuilder builderFunction;

  DeclarationBuilder(Token this.beginToken,
                     ElementBuilder this.builderFunction);

  Element build() => (builderFunction)(this);
}

/**
 * Tracks nested functions, for example, f() { g() { h() {} } }.
 */
class FunctionContext {
  FunctionContext previous;
  LinkBuilder<Statement> statements;
  ExpressionContext expressionContext;
  Link<Expression> name;
  Link<Expression> returnType;
  Token beginFormalsToken;
  Token beginBlockToken;
  NodeList formals;

  FunctionContext(this.previous) : statements = new LinkBuilder<Statement>();

  void push(Statement statement) {
    statements.addLast(statement);
  }

  FunctionExpression buildFunction(Token endToken) {
    assert(expressionContext == null ||
           expressionContext.expressions.isEmpty());
    var statements = new NodeList(beginBlockToken, statements.toLink(),
                                  endToken);
    Block block = new Block(statements);
    return new FunctionExpression(name.head, formals, block,
                                  new TypeAnnotation(returnType.head));
  }

  void buildFormals(Token endToken) {
    formals = new NodeList(beginFormalsToken, const EmptyLink<Node>(), endToken,
                           const SourceString(","));
  }
}

/**
 * Tracks nested expressions, for example, f(g(h())).
 */
class ExpressionContext {
  ExpressionContext previous;
  Link<Expression> expressions;
  NodeList nodes;
  Token beginToken;

  ExpressionContext(this.previous)
    : expressions = const EmptyLink<Expression>();

  void push(Expression expression) {
    expressions = expressions.prepend(expression);
  }

  Node pop() {
    assert(!expressions.isEmpty());
    Node head = expressions.head;
    expressions = expressions.tail;
    return head;
  }

  Link<Expression> popAll() {
    Link<Expression> result = expressions;
    expressions = const EmptyLink<Expression>();
    return result;
  }
}

class BodyListener extends Listener {
  final Logger logger;
  FunctionContext functionContext;
  ExpressionContext expressionContext;
  FunctionExpression functionExpression;

  BodyListener(Canceler canceler, Logger this.logger)
    : super(canceler),
      expressionContext = new ExpressionContext(null);

  void beginFormalParameters(Token token) {
    functionContext.beginFormalsToken = token;
  }

  void endFormalParameters(Token token) {
    functionContext.buildFormals(token);
  }

  void beginArguments(Token token) {
    pushExpressionContext(token);
  }

  void endArguments(Token token) {
    NodeList nodes = new NodeList(expressionContext.beginToken,
                                  popAllExpressions().reverse(), token,
                                  const SourceString(","));
    popExpressionContext();
    expressionContext.nodes = nodes;
  }

  void beginReturnStatement(Token token) {
  }

  void endReturnStatement(Token token) {
    Link<Expression> expressions = popAllExpressions();
    assert(expressions.isEmpty() || expressions.tail.isEmpty());
    pushStatement(new Return(token, expressions.head));
  }

  void beginExpressionStatement(Token token) {
  }

  void endExpressionStatement(Token token) {
    pushStatement(new ExpressionStatement(popExpression(), token));
  }

  void onError(Token token, var error) {
    canceler.cancel("internal error @ ${token.charOffset}: '${token.value}'" +
                    ": ${error}");
  }

  void handleLiteralInt(Token token) {
    pushExpression(new LiteralInt(token, onError));
  }

  void handleLiteralDouble(Token token) {
    pushExpression(new LiteralDouble(token, onError));
  }

  void handleLiteralBool(Token token) {
    pushExpression(new LiteralBool(token, onError));
  }

  void handleLiteralString(Token token) {
    pushExpression(new LiteralString(token));
  }

  void binaryExpression(Token token) {
    NodeList arguments = new NodeList(null, new Link<Node>(popExpression()),
                                      null, null);
    pushExpression(new Send(popExpression(), new Operator(token), arguments));
  }

  void beginSend(Token token) {
  }

  void endSend(Token token) {
    pushExpression(new Send(null, popExpression(), expressionContext.nodes));
  }

  void identifier(Token token) {
    pushExpression(new Identifier(token));
  }

  void voidType(Token token) {
    pushExpression(new Identifier(token));
  }

  void beginFunction(Token token) {
    functionContext = new FunctionContext(functionContext);
  }

  void beginFunctionName(Token token) {
    functionContext.returnType = popAllExpressions();
  }

  void endFunctionName(Token token) {
    functionContext.name = popAllExpressions();
  }

  void beginFunctionBody(Token token) {
    functionContext.beginBlockToken = token;
  }

  void endFunctionBody(Token token) {
    functionExpression = functionContext.buildFunction(token);
  }

  void emptyFunctionBody(Token token) {
    functionExpression = functionContext.buildFunction(token);
  }

  void beginVariablesDeclaration(Token token) {
  }

  void endVariablesDeclaration(Token token) {
    NodeList variables = new NodeList(null, popAllExpressions().reverse());
    pushStatement(new VariableDefinitions(null, null, variables, token));
  }

  void beginInitializedIdentifier(Token token) {
  }

  void endInitializedIdentifier() {
  }

  void beginInitializer(Token token) {
    pushExpressionContext(token);
  }

  void endInitializer(Token assignmentOperator) {
    Expression initializer = popExpression();
    Operator operator = new Operator(assignmentOperator);
    popExpressionContext();
    NodeList arguments = new NodeList.singleton(initializer);
    pushExpression(new Send(popExpression(), operator, arguments));
  }

  void handleVarKeyword(Token token) {
  }

  void beginIfStatement(Token token) {
  }

  void pushExpression(Expression expression) {
    // logger.log("pushExpression($expression)");
    expressionContext.push(expression);
  }

  Node popExpression() => expressionContext.pop();

  Link<Expression> popAllExpressions() => expressionContext.popAll();

  void pushExpressionContext(Token token) {
    expressionContext = new ExpressionContext(expressionContext);
    expressionContext.beginToken = token;
  }

  void popExpressionContext() {
    assert(expressionContext.expressions.isEmpty());
    expressionContext = expressionContext.previous;
  }

  void pushStatement(Statement statement) {
    functionContext.push(statement);
  }
}
