// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

interface TreeElements {
  Element operator[](Node node);
  Selector getSelector(Send send);
}

class TreeElementMapping implements TreeElements {
  Map<Node, Element> map;
  Map<Send, Selector> selectors;
  TreeElementMapping()
    : map = new LinkedHashMap<Node, Element>(),
      selectors = new LinkedHashMap<Send, Selector>();

  operator []=(Node node, Element element) => map[node] = element;
  operator [](Node node) => map[node];

  void setSelector(Send send, Selector selector) {
    selectors[send] = selector;
  }

  Selector getSelector(Send send) => selectors[send];
}

class ResolverTask extends CompilerTask {
  Queue<ClassElement> toResolve;

  // Caches the elements of analyzed constructors to make them available
  // for inlining in later tasks.
  Map<FunctionElement, TreeElements> constructorElements;

  ResolverTask(Compiler compiler)
    : super(compiler), toResolve = new Queue<ClassElement>(),
      constructorElements = new Map<FunctionElement, TreeElements>();

  String get name() => 'Resolver';

  TreeElements resolve(Element element) {
    return measure(() {
      switch (element.kind) {
        case ElementKind.GENERATIVE_CONSTRUCTOR:
        case ElementKind.FUNCTION:
        case ElementKind.GETTER:
        case ElementKind.SETTER:
          return resolveMethodElement(element);

        case ElementKind.FIELD:
        case ElementKind.PARAMETER:
          return resolveVariableElement(element);

        default:
          compiler.unimplemented(
              "resolver", node: element.parseNode(compiler));
      }
    });
  }

  TreeElements resolveMethodElement(FunctionElement element) {
    if (element.kind === ElementKind.GENERATIVE_CONSTRUCTOR &&
        constructorElements[element] !== null) {
      return constructorElements[element];
    }
    FunctionExpression tree = element.parseNode(compiler);
    ResolverVisitor visitor = new ResolverVisitor(compiler, element);
    visitor.useElement(tree, element);
    visitor.setupFunction(tree, element);

    if (tree.initializers != null) {
      new InitializerResolver(visitor, element).resolveInitializers(tree);
    }
    visitor.visit(tree.body);

    // Resolve the type annotations encountered in the method.
    Link<ClassElement> newResolvedClasses = const EmptyLink<ClassElement>();
    while (!toResolve.isEmpty()) {
      ClassElement classElement = toResolve.removeFirst();
      if (!classElement.isResolved) {
        classElement.resolve(compiler);
      }
      newResolvedClasses = newResolvedClasses.prepend(classElement);
    }
    checkClassHierarchy(newResolvedClasses);
    if (element.kind === ElementKind.GENERATIVE_CONSTRUCTOR) {
      constructorElements[element] = visitor.mapping;
    }
    return visitor.mapping;
  }

  TreeElements resolveVariableElement(Element element) {
    Node tree = element.parseNode(compiler);
    ResolverVisitor visitor = new ResolverVisitor(compiler, element);
    if (tree is SendSet) {
      SendSet send = tree;
      visitor.visit(send.arguments.head);
    }
    return visitor.mapping;
  }

  Type resolveType(ClassElement element) {
    return measure(() {
      ClassNode tree = element.parseNode(compiler);
      ClassResolverVisitor visitor =
        new ClassResolverVisitor(compiler, element.getLibrary());
      return visitor.visit(tree);
    });
  }

  FunctionParameters resolveSignature(FunctionElement element) {
    return measure(() => SignatureResolver.analyze(compiler, element));
  }

  void checkClassHierarchy(Link<ClassElement> classes) {
    for(; !classes.isEmpty(); classes = classes.tail) {
      ClassElement classElement = classes.head;
      calculateAllSupertypes(classElement, new Set<ClassElement>());
    }
  }

  Link<Type> getOrCalculateAllSupertypes(ClassElement classElement,
                                         [Set<ClassElement> seen]) {
    Link<Type> allSupertypes = classElement.allSupertypes;
    if (allSupertypes !== null) return allSupertypes;
    if (seen === null) seen = new Set<ClassElement>();
    calculateAllSupertypes(classElement, seen);
    return classElement.allSupertypes;
  }

  void calculateAllSupertypes(ClassElement classElement,
                              Set<ClassElement> seen) {
    // TODO(karlklose): substitute type variables.
    // TODO(karlklose): check if type arguments match, if a classelement occurs
    //                  more than once in the supertypes.
    if (classElement.allSupertypes !== null) return;
    final Type supertype = classElement.supertype;
    if (seen.contains(classElement)) {
      error(classElement.parseNode(compiler),
            MessageKind.CYCLIC_CLASS_HIERARCHY,
            [classElement.name]);
      classElement.allSupertypes = const EmptyLink<Type>();
    } else if (supertype != null) {
      Type supertype = classElement.supertype;
      seen.add(classElement);
      Link<Type> superSupertypes =
        getOrCalculateAllSupertypes(supertype.element, seen);
      Link<Type> supertypes = new Link<Type>(supertype, superSupertypes);
      for (Link<Type> interfaces = classElement.interfaces;
           !interfaces.isEmpty();
           interfaces = interfaces.tail) {
        Element element = interfaces.head.element;
        Link<Type> interfaceSupertypes =
            getOrCalculateAllSupertypes(element, seen);
        supertypes = supertypes.reversePrependAll(interfaceSupertypes);
      }
      seen.remove(classElement);
      classElement.allSupertypes = supertypes;
    } else {
      classElement.allSupertypes = const EmptyLink<Type>();
    }
  }

  error(Node node, MessageKind kind, [arguments = const []]) {
    ResolutionError error = new ResolutionError(kind, arguments);
    compiler.reportError(node, error);
  }
}

class InitializerResolver {
  final ResolverVisitor visitor;
  final FunctionElement constructor;
  final Map<SourceString, Node> initialized;
  Link<Node> initializers;
  bool hasSuper;

  InitializerResolver(this.visitor, this.constructor)
    : initialized = new Map<SourceString, Node>(), hasSuper = false;

  error(Node node, MessageKind kind, [arguments = const []]) {
    visitor.error(node, kind, arguments);
  }

  warning(Node node, MessageKind kind, [arguments = const []]) {
    visitor.warning(node, kind, arguments);
  }

  bool isFieldInitializer(SendSet node) {
    if (node.selector.asIdentifier() == null) return false;
    if (node.receiver == null) return true;
    if (node.receiver.asIdentifier() == null) return false;
    return node.receiver.asIdentifier().isThis();
  }

  void resolveFieldInitializer(SendSet init) {
    // init is of the form [this.]field = value.
    final Node selector = init.selector;
    final SourceString name = selector.asIdentifier().source;
    // Lookup target field.
    Element target;
    if (isFieldInitializer(init)) {
      final ClassElement classElement = constructor.enclosingElement;
      target = classElement.lookupLocalMember(name);
      if (target === null) {
        error(selector, MessageKind.CANNOT_RESOLVE, [name]);
      } else if (target.kind != ElementKind.FIELD) {
        error(selector, MessageKind.NOT_A_FIELD, [name]);
      } else if (!target.isInstanceMember()) {
        error(selector, MessageKind.INIT_STATIC_FIELD, [name]);
      }
    } else {
      error(init, MessageKind.INVALID_RECEIVER_IN_INITIALIZER);
    }
    visitor.useElement(init, target);
    // Check for duplicate initializers.
    if (initialized.containsKey(name)) {
      error(init, MessageKind.DUPLICATE_INITIALIZER, [name]);
      warning(initialized[name], MessageKind.ALREADY_INITIALIZED, [name]);
    }
    initialized[name] = init;
    // Resolve initializing value.
    visitor.visitInStaticContext(init.arguments.head);
  }

  SourceString getConstructorName(Send node) {
    if (node.receiver !== null) {
      return node.selector.asIdentifier().source;
    } else {
      return const SourceString('');
    }
  }

  void resolveSuperOrThis(Send call) {
    noConstructor(e) {
      if (e !== null) error(call, MessageKind.NO_CONSTRUCTOR, [e.name, e.kind]);
    }

    ClassElement lookupTarget = constructor.enclosingElement;
    bool validTarget = true;
    if (Initializers.isSuperConstructorCall(call)) {
      // Check for invalid initializers.
      if (hasSuper) {
        error(call, MessageKind.DUPLICATE_SUPER_INITIALIZER);
      }
      hasSuper = true;
      // Calculate correct lookup target and constructor name.
      if (lookupTarget.name == Types.OBJECT) {
        error(call, MessageKind.SUPER_INITIALIZER_IN_OBJECT);
      } else {
        lookupTarget = lookupTarget.supertype.element;
      }
    } else if (Initializers.isConstructorRedirect(call)) {
      // Check that there are no other initializers.
      if (!initializers.tail.isEmpty()) {
        error(call, MessageKind.REDIRECTING_CTOR_HAS_INITIALIZER);
      }
    } else {
      visitor.error(call, MessageKind.CONSTRUCTOR_CALL_EXPECTED);
      validTarget = false;
    }

    if (validTarget) {
      final SourceString className = lookupTarget.name;
      final SourceString constructorName = getConstructorName(call);
      FunctionElement target =
          lookupTarget.lookupConstructor(className, constructorName,
                                         noConstructor);
      if (target === null && call.arguments.isEmpty()) {
        target = lookupTarget.getSynthesizedConstructor();
      }
      if (target === null) {
        String name = (constructorName === const SourceString(''))
                          ? className.stringValue
                          : "$className.$constructorName";
        error(call, MessageKind.CANNOT_RESOLVE_CONSTRUCTOR, [name]);
      } else {
        final Compiler compiler = visitor.compiler;
        // TODO(karlklose): support optional arguments.
        if (target.parameterCount(compiler) != call.argumentCount()) {
          error(call, MessageKind.NO_MATCHING_CONSTRUCTOR);
        }
      }
      visitor.useElement(call, target);
    }
    // Resolve the arguments of the call.
    for (Link<Node> arguments = call.arguments;
         !arguments.isEmpty();
         arguments = arguments.tail) {
      visitor.visitInStaticContext(arguments.head);
    }
  }

  void resolveInitializers(FunctionExpression node) {
    if (node.initializers === null) return;
    initializers = node.initializers.nodes;
    Compiler compiler = visitor.compiler;
    for (Link<Node> link = initializers;
         !link.isEmpty();
         link = link.tail) {
      if (link.head.asSendSet() != null) {
        final SendSet init = link.head.asSendSet();
        resolveFieldInitializer(init);
      } else if (link.head.asSend() !== null) {
        final Send call = link.head.asSend();
        resolveSuperOrThis(call);
      } else {
        visitor.compiler.cancel('internal error: invalid initializer',
                                node: link.head);
      }
    }
  }
}

class CommonResolverVisitor<R> extends AbstractVisitor<Element> {
  final Compiler compiler;

  CommonResolverVisitor(Compiler this.compiler);

  R visitNode(Node node) {
    cancel(node, 'internal error');
  }

  /** Convenience method for visiting nodes that may be null. */
  R visit(Node node) => (node == null) ? null : node.accept(this);

  void error(Node node, MessageKind kind, [arguments = const []]) {
    ResolutionError error  = new ResolutionError(kind, arguments);
    compiler.reportError(node, error);
  }

  void warning(Node node, MessageKind kind, [arguments = const []]) {
    ResolutionWarning warning  = new ResolutionWarning(kind, arguments);
    compiler.reportWarning(node, warning);
  }

  void cancel(Node node, String message) {
    compiler.cancel(message, node: node);
  }

  void internalError(Node node, String message) {
    compiler.internalError(message, node: node);
  }

  void unimplemented(Node node, String message) {
    compiler.unimplemented(message, node: node);
  }
}

class ResolverVisitor extends CommonResolverVisitor<Element> {
  final TreeElementMapping mapping;
  final Element enclosingElement;
  bool inInstanceContext;
  Scope context;
  ClassElement currentClass;
  bool typeRequired = false;

  ResolverVisitor(Compiler compiler, Element element)
    : this.mapping  = new TreeElementMapping(),
      this.enclosingElement = element,
      inInstanceContext = element.isInstanceMember()
          || element.isGenerativeConstructor(),
      this.context  = element.isMember()
        ? new ClassScope(element.enclosingElement, element.getLibrary())
        : new TopScope(element.getLibrary()),
      this.currentClass = element.isMember() ? element.enclosingElement : null,
      super(compiler);

  Element lookup(Node node, SourceString name) {
    Element result = context.lookup(name);
    if (!inInstanceContext && result != null && result.isInstanceMember()) {
      error(node, MessageKind.NO_INSTANCE_AVAILABLE, [node]);
    }
    return result;
  }

  visitInStaticContext(Node node) {
    bool wasInstanceContext = inInstanceContext;
    inInstanceContext = false;
    visit(node);
    inInstanceContext = wasInstanceContext;
  }

  visitIdentifier(Identifier node) {
    if (node.isThis()) {
      if (!inInstanceContext) {
        error(node, MessageKind.NO_INSTANCE_AVAILABLE, [node]);
      }
      return null;
    } else if (node.isSuper()) {
      if (!inInstanceContext) error(node, MessageKind.NO_SUPER_IN_STATIC);
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
    SourceString className;
    if (node.typeName.asSend() !== null) {
      // In new and const expressions, the type name can be a Send to
      // denote named constructors or library prefixes.
      Send send = node.typeName.asSend();
      className = send.receiver.asIdentifier().source;
    } else {
      className = node.typeName.asIdentifier().source;
    }
    if (className == const SourceString('var')) return null;
    if (className == const SourceString('void')) return null;
    Element element = context.lookup(className);
    if (element === null) {
      if (typeRequired) {
        error(node, MessageKind.CANNOT_RESOLVE_TYPE, [className]);
      } else {
        warning(node, MessageKind.CANNOT_RESOLVE_TYPE, [className]);
      }
    } else if (element.kind !== ElementKind.CLASS) {
      if (typeRequired) {
        error(node, MessageKind.NOT_A_TYPE, [className]);
      } else {
        warning(node, MessageKind.NOT_A_TYPE, [className]);
      }
    } else {
      ClassElement cls = element;
      compiler.resolver.toResolve.add(element);
      // TODO(ahe): This should be a Type.
      useElement(node, element);
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
    return mapping[node] = element;
  }

  void setupFunction(FunctionExpression node, FunctionElement function) {
    context = new MethodScope(context, function);
    // Put the parameters in scope.
    FunctionParameters functionParameters = function.computeParameters(compiler);
    Link<Node> parameterNodes = node.parameters.nodes;
    functionParameters.forEachParameter((Element element) {
      if (element == functionParameters.optionalParameters.head) {
        NodeList nodes = parameterNodes.head;
        parameterNodes = nodes.nodes;
      }
      VariableDefinitions variableDefinitions = parameterNodes.head;
      defineElement(variableDefinitions.definitions.nodes.head, element);
      parameterNodes = parameterNodes.tail;
    });
  }

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
    if (node.name === null) {
      cancel(node, "anonymous functions are not implemented");
    }
    SourceString name = node.name.asIdentifier().source;
    FunctionElement enclosingElement = new FunctionElement.node(
        name, node, ElementKind.FUNCTION, new Modifiers.empty(),
        context.element);
    defineElement(node, enclosingElement);
    setupFunction(node, enclosingElement);

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

  Element resolveSend(Send node) {
    Element resolvedReceiver = visit(node.receiver);

    Element target = null;
    if (node.selector.asIdentifier() === null) {
      // We are calling a closure returned from an expression.
      assert(node.selector.asExpression() !== null);
      assert(resolvedReceiver === null);
      visit(node.selector);
    } else {
      SourceString name = node.selector.asIdentifier().source;
      if (node.receiver === null) {
        target = lookup(node, name);
        if (target === null && !enclosingElement.isInstanceMember()) {
          error(node, MessageKind.CANNOT_RESOLVE, [name]);
        }
      } else if (node.isSuperCall) {
        if (currentClass !== null) {
          ClassElement superElement = currentClass.superclass;
          if (superElement !== null) {
            // TODO(ngeoffray): The lookup should continue on super
            // classes.
            target = superElement.lookupLocalMember(name);
          }
          if (target === null) {
            error(node,
                  MessageKind.METHOD_NOT_FOUND,
                  [superElement.name, name]);
          }
        }
      } else if (resolvedReceiver === null) {
        return null;
      } else if (resolvedReceiver.kind === ElementKind.CLASS) {
        ClassElement receiverClass = resolvedReceiver;
        target = receiverClass.resolve(compiler).lookupLocalMember(name);
        if (target === null) {
          error(node, MessageKind.METHOD_NOT_FOUND, [receiverClass.name, name]);
        } else if (target.isInstanceMember()) {
          error(node, MessageKind.MEMBER_NOT_STATIC,
                [receiverClass.name, name]);
        }
      } else if (resolvedReceiver.kind === ElementKind.PREFIX) {
        PrefixElement prefix = resolvedReceiver;
        target = prefix.library.lookupLocalMember(name);
        if (target == null) {
          error(node, MessageKind.NO_SUCH_LIBRARY_MEMBER,
                [resolvedReceiver.name, name]);
        }
      }
    }
    return target;
  }

  resolveTypeTest(TypeAnnotation node) {
    resolveTypeRequired(node);
  }

  void handleArguments(Send node) {
    int count = 0;
    List<SourceString> namedArguments = <SourceString>[];
    for (Link<Node> link = node.argumentsNode.nodes;
         !link.isEmpty();
         link = link.tail) {
      count++;
      Expression argument = link.head;
      visit(argument);
      if (argument.asNamedArgument() != null) {
        NamedArgument named = argument;
        namedArguments.add(named.name.source);
      }
    }
    mapping.setSelector(node, new Invocation(count, namedArguments));
  }

  visitSend(Send node) {
    Element target = resolveSend(node);
    if (node.isOperator) {
      Operator op = node.selector.asOperator();
      if (op.source.stringValue === 'is') {
        resolveTypeTest(node.arguments.head);
        assert(node.arguments.tail.isEmpty());
        mapping.setSelector(node, Selector.BINARY_OPERATOR);
      } else if (node.arguments.isEmpty()) {
        if (node.isPrefix && op.source.stringValue === '+'
            && node.receiver.asLiteralInt() === null
            && node.receiver.asLiteralDouble() === null) {
          error(node, MessageKind.EXPECTED_LITERAL_NUMBER);
        }
        mapping.setSelector(node, Selector.UNARY_OPERATOR);
      } else {
        visit(node.argumentsNode);
        mapping.setSelector(node, Selector.BINARY_OPERATOR);
      }
    } else if (node.isIndex) {
      visit(node.argumentsNode);
      assert(node.arguments.tail.isEmpty());
      mapping.setSelector(node, Selector.INDEX);
    } else if (node.isPropertyAccess) {
      mapping.setSelector(node, Selector.GETTER);
      if (target != null && target.kind == ElementKind.ABSTRACT_FIELD) {
        AbstractFieldElement field = target;
        target = field.getter;
      }
    } else {
      handleArguments(node);
    }
    // TODO(ngeoffray): Warn if target is null and the send is
    // unqualified.
    return useElement(node, target);
  }

  visitSendSet(SendSet node) {
    Element target = resolveSend(node);
    Element setter = null;
    Element getter = null;
    if (target != null && target.kind == ElementKind.ABSTRACT_FIELD) {
      AbstractFieldElement field = target;
      setter = field.setter;
      getter = field.getter;
    } else {
      setter = target;
      getter = target;
    }
    // TODO(ngeoffray): Check if the target can be assigned.
    Identifier op = node.assignmentOperator;
    bool needsGetter = op.source.stringValue !== '=';
    Selector selector;
    if (needsGetter) {
      if (node.isIndex) {
        selector = Selector.INDEX_AND_INDEX_SET;
      } else {
        selector = Selector.GETTER_AND_SETTER;
      }
      useElement(node.selector, getter);
    } else if (node.isIndex) {
      selector = Selector.INDEX_SET;
    } else {
      selector = Selector.SETTER;
    }
    visit(node.argumentsNode);
    mapping.setSelector(node, selector);
    // TODO(ngeoffray): Warn if target is null and the send is
    // unqualified.
    return useElement(node, setter);
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
    unimplemented(node, 'operator');
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
        new VariableDefinitionsVisitor(compiler, node, this,
                                       ElementKind.VARIABLE);
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
    if (node.isConst()) cancel(node, 'const expressions are not implemented');
    if (node.send.selector.asTypeAnnotation() === null) {
      cancel(
          node, 'named constructors with type arguments are not implemented');
    }

    SourceString constructorName;
    Node typeName = node.send.selector.asTypeAnnotation().typeName;
    if (typeName.asSend() !== null) {
      Identifier receiver = typeName.asSend().receiver.asIdentifier();
      Identifier selector = typeName.asSend().selector.asIdentifier();
      SourceString className = receiver.source;
      SourceString name = selector.source;
      constructorName = Elements.constructConstructorName(className, name);
    } else {
      constructorName = typeName.asIdentifier().source;
    }
    ClassElement cls = resolveTypeRequired(node.send.selector);
    Element constructor = null;
    if (cls !== null) {
      constructor = cls.resolve(compiler).lookupConstructor(constructorName);
      if (constructorName == cls.name
          && constructor === null
          && node.send.argumentsNode.isEmpty()) {
        constructor = cls.getSynthesizedConstructor();
      }
      if (constructor === null) {
        error(node.send, MessageKind.CANNOT_FIND_CONSTRUCTOR, [node.send]);
      } else {
        FunctionElement function = constructor;
        // TODO(karlklose): handle optional arguments.
        if (node.send.argumentCount() != function.parameterCount(compiler)) {
          // TODO(ngeoffray): reslution error with wrong number of
          // parameters. We cannot do this rigth now because of the
          // List constructor.
        }
      }
    } else {
      Node selector = node.send.selector;
      error(selector, MessageKind.CANNOT_RESOLVE_TYPE, [selector]);
    }
    handleArguments(node.send);
    useElement(node.send, constructor);
    return null;
  }

  ClassElement resolveTypeRequired(Node node) {
    bool old = typeRequired;
    typeRequired = true;
    ClassElement cls = visit(node);
    typeRequired = old;
    return cls;
  }

  visitModifiers(Modifiers node) {
    // TODO(ngeoffray): Implement this.
    unimplemented(node, 'modifiers');
  }

  visitLiteralList(LiteralList node) {
    if (node.isConst()) cancel(node, 'const literal lists are not implemented');
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

  visitBreakStatement(BreakStatement node) {
    unimplemented(node, 'break');
  }

  visitContinueStatement(ContinueStatement node) {
    unimplemented(node, 'continue');
  }

  visitForInStatement(ForInStatement node) {
    visit(node.expression);
    Scope scope = new BlockScope(context);
    Node declaration = node.declaredIdentifier;
    visitIn(declaration, scope);
    visitIn(node.body, scope);
    // TODO(lrn): Also allow a single identifier.
    if ((declaration is !Send || declaration.asSend().selector is !Identifier)
        && (declaration is !VariableDefinitions ||
        !declaration.asVariableDefinitions().definitions.nodes.tail.isEmpty()))
    {
      // The variable declaration is either not an identifier, not a
      // declaration, or it's declaring more than one variable.
      error(node.declaredIdentifier, MessageKind.INVALID_FOR_IN, []);
    }
  }

  visitLabelledStatement(LabelledStatement node) {
    unimplemented(node, 'label');
  }

  visitLiteralMap(LiteralMap node) {
    unimplemented(node, 'map');
  }

  visitLiteralMapEntry(LiteralMapEntry node) {
    unimplemented(node, 'map entry');
  }

  visitNamedArgument(NamedArgument node) {
    visit(node.expression);
  }

  visitSwitchStatement(SwitchStatement node) {
    unimplemented(node, 'switch');
  }

  visitTryStatement(TryStatement node) {
    visit(node.tryBlock);
    if (node.catchBlocks.isEmpty() && node.finallyBlock == null) {
      // TODO(ngeoffray): The precise location is
      // node.getEndtoken.next. Adjust when issue #1581 is fixed.
      error(node, MessageKind.NO_CATCH_NOR_FINALLY);
    }
    visit(node.catchBlocks);
    visit(node.finallyBlock);
  }

  visitCatchBlock(CatchBlock node) {
    Scope scope = new BlockScope(context);
    if (node.formals.isEmpty()) {
      error(node, MessageKind.EMPTY_CATCH_DECLARATION);
    } else if (!node.formals.nodes.tail.isEmpty()
               && !node.formals.nodes.tail.tail.isEmpty()) {
      for (Node extra in node.formals.nodes.tail.tail) {
        error(extra, MessageKind.EXTRA_CATCH_DECLARATION);
      }
    }
    visitIn(node.formals, scope);
    visitIn(node.block, scope);
  }

  visitTypedef(Typedef node) {
    unimplemented(node, 'typedef');
  }
}

class ClassResolverVisitor extends CommonResolverVisitor<Type> {
  Scope context;

  ClassResolverVisitor(Compiler compiler, LibraryElement library)
    : context = new TopScope(library),
      super(compiler);

  Type visitClassNode(ClassNode node) {
    ClassElement element = context.lookup(node.name.source);
    compiler.ensure(element !== null);
    compiler.ensure(!element.isResolved);
    element.supertype = visit(node.superclass);
    if (element.name != Types.OBJECT && element.supertype === null) {
      ClassElement objectElement = context.lookup(Types.OBJECT);
      if (objectElement !== null && !objectElement.isResolved) {
        compiler.resolver.toResolve.add(objectElement);
      } else if (objectElement === null){
        error(node, MessageKind.CANNOT_RESOLVE_TYPE, [Types.OBJECT]);
      }
      element.supertype = new SimpleType(Types.OBJECT,
                                         objectElement);
    }
    for (Link<Node> link = node.interfaces.nodes;
         !link.isEmpty();
         link = link.tail) {
      element.interfaces = element.interfaces.prepend(visit(link.head));
    }
    return element.computeType(compiler);
  }

  Type visitTypeAnnotation(TypeAnnotation node) {
    Identifier name = node.typeName.asIdentifier();
    if (name === null) {
      unimplemented(node.typeName, "prefixes");
    }
    Element element = context.lookup(name.source);
    if (element === null) {
      error(node, MessageKind.CANNOT_RESOLVE_TYPE, [name]);
    } else if (element.kind !== ElementKind.CLASS) {
      error(node, MessageKind.NOT_A_TYPE, [name]);
    } else {
      compiler.resolver.toResolve.add(element);
      // TODO(ngeoffray): Use type variables.
      return element.computeType(compiler);
    }
    return null;
  }
}

class VariableDefinitionsVisitor extends CommonResolverVisitor<SourceString> {
  VariableDefinitions definitions;
  ResolverVisitor resolver;
  ElementKind kind;
  VariableListElement variables;

  VariableDefinitionsVisitor(Compiler compiler,
                             this.definitions, this.resolver, this.kind)
    : super(compiler)
  {
    variables = new VariableListElement.node(
        definitions, ElementKind.VARIABLE_LIST, resolver.context.element);
  }

  SourceString visitSendSet(SendSet node) {
    assert(node.arguments.tail.isEmpty()); // Sanity check
    resolver.visit(node.arguments.head);
    return visit(node.selector);
  }

  SourceString visitIdentifier(Identifier node) => node.source;

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      SourceString name = visit(link.head);
      VariableElement element = new VariableElement(
          name, variables, kind, resolver.context.element, node: link.head);
      resolver.defineElement(link.head, element);
    }
  }
}

class SignatureResolver extends CommonResolverVisitor<Element> {
  final Element enclosingElement;
  Link<Element> optionalParameters = const EmptyLink<Element>();
  int optionalParameterCount = 0;
  Node currentDefinitions;

  SignatureResolver(Compiler compiler, this.enclosingElement) : super(compiler);

  Element visitNodeList(NodeList node) {
    // This must be a list of optional arguments.
    if (node.beginToken.stringValue !== '[') {
      internalError(node, "expected optional parameters");
    }
    LinkBuilder<Element> elements = analyzeNodes(node.nodes);
    optionalParameterCount = elements.length;
    optionalParameters = elements.toLink();
    return null;
  }

  Element visitVariableDefinitions(VariableDefinitions node) {
    resolveType(node.type);

    Link<Node> definitions = node.definitions.nodes;
    if (definitions.isEmpty()) {
      cancel(node, 'internal error: no parameter definition');
      return null;
    }
    if (!definitions.tail.isEmpty()) {
      cancel(definitions.tail.head, 'internal error: extra definition');
      return null;
    }
    Node definition = definitions.head;
    if (definition is NodeList) {
      cancel(node, 'optional parameters are not implemented');
    }

    if (currentDefinitions != null) {
      cancel(node, 'function type parameters not supported');
    }
    currentDefinitions = node;
    Element element = definition.accept(this);
    currentDefinitions = null;
    return element;
  }

  Element visitIdentifier(Identifier node) {
    Element variables = new VariableListElement.node(currentDefinitions,
        ElementKind.VARIABLE_LIST, enclosingElement);
    return new VariableElement(node.source, variables,
        ElementKind.PARAMETER, enclosingElement, node: node);
  }

  Element visitSend(Send node) {
    Element element;
    if (node.receiver.asIdentifier() === null ||
        !node.receiver.asIdentifier().isThis()) {
      error(node, MessageKind.INVALID_PARAMETER, []);
    } else if (enclosingElement.kind !== ElementKind.GENERATIVE_CONSTRUCTOR) {
      error(node, MessageKind.FIELD_PARAMETER_NOT_ALLOWED, []);
    } else {
      if (node.selector.asIdentifier() == null) {
        cancel(node,
               'internal error: unimplemented receiver on parameter send');
      }
      SourceString name = node.selector.asIdentifier().source;
      element = currentClass.lookupLocalMember(name);
      if (element.kind !== ElementKind.FIELD) {
        error(node, MessageKind.NOT_A_FIELD, [name]);
      } else if (!element.isInstanceMember()) {
        error(node, MessageKind.NOT_INSTANCE_FIELD, [name]);
      }
    }
    // TODO(ngeoffray): it's not right to put the field element in
    // the parameters element. Create another element instead.
    return element;
  }

  Element visitSendSet(SendSet node) {
    Element element;
    if (node.receiver != null) {
      // TODO(ngeoffray): it's not right to put the field element in
      // the parameters element. Create another element instead.
      element = visitSend(node);
    } else if (node.selector.asIdentifier() != null) {
      Element variables = new VariableListElement.node(currentDefinitions,
          ElementKind.VARIABLE_LIST, enclosingElement);
      element = new VariableElement(node.selector.asIdentifier().source,
          variables, ElementKind.PARAMETER, enclosingElement, node: node);
    }
    // Visit the value. The compile time constant handler will
    // make sure it's a compile time constant.
    resolveExpression(node.arguments.head);
    compiler.enqueue(new WorkItem.toCompile(element));
    return element;
  }

  LinkBuilder<Element> analyzeNodes(Link<Node> link) {
    LinkBuilder<Element> elements = new LinkBuilder<Element>();
    for (; !link.isEmpty(); link = link.tail) {
      Element element = link.head.accept(this);
      if (element != null) {
        elements.addLast(element);
      } else {
        // If parameter is null, the current node should be the last,
        // and a list of optional named parameters.
        if (!link.tail.isEmpty() || (link.head is !NodeList)) {
          internalError(link.head, "expected expected optional parameters");
        }
      }
    }
    return elements;
  }

  static FunctionParameters analyze(Compiler compiler,
                                    FunctionElement element) {
    FunctionExpression node = element.parseNode(compiler);
    SignatureResolver visitor = new SignatureResolver(compiler, element);
    Link<Node> nodes = node.parameters.nodes;
    LinkBuilder<Element> parameters = visitor.analyzeNodes(nodes);
    return new FunctionParameters(parameters.toLink(),
                                  visitor.optionalParameters,
                                  parameters.length,
                                  visitor.optionalParameterCount);
  }

  // TODO(ahe): This is temporary.
  void resolveExpression(Node node) {
    if (node == null) return;
    node.accept(new ResolverVisitor(compiler, enclosingElement));
  }

  // TODO(ahe): This is temporary.
  void resolveType(Node node) {
    if (node == null) return;
    node.accept(new ResolverVisitor(compiler, enclosingElement));
  }

  // TODO(ahe): This is temporary.
  ClassElement get currentClass() {
    return enclosingElement.isMember()
      ? enclosingElement.enclosingElement : null;
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
    : super(parent, element), this.elements = new Map<SourceString, Element>();

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
  ClassScope(ClassElement element, LibraryElement library)
    : super(new TopScope(library), element);

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

class TopScope extends Scope {
  LibraryElement get library() => element;

  TopScope(LibraryElement library) : super(null, library);
  Element lookup(SourceString name) => library.find(name);

  Element add(Element element) {
    throw "Cannot add an element in the top scope";
  }
}
