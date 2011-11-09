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

  int nextFreeBlockId = 0;
  HGraph graph;

  // We build the Ssa graph by simulating a stack machine.
  List<HInstruction> stack;

  Map<Element, HInstruction> definitions;
  // The current block to add instructions to. Might be null, if we are
  // visiting dead code.
  HBasicBlock block;
  // The successor of the current block. Needs to be set by any instruction
  // that closes the current block.
  HBasicBlock successor;

  SsaBuilder(this.compiler, this.elements);

  HGraph build(NodeList parameters, Node body) {
    graph = new HGraph();
    graph.entry.id = nextFreeBlockId++;
    stack = new List<HInstruction>();
    definitions = new Map<Element, HInstruction>();

    block = new HBasicBlock.withId(nextFreeBlockId++);
    // Add the method body as successor of the graph's entry block.
    graph.entry.add(new HGoto());
    graph.setSuccessors(graph.entry, <HBasicBlock>[block]);
    graph.entry.addDominatedBlock(block);

    successor = graph.exit;
    visitParameters(parameters);
    body.accept(this);

    graph.exit.id = nextFreeBlockId++;
    // TODO(floitsch): we add exit as dominated by entry. Does this make sense?
    graph.entry.addDominatedBlock(graph.exit);

    return graph;
  }

  void add(HInstruction instruction) {
    block.add(instruction);
  }

  void push(HInstruction instruction) {
    add(instruction);
    stack.add(instruction);
  }

  HInstruction pop() {
    return stack.removeLast();
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
      // nodeList must contain exactly one argument.
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
      if (block == null) {
        // The block has been aborted by a return or a throw.
        if (!stack.isEmpty()) compiler.cancel('non-empty instruction stack');
        return;
      }
    }
    assert(block.last is !HGoto && block.last is !HReturn);
    // TODO(floitsch): add implicit return iff the successor is the exit block.
    // For now just make it always a Goto.
    block.add(new HGoto());
    graph.setSuccessors(block, <HBasicBlock>[successor]);
    if (!stack.isEmpty()) compiler.cancel('non-empty instruction stack');
  }

  visitExpressionStatement(ExpressionStatement node) {
    visit(node.expression);
    pop();
  }

  visitFor(For node) {
    compiler.unimplemented("SsaBuilder.visitFor");
  }

  visitFunctionExpression(FunctionExpression node) {
    compiler.unimplemented('SsaBuilder.visitFunctionExpression');
  }

  visitIdentifier(Identifier node) {
    Element element = elements[node];
    // TODO(floitsch): bail out if we don't know the element type.
    assert(element !== null);
    HInstruction def = definitions[element];
    assert(def !== null);
    stack.add(def);
  }

  Map<Element, HInstruction> joinDefinitions(
      HBasicBlock joinBlock,
      Map<Element, HInstruction> incoming1,
      Map<Element, HInstruction> incoming2) {
    if (incoming1.length != incoming2.length) compiler.cancel("No Phis yet.");
    incoming1.forEach((element, instruction) {
      if (incoming2[element] !== instruction) compiler.cancel("No Phis yet.");
    });
    return incoming1;
  }

  visitIf(If node) {
    bool hasElse = (node.elsePart !== null);

    // The condition is added to the current block.
    visit(node.condition);
    HInstruction condition = pop();
    HBasicBlock conditionBlock = block;

    Map afterConditionDefinitions =
        new Map<Element, HInstruction>.from(definitions);
    HBasicBlock oldSuccessor = successor;

    // We have to wait to assign an id to the joinBlock until we visited the
    // then and else block.
    HBasicBlock joinBlock = new HBasicBlock();

    // The then part.
    HBasicBlock thenBlock = new HBasicBlock.withId(nextFreeBlockId++);
    block = thenBlock;
    successor = joinBlock;
    visit(node.thenPart);
    bool thenBlockJoined = (block !== null);
    HBasicBlock thenExitBlock = block;
    Map thenDefinitions = definitions;

    // Now the else part.
    bool elseBlockJoined;
    HBasicBlock elseBlock;
    // Reset the definitions to the state after the condition.
    definitions = afterConditionDefinitions;
    if (!hasElse) {
      elseBlockJoined = true;
      elseBlock = joinBlock;
    } else {
      elseBlock = new HBasicBlock.withId(nextFreeBlockId++);
      block = elseBlock;
      successor = joinBlock;
      visit(node.elsePart);
      elseBlockJoined = (block !== null);
    }
    Map elseDefinitions = definitions;
    HBasicBlock elseExitBlock = block;

    conditionBlock.add(new HIf(condition, hasElse));
    graph.setSuccessors(conditionBlock, <HBasicBlock>[thenBlock, elseBlock]);
    conditionBlock.addDominatedBlock(thenBlock);
    if (hasElse) conditionBlock.addDominatedBlock(elseBlock);

    if (!thenBlockJoined && !elseBlockJoined) {
      block = null;
    } else {
      // Now, that we have visited the then and else block we can assign an id
      // to the join block.
      joinBlock.id = nextFreeBlockId++;
      block = joinBlock;
      if (thenBlockJoined && elseBlockJoined) {
        conditionBlock.addDominatedBlock(joinBlock);
        definitions =
            joinDefinitions(joinBlock, thenDefinitions, elseDefinitions);
      } else if (thenBlockJoined) {
        thenExitBlock.addDominatedBlock(joinBlock);
      } else if (elseBlockJoined) {
        elseExitBlock.addDominatedBlock(joinBlock);
      }
    }
    successor = oldSuccessor;
  }

  visitSend(Send node) {
    // TODO(kasperl): This only works for very special cases. Make
    // this way more general soon.
    if (node.selector is Operator) {
      visit(node.receiver);
      visit(node.argumentsNode);
      var right = pop();
      var left = pop();
      Operator op = node.selector;
      // TODO(floitsch): switch to switch (bug 314).
      if (const SourceString("+") == op.source) {
        push(new HAdd([left, right]));
      } else if (const SourceString("-") == op.source) {
        push(new HSubtract([left, right]));
      } else if (const SourceString("*") == op.source) {
        push(new HMultiply([left, right]));
      } else if (const SourceString("/") == op.source) {
        push(new HDivide([left, right]));
      } else if (const SourceString("~/") == op.source) {
        push(new HTruncatingDivide([left, right]));
      }
    } else if (node.isPropertyAccess) {
      if (node.receiver !== null) {
        compiler.unimplemented("SsaBuilder.visitSend with receiver");
      }
      Element element = elements[node];
      stack.add(definitions[element]);
    } else {
      visit(node.argumentsNode);
      var arguments = [];
      for (Link<Node> link = node.arguments;
           !link.isEmpty();
           link = link.tail) {
        arguments.add(pop());
      }
      Identifier selector = node.selector;
      push(new HInvoke(selector.source, arguments));
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
    visit(node.expression);
    var value = pop();
    add(new HReturn(value));
    graph.setSuccessors(block, <HBasicBlock>[graph.exit]);
    // A return aborts the building of the current block.
    block = null;
  }

  visitThrow(Throw node) {
    compiler.unimplemented("SsaBuilder.visitThrow");
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
    return definitions[elements[node]] = value;
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
