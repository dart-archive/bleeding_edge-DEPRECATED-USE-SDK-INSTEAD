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
  R visitIf(HIf node);
  R visitLoad(HLoad node);
  R visitLocal(HLocal node);
  R visitLoopBranch(HLoopBranch node);
  R visitInvoke(HInvoke node);
  R visitInvokeForeign(HInvokeForeign node);
  R visitLiteral(HLiteral node);
  R visitParameter(HParameter node);
  R visitPhi(HPhi node);
  R visitSubtract(HSubtract node);
  R visitMultiply(HMultiply node);
  R visitReturn(HReturn node);
  R visitStore(HStore node);
  R visitThrow(HThrow node);
  R visitTruncatingDivide(HTruncatingDivide node);
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
      if (block.isLoopHeader) {
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

  visitAdd(HAdd node) => visitArithmetic(node);
  visitBoolify(HBoolify node) => visitInstruction(node);
  visitDivide(HDivide node) => visitArithmetic(node);
  visitEquals(HEquals node) => visitInvoke(node);
  visitExit(HExit node) => visitControlFlow(node);
  visitGoto(HGoto node) => visitControlFlow(node);
  visitIf(HIf node) => visitConditionalBranch(node);
  visitInvoke(HInvoke node) => visitInstruction(node);
  visitInvokeForeign(HInvokeForeign node) => visitInvoke(node);
  visitLoad(HLoad node) => visitInstruction(node);
  visitLocal(HLocal node) => visitInstruction(node);
  visitLiteral(HLiteral node) => visitInstruction(node);
  visitLoopBranch(HLoopBranch node) => visitConditionalBranch(node);
  visitPhi(HPhi node) => visitInstruction(node);
  visitMultiply(HMultiply node) => visitArithmetic(node);
  visitParameter(HParameter node) => visitInstruction(node);
  visitReturn(HReturn node) => visitControlFlow(node);
  visitSubtract(HSubtract node) => visitArithmetic(node);
  visitStore(HStore node) => visitInstruction(node);
  visitThrow(HThrow node) => visitControlFlow(node);
  visitTruncatingDivide(HTruncatingDivide node) => visitArithmetic(node);
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
    instruction.notifyAddedToBlock();
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
    instruction.notifyAddedToBlock();
  }

  void remove(HInstruction instruction) {
    assert(contains(instruction));
    assert(instruction.isInBasicBlock());
    assert(instruction.usedBy.isEmpty());
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
    instruction.notifyRemovedFromBlock();
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

  bool isLoopHeader = false;
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
    super.addBefore(first, instruction);
  }

  void addAtExit(HInstruction instruction) {
    assert(isClosed());
    assert(last is HControlFlow);
    super.addBefore(last, instruction);
  }

  void add(HInstruction instruction) {
    assert(instruction is !HControlFlow);
    super.addAfter(last, instruction);
  }

  void addPhi(HPhi phi) {
    phis.addAfter(phis.last, phi);
  }

  void removePhi(HPhi phi) {
    phis.remove(phi);
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

  void addAfter(HInstruction cursor, HInstruction instruction) {
    assert(isOpen() || isClosed());
    super.addAfter(cursor, instruction);
  }

  void remove(HInstruction instruction) {
    assert(isOpen() || isClosed());
    super.remove(instruction);
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
    from._usedBy = [];
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

class HInstruction {
  int id;
  final List<HInstruction> inputs;
  List<HInstruction> _usedBy = null;  // If [null] then the instruction is not
                                      // in a basic block.
  HInstruction previous = null;
  HInstruction next = null;
  int flags = 0;

  // Changes flags.
  static final int FLAG_CHANGES_SOMETHING    = 0;
  static final int FLAG_CHANGES_COUNT        = FLAG_CHANGES_SOMETHING + 1;

  // Depends flags (one for each changes flag).
  static final int FLAG_DEPENDS_ON_SOMETHING = FLAG_CHANGES_COUNT;

  // Other flags.
  static final int FLAG_GENERATE_AT_USE_SITE = FLAG_DEPENDS_ON_SOMETHING + 1;
  static final int FLAG_USE_GVN              = FLAG_GENERATE_AT_USE_SITE + 1;

  static int idCounter;

  HInstruction(this.inputs) {
    prepareGvn();
    id = idCounter++;
  }

  bool getFlag(int position) => (flags & (1 << position)) != 0;
  void setFlag(int position) { flags |= (1 << position); }
  void clearFlag(int position) { flags &= ~(1 << position); }

  static int computeDependsOnFlags(int flags) => flags << FLAG_CHANGES_COUNT;

  int getChangesFlags() => flags & ((1 << FLAG_CHANGES_COUNT) - 1);
  bool hasSideEffects() => getChangesFlags() != 0;
  void prepareGvn() { setAllSideEffects();  }

  void setAllSideEffects() { flags |= ((1 << FLAG_CHANGES_COUNT) - 1); }
  void clearAllSideEffects() { flags &= ~((1 << FLAG_CHANGES_COUNT) - 1); }

  bool generateAtUseSite() => getFlag(FLAG_GENERATE_AT_USE_SITE);
  void setGenerateAtUseSite() { setFlag(FLAG_GENERATE_AT_USE_SITE); }

  bool useGvn() => getFlag(FLAG_USE_GVN);
  void setUseGvn() { setFlag(FLAG_USE_GVN); }

  List<HInstruction> get usedBy() {
    if (_usedBy == null) return const <HInstruction>[];
    return _usedBy;
  }

  bool isInBasicBlock() => _usedBy !== null;

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

  void notifyAddedToBlock() {
    assert(!isInBasicBlock());
    _usedBy = <HInstruction>[];
    // Add [this] to the inputs' uses.
    for (int i = 0; i < inputs.length; i++) {
      assert(inputs[i].isInBasicBlock());
      inputs[i].usedBy.add(this);
    }
    assert(isValid());
  }

  void notifyRemovedFromBlock() {
    assert(isInBasicBlock());
    assert(usedBy.isEmpty());

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
    _usedBy = null;
    assert(isValid());
  }

  bool isLiteralNumber() => false;
  bool isLiteralString() => false;

  bool isValid() {
    HValidator validator = new HValidator();
    validator.visitInstruction(this);
    return validator.isValid;
  }
}

class HBoolify extends HInstruction {
  HBoolify(HInstruction value) : super(<HInstruction>[value]);
  void prepareGvn() {
    clearAllSideEffects();
    setUseGvn();
  }
  accept(HVisitor visitor) => visitor.visitBoolify(this);
  bool typeEquals(other) => other is HBoolify;
  bool dataEquals(HInstruction other) => true;
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
    // Arithmetic expressions can take part in global value numbering
    // and do not have any side-effects if the left-hand side is a
    // literal.
    if (inputs[0] is !HLiteral) return;
    clearAllSideEffects();
    setUseGvn();
  }
  abstract num evaluate(num a, num b);
}

class HAdd extends HArithmetic {
  HAdd(element, inputs) : super(element, inputs);
  void prepareGvn() {
    // Only if the left-hand side is a literal number are we
    // sure the operation will not have any side-effects.
    if (!inputs[0].isLiteralNumber()) return;
    clearAllSideEffects();
    setUseGvn();
  }
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

class HEquals extends HInvoke {
  HEquals(element, inputs) : super(element, inputs);
  void prepareGvn() {
    // Only if the left-hand side is a (any) literal are we
    // sure the operation will not have any side-effects.
    if (inputs[0] is !HLiteral) return;
    clearAllSideEffects();
    setUseGvn();
  }
  accept(HVisitor visitor) => visitor.visitEquals(this);
  bool typeEquals(other) => other is HEquals;
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
    clearAllSideEffects();
    setUseGvn();
  }
  toString() => 'literal: $value';
  accept(HVisitor visitor) => visitor.visitLiteral(this);
  bool isLiteralNumber() => value is num;
  bool isLiteralString() => value is SourceString;
  bool typeEquals(other) => other is HLiteral;
  bool dataEquals(other) => value == other.value;
}

class HParameter extends HInstruction {
  final int parameterIndex;
  HParameter(this.parameterIndex) : super([]);
  void prepareGvn() {
    clearAllSideEffects();
  }
  toString() => 'parameter $parameterIndex';
  accept(HVisitor visitor) => visitor.visitParameter(this);
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

  toString() => 'phi';
  accept(HVisitor visitor) => visitor.visitPhi(this);
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
  void prepareGvn() {}
  bool useGvn() { unreachable(); }
  void setUseGvn() { unreachable(); }
}

class HLoad extends HNonSsaInstruction {
  HLoad(HLocal local) : super([local]);
  toString() => 'load';
  accept(HVisitor visitor) => visitor.visitLoad(this);
}

class HStore extends HNonSsaInstruction {
  HStore(HLocal local, HInstruction value) : super([local, value]);
  toString() => 'store';
  accept(HVisitor visitor) => visitor.visitStore(this);
}

class HLocal extends HNonSsaInstruction {
  Element element;
  HLocal(Element this.element) : super([]);
  toString() => 'local';
  accept(HVisitor visitor) => visitor.visitLocal(this);
}
