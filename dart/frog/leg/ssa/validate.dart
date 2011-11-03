// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class HValidator extends HInstructionVisitor {
  bool isValid = true;
  HGraph graph;

  void visitGraph(HGraph graph) {
    this.graph = graph;
    visitDominatorTree(graph);
  }

  // Note that during construction of the Ssa graph the basic blocks are
  // not required to be valid yet.
  void visitBasicBlock(HBasicBlock block) {
    if (!isValid) return;  // Don't need to continue if we are already invalid.

    // Test that the last instruction is a branching instruction and that the
    // basic block contains the branch-target.
    if (block.first === null || block.last === null) isValid = false;
    if (block.last is !HGoto &&
        block.last is !HReturn &&
        block.last is !HExit) {
      isValid = false;
    }
    if (block.last is HGoto && block.successors.length != 1) isValid = false;
    if (block.last is HReturn &&
        (block.successors.length != 1 || !block.successors[0].isExitBlock())) {
      isValid = false;
    }
    if (block.last is HExit && !block.successors.isEmpty()) isValid = false;

    if (block.successors.isEmpty() &&
        (block.first !== block.last || block.last is !HExit)) {
      isValid = false;
    }

    if (!isValid) return;
    super.visitBasicBlock(block);
  }

  /** Returns how often [instruction] is contained in [instructions]. */
  static int countInstruction(List<HInstruction> instructions,
                              HInstruction instruction) {
    int result = 0;
    for (int i = 0; i < instructions.length; i++) {
      if (instructions[i] === instruction) result++;
    }
    return result;
  }

  /**
   * Returns true if the predicate returns true for every instruction in the
   * list. The argument to [f] is an instruction with the count of how often
   * it appeared in the list [instructions].
   */
  static bool everyInstruction(List<HInstruction> instructions, Function f) {
    var copy = new List<HInstruction>.from(instructions);
    // TODO(floitsch): there is currently no way to sort HInstructions before
    // we have assigned an ID. The loop is therefore O(n^2) for now.
    for (int i = 0; i < copy.length; i++) {
      var current = copy[i];
      if (current === null) continue;
      int count = 1;
      for (int j = i + 1; j < copy.length; j++) {
        if (copy[j] === current) {
          copy[j] = null;
          count++;
        }
      }
      if (!f(current, count)) return false;
    }
    return true;
  }

  void visitInstruction(HInstruction instruction) {
    // Verifies that we are in the use list of our inputs.
    bool hasCorrectInputs(instruction) {
      bool inBasicBlock = instruction.isInBasicBlock();
      return everyInstruction(instruction.inputs, (input, count) {
        if (inBasicBlock) {
          return countInstruction(input.usedBy, instruction) == count;
        } else {
          return countInstruction(input.usedBy, instruction) == 0;
        }
      });
    }

    // Verifies that all our uses have us in their inputs.
    bool hasCorrectUses(instruction) {
      if (!instruction.isInBasicBlock()) return true;
      return everyInstruction(instruction.usedBy, (use, count) {
        return countInstruction(use.inputs, instruction) == count;
      });
    }

    isValid = isValid &&
              hasCorrectInputs(instruction) && hasCorrectUses(instruction);
  }
}
