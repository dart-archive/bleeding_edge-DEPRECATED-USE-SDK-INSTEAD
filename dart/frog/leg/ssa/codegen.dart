// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaCodeGeneratorTask extends CompilerTask {
  SsaCodeGeneratorTask(Compiler compiler) : super(compiler);
  String get name() => 'SSA code generator';

  String generate(Node tree, HGraph graph) {
    return measure(() {
      FunctionExpression function = tree;
      Identifier name = function.name;
      if (GENERATE_SSA_TRACE) {
        new HTracer.singleton().traceGraph("codegen", graph);
      }
      String code = generateMethod(name.source, graph);
      return code;
    });
  }

  String generateMethod(SourceString methodName, HGraph graph) {
    StringBuffer buffer = new StringBuffer();
    SsaCodeGenerator codegen = new SsaCodeGenerator(compiler, buffer);
    graph.number();
    codegen.visitGraph(graph);
    return 'function $methodName() {\n$buffer}\n';
  }
}

class SsaCodeGenerator implements HVisitor {
  final Compiler compiler;
  final StringBuffer buffer;

  int indent = 0;
  HGraph currentGraph;
  HBasicBlock currentBlock;

  SsaCodeGenerator(this.compiler, this.buffer);

  visitGraph(HGraph graph) {
    currentGraph = graph;
    indent++;  // We are already inside a function.
    visitBasicBlock(graph.entry);
  }

  String temporary(HInstruction instruction)
      => 't${instruction.id}';

  void invoke(SourceString selector, List<HInstruction> arguments) {
    buffer.add('$selector(');
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

  visitAdd(HAdd node) {
    invoke(const SourceString('\$add'), node.inputs);
  }

  visitBasicBlock(HBasicBlock node) {
    currentBlock = node;
    HInstruction instruction = node.first;
    while (instruction != null) {
      if (!instruction.generateAtUseSite()) {
        addIndentation();
        if (!instruction.usedBy.isEmpty()) {
          define(instruction);
        } else {
          visit(instruction);
        }
        buffer.add(';\n');
      }
      instruction = instruction.next;
    }
  }

  visitDivide(HDivide node) {
    invoke(const SourceString('\$div'), node.inputs);
  }

  visitExit(HExit node) {
    unreachable();
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
    // TODO(floitsch): Pass the element to the worklist and not just the
    // source.
    if (node.selector != const SourceString('print')) {
      compiler.worklist.add(node.selector);      
    }
    invoke(node.selector, node.inputs);
  }

  visitLiteral(HLiteral node) {
    buffer.add(node.value);
  }

  visitMultiply(HMultiply node) {
    invoke(const SourceString('\$mul'), node.inputs);
  }

  visitReturn(HReturn node) {
    buffer.add('return ');
    use(node.inputs[0]);
  }

  visitSubtract(HSubtract node) {
    invoke(const SourceString('\$sub'), node.inputs);
  }

  visitTruncatingDivide(HTruncatingDivide node) {
    invoke(const SourceString('\$tdiv'), node.inputs);
  }

  void addIndentation() {
    for (int i = 0; i < indent; i++) {
      buffer.add('  ');
    }
  }
}
