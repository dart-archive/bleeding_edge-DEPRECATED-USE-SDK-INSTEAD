// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class BailoutInfo implements Hashable {
  int instructionId;
  int bailoutId;
  BailoutInfo(this.instructionId, this.bailoutId);
}

/*
 * A scope for SSA instructions.
 */
class SsaScope {
  final List<HInstruction> instructions;
  SsaScope() : instructions = <HInstruction>[];
  void add(HInstruction instruction) {
    instructions.add(instruction);
  }
}

/**
 * Keeps track of the execution environment for instructions. An
 * execution environment contains the SSA instructions that are live.
 * Control flow instructions implicitely create scopes that are pushed
 * at entry, and poped at exit.
 */
class Environment {
  // The list of live scopes.
  final List<SsaScope> stack;

  // The current scope for a given instruction.
  SsaScope current;

  Environment() : stack = new List<SsaScope>(), current = new SsaScope();

  void pushScope() {
    stack.addLast(current);
    current = new SsaScope();
  }

  void popScope() {
    current = stack.removeLast();
  }

  void add(HInstruction instruction) {
    current.add(instruction);
  }

  // Builds the list of currently live SSA variables based on the
  // scopes on the [stack] and the [current] scope.
  List<HInstruction> build() {
    List<HInstruction> result = <HInstruction>[];
    for (SsaScope scope in stack) {
      for (HInstruction i in scope.instructions) {
        result.add(i);
      }
    }
    for (HInstruction i in current.instructions) {
      result.add(i);
    }
    return result;
  }
}

/**
 * Builds the environment for each instruction. The graph traversal
 * is the same as in [SsaCodeGenerator], so it shares the same
 * assumptions on the graph structure.
 */
class SsaEnvironmentBuilder extends HBaseVisitor {
  final Compiler compiler;
  final Environment environment;

  SsaEnvironmentBuilder(Compiler this.compiler)
    : environment = new Environment();

  void visitGraph(HGraph graph) {
    visitBasicBlock(graph.entry);
  }

  void visitBasicBlock(HBasicBlock block) {
    if (block.isLoopHeader()) {
      environment.pushScope();
    }

    for (HPhi phi = block.phis.first; phi != null; phi = phi.next) {
      phi.accept(this);
    }

    HInstruction instruction = block.first;
    while (instruction != null) {
      HInstruction next = instruction.next;
      instruction.accept(this);
      instruction = next;
    }
  }

  void visitInstruction(HInstruction instruction) {
    // Cheapest liveness analysis.
    // TODO(ngeoffray): Compute liveness.
    if (!instruction.usedBy.isEmpty() || instruction is HParameterValue) {
      environment.add(instruction);
    }
  }

  void enterBlock(HBasicBlock block) {
    if (block == null) return;
    environment.pushScope();
    visitBasicBlock(block);
    environment.popScope();
  }

  void visitIf(HIf instruction) {
    enterBlock(instruction.thenBlock);
    enterBlock(instruction.elseBlock);
    enterBlock(instruction.joinBlock);
  }

  void visitGoto(HGoto goto) {
    // A goto instruction does not introduce any scope.
    HBasicBlock block = goto.block;
    assert(block.successors.length == 1);
    // If this block is not the dominator of the target, no need to
    // visit the target here. The dominator of the target is
    // responsible for visiting the target.
    if (block.successors[0].dominator != goto.block) return;
    visitBasicBlock(block.successors[0]);
  }

  void visitLoopBranch(HLoopBranch branch) {
    HBasicBlock branchBlock = branch.block;
    if (!branch.isDoWhile()) {
      // Not a do while loop. We visit the body of the loop.
      visitBasicBlock(branchBlock.dominatedBlocks[0]);
    }
    // Pop the scope that was pushed in [visitBasicBlock].
    environment.popScope();
    visitBasicBlock(branchBlock.successors[1]);
  }

  // Deal with all kinds of control flow instructions. In case we add
  // a new one, we will hit an internal error.
  void visitExit(HExit exit) {}
  void visitReturn(HReturn instruction) {}
  void visitThrow(HThrow instruction) {}

  void visitControlFlow(HControlFlow instruction) {
    compiler.internalError('Control flow instructions already dealt with.',
                           instruction: instruction);
  }

  // Instructions that are dealt specially. We know we don't need to
  // put them in the environment.
  void visitStatic(HStatic instruction) {}
  void visitLiteral(HLiteral literal) {}
  void visitThis(HThis instruction) {}
  void visitCheck(HCheck check) {}
  void visitBoolify(HBoolify instruction) {}
}

/**
 * Visits the graph and replaces guards with guards that capture the
 * environment.
 */
class SsaTypeGuardBuilder extends SsaEnvironmentBuilder {

  SsaTypeGuardBuilder(Compiler compiler) : super(compiler);

  void tryInsertTypeGuard(HInstruction instruction,
                          HInstruction insertionPoint) {
    // If we found a type for the instruction, but the instruction
    // does not know if it produces that type, add a type guard.
    if (instruction.type.isKnown() && !instruction.hasExpectedType()) {
      List<HInstruction> inputs = environment.build();
      if (inputs.isEmpty() || (inputs.last() != instruction)) {
        // Guarding something that we don't put in the environment, e.g.
        // static. We put it at the end of [inputs] because that's a
        // requirement of [HTypeGuard].
        inputs.addLast(instruction);
      }
      HTypeGuard guard = new HTypeGuard(
          instruction.type, inputs, instruction.id);
      // Remove the instruction's type, the guard is now holding that
      // type.
      instruction.type = HType.UNKNOWN;
      instruction.block.rewrite(instruction, guard);
      insertionPoint.block.addBefore(insertionPoint, guard);
    }
  }

  void visitInstruction(HInstruction instruction) {
    super.visitInstruction(instruction);
    tryInsertTypeGuard(instruction, instruction.next);
  }

  void visitPhi(HPhi phi) {
    super.visitInstruction(phi);
    tryInsertTypeGuard(phi, phi.block.first);
  }
}

/*
 * Visits the graph and inserts [HBailoutTarget] instructions where
 * the optimized version had [HTypeGuard] instructions.
 */
class SsaBailoutBuilder extends SsaEnvironmentBuilder {
  final Map<int, BailoutInfo> bailouts;

  SsaBailoutBuilder(Compiler compiler, this.bailouts) : super(compiler);

  void checkBailout(HInstruction instruction, HInstruction insertionPoint) {
    BailoutInfo info = bailouts[instruction.id];
    if (info != null) {
      HBailoutTarget bailout =
          new HBailoutTarget(info.bailoutId, environment.build());
      instruction.block.addBefore(insertionPoint, bailout);
    }
  }

  // An [HStatic] is not put in the environment, but there may be a
  // type guard on it.
  void visitStatic(HStatic instruction) {
    checkBailout(instruction, instruction.next);
  }

  visitInstruction(HInstruction instruction) {
    super.visitInstruction(instruction);
    checkBailout(instruction, instruction.next);
  }

  visitPhi(HPhi phi) {
    super.visitInstruction(phi);
    checkBailout(phi, phi.block.first);
  }
}

/**
 * Propagates bailout information to blocks that need it. This visitor
 * is run before codegen, to know which blocks have to deal with
 * bailouts.
 */
class SsaBailoutPropagator extends HBaseVisitor {
  final Compiler compiler;
  final List<HBasicBlock> blocks;

  SsaBailoutPropagator(Compiler this.compiler) : blocks = <HBasicBlock>[];

  void visitGraph(HGraph graph) {
    blocks.addLast(graph.entry);
    visitBasicBlock(graph.entry);
  }

  void visitBasicBlock(HBasicBlock block) {
    if (block.isLoopHeader()) blocks.addLast(block);
    HInstruction instruction = block.first;
    while (instruction != null) {
      instruction.accept(this);
      instruction = instruction.next;
    }
  }

  void enterBlock(HBasicBlock block) {
    if (block == null) return;
    blocks.addLast(block);
    visitBasicBlock(block);
    blocks.removeLast();
  }

  void visitIf(HIf instruction) {
    enterBlock(instruction.thenBlock);
    enterBlock(instruction.elseBlock);
    enterBlock(instruction.joinBlock);
  }

  void visitGoto(HGoto goto) {
    HBasicBlock block = goto.block;
    if (block.successors[0].dominator != block) return;
    visitBasicBlock(block.successors[0]);
  }

  void visitLoopBranch(HLoopBranch branch) {
    HBasicBlock branchBlock = branch.block;
    if (!branch.isDoWhile()) {
      // Not a do while loop. We visit the body of the loop.
      visitBasicBlock(branchBlock.dominatedBlocks[0]);
    }
    blocks.removeLast();
    visitBasicBlock(branchBlock.successors[1]);
  }

  // Deal with all kinds of control flow instructions. In case we add
  // a new one, we will hit an internal error.
  void visitExit(HExit exit) {}
  void visitReturn(HReturn instruction) {}
  void visitThrow(HThrow instruction) {}

  void visitControlFlow(HControlFlow instruction) {
    compiler.internalError('Control flow instructions already dealt with.',
                           instruction: instruction);
  }

  visitBailoutTarget(HBailoutTarget target) {
    blocks.forEach((HBasicBlock block) {
      block.bailouts.add(target);
    });
  }
}
