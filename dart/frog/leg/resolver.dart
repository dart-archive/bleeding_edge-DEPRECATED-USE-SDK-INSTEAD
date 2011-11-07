// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ResolverTask extends CompilerTask {
  ResolverTask(Compiler compiler) : super(compiler);
  String get name() => 'Resolver';

  Map<Node, Element> resolve(Node tree) {
    return measure(() {
      ResolverVisitor visitor = new ResolverVisitor(compiler);
      visitor.visit(tree);
      return visitor.mapping;
    });
  }
}

class ResolverVisitor implements Visitor<Element> {
  final Compiler compiler;
  final Map<Node, Element> mapping;
  Scope context;

  ResolverVisitor(Compiler compiler)
    : this.compiler = compiler,
      mapping = new Map<Node, Element>(),
      context = new Scope(new TopScope(compiler.universe));

  fail(Node node) {
    compiler.cancel('cannot resolve ${node}');
  }

  visit(Node node) {
    if (node == null) return null;
    Element element = node.accept(this);
    if (element !== null) {
      mapping[node] = element;
    }
    return element;
  }

  visitIn(Node node, Scope scope) {
    context = scope;
    Element element = visit(node);
    context = context.parent;
    return element;
  }

  visitBlock(Block node) {
    visitIn(node.statements, new Scope(context));
  }

  visitExpressionStatement(ExpressionStatement node) {
    visit(node.expression);
  }

  visitFunctionExpression(FunctionExpression node) {
    // TODO(ngeoffray): FunctionExpression is currently a top-level
    // method definition.
    // TODO(ngeoffray): Handle parameters.
    if (!node.parameters.nodes.isEmpty()) fail(node);
    Element enclosingElement = visit(node.name);
    visitIn(node.body, new Scope.enclosing(context, enclosingElement));
    return enclosingElement;
  }

  visitIdentifier(Identifier node) {
    Element element = context.lookup(node.source);
    if (element == null) fail(node);
    return element;
  }

  visitIf(If node) {
    visit(node.condition);
    visit(node.thenPart);
    visit(node.elsePart);
  }

  visitSend(Send node) {
    Element target = null;
    visit(node.receiver);
    SourceString name = node.selector.source;
    if (name == const SourceString('print') ||
        name == const SourceString('+') ||
        name == const SourceString('-') ||
        name == const SourceString('*') ||
        name == const SourceString('/') ||
        name == const SourceString('~/')) {
      // Do nothing.
    } else {
      target = visit(node.selector);
      if (target == null) {
        // Complain: we could not resolve the method.
        fail(node);
      }
    }
    visit(node.argumentsNode);
    return target;
  }

  visitSetterSend(SetterSend node) {
    compiler.unimplemented('ResolverVisitor::visitSetterSend');
  }

  visitLiteralInt(LiteralInt node) {
  }

  visitLiteralDouble(LiteralDouble node) {
  }

  visitLiteralBool(LiteralBool node) {
  }

  visitLiteralString(LiteralString node) {
  }

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      visit(link.head);
    }
  }

  visitOperator(Operator node) {
    fail(node);
  }

  visitReturn(Return node) {
    visit(node.expression);
    return null;
  }

  visitTypeAnnotation(TypeAnnotation node) {
  }

  visitVariableDefinitions(VariableDefinitions node) {
    Visitor visitor = new VariableDefinitionsVisitor(node, this);
    visitor.visit(node.definitions);
  }

  Element setElement(Node node, Element element) {
    mapping[node] = element;
    context.add(element);
  }
}

class VariableDefinitionsVisitor implements Visitor<Element> {
  VariableDefinitions definitions;
  ResolverVisitor resolver;

  VariableDefinitionsVisitor(this.definitions, this.resolver);

  visitSend(Send node) {
    assert(node.arguments.tail.isEmpty()); // Sanity check
    Identifier selector = node.selector;
    SourceString name = selector.source;
    assert(name == const SourceString('='));
    resolver.visit(node.arguments.head);

    // Visit the receiver after visiting the initializer, to not put
    // the receiver in the scope.
    visit(node.receiver);
  }

  visitIdentifier(Identifier node) {
    Element variableElement =
        new Element(node.source, resolver.context.enclosingElement);
    resolver.setElement(node, variableElement);
  }

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      visit(link.head);
    }
  }

  visit(Node node) => node.accept(this);
}

class Scope {
  final Scope parent;
  final Map<SourceString, Element> elements;
  final Element enclosingElement;

  Scope(Scope parent)
    : this.enclosing(parent, parent.enclosingElement);

  Scope.enclosing(Scope this.parent, this.enclosingElement)
    : this.elements = {};

  Scope.top() : parent = null, elements = const {}, enclosingElement = null;

  Element lookup(SourceString name) {
    Element element = elements[name];
    if (element !== null) return element;
    return parent.lookup(name);
  }

  void add(Element element) {
    elements[element.name] = element;
  }
}

// TODO(ngeoffray): this top scope should have libraryElement as
// enclosingElement.
class TopScope extends Scope {
  Universe universe;

  TopScope(Universe this.universe) : super.top();
  Element lookup(SourceString name) => universe.find(name);

  void add(Element element) {
    throw "Cannot add an element in the top scope";
  }
}
