// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaCodeGeneratorTask extends CompilerTask {
  SsaCodeGeneratorTask(Compiler compiler) : super(compiler);
  String get name() => 'SSA code generator';

  String generate(FunctionElement function, HGraph graph) {
    return measure(() {
      Map<Element, String> parameterNames =
          new LinkedHashMap<Element, String>();
      for (Link<Element> link = function.parameters;
           !link.isEmpty();
           link = link.tail) {
        Element element = link.head;
        parameterNames[element] = JsNames.getValid('${element.name}');
      }

      String code = generateMethod(parameterNames, graph);
      return code;
    });
  }

  void preGenerateMethod(HGraph graph) {
    if (GENERATE_SSA_TRACE) {
      new HTracer.singleton().traceGraph("codegen", graph);
    }
    new SsaInstructionMerger().visitGraph(graph);
    // Replace the results of check instructions with the
    // original value, if the result is used. This is safe now,
    // since we don't do code motion after this point.
    new SsaCheckInstructionUnuser().visitGraph(graph);
    new SsaConditionMerger().visitGraph(graph);
    new SsaPhiEliminator().visitGraph(graph);
    if (GENERATE_SSA_TRACE) {
      new HTracer.singleton().traceGraph("no-phi", graph);
    }
  }

  String generateMethod(Map<Element, String> parameterNames, HGraph graph) {
    preGenerateMethod(graph);
    StringBuffer buffer = new StringBuffer();
    SsaCodeGenerator codegen =
        new SsaCodeGenerator(compiler, buffer, parameterNames);
    codegen.visitGraph(graph);
    StringBuffer parameters = new StringBuffer();
    List<String> names = parameterNames.getValues();
    for (int i = 0; i < names.length; i++) {
      if (i != 0) parameters.add(', ');
      parameters.add(names[i]);
    }
    return 'function($parameters) {\n$buffer}';
  }
}

class SsaCodeGenerator implements HVisitor {
  final Compiler compiler;
  final StringBuffer buffer;

  final Map<Element, String> parameterNames;
  final Map<int, String> names;
  final Map<String, int> prefixes;

  int indent = 0;
  HGraph currentGraph;
  HBasicBlock currentBlock;

  SsaCodeGenerator(this.compiler, this.buffer, this.parameterNames)
    : names = new Map<int, String>(),
      prefixes = new Map<String, int>() {
    for (final name in parameterNames.getValues()) {
      prefixes[name] = 0;
    }
  }

  visitGraph(HGraph graph) {
    currentGraph = graph;
    indent++;  // We are already inside a function.
    visitBasicBlock(graph.entry);
  }

  String parameter(HParameterValue parameter) => parameterNames[parameter.element];

  String temporary(HInstruction instruction) {
    int id = instruction.id;
    String name = names[id];
    if (name !== null) return name;

    String prefix = 't';
    if (!prefixes.containsKey(prefix)) prefixes[prefix] = 0;
    return newName(id, '${prefix}${prefixes[prefix]++}');
  }

  String local(HLocal local) {
    Element element = local.element;
    if (element != null && element.kind == ElementKind.PARAMETER) {
      return parameterNames[element];
    }
    int id = local.id;
    String name = names[id];
    if (name !== null) return name;

    String prefix;
    if (element !== null) {
      prefix = element.name.stringValue;
    } else {
      prefix = 'v';
    }
    if (!prefixes.containsKey(prefix)) {
      prefixes[prefix] = 0;
      return newName(id, prefix);
    } else {
      return newName(id, '${prefix}_${prefixes[prefix]++}');
    }
  }

  String newName(int id, String name) {
    String result = JsNames.getValid(name);
    names[id] = result;
    return result;
  }

  /**
    * Only visits the arguments starting at inputs[HInvoke.ARGUMENTS_OFFSET].
    */
  void visitArguments(List<HInstruction> inputs) {
    assert(inputs.length >= HInvoke.ARGUMENTS_OFFSET);
    buffer.add('(');
    for (int i = HInvoke.ARGUMENTS_OFFSET; i < inputs.length; i++) {
      if (i != HInvoke.ARGUMENTS_OFFSET) buffer.add(', ');
      use(inputs[i]);
    }
    buffer.add(")");
  }

  void define(HInstruction instruction) {
    buffer.add('var ${temporary(instruction)} = ');
    visit(instruction);
  }

  void use(HInstruction argument) {
    if (argument.generateAtUseSite()) {
      visit(argument);
    } else {
      buffer.add(temporary(argument));
    }
  }

  visit(HInstruction node) {
    return node.accept(this);
  }

  visitBasicBlock(HBasicBlock node) {
    currentBlock = node;
    for (HPhi phi = node.phis.first; phi != null; phi = phi.next) {
      if (!phi.generateAtUseSite()) visit(phi);
    }

    // While loop will be closed by the conditional loop-branch.
    // TODO(floitsch): HACK HACK HACK.
    if (node.isLoopHeader()) {
      addIndentation();
      buffer.add('while (true) {\n');
      indent++;
    }
    HInstruction instruction = node.first;
    while (instruction != null) {
      if (instruction is HGoto || instruction is HExit) {
        visit(instruction);
        return;
      } else if (!instruction.generateAtUseSite()) {
        addIndentation();
        if (instruction.usedBy.isEmpty() || instruction is HLocal) {
          visit(instruction);
        } else {
          define(instruction);
        }
        // Control flow instructions know how to handle ';'.
        if (instruction is !HControlFlow) {
          buffer.add(';\n');
        }
      } else if (instruction is HIf) {
        HIf hif = instruction;
        // The "if" is implementing part of a logical expression.
        // Skip directly forward to to its latest successor, since everything
        // in-between must also be generateAtUseSite.
        assert(hif.trueBranch.id < hif.falseBranch.id);
        visitBasicBlock(hif.falseBranch);
        return;
      }
      instruction = instruction.next;
    }
  }

  visitInvokeBinary(HInvokeBinary node, String op) {
    if (node.builtin) {
      buffer.add('(');
      use(node.left);
      buffer.add(' $op ');
      use(node.right);
      buffer.add(')');
    } else {
      visitInvokeStatic(node);
    }
  }

  visitInvokeUnary(HInvokeUnary node, String op) {
    if (node.builtin) {
      buffer.add('($op');
      use(node.operand);
      buffer.add(')');
    } else {
      visitInvokeStatic(node);
    }
  }

  visitAdd(HAdd node)               => visitInvokeBinary(node, '+');
  visitDivide(HDivide node)         => visitInvokeBinary(node, '/');
  visitMultiply(HMultiply node)     => visitInvokeBinary(node, '*');
  visitSubtract(HSubtract node)     => visitInvokeBinary(node, '-');
  // Truncating divide does not have a JS equivalent.
  visitTruncatingDivide(HTruncatingDivide node) => visitInvokeStatic(node);
  // Modulo cannot be mapped to the native operator (different semantics).
  visitModulo(HModulo node)                     => visitInvokeStatic(node);

  visitBitAnd(HBitAnd node)         => visitInvokeBinary(node, '&');
  visitBitNot(HBitNot node)         => visitInvokeUnary(node, '~');
  visitBitOr(HBitOr node)           => visitInvokeBinary(node, '|');
  visitBitXor(HBitXor node)         => visitInvokeBinary(node, '^');
  visitShiftRight(HShiftRight node) => visitInvokeBinary(node, '>>');

  // Shift left cannot be mapped to the native operator (different semantics).
  visitShiftLeft(HShiftLeft node)   => visitInvokeStatic(node);

  visitNegate(HNegate node)         => visitInvokeUnary(node, '-');

  visitEquals(HEquals node)             => visitInvokeBinary(node, '===');
  visitIdentity(HIdentity node)         => visitInvokeBinary(node, '===');
  visitLess(HLess node)                 => visitInvokeBinary(node, '<');
  visitLessEqual(HLessEqual node)       => visitInvokeBinary(node, '<=');
  visitGreater(HGreater node)           => visitInvokeBinary(node, '>');
  visitGreaterEqual(HGreaterEqual node) => visitInvokeBinary(node, '>=');

  visitLogicalOperator(HLogicalOperator node) {
    buffer.add("((");
    use(node.left);
    buffer.add(")${node.operation}(");
    use(node.right);
    buffer.add("))");
  }

  visitBoolify(HBoolify node) {
    assert(node.inputs.length == 1);
    assert(!node.inputs[0].isBoolean());
    buffer.add('(');
    use(node.inputs[0]);
    buffer.add(' === true)');
  }

  visitExit(HExit node) {
    // Don't do anything.
  }

  visitGoto(HGoto node) {
    assert(currentBlock.successors.length == 1);
    List<HBasicBlock> dominated = currentBlock.dominatedBlocks;
    // With the exception of the entry-node which dominates its successor
    // and the exit node, no block finishing with a 'goto' can have more than
    // one dominated block (since it has only one successor).
    // If the successor is dominated by another block, then the other block
    // is responsible for visiting the successor.
    if (dominated.isEmpty()) return;
    if (dominated.length > 2) unreachable();
    if (dominated.length == 2 && currentBlock !== currentGraph.entry) {
      unreachable();
    }
    assert(dominated[0] == currentBlock.successors[0]);
    visitBasicBlock(dominated[0]);
  }

  visitIf(HIf node) {
    // The currentBlock will be changed when we visit the successors. So keep
    // a local copy around.
    HBasicBlock ifBlock = currentBlock;
    assert(!node.generateAtUseSite());
    buffer.add('if (');
    use(node.inputs[0]);
    buffer.add(') {\n');
    List<HBasicBlock> dominated = ifBlock.dominatedBlocks;
    assert(dominated[0] === ifBlock.successors[0]);
    indent++;
    visitBasicBlock(ifBlock.successors[0]);
    indent--;
    addIndentation();
    int nextDominatedIndex;
    if (node.hasElse) {
      buffer.add('} else {\n');
      indent++;
      assert(dominated[1] === ifBlock.successors[1]);
      visitBasicBlock(ifBlock.successors[1]);
      indent--;
      addIndentation();
    }
    buffer.add("}\n");

    // Normally the HIf dominates the join-block. In this case there is one
    // dominated block that we need to visit:
    // If both the then and else blocks return/throw, then the join-block is
    // either the exit-block, or there is none.
    // We can also have the case where the HIf has no else, but the then-branch
    // terminates. If the code after the 'if' terminates, then the
    // if could become the dominator of the exit-block, thus having
    // three dominated blocks: the then, the code after the if, and the exit
    // block.

    int dominatedCount = dominated.length;
    if (node.hasElse && (dominatedCount == 3 || dominatedCount == 4)) {
      // Normal case. The third dominated block is either the join-block or
      // the exit-block (if both branches terminate).
      // If the if dominates 4 blocks, then at least one branch does a
      // conditional return, and the 4th block is the exit-block.
      assert(dominatedCount != 4 || dominated.last().isExitBlock());
      visitBasicBlock(dominated[2]);
    } else if (node.hasElse) {
      // Both branches terminate, but this HIf is not the dominator of the exit
      // block.
      assert(dominatedCount == 2);
    } else if (!node.hasElse && dominatedCount == 2) {
      // Normal case. Even if the then-branch terminated there is still
      // a join-block.
      assert(!dominated.last().isExitBlock());
      visitBasicBlock(dominated[1]);
    } else {
      // The then-branch terminates, and the code following the if terminates
      // too. The if happens to dominate the exit-block.
      assert(!node.hasElse);
      assert(dominatedCount == 3);
      assert(dominated.last().isExitBlock());
      visitBasicBlock(dominated[1]);
      visitBasicBlock(dominated[2]);
    }
  }

  visitInvokeDynamic(HInvokeDynamic node) {
    use(node.receiver);
    buffer.add('.');
    buffer.add(node.name);
    visitArguments(node.inputs);
    compiler.registerDynamicInvocation(node.name);
  }

  visitInvokeDynamicMethod(HInvokeDynamicMethod node)
      => visitInvokeDynamic(node);

  visitInvokeDynamicSetter(HInvokeDynamicSetter node)
      => visitInvokeDynamic(node);

  visitInvokeDynamicGetter(HInvokeDynamicGetter node)
      => visitInvokeDynamic(node);

  visitInvokeStatic(HInvokeStatic node) {
    compiler.registerStaticInvocation(node.element);
    use(node.target);
    visitArguments(node.inputs);
  }

  visitForeign(HForeign node) {
    String code = '${node.code}';
    List<HInstruction> inputs = node.inputs;
    for (int i = 0; i < inputs.length; i++) {
      HInstruction input = inputs[i];
      String name;
      if (input is HParameterValue) {
        name = parameter(input);
      } else {
        assert(!input.generateAtUseSite());
        name = temporary(input);
      }
      code = code.replaceAll('\$$i', name);
    }
    buffer.add('($code)');
  }

  visitForeignNew(HForeignNew node) {
    String jsClassReference = compiler.namer.isolateAccess(node.element);
    buffer.add('new $jsClassReference(');
    // We can't use 'visitArguments', since our arguments start at input[0].
    List<HInstruction> inputs = node.inputs;
    for (int i = 0; i < inputs.length; i++) {
      if (i != 0) buffer.add(', ');
      use(inputs[i]);
    }
    buffer.add(')');
  }

  static String makeStringLiteral(SourceString literal) {
    // TODO(lrn): Escape string content.
    return literal.toString();
  }


  visitLiteral(HLiteral node) {
    if (node.isLiteralNull()) {
      buffer.add("(void 0)");
    } else if (node.value is num && node.value < 0) {
      buffer.add('(${node.value})');
    } else if (node.isLiteralString()) {
      QuotedString string = node.value;
      string.printOn(buffer);
    } else {
      buffer.add(node.value);
    }
  }

  visitLoopBranch(HLoopBranch node) {
    HBasicBlock branchBlock = currentBlock;
    buffer.add('if (!(');
    use(node.inputs[0]);
    buffer.add(')) break;\n');
    List<HBasicBlock> dominated = currentBlock.dominatedBlocks;
    HBasicBlock loopSuccessor;
    if (dominated.length == 1) {
      // Do While.
      // The first successor is the loop-body and thus a back-edge.
      assert(branchBlock.successors[0].id < branchBlock.id);
      assert(dominated[0] === branchBlock.successors[1]);
      // The body has already been visited. Nothing to do in this branch.
    } else {
      // A normal while loop. Visit the body.
      assert(dominated[0] === branchBlock.successors[0]);
      assert(dominated[1] === branchBlock.successors[1]);
      visitBasicBlock(dominated[0]);
    }
    indent--;
    addIndentation();
    buffer.add('}\n');  // Close 'while' loop.
    visitBasicBlock(branchBlock.successors[1]);
    // TODO(floitsch): with labeled breaks we can have more dominated blocks.
    assert(dominated.length <= 3);
    if (dominated.length == 3) {
      // This happens when the body contains a 'return', and the exit-block is
      // not dominated by a dominator of the while loop.
      assert(dominated[2].isExitBlock());
      visitBasicBlock(dominated[2]);
    }
  }

  visitNot(HNot node) {
    assert(node.inputs.length == 1);
    buffer.add('(!');
    use(node.inputs[0]);
    buffer.add(')');
  }

  visitParameterValue(HParameterValue node) {
    buffer.add(parameter(node));
  }

  visitPhi(HPhi node) {
    unreachable();
  }

  visitReturn(HReturn node) {
    assert(node.inputs.length == 1);
    HInstruction input = node.inputs[0];
    if (input.isLiteralNull()) {
      buffer.add('return;\n');
    } else {
      buffer.add('return ');
      use(node.inputs[0]);
      buffer.add(';\n');
    }
  }

  visitThis(HThis node) {
    buffer.add('this');
  }

  visitThrow(HThrow node) {
    buffer.add('throw ');
    use(node.inputs[0]);
    buffer.add(';\n');
  }

  visitBoundsCheck(HBoundsCheck node) {
    buffer.add('if (');
    use(node.index);
    buffer.add(' < 0 || ');
    use(node.index);
    buffer.add(' >= ');
    use(node.length);
    buffer.add(") throw 'Out of bounds'");
  }

  visitIntegerCheck(HIntegerCheck node) {
    buffer.add('if (');
    use(node.value);
    buffer.add(' !== (');
    use(node.value);
    buffer.add(" | 0)) throw 'Illegal argument'");
  }

  visitTypeGuard(HTypeGuard node) {
    HInstruction input = node.inputs[0];
    assert(!input.generateAtUseSite() || input is HParameterValue);
    if (node.isInteger()) {
      buffer.add('if (');
      use(input);
      buffer.add(' !== (');
      use(input);
      buffer.add(" | 0)) throw('Not an integer')");
    } else if (node.isNumber()) {
      buffer.add('if (typeof ');
      use(input);
      buffer.add(" !== 'number') throw('Not a number')");
    } else if (node.isBoolean()) {
      buffer.add('if (typeof ');
      use(input);
      buffer.add(" !== 'boolean') throw('Not a boolean')");
    } else if (node.isString()) {
      buffer.add('if (typeof ');
      use(input);
      buffer.add(" !== 'string') throw('Not a string')");
    } else if (node.isArray()) {
      buffer.add('if (');
      use(input);
      buffer.add(".constructor !== Array) throw('Not an array')");
    } else if (node.isStringOrArray()) {
      buffer.add('if (typeof ');
      use(input);
      buffer.add(" !== 'string' && ");
      use(input);
      buffer.add(".constructor !== Array) throw('Not a string or array')");
    } else {
      unreachable();
    }
  }

  void addIndentation() {
    for (int i = 0; i < indent; i++) {
      buffer.add('  ');
    }
  }

  void visitStatic(HStatic node) {
    buffer.add(compiler.namer.isolateAccess(node.element));
  }

  void visitStaticStore(HStaticStore node) {
    buffer.add(compiler.namer.isolateAccess(node.element));
    buffer.add(' = ');
    use(node.inputs[0]);
  }

  void visitStore(HStore node) {
    if (node.local.declaredBy === node) {
      buffer.add('var ');
    }
    buffer.add('${local(node.local)} = ');
    use(node.value);
  }

  void visitLoad(HLoad node) {
    buffer.add('${local(node.local)}');
  }

  void visitLocal(HLocal node) {
    buffer.add('var ${local(node)}');
  }

  void visitLiteralList(HLiteralList node) {
    buffer.add('[');
    int len = node.inputs.length;
    for (int i = 0; i < len; i++) {
      if (i != 0) buffer.add(', ');
      use(node.inputs[i]);
    }
    buffer.add(']');
  }

  void visitIndex(HIndex node) {
    if (node.builtin) {
      use(node.inputs[1]);
      buffer.add('[');
      use(node.inputs[2]);
      buffer.add(']');
    } else {
      visitInvokeStatic(node);
    }
  }

  void visitIndexAssign(HIndexAssign node) {
    if (node.builtin) {
      buffer.add('(');
      use(node.inputs[1]);
      buffer.add('[');
      use(node.inputs[2]);
      buffer.add('] = ');
      use(node.inputs[3]);
      buffer.add(')');
    } else {
      visitInvokeStatic(node);
    }
  }

  void visitInvokeInterceptor(HInvokeInterceptor node) {
    if (node.builtinJsName != null) {
      use(node.inputs[1]);
      buffer.add('.');
      buffer.add(node.builtinJsName);
      if (node.getter) return;
      buffer.add('(');
      for (int i = 2; i < node.inputs.length; i++) {
        if (i != 2) buffer.add(', ');
        use(node.inputs[i]);
      }
      buffer.add(")");
    } else {
      return visitInvokeStatic(node);
    }
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
class SsaInstructionMerger extends HGraphVisitor {
  void visitGraph(HGraph graph) {
    visitDominatorTree(graph);
  }

  bool usedOnlyByPhis(instruction) {
    for (HInstruction user in instruction.usedBy) {
      if (user is !HPhi) return false;
    }
    return true;
  }

  void visitBasicBlock(HBasicBlock block) {
    // Visit each instruction of the basic block in last-to-first order.
    // Keep a list of expected inputs of the current "expression" being
    // merged. If instructions occur in the expected order, they are
    // included in the expression.

    // The expectedInputs list holds non-trivial instructions that may
    // be generated at their use site, if they occur in the correct order.
    List<HInstruction> expectedInputs = new List<HInstruction>();
    // Add non-trivial inputs of instruction to expectedInputs, in
    // evaluation order.
    void addInputs(HInstruction instruction) {
      for (HInstruction input in instruction.inputs) {
        if (!input.generateAtUseSite() && input.usedBy.length == 1) {
          expectedInputs.add(input);
        }
      }
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
    HInstruction guarded = node.inputs[0];
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
  static bool isExpression(HInstruction instruction, HBasicBlock limit) {
    if (instruction is HPhi) return false;
    while (instruction.previous != null) {
      instruction = instruction.previous;
      if (!instruction.generateAtUseSite()) {
        return false;
      }
    }
    HBasicBlock block = instruction.block;
    if (!block.phis.isEmpty()) return false;
    if (instruction is HLogicalOperator) {
      // We know that the second operand is an expression.
      return isExpression(instruction.inputs[0], limit);
    }
    return block.predecessors.length == 1 && block.predecessors[0] == limit;
  }

  static void replaceWithLogicalOperator(HPhi phi, String type) {
    HBasicBlock block = phi.block;
    HLogicalOperator logicalOp =
        new HLogicalOperator(type, phi.inputs[0], phi.inputs[1]);
    if (canGenerateAtUseSite(phi))  {
      // TODO(lrn): More tests here?
      logicalOp.tryGenerateAtUseSite();
    }
    block.addAtEntry(logicalOp);
    // Move instruction that uses phi as input to using the logicalOp instead.
    block.rewrite(phi, logicalOp);
    // Remove the no-longer-used phi.
    block.removePhi(phi);
  }

  static bool canGenerateAtUseSite(HPhi phi) {
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

  static void detectLogicControlFlow(HPhi phi) {
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
