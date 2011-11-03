// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaOptimizerTask extends CompilerTask {
  SsaOptimizerTask(Compiler compiler) : super(compiler);
  String get name() => 'SSA optimizer';

  void optimize(HGraph graph) {
    measure(() {
      new SsaConstantFolder().visitGraph(graph);
      new SsaDeadCodeEliminator().visitGraph(graph);
      new SsaGlobalValueNumberer(compiler).visitGraph(graph);
      new SsaInstructionMerger().visitGraph(graph);
    });
  }
}

/**
 * If both inputs to known operations are available execute the operation at
 * compile-time.
 */
class SsaConstantFolder extends HBaseVisitor {
  visitGraph(HGraph graph) {
    visitDominatorTree(graph);
  }

  visitBasicBlock(HBasicBlock block) {
    HInstruction instruction = block.first;
    while (instruction !== null) {
      var replacement = instruction.accept(this);
      if (replacement !== instruction) {
        block.addAfter(instruction, replacement);
        block.rewrite(instruction, replacement);
        block.remove(instruction);
      }
      instruction = instruction.next;
    }
  }

  HInstruction visitInstruction(HInstruction node) {
    return node;
  }

  HInstruction visitAdd(HAdd node) {
    bool isNumber(input) {
      return input is HLiteral && input.value is num;
    }
    List inputs = node.inputs;
    assert(inputs.length == 2);
    if (isNumber(inputs[0]) && isNumber(inputs[1])) {
      return new HLiteral(inputs[0].value + inputs[1].value);
    }
    return node;
  }
}

class SsaDeadCodeEliminator extends HGraphVisitor {
  static bool isDeadCode(HInstruction instruction) {
    return !instruction.hasSideEffects() && instruction.usedBy.isEmpty();
  }

  void visitGraph(HGraph graph) {
    visitPostDominatorTree(graph);
  }

  void visitBasicBlock(HBasicBlock block) {
    HInstruction instruction = block.last;
    while (instruction !== null) {
      var previous = instruction.previous;
      if (isDeadCode(instruction)) block.remove(instruction);
      instruction = previous;
    }
  }
}

class SsaGlobalValueNumberer extends HGraphVisitor {
  final Compiler compiler;
  final Map<HInstruction, HInstruction> values;
  SsaGlobalValueNumberer(this.compiler)
    : values = new Map<HInstruction, HInstruction>();

  void visitGraph(HGraph graph) {
    visitPostDominatorTree(graph);
  }

  void visitBasicBlock(HBasicBlock block) {
    HInstruction instruction = block.first;
    while (instruction !== null) {
      if (instruction.hasSideEffects()) {
        values.clear();
      } else {
        HInstruction other = values[instruction];
        if (other !== null) {
          block.rewrite(instruction, other);
          block.remove(instruction);
        } else {
          values[instruction] = instruction;
        }
      }
      instruction = instruction.next;
    }
  }
}

/**
 * Instead of emitting each SSA instruction with a temporary variable
 * mark instructions that can be emitted at their use-site.
 * For example, in:
 *   t0 = 4;
 *   t1 = 3;
 *   t2 = add(t0, t1);
 * t0 and t1 would be marked and the resulting code would then be:
 *   t2 = add(4, 3);
 */
class SsaInstructionMerger extends HInstructionVisitor {
  void visitGraph(HGraph graph) {
    visitDominatorTree(graph);
  }

  void visitInstruction(HInstruction node) {
    List<HInstruction> inputs = node.inputs;
    HInstruction previousUnused = node.previous;
    // We hope that the instruction's inputs have been pushed from left to
    // right just before this instruction. The last input is then located just
    // before this instruction. If we were able to match the last input we can
    // look at the next-previous instruction and the next argument.
    for (int i = inputs.length - 1; i >= 0; i--) {
      if (previousUnused === null) return;
      if (inputs[i].usedBy.length != 1) return;
      if (inputs[i] !== previousUnused) return;
      // Our arguments are in the correct location to be inlined.
      inputs[i].setCanBeGeneratedAtUseSite();
      previousUnused = previousUnused.previous;
    }
  }
}
