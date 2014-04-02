// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of scanner;

const bool VERBOSE = false;

/**
 * A parser event listener that does nothing except throw exceptions
 * on parser errors.
 */
class Listener {
  void beginArguments(Token token) {
  }

  void endArguments(int count, Token beginToken, Token endToken) {
  }

  void beginBlock(Token token) {
  }

  void endBlock(int count, Token beginToken, Token endToken) {
  }

  void beginCascade(Token token) {
  }

  void endCascade() {
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

  void beginCombinators(Token token) {
  }

  void endCombinators(int count) {
  }

  void beginCompilationUnit(Token token) {
  }

  void endCompilationUnit(int count, Token token) {
  }

  void beginConstructorReference(Token start) {
  }

  void endConstructorReference(Token start, Token periodBeforeName,
                               Token endToken) {
  }

  void beginDoWhileStatement(Token token) {
  }

  void endDoWhileStatement(Token doKeyword, Token whileKeyword,
                           Token endToken) {
  }

  void beginExport(Token token) {
  }

  void endExport(Token exportKeyword, Token semicolon) {
  }

  void beginExpressionStatement(Token token) {
  }

  void endExpressionStatement(Token token) {
  }

  void beginFactoryMethod(Token token) {
  }

  void endFactoryMethod(Token beginToken, Token endToken) {
  }

  void beginFormalParameter(Token token) {
  }

  void endFormalParameter(Token thisKeyword) {
  }

  void handleNoFormalParameters(Token token) {
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

  void endForIn(Token beginToken, Token inKeyword, Token endToken) {
  }

  void beginFunction(Token token) {
  }

  void endFunction(Token getOrSet, Token endToken) {
  }

  void beginFunctionDeclaration(Token token) {
  }

  void endFunctionDeclaration(Token token) {
  }

  void beginFunctionBody(Token token) {
  }

  void endFunctionBody(int count, Token beginToken, Token endToken) {
  }

  void handleNoFunctionBody(Token token) {
  }

  void skippedFunctionBody(Token token) {
  }

  void beginFunctionName(Token token) {
  }

  void endFunctionName(Token token) {
  }

  void beginFunctionTypeAlias(Token token) {
  }

  void endFunctionTypeAlias(Token typedefKeyword, Token endToken) {
  }

  void beginMixinApplication(Token token) {
  }

  void endMixinApplication() {
  }

  void beginNamedMixinApplication(Token token) {
  }

  void endNamedMixinApplication(Token classKeyword,
                                Token implementsKeyword,
                                Token endToken) {
  }

  void beginHide(Token hideKeyword) {
  }

  void endHide(Token hideKeyword) {
  }

  void beginIdentifierList(Token token) {
  }

  void endIdentifierList(int count) {
  }

  void beginTypeList(Token token) {
  }

  void endTypeList(int count) {
  }

  void beginIfStatement(Token token) {
  }

  void endIfStatement(Token ifToken, Token elseToken) {
  }

  void beginImport(Token importKeyword) {
  }

  void endImport(Token importKeyword, Token DeferredKeyword,
                 Token asKeyword, Token semicolon) {
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

  void handleLabel(Token token) {
  }

  void beginLabeledStatement(Token token, int labelCount) {
  }

  void endLabeledStatement(int labelCount) {
  }

  void beginLibraryName(Token token) {
  }

  void endLibraryName(Token libraryKeyword, Token semicolon) {
  }

  void beginLiteralMapEntry(Token token) {
  }

  void endLiteralMapEntry(Token colon, Token endToken) {
  }

  void beginLiteralString(Token token) {
  }

  void endLiteralString(int interpolationCount) {
  }

  void handleStringJuxtaposition(int literalCount) {
  }

  void beginMember(Token token) {
  }

  void endMethod(Token getOrSet, Token beginToken, Token endToken) {
  }

  void beginMetadataStar(Token token) {
  }

  void endMetadataStar(int count, bool forParameter) {
  }

  void beginMetadata(Token token) {
  }

  void endMetadata(Token beginToken, Token periodBeforeName, Token endToken) {
  }

  void beginOptionalFormalParameters(Token token) {
  }

  void endOptionalFormalParameters(int count,
                                   Token beginToken, Token endToken) {
  }

  void beginPart(Token token) {
  }

  void endPart(Token partKeyword, Token semicolon) {
  }

  void beginPartOf(Token token) {
  }

  void endPartOf(Token partKeyword, Token semicolon) {
  }

  void beginRedirectingFactoryBody(Token token) {
  }

  void endRedirectingFactoryBody(Token beginToken, Token endToken) {
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

  void beginShow(Token showKeyword) {
  }

  void endShow(Token showKeyword) {
  }

  void beginSwitchStatement(Token token) {
  }

  void endSwitchStatement(Token switchKeyword, Token endToken) {
  }

  void beginSwitchBlock(Token token) {
  }

  void endSwitchBlock(int caseCount, Token beginToken, Token endToken) {
  }

  void beginLiteralSymbol(Token token) {
  }

  void endLiteralSymbol(Token hashToken, int identifierCount) {
  }

  void beginThrowExpression(Token token) {
  }

  void endThrowExpression(Token throwToken, Token endToken) {
  }

  void beginRethrowStatement(Token token) {
  }

  void endRethrowStatement(Token throwToken, Token endToken) {
  }

  void endTopLevelDeclaration(Token token) {
  }

  void beginTopLevelMember(Token token) {
  }

  void endTopLevelFields(int count, Token beginToken, Token endToken) {
  }

  void endTopLevelMethod(Token beginToken, Token getOrSet, Token endToken) {
  }

  void beginTryStatement(Token token) {
  }

  void handleCaseMatch(Token caseKeyword, Token colon) {
  }

  void handleCatchBlock(Token onKeyword, Token catchKeyword) {
  }

  void handleFinallyBlock(Token finallyKeyword) {
  }

  void endTryStatement(int catchCount, Token tryKeyword, Token finallyKeyword) {
  }

  void endType(Token beginToken, Token endToken) {
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

  void handleAsOperator(Token operathor, Token endToken) {
    // TODO(ahe): Rename [operathor] to "operator" when VM bug is fixed.
  }

  void handleAssignmentExpression(Token token) {
  }

  void handleBinaryExpression(Token token) {
  }

  void handleConditionalExpression(Token question, Token colon) {
  }

  void handleConstExpression(Token token) {
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

  void handleAssertStatement(Token assertKeyword, Token semicolonToken) {
  }

  /** Called with either the token containing a double literal, or
    * an immediately preceding "unary plus" token.
    */
  void handleLiteralDouble(Token token) {
  }

  /** Called with either the token containing an integer literal,
    * or an immediately preceding "unary plus" token.
    */
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

  void handleModifier(Token token) {
  }

  void handleModifiers(int count) {
  }

  void handleNamedArgument(Token colon) {
  }

  void handleNewExpression(Token token) {
  }

  void handleNoArguments(Token token) {
  }

  void handleNoExpression(Token token) {
  }

  void handleNoType(Token token) {
  }

  void handleNoTypeVariables(Token token) {
  }

  void handleOperator(Token token) {
  }

  void handleOperatorName(Token operatorKeyword, Token token) {
  }

  void handleParenthesizedExpression(BeginGroupToken token) {
  }

  void handleQualified(Token period) {
  }

  void handleStringPart(Token token) {
  }

  void handleSuperExpression(Token token) {
  }

  void handleSwitchCase(int labelCount, int expressionCount,
                        Token defaultKeyword, int statementCount,
                        Token firstToken, Token endToken) {
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
    error("expected '$string', but got '${token.value}'", token);
    return skipToEof(token);
  }

  Token expectedIdentifier(Token token) {
    error("expected identifier, but got '${token.value}'", token);
    return skipToEof(token);
  }

  Token expectedType(Token token) {
    error("expected a type, but got '${token.value}'", token);
    return skipToEof(token);
  }

  Token expectedExpression(Token token) {
    String message;
    if (token.info == BAD_INPUT_INFO) {
      message = token.value;
    } else {
      message = "expected an expression, but got '${token.value}'";
    }
    error(message, token);
    return skipToEof(token);
  }

  Token unexpected(Token token) {
    error("unexpected token '${token.value}'", token);
    return skipToEof(token);
  }

  Token expectedBlockToSkip(Token token) {
    error("expected a block, but got '${token.value}'", token);
    return skipToEof(token);
  }

  Token expectedFunctionBody(Token token) {
    error("expected a function body, but got '${token.value}'", token);
    return skipToEof(token);
  }

  Token expectedClassBody(Token token) {
    error("expected a class body, but got '${token.value}'", token);
    return skipToEof(token);
  }

  Token expectedClassBodyToSkip(Token token) {
    error("expected a class body, but got '${token.value}'", token);
    return skipToEof(token);
  }

  Link<Token> expectedDeclaration(Token token) {
    error("expected a declaration, but got '${token.value}'", token);
    return const Link<Token>();
  }

  Token unmatched(Token token) {
    error("unmatched '${token.value}'", token);
    return skipToEof(token);
  }

  skipToEof(Token token) {
    while (!identical(token.info, EOF_INFO)) {
      token = token.next;
    }
    return token;
  }

  void recoverableError(Token token, String message) {
    error(message, token);
  }

  void error(String message, Token token) {
    throw new ParserError("$message @ ${token.charOffset}");
  }

  void reportError(Spannable spannable,
                   MessageKind messageKind,
                   [Map arguments = const {}]) {
    String message = messageKind.message(arguments, true).toString();
    Token token;
    Node node;
    if (spannable is Token) {
      token = spannable;
    } else if (spannable is Node) {
      token = spannable.getBeginToken();
    } else {
      throw new ParserError(message);
    }
    recoverableError(token, message);
  }
}

class ParserError {
  final String reason;
  ParserError(this.reason);
  toString() => reason;
}

typedef int IdGenerator();

/**
 * A parser event listener designed to work with [PartialParser]. It
 * builds elements representing the top-level declarations found in
 * the parsed compilation unit and records them in
 * [compilationUnitElement].
 */
class ElementListener extends Listener {
  final IdGenerator idGenerator;
  final DiagnosticListener listener;
  final CompilationUnitElement compilationUnitElement;
  final StringValidator stringValidator;
  Link<StringQuoting> interpolationScope;

  Link<Node> nodes = const Link<Node>();

  Link<MetadataAnnotation> metadata = const Link<MetadataAnnotation>();

  ElementListener(DiagnosticListener listener,
                  this.compilationUnitElement,
                  this.idGenerator)
      : this.listener = listener,
        stringValidator = new StringValidator(listener),
        interpolationScope = const Link<StringQuoting>();

  void pushQuoting(StringQuoting quoting) {
    interpolationScope = interpolationScope.prepend(quoting);
  }

  StringQuoting popQuoting() {
    StringQuoting result = interpolationScope.head;
    interpolationScope = interpolationScope.tail;
    return result;
  }

  StringNode popLiteralString() {
    StringNode node = popNode();
    // TODO(lrn): Handle interpolations in script tags.
    if (node.isInterpolation) {
      listener.internalError(node,
          "String interpolation not supported in library tags.");
      return null;
    }
    return node;
  }

  bool allowLibraryTags() {
    // Library tags are only allowed in the library file itself, not
    // in sourced files.
    LibraryElement library = compilationUnitElement.getLibrary();
    return !compilationUnitElement.hasMembers
      && library.entryCompilationUnit == compilationUnitElement;
  }

  void endLibraryName(Token libraryKeyword, Token semicolon) {
    Expression name = popNode();
    addLibraryTag(new LibraryName(libraryKeyword, name,
                                  popMetadata(compilationUnitElement)));
  }

  void endImport(Token importKeyword, Token deferredKeyword, Token asKeyword,
                 Token semicolon) {
    NodeList combinators = popNode();
    bool isDeferred = deferredKeyword != null;
    Identifier prefix;
    if (asKeyword != null) {
      prefix = popNode();
    }
    StringNode uri = popLiteralString();
    addLibraryTag(new Import(importKeyword, uri, prefix, combinators,
                             popMetadata(compilationUnitElement),
                             isDeferred: isDeferred));
  }

  void endExport(Token exportKeyword, Token semicolon) {
    NodeList combinators = popNode();
    StringNode uri = popNode();
    addLibraryTag(new Export(exportKeyword, uri, combinators,
                             popMetadata(compilationUnitElement)));
  }

  void endCombinators(int count) {
    if (0 == count) {
      pushNode(null);
    } else {
      pushNode(makeNodeList(count, null, null, " "));
    }
  }

  void endHide(Token hideKeyword) => pushCombinator(hideKeyword);

  void endShow(Token showKeyword) => pushCombinator(showKeyword);

  void pushCombinator(Token keywordToken) {
    NodeList identifiers = popNode();
    pushNode(new Combinator(identifiers, keywordToken));
  }

  void endIdentifierList(int count) {
    pushNode(makeNodeList(count, null, null, ","));
  }

  void endTypeList(int count) {
    pushNode(makeNodeList(count, null, null, ","));
  }

  void endPart(Token partKeyword, Token semicolon) {
    StringNode uri = popLiteralString();
    addLibraryTag(new Part(partKeyword, uri,
                           popMetadata(compilationUnitElement)));
  }

  void endPartOf(Token partKeyword, Token semicolon) {
    Expression name = popNode();
    addPartOfTag(new PartOf(partKeyword, name,
                            popMetadata(compilationUnitElement)));
  }

  void addPartOfTag(PartOf tag) {
    compilationUnitElement.setPartOf(tag, listener);
  }

  void endMetadata(Token beginToken, Token periodBeforeName, Token endToken) {
    if (periodBeforeName != null) {
      popNode(); // Discard name.
    }
    popNode(); // Discard node (Send or Identifier).
    pushMetadata(new PartialMetadataAnnotation(beginToken, endToken));
  }

  void endTopLevelDeclaration(Token token) {
    if (!metadata.isEmpty) {
      recoverableError(metadata.head.beginToken,
                       'Metadata not supported here.');
      metadata = const Link<MetadataAnnotation>();
    }
  }

  void endClassDeclaration(int interfacesCount, Token beginToken,
                           Token extendsKeyword, Token implementsKeyword,
                           Token endToken) {
    String nativeTagInfo = native.checkForNativeClass(this);
    NodeList interfaces =
        makeNodeList(interfacesCount, implementsKeyword, null, ",");
    Node supertype = popNode();
    NodeList typeParameters = popNode();
    Identifier name = popNode();
    int id = idGenerator();
    ClassElement element = new PartialClassElement(
        name.source, beginToken, endToken, compilationUnitElement, id);
    element.nativeTagInfo = nativeTagInfo;
    pushElement(element);
    rejectBuiltInIdentifier(name);
  }

  void rejectBuiltInIdentifier(Identifier name) {
    if (name.token is KeywordToken) {
      Keyword keyword = (name.token as KeywordToken).keyword;
      if (!keyword.isPseudo) {
        recoverableError(name, "Illegal name '${keyword.syntax}'.");
      }
    }
  }

  void endFunctionTypeAlias(Token typedefKeyword, Token endToken) {
    NodeList typeVariables = popNode(); // TOOD(karlklose): do not throw away.
    Identifier name = popNode();
    TypeAnnotation returnType = popNode();
    pushElement(new PartialTypedefElement(name.source, compilationUnitElement,
                                          typedefKeyword));
    rejectBuiltInIdentifier(name);
  }

  void endNamedMixinApplication(Token classKeyword,
                                Token implementsKeyword,
                                Token endToken) {
    NodeList interfaces = (implementsKeyword != null) ? popNode() : null;
    MixinApplication mixinApplication = popNode();
    Modifiers modifiers = popNode();
    NodeList typeParameters = popNode();
    Identifier name = popNode();
    NamedMixinApplication namedMixinApplication = new NamedMixinApplication(
        name, typeParameters, modifiers, mixinApplication, interfaces,
        classKeyword, endToken);

    int id = idGenerator();
    Element enclosing = compilationUnitElement;
    pushElement(new MixinApplicationElementX(name.source, enclosing, id,
                                             namedMixinApplication,
                                             modifiers));
    rejectBuiltInIdentifier(name);
  }

  void endMixinApplication() {
    NodeList mixins = popNode();
    TypeAnnotation superclass = popNode();
    pushNode(new MixinApplication(superclass, mixins));
  }

  void handleVoidKeyword(Token token) {
    pushNode(new TypeAnnotation(new Identifier(token), null));
  }

  void endTopLevelMethod(Token beginToken, Token getOrSet, Token endToken) {
    Identifier name = popNode();
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    ElementKind kind;
    if (getOrSet == null) {
      kind = ElementKind.FUNCTION;
    } else if (identical(getOrSet.stringValue, 'get')) {
      kind = ElementKind.GETTER;
    } else if (identical(getOrSet.stringValue, 'set')) {
      kind = ElementKind.SETTER;
    }
    pushElement(new PartialFunctionElement(name.source, beginToken, getOrSet,
                                           endToken, kind, modifiers,
                                           compilationUnitElement, false));
  }

  void endTopLevelFields(int count, Token beginToken, Token endToken) {
    void buildFieldElement(Identifier name, VariableList fields) {
      pushElement(
          new FieldElementX(name, compilationUnitElement, fields));
    }
    NodeList variables = makeNodeList(count, null, null, ",");
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    buildFieldElements(modifiers, variables, compilationUnitElement,
                       buildFieldElement,
                       beginToken, endToken);
  }

  void buildFieldElements(Modifiers modifiers,
                          NodeList variables,
                          Element enclosingElement,
                          void buildFieldElement(Identifier name,
                                                 VariableList fields),
                          Token beginToken, Token endToken) {
    VariableList fields =
        new PartialFieldList(beginToken, endToken, modifiers);
    for (Link<Node> variableNodes = variables.nodes;
         !variableNodes.isEmpty;
         variableNodes = variableNodes.tail) {
      Expression initializedIdentifier = variableNodes.head;
      Identifier identifier = initializedIdentifier.asIdentifier();
      if (identifier == null) {
        identifier = initializedIdentifier.asSendSet().selector.asIdentifier();
      }
      buildFieldElement(identifier, fields);
    }
  }

  void handleIdentifier(Token token) {
    pushNode(new Identifier(token));
  }

  void handleQualified(Token period) {
    Identifier last = popNode();
    Expression first = popNode();
    pushNode(new Send(first, last));
  }

  void handleNoType(Token token) {
    pushNode(null);
  }

  void endTypeVariable(Token token) {
    TypeAnnotation bound = popNode();
    Identifier name = popNode();
    pushNode(new TypeVariable(name, bound));
    rejectBuiltInIdentifier(name);
  }

  void endTypeVariables(int count, Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, endToken, ','));
  }

  void handleNoTypeVariables(token) {
    pushNode(null);
  }

  void endTypeArguments(int count, Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, endToken, ','));
  }

  void handleNoTypeArguments(Token token) {
    pushNode(null);
  }

  void endType(Token beginToken, Token endToken) {
    NodeList typeArguments = popNode();
    Expression typeName = popNode();
    pushNode(new TypeAnnotation(typeName, typeArguments));
  }

  void handleParenthesizedExpression(BeginGroupToken token) {
    Expression expression = popNode();
    pushNode(new ParenthesizedExpression(expression, token));
  }

  void handleModifier(Token token) {
    pushNode(new Identifier(token));
  }

  void handleModifiers(int count) {
    if (count == 0) {
      pushNode(Modifiers.EMPTY);
    } else {
      NodeList modifierNodes = makeNodeList(count, null, null, ' ');
      pushNode(new Modifiers(modifierNodes));
    }
  }

  Token expected(String string, Token token) {
    reportFatalError(token,
        "Expected '$string', but got '${token.value}'.");
    return skipToEof(token);
  }

  Token expectedIdentifier(Token token) {
    if (token is KeywordToken) {
      reportError(
          token, MessageKind.EXPECTED_IDENTIFIER_NOT_RESERVED_WORD,
          {'keyword': token.value});
    } else {
      reportFatalError(token,
          "Expected identifier, but got '${token.value}'.");
    }
    return token;
  }

  Token expectedType(Token token) {
    reportFatalError(token,
                     "Expected a type, but got '${token.value}'.");
    pushNode(null);
    return skipToEof(token);
  }

  Token expectedExpression(Token token) {
    if (token.info == BAD_INPUT_INFO) {
      reportError(token, MessageKind.GENERIC, {'text': token.value});
      pushNode(new ErrorExpression(token));
      return token.next;
    } else {
      reportFatalError(token,
                       "Expected an expression, but got '${token.value}'.");
      pushNode(null);
      return skipToEof(token);
    }
  }

  Token unexpected(Token token) {
    String message = "Unexpected token '${token.value}'.";
    if (token.info == BAD_INPUT_INFO) {
      message = token.value;
    }
    reportFatalError(token, message);
    return skipToEof(token);
  }

  Token expectedBlockToSkip(Token token) {
    if (identical(token.stringValue, 'native')) {
      return native.handleNativeBlockToSkip(this, token);
    } else {
      return unexpected(token);
    }
  }

  Token expectedFunctionBody(Token token) {
    String printString = token.value;
    reportFatalError(token,
                     "Expected a function body, but got '$printString'.");
    return skipToEof(token);
  }

  Token expectedClassBody(Token token) {
    reportFatalError(token,
                     "Expected a class body, but got '${token.value}'.");
    return skipToEof(token);
  }

  Token expectedClassBodyToSkip(Token token) {
    if (identical(token.stringValue, 'native')) {
      return native.handleNativeClassBodyToSkip(this, token);
    } else {
      return unexpected(token);
    }
  }

  Link<Token> expectedDeclaration(Token token) {
    reportFatalError(token,
                     "Expected a declaration, but got '${token.value}'.");
    return const Link<Token>();
  }

  Token unmatched(Token token) {
    reportFatalError(token,
                     "Unmatched '${token.value}'.");
    return skipToEof(token);
  }

  void recoverableError(Spannable node, String message) {
    // TODO(johnniwinther): Make recoverable errors non-fatal.
    reportFatalError(node, message);
  }

  void pushElement(Element element) {
    popMetadata(element);
    compilationUnitElement.addMember(element, listener);
  }

  Link<MetadataAnnotation> popMetadata(Element element) {
    var result = const Link<MetadataAnnotation>();
    for (Link link = metadata; !link.isEmpty; link = link.tail) {
      element.addMetadata(link.head);
      // Reverse the list as is implicitly done by addMetadata.
      result = result.prepend(link.head);
    }
    metadata = const Link<MetadataAnnotation>();
    return result;
  }

  void pushMetadata(MetadataAnnotation annotation) {
    metadata = metadata.prepend(annotation);
  }

  void addLibraryTag(LibraryTag tag) {
    if (!allowLibraryTags()) {
      recoverableError(tag, 'Library tags not allowed here.');
    }
    compilationUnitElement.getImplementationLibrary().addTag(tag, listener);
  }

  void pushNode(Node node) {
    nodes = nodes.prepend(node);
    if (VERBOSE) log("push $nodes");
  }

  Node popNode() {
    assert(!nodes.isEmpty);
    Node node = nodes.head;
    nodes = nodes.tail;
    if (VERBOSE) log("pop $nodes");
    return node;
  }

  void log(message) {
    print(message);
  }

  NodeList makeNodeList(int count, Token beginToken, Token endToken,
                        String delimiter) {
    Link<Node> poppedNodes = const Link<Node>();
    for (; count > 0; --count) {
      // This effectively reverses the order of nodes so they end up
      // in correct (source) order.
      poppedNodes = poppedNodes.prepend(popNode());
    }
    return new NodeList(beginToken, poppedNodes, endToken, delimiter);
  }

  void beginLiteralString(Token token) {
    String source = token.value;
    StringQuoting quoting = StringValidator.quotingFromString(source);
    pushQuoting(quoting);
    // Just wrap the token for now. At the end of the interpolation,
    // when we know how many there are, go back and validate the tokens.
    pushNode(new LiteralString(token, null));
  }

  void handleStringPart(Token token) {
    // Just push an unvalidated token now, and replace it when we know the
    // end of the interpolation.
    pushNode(new LiteralString(token, null));
  }

  void endLiteralString(int count) {
    StringQuoting quoting = popQuoting();

    Link<StringInterpolationPart> parts =
        const Link<StringInterpolationPart>();
    // Parts of the string interpolation are popped in reverse order,
    // starting with the last literal string part.
    bool isLast = true;
    for (int i = 0; i < count; i++) {
      LiteralString string = popNode();
      DartString validation =
          stringValidator.validateInterpolationPart(string.token, quoting,
                                                    isFirst: false,
                                                    isLast: isLast);
      // Replace the unvalidated LiteralString with a new LiteralString
      // object that has the validation result included.
      string = new LiteralString(string.token, validation);
      Expression expression = popNode();
      parts = parts.prepend(new StringInterpolationPart(expression, string));
      isLast = false;
    }

    LiteralString string = popNode();
    DartString validation =
        stringValidator.validateInterpolationPart(string.token, quoting,
                                                  isFirst: true,
                                                  isLast: isLast);
    string = new LiteralString(string.token, validation);
    if (isLast) {
      pushNode(string);
    } else {
      NodeList partNodes = new NodeList(null, parts, null, "");
      pushNode(new StringInterpolation(string, partNodes));
    }
  }

  void handleStringJuxtaposition(int stringCount) {
    assert(stringCount != 0);
    Expression accumulator = popNode();
    stringCount--;
    while (stringCount > 0) {
      Expression expression = popNode();
      accumulator = new StringJuxtaposition(expression, accumulator);
      stringCount--;
    }
    pushNode(accumulator);
  }

  void reportFatalError(Spannable spannable,
                        String message) {
    listener.reportFatalError(
        spannable, MessageKind.GENERIC, {'text': message});
  }

  void reportError(Spannable spannable,
                   MessageKind errorCode,
                   [Map arguments = const {}]) {
    listener.reportError(spannable, errorCode, arguments);
  }
}

class NodeListener extends ElementListener {
  NodeListener(DiagnosticListener listener, CompilationUnitElement element)
    : super(listener, element, null);

  void addLibraryTag(LibraryTag tag) {
    pushNode(tag);
  }

  void addPartOfTag(PartOf tag) {
    pushNode(tag);
  }

  void endClassDeclaration(int interfacesCount, Token beginToken,
                           Token extendsKeyword, Token implementsKeyword,
                           Token endToken) {
    NodeList body = popNode();
    NodeList interfaces =
        makeNodeList(interfacesCount, implementsKeyword, null, ",");
    Node supertype = popNode();
    NodeList typeParameters = popNode();
    Identifier name = popNode();
    Modifiers modifiers = popNode();
    pushNode(new ClassNode(modifiers, name, typeParameters, supertype,
                           interfaces, beginToken, extendsKeyword, body,
                           endToken));
  }

  void endCompilationUnit(int count, Token token) {
    pushNode(makeNodeList(count, null, null, '\n'));
  }

  void endFunctionTypeAlias(Token typedefKeyword, Token endToken) {
    NodeList formals = popNode();
    NodeList typeParameters = popNode();
    Identifier name = popNode();
    TypeAnnotation returnType = popNode();
    pushNode(new Typedef(returnType, name, typeParameters, formals,
                         typedefKeyword, endToken));
  }

  void endNamedMixinApplication(Token classKeyword,
                                Token implementsKeyword,
                                Token endToken) {
    NodeList interfaces = (implementsKeyword != null) ? popNode() : null;
    Node mixinApplication = popNode();
    Modifiers modifiers = popNode();
    NodeList typeParameters = popNode();
    Identifier name = popNode();
    pushNode(new NamedMixinApplication(name, typeParameters,
                                       modifiers, mixinApplication,
                                       interfaces,
                                       classKeyword, endToken));
  }

  void endClassBody(int memberCount, Token beginToken, Token endToken) {
    pushNode(makeNodeList(memberCount, beginToken, endToken, null));
  }

  void endTopLevelFields(int count, Token beginToken, Token endToken) {
    NodeList variables = makeNodeList(count, null, endToken, ",");
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    pushNode(new VariableDefinitions(type, modifiers, variables));
  }

  void endTopLevelMethod(Token beginToken, Token getOrSet, Token endToken) {
    Statement body = popNode();
    NodeList formalParameters = popNode();
    Identifier name = popNode();
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    ElementKind kind;
    if (getOrSet == null) {
      kind = ElementKind.FUNCTION;
    } else if (identical(getOrSet.stringValue, 'get')) {
      kind = ElementKind.GETTER;
    } else if (identical(getOrSet.stringValue, 'set')) {
      kind = ElementKind.SETTER;
    }
    pushElement(new PartialFunctionElement(name.source, beginToken, getOrSet,
                                           endToken, kind, modifiers,
                                           compilationUnitElement, false));
  }

  void endFormalParameter(Token thisKeyword) {
    Expression name = popNode();
    if (thisKeyword != null) {
      Identifier thisIdentifier = new Identifier(thisKeyword);
      if (name.asSend() == null) {
        name = new Send(thisIdentifier, name);
      } else {
        name = name.asSend().copyWithReceiver(thisIdentifier);
      }
    }
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    NodeList metadata = popNode();
    pushNode(new VariableDefinitions.forParameter(
        metadata, type, modifiers, new NodeList.singleton(name)));
  }

  void endFormalParameters(int count, Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, endToken, ","));
  }

  void handleNoFormalParameters(Token token) {
    pushNode(null);
  }

  void endArguments(int count, Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, endToken, ","));
  }

  void handleNoArguments(Token token) {
    pushNode(null);
  }

  void endConstructorReference(Token start, Token periodBeforeName,
                               Token endToken) {
    Identifier name = null;
    if (periodBeforeName != null) {
      name = popNode();
    }
    NodeList typeArguments = popNode();
    Node classReference = popNode();
    if (typeArguments != null) {
      classReference = new TypeAnnotation(classReference, typeArguments);
    } else {
      Identifier identifier = classReference.asIdentifier();
      Send send = classReference.asSend();
      if (identifier != null) {
        // TODO(ahe): Should be:
        // classReference = new Send(null, identifier);
        classReference = identifier;
      } else if (send != null) {
        classReference = send;
      } else {
        internalError(node: classReference);
      }
    }
    Node constructor = classReference;
    if (name != null) {
      // Either typeName<args>.name or x.y.name.
      constructor = new Send(classReference, name);
    }
    pushNode(constructor);
  }

  void endRedirectingFactoryBody(Token beginToken,
                                 Token endToken) {
    pushNode(new Return(beginToken, endToken, popNode()));
  }

  void endReturnStatement(bool hasExpression,
                          Token beginToken, Token endToken) {
    Expression expression = hasExpression ? popNode() : null;
    pushNode(new Return(beginToken, endToken, expression));
  }

  void endExpressionStatement(Token token) {
    pushNode(new ExpressionStatement(popNode(), token));
  }

  void handleOnError(Token token, var errorInformation) {
    listener.internalError(token, "'${token.value}': ${errorInformation}");
  }

  Token expectedFunctionBody(Token token) {
    if (identical(token.stringValue, 'native')) {
      return native.handleNativeFunctionBody(this, token);
    } else {
      reportFatalError(token,
                       "Expected a function body, but got '${token.value}'.");
      return skipToEof(token);
    }
  }

  Token expectedClassBody(Token token) {
    if (identical(token.stringValue, 'native')) {
      return native.handleNativeClassBody(this, token);
    } else {
      reportFatalError(token,
                       "Expected a class body, but got '${token.value}'.");
      return skipToEof(token);
    }
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

  void handleLiteralNull(Token token) {
    pushNode(new LiteralNull(token));
  }

  void endLiteralSymbol(Token hashToken, int identifierCount) {
    NodeList identifiers = makeNodeList(identifierCount, null, null, '.');
    pushNode(new LiteralSymbol(hashToken, identifiers));
  }

  void handleBinaryExpression(Token token) {
    Node argument = popNode();
    Node receiver = popNode();
    String tokenString = token.stringValue;
    if (identical(tokenString, '.') || identical(tokenString, '..')) {
      Send argumentSend = argument.asSend();
      if (argumentSend == null) {
        // TODO(ahe): The parser should diagnose this problem, not
        // this listener.
        reportFatalError(argument,
                         'Expected an identifier.');
      }
      if (argumentSend.receiver != null) internalError(node: argument);
      if (argument is SendSet) internalError(node: argument);
      pushNode(argument.asSend().copyWithReceiver(receiver));
    } else {
      NodeList arguments = new NodeList.singleton(argument);
      pushNode(new Send(receiver, new Operator(token), arguments));
    }
    if (identical(tokenString, '===')) {
      listener.reportError(token, MessageKind.UNSUPPORTED_EQ_EQ_EQ,
                           {'lhs': receiver, 'rhs': argument});
    }
    if (identical(tokenString, '!==')) {
      listener.reportError(token, MessageKind.UNSUPPORTED_BANG_EQ_EQ,
                           {'lhs': receiver, 'rhs': argument});
    }
  }

  void beginCascade(Token token) {
    pushNode(new CascadeReceiver(popNode(), token));
  }

  void endCascade() {
    pushNode(new Cascade(popNode()));
  }

  void handleAsOperator(Token operathor, Token endToken) {
    TypeAnnotation type = popNode();
    Expression expression = popNode();
    NodeList arguments = new NodeList.singleton(type);
    pushNode(new Send(expression, new Operator(operathor), arguments));
  }

  void handleAssignmentExpression(Token token) {
    Node arg = popNode();
    Node node = popNode();
    Send send = node.asSend();
    if (send == null || !(send.isPropertyAccess || send.isIndex)) {
      reportNotAssignable(node);
    }
    if (send.asSendSet() != null) internalError(node: send);
    NodeList arguments;
    if (send.isIndex) {
      Link<Node> link = const Link<Node>().prepend(arg);
      link = link.prepend(send.arguments.head);
      arguments = new NodeList(null, link);
    } else {
      arguments = new NodeList.singleton(arg);
    }
    Operator op = new Operator(token);
    pushNode(new SendSet(send.receiver, send.selector, op, arguments));
  }

  void reportNotAssignable(Node node) {
    // TODO(ahe): The parser should diagnose this problem, not this
    // listener.
    reportFatalError(node,
                     'Not assignable.');
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

  void endFunctionBody(int count, Token beginToken, Token endToken) {
    if (count == 0 && beginToken == null) {
      pushNode(new EmptyStatement(endToken));
    } else {
      pushNode(new Block(makeNodeList(count, beginToken, endToken, null)));
    }
  }

  void skippedFunctionBody(Token token) {
    pushNode(new Block(new NodeList.empty()));
  }

  void handleNoFunctionBody(Token token) {
    pushNode(new EmptyStatement(token));
  }

  void endFunction(Token getOrSet, Token endToken) {
    Statement body = popNode();
    NodeList initializers = popNode();
    NodeList formals = popNode();
    // The name can be an identifier or a send in case of named constructors.
    Expression name = popNode();
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    pushNode(new FunctionExpression(name, formals, body, type,
                                    modifiers, initializers, getOrSet));
  }

  void endFunctionDeclaration(Token endToken) {
    pushNode(new FunctionDeclaration(popNode()));
  }

  void endVariablesDeclaration(int count, Token endToken) {
    // TODO(ahe): Pick one name for this concept, either
    // VariablesDeclaration or VariableDefinitions.
    NodeList variables = makeNodeList(count, null, endToken, ",");
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    pushNode(new VariableDefinitions(type, modifiers, variables));
  }

  void endInitializer(Token assignmentOperator) {
    Expression initializer = popNode();
    NodeList arguments = new NodeList.singleton(initializer);
    Expression name = popNode();
    Operator op = new Operator(assignmentOperator);
    pushNode(new SendSet(null, name, op, arguments));
  }

  void endIfStatement(Token ifToken, Token elseToken) {
    Statement elsePart = (elseToken == null) ? null : popNode();
    Statement thenPart = popNode();
    ParenthesizedExpression condition = popNode();
    pushNode(new If(condition, thenPart, elsePart, ifToken, elseToken));
  }

  void endForStatement(int updateExpressionCount,
                       Token beginToken, Token endToken) {
    Statement body = popNode();
    NodeList updates = makeNodeList(updateExpressionCount, null, null, ',');
    Statement condition = popNode();
    Node initializer = popNode();
    pushNode(new For(initializer, condition, updates, body, beginToken));
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

  void endThrowExpression(Token throwToken, Token endToken) {
    Expression expression = popNode();
    pushNode(new Throw(expression, throwToken, endToken));
  }

  void endRethrowStatement(Token throwToken, Token endToken) {
    pushNode(new Rethrow(throwToken, endToken));
    if (identical(throwToken.stringValue, 'throw')) {
      listener.reportError(throwToken,
                           MessageKind.UNSUPPORTED_THROW_WITHOUT_EXP);
    }
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
    if (send == null) {
      reportNotAssignable(node);
    }
    if (!(send.isPropertyAccess || send.isIndex)) {
      reportNotAssignable(node);
    }
    if (send.asSendSet() != null) internalError(node: send);
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
    NodeList variables = makeNodeList(count, null, endToken, ",");
    TypeAnnotation type = popNode();
    Modifiers modifiers = popNode();
    pushNode(new VariableDefinitions(type, modifiers, variables));
  }

  void endMethod(Token getOrSet, Token beginToken, Token endToken) {
    Statement body = popNode();
    NodeList initializers = popNode();
    NodeList formalParameters = popNode();
    Expression name = popNode();
    TypeAnnotation returnType = popNode();
    Modifiers modifiers = popNode();
    pushNode(new FunctionExpression(name, formalParameters, body, returnType,
                                    modifiers, initializers, getOrSet));
  }

  void handleLiteralMap(int count, Token beginToken, Token constKeyword,
                        Token endToken) {
    NodeList entries = makeNodeList(count, beginToken, endToken, ',');
    NodeList typeArguments = popNode();
    pushNode(new LiteralMap(typeArguments, entries, constKeyword));
  }

  void endLiteralMapEntry(Token colon, Token endToken) {
    Expression value = popNode();
    Expression key = popNode();
    pushNode(new LiteralMapEntry(key, colon, value));
  }

  void handleLiteralList(int count, Token beginToken, Token constKeyword,
                         Token endToken) {
    NodeList elements = makeNodeList(count, beginToken, endToken, ',');
    pushNode(new LiteralList(popNode(), elements, constKeyword));
  }

  void handleIndexedExpression(Token openSquareBracket,
                               Token closeSquareBracket) {
    NodeList arguments =
        makeNodeList(1, openSquareBracket, closeSquareBracket, null);
    Node receiver = popNode();
    Token token = new StringToken.fromString(INDEX_INFO, '[]',
                                  openSquareBracket.charOffset);
    Node selector = new Operator(token);
    pushNode(new Send(receiver, selector, arguments));
  }

  void handleNewExpression(Token token) {
    NodeList arguments = popNode();
    Node name = popNode();
    pushNode(new NewExpression(token, new Send(null, name, arguments)));
  }

  void handleConstExpression(Token token) {
    // [token] carries the 'const' information.
    handleNewExpression(token);
  }

  void handleOperator(Token token) {
    pushNode(new Operator(token));
  }

  void handleOperatorName(Token operatorKeyword, Token token) {
    Operator op = new Operator(token);
    pushNode(new Send(new Identifier(operatorKeyword), op, null));
  }

  void handleNamedArgument(Token colon) {
    Expression expression = popNode();
    Identifier name = popNode();
    pushNode(new NamedArgument(name, colon, expression));
  }

  void endOptionalFormalParameters(int count,
                                   Token beginToken, Token endToken) {
    pushNode(makeNodeList(count, beginToken, endToken, ','));
  }

  void handleFunctionTypedFormalParameter(Token endToken) {
    NodeList formals = popNode();
    Identifier name = popNode();
    TypeAnnotation returnType = popNode();
    pushNode(null); // Signal "no type" to endFormalParameter.
    pushNode(new FunctionExpression(name, formals, null, returnType,
                                    Modifiers.EMPTY, null, null));
  }

  void handleValuedFormalParameter(Token equals, Token token) {
    Expression defaultValue = popNode();
    Expression parameterName = popNode();
    pushNode(new SendSet(null, parameterName, new Operator(equals),
                         new NodeList.singleton(defaultValue)));
  }

  void endTryStatement(int catchCount, Token tryKeyword, Token finallyKeyword) {
    Block finallyBlock = null;
    if (finallyKeyword != null) {
      finallyBlock = popNode();
    }
    NodeList catchBlocks = makeNodeList(catchCount, null, null, null);
    Block tryBlock = popNode();
    pushNode(new TryStatement(tryBlock, catchBlocks, finallyBlock,
                              tryKeyword, finallyKeyword));
  }

  void handleCaseMatch(Token caseKeyword, Token colon) {
    pushNode(new CaseMatch(caseKeyword, popNode(), colon));
  }

  void handleCatchBlock(Token onKeyword, Token catchKeyword) {
    Block block = popNode();
    NodeList formals = catchKeyword != null? popNode(): null;
    TypeAnnotation type = onKeyword != null ? popNode() : null;
    pushNode(new CatchBlock(type, formals, block, onKeyword, catchKeyword));
  }

  void endSwitchStatement(Token switchKeyword, Token endToken) {
    NodeList cases = popNode();
    ParenthesizedExpression expression = popNode();
    pushNode(new SwitchStatement(expression, cases, switchKeyword));
  }

  void endSwitchBlock(int caseCount, Token beginToken, Token endToken) {
    Link<Node> caseNodes = const Link<Node>();
    while (caseCount > 0) {
      SwitchCase switchCase = popNode();
      caseNodes = caseNodes.prepend(switchCase);
      caseCount--;
    }
    pushNode(new NodeList(beginToken, caseNodes, endToken, null));
  }

  void handleSwitchCase(int labelCount, int caseCount,
                        Token defaultKeyword, int statementCount,
                        Token firstToken, Token endToken) {
    NodeList statements = makeNodeList(statementCount, null, null, null);
    NodeList labelsAndCases =
        makeNodeList(labelCount + caseCount, null, null, null);
    pushNode(new SwitchCase(labelsAndCases, defaultKeyword, statements,
                            firstToken));
  }

  void handleBreakStatement(bool hasTarget,
                            Token breakKeyword, Token endToken) {
    Identifier target = null;
    if (hasTarget) {
      target = popNode();
    }
    pushNode(new BreakStatement(target, breakKeyword, endToken));
  }

  void handleContinueStatement(bool hasTarget,
                               Token continueKeyword, Token endToken) {
    Identifier target = null;
    if (hasTarget) {
      target = popNode();
    }
    pushNode(new ContinueStatement(target, continueKeyword, endToken));
  }

  void handleEmptyStatement(Token token) {
    pushNode(new EmptyStatement(token));
  }

  void endFactoryMethod(Token beginToken, Token endToken) {
    Statement body = popNode();
    NodeList formals = popNode();
    Node name = popNode();

    // TODO(ahe): Move this parsing to the parser.
    int modifierCount = 0;
    Token modifier = beginToken;
    if (modifier.stringValue == "external") {
      handleModifier(modifier);
      modifierCount++;
      modifier = modifier.next;
    }
    if (modifier.stringValue == "const") {
      handleModifier(modifier);
      modifierCount++;
      modifier = modifier.next;
    }
    assert(modifier.stringValue == "factory");
    handleModifier(modifier);
    modifierCount++;
    handleModifiers(modifierCount);
    Modifiers modifiers = popNode();

    pushNode(new FunctionExpression(name, formals, body, null,
                                    modifiers, null, null));
  }

  void endForIn(Token beginToken, Token inKeyword, Token endToken) {
    Statement body = popNode();
    Expression expression = popNode();
    Node declaredIdentifier = popNode();
    pushNode(new ForIn(declaredIdentifier, expression, body,
                                beginToken, inKeyword));
  }

  void endMetadataStar(int count, bool forParameter) {
    // TODO(johnniwinther): Handle metadata for all node kinds.
    if (forParameter) {
      if (0 == count) {
        pushNode(null);
      } else {
        pushNode(makeNodeList(count, null, null, ' '));
      }
    }
  }

  void endMetadata(Token beginToken, Token periodBeforeName, Token endToken) {
    NodeList arguments = popNode();
    if (arguments == null) {
      // This is a constant expression.
      Identifier name;
      if (periodBeforeName != null) {
        name = popNode();
      }
      NodeList typeArguments = popNode();
      Node receiver = popNode();
      if (typeArguments != null) {
        receiver = new TypeAnnotation(receiver, typeArguments);
        recoverableError(typeArguments,
                         'Type arguments are not allowed here.');
      } else {
        Identifier identifier = receiver.asIdentifier();
        Send send = receiver.asSend();
        if (identifier != null) {
          receiver = new Send(null, identifier);
        } else if (send == null) {
          internalError(node: receiver);
        }
      }
      Send send = receiver;
      if (name != null) {
        send = new Send(receiver, name);
      }
      pushNode(new Metadata(beginToken, send));
    } else {
      // This is a const constructor call.
      endConstructorReference(beginToken, periodBeforeName, endToken);
      Node constructor = popNode();
      pushNode(new Metadata(beginToken,
          new NewExpression(null,
              new Send(null, constructor, arguments))));
    }
  }

  void handleAssertStatement(Token assertKeyword, Token semicolonToken) {
    NodeList arguments = popNode();
    Node selector = new Identifier(assertKeyword);
    Node send = new Send(null, selector, arguments);
    pushNode(new ExpressionStatement(send, semicolonToken));
  }

  void endUnamedFunction(Token token) {
    Statement body = popNode();
    NodeList formals = popNode();
    pushNode(new FunctionExpression(null, formals, body, null,
                                    Modifiers.EMPTY, null, null));
  }

  void handleIsOperator(Token operathor, Token not, Token endToken) {
    TypeAnnotation type = popNode();
    Expression expression = popNode();
    Node argument;
    if (not != null) {
      argument = new Send.prefix(type, new Operator(not));
    } else {
      argument = type;
    }

    NodeList arguments = new NodeList.singleton(argument);
    pushNode(new Send(expression, new Operator(operathor), arguments));
  }

  void handleLabel(Token colon) {
    Identifier name = popNode();
    pushNode(new Label(name, colon));
  }

  void endLabeledStatement(int labelCount) {
    Statement statement = popNode();
    NodeList labels = makeNodeList(labelCount, null, null, null);
    pushNode(new LabeledStatement(labels, statement));
  }

  void log(message) {
    listener.log(message);
  }

  void internalError({Token token, Node node}) {
    // TODO(ahe): This should call listener.internalError.
    Spannable spannable = (token == null) ? node : token;
    throw new SpannableAssertionFailure(spannable, 'Internal error in parser.');
  }
}

class PartialFunctionElement extends FunctionElementX {
  final Token beginToken;
  final Token getOrSet;
  final Token endToken;

  /**
   * The position is computed in the constructor using [findMyName]. Computing
   * it on demand fails in case tokens are GC'd.
   */
  final Token _position;

  PartialFunctionElement(String name,
                         Token beginToken,
                         this.getOrSet,
                         this.endToken,
                         ElementKind kind,
                         Modifiers modifiers,
                         Element enclosing,
                         bool hasNoBody)
    : super(name, kind, modifiers, enclosing, hasNoBody),
      beginToken = beginToken,
      _position = ElementX.findNameToken(
          beginToken,
          modifiers.isFactory() ||
            identical(kind, ElementKind.GENERATIVE_CONSTRUCTOR),
          name, enclosing.name);

  FunctionExpression parseNode(DiagnosticListener listener) {
    if (cachedNode != null) return cachedNode;
    parseFunction(Parser p) {
      if (isMember() && modifiers.isFactory()) {
        p.parseFactoryMethod(beginToken);
      } else {
        p.parseFunction(beginToken, getOrSet);
      }
    }
    cachedNode = parse(listener, getCompilationUnit(), parseFunction);
    return cachedNode;
  }

  Token position() => _position;
}

class PartialFieldList extends VariableList {
  final Token beginToken;
  final Token endToken;

  PartialFieldList(Token this.beginToken,
                   Token this.endToken,
                   Modifiers modifiers)
    : super(modifiers);

  VariableDefinitions parseNode(Element element, DiagnosticListener listener) {
    if (definitions != null) return definitions;
    definitions = parse(listener,
                       element.getCompilationUnit(),
                       (p) => p.parseVariablesDeclaration(beginToken));
    if (!definitions.modifiers.isVar() &&
        !definitions.modifiers.isFinal() &&
        !definitions.modifiers.isConst() &&
        definitions.type == null) {
      listener.reportError(
          definitions,
          MessageKind.GENERIC,
          { 'text': 'A field declaration must start with var, final, '
                    'const, or a type annotation.' });
    }
    return definitions;
  }

  computeType(Element element, Compiler compiler) {
    if (type != null) return type;
    // TODO(johnniwinther): Compute this in the resolver.
    compiler.withCurrentElement(element, () {
      VariableDefinitions node = parseNode(element, compiler);
      if (node.type != null) {
        type = compiler.resolver.resolveTypeAnnotation(element, node.type);
      } else {
        type = compiler.types.dynamicType;
      }
    });
    assert(type != null);
    return type;
  }
}

class PartialTypedefElement extends TypedefElementX {
  final Token token;

  PartialTypedefElement(String name, Element enclosing, this.token)
      : super(name, enclosing);

  Node parseNode(DiagnosticListener listener) {
    if (cachedNode != null) return cachedNode;
    cachedNode = parse(listener,
                       getCompilationUnit(),
                       (p) => p.parseTopLevelDeclaration(token));
    return cachedNode;
  }

  Token position() => findMyName(token);
}

/// A [MetadataAnnotation] which is constructed on demand.
class PartialMetadataAnnotation extends MetadataAnnotationX {
  final Token beginToken;
  final Token tokenAfterEndToken;
  Expression cachedNode;

  PartialMetadataAnnotation(this.beginToken, this.tokenAfterEndToken);

  Token get endToken {
    Token token = beginToken;
    while (token.kind != EOF_TOKEN) {
      if (identical(token.next, tokenAfterEndToken)) break;
      token = token.next;
    }
    assert(token != null);
    return token;
  }

  Node parseNode(DiagnosticListener listener) {
    if (cachedNode != null) return cachedNode;
    Metadata metadata = parse(listener,
                              annotatedElement.getCompilationUnit(),
                              (p) => p.parseMetadata(beginToken));
    cachedNode = metadata.expression;
    return cachedNode;
  }
}

Node parse(DiagnosticListener diagnosticListener,
           CompilationUnitElement element,
           doParse(Parser parser)) {
  NodeListener listener = new NodeListener(diagnosticListener, element);
  doParse(new Parser(listener));
  Node node = listener.popNode();
  assert(listener.nodes.isEmpty);
  return node;
}
