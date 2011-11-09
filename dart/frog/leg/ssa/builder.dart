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
  HBasicBlock block;

  SsaBuilder(this.compiler, this.elements);

  HGraph build(NodeList parameters, Node body) {
    graph = new HGraph();
    stack = new List<HInstruction>();
    definitions = new Map<Element, HInstruction>();

    block = graph.addNewBlock();
    graph.entry.addGoto(graph, block);
    visitParameters(parameters);
    body.accept(this);

    // TODO(kasperl): Make this goto an implicit return.
    if (!isAborted()) block.addGoto(graph, graph.exit);
    graph.finalize();
    return graph;
  }

  bool isAborted() {
    return block === null;
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
      if (isAborted()) {
        // The block has been aborted by a return or a throw.
        if (!stack.isEmpty()) compiler.cancel('non-empty instruction stack');
        return;
      }
    }
    assert(block.last is !HGoto && block.last is !HReturn);
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
    // If an element is in one map but not the other we can safely ignore it. It
    // means that a variable was declared in the block. Since variable
    // declarations are scoped the declared variable cannot be alive outside
    // the block.
    // Note: this is only true for nodes where we do joins.
    if (incoming1.length > incoming2.length) {
      // Inverse the two maps.
      return joinDefinitions(joinBlock, incoming2, incoming1);
    }
    Map<Element, HInstruction> joinedDefinitions =
        new Map<Element, HInstruction>();
    assert(incoming1.length <= incoming2.length);
    incoming1.forEach((element, instruction) {
      HInstruction other = incoming2[element];
      if (other === null) return;
      if (instruction === other) {
        joinedDefinitions[element] = instruction;
      } else {
        HInstruction phi = new HPhi(instruction, other);
        joinBlock.add(phi);
        joinedDefinitions[element] = phi;
      }
    });
    return joinedDefinitions;
  }

  visitIf(If node) {
    bool hasElse = (node.elsePart !== null);

    // The condition is added to the current block.
    visit(node.condition);
    HInstruction condition = pop();
    HBasicBlock conditionBlock = block;

    Map afterConditionDefinitions =
        new Map<Element, HInstruction>.from(definitions);

    // The then part.
    HBasicBlock thenBlock = graph.addNewBlock();
    block = thenBlock;
    visit(node.thenPart);
    bool thenBlockJoined = !isAborted();
    HBasicBlock thenExitBlock = block;
    Map thenDefinitions = definitions;

    // Now the else part.
    bool elseBlockJoined;
    HBasicBlock elseBlock;
    HBasicBlock joinBlock;
    // Reset the definitions to the state after the condition.
    definitions = afterConditionDefinitions;
    if (!hasElse) {
      elseBlockJoined = true;
      elseBlock = joinBlock = graph.addNewBlock();
    } else {
      elseBlock = graph.addNewBlock();
      block = elseBlock;
      visit(node.elsePart);
      elseBlockJoined = !isAborted();
      joinBlock = graph.addNewBlock();
    }
    Map elseDefinitions = definitions;
    HBasicBlock elseExitBlock = block;

    conditionBlock.add(new HIf(condition, hasElse));
    graph.setSuccessors(conditionBlock, <HBasicBlock>[thenBlock, elseBlock]);

    if (!thenBlockJoined && !elseBlockJoined) {
      block = null;
    } else {
      block = joinBlock;
      if (thenBlockJoined && elseBlockJoined) {
        thenExitBlock.addGoto(graph, joinBlock);
        if (hasElse) elseExitBlock.addGoto(graph, joinBlock);
        definitions =
            joinDefinitions(joinBlock, thenDefinitions, elseDefinitions);
      } else if (thenBlockJoined) {
        thenExitBlock.addGoto(graph, joinBlock);
      } else if (elseBlockJoined) {
        elseExitBlock.addGoto(graph, joinBlock);
      }
    }
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
