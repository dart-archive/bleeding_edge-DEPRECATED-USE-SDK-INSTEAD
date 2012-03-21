// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

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
class SsaInstructionMerger extends HBaseVisitor {
  List<HInstruction> expectedInputs;

  void visitGraph(HGraph graph) {
    visitDominatorTree(graph);
  }

  bool usedOnlyByPhis(instruction) {
    for (HInstruction user in instruction.usedBy) {
      if (user is !HPhi) return false;
    }
    return true;
  }

  void visitInstruction(HInstruction instruction) {
    for (HInstruction input in instruction.inputs) {
      if (!input.generateAtUseSite() && input.usedBy.length == 1) {
        expectedInputs.add(input);
      }
    }
  }

  // The codegen might use the input multiple times, so it must not be
  // set generate at use site.
  void visitIs(HIs instruction) {}

  // A bailout target does not use its input like the other
  // instructions. Its inputs must be emitted prior to visiting it.
  void visitBailoutTarget(HBailoutTarget instruction) {}

  void visitBasicBlock(HBasicBlock block) {
    // Visit each instruction of the basic block in last-to-first order.
    // Keep a list of expected inputs of the current "expression" being
    // merged. If instructions occur in the expected order, they are
    // included in the expression.

    // The expectedInputs list holds non-trivial instructions that may
    // be generated at their use site, if they occur in the correct order.
    expectedInputs = new List<HInstruction>();
    // Add non-trivial inputs of instruction to expectedInputs, in
    // evaluation order.
    void addInputs(HInstruction instruction) {
      instruction.accept(this);
    }
    // Pop instructions from expectedInputs until instruction is found.
    // Return true if it is found, or false if not.
    bool findInInputs(HInstruction instruction) {
      while (!expectedInputs.isEmpty()) {
        HInstruction nextInput = expectedInputs.removeLast();
        assert(!nextInput.generateAtUseSite());
        assert(nextInput.usedBy.length == 1);
        if (nextInput == instruction) {
          return true;
        }
      }
      return false;
    }

    addInputs(block.last);
    for (HInstruction instruction = block.last.previous;
         instruction !== null;
         instruction = instruction.previous) {
      if (instruction.generateAtUseSite()) {
        continue;
      }
      // See if the current instruction is the next non-trivial
      // expected input. If not, drop the expectedInputs and
      // start over.
      if (findInInputs(instruction)) {
        instruction.tryGenerateAtUseSite();
      } else {
        assert(expectedInputs.isEmpty());
      }
      if (instruction is HForeign) {
        // Never try to merge inputs to HForeign.
        continue;
      } else if (instruction.generateAtUseSite() ||
                 usedOnlyByPhis(instruction)) {
        // In all other cases, try merging all non-trivial inputs.
        addInputs(instruction);
      }
    }
    expectedInputs = null;
  }
}

/**
 * In order to generate efficient code that works with bailouts, we
 * rewrite users of check instruction to use the input of the
 * instruction instead of the check itself.
 */
class SsaCheckInstructionUnuser extends HBaseVisitor {
  void visitGraph(HGraph graph) {
    visitDominatorTree(graph);
  }

  void visitTypeGuard(HTypeGuard node) {
    assert(!node.generateAtUseSite());
    HInstruction guarded = node.guarded;
    currentBlock.rewrite(node, guarded);
    // Remove generate at use site for the input, except for
    // parameters, since they do not introduce any computation.
    if (guarded is !HParameterValue) guarded.clearGenerateAtUseSite();
  }

  void visitBoundsCheck(HBoundsCheck node) {
    assert(!node.generateAtUseSite());
    currentBlock.rewrite(node, node.index);
    // The instruction merger may have not analyzed the 'length'
    // instruction because this bounds check instruction is not
    // generate at use site.
    if (node.length.usedBy.length == 1) node.length.tryGenerateAtUseSite();
  }

  void visitIntegerCheck(HIntegerCheck node) {
    assert(!node.generateAtUseSite());
    currentBlock.rewrite(node, node.value);
  }
}


/**
 *  Detect control flow arising from short-circuit logical operators, and
 *  prepare the program to be generated using these operators instead of
 *  nested ifs and boolean variables.
 */
class SsaConditionMerger extends HGraphVisitor {
  Map<HPhi, String> logicalOperations;

  SsaConditionMerger() : logicalOperations = new Map<HPhi, String>();

  void visitGraph(HGraph graph) {
    visitDominatorTree(graph);
  }

  /**
   * Returns true if the given instruction is an expression that uses up all
   * instructions up to the given [limit].
   *
   * That is, all instructions starting after the [limit] block (at the branch
   * leading to the [instruction]) down to the given [instruction] can be
   * generated at use-site.
   */
  bool isExpression(HInstruction instruction, HBasicBlock limit) {
    if (instruction is HPhi) return false;
    while (instruction.previous != null) {
      instruction = instruction.previous;
      if (!instruction.generateAtUseSite()) {
        return false;
      }
    }
    HBasicBlock block = instruction.block;
    if (!block.phis.isEmpty()) return false;
    return block.predecessors.length == 1 && block.predecessors[0] == limit;
  }

  void replaceWithLogicalOperator(HPhi phi, String type) {
    if (canGenerateAtUseSite(phi)) phi.tryGenerateAtUseSite();
    logicalOperations[phi] = type;
  }

  bool canGenerateAtUseSite(HPhi phi) {
    if (phi.usedBy.length != 1) return false;
    assert(phi.next == null);
    HInstruction use = phi.usedBy[0];

    HInstruction current = phi.block.first;
    while (current != use) {
      if (!current.generateAtUseSite()) return false;
      if (current.next != null) {
        current = current.next;
      } else if (current is HPhi) {
        current = current.block.first;
      } else {
        assert(current is HControlFlow);
        if (current is !HGoto) return false;
        HBasicBlock nextBlock = current.block.successors[0];
        if (!nextBlock.phis.isEmpty()) {
          current = nextBlock.phis.first;
        } else {
          current = nextBlock.first;
        }
      }
    }
    return true;
  }

  void detectLogicControlFlow(HPhi phi) {
    // Check for the most common pattern for a short-circuit logic operation:
    //   B0 b0 = ...; if (b0) goto B1 else B2 (or: if (!b0) goto B2 else B1)
    //   |\
    //   | B1 b1 = ...; goto B2
    //   |/
    //   B2 b2 = phi(b0,b1); if(b2) ...
    // TODO(lrn): Also recognize ?:-flow?

    if (phi.inputs.length != 2) return;
    HInstruction first = phi.inputs[0];
    HBasicBlock firstBlock = first.block;
    HInstruction second = phi.inputs[1];
    HBasicBlock secondBlock = second.block;
    // Check second input of phi being an expression followed by a goto.
    if (second.usedBy.length != 1) return;
    HInstruction secondNext =
        (second is HPhi) ? secondBlock.first : second.next;
    if (secondNext != secondBlock.last) return;
    if (secondBlock.last is !HGoto) return;
    if (secondBlock.successors[0] != phi.block) return;
    if (!isExpression(second, firstBlock)) return;
    // Check first input of phi being followed by a (possibly negated)
    // conditional branch based on the same value.
    if (firstBlock != phi.block.dominator) return;
    if (firstBlock.last is !HConditionalBranch) return;
    HConditionalBranch firstBranch = firstBlock.last;
    // Must be used both for value and for control to avoid the second branch.
    if (first.usedBy.length != 2) return;
    if (firstBlock.successors[1] != phi.block) return;
    HInstruction firstNext = (first is HPhi) ? firstBlock.first : first.next;
    if (firstNext == firstBranch &&
        firstBranch.condition == first) {
      replaceWithLogicalOperator(phi, "&&");
    } else if (firstNext is HNot &&
               firstNext.inputs[0] == first &&
               firstNext.generateAtUseSite() &&
               firstNext.next == firstBlock.last &&
               firstBranch.condition == firstNext) {
      replaceWithLogicalOperator(phi, "||");
    } else {
      return;
    }
    // Detected as logic control flow. Mark the corresponding
    // inputs as generated at use site. These will now be generated
    // as part of an expression.
    first.tryGenerateAtUseSite();
    firstBlock.last.tryGenerateAtUseSite();
    second.tryGenerateAtUseSite();
    secondBlock.last.tryGenerateAtUseSite();
  }

  void visitBasicBlock(HBasicBlock block) {
    if (!block.phis.isEmpty() &&
        block.phis.first == block.phis.last) {
      detectLogicControlFlow(block.phis.first);
    }
  }
}

// Precedence information for JavaScript operators.
class JSPrecedence {
   // Used as precedence for something that's not even an expression.
  static final int STATEMENT_PRECEDENCE      = 0;
  // Precedences of JS operators.
  static final int EXPRESSION_PRECEDENCE     = 1;
  static final int ASSIGNMENT_PRECEDENCE     = 2;
  static final int CONDITIONAL_PRECEDENCE    = 3;
  static final int LOGICAL_OR_PRECEDENCE     = 4;
  static final int LOGICAL_AND_PRECEDENCE    = 5;
  static final int BITWISE_OR_PRECEDENCE     = 6;
  static final int BITWISE_XOR_PRECEDENCE    = 7;
  static final int BITWISE_AND_PRECEDENCE    = 8;
  static final int EQUALITY_PRECEDENCE       = 9;
  static final int RELATIONAL_PRECEDENCE     = 10;
  static final int SHIFT_PRECEDENCE          = 11;
  static final int ADDITIVE_PRECEDENCE       = 12;
  static final int MULTIPLICATIVE_PRECEDENCE = 13;
  static final int PREFIX_PRECEDENCE         = 14;
  static final int POSTFIX_PRECEDENCE        = 15;
  static final int CALL_PRECEDENCE           = 16;
  // We never use "new MemberExpression" without arguments, so we can
  // combine CallExpression and MemberExpression without ambiguity.
  static final int MEMBER_PRECEDENCE         = CALL_PRECEDENCE;
  static final int PRIMARY_PRECEDENCE        = 17;

  // The operators that an occur in HBinaryOp.
  static final Map<String, JSBinaryOperatorPrecedence> binary = const {
    "||" : const JSBinaryOperatorPrecedence(LOGICAL_OR_PRECEDENCE,
                                            LOGICAL_AND_PRECEDENCE),
    "&&" : const JSBinaryOperatorPrecedence(LOGICAL_AND_PRECEDENCE,
                                            BITWISE_OR_PRECEDENCE),
    "|" : const JSBinaryOperatorPrecedence(BITWISE_OR_PRECEDENCE,
                                           BITWISE_XOR_PRECEDENCE),
    "^" : const JSBinaryOperatorPrecedence(BITWISE_XOR_PRECEDENCE,
                                           BITWISE_AND_PRECEDENCE),
    "&" : const JSBinaryOperatorPrecedence(BITWISE_AND_PRECEDENCE,
                                           EQUALITY_PRECEDENCE),
    "==" : const JSBinaryOperatorPrecedence(EQUALITY_PRECEDENCE,
                                            RELATIONAL_PRECEDENCE),
    "!=" : const JSBinaryOperatorPrecedence(EQUALITY_PRECEDENCE,
                                            RELATIONAL_PRECEDENCE),
    "===" : const JSBinaryOperatorPrecedence(EQUALITY_PRECEDENCE,
                                             RELATIONAL_PRECEDENCE),
    "!==" : const JSBinaryOperatorPrecedence(EQUALITY_PRECEDENCE,
                                             RELATIONAL_PRECEDENCE),
    "<" : const JSBinaryOperatorPrecedence(RELATIONAL_PRECEDENCE,
                                           SHIFT_PRECEDENCE),
    ">" : const JSBinaryOperatorPrecedence(RELATIONAL_PRECEDENCE,
                                           SHIFT_PRECEDENCE),
    "<=" : const JSBinaryOperatorPrecedence(RELATIONAL_PRECEDENCE,
                                            SHIFT_PRECEDENCE),
    ">=" : const JSBinaryOperatorPrecedence(RELATIONAL_PRECEDENCE,
                                            SHIFT_PRECEDENCE),
    "<<" : const JSBinaryOperatorPrecedence(SHIFT_PRECEDENCE,
                                            ADDITIVE_PRECEDENCE),
    ">>" : const JSBinaryOperatorPrecedence(SHIFT_PRECEDENCE,
                                            ADDITIVE_PRECEDENCE),
    ">>>" : const JSBinaryOperatorPrecedence(SHIFT_PRECEDENCE,
                                             ADDITIVE_PRECEDENCE),
    "+" : const JSBinaryOperatorPrecedence(ADDITIVE_PRECEDENCE,
                                           MULTIPLICATIVE_PRECEDENCE),
    "-" : const JSBinaryOperatorPrecedence(ADDITIVE_PRECEDENCE,
                                           MULTIPLICATIVE_PRECEDENCE),
    "*" : const JSBinaryOperatorPrecedence(MULTIPLICATIVE_PRECEDENCE,
                                           PREFIX_PRECEDENCE),
    "/" : const JSBinaryOperatorPrecedence(MULTIPLICATIVE_PRECEDENCE,
                                           PREFIX_PRECEDENCE),
    "%" : const JSBinaryOperatorPrecedence(MULTIPLICATIVE_PRECEDENCE,
                                           PREFIX_PRECEDENCE),
  };
}

class JSBinaryOperatorPrecedence {
  final int left;
  final int right;
  const JSBinaryOperatorPrecedence(this.left, this.right);
  // All binary operators (excluding assignment) are left associative.
  int get precedence() => left;
}
