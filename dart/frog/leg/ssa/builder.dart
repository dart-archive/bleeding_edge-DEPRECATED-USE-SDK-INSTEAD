// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaBuilderTask extends CompilerTask {
  SsaBuilderTask(Compiler compiler) : super(compiler);
  String get name() => 'SSA builder';

  HGraph build(Node tree, Map<Node, Element> elements) {
    return measure(() {
      FunctionExpression function = tree;
      HGraph graph =
          compileMethod(function.parameters, function.body, elements);
      assert(graph.isValid());
      if (GENERATE_SSA_TRACE) {
        Identifier name = function.name;
        new HTracer.singleton().traceCompilation(name.source.toString());
        new HTracer.singleton().traceGraph('builder', graph);
      }
      return graph;
    });
  }

  HGraph compileMethod(NodeList parameters,
                       Node body, Map<Node,
                       Element> elements) {
    SsaBuilder builder = new SsaBuilder(compiler, elements);
    HGraph graph = builder.build(parameters, body);
    return graph;
  }
}

class SsaBuilder implements Visitor {
  final Compiler compiler;
  final Map<Node, Element> elements;
  HGraph graph;

  // We build the Ssa graph by simulating a stack machine.
  List<HInstruction> stack;

  Map<Element, HInstruction> definitions;

  // The current block to add instructions to. Might be null, if we are
  // visiting dead code.
  HBasicBlock current;

  SsaBuilder(this.compiler, this.elements);

  HGraph build(NodeList parameters, Node body) {
    stack = new List<HInstruction>();
    definitions = new Map<Element, HInstruction>();

    graph = new HGraph();
    HBasicBlock block = graph.addNewBlock();

    open(graph.entry);
    visitParameters(parameters);
    close(new HGoto()).addSuccessor(block);

    open(block);
    body.accept(this);

    // TODO(kasperl): Make this goto an implicit return.
    if (!isAborted()) close(new HGoto()).addSuccessor(graph.exit);
    graph.finalize();
    return graph;
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

  visitParameters(NodeList parameters) {
    int parameterIndex = 0;
    for (Link<Node> link = parameters.nodes;
         !link.isEmpty();
         link = link.tail) {
      VariableDefinitions container = link.head;
      Link<Node> identifierLink = container.definitions.nodes;
      // The identifier link must contain exactly one argument.
      assert(!identifierLink.isEmpty() && identifierLink.tail.isEmpty());
      if (identifierLink.head is !Identifier) {
        compiler.unimplemented("SsaBuilder.visitParameters non-identifier");
      }
      Identifier parameterId = identifierLink.head;
      Element element = elements[parameterId];
      HParameter parameterInstruction = new HParameter(parameterIndex++);
      definitions[element] = parameterInstruction;
      add(parameterInstruction);
    }
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
    compiler.unimplemented("SsaBuilder.visitClassNode");
  }

  visitExpressionStatement(ExpressionStatement node) {
    visit(node.expression);
    pop();
  }

  visitFor(For node) {
    assert(node.initializer !== null && node.condition !== null &&
           node.update !== null && node.body !== null);
    // The initializer.
    visit(node.initializer);
    assert(!isAborted());
    HBasicBlock initializerBlock = close(new HGoto());

    Map initializerDefinitions =
        new Map<Element, HInstruction>.from(definitions);

    // The condition.
    HBasicBlock conditionBlock = graph.addNewBlock();
    conditionBlock.isLoopHeader = true;
    initializerBlock.addSuccessor(conditionBlock);
    open(conditionBlock);

    // Create phis for all elements in the definitions environment.
    initializerDefinitions.forEach((Element element, HInstruction instruction) {
      HPhi phi = new HPhi.singleInput(element, instruction);
      conditionBlock.addPhi(phi);
      definitions[element] = phi;
    });

    visit(node.condition.expression);
    HBasicBlock conditionExitBlock = close(new HLoopBranch(popBoolified()));

    Map conditionDefinitions = new Map<Element, HInstruction>.from(definitions);

    // The body.
    HBasicBlock bodyBlock = graph.addNewBlock();
    conditionExitBlock.addSuccessor(bodyBlock);
    open(bodyBlock);
    visit(node.body);
    if (isAborted()) {
      compiler.unimplemented("SsaBuilder for loop with aborting body");
    }
    bodyBlock = close(new HGoto());

    // Update.
    HBasicBlock updateBlock = graph.addNewBlock();
    bodyBlock.addSuccessor(updateBlock);
    open(updateBlock);
    visit(node.update);
    assert(!isAborted());
    // The update instruction can just be popped. It is not used.
    HInstruction updateInstruction = pop();
    updateBlock = close(new HGoto());
    // The back-edge completing the cycle.
    updateBlock.addSuccessor(conditionBlock);

    // Update the phis if necessary, or delete them otherwise.
    conditionBlock.forEachPhi((HPhi phi) {
      Element element = phi.element;
      HInstruction postBodyDefinition = definitions[element];
      if (postBodyDefinition !== phi) {
        // Add the post body definition as input to the phi.
        phi.addInput(postBodyDefinition);
      } else {
        // The variable survived without modifications. Replace the
        // phi with its only input.
        assert(phi.inputs.length == 1);
        HInstruction input = phi.inputs[0];
        conditionBlock.rewrite(phi, input);  // Covers all basic blocks.
        conditionBlock.removePhi(phi);
        // Unless the condition introduces a different definition for
        // the element (later restored by the loop body), we have to
        // update the definitions map for the loop exit block to use
        // the definition we've rewritten to.
        if (conditionDefinitions[element] === phi) {
          conditionDefinitions[element] = input;
        }
      }
    });

    HBasicBlock loopExitBlock = graph.addNewBlock();
    conditionExitBlock.addSuccessor(loopExitBlock);
    open(loopExitBlock);
    definitions = conditionDefinitions;
  }

  visitFunctionExpression(FunctionExpression node) {
    compiler.unimplemented('SsaBuilder.visitFunctionExpression');
  }

  visitIdentifier(Identifier node) {
    Element element = elements[node];
    compiler.ensure(element !== null);
    HInstruction def = definitions[element];
    assert(def !== null);
    stack.add(def);
  }

  Map<Element, HInstruction> joinDefinitions(
      HBasicBlock joinBlock,
      Map<Element, HInstruction> incoming1,
      Map<Element, HInstruction> incoming2) {
    // If an element is in one map but not the other we can safely
    // ignore it. It means that a variable was declared in the
    // block. Since variable declarations are scoped the declared
    // variable cannot be alive outside the block. Note: this is only
    // true for nodes where we do joins.
    Map<Element, HInstruction> joinedDefinitions =
        new Map<Element, HInstruction>();
    incoming1.forEach((element, instruction) {
      HInstruction other = incoming2[element];
      if (other === null) return;
      if (instruction === other) {
        joinedDefinitions[element] = instruction;
      } else {
        HInstruction phi = new HPhi.manyInputs(element, [instruction, other]);
        joinBlock.addPhi(phi);
        joinedDefinitions[element] = phi;
      }
    });
    return joinedDefinitions;
  }

  visitIf(If node) {
    // Add the condition to the current block.
    bool hasElse = node.hasElsePart;
    visit(node.condition);
    HBasicBlock conditionBlock = close(new HIf(popBoolified(), hasElse));

    Map conditionDefinitions =
        new Map<Element, HInstruction>.from(definitions);

    // The then part.
    HBasicBlock thenBlock = graph.addNewBlock();
    conditionBlock.addSuccessor(thenBlock);
    open(thenBlock);
    visit(node.thenPart);
    thenBlock = current;
    Map thenDefinitions = definitions;

    // Reset the definitions to the state after the condition.
    definitions = conditionDefinitions;

    // Now the else part.
    HBasicBlock elseBlock = null;
    if (hasElse) {
      elseBlock = graph.addNewBlock();
      conditionBlock.addSuccessor(elseBlock);
      open(elseBlock);
      visit(node.elsePart);
      elseBlock = current;
    }

    if (thenBlock === null && elseBlock === null && hasElse) {
      current = null;
    } else {
      HBasicBlock joinBlock = graph.addNewBlock();
      if (thenBlock !== null) goto(thenBlock, joinBlock);
      if (elseBlock !== null) goto(elseBlock, joinBlock);
      else if (!hasElse) conditionBlock.addSuccessor(joinBlock);
      // If the join block has two predecessors we have to merge the
      // definition maps. The current definitions is what either the
      // condition or the else block left us with, so we merge that
      // with the set of definitions we got after visiting the then
      // part of the if.
      open(joinBlock);
      if (joinBlock.predecessors.length == 2) {
        definitions = joinDefinitions(joinBlock,
                                      thenDefinitions,
                                      definitions);
      }
    }
  }

  SourceString unquote(LiteralString literal) {
    String str = '${literal.value}';
    compiler.ensure(str[0] == '@');
    int quotes = 1;
    String quote = str[1];
    while (str[quotes + 1] === quote) quotes++;
    return new SourceString(str.substring(quotes + 1, str.length - quotes));
  }

  visitSend(Send node) {
    // TODO(kasperl): This only works for very special cases. Make
    // this way more general soon.
    Element element = elements[node];
    if (node.selector is Operator) {
      visit(node.receiver);
      visit(node.argumentsNode);
      var right = pop();
      var left = pop();
      Operator op = node.selector;
      // TODO(floitsch): switch to switch (bug 314).
      if (const SourceString("+") == op.source) {
        push(new HAdd(element, [left, right]));
      } else if (const SourceString("-") == op.source) {
        push(new HSubtract(element, [left, right]));
      } else if (const SourceString("*") == op.source) {
        push(new HMultiply(element, [left, right]));
      } else if (const SourceString("/") == op.source) {
        push(new HDivide(element, [left, right]));
      } else if (const SourceString("~/") == op.source) {
        push(new HTruncatingDivide(element, [left, right]));
      } else if (const SourceString("==") == op.source) {
        push(new HEquals(element, [left, right]));
      }
    } else if (node.isPropertyAccess) {
      if (node.receiver !== null) {
        compiler.unimplemented("SsaBuilder.visitSend with receiver");
      }
      stack.add(definitions[element]);
    } else {
      Link<Node> link = node.arguments;
      if (element.kind === ElementKind.FOREIGN) {
        // If the invoke is on foreign code, don't visit the first
        // argument, which is the foreign code.
        link = link.tail;
      }
      var arguments = [];
      for (; !link.isEmpty(); link = link.tail) {
        visit(link.head);
        arguments.add(pop());
      }

      if (element.kind === ElementKind.FOREIGN) {
        LiteralString literal = node.arguments.head;
        compiler.ensure(literal is LiteralString);
        push(new HInvokeForeign(element, arguments, unquote(literal)));
      } else {
        final Identifier selector = node.selector;
        push(new HInvoke(element, arguments));
      }
    }
  }

  visitSendSet(SendSet node) {
    stack.add(updateDefinition(node));
  }

  void visitLiteralInt(LiteralInt node) {
    push(new HLiteral(node.value));
  }

  void visitLiteralDouble(LiteralDouble node) {
    push(new HLiteral(node.value));
  }

  void visitLiteralBool(LiteralBool node) {
    push(new HLiteral(node.value));
  }

  void visitLiteralString(LiteralString node) {
    push(new HLiteral(node.value));
  }

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      visit(link.head);
    }
  }

  visitOperator(Operator node) {
    compiler.unimplemented("SsaBuilder.visitOperator");
  }

  visitReturn(Return node) {
    if (node.expression === null) {
      compiler.unimplemented("SsaBuilder: return without expression");
    }
    visit(node.expression);
    var value = pop();
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
    // We currently ignore type annotations for generating code.
  }

  HInstruction updateDefinition(SendSet node) {
    if (node.receiver != null) {
      compiler.unimplemented("SsaBuilder: property access");
    }
    Link<Node> link = node.arguments;
    assert(!link.isEmpty() && link.tail.isEmpty());
    visit(link.head);
    HInstruction value = pop();
    definitions[elements[node]] = value;
    return value;
  }

  visitVariableDefinitions(VariableDefinitions node) {
    for (Link<Node> link = node.definitions.nodes;
         !link.isEmpty();
         link = link.tail) {
      Node definition = link.head;
      if (definition is Identifier) {
        compiler.unimplemented(
            "SsaBuilder.visitVariableDefinitions without initial value");
      } else {
        assert(definition is SendSet);
        updateDefinition(definition);
      }
    }
  }
}
