// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class BailoutInfo implements Hashable {
  int blockId;
  int instructionId;
  BailoutInfo(this.blockId, this.instructionId);
  int hashCode() => ((blockId << 16) & 0xFFF0000) + instructionId;
  bool operator ==(other) {
    if (other is !BailoutInfo) return false;
    return blockId == other.blockId && instructionId == other.instructionId;
  }
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
  int instructionId;

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
      environment.add(phi);
    }

    HInstruction instruction = block.first;
    instructionId = 0;
    while (instruction != null) {
      HInstruction next = instruction.next;
      instruction.accept(this);
      instruction = next;
      instructionId++;
    }
  }

  void visitInstruction(HInstruction instruction) {
    // Cheapest liveness analysis.
    // TODO(ngeoffray): Compute liveness.
    if (!instruction.usedBy.isEmpty()) {
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
    enterBlock(instruction.endBlock);
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

  // Instructions that are dealt specialy. We know we don't need to
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

  void visitTypeGuard(HTypeGuard guard) {
    // The bailout version will not have the type guards, so we remove
    // this instruction from the ids.
    instructionId--;
    List<HInstruction> env = environment.build();
    if (env.isEmpty() || (env.last() != guard.guarded)) {
      // Guarding something that we don't put in the environment, e.g.
      // static.
      env.addLast(guard.guarded);
    }
    HTypeGuard newGuard = new HTypeGuard.forBailout(
        guard.type, env, guard.block.id, instructionId);
    guard.block.addBefore(guard, newGuard);
    guard.block.rewrite(guard, newGuard);
    guard.block.remove(guard);
  }
}

/*
 * Visits the graph and inserts [HBailoutTarget] instructions where
 * the optimized version had [HTypeGuard] instructions.
 */
class SsaBailoutBuilder extends SsaEnvironmentBuilder {
  final Set<BailoutInfo> bailouts;
  final BailoutInfo cached;

  SsaBailoutBuilder(Compiler compiler, this.bailouts)
    : super(compiler), cached = new BailoutInfo(null, null);

  void checkBailout(HInstruction instruction) {
    cached.blockId = instruction.block.id;
    cached.instructionId = instructionId;
    if (bailouts.contains(cached)) {
      HBailoutTarget bailout = new HBailoutTarget(environment.build());
      instruction.block.addAfter(instruction, bailout);
    }
  }

  // An [HStatic] is not put in the environment, but there may be a
  // type guard on it.
  void visitStatic(HStatic instruction) {
    checkBailout(instruction);
  }

  visitInstruction(HInstruction instruction) {
    super.visitInstruction(instruction);
    checkBailout(instruction);
  }
}
