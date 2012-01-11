// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaTypePropagator extends HGraphVisitor {

  final Map<int, HInstruction> workmap;
  final List<int> worklist;
  final Compiler compiler;

  SsaTypePropagator(Compiler this.compiler)
      : workmap = new Map<int, HInstruction>(),
        worklist = new List<int>();

  void visitGraph(HGraph graph) {
    new TypeAnnotationReader(compiler).visitGraph(graph);
    visitDominatorTree(graph);
    processWorklist();
    new TypeGuardInserter().visitGraph(graph);
  }

  visitBasicBlock(HBasicBlock block) {
    if (block.isLoopHeader()) {
      block.forEachPhi((HPhi phi) {
        phi.setInitialTypeForLoopPhi();
        addToWorklist(phi);
      });
    } else {
      block.forEachPhi((HPhi phi) {
        if (phi.updateType()) addUsersAndInputsToWorklist(phi);
      });
    }

    HInstruction instruction = block.first;
    while (instruction !== null) {
      if (instruction.updateType()) addUsersAndInputsToWorklist(instruction);
      instruction = instruction.next;
    }
  }

  void processWorklist() {
    while (!worklist.isEmpty()) {
      int id = worklist.removeLast();
      HInstruction instruction = workmap[id];
      assert(instruction !== null);
      workmap.remove(id);
      if (instruction.updateType()) addUsersAndInputsToWorklist(instruction);
    }
  }

  void addUsersAndInputsToWorklist(HInstruction instruction) {
    for (int i = 0, length = instruction.usedBy.length; i < length; i++) {
      addToWorklist(instruction.usedBy[i]);
    }
    for (int i = 0, length = instruction.inputs.length; i < length; i++) {
      addToWorklist(instruction.inputs[i]);
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

class TypeAnnotationReader extends HBaseVisitor {
  final Compiler compiler;

  TypeAnnotationReader(Compiler this.compiler);

  visitParameterValue(HParameterValue parameter) {
    // element is null for 'this'.
    if (parameter.element === null) return;

    Type type = parameter.element.computeType(compiler);
    if (type == null) return;

    if (type.toString() == 'int') {
      parameter.type = HType.INTEGER;
    } else if (type.toString() == 'String') {
      parameter.type = HType.STRING;
    }
  }

  void visitGraph(HGraph graph) {
    visitDominatorTree(graph);
  }
}

class TypeGuardInserter extends HBaseVisitor {

  TypeGuardInserter();

  void visitGraph(HGraph graph) {
    visitDominatorTree(graph);
  }

  visitBasicBlock(HBasicBlock block) {
    HInstruction instruction = block.phis.first;
    while (instruction !== null) {
      instruction = tryInsertTypeGuard(instruction, block.first);
    }
    instruction = block.first;
    while (instruction !== null) {
      instruction = tryInsertTypeGuard(instruction, instruction);
    }
  }

  HInstruction tryInsertTypeGuard(HInstruction instruction,
                                  HInstruction insertionPoint) {
    // If we found a type for the instruction, but the instruction
    // does not know if it produces that type, add a type guard.
    if (instruction.type.isKnown() && !instruction.hasExpectedType()) {
      HTypeGuard guard = new HTypeGuard(instruction.type, instruction);
      // Remove the instruction's type, the guard is now holding that
      // type.
      instruction.type = HType.UNKNOWN;
      instruction.block.rewrite(instruction, guard);
      insertionPoint.block.addAfter(insertionPoint, guard);
      return guard.next;
    }
    return instruction.next;
  }
}
