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

  Link<DeclarationBuilder> builders; // TODO(ahe): Use a stack of nodes instead.
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

class BodyListener extends Listener {
  final Logger logger;
  Link<Node> nodes = const EmptyLink(); /* <Node> Frog bug #322 + #323 */

  BodyListener(Canceler canceler, Logger this.logger) : super(canceler);

  void beginFormalParameters(Token token) {
  }

  void endFormalParameters(int count, Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, endToken, ","));
  }

  void beginArguments(Token token) {
  }

  void endArguments(int count, Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, endToken, ","));
  }

  void handleNoArgumentsOpt(Token token) {
    pushNode(null);
  }

  void beginReturnStatement(Token token) {
  }

  void endReturnStatement(bool hasExpression,
                          Token beginToken, Token endToken) {
    Expression expression = hasExpression ? popNode() : null;
    pushNode(new Return(beginToken, endToken, expression));
  }

  void beginExpressionStatement(Token token) {
  }

  void endExpressionStatement(Token token) {
    pushNode(new ExpressionStatement(popNode(), token));
  }

  void onError(Token token, var error) {
    canceler.cancel("internal error @ ${token.charOffset}: '${token.value}'" +
                    ": ${error}");
  }

  void handleLiteralInt(Token token) {
    pushNode(new LiteralInt(token, onError));
  }

  void handleLiteralDouble(Token token) {
    pushNode(new LiteralDouble(token, onError));
  }

  void handleLiteralBool(Token token) {
    pushNode(new LiteralBool(token, onError));
  }

  void handleLiteralString(Token token) {
    pushNode(new LiteralString(token));
  }

  void handleBinaryExpression(Token token) {
    NodeList arguments = new NodeList(null, new Link<Node>(popNode()),
                                      null, null);
    pushNode(new Send(popNode(), new Operator(token), arguments));
  }

  void handleAssignmentExpression(Token token) {
    NodeList arguments = new NodeList.singleton(popNode());
    Node node = popNode();
    if (node is !Send) canceler.cancel('not assignable: $node');
    Send send = node;
    if (!send.isPropertyAccess) canceler.cancel('not assignable: $node');
    if (send is SetterSend) canceler.cancel('chained assignment');
    pushNode(new SetterSend(send.receiver, send.selector, token, arguments));
  }

  void handleConditionalExpression(Token question, Token colon) {
    Node elseExpression = popNode();
    Node thenExpression = popNode();
    Node condition = popNode();
    // TODO(ahe): Create an AST node.
    canceler.cancel('conditional expression not implemented yet');
  }

  void beginSend(Token token) {
  }

  void endSend(Token token) {
    NodeList arguments = popNode();
    Node selector = popNode();
    // TODO(ahe): Handle receiver.
    pushNode(new Send(null, selector, arguments));
  }

  void identifier(Token token) {
    pushNode(new Identifier(token));
  }

  void voidType(Token token) {
    pushNode(new Identifier(token));
  }

  void beginFunction(Token token) {
  }

  void beginFunctionName(Token token) {
  }

  void endFunctionName(Token token) {
  }

  void beginFunctionBody(Token token) {
  }

  void endFunctionBody(int count, Token beginToken, Token endToken) {
    Block block = new Block(makeNodeList(count, beginToken, endToken, null));
    Node formals = popNode();
    Node name = popNode();
    // TODO(ahe): Return types are optional.
    Node type = new TypeAnnotation(popNode());
    pushNode(new FunctionExpression(name, formals, block, type));
  }

  void beginVariablesDeclaration(Token token) {
  }

  void endVariablesDeclaration(int count, Token endToken) {
    // TODO(ahe): Pick one name for this concept, either
    // VariablesDeclaration or VariableDefinitions.
    NodeList variables = makeNodeList(count, null, null, ",");
    pushNode(new VariableDefinitions(null, null, variables, endToken));
  }

  void beginInitializedIdentifier(Token token) {
  }

  void endInitializedIdentifier() {
  }

  void beginInitializer(Token token) {
  }

  void endInitializer(Token assignmentOperator) {
    Operator operator = new Operator(assignmentOperator);
    Expression initializer = popNode();
    NodeList arguments = new NodeList.singleton(initializer);
    Expression name = popNode();
    pushNode(new Send(name, operator, arguments));
  }

  void handleVarKeyword(Token token) {
  }

  void beginIfStatement(Token token) {
  }

  void endIfStatement(Token ifToken, Token elseToken) {
    Statement elsePart = (elseToken === null) ? null : popNode();
    Statement thenPart = popNode();
    NodeList condition = popNode();
    pushNode(new If(condition, thenPart, elsePart, ifToken, elseToken));
  }

  void beginBlock(Token token) {
  }

  void endBlock(int count, Token beginToken, Token endToken) {
    pushNode(new Block(makeNodeList(count, beginToken, endToken, null)));
  }

  void pushNode(Node node) {
    nodes = nodes.prepend(node);
    logger.log("push $nodes");
  }

  Node popNode() {
    assert(!nodes.isEmpty());
    Node node = nodes.head;
    nodes = nodes.tail;
    logger.log("pop $nodes");
    return node;
  }

  NodeList makeNodeList(int count, Token beginToken, Token endToken,
                        String delimiter) {
    Link<Node> nodes = const EmptyLink<Node>();
    for (; count > 0; --count) {
      // This effectively reverses the order of nodes so they end up
      // in correct (source) order.
      nodes = nodes.prepend(popNode());
    }
    SourceString sourceDelimiter =
        (delimiter == null) ? null : new SourceString(delimiter);
    return new NodeList(beginToken, nodes, endToken, sourceDelimiter);
  }
}

class PartialFunctionElement extends FunctionElement {
  final Token beginToken;
  final Token endToken;
  FunctionExpression node;

  PartialFunctionElement(SourceString name,
                         Token this.beginToken,
                         Token this.endToken)
    : super(name);

  FunctionExpression parseNode(Canceler canceler, Logger logger) {
    if (node != null) return node;

    BodyListener listener = new BodyListener(canceler, logger);
    new BodyParser(listener).parseFunction(beginToken);
    node = listener.popNode();
    logger.log("parsed function: $node");
    return node;
  }
}
