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
  SsaCodeGenerator(this.compiler, this.buffer);

  visitGraph(HGraph graph) {
    void visitBasicBlockAndSuccessors(HBasicBlock block) {
      visit(block);
      if (!block.successors.isEmpty()) {
        assert(block.successors.length == 1);
        visitBasicBlockAndSuccessors(block.successors[0]);
      }
    }

   visitBasicBlockAndSuccessors(graph.entry);
  }

  String temporary(HInstruction instruction)
      => 't${instruction.id}';

  void invoke(SourceString selector, List<HInstruction> arguments) {
    buffer.add("$selector(");
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
    if (argument.canBeGeneratedAtUseSite()) {
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
    HInstruction instruction = node.first;
    while (instruction != null) {
      if (!instruction.canBeSkipped()) {
        buffer.add('  ');
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
    unreachable();
  }

  visitInvoke(HInvoke node) {
    // TODO(floitsch): Pass the element to the worklist and not just the
    // source.
    if (node.selector != const SourceString("print")) {
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
}
