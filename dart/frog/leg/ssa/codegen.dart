// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaCodeGeneratorTask extends CompilerTask {
  SsaCodeGeneratorTask(Compiler compiler) : super(compiler);
  String get name() => 'SSA code generator';

  String generate(WorkItem work, HGraph graph) {
    return measure(() {
      FunctionElement function = work.element;
      Map<Element, String> parameterNames =
          new LinkedHashMap<Element, String>();

      function.computeParameters(compiler).forEachParameter((Element element) {
        parameterNames[element] = JsNames.getValid('${element.name}');
      });

      String code = generateMethod(parameterNames, work, graph);
      return code;
    });
  }

  void preGenerateMethod(HGraph graph, WorkItem work) {
    if (GENERATE_SSA_TRACE) {
      new HTracer.singleton().traceGraph("codegen", graph);
    }
    new SsaInstructionMerger().visitGraph(graph);
    // Replace the results of check instructions with the
    // original value, if the result is used. This is safe now,
    // since we don't do code motion after this point.
    new SsaCheckInstructionUnuser().visitGraph(graph);
    new SsaConditionMerger().visitGraph(graph);
    new SsaPhiEliminator(work).visitGraph(graph);
    if (GENERATE_SSA_TRACE) {
      new HTracer.singleton().traceGraph("no-phi", graph);
    }
  }

  String generateMethod(Map<Element, String> parameterNames,
                        WorkItem work,
                        HGraph graph) {
    preGenerateMethod(graph, work);
    StringBuffer buffer = new StringBuffer();
    StringBuffer parameters = new StringBuffer();
    List<String> names = parameterNames.getValues();
    for (int i = 0; i < names.length; i++) {
      if (i != 0) parameters.add(', ');
      parameters.add(names[i]);
    }

    if (work.isBailoutVersion()) {
      new SsaBailoutPropagator(compiler).visitGraph(graph);
      SsaUnoptimizedCodeGenerator codegen = new SsaUnoptimizedCodeGenerator(
          compiler, work, buffer, parameters, parameterNames);
      codegen.visitGraph(graph);
      StringBuffer newParameters = new StringBuffer();
      if (!parameterNames.isEmpty()) newParameters.add('$parameters, ');
      newParameters.add('state');
      for (int i = 0; i < codegen.maxBailoutParameters; i++) {
        newParameters.add(', env$i');
      }
      return 'function($newParameters) {\n${codegen.setup}$buffer}';
    } else {
      SsaOptimizedCodeGenerator codegen = new SsaOptimizedCodeGenerator(
          compiler, work, buffer, parameters, parameterNames);
      codegen.visitGraph(graph);
      if (!codegen.guards.isEmpty()) {
        addBailoutVersion(codegen.guards, work);
      }
      return 'function($parameters) {\n$buffer}';
    }
  }

  void addBailoutVersion(List<HTypeGuard> guards, WorkItem work) {
    int length = guards.length;
    Map<int, BailoutInfo> bailouts = new Map<int, BailoutInfo>();
    int bailoutId = 1;
    guards.forEach((HTypeGuard guard) {
      if (guard.guarded is !HParameterValue) {
        int originalGuardedId = guard.originalGuardedId;
        BailoutInfo info = new BailoutInfo(originalGuardedId, bailoutId++);
        bailouts[originalGuardedId] = info;
      }
    });
    compiler.enqueue(new WorkItem.bailoutVersion(
        work.element, work.resolutionTree, bailouts));
  }
}

class SsaCodeGenerator implements HVisitor {
  final Compiler compiler;
  final WorkItem work;
  final StringBuffer buffer;
  final StringBuffer parameters;

  final Map<Element, String> parameterNames;
  final Map<int, String> names;
  final Map<String, int> prefixes;

  Element equalsNullElement;
  int indent = 0;
  HGraph currentGraph;
  HBasicBlock currentBlock;

  SsaCodeGenerator(this.compiler,
                   this.work,
                   this.buffer,
                   this.parameters,
                   this.parameterNames)
    : names = new Map<int, String>(),
      prefixes = new Map<String, int>() {
    for (final name in parameterNames.getValues()) {
      prefixes[name] = 0;
    }
    equalsNullElement =
        compiler.builder.interceptors.getEqualsNullInterceptor();
  }

  abstract visitTypeGuard(HTypeGuard node);
  abstract visitBailoutTarget(HBailoutTarget node);

  abstract beginGraph(HGraph graph);
  abstract endGraph(HGraph graph);

  abstract beginLoop(HBasicBlock block);
  abstract endLoop(HBasicBlock block);
  abstract handleLoopCondition(HLoopBranch node);

  abstract startIf(HIf node);
  abstract endIf(HIf node);
  abstract startThen(HIf node);
  abstract endThen(HIf node);
  abstract startElse(HIf node);
  abstract endElse(HIf node);

  visitGraph(HGraph graph) {
    currentGraph = graph;
    indent++;  // We are already inside a function.
    beginGraph(graph);
    visitBasicBlock(graph.entry);
    endGraph(graph);
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

    // While loop will be closed by the conditional loop-branch.
    // TODO(floitsch): HACK HACK HACK.
    if (currentBlock.isLoopHeader()) beginLoop(node);

    HInstruction instruction = node.first;
    while (instruction != null) {
      if (instruction is HGoto || instruction is HExit) {
        visit(instruction);
        return;
      } else if (!instruction.generateAtUseSite()) {
        if (instruction is !HIf && instruction is !HBailoutTarget) {
          addIndentation();
        }
        if (instruction.usedBy.isEmpty() || instruction is HLocal) {
          visit(instruction);
        } else {
          define(instruction);
        }
        // Control flow instructions know how to handle ';'.
        if (instruction is !HControlFlow && instruction is !HBailoutTarget) {
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

  visitEquals(HEquals node) {
    if (node.builtin) {
      buffer.add('(');
      use(node.left);
      buffer.add(' === ');
      use(node.right);
      buffer.add(')');
    } else if (node.element === equalsNullElement) {
      use(node.target);
      buffer.add('(');
      use(node.left);
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
    startIf(node);
    assert(!node.generateAtUseSite());
    startThen(node);
    visitBasicBlock(node.thenBlock);
    endThen(node);
    if (node.hasElse) {
      startElse(node);
      visitBasicBlock(node.elseBlock);
      endElse(node);
    }
    endIf(node);

    // Normally the HIf dominates the join-block. In this case there is one
    // dominated block that we need to visit:
    // If both the then and else blocks return/throw, then the join-block is
    // either the exit-block, or there is none.
    // We can also have the case where the HIf has no else, but the then-branch
    // terminates. If the code after the 'if' terminates, then the
    // if could become the dominator of the exit-block, thus having
    // three dominated blocks: the then, the code after the if, and the exit
    // block.

    List<HBasicBlock> dominated = node.block.dominatedBlocks;
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

  visitInvokeDynamicMethod(HInvokeDynamicMethod node) {
    use(node.receiver);
    buffer.add('.');
    // Remove 'this' from the number of arguments.
    int argumentCount = node.inputs.length - 1;
    buffer.add(compiler.namer.instanceMethodName(node.name, argumentCount));
    visitArguments(node.inputs);
    // Avoid adding the generative constructor name to the list of
    // seen selectors.
    if (node.inputs[0] is !HForeignNew) {
      compiler.registerDynamicInvocation(node.name, argumentCount);
    }
  }

  visitInvokeDynamicSetter(HInvokeDynamicSetter node) {
    use(node.receiver);
    buffer.add('.');
    buffer.add(compiler.namer.setterName(node.name));
    visitArguments(node.inputs);
    compiler.registerDynamicSetter(node.name);
  }

  visitInvokeDynamicGetter(HInvokeDynamicGetter node) {
    use(node.receiver);
    buffer.add('.');
    buffer.add(compiler.namer.getterName(node.name));
    visitArguments(node.inputs);
    compiler.registerDynamicGetter(node.name);
  }

  visitInvokeClosure(HInvokeClosure node) {
    use(node.receiver);
    buffer.add('.');
    buffer.add(compiler.namer.closureInvocationName(node.inputs.length - 1));
    visitArguments(node.inputs);
  }

  visitInvokeStatic(HInvokeStatic node) {
    use(node.target);
    visitArguments(node.inputs);
  }

  visitInvokeSuper(HInvokeSuper node) {
    Element superMethod = node.element;
    Element superClass = superMethod.enclosingElement;
    // Remove the element and 'this'.
    int argumentCount = node.inputs.length - 2;
    String className = compiler.namer.isolatePropertyAccess(superClass);
    String methodName = compiler.namer.instanceMethodName(
        superMethod.name, argumentCount);
    buffer.add('$className.prototype.$methodName.call');
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

  /**
   * Write the contents of the quoted string to a [StringBuffer] in
   * a form that is valid as JavaScript string literal content.
   * The string is assumed quoted by [quote] characters.
   */
  static void writeEscapedString(QuotedString string,
                                 StringBuffer buffer,
                                 int quote,
                                 void cancel(String reason)) {
    bool raw = string.quoting.raw;
    Iterator<int> iterator = string.iterator();
    while (iterator.hasNext()) {
      int code = iterator.next();
      if (code === quote) {
        // We need to add a backslash before quotes, both in normal
        // and in raw strings.
        buffer.add(@'\');
        buffer.add(code === $SQ ? "'" : '"');
      } else if (code === $LF) {
        // Newlines in strings only occur in multiline strings.
        // They need to be written using escapes in JS.
        assert(string.quoting.multiline);
        buffer.add(@'\n');
      } else if (code === $CR) {
        assert(string.quoting.multiline);
        buffer.add(@'\r');
      } else if (code === $LS) {
        // This Unicode line terminator and $PS are invalid in JS string
        // literals.
        buffer.add(@'\u2028');
      } else if (code === $PS) {
        buffer.add(@'\u2029');
      } else if (code !== $BACKSLASH) {
        buffer.add(new String.fromCharCodes([code]));
      } else if (raw) {
        buffer.add(@'\\');
      } else {
        assert(code === $BACKSLASH);
        code = iterator.next();
        switch (code) {
          case $u:
            buffer.add(@'\u');
            code = iterator.next();
            if (code == $OPEN_CURLY_BRACKET) {
              int value = 0;
              code = iterator.next();
              do {
                value = value * 16 + hexDigitValue(code);
                code = iterator.next();
              } while (code !== $CLOSE_CURLY_BRACKET);
              if (code > 0xffff) {
                cancel("Unhandled non-BMP character: " +
                       "U+${code.toRadixString(16)}");
              }
              for (int i = 12; i >= 0; i -= 4) {
                buffer.add(((value >> i) & 0xf).toRadixString(16));
              }
            } else {
              buffer.add(new String.fromCharCodes([code]));
              // Remaining three hex digits will be copied verbatim.
            }
            break;
          case $x:
            buffer.add(@'\x');
            // The two hex digits will be copied verbatim.
            break;
          // Character escapes that identical in meaning in JS.
          case $b: buffer.add(@'\b'); break;
          case $f: buffer.add(@'\f'); break;
          case $n: buffer.add(@'\n'); break;
          case $r: buffer.add(@'\r'); break;
          case $t: buffer.add(@'\t'); break;
          case $v: buffer.add(@'\v'); break;
          // Identity escapes that must be escaped in JS strings.
          case $BACKSLASH: buffer.add(@'\\'); break;
          case $LF: buffer.add(@'\n'); break;
          case $CR: buffer.add(@'\r'); break;
          case $LS: buffer.add(@'\u2028'); break;
          case $PS: buffer.add(@'\u2029'); break;
          // Quotes may or may not need the escape.
          case $SQ:
          case $DQ:
            // Only escape quotes if they match the generated string quotes.
            if (code == quote) buffer.add(@'\');
            buffer.add(code === $SQ ? "'" : '"');
            break;
          default:
            // All other escaped characters are identity escapes,
            // and don't need a backslash in JS.
            buffer.add(new String.fromCharCodes([code]));
            break;
        }
      }
    }
  }


  visitLiteral(HLiteral node) {
    if (node.isLiteralNull()) {
      buffer.add("(void 0)");
    } else if (node.value is num && node.value < 0) {
      buffer.add('(${node.value})');
    } else if (node.isLiteralString()) {
      QuotedString string = node.value;
      StringQuoting quoting = string.quoting;
      String quote = quoting.quoteChar;
      buffer.add(quote);
      writeEscapedString(string, buffer, quoting.quote,
                         (String reason) {
                           compiler.cancel(reason, instruction:node);
                         });
      buffer.add(quote);
    } else {
      buffer.add(node.value);
    }
  }

  visitLoopBranch(HLoopBranch node) {
    HBasicBlock branchBlock = currentBlock;
    handleLoopCondition(node);
    List<HBasicBlock> dominated = currentBlock.dominatedBlocks;
    // For a do while loop, the body has already been visited.
    if (!node.isDoWhile()) {
      visitBasicBlock(dominated[0]);
    }
    endLoop(node.block);
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

  void addIndentation() {
    for (int i = 0; i < indent; i++) {
      buffer.add('  ');
    }
  }

  void visitStatic(HStatic node) {
    compiler.registerStaticUse(node.element);
    buffer.add(compiler.namer.isolateAccess(node.element));
  }

  void visitStaticStore(HStaticStore node) {
    compiler.registerStaticUse(node.element);
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

  void visitIs(HIs node) {
    // TODO(ahe): Get Object class from somewhere instead.
    if (node.typeExpression.name == const SourceString('Object')) {
      // TODO(ahe): This probably belongs in the constant folder.
      if (node.expression.generateAtUseSite()) {
        buffer.add('((');
        visit(node.expression);
        buffer.add('), true)');
      } else {
        buffer.add('true');
      }
    } else {
      buffer.add('((');
      use(node.expression);
      buffer.add(').');
      buffer.add(compiler.namer.operatorIs(node.typeExpression));
      buffer.add(' === true)');
    }
  }
}

class SsaOptimizedCodeGenerator extends SsaCodeGenerator {
  final List<HTypeGuard> guards;
  int state = 0;

  SsaOptimizedCodeGenerator(compiler, work, buffer, parameters, parameterNames)
    : super(compiler, work, buffer, parameters, parameterNames),
      guards = <HTypeGuard>[];

  void beginGraph(HGraph graph) {}
  void endGraph(HGraph graph) {}

  void bailout(HTypeGuard guard, String reason) {
    guards.add(guard);
    HInstruction input = guard.guarded;
    Namer namer = compiler.namer;
    Element element = work.element;
    buffer.add('return ');
    if (element.isInstanceMember()) {
      // TODO(ngeoffray): This does not work in case we come from a
      // super call. We must make bailout names unique.
      buffer.add('this.${namer.getBailoutName(element)}');
    } else {
      buffer.add(namer.isolateBailoutAccess(element));
    }
    int parametersCount = parameterNames.length;
    buffer.add('($parameters');
    if (parametersCount != 0) buffer.add(', ');
    if (guard.guarded is !HParameterValue) {
      buffer.add('${++state}');
      bool first = true;
      // TODO(ngeoffray): if the bailout method takes more arguments,
      // fill the remaining arguments with undefined.
      // TODO(ngeoffray): try to put a variable at a deterministic
      // location, so that multiple bailout calls put the variable at
      // the same parameter index.
      for (int i = 0; i < guard.inputs.length; i++) {
        HInstruction input = guard.inputs[i];
        buffer.add(', ');
        use(guard.inputs[i]);
      }
    } else {
      assert(guard.guarded is HParameterValue);
      buffer.add(' 0');
    }
    buffer.add(')');
  }

  void visitTypeGuard(HTypeGuard node) {
    HInstruction input = node.guarded;
    assert(!input.generateAtUseSite() || input is HParameterValue);
    if (node.isInteger()) {
      buffer.add('if (');
      use(input);
      buffer.add(' !== (');
      use(input);
      buffer.add(' | 0)) ');
      bailout(node, 'Not an integer');
    } else if (node.isNumber()) {
      buffer.add('if (typeof ');
      use(input);
      buffer.add(" !== 'number') ");
      bailout(node, 'Not a number');
    } else if (node.isBoolean()) {
      buffer.add('if (typeof ');
      use(input);
      buffer.add(" !== 'boolean') ");
      bailout(node, 'Not a boolean');
    } else if (node.isString()) {
      buffer.add('if (typeof ');
      use(input);
      buffer.add(" !== 'string') ");
      bailout(node, 'Not a string');
    } else if (node.isArray()) {
      buffer.add('if (');
      use(input);
      buffer.add(".constructor !== Array) ");
      bailout(node, 'Not an array');
    } else if (node.isStringOrArray()) {
      buffer.add('if (typeof ');
      use(input);
      buffer.add(" !== 'string' && ");
      use(input);
      buffer.add(".constructor !== Array) ");
      bailout(node, 'Not a string or array');
    } else {
      unreachable();
    }
  }

  void beginLoop(HBasicBlock block) {
    addIndentation();
    buffer.add('while (true) {\n');
    indent++;
  }

  void endLoop(HBasicBlock block) {
    indent--;
    addIndentation();
    buffer.add('}\n');  // Close 'while' loop.
  }

  void handleLoopCondition(HLoopBranch node) {
    buffer.add('if (!(');
    use(node.inputs[0]);
    buffer.add(')) break;\n');
  }

  void startIf(HIf node) {
  }

  void endIf(HIf node) {
    indent--;
    addIndentation();
    buffer.add('}\n');
  }

  void startThen(HIf node) {
    addIndentation();
    buffer.add('if (');
    use(node.inputs[0]);
    buffer.add(') {\n');
    indent++;
  }

  void endThen(HIf node) {
  }

  void startElse(HIf node) {
    indent--;
    addIndentation();
    buffer.add('} else {\n');
    indent++;
  }

  void endElse(HIf node) {
  }

  void visitBailoutTarget(HBailoutTarget target) {
    compiler.internalError('Bailout target in an optimized method');
  }
}

class SsaUnoptimizedCodeGenerator extends SsaCodeGenerator {

  final StringBuffer setup;
  final List<String> labels;
  int labelId = 0;
  int maxBailoutParameters = 0;

  SsaUnoptimizedCodeGenerator(
      compiler, work, buffer, parameters, parameterNames)
    : super(compiler, work, buffer, parameters, parameterNames),
      setup = new StringBuffer(),
      labels = <String>[];

  String pushLabel() {
    String label = 'L${labelId++}';
    labels.addLast(label);
    return label;
  }

  String popLabel() {
    return labels.removeLast();
  }

  String currentLabel() {
    return labels.last();
  }

  void beginGraph(HGraph graph) {
    if (!graph.entry.hasBailouts()) return;
    addIndentation();
    buffer.add('switch (state) {\n');
    indent++;
    addIndentation();
    buffer.add('case 0:\n');
    indent++;

    // The setup phase of a bailout function sets up the environment for
    // each bailout target. Each bailout target will populate this
    // setup phase. It is put at the beginning of the function.
    setup.add('  switch (state) {\n');
  }

  void endGraph(HGraph graph) {
    if (!graph.entry.hasBailouts()) return;
    indent--; // Close original case.
    indent--;
    addIndentation();
    buffer.add('}\n');  // Close 'switch'.
    setup.add('  }\n');
  }

  void visitTypeGuard(HTypeGuard guard) {
    compiler.internalError('Type guard in an unoptimized method');
  }

  void visitBailoutTarget(HBailoutTarget node) {
    indent--;
    addIndentation();
    buffer.add('case ${node.state}:\n');
    indent++;
    addIndentation();
    buffer.add('state = 0;\n');

    setup.add('    case ${node.state}:\n');
    int i = 0;
    for (HInstruction input in node.inputs) {
      setup.add('      ${temporary(input)} = env$i;\n');
      if (input is HLoad) {
        // We get the load of a phi that was turned into a local in
        // the environment. Update the local with that load.
        HLoad load = input;
        setup.add('      ${local(load.local)} = env$i;\n');
      }
      i++;
    }
    if (i > maxBailoutParameters) maxBailoutParameters = i;
    setup.add('      break;\n');
  }

  void startBailoutCase(List<HBailoutTarget> bailouts1,
                        List<HBailoutTarget> bailouts2) {
    indent--;
    handleBailoutCase(bailouts1);
    handleBailoutCase(bailouts2);
    indent++;
  }

  void handleBailoutCase(List<HBailoutTarget> bailouts) {
    for (int i = 0, len = bailouts.length; i < len; i++) {
      addIndentation();
      buffer.add('case ${bailouts[i].state}:\n');
    }
  }

  void startBailoutSwitch() {
    addIndentation();
    buffer.add('switch (state) {\n');
    indent++;
    addIndentation();
    buffer.add('case 0:\n');
    indent++;
  }

  void endBailoutSwitch() {
    indent--; // Close 'case'.
    indent--;
    addIndentation();
    buffer.add('}\n');  // Close 'switch'.
  }

  void beginLoop(HBasicBlock block) {
    // TODO(ngeoffray): Don't put labels on loops that don't bailout.
    String newLabel = pushLabel();
    if (block.hasBailouts()) {
      startBailoutCase(block.bailouts, const <HBailoutTarget>[]);
    }

    addIndentation();
    buffer.add('$newLabel: while (true) {\n');
    indent++;

    if (block.hasBailouts()) {
      startBailoutSwitch();
    }
  }

  void endLoop(HBasicBlock block) {
    popLabel();
    HBasicBlock header = block.isLoopHeader() ? block : block.parentLoopHeader;
    if (header.hasBailouts()) {
      endBailoutSwitch();
    }
    indent--;
    addIndentation();
    buffer.add('}\n');  // Close 'while'.
  }

  void handleLoopCondition(HLoopBranch node) {
    buffer.add('if (!(');
    use(node.inputs[0]);
    buffer.add(')) break ${currentLabel()};\n');
  }

  void startIf(HIf node) {
    bool hasBailouts = node.thenBlock.hasBailouts()
        || (node.hasElse && node.elseBlock.hasBailouts());
    if (hasBailouts) {
      startBailoutCase(node.thenBlock.bailouts,
          node.hasElse ? node.elseBlock.bailouts : const <HBailoutTarget>[]);
    }
  }

  void endIf(HIf node) {
    indent--;
    addIndentation();
    buffer.add('}\n');
  }

  void startThen(HIf node) {
    addIndentation();
    bool hasBailouts = node.thenBlock.hasBailouts()
        || (node.hasElse && node.elseBlock.hasBailouts());
    buffer.add('if (');
    if (hasBailouts) {
      // TODO(ngeoffray): Put the condition initialization in the
      // [setup] buffer.
      List<HBailoutTarget> bailouts = node.thenBlock.bailouts;
      for (int i = 0, len = bailouts.length; i < len; i++) {
        buffer.add('state == ${bailouts[i].state} || ');
      }
      buffer.add('(state == 0 && ');
    }
    use(node.inputs[0]);
    if (hasBailouts) {
      buffer.add(')');
    }
    buffer.add(') {\n');
    indent++;
    if (node.thenBlock.hasBailouts()) {
      startBailoutSwitch();
    }
  }

  void endThen(HIf node) {
    if (node.thenBlock.hasBailouts()) {
      endBailoutSwitch();
    }
  }

  void startElse(HIf node) {
    indent--;
    addIndentation();
    buffer.add('} else {\n');
    indent++;
    if (node.elseBlock.hasBailouts()) {
      startBailoutSwitch();
    }
  }

  void endElse(HIf node) {
    if (node.elseBlock.hasBailouts()) {
      endBailoutSwitch();
    }
  }
}
