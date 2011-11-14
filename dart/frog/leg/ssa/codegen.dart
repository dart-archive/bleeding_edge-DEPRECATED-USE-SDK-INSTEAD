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
      String code = generateMethod(name.source,
                                   countParameters(function),
                                   graph);
      return code;
    });
  }

  String generateMethod(SourceString methodName,
                        int parameterCount,
                        HGraph graph) {
    StringBuffer buffer = new StringBuffer();
    SsaCodeGenerator codegen = new SsaCodeGenerator(compiler, buffer);
    graph.assignInstructionIds();
    codegen.visitGraph(graph);
    StringBuffer parameters = new StringBuffer();
    for (int i = 0; i < parameterCount; i++) {
      if (i != 0) parameters.add(', ');
      parameters.add(SsaCodeGenerator.parameter(i));
    }
    return 'function $methodName($parameters) {\n$buffer}\n';
  }

  // TODO(floitsch): remove this method when NodeList has a 'length' field.
  static int countParameters(FunctionExpression function) {
    int result = 0;
    for (Link<Node> link = function.parameters.nodes;
         !link.isEmpty();
         link = link.tail) {
      result++;
    }
    return result;
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

  static String temporary(HInstruction instruction) => 't${instruction.id}';
  static String parameter(int index) => 'p$index';

  void invoke(Element element, List<HInstruction> arguments) {
    buffer.add('${element.name}(');
    for (int i = 0; i < arguments.length; i++) {
      if (i != 0) buffer.add(', ');
      use(arguments[i]);
    }
    buffer.add(")");
  }

  void define(HInstruction instruction) {
    // Assigns the instruction's value to its temporary.
    // If the instruction is furthermore used in phis, the temporary is also
    // assigned to the phi's temporary, thus updating the phi's value.
    // If the only use is a phi we can avoid the instruction's temporary
    // and assign only to the phi's temporary.
    List usedBy = instruction.usedBy;
    if (usedBy.length == 1 && usedBy[0] is HPhi) {
      buffer.add('var ${temporary(usedBy[0])} = ');
      visit(instruction);
    } else {
      String instructionId = temporary(instruction);
      buffer.add('var $instructionId = ');
      visit(instruction);
      // Assign the value to any phi.
      for (int i = 0; i < usedBy.length; i++) {
        if (usedBy[i] is HPhi) {
          buffer.add(';\n');
          addIndentation();
          buffer.add('var ${temporary(usedBy[i])} = $instructionId');
        }
      }
    }
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
    if (node.isLoopHeader) {
      buffer.add('while(true) {\n');
      indent++;
    }
    currentBlock = node;
    HInstruction instruction = node.first;
    while (instruction != null) {
      if (!instruction.generateAtUseSite()) {
        addIndentation();
        if (instruction.usedBy.isEmpty() || instruction is HPhi) {
          visit(instruction);
        } else {
          define(instruction);
        }
        buffer.add(';\n');
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
    // TODO(ngeoffray): generate the instruction define here in case
    // we have parameters.
    if (!node.inputs.isEmpty()) {
      buffer.add("(function foreign(\$0");
      for (int i = 1; i < node.inputs.length; i++) {
        buffer.add(', \$$i');
      }
      buffer.add(') { return ');
    }
    buffer.add(node.code);

    if (!node.inputs.isEmpty()) {
      buffer.add('; }) (');
      use(node.inputs[0]);
      for (int i = 1; i < node.inputs.length; i++) {
        buffer.add(', ');
        use(node.inputs[1]);
      }
      buffer.add(')');
    }
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

  visitParameter(HParameter node) {
    buffer.add(parameter(node.parameterIndex));
  }

  visitPhi(HPhi node) {
    // Phi nodes have their values set at their inputs. Every instruction that
    // is used by a phi updates the phi's temporary. Therefore, in most cases,
    // phi's don't need to do anything. The exception is, when a phi is again
    // used in another phi. Then we have to update the other phi's temporary.
    List usedBy = node.usedBy;
    bool firstPhi = true;
    for (int i = 0; i < usedBy.length; i++) {
      if (usedBy[i] is HPhi) {
        if (!firstPhi) {
          buffer.add(";\n");
          addIndentation();
        }
        buffer.add("var ${temporary(usedBy[i])} = ${temporary(node)}");
        firstPhi = false;
      }
    }
  }

  visitReturn(HReturn node) {
    buffer.add('return ');
    use(node.inputs[0]);
  }

  visitThrow(HThrow node) {
    buffer.add('throw ');
    use(node.inputs[0]);
  }

  void addIndentation() {
    for (int i = 0; i < indent; i++) {
      buffer.add('  ');
    }
  }
}
