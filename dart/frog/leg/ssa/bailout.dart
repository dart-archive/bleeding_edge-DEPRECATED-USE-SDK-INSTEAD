// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class BailoutInfo {
  int instructionId;
  int bailoutId;
  BailoutInfo(this.instructionId, this.bailoutId);
}

/**
 * Keeps track of the execution environment for instructions. An
 * execution environment contains the SSA instructions that are live.
 */
class Environment {
  final Set<HInstruction> lives;
  Environment() : lives = new Set<HInstruction>();
  Environment.from(Environment other)
    : lives = new Set<HInstruction>.from(other.lives);

  void remove(HInstruction instruction) {
    lives.remove(instruction);
  }

  void add(HInstruction instruction) {
    if (!instruction.generateAtUseSite()) {
      lives.add(instruction);
    } else {
      for (int i = 0, len = instruction.inputs.length; i < len; i++) {
        add(instruction.inputs[i]);
      }
    }
  }

  void addAll(Environment other) {
    lives.addAll(other.lives);
  }

  List<HInstruction> buildAndSetLast(HInstruction instruction) {
    remove(instruction);
    List<HInstruction> result = new List<HInstruction>.from(lives);
    result.addLast(instruction);
    add(instruction);
    return result;
  }

  bool isEmpty() => lives.isEmpty();
  bool contains(HInstruction instruction) => lives.contains(instruction);
  void clear() => lives.clear();
}

/**
 * Computes the environment for each SSA instruction: visits the graph
 * in post-dominator order. Removes an instruction from the environment
 * and adds its inputs to the environment at the instruction's
 * definition.
 */
class SsaEnvironmentBuilder extends HBaseVisitor {
  final Compiler compiler;
  Environment environment;
  SubGraph subGraph;

  SsaEnvironmentBuilder(Compiler this.compiler);

  void visitGraph(HGraph graph) {
    subGraph = new SubGraph(graph.entry, graph.exit);
    environment = new Environment();
    visitBasicBlock(graph.entry);
    assert(environment.isEmpty());
  }

  void visitSubGraph(SubGraph newSubGraph) {
    SubGraph oldSubGraph = subGraph;
    subGraph = newSubGraph;
    visitBasicBlock(subGraph.start);
    subGraph = oldSubGraph;
  }

  void visitBasicBlock(HBasicBlock block) {
    if (!subGraph.contains(block)) return;
    block.last.accept(this);

    HInstruction instruction = block.last.previous;
    while (instruction != null) {
      HInstruction previous = instruction.previous;
      instruction.accept(this);
      instruction = previous;
    }

    for (HPhi phi = block.phis.first; phi != null; phi = phi.next) {
      phi.accept(this);
    }
  }

  void visitPhi(HPhi phi) {
    environment.remove(phi);
    // If the block is a loop header, we insert the incoming values of
    // the phis, and remove the loop values.
    // If the block is not a loop header, the phi will be handled by
    // the control flow instruction.
    if (phi.block.isLoopHeader()) {
      environment.add(phi.inputs[0]);
      for (int i = 1, len = phi.inputs.length; i < len; i++) {
        environment.remove(phi.inputs[i]);
      }
    }
  }

  void visitInstruction(HInstruction instruction) {
    environment.remove(instruction);
    for (int i = 0, len = instruction.inputs.length; i < len; i++) {
      environment.add(instruction.inputs[i]);
    }
  }

  void visitIf(HIf instruction) {
    HIfBlockInformation info = instruction.blockInformation;
    HBasicBlock joinBlock = info.joinBlock;

    Environment thenEnvironment;
    Environment elseEnvironment;
    // If the if does not have an else, phisInput will contain the
    // instructions coming from this block to the join block. These
    // instructions are inputs of the phis of the join block.
    Environment phisInput;
    if (joinBlock != null) {
      visitBasicBlock(joinBlock);
    }

    thenEnvironment = environment;
    if (instruction.hasElse) {
      // We duplicate the environment for visiting the else block. The
      // then block will be visited with the current environment.
      elseEnvironment = new Environment.from(environment);
    } else {
      // If the instruction does not have a 'then', the join block
      // contains phis from the current block. We create an
      // environment for these phis that will be added to the
      // environment for visiting the current block.
      phisInput = new Environment();
    }

    if (joinBlock != null) {
      for (HPhi phi = joinBlock.phis.first; phi != null; phi = phi.next) {
        if (joinBlock.predecessors[0] == instruction.block) {
          // We're dealing with an 'if' without an else branch.
          phisInput.add(phi.inputs[0]);
          thenEnvironment.add(phi.inputs[1]);
        } else if (joinBlock.predecessors[1] == instruction.block) {
          // The original source code contained a '&&' or '||' as the
          // condition.
          phisInput.add(phi.inputs[1]);
          thenEnvironment.add(phi.inputs[0]);
        } else {
          // A regular if with an else.
          assert(instruction.hasElse);
          thenEnvironment.add(phi.inputs[0]);
          elseEnvironment.add(phi.inputs[1]);
        }
      }
    }

    environment = thenEnvironment;
    visitSubGraph(info.thenGraph);

    if (instruction.hasElse) {
      // Save the live instructions for the then block.
      thenEnvironment = environment;
      // Use the duplicated environment that was created before
      // visiting the then block.
      environment = elseEnvironment;
      visitSubGraph(info.elseGraph);
      // Add the instructions for the then block to the
      // current environment.
      environment.addAll(thenEnvironment);
    } else {
      environment.addAll(phisInput);
    }
  }

  void visitGoto(HGoto goto) {
    HBasicBlock block = goto.block;
    if (block.successors[0].dominator != block) return;
    visitBasicBlock(block.successors[0]);
  }

  void visitBreak(HBreak breakInstruction) {
    unimplemented();
  }

  void visitLoopBranch(HLoopBranch branch) {
    HBasicBlock block = branch.block;

    // Visit the code after the loop.
    visitBasicBlock(block.successors[1]);
    // TODO(ngeoffray): Remove the instructions of the loop-exit from
    // the environment.

    HBasicBlock header = block.isLoopHeader() ? block : block.parentLoopHeader;
    // Put the loop phis in the environment.
    for (HPhi phi = header.phis.first; phi != null; phi = phi.next) {
      for (int i = 1, len = phi.inputs.length; i < len; i++) {
        environment.add(phi.inputs[i]);
      }
    }

    if (!branch.isDoWhile()) {
      assert(block.successors[0] == block.dominatedBlocks[0]);
      visitBasicBlock(block.successors[0]);
    }
  }

  // Deal with all kinds of control flow instructions. In case we add
  // a new one, we will hit an internal error.
  void visitExit(HExit exit) {}

  void visitReturn(HReturn instruction) {
    environment.clear();
    visitInstruction(instruction);
  }

  void visitThrow(HThrow instruction) {
    environment.clear();
    visitInstruction(instruction);
  }

  void visitControlFlow(HControlFlow instruction) {
    compiler.internalError('Control flow instructions already dealt with.',
                           instruction: instruction);
  }
}

/**
 * Visits the graph and replaces guards with guards that capture the
 * environment.
 */
class SsaTypeGuardBuilder extends SsaEnvironmentBuilder implements OptimizationPhase {

  final String name = 'SsaTypeGuardBuilder';

  SsaTypeGuardBuilder(Compiler compiler) : super(compiler);

  void tryInsertTypeGuard(HInstruction instruction,
                          HInstruction insertionPoint) {
    // If we found a type for the instruction, but the instruction
    // does not know if it produces that type, add a type guard.
    if (instruction.type.isKnown() && !instruction.hasExpectedType()) {
      // The type guard expects the guarded instruction to be at the
      // end of the inputs.
      List<HInstruction> inputs = environment.buildAndSetLast(instruction);
      HTypeGuard guard =
          new HTypeGuard(instruction.type, inputs, instruction.id);
      // Remove the instruction's type, the guard is now holding that
      // type.
      instruction.type = HType.UNKNOWN;
      instruction.block.rewrite(instruction, guard);
      insertionPoint.block.addBefore(insertionPoint, guard);
    }
  }


  void visitInstruction(HInstruction instruction) {
    tryInsertTypeGuard(instruction, instruction.next);
    super.visitInstruction(instruction);
  }

  void visitPhi(HPhi phi) {
    tryInsertTypeGuard(phi, phi.block.first);
    super.visitPhi(phi);
  }
}

/*
 * Visits the graph and inserts [HBailoutTarget] instructions where
 * the optimized version had [HTypeGuard] instructions.
 */
class SsaBailoutBuilder extends SsaEnvironmentBuilder implements OptimizationPhase {
  final Map<int, BailoutInfo> bailouts;
  final String name = 'SsaBailoutBuilder';

  SsaBailoutBuilder(Compiler compiler, this.bailouts) : super(compiler);

  void checkBailout(HInstruction instruction, HInstruction insertionPoint) {
    BailoutInfo info = bailouts[instruction.id];
    if (info != null) {
      List<HInstruction> inputs = environment.buildAndSetLast(instruction);
      HBailoutTarget bailout = new HBailoutTarget(info.bailoutId, inputs);
      instruction.block.addBefore(insertionPoint, bailout);
    }
  }

  void visitInstruction(HInstruction instruction) {
    checkBailout(instruction, instruction.next);
    super.visitInstruction(instruction);
  }

  void visitPhi(HPhi phi) {
    checkBailout(phi, phi.block.first);
    super.visitPhi(phi);
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
