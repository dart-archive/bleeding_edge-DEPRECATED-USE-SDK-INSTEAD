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

class ErrorMessages {
  static String cannotResolve(id)
      => "cannot resolve $id";

  static String duplicateDefinition(id)
      => "duplicate definition of $id";
}

class ResolverVisitor implements Visitor<Element> {
  final Compiler compiler;
  final Map<Node, Element> mapping;
  Scope context;

  ResolverVisitor(Compiler compiler)
    : this.compiler = compiler,
      mapping = new LinkedHashMap<Node, Element>(),
      context = new Scope(new TopScope(compiler.universe));

  fail(Node node, [String message = "Unimplemented in the resolver"]) {
    compiler.cancel(message);
  }

  visit(Node node) {
    if (node == null) return null;
    return node.accept(this);
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

  visitFor(For node) {
    Scope scope = new Scope(context);
    visitIn(node.initializer, scope);
    visitIn(node.condition, scope);
    visitIn(node.update, scope);
    visitIn(node.body, scope);
  }

  visitFunctionExpression(FunctionExpression node) {
    // TODO(ngeoffray): FunctionExpression is currently a top-level
    // method definition.
    Element enclosingElement = visit(node.name);
    Scope newScope = new Scope.enclosing(context, enclosingElement);
    visitIn(node.parameters, newScope);
    visitIn(node.body, newScope);
    return enclosingElement;
  }

  visitIdentifier(Identifier node) {
    Element element = context.lookup(node.source);
    if (element == null) fail(node, ErrorMessages.cannotResolve(node));
    useElement(node, element);
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
    final Identifier selector = node.selector;
    final SourceString name = selector.source;
    if (name == const SourceString('+') ||
        name == const SourceString('-') ||
        name == const SourceString('*') ||
        name == const SourceString('/') ||
        name == const SourceString('<') ||
        name == const SourceString('~/')) {
      // Do nothing.
    } else {
      // TODO(ngeoffray): Use the receiver to do the lookup.
      target = context.lookup(name);
      if (target == null) fail(node, ErrorMessages.cannotResolve(name));
    }
    visit(node.argumentsNode);
    return useElement(node, target);
  }

  visitSendSet(SendSet node) {
    Element receiver = visit(node.receiver);
    final Identifier selector = node.selector;
    if (receiver != null) {
      compiler.unimplemented('Resolver: property access');
    }
    // TODO(ngeoffray): Use the receiver to do the lookup.
    Element target = context.lookup(selector.source);
    if (target == null) fail(node, ErrorMessages.cannotResolve(node));
    visit(node.argumentsNode);
    return useElement(node, target);
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

  visitThrow(Throw node) {
    visit(node.expression);
  }

  visitTypeAnnotation(TypeAnnotation node) {
  }

  visitVariableDefinitions(VariableDefinitions node) {
    VariableDefinitionsVisitor visitor =
        new VariableDefinitionsVisitor(node, this);
    visitor.visit(node.definitions);
  }

  Element defineElement(Node node, Element element) {
    compiler.ensure(element !== null);
    mapping[node] = element;
    return context.add(element);
  }

  Element useElement(Node node, Element element) {
    if (element === null) return null;
    return mapping[node] = element;
  }
}

class VariableDefinitionsVisitor implements Visitor<SourceString> {
  VariableDefinitions definitions;
  ResolverVisitor resolver;

  VariableDefinitionsVisitor(this.definitions, this.resolver);

  SourceString visitSendSet(SendSet node) {
    assert(node.arguments.tail.isEmpty()); // Sanity check
    if (node.receiver !== null) {
      resolver.compiler.unimplemented("receiver on a variable definition");
    }
    Identifier selector = node.selector;
    resolver.visit(node.arguments.head);

    return visit(node.selector);
  }

  SourceString visitIdentifier(Identifier node) {
    return node.source;
  }

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      SourceString name = visit(link.head);
      Element element = new VariableElement(
          link.head, definitions.type, name, resolver.context.enclosingElement);
      Element existing = resolver.defineElement(link.head, element);
      if (existing != element) {
        resolver.fail(node, ErrorMessages.duplicateDefinition(link.head));
      }
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

  Element add(Element element) {
    if (elements.containsKey(element.name)) return elements[element.name];
    elements[element.name] = element;
    return element;
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
