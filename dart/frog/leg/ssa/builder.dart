// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Interceptors {
  Compiler compiler;
  Interceptors(Compiler this.compiler);

  SourceString mapOperatorToMethodName(Operator op) {
    String name = op.source.stringValue;
    if (name === '+') return const SourceString('add');
    if (name === '-') return const SourceString('sub');
    if (name === '*') return const SourceString('mul');
    if (name === '/') return const SourceString('div');
    if (name === '~/') return const SourceString('tdiv');
    if (name === '%') return const SourceString('mod');
    if (name === '<<') return const SourceString('shl');
    if (name === '>>') return const SourceString('shr');
    if (name === '|') return const SourceString('or');
    if (name === '&') return const SourceString('and');
    if (name === '^') return const SourceString('xor');
    if (name === '<') return const SourceString('lt');
    if (name === '<=') return const SourceString('le');
    if (name === '>') return const SourceString('gt');
    if (name === '>=') return const SourceString('ge');
    if (name === '==') return const SourceString('eq');
    if (name === '!=') return const SourceString('eq');
    if (name === '===') return const SourceString('eqq');
    if (name === '!==') return const SourceString('eqq');
    if (name === '+=') return const SourceString('add');
    if (name === '-=') return const SourceString('sub');
    if (name === '*=') return const SourceString('mul');
    if (name === '/=') return const SourceString('div');
    if (name === '~/=') return const SourceString('tdiv');
    if (name === '%=') return const SourceString('mod');
    if (name === '<<=') return const SourceString('shl');
    if (name === '>>=') return const SourceString('shr');
    if (name === '|=') return const SourceString('or');
    if (name === '&=') return const SourceString('and');
    if (name === '^=') return const SourceString('xor');
    if (name === '++') return const SourceString('add');
    if (name === '--') return const SourceString('sub');
    compiler.unimplemented('Unknown operator', node: op);
  }

  Element getStaticInterceptor(SourceString name, int parameters) {
    String mangledName = "builtin\$${name}\$${parameters}";
    Element result = compiler.coreLibrary.find(new SourceString(mangledName));
    return result;
  }

  Element getStaticGetInterceptor(SourceString name) {
    String mangledName = "builtin\$get\$${name}";
    Element result = compiler.coreLibrary.find(new SourceString(mangledName));
    return result;
  }

  Element getOperatorInterceptor(Operator op) {
    SourceString name = mapOperatorToMethodName(op);
    Element result = compiler.coreLibrary.find(name);
    return result;
  }

  Element getPrefixOperatorInterceptor(Operator op) {
    String name = op.source.stringValue;
    if (name === '~') {
      return compiler.coreLibrary.find(const SourceString('not'));
    }
    if (name === '-') {
      return compiler.coreLibrary.find(const SourceString('neg'));
    }
    compiler.unimplemented('Unknown operator', node: op);
  }

  Element getIndexInterceptor() {
    return compiler.coreLibrary.find(const SourceString('index'));
  }

  Element getIndexAssignmentInterceptor() {
    return compiler.coreLibrary.find(const SourceString('indexSet'));
  }

  Element getEqualsNullInterceptor() {
    return compiler.coreLibrary.find(const SourceString('eqNull'));
  }
}

class SsaBuilderTask extends CompilerTask {
  SsaBuilderTask(Compiler compiler)
    : super(compiler), interceptors = new Interceptors(compiler);
  String get name() => 'SSA builder';
  Interceptors interceptors;

  HGraph build(WorkItem work) {
    return measure(() {
      FunctionElement element = work.element;
      HInstruction.idCounter = 0;
      SsaBuilder builder = new SsaBuilder(compiler, work);
      HGraph graph;
      switch (element.kind) {
        case ElementKind.GENERATIVE_CONSTRUCTOR:
          graph = compileConstructor(builder, work);
          break;
        case ElementKind.GENERATIVE_CONSTRUCTOR_BODY:
        case ElementKind.FUNCTION:
        case ElementKind.GETTER:
        case ElementKind.SETTER:
          graph = builder.buildMethod(work.element);
          break;
      }
      assert(graph.isValid());
      if (GENERATE_SSA_TRACE) {
        String name;
        if (element.enclosingElement !== null) {
          name = "${element.enclosingElement.name}.${element.name}";
          if (element.kind == ElementKind.GENERATIVE_CONSTRUCTOR_BODY) {
            name = "$name (body)";
          }
        } else {
          name = "${element.name}";
        }
        new HTracer.singleton().traceCompilation(name);
        new HTracer.singleton().traceGraph('builder', graph);
      }
      return graph;
    });
  }

  HGraph compileConstructor(SsaBuilder builder, WorkItem work) {
    // The body of the constructor will be generated in a separate function.
    final ClassElement classElement = work.element.enclosingElement;
    return builder.buildFactory(classElement, work.element);
  }
}

/**
 * Keeps track of locals (including parameters and phis) when building. The
 * 'this' reference is treated as parameter and hence handled by this class,
 * too.
 */
class LocalsHandler {
  // The values of locals that can be directly accessed (without redirections
  // to boxes or closure-fields).
  Map<Element, HInstruction> directLocals;
  Map<Element, Element> redirectionMapping;
  SsaBuilder builder;
  ClosureData closureData;

  LocalsHandler(this.builder)
      : directLocals = new Map<Element, HInstruction>(),
        redirectionMapping = new Map<Element, Element>();

  /**
   * Creates a new [LocalsHandler] based on [other]. We only need to
   * copy the [directLocals], since the other fields can be shared
   * throughout the AST visit.
   */
  LocalsHandler.from(LocalsHandler other)
      : directLocals = new Map<Element, HInstruction>.from(other.directLocals),
        redirectionMapping = other.redirectionMapping,
        builder = other.builder,
        closureData = other.closureData;

  /**
   * Redirects accesses from element [from] to element [to]. The [to] element
   * must be a boxed variable or a variable that is stored in a closure-field.
   */
  void redirectElement(Element from, Element to) {
    assert(redirectionMapping[from] === null);
    redirectionMapping[from] = to;
    assert(isStoredInClosureField(from) || isBoxed(from));
  }

  /**
   * If the scope (function or loop) [node] has captured variables then this
   * method creates a box and sets up the redirections.
   */
  void enterScope(Node node) {
    // See if any variable in the top-scope of the function is captured. If yes
    // we need to create a box-object.
    ClosureScope scopeData = closureData.capturingScopes[node];
    if (scopeData !== null) {
      // The scope has captured variables. Create a box.
      // TODO(floitsch): Clean up this hack. Should we create a box-object by
      // just creating an empty object literal?
      HInstruction box = new HForeign(const SourceString("{}"),
                                      const SourceString('Object'),
                                      <HInstruction>[]);
      builder.add(box);
      // Add the box to the known locals.
      directLocals[scopeData.boxElement] = box;
      // Make sure that accesses to the boxed locals go into the box. We also
      // need to make sure that parameters are copied into the box if necessary.
      scopeData.capturedVariableMapping.forEach((Element from, Element to) {
        // The [from] can only be a parameter for function-scopes and not
        // loop scopes.
        if (from.kind == ElementKind.PARAMETER) {
          // Store the captured parameter in the box. Get the current value
          // before we put the redirection in place.
          HInstruction instruction = readLocal(from);
          redirectElement(from, to);
          // Now that the redirection is set up, the update to the local will
          // write the parameter value into the box.
          updateLocal(from, instruction);
        } else {
          redirectElement(from, to);
        }
      });
    }    
  }

  void startFunction(FunctionElement function,
                     FunctionExpression node) {

    ClosureTranslator translator =
        new ClosureTranslator(builder.compiler, builder.elements);
    closureData = translator.translate(node);

    FunctionParameters params = function.computeParameters(builder.compiler);
    params.forEachParameter((Element element) {
      HParameterValue parameter = new HParameterValue(element);
      builder.add(parameter);
      // Note that for constructors [element] could be a field-element which we
      // treat as if it was a local.
      directLocals[element] = parameter;
    });

    enterScope(node);

    // If the freeVariableMapping is not empty, then this function was a
    // nested closure that captures variables. Redirect the captured
    // variables to fields in the closure.
    closureData.freeVariableMapping.forEach((Element from, Element to) {
      redirectElement(from, to);
    });
    if (closureData.isClosure()) {
      // Inside closure redirect references to itself to [:this:].
      HInstruction thisInstruction = new HThis();
      builder.add(thisInstruction);
      updateLocal(closureData.closureElement, thisInstruction);
    } else if (function.isInstanceMember() ||
               function.isGenerativeConstructor()) {
      HInstruction thisInstruction = new HThis();
      builder.add(thisInstruction);
      updateLocal(closureData.thisElement, thisInstruction);
    }
  }

  /**
   * Returns true if the [For] loop declares a variable that is boxed.
   */
  bool isForLoopDeclaringBoxedLoopVariable(Loop node) {
    For forNode = node.asFor();
    if (forNode === null) return false;
    if (forNode.initializer === null) return false;
    VariableDefinitions definitions =
        forNode.initializer.asVariableDefinitions();
    if (definitions == null) return false;
    ClosureScope scopeData = closureData.capturingScopes[node];
    if (scopeData === null) return false;
    Map<Element, Element> capturedVariableMapping =
        scopeData.capturedVariableMapping;
    for (Link<Node> link = definitions.definitions.nodes;
         !link.isEmpty();
         link = link.tail) {
      Node definition = link.head;
      Element element = builder.elements[definition];
      if (capturedVariableMapping.containsKey(element)) return true;
    }
    return false;
  }

  bool hasValueForDirectLocal(Element element) {
    assert(element !== null);
    assert(isAccessedDirectly(element));
    return directLocals[element] !== null;
  }

  /**
   * Returns true if the local can be accessed directly. Boxed variables or
   * captured variables that are stored in the closure-field return [false].
   */
  bool isAccessedDirectly(Element element) {
    assert(element !== null);
    return redirectionMapping[element] === null
        && !closureData.usedVariablesInTry.contains(element);
  }

  bool isStoredInClosureField(Element element) {
    assert(element !== null);
    if (isAccessedDirectly(element)) return false;
    Element redirectElement = redirectionMapping[element];
    if (redirectElement == null) return false;
    if (redirectElement.enclosingElement.kind == ElementKind.CLASS) {
      assert(redirectElement is ClosureFieldElement);
      return true;
    }
    return false;
  }

  bool isBoxed(Element element) {
    if (isAccessedDirectly(element)) return false;
    if (isStoredInClosureField(element)) return false;
    return redirectionMapping[element] !== null;
  }

  bool isUsedInTry(Element element) {
    return closureData.usedVariablesInTry.contains(element);
  }

  /**
   * Returns an [HInstruction] for the given element. If the element is
   * boxed or stored in a closure then the method generates code to retrieve
   * the value.
   */
  HInstruction readLocal(Element element) {
    if (isAccessedDirectly(element)) {
      if (directLocals[element] == null) {
        builder.compiler.internalError(
            "Could not find value", node: element.parseNode(builder.compiler));
      }
      return directLocals[element];
    } else if (isStoredInClosureField(element)) {
      Element redirect = redirectionMapping[element];
      // We must not use the [LocalsHandler.readThis()] since that could
      // point to a captured this which would be stored in a closure-field
      // itself.
      HInstruction receiver = new HThis();
      builder.add(receiver);
      HInstruction fieldGet = new HFieldGet(redirect, receiver);
      builder.add(fieldGet);
      return fieldGet;
    } else if (isBoxed(element)) {
      Element redirect = redirectionMapping[element];
      // In the function that declares the captured variable the box is
      // accessed as direct local. Inside the nested closure the box is
      // accessed through a closure-field.
      // Calling [readLocal] makes sure we generate the correct code to get
      // the box.
      assert(redirect.enclosingElement.kind == ElementKind.VARIABLE);
      HInstruction box = readLocal(redirect.enclosingElement);
      HInstruction lookup = new HFieldGet(redirect, box);
      builder.add(lookup);
      return lookup;
    } else {
      assert(isUsedInTry(element));
      HInstruction variable = new HFieldGet.fromActivation(element);
      builder.add(variable);
      return variable;
    }
  }

  HInstruction readThis() {
    return readLocal(closureData.thisElement);
  }

  /**
   * Sets the [element] to [value]. If the element is boxed or stored in a
   * closure then the method generates code to set the value.
   */
  void updateLocal(Element element, HInstruction value) {
    if (isAccessedDirectly(element)) {
      directLocals[element] = value;
    } else if (isStoredInClosureField(element)) {
      Element redirect = redirectionMapping[element];
      // We must not use the [LocalsHandler.readThis()] since that could
      // point to a captured this which would be stored in a closure-field
      // itself.
      HInstruction receiver = new HThis();
      builder.add(receiver);
      builder.add(new HFieldSet(redirect, receiver, value));
    } else if (isBoxed(element)) {
      Element redirect = redirectionMapping[element];
      // The box itself could be captured, or be local. A local variable that
      // is captured will be boxed, but the box itself will be a local.
      // Inside the closure the box is stored in a closure-field and cannot
      // be accessed directly.
      assert(redirect.enclosingElement.kind == ElementKind.VARIABLE);
      HInstruction box = readLocal(redirect.enclosingElement);
      builder.add(new HFieldSet(redirect, box, value));
    } else {
      assert(isUsedInTry(element));
      builder.add(new HFieldSet.fromActivation(element,value));
    }
  }

  void startLoop(Node node, HBasicBlock loopEntry) {
    // Create a copy because we modify the map while iterating over
    // it.
    Map<Element, HInstruction> saved =
        new Map<Element, HInstruction>.from(directLocals);

    // Create phis for all elements in the definitions environment.
    saved.forEach((Element element, HInstruction instruction) {
      // We know 'this' cannot be modified.
      if (element !== closureData.thisElement) {
        HPhi phi = new HPhi.singleInput(element, instruction);
        loopEntry.addPhi(phi);
        directLocals[element] = phi;
      } else {
        directLocals[element] = instruction;
      }
    });

    enterScope(node);
  }

  void endLoop(HBasicBlock loopEntry) {
    loopEntry.forEachPhi((HPhi phi) {
      Element element = phi.element;
      HInstruction postLoopDefinition = directLocals[element];
      phi.addInput(postLoopDefinition);
    });
  }

  void mergeWith(LocalsHandler otherLocals, HBasicBlock joinBlock) {
    // If an element is in one map but not the other we can safely
    // ignore it. It means that a variable was declared in the
    // block. Since variable declarations are scoped the declared
    // variable cannot be alive outside the block. Note: this is only
    // true for nodes where we do joins.
    Map<Element, HInstruction> joinedLocals = new Map<Element, HInstruction>();
    otherLocals.directLocals.forEach((element, instruction) {
      // We know 'this' cannot be modified.
      if (element === closureData.thisElement) {
        assert(directLocals[element] == instruction);
        joinedLocals[element] = instruction;
      } else {
        HInstruction mine = directLocals[element];
        if (mine === null) return;
        if (instruction === mine) {
          joinedLocals[element] = instruction;
        } else {
          HInstruction phi = new HPhi.manyInputs(element, [instruction, mine]);
          joinBlock.addPhi(phi);
          joinedLocals[element] = phi;
        }
      }
    });
    directLocals = joinedLocals;
  }
}

class SsaBuilder implements Visitor {
  final Compiler compiler;
  TreeElements elements;
  final Interceptors interceptors;
  final WorkItem work;
  bool methodInterceptionEnabled;
  HGraph graph;
  LocalsHandler localsHandler;

  // We build the Ssa graph by simulating a stack machine.
  List<HInstruction> stack;

  // The current block to add instructions to. Might be null, if we are
  // visiting dead code.
  HBasicBlock current;

  SsaBuilder(Compiler compiler, WorkItem work)
    : this.compiler = compiler,
      this.work = work,
      interceptors = compiler.builder.interceptors,
      methodInterceptionEnabled = true,
      elements = work.resolutionTree,
      graph = new HGraph(),
      stack = new List<HInstruction>() {
    localsHandler = new LocalsHandler(this);
  }

  void disableMethodInterception() {
    assert(methodInterceptionEnabled);
    methodInterceptionEnabled = false;
  }

  void enableMethodInterception() {
    assert(!methodInterceptionEnabled);
    methodInterceptionEnabled = true;
  }

  HGraph buildMethod(FunctionElement functionElement) {
    FunctionExpression function = functionElement.parseNode(compiler);
    openFunction(functionElement, function);
    function.body.accept(this);
    return closeFunction();
  }

  /**
   * Returns the constructor body associated with the given constructor or
   * creates a new constructor body, if none can be found.
   */
  ConstructorBodyElement getConstructorBody(ClassElement classElement,
                                            FunctionElement constructor) {
    assert(constructor.kind === ElementKind.GENERATIVE_CONSTRUCTOR);
    ConstructorBodyElement bodyElement;
    for (Link<Element> backendMembers = classElement.backendMembers;
         !backendMembers.isEmpty();
         backendMembers = backendMembers.tail) {
      Element backendMember = backendMembers.head;
      if (backendMember.kind == ElementKind.GENERATIVE_CONSTRUCTOR_BODY) {
        ConstructorBodyElement body = backendMember;
        if (body.constructor == constructor) {
          bodyElement = backendMember;
          break;
        }
      }
    }
    if (bodyElement === null) {
      bodyElement = new ConstructorBodyElement(constructor);
      TreeElements treeElements =
          compiler.resolver.resolveMethodElement(bodyElement);
      compiler.enqueue(new WorkItem.toCodegen(bodyElement, treeElements));
      classElement.backendMembers =
          classElement.backendMembers.prepend(bodyElement);
    }
    assert(bodyElement.kind === ElementKind.GENERATIVE_CONSTRUCTOR_BODY);
    return bodyElement;
  }

  /**
   * Call [f] for every argument and parameter element of [target]
   * that is used in the invocation [send].
   */
  forEachArgument(Send send, FunctionElement target,
                  f(VariableElement parameter, Node argument)) {
    final FunctionParameters parameters = target.computeParameters(compiler);
    Link<Element> parameterElements = parameters.requiredParameters;
    for (Link<Node> arguments = send.arguments;
         !arguments.isEmpty();
         arguments = arguments.tail) {
      if (parameterElements.isEmpty()) {
        parameterElements = parameters.optionalParameters;
      }
      f(parameterElements.head, arguments.head);
      parameterElements = parameterElements.tail;
    };
  }

  /**
   * Run through the initializers and inline all field initializers. Returns the
   * next constructor to analyze.
   */
  FunctionElement analyzeInitializers(Link<Node> initializers) {
    FunctionElement nextConstructor;
    for (Link<Node> link = initializers; !link.isEmpty(); link = link.tail) {
      assert(link.head is Send);
      if (link.head is !SendSet) {
        // A super initializer or constructor redirection.
        Send call = link.head;
        if (Initializers.isSuperConstructorCall(call)) {
          assert(nextConstructor === null);
          nextConstructor = elements[call];
          // Visit arguments and map the corresponding parameter value to
          // the resulting HInstruction value.
          forEachArgument(call, nextConstructor, (parameter, node) {
            visit(node);
            HInstruction value = pop();
            localsHandler.updateLocal(parameter, value);
          });
        } else {
          compiler.unimplemented('SsaBuilder.buildFactory redirect');
        }
      } else {
        // A field initializer.
        SendSet init = link.head;
        Link<Node> arguments = init.arguments;
        assert(!arguments.isEmpty() && arguments.tail.isEmpty());
        visit(arguments.head);
        // We treat the init field-elements like locals. In the context of
        // the factory this is correct, and simplifies dealing with
        // parameter-initializers (like A(this.x)).
        localsHandler.updateLocal(elements[init], pop());
      }
    }
    return nextConstructor;
  }

  /**
   * Build the factory function corresponding to the constructor
   * [functionElement]:
   *  - Initialize fields with the values of the field initializers of the
   *    current constructor and super constructors or constructors redirected
   *    to, starting from the current constructor.
   *  - Call the the constructor bodies, starting from the constructor(s) in the
   *    super class(es).
   */
  HGraph buildFactory(ClassElement classElement,
                      FunctionElement functionElement) {
    FunctionExpression function = functionElement.parseNode(compiler);
    // The initializer list could contain closures.
    openFunction(functionElement, function);

    final Map<FunctionElement, TreeElements> constructorElements =
        compiler.resolver.constructorElements;
    List<FunctionElement> constructors = new List<FunctionElement>();

    // Analyze the constructor and all referenced constructors and collect
    // initializers and constructor bodies.
    FunctionElement nextConstructor = functionElement;
    while (nextConstructor != null) {
      FunctionElement constructor = nextConstructor;
      constructors.addLast(constructor);
      nextConstructor = null;
      elements = compiler.resolver.resolveMethodElement(constructor);
      FunctionExpression functionNode = constructor.parseNode(compiler);
      Link<Node> initializers = const EmptyLink<Node>();
      if (functionNode.initializers !== null) {
        nextConstructor = analyzeInitializers(functionNode.initializers.nodes);
      }
      if (nextConstructor === null) {
        // No super initializer found. Try to find the default constructor if
        // the class is not Object.
        ClassElement enclosingClass = constructor.enclosingElement;
        ClassElement superClass = enclosingClass.superclass;
        ClassElement objectElement = compiler.coreLibrary.find(Types.OBJECT);
        if (enclosingClass != objectElement) {
          assert(superClass !== null);
          assert(superClass.isResolved);
          nextConstructor = superClass.lookupConstructor(superClass.name);
          if (nextConstructor === null &&
              superClass.canHaveDefaultConstructor()) {
            nextConstructor = superClass.getSynthesizedConstructor();
          } else if (nextConstructor === null) {
            compiler.internalError("no default constructor available");
          }
        }
      }
    }
    // Call the JavaScript constructor with the fields as argument.
    // TODO(floitsch,karlklose): move this code to ClassElement and share with
    //                           the emitter.
    List<HInstruction> constructorArguments = <HInstruction>[];
    ClassElement element = classElement;
    while (element != null) {
      for (Element member in element.members) {
        if (member.isInstanceMember() && member.kind == ElementKind.FIELD) {
          HInstruction value;
          if (localsHandler.hasValueForDirectLocal(member)) {
            value = localsHandler.readLocal(member);
          } else {
            // TODO(karlklose): get default value.
            value = new HLiteral(null, HType.UNKNOWN);
            add(value);
          }
          constructorArguments.add(value);
        }
      }
      element = element.superclass;
    }
    HForeignNew newObject = new HForeignNew(classElement, constructorArguments);
    add(newObject);
    // Generate calls to the constructor bodies.
    for (int index = constructors.length - 1; index >= 0; index--) {
      FunctionElement constructor = constructors[index];
      ConstructorBodyElement body = this.getConstructorBody(classElement,
                                                            constructor);
      List bodyCallInputs = <HInstruction>[];
      bodyCallInputs.add(newObject);
      body.functionParameters.forEachParameter((parameter) {
        bodyCallInputs.add(localsHandler.readLocal(parameter));
      });
      SourceString methodName = body.name;
      add(new HInvokeDynamicMethod(null, methodName, bodyCallInputs));
    }
    close(new HReturn(newObject)).addSuccessor(graph.exit);
    return closeFunction();
  }

  void openFunction(FunctionElement functionElement,
                    FunctionExpression node) {
    HBasicBlock block = graph.addNewBlock();
    open(graph.entry);

    localsHandler.startFunction(functionElement, node);
    close(new HGoto()).addSuccessor(block);

    open(block);
  }

  HGraph closeFunction() {
    // TODO(kasperl): Make this goto an implicit return.
    if (!isAborted()) close(new HGoto()).addSuccessor(graph.exit);
    graph.finalize();
    return graph;
  }

  HBasicBlock addNewBlock() {
    HBasicBlock block = graph.addNewBlock();
    // If adding a new block during building of an expression, it is due to
    // conditional expressions or short-circuit logical operators.
    return block;
  }

  void open(HBasicBlock block) {
    block.open();
    current = block;
  }

  HBasicBlock close(HControlFlow end) {
    HBasicBlock result = current;
    current.close(end);
    current = null;
    return result;
  }

  void goto(HBasicBlock from, HBasicBlock to) {
    from.close(new HGoto());
    from.addSuccessor(to);
  }

  bool isAborted() {
    return current === null;
  }

  void add(HInstruction instruction) {
    current.add(instruction);
  }

  void push(HInstruction instruction) {
    add(instruction);
    stack.add(instruction);
  }

  HInstruction pop() {
    return stack.removeLast();
  }

  HBoolify popBoolified() {
    HBoolify boolified = new HBoolify(pop());
    add(boolified);
    return boolified;
  }

  void visit(Node node) {
    if (node !== null) node.accept(this);
  }

  visitBlock(Block node) {
    for (Link<Node> link = node.statements.nodes;
         !link.isEmpty();
         link = link.tail) {
      visit(link.head);
      if (isAborted()) {
        // The block has been aborted by a return or a throw.
        if (!stack.isEmpty()) compiler.cancel('non-empty instruction stack');
        return;
      }
    }
    assert(!current.isClosed());
    if (!stack.isEmpty()) compiler.cancel('non-empty instruction stack');
  }

  visitClassNode(ClassNode node) {
    unreachable();
  }

  visitExpressionStatement(ExpressionStatement node) {
    visit(node.expression);
    pop();
  }

  /**
   * Creates a new loop-header block. The previous [current] block
   * is closed with an [HGoto] and replaced by the newly created block.
   * Also notifies the locals handler that we're entering a loop.
   */
  void startLoop(Node node) {
    assert(!isAborted());
    HBasicBlock previousBlock = close(new HGoto());

    HBasicBlock loopEntry = graph.addNewLoopHeaderBlock();
    previousBlock.addSuccessor(loopEntry);
    open(loopEntry);

    localsHandler.startLoop(node, loopEntry);
  }

  /**
   * Ends the loop:
   * - creates a new block and adds it as successor to the [branchBlock].
   * - opens the new block (setting as [current]).
   * - notifies the locals handler that we're exiting a loop.
   */
  void endLoop(HBasicBlock loopEntry, HBasicBlock branchBlock) {
    HBasicBlock loopExitBlock = addNewBlock();
    assert(branchBlock.successors.length == 1);
    branchBlock.addSuccessor(loopExitBlock);
    open(loopExitBlock);
    localsHandler.endLoop(loopEntry);
  }

  // For while loops, initializer and update are null.
  visitLoop(Node loop, Node initializer, Expression condition, NodeList updates,
            Node body) {
    if ((condition !== null || updates !== null) &&
        localsHandler.isForLoopDeclaringBoxedLoopVariable(loop)) {
      compiler.unimplemented("SsaBuilder for loop with boxed loop variable",
                             node: loop);
    }

    assert(condition !== null && body !== null);
    // The initializer.
    if (initializer !== null) {
      visit(initializer);
      // We don't care about the value of the initialization.
      if (initializer.asExpression() !== null) pop();
    }
    assert(!isAborted());

    startLoop(loop);
    HBasicBlock conditionBlock = current;

    // The condition.
    visit(condition);
    HBasicBlock conditionExitBlock = close(new HLoopBranch(popBoolified()));

    LocalsHandler savedLocals = new LocalsHandler.from(localsHandler);

    // The body.
    HBasicBlock bodyBlock = addNewBlock();
    conditionExitBlock.addSuccessor(bodyBlock);
    open(bodyBlock);
    visit(body);
    if (isAborted()) {
      compiler.unimplemented("SsaBuilder for loop with aborting body",
                             node: body);
    }
    bodyBlock = close(new HGoto());

    // Update.
    // We create an update block, even when we are in a while loop. There the
    // update block is the jump-target for continue statements. We could avoid
    // the creation if there is no continue, but for now we always create it.
    HBasicBlock updateBlock = addNewBlock();
    bodyBlock.addSuccessor(updateBlock);
    open(updateBlock);
    if (updates !== null) {
      for (Expression expression in updates) {
        visit(expression);
        assert(!isAborted());
        // The result of the update instruction isn't used, and can just
        // be dropped.
        HInstruction updateInstruction = pop();
      }
    }
    updateBlock = close(new HGoto());
    // The back-edge completing the cycle.
    updateBlock.addSuccessor(conditionBlock);
    conditionBlock.postProcessLoopHeader();

    endLoop(conditionBlock, conditionExitBlock);
    localsHandler = savedLocals;
  }

  visitFor(For node) {
    if (node.condition === null) {
      compiler.unimplemented("SsaBuilder for loop without condition");
    }
    assert(node.body !== null);
    visitLoop(node, node.initializer, node.condition, node.update, node.body);
  }

  visitWhile(While node) {
    visitLoop(node, null, node.condition, null, node.body);
  }

  visitDoWhile(DoWhile node) {
    startLoop(node);
    HBasicBlock loopEntryBlock = current;

    visit(node.body);
    if (isAborted()) {
      compiler.unimplemented("SsaBuilder for loop with aborting body");
    }

    // If there are no continues we could avoid the creation of the condition
    // block. This could also lead to a block having multiple entries and exits.
    HBasicBlock bodyExitBlock = close(new HGoto());
    HBasicBlock conditionBlock = addNewBlock();
    bodyExitBlock.addSuccessor(conditionBlock);
    open(conditionBlock);
    visit(node.condition);
    assert(!isAborted());
    conditionBlock = close(new HLoopBranch(popBoolified()));

    conditionBlock.addSuccessor(loopEntryBlock);  // The back-edge.
    loopEntryBlock.postProcessLoopHeader();

    endLoop(loopEntryBlock, conditionBlock);
  }

  visitFunctionExpression(FunctionExpression node) {
    ClosureData nestedClosureData = closureDataCache[node];
    assert(nestedClosureData !== null);
    assert(nestedClosureData.closureClassElement !== null);
    ClassElement closureClassElement =
        nestedClosureData.closureClassElement;
    FunctionElement callElement = nestedClosureData.callElement;
    compiler.enqueue(new WorkItem.toCodegen(callElement, elements));
    compiler.registerInstantiatedClass(closureClassElement);
    assert(closureClassElement.members.isEmpty());

    List<HInstruction> capturedVariables = <HInstruction>[];
    for (Element member in closureClassElement.backendMembers) {
      // The backendMembers also contains the call method(s). We are only
      // interested in the fields.
      if (member.kind == ElementKind.FIELD) {
        Element capturedLocal = nestedClosureData.capturedFieldMapping[member];
        assert(capturedLocal != null);
        capturedVariables.add(localsHandler.readLocal(capturedLocal));
      }
    }

    push(new HForeignNew(closureClassElement, capturedVariables));
  }

  visitIdentifier(Identifier node) {
    if (node.isThis()) {
      stack.add(localsHandler.readThis());
    } else {
      compiler.internalError("SsaBuilder.visitIdentifier on non-this",
                             node: node);
    }
  }

  visitIf(If node) {
    visit(node.condition);
    Function visitElse;
    if (node.elsePart != null) {
      visitElse = () {
        visit(node.elsePart);
      };
    }
    handleIf(() => visit(node.thenPart), visitElse);
  }

  void handleIf(void visitThen(), void visitElse()) {
    bool hasElse = visitElse != null;
    HBasicBlock conditionBlock = close(new HIf(popBoolified(), hasElse));

    LocalsHandler savedLocals = new LocalsHandler.from(localsHandler);

    // The then part.
    HBasicBlock thenBlock = addNewBlock();
    conditionBlock.addSuccessor(thenBlock);
    open(thenBlock);
    visitThen();
    thenBlock = current;

    // Reset the locals state to the state after the condition and keep the
    // current state in [thenLocals].
    LocalsHandler thenLocals = localsHandler;
    localsHandler = savedLocals;

    // Now the else part.
    HBasicBlock elseBlock = null;
    if (hasElse) {
      elseBlock = addNewBlock();
      conditionBlock.addSuccessor(elseBlock);
      open(elseBlock);
      visitElse();
      elseBlock = current;
    }

    if (thenBlock === null && elseBlock === null && hasElse) {
      current = null;
    } else {
      HBasicBlock joinBlock = addNewBlock();
      if (thenBlock !== null) goto(thenBlock, joinBlock);
      if (elseBlock !== null) goto(elseBlock, joinBlock);
      else if (!hasElse) conditionBlock.addSuccessor(joinBlock);
      // If the join block has two predecessors we have to merge the
      // locals. The current locals is what either the
      // condition or the else block left us with, so we merge that
      // with the set of locals we got after visiting the then
      // part of the if.
      open(joinBlock);
      if (joinBlock.predecessors.length == 2) {
        localsHandler.mergeWith(thenLocals, joinBlock);
      }
    }
  }

  SourceString unquote(LiteralString literal, int start) {
    String str = '${literal.value}';
    int quotes = 1;
    String quote = str[start];
    while (str[quotes + start] === quote) quotes++;
    return new SourceString(str.substring(quotes + start, str.length - quotes));
  }

  void visitLogicalAndOr(Send node, Operator op) {
    // x && y is transformed into:
    //   t0 = boolify(x);
    //   if (t0) t1 = boolify(y);
    //   result = phi(t0, t1);
    //
    // x || y is transformed into:
    //   t0 = boolify(x);
    //   if (not(t0)) t1 = boolify(y);
    //   result = phi(t0, t1);
    bool isAnd = (const SourceString("&&") == op.source);

    visit(node.receiver);
    HInstruction boolifiedLeft = popBoolified();
    HInstruction condition;
    if (isAnd) {
      condition = boolifiedLeft;
    } else {
      condition = new HNot(boolifiedLeft);
      add(condition);
    }
    HBasicBlock leftBlock = close(new HIf(condition, false));
    LocalsHandler savedLocals = new LocalsHandler.from(localsHandler);

    HBasicBlock rightBlock = addNewBlock();
    leftBlock.addSuccessor(rightBlock);
    open(rightBlock);
    visit(node.argumentsNode);
    HInstruction boolifiedRight = popBoolified();
    rightBlock = close(new HGoto());

    HBasicBlock joinBlock = addNewBlock();
    leftBlock.addSuccessor(joinBlock);
    rightBlock.addSuccessor(joinBlock);
    open(joinBlock);

    localsHandler.mergeWith(savedLocals, joinBlock);
    HPhi result = new HPhi.manyInputs(null, [boolifiedLeft, boolifiedRight]);
    joinBlock.addPhi(result);
    stack.add(result);
  }

  void visitLogicalNot(Send node) {
    assert(node.argumentsNode is Prefix);
    visit(node.receiver);
    HNot not = new HNot(popBoolified());
    push(not);
  }

  void visitUnary(Send node, Operator op) {
    assert(node.argumentsNode is Prefix);
    visit(node.receiver);
    assert(op.token.kind !== PLUS_TOKEN);
    HInstruction operand = pop();
    HInstruction target =
        new HStatic(interceptors.getPrefixOperatorInterceptor(op));
    add(target);
    switch (op.source.stringValue) {
      case "-": push(new HNegate(target, operand)); break;
      case "~": push(new HBitNot(target, operand)); break;
      default: unreachable();
    }
  }

  void visitBinary(HInstruction left, Operator op, HInstruction right) {
    Element element = interceptors.getOperatorInterceptor(op);
    assert(element != null);
    HInstruction target = new HStatic(element);
    add(target);
    switch (op.source.stringValue) {
      case "+":
      case "++":
      case "+=":
        push(new HAdd(target, left, right));
        break;
      case "-":
      case "--":
      case "-=":
        push(new HSubtract(target, left, right));
        break;
      case "*":
      case "*=":
        push(new HMultiply(target, left, right));
        break;
      case "/":
      case "/=":
        push(new HDivide(target, left, right));
        break;
      case "~/":
      case "~/=":
        push(new HTruncatingDivide(target, left, right));
        break;
      case "%":
      case "%=":
        push(new HModulo(target, left, right));
        break;
      case "<<":
      case "<<=":
        push(new HShiftLeft(target, left, right));
        break;
      case ">>":
      case ">>=":
        push(new HShiftRight(target, left, right));
        break;
      case "|":
      case "|=":
        push(new HBitOr(target, left, right));
        break;
      case "&":
      case "&=":
        push(new HBitAnd(target, left, right));
        break;
      case "^":
      case "^=":
        push(new HBitXor(target, left, right));
        break;
      case "==":
        push(new HEquals(target, left, right));
        break;
      case "===":
        push(new HIdentity(target, left, right));
        break;
      case "!==":
        HIdentity eq = new HIdentity(target, left, right);
        add(eq);
        push(new HNot(eq));
        break;
      case "<":
        push(new HLess(target, left, right));
        break;
      case "<=":
        push(new HLessEqual(target, left, right));
        break;
      case ">":
        push(new HGreater(target, left, right));
        break;
      case ">=":
        push(new HGreaterEqual(target, left, right));
        break;
      case "!=":
        HEquals eq = new HEquals(target, left, right);
        add(eq);
        HBoolify bl = new HBoolify(eq);
        add(bl);
        push(new HNot(bl));
        break;
      default: compiler.unimplemented("SsaBuilder.visitBinary");
    }
  }

  void generateGetter(Send send, Element element) {
    Selector selector = elements.getSelector(send);
    if (Elements.isStaticOrTopLevelField(element)) {
      push(new HStatic(element));
      if (element.kind == ElementKind.GETTER) {
        push(new HInvokeStatic(selector, <HInstruction>[pop()]));
      }
    } else if (element === null || Elements.isInstanceField(element)) {
      HInstruction receiver;
      if (send.receiver == null) {
        receiver = localsHandler.readThis();
      } else {
        visit(send.receiver);
        receiver = pop();
      }
      SourceString getterName = send.selector.asIdentifier().source;
      Element staticInterceptor = null;
      if (methodInterceptionEnabled) {
        staticInterceptor = interceptors.getStaticGetInterceptor(getterName);
      }
      if (staticInterceptor != null) {
        HStatic target = new HStatic(staticInterceptor);
        add(target);
        List<HInstruction> inputs = <HInstruction>[target, receiver];
        push(new HInvokeInterceptor(selector, getterName, true, inputs));
      } else {
        push(new HInvokeDynamicGetter(selector, null, getterName, receiver));
      }
    } else if (Elements.isStaticOrTopLevelFunction(element)) {
      push(new HStatic(element));
      compiler.registerGetOfStaticFunction(element);
    } else {
      stack.add(localsHandler.readLocal(element));
    }
  }

  void generateSetter(SendSet send, Element element, HInstruction value) {
    Selector selector = elements.getSelector(send);
    if (Elements.isStaticOrTopLevelField(element)) {
      if (element.kind == ElementKind.SETTER) {
        HStatic target = new HStatic(element);
        add(target);
        add(new HInvokeStatic(selector, <HInstruction>[target, value]));
      } else {
        add(new HStaticStore(element, value));
      }
      stack.add(value);
    } else if (element === null || Elements.isInstanceField(element)) {
      SourceString dartSetterName = send.selector.asIdentifier().source;
      HInstruction receiver;
      if (send.receiver == null) {
        receiver = localsHandler.readThis();
      } else {
        visit(send.receiver);
        receiver = pop();
      }
      add(new HInvokeDynamicSetter(
          selector, null, dartSetterName, receiver, value));
      stack.add(value);
    } else {
      localsHandler.updateLocal(element, value);
      stack.add(value);
    }
  }

  visitOperatorSend(node) {
    assert(node.selector is Operator);
    Operator op = node.selector;
    if (const SourceString("[]") == op.source) {
      HStatic target = new HStatic(interceptors.getIndexInterceptor());
      add(target);
      visit(node.receiver);
      HInstruction receiver = pop();
      visit(node.argumentsNode);
      HInstruction index = pop();
      push(new HIndex(target, receiver, index));
    } else if (const SourceString("&&") == op.source ||
               const SourceString("||") == op.source) {
      visitLogicalAndOr(node, op);
    } else if (const SourceString("!") == op.source) {
      visitLogicalNot(node);
    } else if (node.argumentsNode is Prefix) {
      visitUnary(node, op);
    } else if (const SourceString("is") == op.source) {
      visit(node.receiver);
      HInstruction expression = pop();
      push(new HIs(elements[node.arguments.head], expression));
    } else {
      visit(node.receiver);
      visit(node.argumentsNode);
      var right = pop();
      var left = pop();
      visitBinary(left, op, right);
    }
  }

  void addDynamicSendArgumentsToList(Send node, List<HInstruction> list) {
    Selector selector = elements.getSelector(node);
    if (selector.namedArgumentCount == 0) {
      addGenericSendArgumentsToList(node.arguments, list);
    } else {
      // Visit positional arguments and add them to the list.
      Link<Node> arguments = node.arguments;
      int positionalArgumentCount = selector.positionalArgumentCount;
      for (int i = 0;
           i < positionalArgumentCount;
           arguments = arguments.tail, i++) {
        visit(arguments.head);
        list.add(pop());
      }

      // Visit named arguments and add them into a temporary map.
      Map<SourceString, HInstruction> instructions =
          new Map<SourceString, HInstruction>();
      List<SourceString> namedArguments = selector.namedArguments;
      int nameIndex = 0;
      for (; !arguments.isEmpty(); arguments = arguments.tail) {
        visit(arguments.head);
        instructions[namedArguments[nameIndex++]] = pop();
      }

      // Iterate through the named arguments to add them to the list
      // of instructions, in an order that can be shared with
      // selectors with the same named arguments.
      List<SourceString> orderedNames = selector.getOrderedNamedArguments();
      for (SourceString name in orderedNames) {
        list.add(instructions[name]);
      }
    }
  }

  void addStaticSendArgumentsToList(Send node,
                                    FunctionElement element,
                                    List<HInstruction> list) {
    Selector selector = elements.getSelector(node);
    FunctionParameters parameters = element.computeParameters(compiler);
    if (!selector.applies(compiler, element)) {
      // TODO(ngeoffray): Match the VM behavior and throw an
      // exception at runtime.
      compiler.cancel('Unimplemented non-matching static call', node: node);
    } else if (selector.positionalArgumentCount == parameters.parameterCount) {
      addGenericSendArgumentsToList(node.arguments, list);
    } else {
      // If there are named arguments, provide them in the order
      // expected by the called function, which is the source order.

      // Visit positional arguments and add them to the list.
      Link<Node> arguments = node.arguments;
      int positionalArgumentCount = selector.positionalArgumentCount;
      for (int i = 0;
           i < positionalArgumentCount;
           arguments = arguments.tail, i++) {
        visit(arguments.head);
        list.add(pop());
      }

      // Visit named arguments and add them into a temporary list.
      List<HInstruction> namedArguments = <HInstruction>[];
      for (; !arguments.isEmpty(); arguments = arguments.tail) {
        visit(arguments.head);
        namedArguments.add(pop());
      }

      Link<Element> remainingNamedParameters = parameters.optionalParameters;
      // Skip the optional parameters that have been given in the
      // positional arguments.
      for (int i = parameters.requiredParameterCount;
           i < positionalArgumentCount;
           i++) {
        remainingNamedParameters = remainingNamedParameters.tail;
      }

      // Loop over the remaining named parameters, and try to find
      // their values: either in the temporary list or using the
      // default value.
      for (;
           !remainingNamedParameters.isEmpty();
           remainingNamedParameters = remainingNamedParameters.tail) {
        Element parameter = remainingNamedParameters.head;
        int foundIndex = -1;
        for (int i = 0; i < selector.namedArguments.length; i++) {
          SourceString name = selector.namedArguments[i];
          if (name == parameter.name) {
            foundIndex = i;
            break;
          }
        }
        if (foundIndex != -1) {
          list.add(namedArguments[foundIndex]);
        } else {
          push(new HLiteral(
              compiler.compileVariable(parameter), HType.UNKNOWN));
          list.add(pop());
        }
      }
    }
  }

  void addGenericSendArgumentsToList(Link<Node> link, List<HInstruction> list) {
    for (; !link.isEmpty(); link = link.tail) {
      visit(link.head);
      list.add(pop());
    }
  }

  visitDynamicSend(Send node) {
    Selector selector = elements.getSelector(node);
    var inputs = <HInstruction>[];

    SourceString dartMethodName;
    bool isNotEquals = false;
    if (node.isIndex && !node.arguments.tail.isEmpty()) {
      dartMethodName = Elements.constructOperatorName(
          const SourceString('operator'),
          const SourceString('[]='));
    } else if (node.selector.asOperator() != null) {
      SourceString name = node.selector.asIdentifier().source;
      isNotEquals = name.stringValue === '!=';
      dartMethodName = Elements.constructOperatorName(
          const SourceString('operator'),
          name,
          node.argumentsNode is Prefix);
    } else {
      dartMethodName = node.selector.asIdentifier().source;
    }

    Element interceptor = null;
    if (methodInterceptionEnabled) {
      interceptor = interceptors.getStaticInterceptor(dartMethodName,
                                                      node.argumentCount());
    }
    if (interceptor != null) {
      HStatic target = new HStatic(interceptor);
      add(target);
      inputs.add(target);
      visit(node.receiver);
      inputs.add(pop());
      addGenericSendArgumentsToList(node.arguments, inputs);
      push(new HInvokeInterceptor(selector, dartMethodName, false, inputs));
      return;
    }

    if (node.receiver === null) {
      inputs.add(localsHandler.readThis());
    } else {
      visit(node.receiver);
      inputs.add(pop());
    }

    addDynamicSendArgumentsToList(node, inputs);

    // The first entry in the inputs list is the receiver.
    push(new HInvokeDynamicMethod(selector, dartMethodName, inputs));

    if (isNotEquals) {
      HNot not = new HNot(popBoolified());
      push(not);
    }
  }

  visitClosureSend(Send node) {
    Selector selector = elements.getSelector(node);
    assert(node.receiver === null);
    Element element = elements[node];
    HInstruction closureTarget;
    if (element === null) {
      visit(node.selector);
      closureTarget = pop();
    } else {
      assert(Elements.isLocal(element));
      closureTarget = localsHandler.readLocal(element);
    }
    var inputs = <HInstruction>[];
    inputs.add(closureTarget);
    addDynamicSendArgumentsToList(node, inputs);
    push(new HInvokeClosure(selector, inputs));
  }

  visitForeignSend(Send node) {
    Identifier selector = node.selector;
    switch (selector.source.stringValue) {
      case "JS":
        Link<Node> link = node.arguments;
        // If the invoke is on foreign code, don't visit the first
        // argument, which is the type, and the second argument,
        // which is the foreign code.
        link = link.tail.tail;
        List<HInstruction> inputs = <HInstruction>[];
        addGenericSendArgumentsToList(link, inputs);
        LiteralString type = node.arguments.head;
        LiteralString literal = node.arguments.tail.head;
        compiler.ensure(literal is LiteralString);
        compiler.ensure(type is LiteralString);
        compiler.ensure(literal.value.stringValue[0] == '@');
        push(new HForeign(unquote(literal, 1), unquote(type, 0), inputs));
        break;
      case "UNINTERCEPTED":
        Link<Node> link = node.arguments;
        if (!link.tail.isEmpty()) {
          compiler.cancel('More than one expression in UNINTERCEPTED()');
        }
        Expression expression = link.head;
        disableMethodInterception();
        visit(expression);
        enableMethodInterception();
        break;
      case "JS_HAS_EQUALS":
        List<HInstruction> inputs = <HInstruction>[];
        if (!node.arguments.tail.isEmpty()) {
          compiler.cancel('More than one expression in JS_HAS_EQUALS()');
        }
        addGenericSendArgumentsToList(node.arguments, inputs);
        String name = compiler.namer.instanceMethodName(
            Namer.OPERATOR_EQUALS, 1);
        push(new HForeign(
            new SourceString('\$0.$name'), const SourceString('bool'), inputs));
        break;
      default:
        throw "Unknown foreign: ${node.selector}";
    }
  }

  visitSuperSend(Send node) {
    Selector selector = elements.getSelector(node);
    Element element = elements[node];
    HStatic target = new HStatic(element);
    HInstruction context = localsHandler.readThis();
    add(target);
    var inputs = <HInstruction>[target, context];
    addStaticSendArgumentsToList(node, element, inputs);
    push(new HInvokeSuper(selector, inputs));
  }

  visitStaticSend(Send node) {
    Selector selector = elements.getSelector(node);
    Element element = elements[node];
    if (element.kind != ElementKind.FUNCTION &&
        element.kind != ElementKind.GENERATIVE_CONSTRUCTOR) {
      compiler.unimplemented(
          "SsaBuilder.visitStaticSend static field invocation",
          node: node);
    }
    HStatic target = new HStatic(element);
    add(target);
    var inputs = <HInstruction>[];
    inputs.add(target);
    addStaticSendArgumentsToList(node, element, inputs);
    push(new HInvokeStatic(selector, inputs));
  }

  visitSend(Send node) {
    if (node.selector is Operator && methodInterceptionEnabled) {
      visitOperatorSend(node);
    } else if (node.isPropertyAccess) {
      generateGetter(node, elements[node]);
    } else if (Elements.isClosureSend(node, elements)) {
      visitClosureSend(node);
    } else if (node.isSuperCall) {
      visitSuperSend(node);
    } else {
      Element element = elements[node];
      if (element === null) {
        // Example: f() with 'f' unbound.
        // This can only happen inside an instance method.
        visitDynamicSend(node);
      } else if (element.kind == ElementKind.CLASS) {
        compiler.internalError("Cannot generate code for send", node: node);
      } else if (element.isInstanceMember()) {
        // Example: f() with 'f' bound to instance method.
        visitDynamicSend(node);
      } else if (element.kind === ElementKind.FOREIGN) {
        visitForeignSend(node);
      } else if (!element.isInstanceMember()) {
        // Example: A.f() or f() with 'f' bound to a static function.
        // Also includes new A() or new A.named() which is treated like a
        // static call to a factory.
        visitStaticSend(node);
      } else {
        compiler.internalError("Cannot generate code for send", node: node);
      }
    }
  }

  visitNewExpression(NewExpression node) => visitSend(node.send);

  visitSendSet(SendSet node) {
    Operator op = node.assignmentOperator;
    if (node.isIndex) {
      if (!methodInterceptionEnabled) {
        assert(op.source.stringValue === '=');
        visitDynamicSend(node);
      } else {
        HStatic target = new HStatic(
            interceptors.getIndexAssignmentInterceptor());
        add(target);
        visit(node.receiver);
        HInstruction receiver = pop();
        visit(node.argumentsNode);
        if (const SourceString("=") == op.source) {
          HInstruction value = pop();
          HInstruction index = pop();
          push(new HIndexAssign(target, receiver, index, value));
        } else {
          HInstruction value;
          HInstruction index;
          bool isCompoundAssignment = op.source.stringValue.endsWith('=');
          // Compound assignments are considered as being prefix.
          bool isPrefix = !node.isPostfix;
          Element getter = elements[node.selector];
          if (isCompoundAssignment) {
            value = pop();
            index = pop();
          } else {
            index = pop();
            value = new HLiteral(1, HType.INTEGER);
            add(value);
          }
          HStatic indexMethod = new HStatic(interceptors.getIndexInterceptor());
          add(indexMethod);
          HInstruction left = new HIndex(indexMethod, receiver, index);
          add(left);
          Element opElement = elements[op];
          visitBinary(left, op, value);
          HInstruction assign = new HIndexAssign(
              target, receiver, index, pop());
          add(assign);
          if (isPrefix) {
            stack.add(assign);
          } else {
            stack.add(left);
          }
        }
      }
    } else if (const SourceString("=") == op.source) {
      Element element = elements[node];
      Link<Node> link = node.arguments;
      assert(!link.isEmpty() && link.tail.isEmpty());
      visit(link.head);
      HInstruction value = pop();
      generateSetter(node, element, value);
    } else if (op.source.stringValue === "is") {
      compiler.internalError("is-operator as SendSet", node: op);
    } else {
      assert(const SourceString("++") == op.source ||
             const SourceString("--") == op.source ||
             node.assignmentOperator.source.stringValue.endsWith("="));
      Element element = elements[node];
      bool isCompoundAssignment = !node.arguments.isEmpty();
      bool isPrefix = !node.isPostfix;  // Compound assignments are prefix.
      generateGetter(node, elements[node.selector]);
      HInstruction left = pop();
      HInstruction right;
      if (isCompoundAssignment) {
        visit(node.argumentsNode);
        right = pop();
      } else {
        right = new HLiteral(1, HType.INTEGER);
        add(right);
      }
      visitBinary(left, op, right);
      HInstruction operation = pop();
      assert(operation !== null);
      generateSetter(node, element, operation);
      if (!isPrefix) {
        pop();
        stack.add(left);
      }
    }
  }

  void visitLiteralInt(LiteralInt node) {
    push(new HLiteral(node.value, HType.INTEGER));
  }

  void visitLiteralDouble(LiteralDouble node) {
    push(new HLiteral(node.value, HType.DOUBLE));
  }

  void visitLiteralBool(LiteralBool node) {
    push(new HLiteral(node.value, HType.BOOLEAN));
  }

  void visitLiteralString(LiteralString node) {
    push(new HLiteral(node.dartString, HType.STRING));
  }

  void visitLiteralNull(LiteralNull node) {
    push(new HLiteral(null, HType.UNKNOWN));
  }

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      visit(link.head);
    }
  }

  void visitParenthesizedExpression(ParenthesizedExpression node) {
    visit(node.expression);
  }

  visitOperator(Operator node) {
    // Operators are intercepted in their surrounding Send nodes.
    unreachable();
  }

  visitReturn(Return node) {
    HInstruction value;
    if (node.expression === null) {
      value = new HLiteral(null, HType.UNKNOWN);
      add(value);
    } else {
      visit(node.expression);
      value = pop();
    }
    close(new HReturn(value)).addSuccessor(graph.exit);
  }

  visitThrow(Throw node) {
    if (node.expression === null) {
      compiler.unimplemented("SsaBuilder: throw without expression");
    }
    visit(node.expression);
    close(new HThrow(pop()));
  }

  visitTypeAnnotation(TypeAnnotation node) {
    compiler.internalError('visiting type annotation in SSA builder',
                           node: node);
  }

  visitVariableDefinitions(VariableDefinitions node) {
    for (Link<Node> link = node.definitions.nodes;
         !link.isEmpty();
         link = link.tail) {
      Node definition = link.head;
      if (definition is Identifier) {
        HInstruction initialValue = new HLiteral(null, HType.UNKNOWN);
        add(initialValue);
        localsHandler.updateLocal(elements[definition], initialValue);
      } else {
        assert(definition is SendSet);
        visitSendSet(definition);
        pop();  // Discard value.
      }
    }
  }

  visitLiteralList(LiteralList node) {
    List<HInstruction> inputs = <HInstruction>[];
    for (Link<Node> link = node.elements.nodes;
         !link.isEmpty();
         link = link.tail) {
      visit(link.head);
      inputs.add(pop());
    }
    push(new HLiteralList(inputs));
  }

  visitConditional(Conditional node) {
    visit(node.condition);
    HBasicBlock conditionBlock = close(new HIf(popBoolified(), true));
    LocalsHandler savedLocals = new LocalsHandler.from(localsHandler);

    HBasicBlock thenBlock = addNewBlock();
    conditionBlock.addSuccessor(thenBlock);
    open(thenBlock);
    visit(node.thenExpression);
    HInstruction thenInstruction = pop();
    thenBlock = close(new HGoto());
    LocalsHandler thenLocals = localsHandler;
    localsHandler = savedLocals;

    HBasicBlock elseBlock = addNewBlock();
    conditionBlock.addSuccessor(elseBlock);
    open(elseBlock);
    visit(node.elseExpression);
    HInstruction elseInstruction = pop();
    elseBlock = close(new HGoto());

    HBasicBlock joinBlock = addNewBlock();
    thenBlock.addSuccessor(joinBlock);
    elseBlock.addSuccessor(joinBlock);
    open(joinBlock);

    localsHandler.mergeWith(thenLocals, joinBlock);
    HPhi phi = new HPhi.manyInputs(null, [thenInstruction, elseInstruction]);
    joinBlock.addPhi(phi);
    stack.add(phi);
  }

  visitStringInterpolation(StringInterpolation node) {
    Operator op = new Operator.synthetic("+");
    HInstruction target = new HStatic(interceptors.getOperatorInterceptor(op));
    add(target);
    visit(node.string);
    // Handle the parts here, to avoid recreating [target].
    for (StringInterpolationPart part in node.parts) {
      HInstruction prefix = pop();
      visit(part.expression);
      push(new HAdd(target, prefix, pop()));
      prefix = pop();
      visit(part.string);
      push(new HAdd(target, prefix, pop()));
    }
  }

  visitStringInterpolationPart(StringInterpolationPart node) {
    // The parts are iterated in visitStringInterpolation.
    unreachable();
  }

  visitEmptyStatement(EmptyStatement node) {
    compiler.unimplemented('SsaBuilder.visitEmptyStatement',
                           node: node);
  }

  visitModifiers(Modifiers node) {
    compiler.unimplemented('SsaBuilder.visitModifiers', node: node);
  }

  visitBreakStatement(BreakStatement node) {
    compiler.unimplemented('SsaBuilder.visitBreakStatement', node: node);
  }

  visitContinueStatement(ContinueStatement node) {
    compiler.unimplemented('SsaBuilder.visitContinueStatement', node: node);
  }

  visitForInStatement(ForInStatement node) {
    // Generate a structure equivalent to:
    //   Iterator<E> $iter = <iterable>.iterator()
    //   while ($iter.hasNext()) {
    //     E <declaredIdentifier> = $iter.next();
    //     <body>
    //   }
    SourceString iteratorName = const SourceString("iterator");

    Selector selector = Selector.INVOCATION_0;
    Element interceptor = interceptors.getStaticInterceptor(iteratorName, 0);
    assert(interceptor != null);
    HStatic target = new HStatic(interceptor);
    add(target);
    visit(node.expression);
    List<HInstruction> inputs = <HInstruction>[target, pop()];
    HInstruction iterator = new HInvokeInterceptor(
        selector, iteratorName, false, inputs);
    add(iterator);

    startLoop(node);
    HBasicBlock conditionBlock = current;

    // The condition.
    push(new HInvokeDynamicMethod(
        selector, const SourceString('hasNext'), [iterator]));
    HBasicBlock conditionExitBlock = close(new HLoopBranch(popBoolified()));

    LocalsHandler savedLocals = new LocalsHandler.from(localsHandler);

    // The body.
    HBasicBlock bodyBlock = addNewBlock();
    conditionExitBlock.addSuccessor(bodyBlock);
    open(bodyBlock);

    push(new HInvokeDynamicMethod(
        selector, const SourceString('next'), [iterator]));

    Element variable;
    if (node.declaredIdentifier.asSend() !== null) {
      variable = elements[node.declaredIdentifier];
    } else {
      assert(node.declaredIdentifier.asVariableDefinitions() !== null);
      VariableDefinitions variableDefinitions = node.declaredIdentifier;
      variable = elements[variableDefinitions.definitions.nodes.head];
    }
    localsHandler.updateLocal(variable, pop());

    visit(node.body);
    if (isAborted()) {
      compiler.unimplemented("SsaBuilder for loop with aborting body",
                             node: node);
    }
    bodyBlock = close(new HGoto());

    // Update.
    // We create an update block, even if we are in a for-in loop. The
    // update block is the jump-target for continue statements. We could avoid
    // the creation if there is no continue, but for now we always create it.
    HBasicBlock updateBlock = addNewBlock();
    bodyBlock.addSuccessor(updateBlock);
    open(updateBlock);
    updateBlock = close(new HGoto());
    // The back-edge completing the cycle.
    updateBlock.addSuccessor(conditionBlock);
    conditionBlock.postProcessLoopHeader();

    endLoop(conditionBlock, conditionExitBlock);
    localsHandler = savedLocals;
  }

  visitLabelledStatement(LabelledStatement node) {
    compiler.unimplemented('SsaBuilder.visitLabelledStatement', node: node);
  }

  visitLiteralMap(LiteralMap node) {
    compiler.unimplemented('SsaBuilder.visitLiteralMap', node: node);
  }

  visitLiteralMapEntry(LiteralMapEntry node) {
    compiler.unimplemented('SsaBuilder.visitLiteralMapEntry', node: node);
  }

  visitNamedArgument(NamedArgument node) {
    visit(node.expression);
  }

  visitSwitchStatement(SwitchStatement node) {
    compiler.unimplemented('SsaBuilder.visitSwitchStatement', node: node);
  }

  visitTryStatement(TryStatement node) {
    work.allowSpeculativeOptimization = false;
    assert(!work.isBailoutVersion());
    HBasicBlock enterBlock = graph.addNewBlock();
    close(new HGoto()).addSuccessor(enterBlock);
    open(enterBlock);
    HTry tryInstruction = new HTry();
    close(tryInstruction);

    HBasicBlock tryBody = graph.addNewBlock();
    enterBlock.addSuccessor(tryBody);
    open(tryBody);
    visit(node.tryBlock);
    List<HBasicBlock> blocks = <HBasicBlock>[];
    if (!isAborted()) blocks.add(close(new HGoto()));

    if (!node.catchBlocks.isEmpty()) {
      HBasicBlock block = graph.addNewBlock();
      enterBlock.addSuccessor(block);
      open(block);
      // Note that the name of this element is irrelevant.
      Element element = new Element(
          const SourceString('exception'), ElementKind.PARAMETER, work.element);
      HParameterValue exception = new HParameterValue(element);
      add(exception);
      tryInstruction.exception = exception;
      Link<Node> link = node.catchBlocks.nodes;

      void pushCondition(CatchBlock catchBlock) {
        VariableDefinitions declaration = catchBlock.formals.nodes.head;
        HInstruction condition = null;
        if (declaration.type == null) {
          condition = new HLiteral(true, HType.BOOLEAN);
        } else {
          Element element = elements[declaration.type];
          if (element == null) {
            compiler.cancel('Catch with unresolved type', node: catchBlock);
          }
          condition = new HIs(element, exception);
        }
        push(condition);
      }

      void visitThen() {
        CatchBlock catchBlock = link.head;
        link = link.tail;
        VariableDefinitions declaration = catchBlock.formals.nodes.head;
        localsHandler.updateLocal(elements[declaration.definitions.nodes.head],
                                  exception);
        visit(catchBlock);
      }

      void visitElse() {
        if (link.isEmpty()) {
          close(new HThrow(exception));
        } else {
          CatchBlock newBlock = link.head;
          pushCondition(newBlock);
          handleIf(visitThen, visitElse);
        }
      }

      CatchBlock firstBlock = link.head;
      pushCondition(firstBlock);
      handleIf(visitThen, visitElse);
      if (!isAborted()) blocks.add(close(new HGoto()));
    }

    if (node.finallyBlock != null) {
      HBasicBlock finallyBlock = graph.addNewBlock();
      enterBlock.addSuccessor(finallyBlock);
      open(finallyBlock);
      visit(node.finallyBlock);
      if (!isAborted()) blocks.add(close(new HGoto()));
      tryInstruction.finallyBlock = finallyBlock;
    }

    // If no block is going to an exit block, the Dart code after the
    // try is dead code.
    if (!blocks.isEmpty()) {
      HBasicBlock exitBlock = graph.addNewBlock();

      for (HBasicBlock block in blocks) {
        block.addSuccessor(exitBlock);
      }

      open(exitBlock);
    }
  }

  visitScriptTag(ScriptTag node) {
    compiler.unimplemented('SsaBuilder.visitScriptTag', node: node);
  }

  visitCatchBlock(CatchBlock node) {
    visit(node.block);
  }

  visitTypedef(Typedef node) {
    compiler.unimplemented('SsaBuilder.visitTypedef', node: node);
  }
}
