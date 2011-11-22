// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaOptimizerTask extends CompilerTask {
  SsaOptimizerTask(Compiler compiler) : super(compiler);
  String get name() => 'SSA optimizer';

  void optimize(HGraph graph) {
    measure(() {
      new SsaConstantFolder().visitGraph(graph);
      new SsaTypePropagator().visitGraph(graph);
      new SsaGlobalValueNumberer(compiler).visitGraph(graph);
      new SsaDeadCodeEliminator().visitGraph(graph);
      new SsaDeadPhiEliminator().visitGraph(graph);
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

  HInstruction visitEquals(HEquals node) {
    List<HInstruction> inputs = node.inputs;
    if (inputs[0] is HLiteral && inputs[1] is HLiteral) {
      HLiteral op1 = inputs[0];
      HLiteral op2 = inputs[1];
      return new HLiteral(op1.value == op2.value);
    }
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

class SsaTypePropagator extends HGraphVisitor {

  final Map<int, HInstruction> workmap;
  final List<int> worklist;

  SsaTypePropagator()
      : workmap = new Map<int, HInstruction>(),
        worklist = new List<int>();

  void visitGraph(HGraph graph) {
    visitDominatorTree(graph);
    processWorklist();
  }

  visitBasicBlock(HBasicBlock block) {
    if (block.isLoopHeader()) {
      block.forEachPhi((HPhi phi) {
        if (phi.updateTypeForLoopPhi()) addToWorklist(phi);
      });
    } else {
      block.forEachPhi((HPhi phi) {
        phi.updateType();
      });
    }

    HInstruction instruction = block.first;
    while (instruction !== null) {
      instruction.updateType();
      instruction = instruction.next;
    }
  }

  void processWorklist() {
    while (!worklist.isEmpty()) {
      int id = worklist.removeLast();
      HInstruction instruction = workmap[id];
      assert(instruction !== null);
      workmap.remove(id);
      if (instruction.updateType()) {
        for (int i = 0, length = instruction.usedBy.length; i < length; i++) {
          addToWorklist(instruction.usedBy[i]);
        }
      }
    }
  }

  void addToWorklist(HInstruction instruction) {
    final int id = instruction.id;
    if (!workmap.containsKey(id)) {
      worklist.add(id);
      workmap[id] = instruction;
    }
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

class SsaDeadPhiEliminator {
  void visitGraph(HGraph graph) {
    final List<HPhi> worklist = <HPhi>[];
    // A set to keep track of the live phis that we found.
    final Set<HPhi> livePhis = new Set<HPhi>();

    // Add to the worklist all live phis: phis referenced by non-phi
    // instructions.
    for (final block in graph.blocks) {
      block.forEachPhi((HPhi phi) {
        for (final user in phi.usedBy) {
          if (user is !HPhi) {
            worklist.add(phi);
            livePhis.add(phi);
            break;
          }
        }
      });
    }

    // Process the worklist by propagating liveness to phi inputs.
    while (!worklist.isEmpty()) {
      HPhi phi = worklist.removeLast();
      for (final input in phi.inputs) {
        if (input is HPhi && !livePhis.contains(input)) {
          worklist.add(input);
          livePhis.add(input);
        }
      }
    }

    // Remove phis that are not live.
    for (final block in graph.blocks) {
      HPhi current = block.phis.first;
      HPhi next = null;
      while (current != null) {
        next = current.next;
        if (!livePhis.contains(current)) block.removePhi(current);
        current = next;
      }
    }
  }
}

class SsaGlobalValueNumberer {
  final Compiler compiler;
  final Set<int> visited;

  List<int> blockChangesFlags;
  List<int> loopChangesFlags;

  SsaGlobalValueNumberer(this.compiler) : visited = new Set<int>();

  void visitGraph(HGraph graph) {
    computeChangesFlags(graph);
    moveLoopInvariantCode(graph);
    visitBasicBlock(graph.entry, new ValueSet());
  }

  void moveLoopInvariantCode(HGraph graph) {
    for (int i = graph.blocks.length - 1; i >= 0; i--) {
      HBasicBlock block = graph.blocks[i];
      if (block.isLoopHeader()) {
        int changesFlags = loopChangesFlags[block.id];
        HBasicBlock last = block.loopInformation.getLastBackEdge();
        for (int j = block.id; j <= last.id; j++) {
          moveLoopInvariantCodeFromBlock(graph.blocks[j], block, changesFlags);
        }
      }
    }
  }

  void moveLoopInvariantCodeFromBlock(HBasicBlock block,
                                      HBasicBlock loopHeader,
                                      int changesFlags) {
    HBasicBlock preheader = loopHeader.predecessors[0];
    int dependsFlags = HInstruction.computeDependsOnFlags(changesFlags);
    HInstruction instruction = block.first;
    while (instruction != null) {
      HInstruction next = instruction.next;
      if (instruction.useGvn() && (instruction.flags & dependsFlags) == 0) {
        bool loopInvariantInputs = true;
        List<HInstruction> inputs = instruction.inputs;
        for (int i = 0, length = inputs.length; i < length; i++) {
          if (isInputDefinedAfterDominator(inputs[i], preheader)) {
            loopInvariantInputs = false;
            break;
          }
        }

        // If the inputs are loop invariant, we can move the
        // instruction from the current block to the pre-header block.
        if (loopInvariantInputs) {
          block.detach(instruction);
          preheader.moveAtExit(instruction);
        }
      }
      instruction = next;
    }
  }

  bool isInputDefinedAfterDominator(HInstruction input,
                                    HBasicBlock dominator) {
    return input.block.id > dominator.id;
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

  void computeChangesFlags(HGraph graph) {
    // Create the changes flags lists. Make sure to initialize the
    // loop changes flags list to zero so we can use bitwise or when
    // propagating loop changes upwards.
    final int length = graph.blocks.length;
    blockChangesFlags = new List<int>(length);
    loopChangesFlags = new List<int>(length);
    for (int i = 0; i < length; i++) loopChangesFlags[i] = 0;

    // Run through all the basic blocks in the graph and fill in the
    // changes flags lists.
    for (int i = length - 1; i >= 0; i--) {
      final HBasicBlock block = graph.blocks[i];
      final int id = block.id;

      // Compute block changes flags for the block.
      int changesFlags = 0;
      HInstruction instruction = block.first;
      while (instruction !== null) {
        instruction.prepareGvn();
        changesFlags |= instruction.getChangesFlags();
        instruction = instruction.next;
      }
      assert(blockChangesFlags[id] === null);
      blockChangesFlags[id] = changesFlags;

      // Loop headers are part of their loop, so update the loop
      // changes flags accordingly.
      if (block.isLoopHeader()) {
        loopChangesFlags[id] |= changesFlags;
      }

      // Propagate loop changes flags upwards.
      HBasicBlock parentLoopHeader = block.parentLoopHeader;
      if (parentLoopHeader !== null) {
        loopChangesFlags[parentLoopHeader.id] |= (block.isLoopHeader())
            ? loopChangesFlags[id]
            : changesFlags;
      }
    }
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
        changesFlags |= blockChangesFlags[id];
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
    // We don't want to generate instructions at the call-site of JS.
    if (node is HInvokeForeign) return;

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
      if (inputs[i].usedBy.length != 1) return;
      if (inputs[i] !== previousUnused) return;
      // Our arguments are in the correct location to be inlined.
      inputs[i].setGenerateAtUseSite();
      previousUnused = previousUnused.previous;
    }
  }
}
