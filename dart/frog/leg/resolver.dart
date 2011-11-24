// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ResolverTask extends CompilerTask {
  Queue<ClassElement> toResolve;
  ResolverTask(Compiler compiler)
    : super(compiler), toResolve = new Queue<ClassElement>();

  String get name() => 'Resolver';

  Map<Node, Element> resolve(FunctionExpression tree) {
    return measure(() {
      ResolverVisitor visitor = new SignatureResolverVisitor(compiler);
      visitor.visit(tree);

      visitor = new FullResolverVisitor.from(visitor);
      visitor.visit(tree.body);

      // Resolve the type annotations encountered in the method.
      while (!toResolve.isEmpty()) {
        toResolve.removeFirst().resolve(compiler);
      }
      return visitor.mapping;
    });
  }

  // Used for testing.
  Map<Node, Element> resolveStatement(Node node) {
    ResolverVisitor visitor = new FullResolverVisitor(compiler);
    visitor.visit(node);

    // Resolve the type annotations encountered in the code.
    while (!toResolve.isEmpty()) {
      toResolve.removeFirst().resolve(compiler);
    }
    return visitor.mapping;
  }

  void resolveType(ClassNode tree) {
    measure(() {
      ClassResolverVisitor visitor = new ClassResolverVisitor(compiler);
      visitor.visit(tree);
    });
  }

  void resolveSignature(FunctionExpression node) {
    measure(() {
      SignatureResolverVisitor visitor = new SignatureResolverVisitor(compiler);
      visitor.visitFunctionExpression(node);
    });
  }
}

class ResolverVisitor implements Visitor<Element> {
  final Compiler compiler;
  final Map<Node, Element> mapping;
  Scope context;

  ResolverVisitor(Compiler compiler)
    : this.compiler = compiler,
      mapping = new LinkedHashMap<Node, Element>(),
      context = new Scope(new TopScope(compiler.universe));

  ResolverVisitor.from(ResolverVisitor other)
    : compiler = other.compiler,
      mapping = other.mapping,
      context = other.context;

  error(Node node, MessageKind kind, [arguments = const []]) {
    ResolutionError error  = new ResolutionError(kind, arguments);
    compiler.cancel(error.toString());
  }

  warning(Node node, MessageKind kind, [arguments = const []]) {
    ResolutionWarning warning  = new ResolutionWarning(kind, arguments);
    compiler.reportWarning(node, warning);
  }

  cancel(Node node, String message) {
    compiler.cancel(message);
  }

  visit(Node node) {
    if (node == null) return null;
    return node.accept(this);
  }

  visitIdentifier(Identifier node) {
    Element element = context.lookup(node.source);
    if (element == null) {
      error(node, MessageKind.CANNOT_RESOLVE, [node]);
    }
    return useElement(node, element);
  }

  visitTypeAnnotation(TypeAnnotation node) {
    Identifier name = node.typeName;
    if (name.source == const SourceString('var')) return null;
    if (name.source == const SourceString('void')) return null;
    Element element = context.lookup(name.source);
    if (element === null) {
      warning(node, MessageKind.CANNOT_RESOLVE_TYPE, [name]);
    } else if (element.kind !== ElementKind.CLASS) {
      warning(node, MessageKind.NOT_A_TYPE, [name]);
    } else {
      ClassElement cls = element;
      compiler.resolver.toResolve.add(element);
    }
    return useElement(node, element);
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

class SignatureResolverVisitor extends ResolverVisitor {

  SignatureResolverVisitor(Compiler compiler) : super(compiler);

  visitFunctionExpression(FunctionExpression node) {
    FunctionElement enclosingElement = visit(node.name);
    context = new Scope.enclosing(context, enclosingElement);

    if (enclosingElement.parameters == null) {
      ParametersVisitor visitor = new ParametersVisitor(this);
      visitor.visit(node.parameters);
      enclosingElement.parameters = visitor.elements.toLink();
    } else {
      Link<Node> parameterNodes = node.parameters.nodes;
      for (Link<Element> link = enclosingElement.parameters;
           !link.isEmpty() && !parameterNodes.isEmpty();
           link = link.tail, parameterNodes = parameterNodes.tail) {
        defineElement(parameterNodes.head.definitions.nodes.head, link.head);
      }
    }

    return enclosingElement;
  }
}

class FullResolverVisitor extends ResolverVisitor {

  FullResolverVisitor(Compiler compiler) : super(compiler);
  FullResolverVisitor.from(ResolverVisitor other) : super.from(other);

  Element visitClassNode(ClassNode node) {
    cancel(node, "shouldn't be called");
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
    FunctionElement enclosingElement = visit(node.name);
    context = new Scope.enclosing(context, enclosingElement);

    ParametersVisitor visitor = new ParametersVisitor(this);
    visitor.visit(node.parameters);
    enclosingElement.parameters = visitor.elements.toLink();

    visit(node.body);
    context = context.parent;
    return enclosingElement;
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
    if (name == const SourceString('%')) return const SourceString('mod');
    if (name == const SourceString('==')) return const SourceString('eq');
    if (name == const SourceString('<')) return const SourceString('lt');
    if (name == const SourceString('<=')) return const SourceString('le');
    if (name == const SourceString('>')) return const SourceString('gt');
    if (name == const SourceString('>=')) return const SourceString('ge');
    if (name == const SourceString('<<')) return const SourceString('shl');
    if (name == const SourceString('>>')) return const SourceString('shr');
    return name;
  }

  visitSend(Send node) {
    visit(node.receiver);
    final Identifier identifier = node.selector;
    if (node.receiver !== null && identifier is !Operator) {
      cancel(node, 'Cannot handle qualified method calls');
    }
    final SourceString name =
        potentiallyMapOperatorToMethodName(identifier.source);
    // TODO(ngeoffray): Use the receiver to do the lookup.
    Element target = context.lookup(name);
    // TODO(ngeoffray): implement resolution for logical operators.
    if (target == null && !((name == const SourceString('&&') ||
                             name == const SourceString('||') ||
                             name == const SourceString('!')))) {
      error(node, MessageKind.CANNOT_RESOLVE, [name]);
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
    if (target == null) {
      error(node, MessageKind.CANNOT_RESOLVE, [node]);
    }
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

  visitLiteralNull(LiteralNull node) {
  }

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      visit(link.head);
    }
  }

  visitOperator(Operator node) {
    cancel(node, "Unimplemented");
  }

  visitReturn(Return node) {
    visit(node.expression);
  }

  visitThrow(Throw node) {
    visit(node.expression);
  }

  visitVariableDefinitions(VariableDefinitions node) {
    visit(node.type);
    VariableDefinitionsVisitor visitor =
        new VariableDefinitionsVisitor(node, this, ElementKind.VARIABLE);
    visitor.visit(node.definitions);
  }
}

class ClassResolverVisitor extends AbstractVisitor<Type> {
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
      compiler.cancel(
          new ResolutionError(MessageKind.CANNOT_RESOLVE_TYPE, [name]).toString());
    } else if (element.kind !== ElementKind.CLASS) {
      // TODO(ngeoffray): Should be a reportError.
      compiler.cancel(
          new ResolutionError(MessageKind.NOT_A_TYPE, [name]).toString());
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

  visitNode(Node node) {
    compiler.cancel('internal error');
  }
}

class VariableDefinitionsVisitor extends AbstractVisitor<SourceString> {
  VariableDefinitions definitions;
  ResolverVisitor resolver;
  ElementKind kind;

  VariableDefinitionsVisitor(this.definitions, this.resolver, this.kind);

  SourceString visitSendSet(SendSet node) {
    assert(node.arguments.tail.isEmpty()); // Sanity check
    if (node.receiver !== null) {
      resolver.cancel(node,
          "receiver on a variable definition not implemented");
    }
    Identifier selector = node.selector;
    resolver.visit(node.arguments.head);

    return visit(node.selector);
  }

  SourceString visitIdentifier(Identifier node) => node.source;

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      SourceString name = visit(link.head);
      Element element = new VariableElement(link.head, definitions.type,
          kind, name, resolver.context.enclosingElement);
      Element existing = resolver.defineElement(link.head, element);
      if (existing != element) {
        resolver.error(node, MessageKind.DUPLICATE_DEFINITION, [link.head]);
      }
    }
  }

  visit(Node node) => node.accept(this);
}

class ParametersVisitor extends AbstractVisitor<Element> {
  ResolverVisitor resolver;
  LinkBuilder<Element> elements;
  ParametersVisitor(this.resolver) : elements = new LinkBuilder<Element>();

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      elements.addLast(visit(link.head));
    }
  }

  visitVariableDefinitions(VariableDefinitions node) {
    resolver.visit(node.type);
    VariableDefinitionsVisitor visitor =
        new VariableDefinitionsVisitor(node, resolver, ElementKind.PARAMETER);
    visitor.visit(node.definitions);
    return resolver.mapping[node.definitions.nodes.head];
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

  Element add(Element element) {
    throw "Cannot add an element in the top scope";
  }
}
