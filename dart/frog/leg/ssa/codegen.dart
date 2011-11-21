// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaCodeGeneratorTask extends CompilerTask {
  SsaCodeGeneratorTask(Compiler compiler) : super(compiler);
  String get name() => 'SSA code generator';

  String generate(Node tree, HGraph graph) {
    return measure(() {
      FunctionExpression function = tree;
      NodeList parameters = function.parameters;
      List<String> parameterNames = [];
      for (var link = parameters.nodes; !link.isEmpty(); link = link.tail) {
        VariableDefinitions parameter = link.head;
        SourceString name = parameter.definitions.nodes.head.dynamic.source;
        parameterNames.add(JsNames.getValid('$name'));
      }

      Identifier name = function.name;
      if (GENERATE_SSA_TRACE) {
        new HTracer.singleton().traceGraph("codegen", graph);
      }
      String code = generateMethod(name.source,
                                   parameterNames,
                                   graph);
      return code;
    });
  }

  String generateMethod(SourceString methodName,
                        List<String> parameterNames,
                        HGraph graph) {
    new SsaPhiEliminator().visitGraph(graph);
    if (GENERATE_SSA_TRACE) {
      new HTracer.singleton().traceGraph("no-phi", graph);
    }
    StringBuffer buffer = new StringBuffer();
    SsaCodeGenerator codegen =
        new SsaCodeGenerator(compiler, buffer, parameterNames);
    codegen.visitGraph(graph);
    StringBuffer parameters = new StringBuffer();
    for (int i = 0; i < parameterNames.length; i++) {
      if (i != 0) parameters.add(', ');
      parameters.add(parameterNames[i]);
    }
    return 'function $methodName($parameters) {\n$buffer}\n';
  }
}

class SsaCodeGenerator implements HVisitor {
  final Compiler compiler;
  final StringBuffer buffer;

  final List<String> parameterNames;
  final Map<int, String> names;
  final Map<String, int> prefixes;

  int indent = 0;
  HGraph currentGraph;
  HBasicBlock currentBlock;

  SsaCodeGenerator(this.compiler, this.buffer, this.parameterNames)
    : names = new Map<int, String>(),
      prefixes = new Map<String, int>() {
    for (final name in parameterNames) {
      prefixes[name] = 0;
    }
  }

  visitGraph(HGraph graph) {
    currentGraph = graph;
    indent++;  // We are already inside a function.
    visitBasicBlock(graph.entry);
  }

  String parameter(int index) => parameterNames[index];

  String temporary(HInstruction instruction) {
    int id = instruction.id;
    String name = names[id];
    if (name !== null) return name;

    String prefix = 't';
    if (!prefixes.containsKey(prefix)) prefixes[prefix] = 0;
    return newName(id, '${prefix}${prefixes[prefix]++}');
  }

  String local(HLocal local) {
    int id = local.id;
    String name = names[id];
    if (name !== null) return name;

    String prefix;
    if (local.element !== null) {
      prefix = local.element.name.stringValue;
    } else {
      prefix = 'v';
    }
    if (!prefixes.containsKey(prefix)) {
      prefixes[prefix] = 0;
      return newName(id, prefix);
    } else {
      return newName(id, '${prefix}_${prefixes[prefix]++}');
    }
  }

  String newName(int id, String name) {
    String result = JsNames.getValid(name);
    names[id] = result;
    return result;
  }

  void invoke(Element element, List<HInstruction> arguments) {
    buffer.add('${element.name}(');
    for (int i = 0; i < arguments.length; i++) {
      if (i != 0) buffer.add(', ');
      use(arguments[i]);
    }
    buffer.add(")");
  }

  void define(HInstruction instruction) {
    buffer.add('var ${temporary(instruction)} = ');
    visit(instruction);
  }

  void use(HInstruction argument) {
    if (argument.generateAtUseSite()) {
      visit(argument);
    } else {
      buffer.add(temporary(argument));
    }
  }

  visit(node) {
    return node.accept(this);
  }

  visitBasicBlock(HBasicBlock node) {
    // While loop will be closed by the conditional loop-branch.
    // TODO(floitsch): HACK HACK HACK.
    if (node.isLoopHeader()) {
      addIndentation();
      buffer.add('while (true) {\n');
      indent++;
    }
    currentBlock = node;

    HInstruction instruction = node.first;
    while (instruction != null) {
      if (instruction is HGoto || instruction is HExit) {
        visit(instruction);
      } else if (!instruction.generateAtUseSite()) {
        addIndentation();
        if (instruction.usedBy.isEmpty() || instruction is HLocal) {
          visit(instruction);
        } else {
          define(instruction);
        }
        // Control flow instructions know how to handle ';'.
        if (instruction is !HControlFlow) buffer.add(';\n');
      }
      instruction = instruction.next;
    }
  }

  visitAdd(HAdd node)                           => visitInvoke(node);
  visitDivide(HDivide node)                     => visitInvoke(node);
  visitEquals(HEquals node)                     => visitInvoke(node);
  visitMultiply(HMultiply node)                 => visitInvoke(node);
  visitSubtract(HSubtract node)                 => visitInvoke(node);
  visitTruncatingDivide(HTruncatingDivide node) => visitInvoke(node);

  visitBoolify(HBoolify node) {
    assert(node.inputs.length == 1);
    buffer.add('(');
    use(node.inputs[0]);
    buffer.add(' === true)');
  }

  visitExit(HExit node) {
    // Don't do anything.
  }

  visitGoto(HGoto node) {
    assert(currentBlock.successors.length == 1);
    List<HBasicBlock> dominated = currentBlock.dominatedBlocks;
    // With the exception of the entry-node which is dominates its successor
    // and the exit node, no Block finishing with a 'goto' can have more than
    // one dominated block (since it has only one successor).
    // If the successor is dominated by another block, then the other block
    // is responsible for visiting the successor.
    if (dominated.isEmpty()) return;
    if (dominated.length > 2) unreachable();
    if (dominated.length == 2 && currentBlock !== currentGraph.entry) {
      unreachable();
    }
    assert(dominated[0] == currentBlock.successors[0]);
    visitBasicBlock(dominated[0]);
  }

  visitIf(HIf node) {
    // The currentBlock will be changed when we visit the successors. So keep
    // a local copy around.
    HBasicBlock ifBlock = currentBlock;
    buffer.add('if (');
    use(node.inputs[0]);
    buffer.add(') {\n');
    indent++;
    List<HBasicBlock> dominated = currentBlock.dominatedBlocks;
    assert(dominated[0] === ifBlock.successors[0]);
    visitBasicBlock(ifBlock.successors[0]);
    indent--;
    addIndentation();
    int nextDominatedIndex;
    if (node.hasElse) {
      assert(dominated[1] === ifBlock.successors[1]);
      buffer.add('} else {\n');
      indent++;
      visitBasicBlock(ifBlock.successors[1]);
      indent--;
      nextDominatedIndex = 2;
      addIndentation();
      buffer.add("}\n");
    } else {
      buffer.add("}\n");
      nextDominatedIndex = 1;
    }
    assert(dominated.length <= nextDominatedIndex + 1);
    // The HIf doesn't dominate the join, if both branches return or throw.
    if (dominated.length == nextDominatedIndex + 1) {
      visitBasicBlock(dominated[nextDominatedIndex]);
    }
  }

  visitInvoke(HInvoke node) {
    compiler.worklist.add(node.element);
    invoke(node.element, node.inputs);
  }

  visitInvokeForeign(HInvokeForeign node) {
    String code = '${node.code}';
    List<HInstruction> inputs = node.inputs;
    for (int i = 0; i < inputs.length; i++) {
      HInstruction input = inputs[i];
      assert(!input.generateAtUseSite());
      code = code.replaceAll('\$$i', temporary(input));
    }
    buffer.add('($code)');
  }

  visitLiteral(HLiteral node) {
    buffer.add(node.value);
  }

  visitLoopBranch(HLoopBranch node) {
    HBasicBlock branchBlock = currentBlock;
    buffer.add('if (!(');
    use(node.inputs[0]);
    buffer.add(')) break;\n');
    List<HBasicBlock> dominated = currentBlock.dominatedBlocks;
    assert(dominated.length == 2);
    assert(dominated[0] === branchBlock.successors[0]);
    visit(dominated[0]);
    indent--;
    addIndentation();
    buffer.add('}\n');  // Close 'while' loop.
    assert(dominated[1] === branchBlock.successors[1]);
    visit(dominated[1]);
  }

  visitNot(HNot node) {
    assert(node.inputs.length == 1);
    buffer.add('(!');
    use(node.inputs[0]);
    buffer.add(')');
  }

  visitParameter(HParameter node) {
    buffer.add(parameter(node.parameterIndex));
  }

  visitPhi(HPhi node) {
    // The SsaPhiEliminator made sure phis are gone in the function.
    unreachable();
  }

  visitReturn(HReturn node) {
    buffer.add('return ');
    use(node.inputs[0]);
    buffer.add(';\n');
  }

  visitThrow(HThrow node) {
    buffer.add('throw ');
    use(node.inputs[0]);
    buffer.add(';\n');
  }

  void addIndentation() {
    for (int i = 0; i < indent; i++) {
      buffer.add('  ');
    }
  }

  void visitStore(HStore node) {
    buffer.add('${local(node.inputs[0])} = ');
    use(node.inputs[1]);
  }

  void visitLoad(HLoad node) {
    buffer.add('${local(node.inputs[0])}');
  }

  void visitLocal(HLocal node) {
    buffer.add('var ${local(node)}');
  }
}
