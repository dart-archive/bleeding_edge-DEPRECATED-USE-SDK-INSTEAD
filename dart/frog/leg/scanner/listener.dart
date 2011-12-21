// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

final bool VERBOSE = false;

class Listener {
  void beginArguments(Token token) {
  }

  void endArguments(int count, Token beginToken, Token endToken) {
  }

  void beginBlock(Token token) {
  }

  void endBlock(int count, Token beginToken, Token endToken) {
  }

  void beginClassBody(Token token) {
  }

  void endClassBody(int memberCount, Token beginToken, Token endToken) {
  }

  void beginClassDeclaration(Token token) {
  }

  void endClassDeclaration(int interfacesCount, Token beginToken,
                           Token extendsKeyword, Token implementsKeyword,
                           Token endToken) {
  }

  void beginDoWhileStatement(Token token) {
  }

  void endDoWhileStatement(Token doKeyword, Token whileKeyword,
                           Token endToken) {
  }

  void beginExpressionStatement(Token token) {
  }

  void endExpressionStatement(Token token) {
  }

  void beginDefaultClause(Token token) {
  }

  void handleNoDefaultClause(Token token) {
  }

  void endDefaultClause(Token defaultKeyword) {
  }

  void beginFactoryMethod(Token token) {
  }

  void endFactoryMethod(Token factoryKeyword, Token periodBeforeName) {
  }

  void beginFormalParameter(Token token) {
  }

  void endFormalParameter(Token token, Token thisKeyword) {
  }

  void beginFormalParameters(Token token) {
  }

  void endFormalParameters(int count, Token beginToken, Token endToken) {
  }

  void endFields(int count, Token beginToken, Token endToken) {
  }

  void beginForStatement(Token token) {
  }

  void endForStatement(int updateExpressionCount,
                       Token beginToken, Token endToken) {
  }

  void endForInStatement(Token beginToken, Token inKeyword, Token endToken) {
  }

  void beginFunction(Token token) {
  }

  void endFunction(Token token) {
  }

  void beginFunctionBody(Token token) {
  }

  void endFunctionBody(int count, Token beginToken, Token endToken) {
  }

  void handleNoFunctionBody(Token token) {
  }

  void beginFunctionName(Token token) {
  }

  void endFunctionName(Token token) {
  }

  void beginFunctionTypeAlias(Token token) {
  }

  void endFunctionTypeAlias(Token token) {
  }

  void beginIfStatement(Token token) {
  }

  void endIfStatement(Token ifToken, Token elseToken) {
  }

  void beginInitializedIdentifier(Token token) {
  }

  void endInitializedIdentifier() {
  }

  void beginInitializer(Token token) {
  }

  void endInitializer(Token assignmentOperator) {
  }

  void beginInitializers(Token token) {
  }

  void endInitializers(int count, Token beginToken, Token endToken) {
  }

  void handleNoInitializers() {
  }

  void beginInterface(Token token) {
  }

  void endInterface(int supertypeCount, Token token) {
  }

  void beginLabelledStatement(Token token) {
  }

  void endLabelledStatement(Token colon) {
  }

  void beginLibraryTag(Token token) {
  }

  void endLibraryTag(bool hasPrefix, Token beginToken, Token endToken) {
  }

  void beginLiteralMapEntry(Token token) {
  }

  void endLiteralMapEntry(Token token) {
  }

  void beginMember(Token token) {
  }

  void endMethod(Token beginToken, Token period, Token endToken) {
  }

  void beginOptionalFormalParameters(Token token) {
  }

  void endOptionalFormalParameters(int count,
                                   Token beginToken, Token endToken) {
  }

  void beginReturnStatement(Token token) {
  }

  void endReturnStatement(bool hasExpression,
                          Token beginToken, Token endToken) {
  }

  void beginSend(Token token) {
  }

  void endSend(Token token) {
  }

  void beginSwitchStatement(Token token) {
  }

  void endSwitchStatement(Token switchKeyword) {
  }

  void beginThrowStatement(Token token) {
  }

  void endThrowStatement(Token throwToken, Token endToken) {
  }

  void endRethrowStatement(Token throwToken, Token endToken) {
  }

  void beginTopLevelMember(Token token) {
  }

  void endTopLevelFields(int count, Token beginToken, Token endToken) {
  }

  void endTopLevelMethod(Token beginToken, Token endToken) {
  }

  void beginTryStatement(Token token) {
  }

  void handleCatchBlock(Token catchKeyword) {
  }

  void handleFinallyBlock(Token finallyKeyword) {
  }

  void endTryStatement(int catchCount, Token tryKeyword, Token finallyKeyword) {
  }

  void endType(int count, Token beginToken, Token endToken) {
  }

  void beginTypeArguments(Token token) {
  }

  void endTypeArguments(int count, Token beginToken, Token endToken) {
  }

  void handleNoTypeArguments(Token token) {
  }

  void beginTypeVariable(Token token) {
  }

  void endTypeVariable(Token token) {
  }

  void beginTypeVariables(Token token) {
  }

  void endTypeVariables(int count, Token beginToken, Token endToken) {
  }

  void beginUnamedFunction(Token token) {
  }

  void endUnamedFunction(Token token) {
  }

  void beginVariablesDeclaration(Token token) {
  }

  void endVariablesDeclaration(int count, Token endToken) {
  }

  void beginWhileStatement(Token token) {
  }

  void endWhileStatement(Token whileKeyword, Token endToken) {
  }

  void handleAssignmentExpression(Token token) {
  }

  void handleBinaryExpression(Token token) {
  }

  void handleConditionalExpression(Token question, Token colon) {
  }

  void handleConstExpression(Token token, bool named) {
  }

  void handleFunctionTypedFormalParameter(Token token) {
  }

  void handleIdentifier(Token token) {
  }

  void handleIndexedExpression(Token openCurlyBracket,
                               Token closeCurlyBracket) {
  }

  void handleIsOperator(Token operathor, Token not, Token endToken) {
    // TODO(ahe): Rename [operathor] to "operator" when VM bug is fixed.
  }

  void handleLiteralBool(Token token) {
  }

  void handleBreakStatement(bool hasTarget,
                            Token breakKeyword, Token endToken) {
  }

  void handleContinueStatement(bool hasTarget,
                               Token continueKeyword, Token endToken) {
  }

  void handleEmptyStatement(Token token) {
  }

  void handleLiteralDouble(Token token) {
  }

  void handleLiteralInt(Token token) {
  }

  void handleLiteralList(int count, Token beginToken, Token constKeyword,
                         Token endToken) {
  }

  void handleLiteralMap(int count, Token beginToken, Token constKeyword,
                        Token endToken) {
  }

  void handleLiteralNull(Token token) {
  }

  void handleLiteralString(Token token) {
  }

  void handleModifier(Token token) {
  }

  void handleModifiers(int count) {
  }

  void handleNamedArgument(Token colon) {
  }

  void handleNewExpression(Token token, bool named) {
  }

  void handleNoArguments(Token token) {
  }

  void handleNoExpression(Token token) {
  }

  void handleNoType(Token token) {
  }

  void handleNoTypeVariables(Token token) {
  }

  void handleOperatorName(Token operatorKeyword, Token token) {
  }

  void handleParenthesizedExpression(BeginGroupToken token) {
  }

  void handleQualified(Token period) {
  }

  void handleStringInterpolationParts(int count) {
  }

  void handleSuperExpression(Token token) {
  }

  void handleThisExpression(Token token) {
  }

  void handleUnaryPostfixAssignmentExpression(Token token) {
  }

  void handleUnaryPrefixExpression(Token token) {
  }

  void handleUnaryPrefixAssignmentExpression(Token token) {
  }

  void handleValuedFormalParameter(Token equals, Token token) {
  }

  void handleVoidKeyword(Token token) {
  }

  Token expected(String string, Token token) {
    error("Expected '$string', but got '$token'", token);
  }

  void expectedIdentifier(Token token) {
    error("Expected identifier, but got '$token'", token);
  }

  Token expectedType(Token token) {
    error("Expected a type, but got '$token'", token);
  }

  Token expectedBlock(Token token) {
    error("Expected a block, but got '$token'", token);
  }

  Token unexpected(Token token) {
    error("Unexpected token '$token'", token);
  }

  void recoverableError(String message, [Token token, Node node]) {
    if (token === null && node !== null) {
      token = node.getBeginToken();
    }
    error(message, token);
  }

  void error(String message, Token token) {
    throw new ParserError("$message @ ${token.charOffset}");
  }
}

class ParserError {
  final String reason;
  ParserError(this.reason);
  toString() => reason;
}

/**
 * A listener for parser events.
 */
class ElementListener extends Listener {
  final Canceler canceler;

  Link<Node> nodes = const EmptyLink<Node>();
  Link<Element> topLevelElements = const EmptyLink<Element>();
  Element compilationUnitElement = null; // TODO(ahe): Initalize this.

  ElementListener(Canceler this.canceler);

  void endLibraryTag(bool hasPrefix, Token beginToken, Token endToken) {
    // TODO(ahe): Implement this.
    canceler.cancel("Cannot handle library tags", token: beginToken);
    LiteralString prefix = null;
    Identifier argumentName = null;
    if (hasPrefix) {
      prefix = popNode();
      argumentName = popNode();
    }
    LiteralString firstArgument = popNode();
    Identifier tag = popNode();
  }

  void endClassDeclaration(int interfacesCount, Token beginToken,
                           Token extendsKeyword, Token implementsKeyword,
                           Token endToken) {
    discardNodes("implements", interfacesCount);
    TypeAnnotation supertype = popNode();
    Identifier name = popNode();
    pushElement(new PartialClassElement(name.source, beginToken, endToken));
  }

  void endDefaultClause(Token defaultKeyword) {
    canceler.cancel("Default clauses are not implemented",
                    token: defaultKeyword);
  }

  void handleNoDefaultClause(Token token) {
    pushNode(null);
  }

  void endInterface(int supertypeCount, Token token) {
    // TODO(ahe): Implement this.
    canceler.cancel("Cannot handle interfaces", token: token);
    Node defaultClause = popNode();
    discardNodes("extends", supertypeCount);
    Identifier name = popNode();
  }

  void endFunctionTypeAlias(Token token) {
    // TODO(ahe): Implement this.
    canceler.cancel("Cannot handle typedefs", token: token);
    NodeList parameters = popNode();
    Identifier name = popNode();
    TypeAnnotation returnType = popNode();
  }

  void endTopLevelMethod(Token beginToken, Token endToken) {
    Identifier name = popNode();
    Modifiers modifiers = popNode();
    pushElement(new PartialFunctionElement(name.source, beginToken, endToken,
                                           ElementKind.FUNCTION, modifiers,
                                           compilationUnitElement));
  }

  void endTopLevelFields(int count, Token beginToken, Token endToken) {
    void buildFieldElement(SourceString name, Element fields) {
      pushElement(new VariableElement(name, fields, ElementKind.FIELD, null));
    }
    NodeList variables = makeNodeList(count, null, null, ",");
    Modifiers modifiers = popNode();
    buildFieldElements(modifiers, variables, buildFieldElement,
                       beginToken, endToken);
  }

  void buildFieldElements(Modifiers modifiers,
                          NodeList variables,
                          void buildFieldElement(SourceString name,
                                                 Element fields),
                          Token beginToken, Token endToken) {
    Element fields = new PartialFieldListElement(beginToken, endToken,
                                                 modifiers, null);
    for (Link<Node> nodes = variables.nodes; !nodes.isEmpty();
         nodes = nodes.tail) {
      Expression initializedIdentifier = nodes.head;
      SourceString name = initializedIdentifier.asIdentifier().source;
      buildFieldElement(name, fields);
    }
  }

  void endInitializer(Token assignmentOperator) {
    canceler.cancel("field initializers are not implemented",
                    token: assignmentOperator);
  }

  void handleIdentifier(Token token) {
    pushNode(new Identifier(token));
  }

  void handleQualified(Token period) {
    canceler.cancel("library prefixes are not implemented", token: period);
    Identifier part = popNode();
  }

  void handleNoType(Token token) {
    pushNode(null);
  }

  void endTypeVariable(Token token) {
    TypeAnnotation bound = popNode();
    Identifier name = popNode();
  }

  void endTypeArguments(int count, Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, endToken, ','));
  }

  void handleNoTypeArguments(Token token) {
    pushNode(null);
  }

  void endType(int count, Token beginToken, Token endToken) {
    NodeList typeArguments = popNode();
    Identifier typeName = popNode();
    TypeAnnotation type = new TypeAnnotation(typeName, typeArguments);
    // TODO(ahe): Don't discard library prefixes.
    discardNodes("library prefix", count - 1); // Discard library prefixes.
    pushNode(type);
  }

  void handleParenthesizedExpression(BeginGroupToken token) {
    Expression expression = popNode();
    pushNode(new ParenthesizedExpression(expression, token));
  }

  void handleModifier(Token token) {
    pushNode(new Identifier(token));
  }

  void handleModifiers(int count) {
    NodeList nodes = makeNodeList(count, null, null, null);
    pushNode(new Modifiers(nodes));
  }

  void discardNodes(String reason, int count) {
    for (; count > 0; --count) {
      Node node = popNode();
      canceler.cancel('Unsupported feature $reason', node: node);
    }
  }

  Token expected(String string, Token token) {
    canceler.cancel("Expected '$string', but got '$token'", token: token);
  }

  void expectedIdentifier(Token token) {
    canceler.cancel("Expected identifier, but got '$token'", token: token);
  }

  Token expectedType(Token token) {
    canceler.cancel("Expected a type, but got '$token'", token: token);
  }

  Token expectedBlock(Token token) {
    canceler.cancel("Expected a block, but got '$token'", token: token);
  }

  Token unexpected(Token token) {
    canceler.cancel("Unexpected token '$token'", token: token);
  }

  void recoverableError(String message, [Token token, Node node]) {
    canceler.cancel(message, token: token, node: node);
  }

  void pushElement(Element element) {
    topLevelElements = topLevelElements.prepend(element);
  }

  void pushNode(Node node) {
    nodes = nodes.prepend(node);
    if (VERBOSE) log("push $nodes");
  }

  Node popNode() {
    assert(!nodes.isEmpty());
    Node node = nodes.head;
    nodes = nodes.tail;
    if (VERBOSE) log("pop $nodes");
    return node;
  }

  Node peekNode() {
    assert(!nodes.isEmpty());
    Node node = nodes.head;
    if (VERBOSE) log("peek $node");
    return node;
  }

  void log(message) {
    print(message);
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
        (delimiter === null) ? null : new SourceString(delimiter);
    return new NodeList(beginToken, nodes, endToken, sourceDelimiter);
  }
}

class NodeListener extends ElementListener {
  final Logger logger;

  NodeListener(Canceler canceler, Logger this.logger) : super(canceler);

  void endClassDeclaration(int interfacesCount, Token beginToken,
                           Token extendsKeyword, Token implementsKeyword,
                           Token endToken) {
    NodeList body = popNode();
    NodeList interfaces =
        makeNodeList(interfacesCount, implementsKeyword, null, ",");
    TypeAnnotation supertype = popNode();
    // TODO(ahe): Type variables.
    Identifier name = popNode();
    pushNode(new ClassNode(name, supertype, interfaces, beginToken,
                           extendsKeyword, endToken));
  }

  void endClassBody(int memberCount, Token beginToken, Token endToken) {
    pushNode(makeNodeList(memberCount, beginToken, endToken, null));
  }

  void endTopLevelFields(int count, Token beginToken, Token endToken) {
    NodeList variables = makeNodeList(count, null, null, ",");
    Modifiers modifiers = popNode();
    pushNode(new VariableDefinitions(null, modifiers, variables, endToken));
  }

  void endTopLevelMethod(Token beginToken, Token endToken) {
    Statement body = popNode();
    NodeList formalParameters = popNode();
    Identifier name = popNode();
    Modifiers modifiers = popNode();
    pushElement(new PartialFunctionElement(name.source, beginToken, endToken,
                                           ElementKind.FUNCTION, modifiers,
                                           compilationUnitElement));
  }

  void endFormalParameter(Token token, Token thisKeyword) {
    NodeList name = new NodeList.singleton(popNode());
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    pushNode(new VariableDefinitions(type, modifiers, name, token));
    if (thisKeyword !== null) {
      canceler.cancel('field formal parameters not implemented',
                      token: thisKeyword);
    }
  }

  void endFormalParameters(int count, Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, endToken, ","));
  }

  void endArguments(int count, Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, endToken, ","));
  }

  void handleNoArguments(Token token) {
    pushNode(null);
  }

  void endReturnStatement(bool hasExpression,
                          Token beginToken, Token endToken) {
    Expression expression = hasExpression ? popNode() : null;
    pushNode(new Return(beginToken, endToken, expression));
  }

  void endExpressionStatement(Token token) {
    pushNode(new ExpressionStatement(popNode(), token));
  }

  void handleOnError(Token token, var error) {
    canceler.cancel("internal error: '${token.value}': ${error}", token: token);
  }

  void handleLiteralInt(Token token) {
    pushNode(new LiteralInt(token, (t, e) => handleOnError(t, e)));
  }

  void handleLiteralDouble(Token token) {
    pushNode(new LiteralDouble(token, (t, e) => handleOnError(t, e)));
  }

  void handleLiteralBool(Token token) {
    pushNode(new LiteralBool(token, (t, e) => handleOnError(t, e)));
  }

  void handleLiteralString(Token token) {
    pushNode(new LiteralString(token));
  }

  void handleLiteralNull(Token token) {
    pushNode(new LiteralNull(token));
  }

  void handleBinaryExpression(Token token) {
    Node argument = popNode();
    Node receiver = popNode();
    if ((token.stringValue === '.') &&
        (argument is Send) && (argument.asSend().receiver === null)) {
      if (argument is SendSet) internalError(node: argument);
      pushNode(argument.asSend().copyWithReceiver(receiver));
    } else {
      // TODO(ahe): If token.stringValue === '.', the resolver should
      // reject this.
      NodeList arguments = new NodeList.singleton(argument);
      pushNode(new Send(receiver, new Operator(token), arguments));
    }
  }

  void handleAssignmentExpression(Token token) {
    Node arg = popNode();
    Node node = popNode();
    Send send = node.asSend();
    if (send === null) internalError(node: node);
    if (!(send.isPropertyAccess || send.isIndex)) internalError(node: send);
    if (send.asSendSet() !== null) internalError(node: send);
    NodeList arguments;
    if (send.isIndex) {
      Link<Node> link = new Link<Node>(arg);
      link = link.prepend(send.arguments.head);
      arguments = new NodeList(null, link);
    } else {
      arguments = new NodeList.singleton(arg);
    }
    Operator op = new Operator(token);
    pushNode(new SendSet(send.receiver, send.selector, op, arguments));
  }

  void handleConditionalExpression(Token question, Token colon) {
    Node elseExpression = popNode();
    Node thenExpression = popNode();
    Node condition = popNode();
    pushNode(new Conditional(
        condition, thenExpression, elseExpression, question, colon));
  }

  void endSend(Token token) {
    NodeList arguments = popNode();
    Node selector = popNode();
    // TODO(ahe): Handle receiver.
    pushNode(new Send(null, selector, arguments));
  }

  void handleVoidKeyword(Token token) {
    pushNode(new TypeAnnotation(new Identifier(token), null));
  }

  void endFunctionBody(int count, Token beginToken, Token endToken) {
    pushNode(new Block(makeNodeList(count, beginToken, endToken, null)));
  }

  void handleNoFunctionBody(Token token) {
    pushNode(null);
  }

  void endFunction(Token token) {
    Statement body = popNode();
    NodeList initializers = popNode();
    NodeList formals = popNode();
    Identifier name = popNode();
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    pushNode(new FunctionExpression(name, formals, body, type,
                                    modifiers, initializers));
  }

  void endVariablesDeclaration(int count, Token endToken) {
    // TODO(ahe): Pick one name for this concept, either
    // VariablesDeclaration or VariableDefinitions.
    NodeList variables = makeNodeList(count, null, null, ",");
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    pushNode(new VariableDefinitions(type, modifiers, variables, endToken));
  }

  void endInitializer(Token assignmentOperator) {
    Expression initializer = popNode();
    NodeList arguments = new NodeList.singleton(initializer);
    Expression name = popNode();
    Operator op = new Operator(assignmentOperator);
    pushNode(new SendSet(null, name, op, arguments));
  }

  void endIfStatement(Token ifToken, Token elseToken) {
    Statement elsePart = (elseToken === null) ? null : popNode();
    Statement thenPart = popNode();
    ParenthesizedExpression condition = popNode();
    pushNode(new If(condition, thenPart, elsePart, ifToken, elseToken));
  }

  void endForStatement(int updateExpressionCount,
                       Token beginToken, Token endToken) {
    Statement body = popNode();
    // TODO(ahe): Don't discard.
    discardNodes("additional update expression", updateExpressionCount - 1);
    if (updateExpressionCount == 0) {
      pushNode(null); // TODO(ahe): Hack.
    }
    Node update = popNode();
    Statement condition = popNode();
    Node initializer = popNode();
    pushNode(new For(initializer, condition, update, body, beginToken));
  }

  void handleNoExpression(Token token) {
    pushNode(null);
  }

  void endDoWhileStatement(Token doKeyword, Token whileKeyword,
                           Token endToken) {
    Expression condition = popNode();
    Statement body = popNode();
    pushNode(new DoWhile(body, condition, doKeyword, whileKeyword, endToken));
  }

  void endWhileStatement(Token whileKeyword, Token endToken) {
    Statement body = popNode();
    Expression condition = popNode();
    pushNode(new While(condition, body, whileKeyword));
  }

  void endBlock(int count, Token beginToken, Token endToken) {
    pushNode(new Block(makeNodeList(count, beginToken, endToken, null)));
  }

  void endThrowStatement(Token throwToken, Token endToken) {
    Expression expression = popNode();
    pushNode(new Throw(expression, throwToken, endToken));
  }

  void endRethrowStatement(Token throwToken, Token endToken) {
    pushNode(new Throw(null, throwToken, endToken));
  }

  void handleUnaryPrefixExpression(Token token) {
    pushNode(new Send.prefix(popNode(), new Operator(token)));
  }

  void handleSuperExpression(Token token) {
    pushNode(new Identifier(token));
  }

  void handleThisExpression(Token token) {
    pushNode(new Identifier(token));
  }

  void handleUnaryAssignmentExpression(Token token, bool isPrefix) {
    Node node = popNode();
    Send send = node.asSend();
    if (send === null) internalError(node: node);
    if (!(send.isPropertyAccess || send.isIndex)) internalError(node: send);
    if (send.asSendSet() !== null) internalError(node: send);
    Node argument = null;
    if (send.isIndex) argument = send.arguments.head;
    Operator op = new Operator(token);

    if (isPrefix) {
      pushNode(new SendSet.prefix(send.receiver, send.selector, op, argument));
    } else {
      pushNode(new SendSet.postfix(send.receiver, send.selector, op, argument));
    }
  }

  void handleUnaryPostfixAssignmentExpression(Token token) {
    handleUnaryAssignmentExpression(token, false);
  }

  void handleUnaryPrefixAssignmentExpression(Token token) {
    handleUnaryAssignmentExpression(token, true);
  }

  void endInitializers(int count, Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, null, ','));
  }

  void handleNoInitializers() {
    pushNode(null);
  }

  void endFields(int count, Token beginToken, Token endToken) {
    NodeList variables = makeNodeList(count, null, null, ",");
    Modifiers modifiers = popNode();
    pushNode(new VariableDefinitions(null, modifiers, variables, endToken));
  }

  void endMethod(Token beginToken, Token period, Token endToken) {
    Statement body = popNode();
    NodeList initializers = popNode();
    NodeList formalParameters = popNode();
    Identifier name = popNode(); // TODO(ahe): What about constructors?
    if (period !== null) {
      canceler.cancel('named constructors are not implemented', node: name);
      name = popNode();
    }
    Modifiers modifiers = popNode();
    pushNode(new FunctionExpression(name, formalParameters, body, null,
                                    modifiers, initializers));
  }

  void endLiteralMapEntry(Token token) {
    Expression value = popNode();
    Expression key = popNode();
    if (key.asLiteralString() === null) {
      recoverableError('Expected a constant string', node: key);
    }
    // TODO(ahe): Create AST node.
    pushNode(new UnimplementedExpression('map entry', [key, value]));
    canceler.cancel('map entries are not implemented', node: key);
  }

  void handleLiteralMap(int count, Token beginToken, Token constKeyword,
                        Token endToken) {
    NodeList entries = makeNodeList(count, beginToken, endToken, ',');
    NodeList typeArguments = popNode();
    // TODO(ahe): Type arguments are discarded.
    // TODO(ahe): Create AST node.
    pushNode(new UnimplementedExpression('map', [entries]));
    canceler.cancel('literal map not implemented', node: entries);
  }

  void handleLiteralList(int count, Token beginToken, Token constKeyword,
                         Token endToken) {
    if (constKeyword !== null) {
      canceler.cancel('const literal lists are not implemented',
                      token: constKeyword);
    }
    NodeList elements = makeNodeList(count, beginToken, endToken, ',');
    NodeList typeArguments = popNode();
    // TODO(ahe): Type arguments are discarded.
    pushNode(new LiteralList(null, elements));
  }

  void handleIndexedExpression(Token openSquareBracket,
                               Token closeSquareBracket) {
    NodeList arguments =
        makeNodeList(1, openSquareBracket, closeSquareBracket, null);
    Node receiver = popNode();
    Node selector = new Operator.synthetic('[]');
    pushNode(new Send(receiver, selector, arguments));
  }

  void handleNewExpression(Token token, bool named) {
    if (named) {
      canceler.cancel('named constructors are not implemented', token: token);
    }
    NodeList arguments = popNode();
    if (named) {
      Identifier name = popNode();
    }
    TypeAnnotation type = popNode();
    pushNode(new NewExpression(token, new Send(null, type, arguments)));
  }

  void handleConstExpression(Token token, bool named) {
    canceler.cancel('const expressions are not implemented', token: token);
    NodeList arguments = popNode();
    if (named) {
      Identifier name = popNode();
    }
    TypeAnnotation type = popNode();
    pushNode(new NewExpression(token, new Send(null, type, arguments)));
  }

  void handleOperatorName(Token operatorKeyword, Token token) {
    canceler.cancel('user defined operators are not implemented', token: token);
    pushNode(new Identifier(operatorKeyword));
  }

  void handleNamedArgument(Token colon) {
    canceler.cancel('named arguments are not implemented', token: colon);
    Expression argument = popNode();
    Identifier name = popNode();
    pushNode(new UnimplementedExpression('named argument', [name, argument]));
  }

  void handleStringInterpolationParts(int count) {
    Link<StringInterpolationPart> parts =
      const EmptyLink<StringInterpolationPart>();
    for (int i = 0; i < count; i++) {
      LiteralString string = popNode();
      Expression expression = popNode();
      parts = parts.prepend(new StringInterpolationPart(expression, string));
    }
    if (!parts.isEmpty()) {
      LiteralString string = popNode();
      NodeList nodes = new NodeList(null, parts, null, null);
      pushNode(new StringInterpolation(string, nodes));
    }
  }

  void endOptionalFormalParameters(int count,
                                   Token beginToken, Token endToken) {
    canceler.cancel('optional formal parameters are not implemented',
                    token: beginToken);
    discardNodes("optional formal parameter", count);
  }

  void handleFunctionTypedFormalParameter(Token token) {
    canceler.cancel('function typed formal parameters are not implemented',
                    token: token);
    NodeList parameters = popNode();
  }

  void handleValuedFormalParameter(Token equals, Token token) {
    canceler.cancel('valued formal parameters are not implemented',
                    token: equals);
    Expression defaultValue = popNode();
  }

  void endTryStatement(int catchCount, Token tryKeyword, Token finallyKeyword) {
    canceler.cancel('try statement is not implemented', token: tryKeyword);
  }

  void handleCatchBlock(Token catchKeyword) {
    canceler.cancel('catch blocks are not implemented', token: catchKeyword);
    Block block = popNode();
    NodeList parameters = popNode();
  }

  void handleFinallyBlock(Token finallyKeyword) {
    canceler.cancel('finally blocks are not implemented',
                    token: finallyKeyword);
    Block block = popNode();
  }

  void endSwitchStatement(Token switchKeyword) {
    canceler.cancel('switch statement is not implemented',
                    token: switchKeyword);
    ParenthesizedExpression expression = popNode();
    pushNode(new UnimplementedStatement('switch', [expression]));
  }

  void handleBreakStatement(bool hasTarget,
                            Token breakKeyword, Token endToken) {
    canceler.cancel('break statement is not implemented', token: breakKeyword);
    Identifier target = null;
    if (hasTarget) {
      target = popNode();
    }
    pushNode(new UnimplementedStatement('break', [target]));
  }

  void handleContinueStatement(bool hasTarget,
                               Token continueKeyword, Token endToken) {
    canceler.cancel('continue statement is not implemented',
                    token: continueKeyword);
    Identifier target = null;
    if (hasTarget) {
      target = popNode();
    }
    pushNode(new UnimplementedStatement('continue', [target]));
  }

  void handleEmptyStatement(Token token) {
    canceler.cancel('empty statement is not implemented', token: token);
    pushNode(new UnimplementedStatement('empty', []));
  }

  void endFactoryMethod(Token factoryKeyword, Token periodBeforeName) {
    canceler.cancel('factory methods are not implemented',
                    token: factoryKeyword);
    Statement body = popNode();
    NodeList parameters = popNode();
    Identifier name = null;
    if (periodBeforeName !== null) {
      name = popNode();
    }
    Node typeName = popNode();
    pushNode(new UnimplementedStatement('factory',
                                        [typeName, name, parameters, body]));
  }

  void endForInStatement(Token beginToken, Token inKeyword, Token endToken) {
    canceler.cancel('for-in is not implemented', token: inKeyword);
    Statement statement = popNode();
    Expression expression = popNode();
    Node variablesDeclarationOrExpression = popNode();
    pushNode(new UnimplementedStatement('for-in',
                                        [variablesDeclarationOrExpression,
                                         expression,
                                         statement]));
  }

  void endUnamedFunction(Token token) {
    Statement body = popNode();
    NodeList formals = popNode();
    pushNode(new FunctionExpression(null, formals, body, null, null, null));
  }

  void handleIsOperator(Token operathor, Token not, Token endToken) {
    TypeAnnotation type = popNode();
    Expression expression = popNode();
    NodeList arguments = new NodeList.singleton(type);
    pushNode(new Send(expression, new Operator(operathor), arguments));
  }

  void endLabelledStatement(Token colon) {
    Statement statement = popNode();
    Identifier label = popNode();
    canceler.cancel('labels are not implemented', node: label);
    pushNode(new UnimplementedStatement('labelled', [label, statement]));
  }

  void log(message) {
    logger.log(message);
  }

  void internalError([Token token, Node node]) {
    canceler.cancel('internal error', token: token, node: node);
    throw 'internal error';
  }
}

class PartialFunctionElement extends FunctionElement {
  final Token beginToken;
  final Token endToken;

  PartialFunctionElement(SourceString name,
                         Token this.beginToken, Token this.endToken,
                         ElementKind kind,
                         Modifiers modifiers,
                         Element enclosing)
    : super(name, kind, modifiers, enclosing);

  FunctionExpression parseNode(Canceler canceler, Logger logger) {
    if (node != null) return node;
    node = parse(canceler, logger, (p) => p.parseFunction(beginToken));
    return node;
  }
}

class PartialFieldListElement extends VariableListElement {
  final Token beginToken;
  final Token endToken;

  PartialFieldListElement(Token this.beginToken,
                          Token this.endToken,
                          Modifiers modifiers,
                          Element enclosing)
    : super(ElementKind.VARIABLE_LIST, modifiers, enclosing);

  Node parseNode(Canceler canceler, Logger logger) {
    if (node != null) return node;
    node = parse(canceler, logger,
        (p) => p.parseVariablesDeclaration(beginToken));
    for (Expression definition in node.definitions.nodes) {
      Send initializedField = definition.asSend();
      if (initializedField !== null) {
        canceler.cancel('field initializers are not implemented',
                        node: initializedField.argumentsNode);
      }
    }
    return node;
  }
}

Node parse(Canceler canceler, Logger logger, doParse(Parser parser)) {
  NodeListener listener = new NodeListener(canceler, logger);
  doParse(new Parser(listener));
  Node node = listener.popNode();
  assert(listener.nodes.isEmpty());
  return node;
}
