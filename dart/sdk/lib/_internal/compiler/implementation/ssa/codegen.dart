// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of ssa;

class SsaCodeGeneratorTask extends CompilerTask {

  final JavaScriptBackend backend;

  SsaCodeGeneratorTask(JavaScriptBackend backend)
      : this.backend = backend,
        super(backend.compiler);
  String get name => 'SSA code generator';
  NativeEmitter get nativeEmitter => backend.emitter.nativeEmitter;


  js.Node attachPosition(js.Node node, Element element) {
    // TODO(sra): Attaching positions might be cleaner if the source position
    // was on a wrapping node.
    SourceFile sourceFile = sourceFileOfElement(element);
    Node expression = element.implementation.parseNode(backend.compiler);
    Token beginToken;
    Token endToken;
    if (expression == null) {
      // Synthesized node. Use the enclosing element for the location.
      beginToken = endToken = element.position();
    } else {
      beginToken = expression.getBeginToken();
      endToken = expression.getEndToken();
    }
    // TODO(podivilov): find the right sourceFile here and remove offset checks
    // below.
    if (beginToken.charOffset < sourceFile.length) {
      node.sourcePosition = new SourceFileLocation(sourceFile, beginToken);
    }
    if (endToken.charOffset < sourceFile.length) {
      node.endSourcePosition = new SourceFileLocation(sourceFile, endToken);
    }
    return node;
  }

  SourceFile sourceFileOfElement(Element element) {
    // TODO(johnniwinther): remove the 'element.patch' hack.
    FunctionElement functionElement = element.asFunctionElement();
    if (functionElement != null && functionElement.patch != null) {
      element = functionElement.patch;
    }
    return element.getCompilationUnit().script.file;
  }

  js.Fun buildJavaScriptFunction(FunctionElement element,
                                 List<js.Parameter> parameters,
                                 js.Block body) {
    return attachPosition(new js.Fun(parameters, body), element);
  }

  CodeBuffer prettyPrint(js.Node node) {
    var code = js.prettyPrint(node, compiler, allowVariableMinification: true);
    return code;
  }

  js.Expression generateCode(CodegenWorkItem work, HGraph graph) {
    if (work.element.isField()) {
      return generateLazyInitializer(work, graph);
    } else {
      return generateMethod(work, graph);
    }
  }

  js.Expression generateLazyInitializer(work, graph) {
    return measure(() {
      compiler.tracer.traceGraph("codegen", graph);
      SsaCodeGenerator codegen = new SsaCodeGenerator(backend, work);
      codegen.visitGraph(graph);
      return new js.Fun(codegen.parameters,
          attachPosition(codegen.body, work.element));
    });
  }

  js.Expression generateMethod(CodegenWorkItem work, HGraph graph) {
    return measure(() {
      SsaCodeGenerator codegen = new SsaCodeGenerator(backend, work);
      codegen.visitGraph(graph);
      compiler.tracer.traceGraph("codegen", graph);
      FunctionElement element = work.element;
      return buildJavaScriptFunction(element, codegen.parameters, codegen.body);
    });
  }
}

typedef void ElementAction(Element element);

class SsaCodeGenerator implements HVisitor, HBlockInformationVisitor {
  /**
   * Returned by [expressionType] to tell how code can be generated for
   * a subgraph.
   * - [TYPE_STATEMENT] means that the graph must be generated as a statement,
   * which is always possible.
   * - [TYPE_EXPRESSION] means that the graph can be generated as an expression,
   * or possibly several comma-separated expressions.
   * - [TYPE_DECLARATION] means that the graph can be generated as an
   * expression, and that it only generates expressions of the form
   *   variable = expression
   * which are also valid as parts of a "var" declaration.
   */
  static const int TYPE_STATEMENT = 0;
  static const int TYPE_EXPRESSION = 1;
  static const int TYPE_DECLARATION = 2;

  /**
   * Whether we are currently generating expressions instead of statements.
   * This includes declarations, which are generated as expressions.
   */
  bool isGeneratingExpression = false;

  final JavaScriptBackend backend;
  final CodegenWorkItem work;

  final Set<HInstruction> generateAtUseSite;
  final Set<HInstruction> controlFlowOperators;
  final Map<Element, ElementAction> breakAction;
  final Map<Element, ElementAction> continueAction;
  final List<js.Parameter> parameters;

  js.Block currentContainer;
  js.Block get body => currentContainer;
  List<js.Expression> expressionStack;
  List<js.Block> oldContainerStack;

  /**
   * Contains the names of the instructions, as well as the parallel
   * copies to perform on block transitioning.
   */
  VariableNames variableNames;
  bool shouldGroupVarDeclarations = false;

  /**
   * While generating expressions, we can't insert variable declarations.
   * Instead we declare them at the start of the function.  When minifying
   * we do this most of the time, because it reduces the size unless there
   * is only one variable.
   */
  final Set<String> collectedVariableDeclarations;

  /**
   * Set of variables and parameters that have already been declared.
   */
  final Set<String> declaredLocals;

  int indent = 0;
  HGraph currentGraph;

  // Records a block-information that is being handled specially.
  // Used to break bad recursion.
  HBlockInformation currentBlockInformation;
  // The subgraph is used to delimit traversal for some constructions, e.g.,
  // if branches.
  SubGraph subGraph;

  SsaCodeGenerator(this.backend, CodegenWorkItem work)
    : this.work = work,
      declaredLocals = new Set<String>(),
      collectedVariableDeclarations = new Set<String>(),
      currentContainer = new js.Block.empty(),
      parameters = <js.Parameter>[],
      expressionStack = <js.Expression>[],
      oldContainerStack = <js.Block>[],
      generateAtUseSite = new Set<HInstruction>(),
      controlFlowOperators = new Set<HInstruction>(),
      breakAction = new Map<Element, ElementAction>(),
      continueAction = new Map<Element, ElementAction>();

  Compiler get compiler => backend.compiler;
  NativeEmitter get nativeEmitter => backend.emitter.nativeEmitter;
  CodegenEnqueuer get world => backend.compiler.enqueuer.codegen;

  bool isGenerateAtUseSite(HInstruction instruction) {
    return generateAtUseSite.contains(instruction);
  }

  bool isNonNegativeInt32Constant(HInstruction instruction) {
    if (instruction.isConstantInteger()) {
      HConstant constantInstruction = instruction;
      PrimitiveConstant primitiveConstant = constantInstruction.constant;
      int value = primitiveConstant.value;
      if (value >= 0 && value < (1 << 31)) {
        return true;
      }
    }
    return false;
  }

  bool hasNonBitOpUser(HInstruction instruction, Set<HPhi> phiSet) {
    for (HInstruction user in instruction.usedBy) {
      if (user is HPhi) {
        if (!phiSet.contains(user)) {
          phiSet.add(user);
          if (hasNonBitOpUser(user, phiSet)) return true;
        }
      } else if (user is! HBitNot && user is! HBinaryBitOp) {
        return true;
      }
    }
    return false;
  }

  // We want the outcome of bit-operations to be positive. However, if
  // the result of a bit-operation is only used by other bit
  // operations we do not have to convert to an unsigned
  // integer. Also, if we are using & with a positive constant we know
  // that the result is positive already and need no conversion.
  bool requiresUintConversion(HInstruction instruction) {
    if (instruction is HBitAnd) {
      HBitAnd bitAnd = instruction;
      if (isNonNegativeInt32Constant(bitAnd.left) ||
          isNonNegativeInt32Constant(bitAnd.right)) {
        return false;
      }
    }
    return hasNonBitOpUser(instruction, new Set<HPhi>());
  }

  /**
   * If the [instruction] is not `null` it will be used to attach the position
   * to the [statement].
   */
  void pushStatement(js.Statement statement, [HInstruction instruction]) {
    assert(expressionStack.isEmpty);
    if (instruction != null) {
      attachLocation(statement, instruction);
    }
    currentContainer.statements.add(statement);
  }

  void insertStatementAtStart(js.Statement statement) {
    currentContainer.statements.insert(0, statement);
  }

  /**
   * If the [instruction] is not `null` it will be used to attach the position
   * to the [expression].
   */
  pushExpressionAsStatement(js.Expression expression,
                            [HInstruction instruction]) {
    pushStatement(new js.ExpressionStatement(expression), instruction);
  }

  /**
   * If the [instruction] is not `null` it will be used to attach the position
   * to the [expression].
   */
  push(js.Expression expression, [HInstruction instruction]) {
    if (instruction != null) {
      attachLocation(expression, instruction);
    }
    expressionStack.add(expression);
  }

  js.Expression pop() {
    return expressionStack.removeLast();
  }

  attachLocationToLast(HInstruction instruction) {
    attachLocation(expressionStack.last, instruction);
  }

  js.Node attachLocation(js.Node jsNode, HInstruction instruction) {
    jsNode.sourcePosition = instruction.sourcePosition;
    return jsNode;
  }

  js.Node attachLocationRange(js.Node jsNode,
                              SourceFileLocation sourcePosition,
                              SourceFileLocation endSourcePosition) {
    jsNode.sourcePosition = sourcePosition;
    jsNode.endSourcePosition = endSourcePosition;
    return jsNode;
  }

  void preGenerateMethod(HGraph graph) {
    new SsaTypeKnownRemover().visitGraph(graph);
    new SsaInstructionMerger(generateAtUseSite, compiler).visitGraph(graph);
    new SsaConditionMerger(
        generateAtUseSite, controlFlowOperators).visitGraph(graph);
    SsaLiveIntervalBuilder intervalBuilder = new SsaLiveIntervalBuilder(
        compiler, generateAtUseSite, controlFlowOperators);
    intervalBuilder.visitGraph(graph);
    SsaVariableAllocator allocator = new SsaVariableAllocator(
        compiler,
        intervalBuilder.liveInstructions,
        intervalBuilder.liveIntervals,
        generateAtUseSite);
    allocator.visitGraph(graph);
    variableNames = allocator.names;
    shouldGroupVarDeclarations = allocator.names.numberOfVariables > 1;
  }

  void handleDelayedVariableDeclarations() {
    // If we have only one variable declaration and the first statement is an
    // assignment to that variable then we can merge the two.  We count the
    // number of variables in the variable allocator to try to avoid this issue,
    // but it sometimes happens that the variable allocator introduces a
    // temporary variable that it later eliminates.
    if (!collectedVariableDeclarations.isEmpty) {
      if (collectedVariableDeclarations.length == 1 &&
          currentContainer.statements.length >= 1 &&
          currentContainer.statements[0] is js.ExpressionStatement) {
        String name = collectedVariableDeclarations.first;
        js.ExpressionStatement statement = currentContainer.statements[0];
        if (statement.expression is js.Assignment) {
          js.Assignment assignment = statement.expression;
          if (!assignment.isCompound &&
              assignment.leftHandSide is js.VariableReference) {
            js.VariableReference variableReference = assignment.leftHandSide;
            if (variableReference.name == name) {
              js.VariableDeclaration decl = new js.VariableDeclaration(name);
              js.VariableInitialization initialization =
                  new js.VariableInitialization(decl, assignment.value);
              currentContainer.statements[0] = new js.ExpressionStatement(
                  new js.VariableDeclarationList([initialization]));
              return;
            }
          }
        }
      }
      // If we can't merge the declaration with the first assignment then we
      // just do it with a new var z,y,x; statement.
      List<js.VariableInitialization> declarations =
          <js.VariableInitialization>[];
      collectedVariableDeclarations.forEach((String name) {
        declarations.add(new js.VariableInitialization(
            new js.VariableDeclaration(name), null));
      });
      var declarationList = new js.VariableDeclarationList(declarations);
      insertStatementAtStart(new js.ExpressionStatement(declarationList));
    }
  }

  visitGraph(HGraph graph) {
    preGenerateMethod(graph);
    currentGraph = graph;
    indent++;  // We are already inside a function.
    subGraph = new SubGraph(graph.entry, graph.exit);
    visitBasicBlock(graph.entry);
    handleDelayedVariableDeclarations();
  }

  void visitSubGraph(SubGraph newSubGraph) {
    SubGraph oldSubGraph = subGraph;
    subGraph = newSubGraph;
    visitBasicBlock(subGraph.start);
    subGraph = oldSubGraph;
  }

  /**
   * Check whether a sub-graph can be generated as an expression, or even
   * as a declaration, or if it has to fall back to being generated as
   * a statement.
   * Expressions are anything that doesn't generate control flow constructs.
   * Declarations must only generate assignments on the form "id = expression",
   * and not, e.g., expressions where the value isn't assigned, or where it's
   * assigned to something that's not a simple variable.
   */
  int expressionType(HExpressionInformation info) {
    // The only HExpressionInformation used as part of a HBlockInformation is
    // current HSubExpressionBlockInformation, so it's the only one reaching
    // here. If we start using the other HExpressionInformation types too,
    // this code should be generalized.
    assert(info is HSubExpressionBlockInformation);
    HSubExpressionBlockInformation expressionInfo = info;
    SubGraph limits = expressionInfo.subExpression;

    // Start assuming that we can generate declarations. If we find a
    // counter-example, we degrade our assumption to either expression or
    // statement, and in the latter case, we can return immediately since
    // it can't get any worse. E.g., a function call where the return value
    // isn't used can't be in a declaration.
    int result = TYPE_DECLARATION;
    HBasicBlock basicBlock = limits.start;
    do {
      HInstruction current = basicBlock.first;
      while (current != basicBlock.last) {
        // E.g, bounds check.
        if (current.isControlFlow()) {
          return TYPE_STATEMENT;
        }
        // HFieldSet generates code on the form x.y = ..., which isn't
        // valid in a declaration, but it also always have no uses, so
        // it's caught by that test too.
        assert(current is! HFieldSet || current.usedBy.isEmpty);
        if (current.usedBy.isEmpty) {
          result = TYPE_EXPRESSION;
        }
        current = current.next;
      }
      if (current is HGoto) {
        basicBlock = basicBlock.successors[0];
      } else if (current is HConditionalBranch) {
        if (generateAtUseSite.contains(current)) {
          // Short-circuit control flow operator trickery.
          // Check the second half, which will continue into the join.
          // (The first half is [inputs[0]], the second half is [successors[0]],
          // and [successors[1]] is the join-block).
          basicBlock = basicBlock.successors[0];
        } else {
          // We allow an expression to end on an HIf (a condition expression).
          return identical(basicBlock, limits.end) ? result : TYPE_STATEMENT;
        }
      } else {
        // Expression-incompatible control flow.
        return TYPE_STATEMENT;
      }
    } while (limits.contains(basicBlock));
    return result;
  }

  bool isJSExpression(HExpressionInformation info) {
    return !identical(expressionType(info), TYPE_STATEMENT);
  }

  bool isJSDeclaration(HExpressionInformation info) {
    return identical(expressionType(info), TYPE_DECLARATION);
  }

  bool isJSCondition(HExpressionInformation info) {
    HSubExpressionBlockInformation graph = info;
    SubExpression limits = graph.subExpression;
    return !identical(expressionType(info), TYPE_STATEMENT) &&
       (limits.end.last is HConditionalBranch);
  }

  /**
   * Generate statements from block information.
   * If the block information contains expressions, generate only
   * assignments, and if it ends in a conditional branch, don't generate
   * the condition.
   */
  void generateStatements(HBlockInformation block) {
    if (block is HStatementInformation) {
      block.accept(this);
    } else {
      HSubExpressionBlockInformation expression = block;
      visitSubGraph(expression.subExpression);
    }
  }

  js.Block generateStatementsInNewBlock(HBlockInformation block) {
    js.Block result = new js.Block.empty();
    js.Block oldContainer = currentContainer;
    currentContainer = result;
    generateStatements(block);
    currentContainer = oldContainer;
    return result;
  }

  /**
   * If the [block] only contains one statement returns that statement. If the
   * that statement itself is a block, recursively calls this method.
   *
   * If the block is empty, returns a new instance of [js.NOP].
   */
  js.Statement unwrapStatement(js.Block block) {
    int len = block.statements.length;
    if (len == 0) return new js.EmptyStatement();
    if (len == 1) {
      js.Statement result = block.statements[0];
      if (result is Block) return unwrapStatement(result);
      return result;
    }
    return block;
  }

  /**
   * Generate expressions from block information.
   */
  js.Expression generateExpression(HExpressionInformation expression) {
    // Currently we only handle sub-expression graphs.
    assert(expression is HSubExpressionBlockInformation);

    bool oldIsGeneratingExpression = isGeneratingExpression;
    isGeneratingExpression = true;
    List<js.Expression> oldExpressionStack = expressionStack;
    List<js.Expression> sequenceElements = <js.Expression>[];
    expressionStack = sequenceElements;
    HSubExpressionBlockInformation expressionSubGraph = expression;
    visitSubGraph(expressionSubGraph.subExpression);
    expressionStack = oldExpressionStack;
    isGeneratingExpression = oldIsGeneratingExpression;
    if (sequenceElements.isEmpty) {
      // Happens when the initializer, condition or update of a loop is empty.
      return null;
    } else if (sequenceElements.length == 1) {
      return sequenceElements[0];
    } else {
      return new js.Sequence(sequenceElements);
    }
  }

  /**
    * Only visits the arguments starting at inputs[HInvoke.ARGUMENTS_OFFSET].
    */
  List<js.Expression> visitArguments(List<HInstruction> inputs,
                                     {int start: HInvoke.ARGUMENTS_OFFSET}) {
    assert(inputs.length >= start);
    List<js.Expression> result = <js.Expression>[];
    for (int i = start; i < inputs.length; i++) {
      use(inputs[i]);
      result.add(pop());
    }
    return result;
  }

  bool isVariableDeclared(String variableName) {
    return declaredLocals.contains(variableName) ||
        collectedVariableDeclarations.contains(variableName);
  }

  js.Expression generateExpressionAssignment(String variableName,
                                             js.Expression value) {
    if (value is js.Binary) {
      js.Binary binary = value;
      String op = binary.op;
      if (op == '+' || op == '-' || op == '/' || op == '*' || op == '%' ||
          op == '^' || op == '&' || op == '|') {
        if (binary.left is js.VariableUse &&
            (binary.left as js.VariableUse).name == variableName) {
          // We know now, that we can shorten x = x + y into x += y.
          // Also check for the shortcut where y equals 1: x++ and x--.
          if ((op == '+' || op == '-') &&
              binary.right is js.LiteralNumber &&
              (binary.right as js.LiteralNumber).value == "1") {
            return new js.Prefix(op == '+' ? '++' : '--', binary.left);
          }
          return new js.Assignment.compound(binary.left, op, binary.right);
        }
      }
    }
    return new js.Assignment(new js.VariableUse(variableName), value);
  }

  void assignVariable(String variableName, js.Expression value) {
    if (isGeneratingExpression) {
      // If we are in an expression then we can't declare the variable here.
      // We have no choice, but to use it and then declare it separately.
      if (!isVariableDeclared(variableName)) {
        collectedVariableDeclarations.add(variableName);
      }
      push(generateExpressionAssignment(variableName, value));
      // Otherwise if we are trying to declare inline and we are in a statement
      // then we declare (unless it was already declared).
    } else if (!shouldGroupVarDeclarations &&
               !declaredLocals.contains(variableName)) {
      // It may be necessary to remove it from the ones to be declared later.
      collectedVariableDeclarations.remove(variableName);
      declaredLocals.add(variableName);
      js.VariableDeclaration decl = new js.VariableDeclaration(variableName);
      js.VariableInitialization initialization =
          new js.VariableInitialization(decl, value);

      pushExpressionAsStatement(new js.VariableDeclarationList(
          <js.VariableInitialization>[initialization]));
    } else {
      // Otherwise we are just going to use it.  If we have not already declared
      // it then we make sure we will declare it later.
      if (!declaredLocals.contains(variableName)) {
        collectedVariableDeclarations.add(variableName);
      }
      pushExpressionAsStatement(
          generateExpressionAssignment(variableName, value));
    }
  }

  void define(HInstruction instruction) {
    // For simple type checks like i = intTypeCheck(i), we don't have to
    // emit an assignment, because the intTypeCheck just returns its
    // argument.
    bool needsAssignment = true;
    if (instruction is HTypeConversion) {
      HTypeConversion typeConversion = instruction;
      String inputName = variableNames.getName(typeConversion.checkedInput);
      if (variableNames.getName(instruction) == inputName) {
        needsAssignment = false;
      }
    }
    if (instruction is HLocalValue) {
      needsAssignment = false;
    }

    if (needsAssignment &&
        !instruction.isControlFlow() && variableNames.hasName(instruction)) {
      visitExpression(instruction);
      assignVariable(variableNames.getName(instruction), pop());
      return;
    }

    if (isGeneratingExpression) {
      visitExpression(instruction);
    } else {
      visitStatement(instruction);
    }
  }

  void use(HInstruction argument) {
    if (isGenerateAtUseSite(argument)) {
      visitExpression(argument);
    } else if (argument is HCheck && !variableNames.hasName(argument)) {
      HCheck check = argument;
      // This can only happen if the checked node does not have a name.
      assert(!variableNames.hasName(check.checkedInput));
      use(check.checkedInput);
    } else {
      assert(variableNames.hasName(argument));
      push(new js.VariableUse(variableNames.getName(argument)));
    }
  }

  visit(HInstruction node) {
    node.accept(this);
  }

  visitExpression(HInstruction node) {
    bool oldIsGeneratingExpression = isGeneratingExpression;
    isGeneratingExpression = true;
    visit(node);
    isGeneratingExpression = oldIsGeneratingExpression;
  }

  visitStatement(HInstruction node) {
    assert(!isGeneratingExpression);
    visit(node);
    if (!expressionStack.isEmpty) {
      assert(expressionStack.length == 1);
      pushExpressionAsStatement(pop());
    }
  }

  void continueAsBreak(LabelElement target) {
    pushStatement(new js.Break(backend.namer.continueLabelName(target)));
  }

  void implicitContinueAsBreak(TargetElement target) {
    pushStatement(new js.Break(
        backend.namer.implicitContinueLabelName(target)));
  }

  void implicitBreakWithLabel(TargetElement target) {
    pushStatement(new js.Break(backend.namer.implicitBreakLabelName(target)));
  }

  js.Statement wrapIntoLabels(js.Statement result, List<LabelElement> labels) {
    for (LabelElement label in labels) {
      if (label.isTarget) {
        String breakLabelString = backend.namer.breakLabelName(label);
        result = new js.LabeledStatement(breakLabelString, result);
      }
    }
    return result;
  }


  // The regular [visitIf] method implements the needed logic.
  bool visitIfInfo(HIfBlockInformation info) => false;

  bool visitSwitchInfo(HSwitchBlockInformation info) {
    bool isExpression = isJSExpression(info.expression);
    if (!isExpression) {
      generateStatements(info.expression);
    }

    if (isExpression) {
      push(generateExpression(info.expression));
    } else {
      use(info.expression.conditionExpression);
    }
    js.Expression key = pop();
    List<js.SwitchClause> cases = <js.SwitchClause>[];

    js.Block oldContainer = currentContainer;
    for (int i = 0; i < info.matchExpressions.length; i++) {
      for (Constant constant in info.matchExpressions[i]) {
        generateConstant(constant);
        currentContainer = new js.Block.empty();
        cases.add(new js.Case(pop(), currentContainer));
      }
      if (i == info.matchExpressions.length - 1) {
        currentContainer = new js.Block.empty();
        cases.add(new js.Default(currentContainer));
      }
      generateStatements(info.statements[i]);
    }
    currentContainer = oldContainer;

    js.Statement result = new js.Switch(key, cases);
    pushStatement(wrapIntoLabels(result, info.labels));
    return true;
  }

  bool visitSequenceInfo(HStatementSequenceInformation info) {
    return false;
  }

  bool visitSubGraphInfo(HSubGraphBlockInformation info) {
    visitSubGraph(info.subGraph);
    return true;
  }

  bool visitSubExpressionInfo(HSubExpressionBlockInformation info) {
    return false;
  }

  bool visitAndOrInfo(HAndOrBlockInformation info) {
    return false;
  }

  bool visitTryInfo(HTryBlockInformation info) {
    js.Block body = generateStatementsInNewBlock(info.body);
    js.Catch catchPart = null;
    js.Block finallyPart = null;
    if (info.catchBlock != null) {
      void register(ClassElement classElement) {
        if (classElement != null) {
          world.registerInstantiatedClass(classElement, work.resolutionTree);
        }
      }
      register(backend.jsPlainJavaScriptObjectClass);
      register(backend.jsUnknownJavaScriptObjectClass);

      HLocalValue exception = info.catchVariable;
      String name = variableNames.getName(exception);
      js.VariableDeclaration decl = new js.VariableDeclaration(name);
      js.Block catchBlock = generateStatementsInNewBlock(info.catchBlock);
      catchPart = new js.Catch(decl, catchBlock);
    }
    if (info.finallyBlock != null) {
      finallyPart = generateStatementsInNewBlock(info.finallyBlock);
    }
    pushStatement(new js.Try(body, catchPart, finallyPart));
    return true;
  }

  void visitBodyIgnoreLabels(HLoopBlockInformation info) {
    if (info.body.start.isLabeledBlock()) {
      HBlockInformation oldInfo = currentBlockInformation;
      currentBlockInformation = info.body.start.blockFlow.body;
      generateStatements(info.body);
      currentBlockInformation = oldInfo;
    } else {
      generateStatements(info.body);
    }
  }

  bool visitLoopInfo(HLoopBlockInformation info) {
    HExpressionInformation condition = info.condition;
    bool isConditionExpression = isJSCondition(condition);

    js.Loop loop;

    switch (info.kind) {
      // Treate all three "test-first" loops the same way.
      case HLoopBlockInformation.FOR_LOOP:
      case HLoopBlockInformation.WHILE_LOOP:
      case HLoopBlockInformation.FOR_IN_LOOP:
      case HLoopBlockInformation.SWITCH_CONTINUE_LOOP:
        HBlockInformation initialization = info.initializer;
        int initializationType = TYPE_STATEMENT;
        if (initialization != null) {
          initializationType = expressionType(initialization);
          if (initializationType == TYPE_STATEMENT) {
            generateStatements(initialization);
            initialization = null;
          }
        }
        if (isConditionExpression &&
            info.updates != null && isJSExpression(info.updates)) {
          // If we have an updates graph, and it's expressible as an
          // expression, generate a for-loop.
          js.Expression jsInitialization = null;
          if (initialization != null) {
            int delayedVariablesCount = collectedVariableDeclarations.length;
            jsInitialization = generateExpression(initialization);
            if (!shouldGroupVarDeclarations &&
                delayedVariablesCount < collectedVariableDeclarations.length) {
              // We just added a new delayed variable-declaration. See if we
              // can put in a 'var' in front of the initialization to make it
              // go away.
              List<js.Expression> expressions;
              if (jsInitialization is js.Sequence) {
                js.Sequence sequence = jsInitialization;
                expressions = sequence.expressions;
              } else {
                expressions = <js.Expression>[jsInitialization];
              }
              bool canTransformToVariableDeclaration = true;
              for (js.Expression expression in expressions) {
                bool expressionIsVariableAssignment = false;
                if (expression is js.Assignment) {
                  js.Assignment assignment = expression;
                  if (assignment.leftHandSide is js.VariableUse &&
                      assignment.compoundTarget == null) {
                    expressionIsVariableAssignment = true;
                  }
                }
                if (!expressionIsVariableAssignment) {
                  canTransformToVariableDeclaration = false;
                  break;
                }
              }
              if (canTransformToVariableDeclaration) {
                List<js.VariableInitialization> inits =
                    <js.VariableInitialization>[];
                for (js.Assignment assignment in expressions) {
                  String id = (assignment.leftHandSide as js.VariableUse).name;
                  js.Node declaration = new js.VariableDeclaration(id);
                  inits.add(new js.VariableInitialization(declaration,
                                                          assignment.value));
                  collectedVariableDeclarations.remove(id);
                  declaredLocals.add(id);
                }
                jsInitialization = new js.VariableDeclarationList(inits);
              }
            }
          }
          js.Expression jsCondition = generateExpression(condition);
          js.Expression jsUpdates = generateExpression(info.updates);
          // The body might be labeled. Ignore this when recursing on the
          // subgraph.
          // TODO(lrn): Remove this extra labeling when handling all loops
          // using subgraphs.
          js.Block oldContainer = currentContainer;
          js.Statement body = new js.Block.empty();
          currentContainer = body;
          visitBodyIgnoreLabels(info);
          currentContainer = oldContainer;
          body = unwrapStatement(body);
          loop = new js.For(jsInitialization, jsCondition, jsUpdates, body);
        } else {
          // We have either no update graph, or it's too complex to
          // put in an expression.
          if (initialization != null) {
            generateStatements(initialization);
          }
          js.Expression jsCondition;
          js.Block oldContainer = currentContainer;
          js.Statement body = new js.Block.empty();
          if (isConditionExpression) {
            jsCondition = generateExpression(condition);
            currentContainer = body;
          } else {
            jsCondition = newLiteralBool(true);
            currentContainer = body;
            generateStatements(condition);
            use(condition.conditionExpression);
            js.Expression ifTest = new js.Prefix("!", pop());
            js.Break jsBreak = new js.Break(null);
            pushStatement(new js.If.noElse(ifTest, jsBreak));
          }
          if (info.updates != null) {
            wrapLoopBodyForContinue(info);
            generateStatements(info.updates);
          } else {
            visitBodyIgnoreLabels(info);
          }
          currentContainer = oldContainer;
          body = unwrapStatement(body);
          loop = new js.While(jsCondition, body);
        }
        break;
      case HLoopBlockInformation.DO_WHILE_LOOP:
        if (info.initializer != null) {
          generateStatements(info.initializer);
        }
        js.Block oldContainer = currentContainer;
        js.Block body = new js.Block.empty();
        // If there are phi copies in the block that jumps to the
        // loop entry, we must emit the condition like this:
        // do {
        //   body;
        //   if (condition) {
        //     phi updates;
        //     continue;
        //   } else {
        //     break;
        //   }
        // } while (true);
        HBasicBlock avoidEdge = info.end.successors[0];
        js.Block updateBody = new js.Block.empty();
        currentContainer = updateBody;
        assignPhisOfSuccessors(avoidEdge);
        bool hasPhiUpdates = !updateBody.statements.isEmpty;
        currentContainer = body;
        visitBodyIgnoreLabels(info);
        if (info.updates != null) {
          generateStatements(info.updates);
        }
        if (isConditionExpression) {
          push(generateExpression(condition));
        } else {
          generateStatements(condition);
          use(condition.conditionExpression);
        }
        js.Expression jsCondition = pop();
        if (jsCondition == null) {
          // If the condition is dead code, we turn the do-while into
          // a simpler while because we will never reach the condition
          // at the end of the loop anyway.
          loop = new js.While(newLiteralBool(true), unwrapStatement(body));
        } else {
          if (hasPhiUpdates) {
            updateBody.statements.add(new js.Continue(null));
            body.statements.add(
                new js.If(jsCondition, updateBody, new js.Break(null)));
            jsCondition = newLiteralBool(true);
          }
          loop = new js.Do(unwrapStatement(body), jsCondition);
        }
        currentContainer = oldContainer;
        break;
      default:
        compiler.internalError(
          'Unexpected loop kind: ${info.kind}',
          instruction: condition.conditionExpression);
    }
    attachLocationRange(loop, info.sourcePosition, info.endSourcePosition);
    js.Statement result = loop;
    if (info.kind == HLoopBlockInformation.SWITCH_CONTINUE_LOOP) {
      String continueLabelString =
          backend.namer.implicitContinueLabelName(info.target);
      result = new js.LabeledStatement(continueLabelString, result);
    }
    pushStatement(wrapIntoLabels(result, info.labels));
    return true;
  }

  bool visitLabeledBlockInfo(HLabeledBlockInformation labeledBlockInfo) {
    Link<Element> continueOverrides = const Link<Element>();

    js.Block oldContainer = currentContainer;
    js.Block body = new js.Block.empty();
    js.Statement result = body;

    currentContainer = body;

    // If [labeledBlockInfo.isContinue], the block is an artificial
    // block around the body of a loop with an update block, so that
    // continues of the loop can be written as breaks of the body
    // block.
    if (labeledBlockInfo.isContinue) {
      for (LabelElement label in labeledBlockInfo.labels) {
        if (label.isContinueTarget) {
          String labelName = backend.namer.continueLabelName(label);
          result = new js.LabeledStatement(labelName, result);
          continueAction[label] = continueAsBreak;
          continueOverrides = continueOverrides.prepend(label);
        }
      }
      // For handling unlabeled continues from the body of a loop.
      // TODO(lrn): Consider recording whether the target is in fact
      // a target of an unlabeled continue, and not generate this if it isn't.
      TargetElement target = labeledBlockInfo.target;
      String labelName = backend.namer.implicitContinueLabelName(target);
      result = new js.LabeledStatement(labelName, result);
      continueAction[target] = implicitContinueAsBreak;
      continueOverrides = continueOverrides.prepend(target);
    } else {
      for (LabelElement label in labeledBlockInfo.labels) {
        if (label.isBreakTarget) {
          String labelName = backend.namer.breakLabelName(label);
          result = new js.LabeledStatement(labelName, result);
        }
      }
    }
    TargetElement target = labeledBlockInfo.target;
    if (target.isSwitch) {
      // This is an extra block around a switch that is generated
      // as a nested if/else chain. We add an extra break target
      // so that case code can break.
      String labelName = backend.namer.implicitBreakLabelName(target);
      result = new js.LabeledStatement(labelName, result);
      breakAction[target] = implicitBreakWithLabel;
    }

    currentContainer = body;
    generateStatements(labeledBlockInfo.body);

    if (labeledBlockInfo.isContinue) {
      while (!continueOverrides.isEmpty) {
        continueAction.remove(continueOverrides.head);
        continueOverrides = continueOverrides.tail;
      }
    } else {
      breakAction.remove(labeledBlockInfo.target);
    }

    currentContainer = oldContainer;
    pushStatement(result);
    return true;
  }

  // Wraps a loop body in a block to make continues have a target to break
  // to (if necessary).
  void wrapLoopBodyForContinue(HLoopBlockInformation info) {
    TargetElement target = info.target;
    if (target != null && target.isContinueTarget) {
      js.Block oldContainer = currentContainer;
      js.Block body = new js.Block.empty();
      currentContainer = body;
      js.Statement result = body;
      for (LabelElement label in info.labels) {
        if (label.isContinueTarget) {
          String labelName = backend.namer.continueLabelName(label);
          result = new js.LabeledStatement(labelName, result);
          continueAction[label] = continueAsBreak;
        }
      }
      String labelName = backend.namer.implicitContinueLabelName(target);
      result = new js.LabeledStatement(labelName, result);
      continueAction[info.target] = implicitContinueAsBreak;
      visitBodyIgnoreLabels(info);
      continueAction.remove(info.target);
      for (LabelElement label in info.labels) {
        if (label.isContinueTarget) {
          continueAction.remove(label);
        }
      }
      currentContainer = oldContainer;
      pushStatement(result);
    } else {
      // Loop body contains no continues, so we don't need a break target.
      generateStatements(info.body);
    }
  }

  bool handleBlockFlow(HBlockFlow block) {
    HBlockInformation info = block.body;
    // If we reach here again while handling the attached information,
    // e.g., because we call visitSubGraph on a subgraph starting on
    // the same block, don't handle it again.
    // When the structure graph is complete, we will be able to have
    // different structures starting on the same basic block (e.g., an
    // "if" and its condition).
    if (identical(info, currentBlockInformation)) return false;

    HBlockInformation oldBlockInformation = currentBlockInformation;
    currentBlockInformation = info;
    bool success = info.accept(this);
    currentBlockInformation = oldBlockInformation;
    if (success) {
      HBasicBlock continuation = block.continuation;
      if (continuation != null) {
        visitBasicBlock(continuation);
      }
    }
    return success;
  }

  void visitBasicBlock(HBasicBlock node) {
    if (!node.isLive) return;

    // Abort traversal if we are leaving the currently active sub-graph.
    if (!subGraph.contains(node)) return;

    // If this node has block-structure based information attached,
    // try using that to traverse from here.
    if (node.blockFlow != null && handleBlockFlow(node.blockFlow)) {
      return;
    }
    iterateBasicBlock(node);
  }

  void emitAssignment(String destination, String source) {
    assignVariable(destination, new js.VariableUse(source));
  }

  /**
   * Sequentialize a list of conceptually parallel copies. Parallel
   * copies may contain cycles, that this method breaks.
   */
  void sequentializeCopies(Iterable<Copy> copies,
                           String tempName,
                           void doAssignment(String target, String source)) {
    // Map to keep track of the current location (ie the variable that
    // holds the initial value) of a variable.
    Map<String, String> currentLocation = new Map<String, String>();

    // Map to keep track of the initial value of a variable.
    Map<String, String> initialValue = new Map<String, String>();

    // List of variables to assign a value.
    List<String> worklist = <String>[];

    // List of variables that we can assign a value to (ie are not
    // being used anymore).
    List<String> ready = <String>[];

    // Prune [copies] by removing self-copies.
    List<Copy> prunedCopies = <Copy>[];
    for (Copy copy in copies) {
      if (copy.source != copy.destination) {
        prunedCopies.add(copy);
      }
    }
    copies = prunedCopies;


    // For each copy, set the current location of the source to
    // itself, and the initial value of the destination to the source.
    // Add the destination to the list of copies to make.
    for (Copy copy in copies) {
      currentLocation[copy.source] = copy.source;
      initialValue[copy.destination] = copy.source;
      worklist.add(copy.destination);
    }

    // For each copy, if the destination does not have a current
    // location, then we can safely assign to it.
    for (Copy copy in copies) {
      if (currentLocation[copy.destination] == null) {
        ready.add(copy.destination);
      }
    }

    while (!worklist.isEmpty) {
      while (!ready.isEmpty) {
        String destination = ready.removeLast();
        String source = initialValue[destination];
        // Since [source] might have been updated, use the current
        // location of [source]
        String copy = currentLocation[source];
        doAssignment(destination, copy);
        // Now [destination] is the current location of [source].
        currentLocation[source] = destination;
        // If [source] hasn't been updated and needs to have a value,
        // add it to the list of variables that can be updated. Copies
        // of [source] will now use [destination].
        if (source == copy && initialValue[source] != null) {
          ready.add(source);
        }
      }

      // Check if we have a cycle.
      String current = worklist.removeLast();
      // If [current] is used as a source, and the assignment has been
      // done, we are done with this variable. Otherwise there is a
      // cycle that we break by using a temporary name.
      if (currentLocation[current] != null
          && current != currentLocation[initialValue[current]]) {
        doAssignment(tempName, current);
        currentLocation[current] = tempName;
        // [current] can now be safely updated. Copies of [current]
        // will now use [tempName].
        ready.add(current);
      }
    }
  }

  void assignPhisOfSuccessors(HBasicBlock node) {
    CopyHandler handler = variableNames.getCopyHandler(node);
    if (handler == null) return;

    // Map the instructions to strings.
    Iterable<Copy> copies = handler.copies.map((Copy copy) {
      return new Copy(variableNames.getName(copy.source),
                      variableNames.getName(copy.destination));
    });

    sequentializeCopies(copies, variableNames.getSwapTemp(), emitAssignment);

    for (Copy copy in handler.assignments) {
      String name = variableNames.getName(copy.destination);
      use(copy.source);
      assignVariable(name, pop());
    }
  }

  void iterateBasicBlock(HBasicBlock node) {
    HInstruction instruction = node.first;
    while (!identical(instruction, node.last)) {
      if (!isGenerateAtUseSite(instruction)) {
        define(instruction);
      }
      instruction = instruction.next;
    }
    assignPhisOfSuccessors(node);
    visit(instruction);
  }

  visitInvokeBinary(HInvokeBinary node, String op) {
    use(node.left);
    js.Expression jsLeft = pop();
    use(node.right);
    push(new js.Binary(op, jsLeft, pop()), node);
  }

  visitRelational(HRelational node, String op) => visitInvokeBinary(node, op);

  // We want the outcome of bit-operations to be positive. We use the unsigned
  // shift operator to achieve this.
  visitBitInvokeBinary(HBinaryBitOp node, String op) {
    visitInvokeBinary(node, op);
    if (requiresUintConversion(node)) {
      push(new js.Binary(">>>", pop(), new js.LiteralNumber("0")), node);
    }
  }

  visitInvokeUnary(HInvokeUnary node, String op) {
    use(node.operand);
    push(new js.Prefix(op, pop()), node);
  }

  // We want the outcome of bit-operations to be positive. We use the unsigned
  // shift operator to achieve this.
  visitBitInvokeUnary(HInvokeUnary node, String op) {
    visitInvokeUnary(node, op);
    if (requiresUintConversion(node)) {
      push(new js.Binary(">>>", pop(), new js.LiteralNumber("0")), node);
    }
  }

  void emitIdentityComparison(HInstruction left,
                              HInstruction right,
                              bool inverse) {
    String op = singleIdentityComparison(left, right, compiler);
    if (op != null) {
      use(left);
      js.Expression jsLeft = pop();
      use(right);
      push(new js.Binary(mapRelationalOperator(op, inverse), jsLeft, pop()));
    } else {
      assert(NullConstant.JsNull == 'null');
      use(left);
      js.Binary leftEqualsNull =
          new js.Binary("==", pop(), new js.LiteralNull());
      use(right);
      js.Binary rightEqualsNull =
          new js.Binary(mapRelationalOperator("==", inverse),
                        pop(), new js.LiteralNull());
      use(right);
      use(left);
      js.Binary tripleEq = new js.Binary(mapRelationalOperator("===", inverse),
                                         pop(), pop());

      push(new js.Conditional(leftEqualsNull, rightEqualsNull, tripleEq));
    }
  }

  visitIdentity(HIdentity node) {
    emitIdentityComparison(node.left, node.right, false);
  }

  visitAdd(HAdd node)               => visitInvokeBinary(node, '+');
  visitDivide(HDivide node)         => visitInvokeBinary(node, '/');
  visitMultiply(HMultiply node)     => visitInvokeBinary(node, '*');
  visitSubtract(HSubtract node)     => visitInvokeBinary(node, '-');
  visitBitAnd(HBitAnd node)         => visitBitInvokeBinary(node, '&');
  visitBitNot(HBitNot node)         => visitBitInvokeUnary(node, '~');
  visitBitOr(HBitOr node)           => visitBitInvokeBinary(node, '|');
  visitBitXor(HBitXor node)         => visitBitInvokeBinary(node, '^');
  visitShiftLeft(HShiftLeft node)   => visitBitInvokeBinary(node, '<<');

  visitNegate(HNegate node)         => visitInvokeUnary(node, '-');

  visitLess(HLess node)                 => visitRelational(node, '<');
  visitLessEqual(HLessEqual node)       => visitRelational(node, '<=');
  visitGreater(HGreater node)           => visitRelational(node, '>');
  visitGreaterEqual(HGreaterEqual node) => visitRelational(node, '>=');

  visitBoolify(HBoolify node) {
    assert(node.inputs.length == 1);
    use(node.inputs[0]);
    push(new js.Binary('===', pop(), newLiteralBool(true)), node);
  }

  visitExit(HExit node) {
    // Don't do anything.
  }

  visitGoto(HGoto node) {
    HBasicBlock block = node.block;
    assert(block.successors.length == 1);
    List<HBasicBlock> dominated = block.dominatedBlocks;
    // With the exception of the entry-node which dominates its successor
    // and the exit node, no block finishing with a 'goto' can have more than
    // one dominated block (since it has only one successor).
    // If the successor is dominated by another block, then the other block
    // is responsible for visiting the successor.
    if (dominated.isEmpty) return;
    if (dominated.length > 2) {
      compiler.internalError('dominated.length = ${dominated.length}',
                             instruction: node);
    }
    if (dominated.length == 2 && block != currentGraph.entry) {
      compiler.internalError('node.block != currentGraph.entry',
                             instruction: node);
    }
    assert(dominated[0] == block.successors[0]);
    visitBasicBlock(dominated[0]);
  }

  visitLoopBranch(HLoopBranch node) {
    assert(node.block == subGraph.end);
    // We are generating code for a loop condition.
    // If we are generating the subgraph as an expression, the
    // condition will be generated as the expression.
    // Otherwise, we don't generate the expression, and leave that
    // to the code that called [visitSubGraph].
    if (isGeneratingExpression) {
      use(node.inputs[0]);
    }
  }

  /**
   * Checks if [map] contains an [ElementAction] for [element], and
   * if so calls that action and returns true.
   * Otherwise returns false.
   */
  bool tryCallAction(Map<Element, ElementAction> map, Element element) {
    ElementAction action = map[element];
    if (action == null) return false;
    action(element);
    return true;
  }

  visitBreak(HBreak node) {
    assert(node.block.successors.length == 1);
    if (node.label != null) {
      LabelElement label = node.label;
      if (!tryCallAction(breakAction, label)) {
        pushStatement(new js.Break(backend.namer.breakLabelName(label)), node);
      }
    } else {
      TargetElement target = node.target;
      if (!tryCallAction(breakAction, target)) {
        if (node.breakSwitchContinueLoop) {
          pushStatement(new js.Break(
              backend.namer.implicitContinueLabelName(target)), node);
        } else {
          pushStatement(new js.Break(null), node);
        }
      }
    }
  }

  visitContinue(HContinue node) {
    assert(node.block.successors.length == 1);
    if (node.label != null) {
      LabelElement label = node.label;
      if (!tryCallAction(continueAction, label)) {
        // TODO(floitsch): should this really be the breakLabelName?
        pushStatement(new js.Continue(backend.namer.breakLabelName(label)),
                      node);
      }
    } else {
      TargetElement target = node.target;
      if (!tryCallAction(continueAction, target)) {
        if (target.statement is SwitchStatement) {
          pushStatement(new js.Continue(
              backend.namer.implicitContinueLabelName(target)), node);
        } else {
          pushStatement(new js.Continue(null), node);
        }
      }
    }
  }

  visitExitTry(HExitTry node) {
    // An [HExitTry] is used to represent the control flow graph of a
    // try/catch block, ie the try body is always a predecessor
    // of the catch and finally. Here, we continue visiting the try
    // body by visiting the block that contains the user-level control
    // flow instruction.
    visitBasicBlock(node.bodyTrySuccessor);
  }

  visitTry(HTry node) {
    // We should never get here. Try/catch/finally is always handled using block
    // information in [visitTryInfo].
    compiler.internalError('visitTry should not be called', instruction: node);
  }

  bool tryControlFlowOperation(HIf node) {
    if (!controlFlowOperators.contains(node)) return false;
    HPhi phi = node.joinBlock.phis.first;
    bool atUseSite = isGenerateAtUseSite(phi);
    // Don't generate a conditional operator in this situation:
    // i = condition ? bar() : i;
    // But generate this instead:
    // if (condition) i = bar();
    // Usually, the variable name is longer than 'if' and it takes up
    // more space to duplicate the name.
    if (!atUseSite
        && variableNames.getName(phi) == variableNames.getName(phi.inputs[1])) {
      return false;
    }
    if (!atUseSite) define(phi);
    visitBasicBlock(node.joinBlock);
    return true;
  }

  void generateIf(HIf node, HIfBlockInformation info) {
    use(node.inputs[0]);
    js.Expression test = pop();

    HStatementInformation thenGraph = info.thenGraph;
    HStatementInformation elseGraph = info.elseGraph;
    js.Statement thenPart =
        unwrapStatement(generateStatementsInNewBlock(thenGraph));
    js.Statement elsePart =
        unwrapStatement(generateStatementsInNewBlock(elseGraph));

    pushStatement(new js.If(test, thenPart, elsePart), node);
  }

  visitIf(HIf node) {
    if (tryControlFlowOperation(node)) return;

    HInstruction condition = node.inputs[0];
    HIfBlockInformation info = node.blockInformation.body;

    if (condition.isConstant()) {
      HConstant constant = condition;
      if (constant.constant.isTrue()) {
        generateStatements(info.thenGraph);
      } else {
        generateStatements(info.elseGraph);
      }
    } else {
      generateIf(node, info);
    }

    HBasicBlock joinBlock = node.joinBlock;
    if (joinBlock != null && !identical(joinBlock.dominator, node.block)) {
      // The join block is dominated by a block in one of the branches.
      // The subgraph traversal never reached it, so we visit it here
      // instead.
      visitBasicBlock(joinBlock);
    }

    // Visit all the dominated blocks that are not part of the then or else
    // branches, and is not the join block.
    // Depending on how the then/else branches terminate
    // (e.g., return/throw/break) there can be any number of these.
    List<HBasicBlock> dominated = node.block.dominatedBlocks;
    for (int i = 2; i < dominated.length; i++) {
      visitBasicBlock(dominated[i]);
    }
  }

  js.Call jsPropertyCall(js.Expression receiver,
                         String fieldName,
                         List<js.Expression> arguments) {
    return new js.Call(new js.PropertyAccess.field(receiver, fieldName),
                       arguments);
  }

  void visitInterceptor(HInterceptor node) {
    backend.registerSpecializedGetInterceptor(node.interceptedClasses);
    String name = backend.namer.getInterceptorName(
        backend.getInterceptorMethod, node.interceptedClasses);
    var isolate = new js.VariableUse(
        backend.namer.globalObjectFor(compiler.interceptorsLibrary));
    use(node.receiver);
    List<js.Expression> arguments = <js.Expression>[pop()];
    push(jsPropertyCall(isolate, name, arguments), node);
    backend.registerUseInterceptor(world);
  }

  visitInvokeDynamicMethod(HInvokeDynamicMethod node) {
    use(node.receiver);
    js.Expression object = pop();
    String name = node.selector.name;
    String methodName;
    List<js.Expression> arguments = visitArguments(node.inputs);
    Element target = node.element;

    if (target != null && !node.isInterceptedCall) {
      if (target == backend.jsArrayAdd) {
        methodName = 'push';
      } else if (target == backend.jsArrayRemoveLast) {
        methodName = 'pop';
      } else if (target == backend.jsStringSplit) {
        methodName = 'split';
        // Split returns a List, so we make sure the backend knows the
        // list class is instantiated.
        world.registerInstantiatedClass(
            compiler.listClass, work.resolutionTree);
      } else if (target == backend.jsStringOperatorAdd) {
        push(new js.Binary('+', object, arguments[0]), node);
        return;
      } else if (target.isNative() && target.isFunction()
                 && !node.isInterceptedCall) {
        // A direct (i.e. non-interceptor) native call is the result of
        // optimization.  The optimization ensures any type checks or
        // conversions have been satisified.
        methodName = target.fixedBackendName();
      }
    }

    if (methodName == null) {
      methodName = backend.namer.invocationName(node.selector);
      registerMethodInvoke(node);
    }
    push(jsPropertyCall(object, methodName, arguments), node);
  }

  void visitInvokeConstructorBody(HInvokeConstructorBody node) {
    use(node.inputs[0]);
    js.Expression object = pop();
    String methodName = backend.namer.getNameOfInstanceMember(node.element);
    List<js.Expression> arguments = visitArguments(node.inputs);
    push(jsPropertyCall(object, methodName, arguments), node);
    world.registerStaticUse(node.element);
  }

  void visitOneShotInterceptor(HOneShotInterceptor node) {
    List<js.Expression> arguments = visitArguments(node.inputs);
    var isolate = new js.VariableUse(
        backend.namer.globalObjectFor(compiler.interceptorsLibrary));
    Selector selector = getOptimizedSelectorFor(node, node.selector);
    String methodName = backend.registerOneShotInterceptor(selector);
    push(jsPropertyCall(isolate, methodName, arguments), node);
    if (selector.isGetter()) {
      registerGetter(node);
    } else if (selector.isSetter()) {
      registerSetter(node);
    } else {
      registerMethodInvoke(node);
    }
    backend.registerUseInterceptor(world);
  }

  Selector getOptimizedSelectorFor(HInvokeDynamic node, Selector selector) {
    if (node.element != null) {
      // Create an artificial type mask to make sure only
      // [node.element] will be enqueued. We're not using the receiver
      // type because our optimizations might end up in a state where the
      // invoke dynamic knows more than the receiver.
      ClassElement enclosing = node.element.getEnclosingClass();
      TypeMask receiverType = new TypeMask.nonNullExact(enclosing.declaration);
      return new TypedSelector(receiverType, selector);
    }
    // If [JSInvocationMirror._invokeOn] is enabled, and this call
    // might hit a `noSuchMethod`, we register an untyped selector.
    return selector.extendIfReachesAll(compiler);
  }

  void registerMethodInvoke(HInvokeDynamic node) {
    Selector selector = getOptimizedSelectorFor(node, node.selector);

    // If we don't know what we're calling or if we are calling a getter,
    // we need to register that fact that we may be calling a closure
    // with the same arguments.
    Element target = node.element;
    if (target == null || target.isGetter()) {
      // TODO(kasperl): If we have a typed selector for the call, we
      // may know something about the types of closures that need
      // the specific closure call method.
      Selector call = new Selector.callClosureFrom(selector);
      world.registerDynamicInvocation(call);
    }
    world.registerDynamicInvocation(selector);
  }

  void registerSetter(HInvokeDynamic node) {
    Selector selector = getOptimizedSelectorFor(node, node.selector);
    world.registerDynamicSetter(selector);
  }

  void registerGetter(HInvokeDynamic node) {
    Selector selector = getOptimizedSelectorFor(node, node.selector);
    world.registerDynamicGetter(selector);
  }

  visitInvokeDynamicSetter(HInvokeDynamicSetter node) {
    use(node.receiver);
    String name = backend.namer.invocationName(node.selector);
    push(jsPropertyCall(pop(), name, visitArguments(node.inputs)), node);
    registerSetter(node);
  }

  visitInvokeDynamicGetter(HInvokeDynamicGetter node) {
    use(node.receiver);
    String name = backend.namer.invocationName(node.selector);
    push(jsPropertyCall(pop(), name, visitArguments(node.inputs)), node);
    registerGetter(node);
  }

  visitInvokeClosure(HInvokeClosure node) {
    Selector call = new Selector.callClosureFrom(node.selector);
    use(node.receiver);
    push(jsPropertyCall(pop(),
                        backend.namer.invocationName(call),
                        visitArguments(node.inputs)),
         node);
    world.registerDynamicInvocation(call);
  }

  visitInvokeStatic(HInvokeStatic node) {
    Element element = node.element;
    ClassElement cls = element.getEnclosingClass();
    List<DartType> instantiatedTypes = node.instantiatedTypes;

    world.registerStaticUse(element);

    if (instantiatedTypes != null && !instantiatedTypes.isEmpty) {
      instantiatedTypes.forEach((type) {
        world.registerInstantiatedType(type, work.resolutionTree);
      });
    }

    push(new js.VariableUse(backend.namer.isolateAccess(node.element)));
    push(new js.Call(pop(), visitArguments(node.inputs, start: 0)), node);
  }

  visitInvokeSuper(HInvokeSuper node) {
    Element superMethod = node.element;
    world.registerStaticUse(superMethod);
    ClassElement superClass = superMethod.getEnclosingClass();
    if (superMethod.kind == ElementKind.FIELD) {
      String fieldName = superMethod.hasFixedBackendName()
          ? superMethod.fixedBackendName()
          : node.caller.isShadowedByField(superMethod)
              ? backend.namer.shadowedFieldName(superMethod)
              : backend.namer.instanceFieldName(superMethod);
      use(node.inputs[0]);
      js.PropertyAccess access =
          new js.PropertyAccess.field(pop(), fieldName);
      if (node.isSetter) {
        use(node.value);
        push(new js.Assignment(access, pop()), node);
      } else {
        push(access, node);
      }
    } else {
      Selector selector = node.selector;
      String methodName;
      if (selector.isGetter()) {
        // If the selector we need to register a typed getter to the
        // [world]. The emitter needs to know if it needs to emit a
        // bound closure for a method.
        TypeMask receiverType = new TypeMask.nonNullExact(superClass);
        selector = new TypedSelector(receiverType, selector);
        world.registerDynamicGetter(selector);
        methodName = backend.namer.invocationName(selector);
      } else {
        methodName = backend.namer.getNameOfInstanceMember(superMethod);
      }
      js.PropertyAccess method =
          backend.namer.elementAccess(superClass)['prototype'][methodName];
      push(jsPropertyCall(
          method, "call", visitArguments(node.inputs, start: 0)), node);
    }
  }

  visitFieldGet(HFieldGet node) {
    use(node.receiver);
    Element element = node.element;
    if (node.isNullCheck) {
      // We access a JavaScript member we know all objects besides
      // null and undefined have: V8 does not like accessing a member
      // that does not exist.
      push(new js.PropertyAccess.field(pop(), 'toString'), node);
    } else if (element == backend.jsIndexableLength) {
      // We're accessing a native JavaScript property called 'length'
      // on a JS String or a JS array. Therefore, the name of that
      // property should not be mangled.
      if (backend.isTypedArray(node.receiver.instructionType)) {
        // TODO(12929): Remove this custom code for typed arrays once V8
        // optimizes their length access.
        // Do a call to `fetchLength` instead of accessing `length`
        // directly. Because `fetchLength` is a constant we use its
        // constant value instead.
        if (node.usedBy.isEmpty) {
          // The length access has not been removed because it is
          // acting as a null check. For a faster null check, we use
          // toString.
          push(new js.PropertyAccess.field(pop(), 'toString'), node);
        } else {
          Element element = compiler.findRequiredElement(
              compiler.typedDataLibrary, 'fetchLength');
          Constant constant =
              compiler.constantHandler.getConstantForVariable(element);
          assert(invariant(element, constant != null,
              message: 'No constant computed for $element'));
          var jsConstant = backend.emitter.constantReference(constant);
          push(new js.Call(jsConstant, [pop()]), node);
        }
      } else {
        push(new js.PropertyAccess.field(pop(), 'length'), node);
      }
    } else {
      String name = _fieldPropertyName(element);
      push(new js.PropertyAccess.field(pop(), name), node);
      world.registerFieldGetter(element);
    }
  }

  visitFieldSet(HFieldSet node) {
    Element element = node.element;
    world.registerFieldSetter(element);
    String name = _fieldPropertyName(element);
    use(node.receiver);
    js.Expression receiver = pop();
    use(node.value);
    push(new js.Assignment(new js.PropertyAccess.field(receiver, name), pop()),
        node);
  }

  String _fieldPropertyName(Element element) => element.hasFixedBackendName()
      ? element.fixedBackendName()
      : backend.namer.getNameOfInstanceMember(element);

  visitLocalGet(HLocalGet node) {
    use(node.receiver);
  }

  visitLocalSet(HLocalSet node) {
    use(node.value);
    assignVariable(variableNames.getName(node.receiver), pop());
  }

  void registerForeignTypes(HForeign node) {
    native.NativeBehavior nativeBehavior = node.nativeBehavior;
    if (nativeBehavior == null) return;
    nativeBehavior.typesReturned.forEach((type) {
      if (type is DartType) {
        world.registerInstantiatedType(type, work.resolutionTree);
      }
    });
  }

  visitForeign(HForeign node) {
    List<HInstruction> inputs = node.inputs;
    if (node.isJsStatement()) {
      if (!inputs.isEmpty) {
        compiler.internalError("foreign statement with inputs",
                               instruction: node);
      }
      pushStatement(node.codeAst, node);
    } else {
      if (!inputs.isEmpty) {
        List<js.Expression> interpolatedExpressions = <js.Expression>[];
        for (int i = 0; i < inputs.length; i++) {
          use(inputs[i]);
          interpolatedExpressions.add(pop());
        }
        var visitor = new js.UninterpolateJSExpression(interpolatedExpressions);
        push(visitor.visit(node.codeAst), node);
      } else {
        push(node.codeAst, node);
      }
    }

    // TODO(sra): Tell world.nativeEnqueuer about the types created here.
    registerForeignTypes(node);
  }

  visitForeignNew(HForeignNew node) {
    String jsClassReference = backend.namer.isolateAccess(node.element);
    List<js.Expression> arguments = visitArguments(node.inputs, start: 0);
    // TODO(floitsch): jsClassReference is an Access. We shouldn't treat it
    // as if it was a string.
    push(new js.New(new js.VariableUse(jsClassReference), arguments), node);
    registerForeignTypes(node);
    if (node.instantiatedTypes == null) {
      return;
    }
    node.instantiatedTypes.forEach((type) {
      world.registerInstantiatedType(type, work.resolutionTree);
    });
  }

  js.Expression newLiteralBool(bool value) {
    if (compiler.enableMinification) {
      // Use !0 for true, !1 for false.
      return new js.Prefix("!", new js.LiteralNumber(value ? "0" : "1"));
    } else {
      return new js.LiteralBool(value);
    }
  }

  void generateConstant(Constant constant) {
    if (constant.isFunction()) {
      FunctionConstant function = constant;
      world.registerStaticUse(function.element);
    }
    if (constant.isType()) {
      // If the type is a web component, we need to ensure the constructors are
      // available to 'upgrade' the native object.
      TypeConstant type = constant;
      Element element = type.representedType.element;
      if (element != null && element.isClass()) {
        backend.customElementsAnalysis.registerTypeConstant(element, world);
      }
    }
    push(backend.emitter.constantReference(constant));
  }

  visitConstant(HConstant node) {
    assert(isGenerateAtUseSite(node));
    generateConstant(node.constant);

    backend.registerCompileTimeConstant(node.constant, work.resolutionTree);
    compiler.constantHandler.addCompileTimeConstantForEmission(node.constant);
  }

  visitNot(HNot node) {
    assert(node.inputs.length == 1);
    generateNot(node.inputs[0]);
    attachLocationToLast(node);
  }

  static String mapRelationalOperator(String op, bool inverse) {
    Map<String, String> inverseOperator = const <String, String>{
      "==" : "!=",
      "!=" : "==",
      "===": "!==",
      "!==": "===",
      "<"  : ">=",
      "<=" : ">",
      ">"  : "<=",
      ">=" : "<"
    };
    return inverse ? inverseOperator[op] : op;
  }

  void generateNot(HInstruction input) {
    bool canGenerateOptimizedComparison(HInstruction instruction) {
      if (instruction is !HRelational) return false;

      HRelational relational = instruction;
      BinaryOperation operation = relational.operation(backend.constantSystem);

      HInstruction left = relational.left;
      HInstruction right = relational.right;
      if (left.isString(compiler) && right.isString(compiler)) {
        return true;
      }

      // This optimization doesn't work for NaN, so we only do it if the
      // type is known to be an integer.
      return left.isInteger(compiler) && right.isInteger(compiler);
    }

    bool handledBySpecialCase = false;
    if (isGenerateAtUseSite(input)) {
      handledBySpecialCase = true;
      if (input is HIs) {
        emitIs(input, '!==');
      } else if (input is HIdentity) {
        HIdentity identity = input;
        emitIdentityComparison(identity.left, identity.right, true);
      } else if (input is HBoolify) {
        use(input.inputs[0]);
        push(new js.Binary("!==", pop(), newLiteralBool(true)), input);
      } else if (canGenerateOptimizedComparison(input)) {
        HRelational relational = input;
        BinaryOperation operation =
            relational.operation(backend.constantSystem);
        String op = mapRelationalOperator(operation.name, true);
        visitRelational(input, op);
      } else {
        handledBySpecialCase = false;
      }
    }
    if (!handledBySpecialCase) {
      use(input);
      push(new js.Prefix("!", pop()));
    }
  }

  visitParameterValue(HParameterValue node) {
    assert(!isGenerateAtUseSite(node));
    String name = variableNames.getName(node);
    parameters.add(new js.Parameter(name));
    declaredLocals.add(name);
  }

  visitLocalValue(HLocalValue node) {
    assert(!isGenerateAtUseSite(node));
    String name = variableNames.getName(node);
    collectedVariableDeclarations.add(name);
  }

  visitPhi(HPhi node) {
    // This method is only called for phis that are generated at use
    // site. A phi can be generated at use site only if it is the
    // result of a control flow operation.
    HBasicBlock ifBlock = node.block.dominator;
    assert(controlFlowOperators.contains(ifBlock.last));
    HInstruction input = ifBlock.last.inputs[0];
    if (input.isConstantFalse()) {
      use(node.inputs[1]);
    } else if (input.isConstantTrue()) {
      use(node.inputs[0]);
    } else if (node.inputs[1].isConstantBoolean()) {
      String operation = node.inputs[1].isConstantFalse() ? '&&' : '||';
      if (operation == '||') {
        if (input is HNot) {
          use(input.inputs[0]);
        } else {
          generateNot(input);
        }
      } else {
        use(input);
      }
      js.Expression left = pop();
      use(node.inputs[0]);
      push(new js.Binary(operation, left, pop()));
    } else {
      use(input);
      js.Expression test = pop();
      use(node.inputs[0]);
      js.Expression then = pop();
      use(node.inputs[1]);
      push(new js.Conditional(test, then, pop()));
    }
  }

  visitReturn(HReturn node) {
    assert(node.inputs.length == 1);
    HInstruction input = node.inputs[0];
    if (input.isConstantNull()) {
      pushStatement(new js.Return(null), node);
    } else {
      use(node.inputs[0]);
      pushStatement(new js.Return(pop()), node);
    }
  }

  visitThis(HThis node) {
    push(new js.This());
  }

  visitThrow(HThrow node) {
    if (node.isRethrow) {
      use(node.inputs[0]);
      pushStatement(new js.Throw(pop()), node);
    } else {
      generateThrowWithHelper('wrapException', node.inputs[0]);
    }
  }

  visitRangeConversion(HRangeConversion node) {
    // Range conversion instructions are removed by the value range
    // analyzer.
    assert(false);
  }

  visitBoundsCheck(HBoundsCheck node) {
    // TODO(ngeoffray): Separate the two checks of the bounds check, so,
    // e.g., the zero checks can be shared if possible.

    // If the checks always succeeds, we would have removed the bounds check
    // completely.
    assert(node.staticChecks != HBoundsCheck.ALWAYS_TRUE);
    if (node.staticChecks != HBoundsCheck.ALWAYS_FALSE) {
      js.Expression under;
      js.Expression over;
      if (node.staticChecks != HBoundsCheck.ALWAYS_ABOVE_ZERO) {
        use(node.index);
        if (node.index.isInteger(compiler)) {
          under = js.js("# < 0", pop());
        } else {
          js.Expression jsIndex = pop();
          under = js.js("# >>> 0 !== #", [jsIndex, jsIndex]);
        }
      } else if (!node.index.isInteger(compiler)) {
        checkInt(node.index, '!==');
        under = pop();
      }
      if (node.staticChecks != HBoundsCheck.ALWAYS_BELOW_LENGTH) {
        var index = node.index;
        use(index);
        js.Expression jsIndex = pop();
        use(node.length);
        over = new js.Binary(">=", jsIndex, pop());
      }
      assert(over != null || under != null);
      js.Expression underOver = under == null
          ? over
          : over == null
              ? under
              : new js.Binary("||", under, over);
      js.Statement thenBody = new js.Block.empty();
      js.Block oldContainer = currentContainer;
      currentContainer = thenBody;
      generateThrowWithHelper('ioore', [node.array, node.index]);
      currentContainer = oldContainer;
      thenBody = unwrapStatement(thenBody);
      pushStatement(new js.If.noElse(underOver, thenBody), node);
    } else {
      generateThrowWithHelper('ioore', [node.array, node.index]);
    }
  }

  void generateThrowWithHelper(String helperName, argument) {
    Element helper = compiler.findHelper(helperName);
    world.registerStaticUse(helper);
    js.VariableUse jsHelper =
        new js.VariableUse(backend.namer.isolateAccess(helper));
    List arguments = [];
    var location;
    if (argument is List) {
      location = argument[0];
      argument.forEach((instruction) {
        use(instruction);
        arguments.add(pop());
      });
    } else {
      location = argument;
      use(argument);
      arguments.add(pop());
    }
    js.Call value = new js.Call(jsHelper, arguments);
    attachLocation(value, location);
    // BUG(4906): Using throw here adds to the size of the generated code
    // but it has the advantage of explicitly telling the JS engine that
    // this code path will terminate abruptly. Needs more work.
    pushStatement(new js.Throw(value));
  }

  visitThrowExpression(HThrowExpression node) {
    HInstruction argument = node.inputs[0];
    use(argument);

    Element helper = compiler.findHelper("throwExpression");
    world.registerStaticUse(helper);

    js.VariableUse jsHelper =
        new js.VariableUse(backend.namer.isolateAccess(helper));
    js.Call value = new js.Call(jsHelper, [pop()]);
    attachLocation(value, argument);
    push(value, node);
  }

  void visitSwitch(HSwitch node) {
    // Switches are handled using [visitSwitchInfo].
  }

  void visitStatic(HStatic node) {
    Element element = node.element;
    if (element.isFunction()) {
      push(new js.VariableUse(
          backend.namer.isolateStaticClosureAccess(node.element)));
    } else {
      push(new js.VariableUse(backend.namer.isolateAccess(node.element)));
    }
    world.registerStaticUse(element);
  }

  void visitLazyStatic(HLazyStatic node) {
    Element element = node.element;
    world.registerStaticUse(element);
    String lazyGetter = backend.namer.isolateLazyInitializerAccess(element);
    js.VariableUse target = new js.VariableUse(lazyGetter);
    js.Call call = new js.Call(target, <js.Expression>[]);
    push(call, node);
  }

  void visitStaticStore(HStaticStore node) {
    world.registerStaticUse(node.element);
    js.Node variable = backend.namer.elementAccess(node.element);
    use(node.inputs[0]);
    push(new js.Assignment(variable, pop()), node);
  }

  void visitStringConcat(HStringConcat node) {
    use(node.left);
    js.Expression jsLeft = pop();
    use(node.right);
    push(new js.Binary('+', jsLeft, pop()), node);
  }

  void visitStringify(HStringify node) {
    HInstruction input = node.inputs.first;
    if (input.isString(compiler)) {
      use(input);
    } else if (input.isInteger(compiler) || input.isBoolean(compiler)) {
      // JavaScript's + operator with a string for the left operand will convert
      // the right operand to a string, and the conversion result is correct.
      use(input);
      if (node.usedBy.length == 1
          && node.usedBy[0] is HStringConcat
          && node.usedBy[0].inputs[1] == node) {
        // The context is already <string> + value.
      } else {
        // Force an empty string for the first operand.
        push(new js.Binary('+', js.string(""), pop()), node);
      }
    } else {
      Element convertToString = backend.getStringInterpolationHelper();
      world.registerStaticUse(convertToString);
      js.VariableUse variableUse =
          new js.VariableUse(backend.namer.isolateAccess(convertToString));
      use(input);
      push(new js.Call(variableUse, <js.Expression>[pop()]), node);
    }
  }

  void visitLiteralList(HLiteralList node) {
    world.registerInstantiatedClass(
        compiler.listClass, work.resolutionTree);
    generateArrayLiteral(node);
  }

  void generateArrayLiteral(HLiteralList node) {
    int len = node.inputs.length;
    List<js.ArrayElement> elements = <js.ArrayElement>[];
    for (int i = 0; i < len; i++) {
      use(node.inputs[i]);
      elements.add(new js.ArrayElement(i, pop()));
    }
    push(new js.ArrayInitializer(len, elements), node);
  }

  void visitIndex(HIndex node) {
    use(node.receiver);
    js.Expression receiver = pop();
    use(node.index);
    push(new js.PropertyAccess(receiver, pop()), node);
  }

  void visitIndexAssign(HIndexAssign node) {
    use(node.receiver);
    js.Expression receiver = pop();
    use(node.index);
    js.Expression index = pop();
    use(node.value);
    push(new js.Assignment(new js.PropertyAccess(receiver, index), pop()),
         node);
  }

  void checkInt(HInstruction input, String cmp) {
    use(input);
    js.Expression left = pop();
    use(input);
    js.Expression or0 = new js.Binary("|", pop(), new js.LiteralNumber("0"));
    push(new js.Binary(cmp, left, or0));
  }

  void checkBigInt(HInstruction input, String cmp) {
    use(input);
    js.Expression left = pop();
    use(input);
    js.Expression right = pop();
    // TODO(4984): Deal with infinity and -0.0.
    push(js.js('Math.floor(#) $cmp #', <js.Expression>[left, right]));
  }

  void checkTypeOf(HInstruction input, String cmp, String typeName) {
    use(input);
    js.Expression typeOf = new js.Prefix("typeof", pop());
    push(new js.Binary(cmp, typeOf, js.string(typeName)));
  }

  void checkNum(HInstruction input, String cmp)
      => checkTypeOf(input, cmp, 'number');

  void checkDouble(HInstruction input, String cmp)  => checkNum(input, cmp);

  void checkString(HInstruction input, String cmp)
      => checkTypeOf(input, cmp, 'string');

  void checkBool(HInstruction input, String cmp)
      => checkTypeOf(input, cmp, 'boolean');

  void checkObject(HInstruction input, String cmp) {
    assert(NullConstant.JsNull == 'null');
    if (cmp == "===") {
      checkTypeOf(input, '===', 'object');
      js.Expression left = pop();
      use(input);
      js.Expression notNull = new js.Binary("!==", pop(), new js.LiteralNull());
      push(new js.Binary("&&", left, notNull));
    } else {
      assert(cmp == "!==");
      checkTypeOf(input, '!==', 'object');
      js.Expression left = pop();
      use(input);
      js.Expression eqNull = new js.Binary("===", pop(), new js.LiteralNull());
      push(new js.Binary("||", left, eqNull));
    }
  }

  void checkArray(HInstruction input, String cmp) {
    use(input);
    js.PropertyAccess constructor =
        new js.PropertyAccess.field(pop(), 'constructor');
    push(new js.Binary(cmp, constructor, new js.VariableUse('Array')));
  }

  void checkFieldExists(HInstruction input, String fieldName) {
    use(input);
    js.PropertyAccess field = new js.PropertyAccess.field(pop(), fieldName);
    // Double negate to boolify the result.
    push(new js.Prefix('!', new js.Prefix('!', field)));
  }

  void checkFieldDoesNotExist(HInstruction input, String fieldName) {
    use(input);
    js.PropertyAccess field = new js.PropertyAccess.field(pop(), fieldName);
    push(new js.Prefix('!', field));
  }

  void checkImmutableArray(HInstruction input) {
    checkFieldExists(input, 'immutable\$list');
  }

  void checkMutableArray(HInstruction input) {
    checkFieldDoesNotExist(input, 'immutable\$list');
  }

  void checkExtendableArray(HInstruction input) {
    checkFieldDoesNotExist(input, 'fixed\$length');
  }

  void checkFixedArray(HInstruction input) {
    checkFieldExists(input, 'fixed\$length');
  }

  void checkNull(HInstruction input) {
    use(input);
    push(new js.Binary('==', pop(), new js.LiteralNull()));
  }

  void checkNonNull(HInstruction input) {
    use(input);
    push(new js.Binary('!=', pop(), new js.LiteralNull()));
  }

  bool checkIndexingBehavior(HInstruction input, {bool negative: false}) {
    if (!compiler.enqueuer.resolution.seenClasses.contains(
          backend.jsIndexingBehaviorInterface)) {
      return false;
    }

    use(input);
    js.Expression object1 = pop();
    use(input);
    js.Expression object2 = pop();
    push(backend.generateIsJsIndexableCall(object1, object2));
    if (negative) push(new js.Prefix('!', pop()));
    return true;
  }

  void checkType(HInstruction input, HInstruction interceptor,
                 DartType type, {bool negative: false}) {
    Element element = type.element;
    if (element == backend.jsArrayClass) {
      checkArray(input, negative ? '!==': '===');
      return;
    } else if (element == backend.jsMutableArrayClass) {
      if (negative) {
        checkImmutableArray(input);
      } else {
        checkMutableArray(input);
      }
      return;
    } else if (element == backend.jsExtendableArrayClass) {
      if (negative) {
        checkFixedArray(input);
      } else {
        checkExtendableArray(input);
      }
      return;
    } else if (element == backend.jsFixedArrayClass) {
      if (negative) {
        checkExtendableArray(input);
      } else {
        checkFixedArray(input);
      }
      return;
    }
    if (interceptor != null) {
      use(interceptor);
    } else {
      use(input);
    }

    world.registerIsCheck(type, work.resolutionTree);

    js.PropertyAccess field =
        new js.PropertyAccess.field(pop(), backend.namer.operatorIsType(type));
    // We always negate at least once so that the result is boolified.
    push(new js.Prefix('!', field));
    // If the result is not negated, put another '!' in front.
    if (!negative) push(new js.Prefix('!', pop()));
  }

  void handleNumberOrStringSupertypeCheck(HInstruction input,
                                          HInstruction interceptor,
                                          DartType type,
                                          { bool negative: false }) {
    assert(!identical(type.element, compiler.listClass)
           && !Elements.isListSupertype(type.element, compiler)
           && !Elements.isStringOnlySupertype(type.element, compiler));
    String relation = negative ? '!==' : '===';
    checkNum(input, relation);
    js.Expression numberTest = pop();
    checkString(input, relation);
    js.Expression stringTest = pop();
    checkObject(input, relation);
    js.Expression objectTest = pop();
    checkType(input, interceptor, type, negative: negative);
    String combiner = negative ? '&&' : '||';
    String combiner2 = negative ? '||' : '&&';
    push(new js.Binary(combiner,
                       new js.Binary(combiner, numberTest, stringTest),
                       new js.Binary(combiner2, objectTest, pop())));
  }

  void handleStringSupertypeCheck(HInstruction input,
                                  HInstruction interceptor,
                                  DartType type,
                                  { bool negative: false }) {
    assert(!identical(type.element, compiler.listClass)
           && !Elements.isListSupertype(type.element, compiler)
           && !Elements.isNumberOrStringSupertype(type.element, compiler));
    String relation = negative ? '!==' : '===';
    checkString(input, relation);
    js.Expression stringTest = pop();
    checkObject(input, relation);
    js.Expression objectTest = pop();
    checkType(input, interceptor, type, negative: negative);
    String combiner = negative ? '||' : '&&';
    push(new js.Binary(negative ? '&&' : '||',
                       stringTest,
                       new js.Binary(combiner, objectTest, pop())));
  }

  void handleListOrSupertypeCheck(HInstruction input,
                                  HInstruction interceptor,
                                  DartType type,
                                  { bool negative: false }) {
    assert(!identical(type.element, compiler.stringClass)
           && !Elements.isStringOnlySupertype(type.element, compiler)
           && !Elements.isNumberOrStringSupertype(type.element, compiler));
    String relation = negative ? '!==' : '===';
    checkObject(input, relation);
    js.Expression objectTest = pop();
    checkArray(input, relation);
    js.Expression arrayTest = pop();
    checkType(input, interceptor, type, negative: negative);
    String combiner = negative ? '&&' : '||';
    push(new js.Binary(negative ? '||' : '&&',
                       objectTest,
                       new js.Binary(combiner, arrayTest, pop())));
  }

  void visitIs(HIs node) {
    emitIs(node, "===");
  }

  void emitIs(HIs node, String relation)  {
    DartType type = node.typeExpression;
    world.registerIsCheck(type, work.resolutionTree);
    HInstruction input = node.expression;

    // If this is changed to single == there are several places below that must
    // be changed to match.
    assert(relation == '===' || relation == '!==');
    bool negative = relation == '!==';

    if (node.isVariableCheck || node.isCompoundCheck) {
      use(node.checkCall);
      if (negative) push(new js.Prefix('!', pop()));
    } else {
      assert(node.isRawCheck);
      HInstruction interceptor = node.interceptor;
      LibraryElement coreLibrary = compiler.coreLibrary;
      ClassElement objectClass = compiler.objectClass;
      Element element = type.element;
      if (element == compiler.nullClass) {
        if (negative) {
          checkNonNull(input);
        } else {
          checkNull(input);
        }
      } else if (identical(element, objectClass) || type.treatAsDynamic) {
        // The constant folder also does this optimization, but we make
        // it safe by assuming it may have not run.
        push(newLiteralBool(!negative), node);
      } else if (element == compiler.stringClass) {
        checkString(input, relation);
        attachLocationToLast(node);
      } else if (element == compiler.doubleClass) {
        checkDouble(input, relation);
        attachLocationToLast(node);
      } else if (element == compiler.numClass) {
        checkNum(input, relation);
        attachLocationToLast(node);
      } else if (element == compiler.boolClass) {
        checkBool(input, relation);
        attachLocationToLast(node);
      } else if (element == compiler.intClass) {
        // The is check in the code tells us that it might not be an
        // int. So we do a typeof first to avoid possible
        // deoptimizations on the JS engine due to the Math.floor check.
        checkNum(input, relation);
        js.Expression numTest = pop();
        checkBigInt(input, relation);
        push(new js.Binary(negative ? '||' : '&&', numTest, pop()), node);
      } else if (Elements.isNumberOrStringSupertype(element, compiler)) {
        handleNumberOrStringSupertypeCheck(
            input, interceptor, type, negative: negative);
        attachLocationToLast(node);
      } else if (Elements.isStringOnlySupertype(element, compiler)) {
        handleStringSupertypeCheck(
            input, interceptor, type, negative: negative);
        attachLocationToLast(node);
      } else if (identical(element, compiler.listClass)
                 || Elements.isListSupertype(element, compiler)) {
        handleListOrSupertypeCheck(
            input, interceptor, type, negative: negative);
        attachLocationToLast(node);
      } else if (type.kind == TypeKind.FUNCTION) {
        checkType(input, interceptor, type, negative: negative);
        attachLocationToLast(node);
      } else if ((input.canBePrimitive(compiler)
                  && !input.canBePrimitiveArray(compiler))
                 || input.canBeNull()) {
        checkObject(input, relation);
        js.Expression objectTest = pop();
        checkType(input, interceptor, type, negative: negative);
        push(new js.Binary(negative ? '||' : '&&', objectTest, pop()), node);
      } else {
        checkType(input, interceptor, type, negative: negative);
        attachLocationToLast(node);
      }
    }
  }

  js.Expression generateTest(HInstruction input, TypeMask checkedType) {
    TypeMask receiver = input.instructionType;
    // Figure out if it is beneficial to turn this into a null check.
    // V8 generally prefers 'typeof' checks, but for integers and
    // indexable primitives we cannot compile this test into a single
    // typeof check so the null check is cheaper.
    bool turnIntoNumCheck = input.isIntegerOrNull(compiler)
        && checkedType.containsOnlyInt(compiler);
    bool turnIntoNullCheck = !turnIntoNumCheck
        && (checkedType.nullable() == receiver)
        && (checkedType.containsOnlyInt(compiler)
            || checkedType.satisfies(backend.jsIndexableClass, compiler));
    js.Expression test;
    if (turnIntoNullCheck) {
      use(input);
      test = new js.Binary("==", pop(), new js.LiteralNull());
    } else if (checkedType.containsOnlyInt(compiler) && !turnIntoNumCheck) {
      // input is !int
      checkInt(input, '!==');
      test = pop();
    } else if (checkedType.containsOnlyNum(compiler) || turnIntoNumCheck) {
      // input is !num
      checkNum(input, '!==');
      test = pop();
    } else if (checkedType.containsOnlyBool(compiler)) {
      // input is !bool
      checkBool(input, '!==');
      test = pop();
    } else if (checkedType.containsOnlyString(compiler)) {
      // input is !string
      checkString(input, '!==');
      test = pop();
    } else if (checkedType.satisfies(backend.jsExtendableArrayClass,
                                     compiler)) {
      // input is !Object || input is !Array || input.isFixed
      checkObject(input, '!==');
      js.Expression objectTest = pop();
      checkArray(input, '!==');
      js.Expression arrayTest = pop();
      checkFixedArray(input);
      test = new js.Binary('||', objectTest, arrayTest);
      test = new js.Binary('||', test, pop());
    } else if (checkedType.satisfies(backend.jsMutableArrayClass, compiler)) {
      // input is !Object
      // || ((input is !Array || input.isImmutable)
      //     && input is !JsIndexingBehavior)
      checkObject(input, '!==');
      js.Expression objectTest = pop();
      checkArray(input, '!==');
      js.Expression arrayTest = pop();
      checkImmutableArray(input);
      js.Binary notArrayOrImmutable = new js.Binary('||', arrayTest, pop());

      js.Binary notIndexing = checkIndexingBehavior(input, negative: true)
          ? new js.Binary('&&', notArrayOrImmutable, pop())
          : notArrayOrImmutable;
      test = new js.Binary('||', objectTest, notIndexing);
    } else if (checkedType.satisfies(backend.jsArrayClass, compiler)) {
      // input is !Object
      // || (input is !Array && input is !JsIndexingBehavior)
      checkObject(input, '!==');
      js.Expression objectTest = pop();
      checkArray(input, '!==');
      js.Expression arrayTest = pop();

      js.Expression notIndexing = checkIndexingBehavior(input, negative: true)
          ? new js.Binary('&&', arrayTest, pop())
          : arrayTest;
      test = new js.Binary('||', objectTest, notIndexing);
    } else if (checkedType.satisfies(backend.jsIndexableClass, compiler)) {
      // input is !String
      // && (input is !Object
      //     || (input is !Array && input is !JsIndexingBehavior))
      checkString(input, '!==');
      js.Expression stringTest = pop();
      checkObject(input, '!==');
      js.Expression objectTest = pop();
      checkArray(input, '!==');
      js.Expression arrayTest = pop();

      js.Binary notIndexingTest = checkIndexingBehavior(input, negative: true)
          ? new js.Binary('&&', arrayTest, pop())
          : arrayTest;
      js.Binary notObjectOrIndexingTest =
          new js.Binary('||', objectTest, notIndexingTest);
      test = new js.Binary('&&', stringTest, notObjectOrIndexingTest);
    } else {
      compiler.internalError('Unexpected check', instruction: input);
    }
    return test;
  }

  void visitTypeConversion(HTypeConversion node) {
    if (node.isArgumentTypeCheck || node.isReceiverTypeCheck) {
      // An int check if the input is not int or null, is not
      // sufficient for doing a argument or receiver check.
      assert(!node.checkedType.containsOnlyInt(compiler) ||
             node.checkedInput.isIntegerOrNull(compiler));
      js.Expression test = generateTest(node.checkedInput, node.checkedType);
      js.Block oldContainer = currentContainer;
      js.Statement body = new js.Block.empty();
      currentContainer = body;
      if (node.isArgumentTypeCheck) {
        generateThrowWithHelper('iae', node.checkedInput);
      } else if (node.isReceiverTypeCheck) {
        use(node.checkedInput);
        String methodName =
            backend.namer.invocationName(node.receiverTypeCheckSelector);
        js.Expression call = jsPropertyCall(pop(), methodName, []);
        pushStatement(new js.Throw(call));
      }
      currentContainer = oldContainer;
      body = unwrapStatement(body);
      pushStatement(new js.If.noElse(test, body), node);
      return;
    }

    assert(node.isCheckedModeCheck || node.isCastTypeCheck);
    DartType type = node.typeExpression;
    assert(type.kind != TypeKind.TYPEDEF);
    if (type.kind == TypeKind.FUNCTION) {
      // TODO(5022): We currently generate $isFunction checks for
      // function types.
      world.registerIsCheck(
          compiler.functionClass.computeType(compiler), work.resolutionTree);
    }
    world.registerIsCheck(type, work.resolutionTree);

    CheckedModeHelper helper;
    FunctionElement helperElement;
    if (node.isBooleanConversionCheck) {
      helper =
          const CheckedModeHelper('boolConversionCheck');
    } else {
      helper =
          backend.getCheckedModeHelper(type, typeCast: node.isCastTypeCheck);
    }

    push(helper.generateCall(this, node));
  }

  void visitTypeKnown(HTypeKnown node) {
    // [HTypeKnown] instructions are removed before generating code.
    assert(false);
  }
}

String singleIdentityComparison(HInstruction left,
                                HInstruction right,
                                Compiler compiler) {
  // Returns the single identity comparison (== or ===) or null if a more
  // complex expression is required.
  if ((left.isConstant() && left.isConstantSentinel()) ||
      (right.isConstant() && right.isConstantSentinel())) return '===';
  if (left.canBeNull() && right.canBeNull()) {
    if (left.isConstantNull() || right.isConstantNull() ||
        (left.isPrimitive(compiler) &&
         left.instructionType == right.instructionType)) {
      return '==';
    }
    return null;
  } else {
    return '===';
  }
}
