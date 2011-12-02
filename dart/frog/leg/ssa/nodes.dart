// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

interface HVisitor<R> {
  R visitAdd(HAdd node);
  R visitBasicBlock(HBasicBlock node);
  R visitBitAnd(HBitAnd node);
  R visitBitNot(HBitNot node);
  R visitBitOr(HBitOr node);
  R visitBitXor(HBitXor node);
  R visitBoolify(HBoolify node);
  R visitDivide(HDivide node);
  R visitEquals(HEquals node);
  R visitExit(HExit node);
  R visitForeign(HForeign node);
  R visitGoto(HGoto node);
  R visitGreater(HGreater node);
  R visitGreaterEqual(HGreaterEqual node);
  R visitIf(HIf node);
  R visitInvokeStatic(HInvokeStatic node);
  R visitLess(HLess node);
  R visitLessEqual(HLessEqual node);
  R visitLoad(HLoad node);
  R visitLocal(HLocal node);
  R visitLoopBranch(HLoopBranch node);
  R visitLiteral(HLiteral node);
  R visitLiteralList(HLiteralList node);
  R visitModulo(HModulo node);
  R visitMultiply(HMultiply node);
  R visitNegate(HNegate node);
  R visitNot(HNot node);
  R visitParameterValue(HParameterValue node);
  R visitPhi(HPhi node);
  R visitReturn(HReturn node);
  R visitShiftLeft(HShiftLeft node);
  R visitShiftRight(HShiftRight node);
  R visitStatic(HStatic node);
  R visitStore(HStore node);
  R visitSubtract(HSubtract node);
  R visitThrow(HThrow node);
  R visitTruncatingDivide(HTruncatingDivide node);
  R visitTypeGuard(HTypeGuard node);
}

class HGraphVisitor {
  visitDominatorTree(HGraph graph) {
    void visitBasicBlockAndSuccessors(HBasicBlock block) {
      visitBasicBlock(block);
      List dominated = block.dominatedBlocks;
      for (int i = 0; i < dominated.length; i++) {
        visitBasicBlockAndSuccessors(dominated[i]);
      }
    }

    visitBasicBlockAndSuccessors(graph.entry);
  }

  visitPostDominatorTree(HGraph graph) {
    void visitBasicBlockAndSuccessors(HBasicBlock block) {
      List dominated = block.dominatedBlocks;
      for (int i = dominated.length - 1; i >= 0; i--) {
        visitBasicBlockAndSuccessors(dominated[i]);
      }
      visitBasicBlock(block);
    }

    visitBasicBlockAndSuccessors(graph.entry);
  }

  abstract visitBasicBlock(HBasicBlock block);
}

class HInstructionVisitor extends HGraphVisitor {
  HBasicBlock currentBlock;

  abstract visitInstruction(HInstruction node);

  visitBasicBlock(HBasicBlock node) {
    void visitInstructionList(HInstructionList list) {
      HInstruction instruction = list.first;
      while (instruction !== null) {
        visitInstruction(instruction);
        instruction = instruction.next;
        assert(instruction != list.first);
      }
    }

    currentBlock = node;
    visitInstructionList(node);
  }
}

class HGraph {
  HBasicBlock entry;
  HBasicBlock exit;
  final List<HBasicBlock> blocks;

  HGraph() : blocks = new List<HBasicBlock>() {
    entry = addNewBlock();
    // The exit block will be added later, so it has an id that is
    // after all others in the system.
    exit = new HBasicBlock();
  }

  void addBlock(HBasicBlock block) {
    int id = blocks.length;
    block.id = id;
    blocks.add(block);
    assert(blocks[id] === block);
  }

  HBasicBlock addNewBlock() {
    HBasicBlock result = new HBasicBlock();
    addBlock(result);
    return result;
  }

  HBasicBlock addNewLoopHeaderBlock() {
    HBasicBlock result = addNewBlock();
    result.loopInformation = new HLoopInformation(result);
    return result;
  }

  void finalize() {
    addBlock(exit);
    exit.open();
    exit.close(new HExit());
    assignDominators();
  }

  void assignDominators() {
    // Run through the blocks in order of increasing ids so we are
    // guaranteed that we have computed dominators for all blocks
    // higher up in the dominator tree.
    for (int i = 0, length = blocks.length; i < length; i++) {
      HBasicBlock block = blocks[i];
      List<HBasicBlock> predecessors = block.predecessors;
      if (block.isLoopHeader()) {
        assert(predecessors.length >= 2);
        block.assignCommonDominator(predecessors[0]);
      } else {
        for (int j = predecessors.length - 1; j >= 0; j--) {
          block.assignCommonDominator(predecessors[j]);
        }
      }
    }
  }

  bool isValid() {
    HValidator validator = new HValidator();
    validator.visitGraph(this);
    return validator.isValid;
  }
}

class HBaseVisitor extends HGraphVisitor implements HVisitor {
  HBasicBlock currentBlock;

  visitBasicBlock(HBasicBlock node) {
    currentBlock = node;

    HInstruction instruction = node.first;
    while (instruction !== null) {
      instruction.accept(this);
      instruction = instruction.next;
    }
  }

  visitInstruction(HInstruction) {}

  visitBinaryArithmetic(HBinaryArithmetic node) => visitInvokeBinary(node);
  visitBinaryBitOp(HBinaryBitOp node) => visitBinaryArithmetic(node);
  visitInvokeBinary(HInvokeBinary node) => visitInvokeStatic(node);
  visitInvokeUnary(HInvokeUnary node) => visitInvokeStatic(node);
  visitConditionalBranch(HConditionalBranch node) => visitControlFlow(node);
  visitControlFlow(HControlFlow node) => visitInstruction(node);
  visitRelational(HRelational node) => visitInvokeBinary(node);

  visitAdd(HAdd node) => visitBinaryArithmetic(node);
  visitBitAnd(HBitAnd node) => visitBinaryBitOp(node);
  visitBitNot(HBitNot node) => visitInvokeUnary(node);
  visitBitOr(HBitOr node) => visitBinaryBitOp(node);
  visitBitXor(HBitXor node) => visitBinaryBitOp(node);
  visitBoolify(HBoolify node) => visitInstruction(node);
  visitDivide(HDivide node) => visitBinaryArithmetic(node);
  visitEquals(HEquals node) => visitRelational(node);
  visitExit(HExit node) => visitControlFlow(node);
  visitForeign(HForeign node) => visitInstruction(node);
  visitGoto(HGoto node) => visitControlFlow(node);
  visitGreater(HGreater node) => visitRelational(node);
  visitGreaterEqual(HGreaterEqual node) => visitRelational(node);
  visitIf(HIf node) => visitConditionalBranch(node);
  visitInvokeStatic(HInvokeStatic node) => visitInstruction(node);
  visitLess(HLess node) => visitRelational(node);
  visitLessEqual(HLessEqual node) => visitRelational(node);
  visitLoad(HLoad node) => visitInstruction(node);
  visitLocal(HLocal node) => visitInstruction(node);
  visitLiteral(HLiteral node) => visitInstruction(node);
  visitLiteralList(HLiteralList node) => visitInstruction(node);
  visitLoopBranch(HLoopBranch node) => visitConditionalBranch(node);
  visitModulo(HModulo node) => visitBinaryArithmetic(node);
  visitNegate(HNegate node) => visitInvokeUnary(node);
  visitNot(HNot node) => visitInstruction(node);
  visitPhi(HPhi node) => visitInstruction(node);
  visitMultiply(HMultiply node) => visitBinaryArithmetic(node);
  visitParameterValue(HParameterValue node) => visitInstruction(node);
  visitReturn(HReturn node) => visitControlFlow(node);
  visitShiftRight(HShiftRight node) => visitBinaryBitOp(node);
  visitShiftLeft(HShiftLeft node) => visitBinaryBitOp(node);
  visitSubtract(HSubtract node) => visitBinaryArithmetic(node);
  visitStatic(HStatic node) => visitInstruction(node);
  visitStore(HStore node) => visitInstruction(node);
  visitThrow(HThrow node) => visitControlFlow(node);
  visitTruncatingDivide(HTruncatingDivide node) => visitBinaryArithmetic(node);
  visitTypeGuard(HTypeGuard node) => visitInstruction(node);
}

class HInstructionList {
  HInstruction first = null;
  HInstruction last = null;

  bool isEmpty() {
    return first === null;
  }

  void addAfter(HInstruction cursor, HInstruction instruction) {
    if (cursor === null) {
      assert(isEmpty());
      first = last = instruction;
    } else if (cursor === last) {
      last.next = instruction;
      instruction.previous = last;
      last = instruction;
    } else {
      instruction.previous = cursor;
      instruction.next = cursor.next;
      cursor.next.previous = instruction;
      cursor.next = instruction;
    }
  }

  void addBefore(HInstruction cursor, HInstruction instruction) {
    if (cursor === null) {
      assert(isEmpty());
      first = last = instruction;
    } else if (cursor === first) {
      first.previous = instruction;
      instruction.next = first;
      first = instruction;
    } else {
      instruction.next = cursor;
      instruction.previous = cursor.previous;
      cursor.previous.next = instruction;
      cursor.previous = instruction;
    }
  }

  void detach(HInstruction instruction) {
    assert(contains(instruction));
    assert(instruction.isInBasicBlock());
    if (instruction.previous === null) {
      first = instruction.next;
    } else {
      instruction.previous.next = instruction.next;
    }
    if (instruction.next === null) {
      last = instruction.previous;
    } else {
      instruction.next.previous = instruction.previous;
    }
    instruction.previous = null;
    instruction.next = null;
  }

  void remove(HInstruction instruction) {
    assert(instruction.usedBy.isEmpty());
    detach(instruction);
  }

  /** Linear search for [instruction]. */
  bool contains(HInstruction instruction) {
    HInstruction cursor = first;
    while (cursor != null) {
      if (cursor === instruction) return true;
      cursor = cursor.next;
    }
    return false;
  }
}

class HBasicBlock extends HInstructionList {
  // The [id] must be such that any successor's id is greater than
  // this [id]. The exception are back-edges.
  int id;

  static final int STATUS_NEW = 0;
  static final int STATUS_OPEN = 1;
  static final int STATUS_CLOSED = 2;
  int status = STATUS_NEW;

  HInstructionList phis;

  HLoopInformation loopInformation = null;
  HBasicBlock parentLoopHeader = null;

  final List<HBasicBlock> predecessors;
  List<HBasicBlock> successors;

  HBasicBlock dominator = null;
  final List<HBasicBlock> dominatedBlocks;

  HBasicBlock() : this.withId(null);
  HBasicBlock.withId(this.id)
      : phis = new HInstructionList(),
        predecessors = <HBasicBlock>[],
        successors = const <HBasicBlock>[],
        dominatedBlocks = <HBasicBlock>[];

  bool isNew() => status == STATUS_NEW;
  bool isOpen() => status == STATUS_OPEN;
  bool isClosed() => status == STATUS_CLOSED;

  bool isLoopHeader() => loopInformation !== null;

  void open() {
    assert(isNew());
    status = STATUS_OPEN;
  }

  void close(HControlFlow end) {
    assert(isOpen());
    addAfter(last, end);
    status = STATUS_CLOSED;
  }

  // TODO(kasperl): I really don't want to pass the compiler into this
  // method. Maybe we need a better logging framework.
  void printToCompiler(Compiler compiler) {
    HInstruction instruction = first;
    while (instruction != null) {
      var id = instruction.id;
      compiler.log('$id: $instruction ${instruction.inputsToString()}');
      instruction = instruction.next;
    }
  }

  accept(HVisitor visitor) => visitor.visitBasicBlock(this);

  void addAtEntry(HInstruction instruction) {
    assert(isClosed());
    assert(instruction is !HPhi);
    super.addBefore(first, instruction);
    instruction.notifyAddedToBlock(this);
  }

  void addAtExit(HInstruction instruction) {
    assert(isClosed());
    assert(last is HControlFlow);
    assert(instruction is !HPhi);
    super.addBefore(last, instruction);
    instruction.notifyAddedToBlock(this);
  }

  void moveAtExit(HInstruction instruction) {
    assert(instruction is !HPhi);
    assert(instruction.isInBasicBlock());
    assert(isClosed());
    assert(last is HControlFlow);
    super.addBefore(last, instruction);
    instruction.block = this;
    assert(isValid());
  }

  void add(HInstruction instruction) {
    assert(instruction is !HControlFlow);
    assert(instruction is !HPhi);
    super.addAfter(last, instruction);
    instruction.notifyAddedToBlock(this);
  }

  void addPhi(HPhi phi) {
    phis.addAfter(phis.last, phi);
    phi.notifyAddedToBlock(this);
  }

  void removePhi(HPhi phi) {
    phis.remove(phi);
    phi.notifyRemovedFromBlock(this);
  }

  void addAfter(HInstruction cursor, HInstruction instruction) {
    assert(cursor is !HPhi);
    assert(instruction is !HPhi);
    assert(isOpen() || isClosed());
    super.addAfter(cursor, instruction);
    instruction.notifyAddedToBlock(this);
  }

  void addBefore(HInstruction cursor, HInstruction instruction) {
    assert(cursor is !HPhi);
    assert(instruction is !HPhi);
    assert(isOpen() || isClosed());
    super.addBefore(cursor, instruction);
    instruction.notifyAddedToBlock(this);
  }

  void remove(HInstruction instruction) {
    assert(isOpen() || isClosed());
    assert(instruction is !HPhi);
    super.remove(instruction);
    instruction.notifyRemovedFromBlock(this);
  }

  void addSuccessor(HBasicBlock block) {
    // Forward branches are only allowed to new blocks.
    assert(isClosed() && (block.isNew() || block.id < id));
    if (successors.isEmpty()) {
      successors = [block];
    } else {
      successors.add(block);
    }
    block.predecessors.add(this);
  }

  void postProcessLoopHeader() {
    assert(isLoopHeader());
    // Only the first entry into the loop is from outside the
    // loop. All other entries must be back edges.
    for (int i = 1, length = predecessors.length; i < length; i++) {
      loopInformation.addBackEdge(predecessors[i]);
    }
  }

  /**
   * Rewrites all uses of the [from] instruction to using the [to]
   * instruction instead.
   */
  void rewrite(HInstruction from, HInstruction to) {
    for (HInstruction use in from.usedBy) {
      rewriteInput(use, from, to);
    }
    to.usedBy.addAll(from.usedBy);
    from.usedBy.clear();
  }

  static void rewriteInput(HInstruction instruction,
                           HInstruction from,
                           HInstruction to) {
    List inputs = instruction.inputs;
    for (int i = 0; i < inputs.length; i++) {
      if (inputs[i] === from) inputs[i] = to;
    }
  }

  bool isExitBlock() {
    return first === last && first is HExit;
  }

  void addDominatedBlock(HBasicBlock block) {
    assert(isClosed());
    assert(id !== null && block.id !== null);
    assert(dominatedBlocks.indexOf(block) < 0);
    // Keep the list of dominated blocks sorted such that if there are two
    // succeeding blocks in the list, the predecessor is before the successor.
    // Assume that we add the dominated blocks in the right order.
    int index = dominatedBlocks.length;
    while (index > 0 && dominatedBlocks[index - 1].id > block.id) {
      index--;
    }
    if (index == dominatedBlocks.length) {
      dominatedBlocks.add(block);
    } else {
      dominatedBlocks.insertRange(index, 1, block);
    }
    assert(block.dominator === null);
    block.dominator = this;
  }

  void removeDominatedBlock(HBasicBlock block) {
    assert(isClosed());
    assert(id !== null && block.id !== null);
    int index = dominatedBlocks.indexOf(block);
    assert(index >= 0);
    if (index == dominatedBlocks.length - 1) {
      dominatedBlocks.removeLast();
    } else {
      dominatedBlocks.removeRange(index, 1);
    }
    assert(block.dominator === this);
    block.dominator = null;
  }

  void assignCommonDominator(HBasicBlock predecessor) {
    assert(isClosed());
    if (dominator === null) {
      // If this basic block doesn't have a dominator yet we use the
      // given predecessor as the dominator.
      predecessor.addDominatedBlock(this);
    } else if (predecessor.dominator !== null) {
      // If the predecessor has a dominator and this basic block has a
      // dominator, we find a common parent in the dominator tree and
      // use that as the dominator.
      HBasicBlock first = dominator;
      HBasicBlock second = predecessor;
      while (first !== second) {
        if (first.id > second.id) {
          first = first.dominator;
        } else {
          second = second.dominator;
        }
        assert(first !== null && second !== null);
      }
      if (dominator !== first) {
        dominator.removeDominatedBlock(this);
        first.addDominatedBlock(this);
      }
    }
  }

  void forEachPhi(void f(HPhi phi)) {
    HPhi current = phis.first;
    while (current !== null) {
      f(current);
      current = current.next;
    }
  }

  bool isValid() {
    assert(isClosed());
    HValidator validator = new HValidator();
    validator.visitBasicBlock(this);
    return validator.isValid;
  }
}

class HLoopInformation {
  final HBasicBlock header;
  final List<HBasicBlock> blocks;
  final List<HBasicBlock> backEdges;

  HLoopInformation(this.header)
      : blocks = new List<HBasicBlock>(),
        backEdges = new List<HBasicBlock>();

  void addBackEdge(HBasicBlock predecessor) {
    backEdges.add(predecessor);
    addBlock(predecessor);
  }

  // Adds a block and transitively all its predecessors in the loop as
  // loop blocks.
  void addBlock(HBasicBlock block) {
    if (block === header) return;
    HBasicBlock parentHeader = block.parentLoopHeader;
    if (parentHeader === header) {
      // Nothing to do in this case.
    } else if (parentHeader !== null) {
      addBlock(parentHeader);
    } else {
      block.parentLoopHeader = header;
      blocks.add(block);
      for (int i = 0, length = block.predecessors.length; i < length; i++) {
        addBlock(block.predecessors[i]);
      }
    }
  }

  HBasicBlock getLastBackEdge() {
    int maxId = -1;
    HBasicBlock result = null;
    for (int i = 0, length = backEdges.length; i < length; i++) {
      HBasicBlock current = backEdges[i];
      if (current.id > maxId) {
        maxId = current.id;
        result = current;
      }
    }
    return result;
  }
}

class HInstruction implements Hashable {
  final int id;
  static int idCounter;

  final List<HInstruction> inputs;
  final List<HInstruction> usedBy;

  HBasicBlock block;
  HInstruction previous = null;
  HInstruction next = null;
  int flags = 0;
  int type = TYPE_UNKNOWN;

  // Changes flags.
  static final int FLAG_CHANGES_SOMETHING    = 0;
  static final int FLAG_CHANGES_COUNT        = FLAG_CHANGES_SOMETHING + 1;

  // Depends flags (one for each changes flag).
  static final int FLAG_DEPENDS_ON_SOMETHING = FLAG_CHANGES_COUNT;

  // Other flags.
  static final int FLAG_GENERATE_AT_USE_SITE = FLAG_DEPENDS_ON_SOMETHING + 1;
  static final int FLAG_USE_GVN              = FLAG_GENERATE_AT_USE_SITE + 1;

  // Types.
  static final int TYPE_UNKNOWN = 0;
  static final int TYPE_BOOLEAN = 1;
  static final int TYPE_NUMBER = 2;
  static final int TYPE_STRING = 3;
  static final int TYPE_CONFLICT = 4;

  HInstruction(this.inputs) : id = idCounter++, usedBy = <HInstruction>[];

  int hashCode() => id;

  bool getFlag(int position) => (flags & (1 << position)) != 0;
  void setFlag(int position) { flags |= (1 << position); }
  void clearFlag(int position) { flags &= ~(1 << position); }

  static int computeDependsOnFlags(int flags) => flags << FLAG_CHANGES_COUNT;

  int getChangesFlags() => flags & ((1 << FLAG_CHANGES_COUNT) - 1);
  bool hasSideEffects() => getChangesFlags() != 0;
  void prepareGvn() { setAllSideEffects(); }

  void setAllSideEffects() { flags |= ((1 << FLAG_CHANGES_COUNT) - 1); }
  void clearAllSideEffects() { flags &= ~((1 << FLAG_CHANGES_COUNT) - 1); }

  bool generateAtUseSite() => getFlag(FLAG_GENERATE_AT_USE_SITE);
  void setGenerateAtUseSite() { setFlag(FLAG_GENERATE_AT_USE_SITE); }
  void clearGenerateAtUseSite()  { clearFlag(FLAG_GENERATE_AT_USE_SITE); }

  bool useGvn() => getFlag(FLAG_USE_GVN);
  void setUseGvn() { setFlag(FLAG_USE_GVN); }

  bool isUnknown() => type == TYPE_UNKNOWN || type == TYPE_CONFLICT;
  bool isBoolean() => type == TYPE_BOOLEAN;
  bool isNumber() => type == TYPE_NUMBER;
  bool isString() => type == TYPE_STRING;

  // Compute the type of the instruction.
  int computeType() => computeDesiredType();

  int computeDesiredType() {
    int candidateType = TYPE_UNKNOWN;
    for (final user in usedBy) {
      int type = user.computeDesiredInputType(this);
      if (candidateType == TYPE_UNKNOWN) {
        candidateType = type;
      } else if (type != TYPE_UNKNOWN && candidateType != type) {
        candidateType = TYPE_UNKNOWN;
        break;
      }
    }
    return candidateType;
  }

  int computeDesiredInputType(HInstruction input) => TYPE_UNKNOWN;

  // Returns whether the instruction does produce the type it claims.
  // For most instructions, this returns false. A type guard will be
  // inserted to make sure the users get the right type in.
  bool hasExpectedType() => false;

  // Re-compute and update the type of the instruction. Returns
  // whether or not the type was changed.
  bool updateType() {
    if (type == TYPE_CONFLICT) return false;
    int newType = computeType();
    bool changed = (type != newType);
    if (type == TYPE_UNKNOWN) {
      type = newType;
      return changed;
    } else if (changed) {
      // We found a different type from what we found before. Be
      // pessismistic for now and mark it as conflicting.
      type = TYPE_CONFLICT;
      return changed;
    }
    return false;
  }

  bool isInBasicBlock() => block !== null;

  String inputsToString() {
    void addAsCommaSeparated(StringBuffer buffer, List<HInstruction> list) {
      for (int i = 0; i < list.length; i++) {
        if (i != 0) buffer.add(', ');
        buffer.add("@${list[i].id}");
      }
    }

    StringBuffer buffer = new StringBuffer();
    buffer.add('(');
    addAsCommaSeparated(buffer, inputs);
    buffer.add(') - used at [');
    addAsCommaSeparated(buffer, usedBy);
    buffer.add(']');
    return buffer.toString();
  }

  bool equals(HInstruction other) {
    assert(useGvn() && other.useGvn());
    // Check that the type and the flags match.
    if (!typeEquals(other)) return false;
    if (flags != other.flags) return false;
    // Check that the inputs match.
    final int inputsLength = inputs.length;
    final List<HInstruction> otherInputs = other.inputs;
    if (inputsLength != otherInputs.length) return false;
    for (int i = 0; i < inputsLength; i++) {
      if (inputs[i] !== otherInputs[i]) return false;
    }
    // Check that the data in the instruction matches.
    return dataEquals(other);
  }

  // These methods should be overwritten by instructions that
  // participate in global value numbering.
  bool typeEquals(HInstruction other) => false;
  bool dataEquals(HInstruction other) => false;

  abstract accept(HVisitor visitor);

  void notifyAddedToBlock(HBasicBlock block) {
    assert(!isInBasicBlock());
    assert(this.block === null);
    // Add [this] to the inputs' uses.
    for (int i = 0; i < inputs.length; i++) {
      assert(inputs[i].isInBasicBlock());
      inputs[i].usedBy.add(this);
    }
    this.block = block;
    assert(isValid());
  }

  void notifyRemovedFromBlock(HBasicBlock block) {
    assert(isInBasicBlock());
    assert(usedBy.isEmpty());
    assert(this.block === block);

    // Remove [this] from the inputs' uses.
    for (int i = 0; i < inputs.length; i++) {
      List inputUsedBy = inputs[i].usedBy;
      for (int j = 0; j < inputUsedBy.length; j++) {
        if (inputUsedBy[j] === this) {
          inputUsedBy[j] = inputUsedBy[inputUsedBy.length - 1];
          inputUsedBy.removeLast();
          break;
        }
      }
    }
    this.block = null;
    assert(isValid());
  }

  bool isLiteralNull() => false;
  bool isLiteralNumber() => false;
  bool isLiteralString() => false;

  bool isValid() {
    HValidator validator = new HValidator();
    validator.currentBlock = block;
    validator.visitInstruction(this);
    return validator.isValid;
  }
}

class HBoolify extends HInstruction {
  HBoolify(HInstruction value) : super(<HInstruction>[value]);
  void prepareGvn() {
    assert(!hasSideEffects());
    setUseGvn();
  }

  int computeType() => TYPE_BOOLEAN;
  bool hasExpectedType() => true;

  accept(HVisitor visitor) => visitor.visitBoolify(this);
  bool typeEquals(other) => other is HBoolify;
  bool dataEquals(HInstruction other) => true;
}

class HTypeGuard extends HInstruction {
  HTypeGuard(type, value) : super([value]) {
    this.type = type;
  }

  void prepareGvn() {
    assert(!hasSideEffects());
    setUseGvn();
  }

  int computeType() => type;
  bool hasExpectedType() => true;

  accept(HVisitor visitor) => visitor.visitTypeGuard(this);
  bool typeEquals(other) => other is HTypeGuard;
  bool dataEquals(HTypeGuard other) => type == other.type;
}

class HConditionalBranch extends HControlFlow {
  HConditionalBranch(inputs) : super(inputs);
  abstract toString();
}

class HControlFlow extends HInstruction {
  HControlFlow(inputs) : super(inputs);
  abstract toString();
}

class HInvokeStatic extends HInstruction {
  /** The first input must be the target. */
  HInvokeStatic(inputs) : super(inputs);
  toString() => 'invoke static: ${element.name}';
  accept(HVisitor visitor) => visitor.visitInvokeStatic(this);
  Element get element() => target.element;
  HStatic get target() => inputs[0];
}

class HForeign extends HInstruction {
  final SourceString code;
  HForeign(inputs, this.code) : super(inputs);
  accept(HVisitor visitor) => visitor.visitForeign(this);
}

class HInvokeBinary extends HInvokeStatic {
  bool builtin = false;
  HInvokeBinary(HStatic target, HInstruction left, HInstruction right)
      : super(<HInstruction>[target, left, right]);

  HInstruction fold() {
    if (left.isLiteralNumber() && right.isLiteralNumber()) {
      HLiteral op1 = left;
      HLiteral op2 = right;
      return new HLiteral(evaluate(op1.value, op2.value));
    }
    return this;
  }

  HInstruction get left() => inputs[1];
  HInstruction get right() => inputs[2];

  int computeInputsType() {
    int leftType = left.type;
    int rightType = right.type;
    if (leftType == TYPE_UNKNOWN || rightType == TYPE_UNKNOWN) {
      return TYPE_UNKNOWN;
    }
    if (leftType != rightType) return TYPE_CONFLICT;
    return leftType;
  }

  abstract evaluate(num a, num b);
}

class HBinaryArithmetic extends HInvokeBinary {
  HBinaryArithmetic(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);

  void prepareGvn() {
    // An arithmetic expression can take part in global value
    // numbering and do not have any side-effects if we that all
    // inputs are numbers.
    if (builtin) {
      assert(!hasSideEffects());
      setUseGvn();
    } else {
      setAllSideEffects();
    }
  }

  int computeType() {
    int type = computeInputsType();
    builtin = (type == TYPE_NUMBER);
    if (left.isNumber()) return TYPE_NUMBER;
    if (type != TYPE_UNKNOWN) return type;
    return super.computeType();
  }

  int computeDesiredInputType(HInstruction input) {
    // TODO(floitsch): we want the target to be a function.
    if (input == target) return TYPE_UNKNOWN;
    return left.isNumber() ? TYPE_NUMBER : TYPE_UNKNOWN;
  }
  bool hasExpectedType() => type == TYPE_NUMBER;

  abstract num evaluate(num a, num b);
}

class HAdd extends HBinaryArithmetic {
  HAdd(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitAdd(this);
  num evaluate(num a, num b) => a + b;
  bool typeEquals(other) => other is HAdd;
  bool dataEquals(HInstruction other) => true;

  int computeType() {
    int type = computeInputsType();
    builtin = (type == TYPE_NUMBER || type == TYPE_STRING);
    if (left.isNumber()) return TYPE_NUMBER;
    if (left.isString()) return TYPE_STRING;
    return TYPE_UNKNOWN;
  }

  int computeDesiredInputType(HInstruction input) {
    // TODO(floitsch): we want the target to be a function.
    if (input == target) return TYPE_UNKNOWN;
    if (left.isString()) return TYPE_STRING;
    if (left.isNumber()) return TYPE_NUMBER;
    return TYPE_UNKNOWN;
  }

  bool hasExpectedType() {
    if (left.isNumber()) return type == TYPE_NUMBER;
    if (left.isString()) return type == TYPE_STRING;
    return type == TYPE_UNKNOWN;
  }
}

class HDivide extends HBinaryArithmetic {
  HDivide(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitDivide(this);
  num evaluate(num a, num b) => a / b;
  bool typeEquals(other) => other is HDivide;
  bool dataEquals(HInstruction other) => true;
}

class HModulo extends HBinaryArithmetic {
  HModulo(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitModulo(this);
  num evaluate(num a, num b) => a % b;
  bool typeEquals(other) => other is HModulo;
  bool dataEquals(HInstruction other) => true;
}

class HMultiply extends HBinaryArithmetic {
  HMultiply(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitMultiply(this);
  num evaluate(num a, num b) => a * b;
  bool typeEquals(other) => other is HMultiply;
  bool dataEquals(HInstruction other) => true;
}

class HSubtract extends HBinaryArithmetic {
  HSubtract(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitSubtract(this);
  num evaluate(num a, num b) => a - b;
  bool typeEquals(other) => other is HSubtract;
  bool dataEquals(HInstruction other) => true;
}

class HTruncatingDivide extends HBinaryArithmetic {
  HTruncatingDivide(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitTruncatingDivide(this);

  HInstruction fold() {
    // Avoid a DivisionByZeroException.
    if (right.isLiteralNumber() && right.dynamic.value == 0) {
      return this;
    }
    return super.fold();
  }

  num evaluate(num a, num b) => a ~/ b;
  bool typeEquals(other) => other is HTruncatingDivide;
  bool dataEquals(HInstruction other) => true;
}

// TODO(floitsch): Should HBinaryArithmetic really be the super class of
// HBinaryBitOp?
class HBinaryBitOp extends HBinaryArithmetic {
  HBinaryBitOp(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);

  HInstruction fold() {
    // Bit-operations are only defined on integers.
    if (left.isLiteralNumber() && right.isLiteralNumber()) {
      HLiteral op1 = left;
      HLiteral op2 = right;
      // Avoid exceptions.
      if (op1.value is int && op2.value is int) {
        return new HLiteral(evaluate(op1.value, op2.value));
      }
    }
    return this;
  }
}

class HShiftLeft extends HBinaryBitOp {
  HShiftLeft(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitShiftLeft(this);

  HInstruction fold() {
    if (right.isLiteralNumber()) {
      // TODO(floitsch): find good max left-shift amount.
      final int MAX_SHIFT_LEFT_AMOUNT = 50;
      HLiteral op2 = right;
      // Only positive shifting is allowed. Also guard against out-of-memory
      // shifts.
      if (op2.value < 0 || op2.value > MAX_SHIFT_LEFT_AMOUNT) return this;
    }
    return super.fold();
  }

  int evaluate(int a, int b) => a << b;
  bool typeEquals(other) => other is HShiftLeft;
  bool dataEquals(HInstruction other) => true;
}

class HShiftRight extends HBinaryBitOp {
  HShiftRight(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitShiftRight(this);

  HInstruction fold() {
    if (right.isLiteralNumber()) {
      HLiteral op2 = right;
      // Only positive shifting is allowed.
      if (op2.value < 0) return this;
    }
    return super.fold();
  }

  int evaluate(int a, int b) => a >> b;
  bool typeEquals(other) => other is HShiftRight;
  bool dataEquals(HInstruction other) => true;
}

class HBitOr extends HBinaryBitOp {
  HBitOr(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitBitOr(this);

  int evaluate(int a, int b) => a | b;
  bool typeEquals(other) => other is HBitOr;
  bool dataEquals(HInstruction other) => true;
}

class HBitAnd extends HBinaryBitOp {
  HBitAnd(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitBitAnd(this);

  int evaluate(int a, int b) => a & b;
  bool typeEquals(other) => other is HBitAnd;
  bool dataEquals(HInstruction other) => true;
}

class HBitXor extends HBinaryBitOp {
  HBitXor(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  accept(HVisitor visitor) => visitor.visitBitXor(this);

  int evaluate(int a, int b) => a ^ b;
  bool typeEquals(other) => other is HBitXor;
  bool dataEquals(HInstruction other) => true;
}

class HInvokeUnary extends HInvokeStatic {
  bool builtin = false;
  HInvokeUnary(HStatic target, HInstruction input)
      : super(<HInstruction>[target, input]);

  HInstruction get operand() => inputs[1];

  HInstruction fold() {
    if (operand.isLiteralNumber()) {
      HLiteral input = operand;
      return new HLiteral(evaluate(input.value));
    }
    return this;
  }

  abstract num evaluate(num a);
}

class HNegate extends HInvokeUnary {
  HNegate(HStatic target, HInstruction input) : super(target, input);
  accept(HVisitor visitor) => visitor.visitNegate(this);

  HInstruction fold() {
    if (operand.isLiteralNumber()) {
      HLiteral input = operand;
      return new HLiteral(evaluate(input.value));
    }
    return this;
  }

  num evaluate(num a) => -a;
  bool typeEquals(other) => other is HNegate;
  bool dataEquals(HInstruction other) => true;
}

class HBitNot extends HInvokeUnary {
  HBitNot(HStatic target, HInstruction input) : super(target, input);
  accept(HVisitor visitor) => visitor.visitBitNot(this);

  HInstruction fold() {
    if (operand.isLiteralNumber()) {
      HLiteral input = operand;
      if (input.value is int) return new HLiteral(evaluate(input.value));
    }
    return this;
  }

  int evaluate(int a) => ~a;
  bool typeEquals(other) => other is HBitNot;
  bool dataEquals(HInstruction other) => true;
}

class HExit extends HControlFlow {
  HExit() : super(const <HInstruction>[]);
  toString() => 'exit';
  accept(HVisitor visitor) => visitor.visitExit(this);
}

class HGoto extends HControlFlow {
  HGoto() : super(const <HInstruction>[]);
  toString() => 'goto';
  accept(HVisitor visitor) => visitor.visitGoto(this);
}

class HIf extends HConditionalBranch {
  bool hasElse;
  HIf(HInstruction condition, this.hasElse) : super(<HInstruction>[condition]);
  toString() => 'if';
  accept(HVisitor visitor) => visitor.visitIf(this);
}

class HLoopBranch extends HConditionalBranch {
  HLoopBranch(HInstruction condition) : super(<HInstruction>[condition]);
  toString() => 'loop-branch';
  accept(HVisitor visitor) => visitor.visitLoopBranch(this);
}

class HLiteral extends HInstruction {
  final value;
  HLiteral(this.value) : super([]);
  void prepareGvn() {
    // We allow global value numbering of literals, but we still
    // prefer generating them at use sites. This allows us to do
    // better GVN'ing of instructions that use literals as input.
    assert(!hasSideEffects());
    setUseGvn();
    setGenerateAtUseSite();  // Maybe avoid this if the literal is big?
  }
  toString() => 'literal: $value';
  accept(HVisitor visitor) => visitor.visitLiteral(this);

  int computeType() {
    if (isLiteralNumber()) {
      return TYPE_NUMBER;
    } else if (isLiteralBoolean()) {
      return TYPE_BOOLEAN;
    } else if (isLiteralString()) {
      return TYPE_STRING;
    } else {
      return TYPE_UNKNOWN;
    }
  }

  bool hasExpectedType() => true;

  bool isLiteralBoolean() => value is bool;
  bool isLiteralNull() => value === null;
  bool isLiteralNumber() => value is num;
  bool isLiteralString() => value is SourceString;
  bool typeEquals(other) => other is HLiteral;
  bool dataEquals(HLiteral other) => value == other.value;
}

class HNot extends HInstruction {
  HNot(HInstruction value) : super(<HInstruction>[value]);
  void prepareGvn() {
    assert(!hasSideEffects());
    setUseGvn();
  }

  int computeType() => TYPE_BOOLEAN;
  bool hasExpectedType() => true;

  accept(HVisitor visitor) => visitor.visitNot(this);
  bool typeEquals(other) => other is HNot;
  bool dataEquals(HInstruction other) => true;
}

class HParameterValue extends HInstruction {
  final Element element;

  HParameterValue(this.element) : super([]) {
    setGenerateAtUseSite();
  }

  void prepareGvn() {
    assert(!hasSideEffects());
  }
  toString() => 'parameter ${element.name}';
  accept(HVisitor visitor) => visitor.visitParameterValue(this);
}

class HPhi extends HInstruction {
  final Element element;
  // The order of the [inputs] must correspond to the order of the
  // predecessor-edges. That is if an input comes from the first predecessor
  // of the surrounding block, then the input must be the first in the [HPhi].
  HPhi.singleInput(this.element, HInstruction input)
      : super(<HInstruction>[input]);
  HPhi.manyInputs(this.element, List<HInstruction> inputs)
      : super(inputs);

  void addInput(HInstruction input) {
    assert(isInBasicBlock());
    inputs.add(input);
    input.usedBy.add(this);
  }

  // Compute the (shared) type of the inputs if any. If all inputs
  // have the same known type return it. If any two inputs have
  // different known types, we'll return a conflict -- otherwise we'll
  // simply return an unknown type.
  int computeInputsType() {
    bool seenUnknown = false;
    int candidateType = -1;
    for (int i = 0, length = inputs.length; i < length; i++) {
      int inputType = inputs[i].type;
      if (inputType == TYPE_UNKNOWN) {
        seenUnknown = true;
      } else if (candidateType == -1) {
        candidateType = inputType;
      } else if (candidateType != inputType) {
        return TYPE_CONFLICT;
      }
    }
    if (seenUnknown) return TYPE_UNKNOWN;
    return candidateType;
  }

  int computeType() {
    int type = computeInputsType();
    if (type != TYPE_UNKNOWN) return type;
    return super.computeType();
  }

  int computeDesiredInputType(HInstruction input) {
    return type;
  }

  bool hasExpectedType() {
    for (int i = 0; i < inputs.length; i++) {
      if (inputs[i].type !== type) return false;
    }
    return true;
  }

  bool updateTypeForLoopPhi() {
    assert(block.isLoopHeader());
    if (inputs[0].isUnknown()) return false;
    type = inputs[0].type;
    return true;
  }

  toString() => 'phi';
  accept(HVisitor visitor) => visitor.visitPhi(this);
}

class HRelational extends HInvokeBinary {
  HRelational(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);

  void prepareGvn() {
    // Relational expressions can take part in global value numbering
    // and do not have any side-effects if we know all the inputs are
    // numbers. This can be improved for at least equality.
    if (builtin) {
      assert(!hasSideEffects());
      setUseGvn();
    } else {
      setAllSideEffects();
    }
  }

  // TODO(kasperl): This can be improved for at least for equality.
  int computeType() {
    builtin = computeInputsType() == TYPE_NUMBER;
    if (left.isNumber()) return TYPE_BOOLEAN;
    if (type != TYPE_UNKNOWN) return type;
    return super.computeType();
  }

  int computeDesiredInputType(HInstruction input) {
    // TODO(floitsch): we want the target to be a function.
    if (input == inputs[0]) return TYPE_UNKNOWN;
    return left.isNumber() ? TYPE_NUMBER : TYPE_UNKNOWN;
  }

  bool hasExpectedType() => type == TYPE_BOOLEAN;

  abstract bool evaluate(num a, num b);
}

class HEquals extends HRelational {
  HEquals(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  bool evaluate(num a, num b) => a == b;
  accept(HVisitor visitor) => visitor.visitEquals(this);
  bool typeEquals(other) => other is HEquals;
  bool dataEquals(HInstruction other) => true;
}

class HGreater extends HRelational {
  HGreater(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  bool evaluate(num a, num b) => a > b;
  accept(HVisitor visitor) => visitor.visitGreater(this);
  bool typeEquals(other) => other is HGreater;
  bool dataEquals(HInstruction other) => true;
}

class HGreaterEqual extends HRelational {
  HGreaterEqual(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  bool evaluate(num a, num b) => a >= b;
  accept(HVisitor visitor) => visitor.visitGreaterEqual(this);
  bool typeEquals(other) => other is HGreaterEqual;
  bool dataEquals(HInstruction other) => true;
}

class HLess extends HRelational {
  HLess(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  bool evaluate(num a, num b) => a < b;
  accept(HVisitor visitor) => visitor.visitLess(this);
  bool typeEquals(other) => other is HLess;
  bool dataEquals(HInstruction other) => true;
}

class HLessEqual extends HRelational {
  HLessEqual(HStatic target, HInstruction left, HInstruction right)
      : super(target, left, right);
  bool evaluate(num a, num b) => a <= b;
  accept(HVisitor visitor) => visitor.visitLessEqual(this);
  bool typeEquals(other) => other is HLessEqual;
  bool dataEquals(HInstruction other) => true;
}

class HReturn extends HControlFlow {
  HReturn(value) : super([value]);
  toString() => 'return';
  accept(HVisitor visitor) => visitor.visitReturn(this);
}

class HThrow extends HControlFlow {
  HThrow(value) : super([value]);
  toString() => 'throw';
  accept(HVisitor visitor) => visitor.visitThrow(this);
}

class HStatic extends HInstruction {
  Element element;
  HStatic(this.element) : super([]);
  void prepareGvn() {
    // TODO(floitsch): accesses to non-final values must be guarded.
    setUseGvn();
    clearAllSideEffects();
    // TODO(floitsch): we probably want to share statics.
    setGenerateAtUseSite();
  }
  toString() => 'static ${element.name}';
  accept(HVisitor visitor) => visitor.visitStatic(this);

  bool typeEquals(other) => other is HStatic;
  bool dataEquals(HStatic other) => element == other.element;
}

class HLiteralList extends HInstruction {
  HLiteralList(inputs) : super(inputs);
  void prepareGvn() => clearAllSideEffects();
  toString() => 'literal list';
  accept(HVisitor visitor) => visitor.visitLiteralList(this);
}

class HNonSsaInstruction extends HInstruction {
  HNonSsaInstruction(inputs) : super(inputs);
  // Non-SSA instructions cannot take part in GVN.
  void prepareGvn() { unreachable(); }
  bool useGvn() { unreachable(); }
  void setUseGvn() { unreachable(); }
}

class HLoad extends HNonSsaInstruction {
  HLoad(HLocal local, type) : super([local]) { this.type = type; }
  HLocal get local() => inputs[0];
  toString() => 'load';
  accept(HVisitor visitor) => visitor.visitLoad(this);
}

class HStore extends HNonSsaInstruction {
  HStore(HLocal local, HInstruction value) : super([local, value]);
  HLocal get local() => inputs[0];
  HInstruction get value() => inputs[1];
  toString() => 'store';
  accept(HVisitor visitor) => visitor.visitStore(this);
}

class HLocal extends HNonSsaInstruction {
  Element element;
  HInstruction declaredBy;
  HLocal(Element this.element) : super([]) {
    declaredBy = this;
  }
  toString() => 'local';
  accept(HVisitor visitor) => visitor.visitLocal(this);
}
