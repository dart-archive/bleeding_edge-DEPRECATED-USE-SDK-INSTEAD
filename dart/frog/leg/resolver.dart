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
      FunctionExpression tree = element.parseNode(compiler, compiler);
      ResolverVisitor visitor = new SignatureResolverVisitor(compiler, element);
      visitor.visit(tree);

      visitor = new FullResolverVisitor.from(visitor);
      if (tree.initializers != null) {
        resolveInitializers(element, tree, visitor);
      }
      visitor.visit(tree.body);

      // Resolve the type annotations encountered in the method.
      while (!toResolve.isEmpty()) {
        toResolve.removeFirst().resolve(compiler);
      }
      return visitor.mapping;
    });
  }

  bool isInitializer(SendSet node) {
    if (node.selector.asIdentifier() == null) return false;
    if (node.receiver == null) return true;
    if (node.receiver.asIdentifier() == null) return false;
    return node.receiver.asIdentifier().isThis();
  }

  SourceString getInitializerFieldName(SendSet node, onError(node)) {
    if (!isInitializer(node)) onError(node);
    return node.selector.asIdentifier().source;
  }

  void resolveInitializers(Element element, FunctionExpression node,
                           ResolverVisitor visitor) {
    void onError(node) {
      visitor.error(node, MessageKind.INVALID_RECEIVER_IN_INITIALIZER);
    }
    Map<SourceString, Node> initialized = new Map<SourceString, Node>();
    for (Link<Node> link = node.initializers.nodes;
         !link.isEmpty();
         link = link.tail) {
      if (link.head.asSendSet() != null) {
        SendSet init = link.head;
        SourceString name = getInitializerFieldName(init, onError);
        ClassElement classElement = element.enclosingElement;
        Element target = classElement.lookupLocalMember(name);
        Node selector = init.selector;
        if (target == null) {
          visitor.error(selector, MessageKind.CANNOT_RESOLVE, [name]);
        } else if (target.kind != ElementKind.FIELD) {
          visitor.error(selector, MessageKind.NOT_A_FIELD, [name]);
        } else if (!target.isInstanceMember()) {
          visitor.error(selector, MessageKind.INIT_STATIC_FIELD, [name]);
        }
        visitor.useElement(init, target);
        if (initialized.containsKey(name)) {
          visitor.error(init, MessageKind.DUPLICATE_INITIALIZER, [name]);
          visitor.warning(initialized[name], MessageKind.ALREADY_INITIALIZED,
                          [name]);
        }
        initialized[name] = init;
        Node value = init.arguments.head;
        visitor.visitInStaticContext(value);
      } else if (link.head.asSend() !== null) {
        // TODO(karlklose): super(...), this(...).
        compiler.cancel('uniplemented', node:link.head);
      } else {
        compiler.cancel('internal error: invalid initializer',
                        node: link.head);
      }
    }
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

class ResolverVisitor extends AbstractVisitor<Element> {
  final Compiler compiler;
  final TreeElements mapping;
  final Element enclosingElement;
  bool inInstanceContext;
  Scope context;

  ResolverVisitor(Compiler compiler, Element element)
    : this.compiler = compiler,
      this.mapping  = new TreeElements(),
      this.enclosingElement = element,
      inInstanceContext =
        element.isInstanceMember() || element.kind == ElementKind.CONSTRUCTOR,
      this.context  = element.isMember()
        ? new ClassScope(element.enclosingElement, compiler.universe)
        : new TopScope(compiler.universe);

  ResolverVisitor.from(ResolverVisitor other)
    : compiler = other.compiler,
      mapping = other.mapping,
      enclosingElement = other.enclosingElement,
      inInstanceContext = other.inInstanceContext,
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
    compiler.cancel(message, node: node);
  }

  Element lookup(Node node, SourceString name) {
    Element result = context.lookup(name);
    if (!inInstanceContext && result != null && result.isInstanceMember()) {
      error(node, MessageKind.NOT_STATIC, [node]);
    }
    return result;
  }

  visitInStaticContext(Node node) {
    bool wasInstanceContext = inInstanceContext;
    inInstanceContext = false;
    visit(node);
    inInstanceContext = wasInstanceContext;
  }

  visit(Node node) {
    if (node == null) return null;
    return node.accept(this);
  }

  visitIdentifier(Identifier node) {
    if (node.isThis()) {
      if (!inInstanceContext) error(node, MessageKind.NO_THIS_IN_STATIC);
      return null;
    } else {
      Element element = lookup(node, node.source);
      if (element == null) {
        error(node, MessageKind.CANNOT_RESOLVE, [node]);
      }
      return useElement(node, element);
    }
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
    return element;
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

  visitNode(Node node) {
    cancel(node, 'not implemented');
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
        VariableDefinitions variableDefinitions = parameterNodes.head;
        defineElement(variableDefinitions.definitions.nodes.head, link.head);
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
        node, ElementKind.FUNCTION, null, context.element);
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
      if (name.stringValue === '[]') return const SourceString('index');
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
    if (name.stringValue === '!=') return const SourceString('eq');

    // Index operator.
    if (name.stringValue === '[]') return const SourceString('index');
    compiler.unimplemented("mapOperatorToMethodName");
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
    compiler.unimplemented("mapAssignmentOperatorToMethodName");
  }

  Element resolveSend(Send node) {
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
    } else if (node.receiver === null) {
      target = lookup(node, name);
      if (target == null && !enclosingElement.isInstanceMember()) {
        error(node, MessageKind.CANNOT_RESOLVE, [name]);
      }
    } else if (receiver === null) {
      return null;
    } else if (receiver.kind === ElementKind.CLASS) {
      ClassElement receiverClass = receiver;
      target = receiverClass.resolve(compiler).lookupLocalMember(name);
      if (target == null) {
        error(node, MessageKind.METHOD_NOT_FOUND, [receiver, name]);
      } else if (target.isInstanceMember()) {
        error(node, MessageKind.MEMBER_NOT_STATIC, [receiver, name]);
      }
    }
    return target;
  }

  visitSend(Send node) {
    Element target = resolveSend(node);
    // TODO(ngeoffray): If target is a field, check that there's a
    // getter.
    return useElement(node, target);
  }

  visitSendSet(SendSet node) {
    Element target = resolveSend(node);
    // TODO(ngeoffray): If target is a field, check that there's a
    // setter.
    // TODO(ngeoffray): Check if the target can be assigned.
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
        getter = target;
      } else {
        // TODO(ngeoffray): Find the getter from the setter.
        getter = context.lookup(node.selector.asIdentifier().source);
      }
      useElement(node.selector, getter);
    }
    if (node.isIndex) {
      assert(target.name.stringValue === 'index');
      target = compiler.universe.find(const SourceString('indexSet'));
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
    Element constructor = null;
    if (cls !== null) {
      // TODO(ngeoffray): set constructor-name correctly.
      SourceString name = cls.name;
      constructor = cls.resolve(compiler).lookupConstructor(name);
      if (name == cls.name
          && constructor === null
          && node.send.argumentsNode.isEmpty()) {
        constructor = cls.getSynthesizedConstructor();
      }
      if (constructor === null) {
        error(node, MessageKind.CANNOT_FIND_CONSTRUCTOR, [node]);
      }
    }

    useElement(node.send, constructor);
    return null;
  }

  visitModifiers(Modifiers node) {
    // TODO(ngeoffray): Implement this.
    cancel(node, "Unimplemented");
  }

  visitLiteralList(LiteralList node) {
    visit(node.elements);
  }

  visitConditional(Conditional node) {
    node.visitChildren(this);
  }

  visitStringInterpolation(StringInterpolation node) {
    node.visitChildren(this);
  }

  visitStringInterpolationPart(StringInterpolationPart node) {
    node.visitChildren(this);
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
  VariableListElement variables;

  VariableDefinitionsVisitor(this.definitions, this.resolver, this.kind) {
    variables = new VariableListElement.node(
        definitions, ElementKind.VARIABLE_LIST, resolver.context.element);
  }

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
      Element element = new VariableElement(
          name, variables, kind, resolver.context.element);
      resolver.defineElement(link.head, element);
    }
  }

  visit(Node node) => node.accept(this);

  visitNode(Node node) {
    resolver.cancel(node, 'not implemented');
  }
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

  visitNode(Node node) {
    resolver.cancel(node, 'not implemented');
  }
}

class Scope {
  final Element element;
  final Scope parent;

  Scope(this.parent, this.element);
  abstract Element add(Element element);
  abstract Element lookup(SourceString name);
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
    Element element = cls.lookupLocalMember(name);
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
