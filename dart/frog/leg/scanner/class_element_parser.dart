// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ClassElementParser extends PartialParser {
  ClassElementParser(Listener listener) : super(listener);

  Token parseClassBody(Token token) => fullParseClassBody(token);
}

class PartialClassElement extends ClassElement {
  final Token beginToken;
  final Token endToken;
  Node cachedNode;

  PartialClassElement(SourceString name,
                      Token this.beginToken,
                      Token this.endToken,
                      CompilationUnitElement enclosing)
    : super(name, enclosing);

  ClassNode parseNode(Canceler canceler, Logger logger) {
    if (cachedNode != null) return cachedNode;
    MemberListener listener = new MemberListener(canceler, logger, this);
    Parser parser = new ClassElementParser(listener);
    Token token = parser.parseClass(beginToken);
    assert(token === endToken.next);
    cachedNode = listener.popNode();
    assert(listener.nodes.isEmpty());
    assert(listener.topLevelElements.isEmpty());
    return cachedNode;
  }
}

class MemberListener extends NodeListener {
  final ClassElement enclosingElement;

  MemberListener(Canceler canceler, Logger logger,
                 [Element this.enclosingElement = null])
    : super(canceler, logger);

  bool isConstructor(Identifier name) {
    return enclosingElement !== null &&
           enclosingElement.kind == ElementKind.CLASS &&
           enclosingElement.name == name.source;
  }

  void endMethod(Token beginToken, Token endToken) {
    super.endMethod(beginToken, endToken);
    FunctionExpression method = popNode();
    pushNode(null);
    Expression qualified = method.name;
    Identifier name = qualified.asIdentifier();
    if (name === null) {
      canceler.cancel('qualified names are not implemented', node: qualified);
    }
    ElementKind kind = isConstructor(name) ?
                       ElementKind.GENERATIVE_CONSTRUCTOR :
                       ElementKind.FUNCTION;
    Element memberElement =
        new PartialFunctionElement(name.source, beginToken, endToken,
                                   kind, method.modifiers, enclosingElement);
    enclosingElement.addMember(memberElement);
  }

  void endFactoryMethod(Token factoryKeyword, Token periodBeforeName,
                        Token endToken) {
    super.endFactoryMethod(factoryKeyword, periodBeforeName, endToken);
    FunctionExpression method = popNode();
    pushNode(null);
    // TODO(ahe): Named constructors.
    if (method.name.asIdentifier() == null) {
      canceler.cancel('Qualified factory names not implemented', node: method);
    }
    Identifier name = method.name;
    ElementKind kind = ElementKind.FUNCTION;
    Element memberElement =
        new PartialFunctionElement(name.source, factoryKeyword, endToken, kind,
                                   method.modifiers, enclosingElement);
    enclosingElement.addMember(memberElement);
  }

  void endFields(int count, Token beginToken, Token endToken) {
    super.endFields(count, beginToken, endToken);
    VariableDefinitions variableDefinitions = popNode();
    Modifiers modifiers = variableDefinitions.modifiers;
    pushNode(null);
    void buildFieldElement(SourceString name, Element fields) {
      Element element = new VariableElement(
          name, fields, ElementKind.FIELD, enclosingElement);
      enclosingElement.addMember(element);
    }
    buildFieldElements(modifiers, variableDefinitions.definitions,
                       buildFieldElement, beginToken, endToken);
  }

  void endInitializer(Token assignmentOperator) {
    pushNode(null); // Super expects an expression, but
                    // ClassElementParser just skips expressions.
    super.endInitializer(assignmentOperator);
  }

  void endInitializers(int count, Token beginToken, Token endToken) {
    pushNode(null);
  }
}
