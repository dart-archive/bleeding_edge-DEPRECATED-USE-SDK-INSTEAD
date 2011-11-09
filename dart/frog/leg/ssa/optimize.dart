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

  HInstruction visitArithmetic(HArithmetic node) {
    List<HInstruction> inputs = node.inputs;
    assert(inputs.length == 2);
    if (inputs[0].isLiteralNumber() && inputs[1].isLiteralNumber()) {
      HLiteral op1 = inputs[0];
      HLiteral op2 = inputs[1];
      // TODO(floitsch): don't use try/catch but use a closure instead and
      // let the ~/ check that the right-hand-side is not 0.
      try {
        num folded = node.evaluate(op1.value, op2.value);
        return new HLiteral(folded);
      } catch(IntegerDivisionByZeroException e) {
        // TODO(floitsch): return an exception throwing node.
        return node;
      }
    }
    return node;
  }

  HInstruction visitAdd(HAdd node) {
    // String + is defined for all literals. We don't need to know which
    // literal type the right-hand side is.
    // TODO(floitsch): is String + literal a compile-time expression? If not
    // we must pay attention not to canonicalize the concatenated string with
    // an already existing string.
    if (node.inputs[0].isLiteralString()) {
      HLiteral op1 = node.inputs[0];
      HLiteral op2 = node.inputs[1];
      return new HLiteral(new SourceString("${op1.value} + ${op2.value}"));
    }
    return visitArithmetic(node);
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

class SsaGlobalValueNumberer {
  final Compiler compiler;
  final Set<int> visited;
  List<int> blockChangesFlags;

  SsaGlobalValueNumberer(this.compiler) : visited = new Set<int>();

  void visitGraph(HGraph graph) {
    blockChangesFlags = new List<int>(graph.exit.id + 1);
    visitBasicBlock(graph.entry, new ValueSet());
  }

  void visitBasicBlock(HBasicBlock block, ValueSet values) {
    HInstruction instruction = block.first;
    while (instruction !== null) {
      int flags = instruction.getChangesFlags();
      if (flags != 0) {
        assert(!instruction.useGvn());
        values.kill(flags);
      } else if (instruction.useGvn()) {
        HInstruction other = values.lookup(instruction);
        if (other !== null) {
          assert(other.equals(instruction) && instruction.equals(other));
          block.rewrite(instruction, other);
          block.remove(instruction);
        } else {
          values.add(instruction);
        }
      }
      instruction = instruction.next;
    }

    List<HBasicBlock> dominatedBlocks = block.dominatedBlocks;
    for (int i = 0, length = dominatedBlocks.length; i < length; i++) {
      HBasicBlock dominated = dominatedBlocks[i];
      // No need to copy the value set for the last child.
      ValueSet successorValues = (i == length - 1) ? values : values.copy();
      // If we have no values in our set, we do not have to kill
      // anything. Also, if the range of block ids from the current
      // block to the dominated block is empty, there is no blocks on
      // any path from the current block to the dominated block so we
      // don't have to do anything either.
      assert(block.id < dominated.id);
      if (!successorValues.isEmpty() && block.id + 1 < dominated.id) {
        visited.clear();
        int changesFlags = getChangesFlagsForDominatedBlock(block, dominated);
        successorValues.kill(changesFlags);
      }
      visitBasicBlock(dominated, successorValues);
    }
  }

  int getChangesFlagsForBlock(HBasicBlock block) {
    final int id = block.id;
    final int cached = blockChangesFlags[id];
    if (cached !== null) return cached;
    int changesFlags = 0;
    HInstruction instruction = block.first;
    while (instruction !== null) {
      changesFlags |= instruction.getChangesFlags();
      instruction = instruction.next;
    }
    return blockChangesFlags[id] = changesFlags;
  }

  int getChangesFlagsForDominatedBlock(HBasicBlock dominator,
                                       HBasicBlock dominated) {
    int changesFlags = 0;
    List<HBasicBlock> predecessors = dominated.predecessors;
    for (int i = 0, length = predecessors.length; i < length; i++) {
      HBasicBlock block = predecessors[i];
      int id = block.id;
      // If the current predecessor block is on the path from the
      // dominator to the dominated, it must have an id that is in the
      // range from the dominator to the dominated.
      if (dominator.id < id && id < dominated.id && !visited.contains(id)) {
        visited.add(id);
        changesFlags |= getChangesFlagsForBlock(block);
        changesFlags |= getChangesFlagsForDominatedBlock(dominator, block);
      }
    }
    return changesFlags;
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
      // HPhis cannot be generated at use site.
      // Also they are at the beginning of a block. So if we reach them, we
      // can abort the loop.
      if (previousUnused is HPhi) return;
      if (inputs[i].usedBy.length != 1) return;
      if (inputs[i] !== previousUnused) return;
      // Our arguments are in the correct location to be inlined.
      inputs[i].setGenerateAtUseSite();
      previousUnused = previousUnused.previous;
    }
  }
}
