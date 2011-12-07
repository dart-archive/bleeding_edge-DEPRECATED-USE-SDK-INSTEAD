// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class TreeElements {
  Map<Node, Element> map;
  TreeElements() : map = new LinkedHashMap<Node, Element>();
  operator []=(Node node, Element element) => map[node] = element;
  operator [](Node node) => map[node];
}

class ResolverTask extends CompilerTask {
  Queue<ClassElement> toResolve;
  ResolverTask(Compiler compiler)
    : super(compiler), toResolve = new Queue<ClassElement>();

  String get name() => 'Resolver';

  TreeElements resolve(FunctionElement element) {
    return measure(() {
      FunctionExpression tree = element.node;
      ResolverVisitor visitor = new SignatureResolverVisitor(compiler, element);
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

  void resolveType(ClassElement element) {
    measure(() {
      ClassNode tree = element.node;
      ClassResolverVisitor visitor = new ClassResolverVisitor(compiler);
      visitor.visit(tree);
    });
  }

  void resolveSignature(FunctionElement element) {
    measure(() {
      FunctionExpression node = element.node;
      SignatureResolverVisitor visitor =
          new SignatureResolverVisitor(compiler, element);
      visitor.visitFunctionExpression(node);
    });
  }
}

class ResolverVisitor implements Visitor<Element> {
  final Compiler compiler;
  final TreeElements mapping;
  Scope context;

  ResolverVisitor(Compiler compiler, Element element)
    : this.compiler = compiler,
      this.mapping  = new TreeElements(),
      this.context  = element.isClassMember()
        ? new ClassScope(element.enclosingElement, compiler.universe)
        : new TopScope(compiler.universe);

  ResolverVisitor.from(ResolverVisitor other)
    : compiler = other.compiler,
      mapping = other.mapping,
      context = other.context;

  error(Node node, MessageKind kind, [arguments = const []]) {
    ResolutionError error  = new ResolutionError(kind, arguments);
    compiler.reportError(node, error);
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
    // TODO(karlklose): remove when we have int in the core lib.
    if (name.source == const SourceString('int')) return null;
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
    Element existing = context.add(element);
    if (existing != element) {
      error(node, MessageKind.DUPLICATE_DEFINITION, [node]);
    }
    return element;
  }

  Element useElement(Node node, Element element) {
    if (element === null) return null;
    mapping[node] = element;
    // TODO(ngeoffray): frog does not like a return on an assignment.
    return element;
  }
}

class SignatureResolverVisitor extends ResolverVisitor {
  FunctionElement element;

  SignatureResolverVisitor(Compiler compiler, FunctionElement element)
      : super(compiler, element), this.element = element;

  visitFunctionExpression(FunctionExpression node) {
    useElement(node, element);
    context = new MethodScope(context, element);

    if (element.parameters == null) {
      ParametersVisitor visitor = new ParametersVisitor(this);
      visitor.visit(node.parameters);
      element.parameters = visitor.elements.toLink();
    } else {
      Link<Node> parameterNodes = node.parameters.nodes;
      for (Link<Element> link = element.parameters;
           !link.isEmpty() && !parameterNodes.isEmpty();
           link = link.tail, parameterNodes = parameterNodes.tail) {
        defineElement(parameterNodes.head.definitions.nodes.head, link.head);
      }
    }

    return element;
  }
}

class FullResolverVisitor extends ResolverVisitor {

  FullResolverVisitor(Compiler compiler, Element element)
    : super(compiler, element);
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
    visitIn(node.statements, new BlockScope(context));
  }

  visitDoWhile(DoWhile node) {
    visitIn(node.body, new BlockScope(context));
    visit(node.condition);
  }

  visitExpressionStatement(ExpressionStatement node) {
    visit(node.expression);
  }

  visitFor(For node) {
    Scope scope = new BlockScope(context);
    visitIn(node.initializer, scope);
    visitIn(node.condition, scope);
    visitIn(node.update, scope);
    visitIn(node.body, scope);
  }

  visitFunctionExpression(FunctionExpression node) {
    visit(node.returnType);
    FunctionElement enclosingElement = new FunctionElement.node(
        node, ElementKind.FUNCTION, context.element);
    defineElement(node, enclosingElement);
    context = new MethodScope(context, enclosingElement);

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

  static bool isLogicalOperator(Identifier op) {
    String str = op.source.stringValue;
    return (str === '&&' || str == '||' || str == '!');
  }

  SourceString mapOperatorToMethodName(final SourceString name,
                                       final bool isPrefix) {
    if (isPrefix) {
      if (name.stringValue === '-') return const SourceString('neg');
      if (name.stringValue === '~') return const SourceString('not');
      unreachable();
    }
    // Additive operators.
    if (name.stringValue === '+') return const SourceString('add');
    if (name.stringValue === '-') return const SourceString('sub');

    // Multiplicative operators.
    if (name.stringValue === '*') return const SourceString('mul');
    if (name.stringValue === '/') return const SourceString('div');
    if (name.stringValue === '~/') return const SourceString('tdiv');
    if (name.stringValue === '%') return const SourceString('mod');

    // Shift operators.
    if (name.stringValue === '<<') return const SourceString('shl');
    if (name.stringValue === '>>') return const SourceString('shr');

    // Bitwise operators.
    if (name.stringValue === '|') return const SourceString('or');
    if (name.stringValue === '&') return const SourceString('and');
    if (name.stringValue === '^') return const SourceString('xor');

    // Relational operators.
    if (name.stringValue === '<') return const SourceString('lt');
    if (name.stringValue === '<=') return const SourceString('le');
    if (name.stringValue === '>') return const SourceString('gt');
    if (name.stringValue === '>=') return const SourceString('ge');

    if (name.stringValue === '==') return const SourceString('eq');

    // Index operator.
    if (name.stringValue === '[]') return const SourceString('index');
    unreachable();
  }

  SourceString mapAssignmentOperatorToMethodName(SourceString name) {
    if (name.stringValue === '+=') return const SourceString('add');
    if (name.stringValue === '-=') return const SourceString('sub');
    if (name.stringValue === '*=') return const SourceString('mul');
    if (name.stringValue === '/=') return const SourceString('div');
    if (name.stringValue === '~/=') return const SourceString('tdiv');
    if (name.stringValue === '%=') return const SourceString('mod');
    if (name.stringValue === '<<=') return const SourceString('shl');
    if (name.stringValue === '>>=') return const SourceString('shr');
    if (name.stringValue === '|=') return const SourceString('or');
    if (name.stringValue === '&=') return const SourceString('and');
    if (name.stringValue === '^=') return const SourceString('xor');
    if (name.stringValue === '++') return const SourceString('add');
    if (name.stringValue === '--') return const SourceString('sub');
    unreachable();
  }

  visitSend(Send node) {
    Element receiver = visit(node.receiver);
    visit(node.argumentsNode);

    Identifier selector = node.selector;
    SourceString name = selector.source;
    // No need to assign an element for a logical operation.
    if (isLogicalOperator(selector)) return null;

    Element target = null;
    if (node.isOperator) {
      SourceString opName = mapOperatorToMethodName(name, node.isPrefix);
      target = compiler.universe.find(opName);
    } else if (receiver == null) {
      target = context.lookup(name);
      if (target == null) {
        // TODO(ngeoffray): Check if the enclosingElement has 'this'.
        error(node, MessageKind.CANNOT_RESOLVE, [name]);
      }
    } else if (receiver.kind === ElementKind.CLASS) {
      // TODO(ngeoffray): Find the element in the class.
    }

    return useElement(node, target);
  }

  visitSendSet(SendSet node) {
    Element receiver = visit(node.receiver);
    visit(node.argumentsNode);

    Identifier selector = node.selector;
    Element target;
    if (node.isIndex) {
      target = compiler.universe.find(const SourceString('indexSet'));
    } else {
      target = context.lookup(selector.source);
    }
    // TODO(ngeoffray): Check if the enclosingElement has 'this'.
    if (target == null) {
      error(node, MessageKind.CANNOT_RESOLVE, [node]);
    }

    Identifier op = node.assignmentOperator;
    if (op.source.stringValue !== '=') {
      // Operation-assignment. For example +=.
      // We need to resolve the '+' and also the getter for the left-hand-side.
      SourceString name = mapAssignmentOperatorToMethodName(op.source);
      Element operatorElement = compiler.universe.find(name);
      useElement(op, operatorElement);
      // Resolve the getter for the lhs (receiver+selector).
      // Currently this is the same as the setter.
      // TODO(ngeoffray): Adapt for fields.
      Element getter;
      if (node.isIndex) {
        getter = compiler.universe.find(const SourceString('index'));
      } else {
        getter = context.lookup(selector.source);
      }
      useElement(selector, getter);
    }
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

  visitWhile(While node) {
    visit(node.condition);
    visitIn(node.body, new BlockScope(context));
  }

  visitParenthesizedExpression(ParenthesizedExpression node) {
    visit(node.expression);
  }

  visitNewExpression(NewExpression node) {
    visit(node.send.argumentsNode);

    ClassElement cls = visit(node.send.selector);
    SourceString name = const SourceString("");
    Element constructor = null;
    if (cls !== null) {
      // TODO(ngeoffray): define what isResolved means. Also, pass the
      // needed element to resolve?
      if (!cls.isResolved) compiler.resolveType(cls);
      constructor = cls.lookupLocalElement(name);
      if (name.stringValue === ''
          && constructor === null
          && node.send.argumentsNode.isEmpty()
          && cls.canHaveDefaultConstructor()) {
        constructor = new SynthesizedConstructorElement(cls);
        cls.addConstructor(constructor);
      }
      if (constructor === null) {
        error(node, MessageKind.CANNOT_FIND_CONSTRUCTOR, [node]);
      }
    }

    return useElement(node, constructor);
  }

  visitLiteralList(LiteralList node) {
    visit(node.elements);
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
      compiler.reportError(node,
          new ResolutionError(MessageKind.CANNOT_RESOLVE_TYPE, [name]));
    } else if (element.kind !== ElementKind.CLASS) {
      compiler.reportError(node,
          new ResolutionError(MessageKind.NOT_A_TYPE, [name]));
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
          kind, name, resolver.context.element);
      resolver.defineElement(link.head, element);
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
  final Element element;
  final Scope parent;

  Scope(this.parent, this.element);
  abstract Element add(Element element);
  abstract Element lookup(Element element);
}

class MethodScope extends Scope {
  final Map<SourceString, Element> elements;

  MethodScope(Scope parent, Element element)
    : super(parent, element), this.elements = {};

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

class BlockScope extends MethodScope {
  BlockScope(Scope parent) : super(parent, parent.element);
}

class ClassScope extends Scope {
  ClassScope(ClassElement element, Universe universe)
    : super(new TopScope(universe), element);

  Element lookup(SourceString name) {
    ClassElement cls = element;
    Element element = cls.lookupLocalElement(name);
    if (element != null) return element;
    element = parent.lookup(name);
    if (element != null) return element;
    // TODO(ngeoffray): Lookup in the super class.
    return null;
  }

  Element add(Element element) {
    throw "Cannot add an element in a class scope";
  }
}

// TODO(ngeoffray): this top scope should have libraryElement as
// element.
class TopScope extends Scope {
  Universe universe;

  TopScope(Universe this.universe) : super(null, null);
  Element lookup(SourceString name) => universe.find(name);

  Element add(Element element) {
    throw "Cannot add an element in the top scope";
  }
}
