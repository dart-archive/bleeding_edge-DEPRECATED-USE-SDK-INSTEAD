// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ResolverTask extends CompilerTask {
  Queue<ClassElement> toResolve;
  ResolverTask(Compiler compiler)
    : super(compiler), toResolve = new Queue<ClassElement>();

  String get name() => 'Resolver';

  Map<Node, Element> resolve(Node tree) {
    return measure(() {
      ResolverVisitor visitor = new ResolverVisitor(compiler);
      visitor.visit(tree);
      // Resolve the type annotations encountered in the method.
      while (!toResolve.isEmpty()) {
        toResolve.removeFirst().resolve(compiler);
      }
      return visitor.mapping;
    });
  }

  void resolveType(Node tree) {
    measure(() {
      ClassResolverVisitor visitor = new ClassResolverVisitor(compiler);
      visitor.visit(tree);
    });
  }
}

class ErrorMessages {
  static String cannotResolve(id)
      => "cannot resolve $id";

  static String cannotResolveType(id)
      => "cannot resolve type $id";

  static String duplicateDefinition(id)
      => "duplicate definition of $id";

  static String notAType(id)
      => "$id is not a type";
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

  warning(Node node, String message) {
    compiler.reportWarning(node, message);
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
    visit(node.returnType);
    Element enclosingElement = visit(node.name);
    Scope newScope = new Scope.enclosing(context, enclosingElement);
    visitIn(node.parameters, newScope);
    visitIn(node.body, newScope);
    return enclosingElement;
  }

  visitIdentifier(Identifier node) {
    Element element = context.lookup(node.source);
    if (element == null) fail(node, ErrorMessages.cannotResolve(node));
    return useElement(node, element);
  }

  visitIf(If node) {
    visit(node.condition);
    visit(node.thenPart);
    visit(node.elsePart);
  }

  SourceString potentiallyMapOperatorToMethodName(final SourceString name) {
    // TODO(ngeoffray): Use a map once frog can handle it.
    if (name == const SourceString('+')) return const SourceString('add');
    if (name == const SourceString('-')) return const SourceString('sub');
    if (name == const SourceString('*')) return const SourceString('mul');
    if (name == const SourceString('/')) return const SourceString('div');
    if (name == const SourceString('~/')) return const SourceString('tdiv');
    if (name == const SourceString('==')) return const SourceString('eq');
    if (name == const SourceString('<')) return const SourceString('lt');
    return name;
  }

  visitSend(Send node) {
    visit(node.receiver);
    final Identifier identifier = node.selector;
    final SourceString name =
        potentiallyMapOperatorToMethodName(identifier.source);
    // TODO(ngeoffray): Use the receiver to do the lookup.
    Element target = context.lookup(name);
    // TODO(ngeoffray): implement resolution for logical operators.
    if (target == null && !((name == const SourceString('&&') ||
                            (name == const SourceString('||'))))) {
      fail(node, ErrorMessages.cannotResolve(name));
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
  }

  visitThrow(Throw node) {
    visit(node.expression);
  }

  visitTypeAnnotation(TypeAnnotation node) {
    Identifier name = node.typeName;
    if (name.source == const SourceString('var')) return null;
    if (name.source == const SourceString('void')) return null;
    Element element = context.lookup(name.source);
    if (element === null) {
      warning(node, ErrorMessages.cannotResolveType(name));
    } else if (element.kind !== ElementKind.CLASS) {
      warning(node, ErrorMessages.notAType(name));
    } else {
      ClassElement cls = element;
      compiler.resolver.toResolve.add(element);
    }
    return useElement(node, element);
  }

  visitVariableDefinitions(VariableDefinitions node) {
    visit(node.type);
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
    mapping[node] = element;
    // TODO(ngeoffray): frog does not like a return on an assignment.
    return element;
  }
}

class ClassResolverVisitor implements Visitor<Type> {
  Compiler compiler;
  Scope context;

  ClassResolverVisitor(Compiler compiler)
    : this.compiler = compiler, context = new TopScope(compiler.universe);

  Type visitClassNode(ClassNode node) {
    ClassElement element = context.lookup(node.name.source);
    compiler.ensure(element !== null);
    compiler.ensure(!element.isResolved);
    element.supertype = visit(node.superclass);
    for (Link<Node> link = node.interfaces.nodes;
         !link.isEmpty();
         link = link.tail) {
      element.interfaces = element.interfaces.prepend(visit(link.head));
    }
    return element.computeType(compiler, null);
  }

  Type visitTypeAnnotation(TypeAnnotation node) {
    Identifier name = node.typeName;
    Element element = context.lookup(name.source);
    if (element === null) {
      // TODO(ngeoffray): Should be a reportError.
      compiler.cancel(ErrorMessages.cannotResolveType(name));
    } else if (element.kind !== ElementKind.CLASS) {
      // TODO(ngeoffray): Should be a reportError.
      compiler.cancel(ErrorMessages.notAType(name));
    } else {
      compiler.resolver.toResolve.add(element);
      // TODO(ngeoffray): Use type variables.
      return element.computeType(compiler, null);
    }
    return null;
  }

  Type visit(Node node) {
    if (node === null) return null;
    return node.accept(this);
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

  SourceString visitIdentifier(Identifier node) => node.source;

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
