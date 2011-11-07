// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaBuilderTask extends CompilerTask {
  SsaBuilderTask(Compiler compiler) : super(compiler);
  String get name() => 'SSA builder';

  HGraph build(Node tree, Map<Node, Element> elements) {
    return measure(() {
      FunctionExpression function = tree;
      HGraph graph = compileMethod(function.body, elements);
      assert(graph.isValid());
      if (GENERATE_SSA_TRACE) {
        Identifier name = function.name;
        new HTracer.singleton().traceCompilation(name.source.toString());
        new HTracer.singleton().traceGraph('builder', graph);
      }
      return graph;
    });
  }

  HGraph compileMethod(Node body, Map<Node, Element> elements) {
    SsaBuilder builder = new SsaBuilder(compiler, elements);
    HGraph graph = builder.build(body);
    return graph;
  }
}

class SsaBuilder implements Visitor {
  final Compiler compiler;
  final Map<Node, Element> elements;
  
  HBasicBlock block;
  HGraph graph;
  List<HInstruction> stack;
  Map<Element, HInstruction> definitions;

  SsaBuilder(this.compiler, this.elements);

  HGraph build(Node body) {
    graph = new HGraph();
    block = new HBasicBlock();
    stack = new List<HInstruction>();
    definitions = new Map<Element, HInstruction>();
    body.accept(this);
    // TODO(floitsch): add implicit return. For now just make it a Goto.
    if (block.last === null || block.last is !HReturn) {
      block.add(new HGoto());
      graph.setSuccessors(block, <HBasicBlock>[graph.exit]);
    }

    // Add the method body as successor of the graph's entry block.
    graph.entry.add(new HGoto());
    graph.setSuccessors(graph.entry, <HBasicBlock>[block]);
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

  visitBlock(Block node) {
    visit(node.statements);
    if (!stack.isEmpty()) compiler.cancel('non-empty instruction stack');
  }

  visitExpressionStatement(ExpressionStatement node) {
    visit(node.expression);
    pop();
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

  visitIf(If node) {
    compiler.unimplemented('SsaBuilder.visitIf');
    // TODO(floitsch): Implement this.
    // visit(node.condition);
    // visit(node.thenPart);
    // if (node.elsePart !== null) visit(node.elsePart);
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
      visit(node.selector);
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
    compiler.unimplemented('SsaBuilder.visitSendSet');
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

  visitParameter(Parameter node) {
    compiler.unimplemented("SsaBuilder.visitParameter");
  }

  visitReturn(Return node) {
    visit(node.expression);
    var value = pop();
    add(new HReturn(value));
    graph.setSuccessors(block, <HBasicBlock>[graph.exit]);
  }

  visitTypeAnnotation(TypeAnnotation node) {
    // We currently ignore type annotations for generating code.
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
        assert(definition is Send);
        Send init = definition;
        assert(init.selector.dynamic.source == const SourceString("="));
        Identifier id = init.receiver;
        Link<Node> link = init.arguments;
        assert(!link.isEmpty() && link.tail.isEmpty());
        visit(link.head);
        HInstruction initialValue = pop();
        definitions[elements[id]] = initialValue;
      }
    }
  }
}
