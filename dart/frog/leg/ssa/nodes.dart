// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

interface HVisitor<R> {
  R visitAdd(HAdd node);
  R visitBasicBlock(HBasicBlock node);
  R visitBoolify(HBoolify node);
  R visitDivide(HDivide node);
  R visitEquals(HEquals node);
  R visitExit(HExit node);
  R visitGoto(HGoto node);
  R visitGreater(HGreater node);
  R visitGreaterEqual(HGreaterEqual node);
  R visitIf(HIf node);
  R visitLess(HLess node);
  R visitLessEqual(HLessEqual node);
  R visitLoad(HLoad node);
  R visitLocal(HLocal node);
  R visitLoopBranch(HLoopBranch node);
  R visitInvoke(HInvoke node);
  R visitInvokeForeign(HInvokeForeign node);
  R visitLiteral(HLiteral node);
  R visitModulo(HModulo node);
  R visitNot(HNot node);
  R visitParameterValue(HParameterValue node);
  R visitPhi(HPhi node);
  R visitSubtract(HSubtract node);
  R visitMultiply(HMultiply node);
  R visitReturn(HReturn node);
  R visitStore(HStore node);
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

  visitArithmetic(HArithmetic node) => visitInvoke(node);
  visitConditionalBranch(HConditionalBranch node) => visitControlFlow(node);
  visitControlFlow(HControlFlow node) => visitInstruction(node);
  visitRelational(HRelational node) => visitInstruction(node);

  visitAdd(HAdd node) => visitArithmetic(node);
  visitBoolify(HBoolify node) => visitInstruction(node);
  visitDivide(HDivide node) => visitArithmetic(node);
  visitEquals(HEquals node) => visitRelational(node);
  visitExit(HExit node) => visitControlFlow(node);
  visitGoto(HGoto node) => visitControlFlow(node);
  visitGreater(HGreater node) => visitRelational(node);
  visitGreaterEqual(HGreaterEqual node) => visitRelational(node);
  visitIf(HIf node) => visitConditionalBranch(node);
  visitInvoke(HInvoke node) => visitInstruction(node);
  visitInvokeForeign(HInvokeForeign node) => visitInvoke(node);
  visitLess(HLess node) => visitRelational(node);
  visitLessEqual(HLessEqual node) => visitRelational(node);
  visitLoad(HLoad node) => visitInstruction(node);
  visitLocal(HLocal node) => visitInstruction(node);
  visitLiteral(HLiteral node) => visitInstruction(node);
  visitModulo(HModulo node) => visitArithmetic(node);
  visitLoopBranch(HLoopBranch node) => visitConditionalBranch(node);
  visitNot(HNot node) => visitInstruction(node);
  visitPhi(HPhi node) => visitInstruction(node);
  visitMultiply(HMultiply node) => visitArithmetic(node);
  visitParameterValue(HParameterValue node) => visitInstruction(node);
  visitReturn(HReturn node) => visitControlFlow(node);
  visitSubtract(HSubtract node) => visitArithmetic(node);
  visitStore(HStore node) => visitInstruction(node);
  visitThrow(HThrow node) => visitControlFlow(node);
  visitTruncatingDivide(HTruncatingDivide node) => visitArithmetic(node);
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
    instruction.next = last;
    instruction.previous = last.previous;
    last.previous.next = instruction;
    last.previous = instruction;
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
  static final int TYPE_CONFLICT = 3;

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

  bool useGvn() => getFlag(FLAG_USE_GVN);
  void setUseGvn() { setFlag(FLAG_USE_GVN); }

  bool isUnknown() => type == TYPE_UNKNOWN || type == TYPE_CONFLICT;
  bool isBoolean() => type == TYPE_BOOLEAN;
  bool isNumber() => type == TYPE_NUMBER;

  // Compute the type of the instruction.
  int computeType() => TYPE_UNKNOWN;

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

  // Re-compute and update the type of the instruction. Returns
  // whether or not the type was changed.
  bool updateType() {
    if (type == TYPE_CONFLICT) return false;
    int newType = computeType();
    bool changed = (type != newType);
    type = newType;
    return changed;
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

class HInvoke extends HInstruction {
  final Element element;
  HInvoke(this.element, inputs) : super(inputs);
  toString() => 'invoke: ${element.name}';
  accept(HVisitor visitor) => visitor.visitInvoke(this);
}

class HInvokeForeign extends HInvoke {
  final SourceString code;
  HInvokeForeign(element, inputs, this.code) : super(element, inputs);
  accept(HVisitor visitor) => visitor.visitInvokeForeign(this);
}

class HArithmetic extends HInvoke {
  HArithmetic(element, inputs) : super(element, inputs);

  void prepareGvn() {
    // An arithmetic expression can take part in global value
    // numbering and do not have any side-effects if we know the
    // result of it is a number.
    if (isNumber()) {
      assert(!hasSideEffects());
      setUseGvn();
    } else {
      setAllSideEffects();
    }
  }

  int computeType() {
    // TODO(kasperl): We should be able to deal with more types
    // here. For now, we only care about numbers.
    int inputsType = computeInputsType();
    return (inputsType == TYPE_NUMBER)
        ? TYPE_NUMBER
        : TYPE_UNKNOWN;
  }

  abstract num evaluate(num a, num b);
}

class HAdd extends HArithmetic {
  HAdd(element, inputs) : super(element, inputs);
  accept(HVisitor visitor) => visitor.visitAdd(this);
  num evaluate(num a, num b) => a + b;
  bool typeEquals(other) => other is HAdd;
  bool dataEquals(HInstruction other) => true;
}

class HDivide extends HArithmetic {
  HDivide(element, inputs) : super(element, inputs);
  accept(HVisitor visitor) => visitor.visitDivide(this);
  num evaluate(num a, num b) => a / b;
  bool typeEquals(other) => other is HDivide;
  bool dataEquals(HInstruction other) => true;
}

class HModulo extends HArithmetic {
  HModulo(element, inputs) : super(element, inputs);
  accept(HVisitor visitor) => visitor.visitModulo(this);
  num evaluate(num a, num b) => a % b;
  bool typeEquals(other) => other is HModulo;
  bool dataEquals(HInstruction other) => true;
}

class HMultiply extends HArithmetic {
  HMultiply(element, inputs) : super(element, inputs);
  accept(HVisitor visitor) => visitor.visitMultiply(this);
  num evaluate(num a, num b) => a * b;
  bool typeEquals(other) => other is HMultiply;
  bool dataEquals(HInstruction other) => true;
}

class HSubtract extends HArithmetic {
  HSubtract(element, inputs) : super(element, inputs);
  accept(HVisitor visitor) => visitor.visitSubtract(this);
  num evaluate(num a, num b) => a - b;
  bool typeEquals(other) => other is HSubtract;
  bool dataEquals(HInstruction other) => true;
}

class HTruncatingDivide extends HArithmetic {
  HTruncatingDivide(element, inputs) : super(element, inputs);
  accept(HVisitor visitor) => visitor.visitTruncatingDivide(this);
  num evaluate(num a, num b) => a ~/ b;
  bool typeEquals(other) => other is HTruncatingDivide;
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
    } else if (value is bool) {
      return TYPE_BOOLEAN;
    } else {
      return TYPE_UNKNOWN;
    }
  }

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

  int computeType() => computeInputsType();

  bool updateTypeForLoopPhi() {
    assert(block.isLoopHeader());
    if (inputs[0].isUnknown()) return false;
    type = inputs[0].type;
    return true;
  }

  toString() => 'phi';
  accept(HVisitor visitor) => visitor.visitPhi(this);
}

class HRelational extends HInvoke {
  HRelational(Element element, List<HInstruction> inputs)
      : super(element, inputs);

  void prepareGvn() {
    // Relational expressions can take part in global value
    // numbering and do not have any side-effects if we know the
    // result of it is a boolean.
    if (isBoolean()) {
      assert(!hasSideEffects());
      setUseGvn();
    } else {
      setAllSideEffects();
    }
  }

  int computeType() {
    // TODO(kasperl): We should be able to deal with more types
    // in the HEquals case. For now, we only care about numbers.
    int inputsType = computeInputsType();
    return (inputsType == TYPE_NUMBER)
        ? TYPE_BOOLEAN
        : TYPE_UNKNOWN;
  }

  abstract bool evaluate(num a, num b);
}

class HEquals extends HRelational {
  HEquals(element, inputs) : super(element, inputs);
  bool evaluate(num a, num b) => a == b;
  accept(HVisitor visitor) => visitor.visitEquals(this);
  bool typeEquals(other) => other is HEquals;
  bool dataEquals(HInstruction other) => true;
}

class HGreater extends HRelational {
  HGreater(element, inputs) : super(element, inputs);
  bool evaluate(num a, num b) => a > b;
  accept(HVisitor visitor) => visitor.visitGreater(this);
  bool typeEquals(other) => other is HGreater;
  bool dataEquals(HInstruction other) => true;
}

class HGreaterEqual extends HRelational {
  HGreaterEqual(element, inputs) : super(element, inputs);
  bool evaluate(num a, num b) => a >= b;
  accept(HVisitor visitor) => visitor.visitGreaterEqual(this);
  bool typeEquals(other) => other is HGreaterEqual;
  bool dataEquals(HInstruction other) => true;
}

class HLess extends HRelational {
  HLess(element, inputs) : super(element, inputs);
  bool evaluate(num a, num b) => a < b;
  accept(HVisitor visitor) => visitor.visitLess(this);
  bool typeEquals(other) => other is HLess;
  bool dataEquals(HInstruction other) => true;
}

class HLessEqual extends HRelational {
  HLessEqual(element, inputs) : super(element, inputs);
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
