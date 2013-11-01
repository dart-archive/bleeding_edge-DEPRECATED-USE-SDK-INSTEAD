// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of ssa;

abstract class OptimizationPhase {
  String get name;
  void visitGraph(HGraph graph);
}

class SsaOptimizerTask extends CompilerTask {
  final JavaScriptBackend backend;
  SsaOptimizerTask(JavaScriptBackend backend)
    : this.backend = backend,
      super(backend.compiler);
  String get name => 'SSA optimizer';
  Compiler get compiler => backend.compiler;

  void runPhases(HGraph graph, List<OptimizationPhase> phases) {
    for (OptimizationPhase phase in phases) {
      runPhase(graph, phase);
    }
  }

  void runPhase(HGraph graph, OptimizationPhase phase) {
    phase.visitGraph(graph);
    compiler.tracer.traceGraph(phase.name, graph);
    assert(graph.isValid());
  }

  void optimize(CodegenWorkItem work, HGraph graph) {
    ConstantSystem constantSystem = compiler.backend.constantSystem;
    JavaScriptItemCompilationContext context = work.compilationContext;
    measure(() {
      SsaDeadCodeEliminator dce;
      List<OptimizationPhase> phases = <OptimizationPhase>[
          // Run trivial instruction simplification first to optimize
          // some patterns useful for type conversion.
          new SsaInstructionSimplifier(constantSystem, backend, work),
          new SsaTypeConversionInserter(compiler),
          new SsaRedundantPhiEliminator(),
          new SsaDeadPhiEliminator(),
          new SsaTypePropagator(compiler),
          // After type propagation, more instructions can be
          // simplified.
          new SsaInstructionSimplifier(constantSystem, backend, work),
          new SsaCheckInserter(backend, work, context.boundsChecked),
          new SsaInstructionSimplifier(constantSystem, backend, work),
          new SsaCheckInserter(backend, work, context.boundsChecked),
          new SsaTypePropagator(compiler),
          // Run a dead code eliminator before LICM because dead
          // interceptors are often in the way of LICM'able instructions.
          new SsaDeadCodeEliminator(compiler),
          new SsaGlobalValueNumberer(compiler),
          // After GVN, some instructions might need their type to be
          // updated because they now have different inputs.
          new SsaTypePropagator(compiler),
          new SsaCodeMotion(),
          new SsaValueRangeAnalyzer(compiler, constantSystem, work),
          // Previous optimizations may have generated new
          // opportunities for instruction simplification.
          new SsaInstructionSimplifier(constantSystem, backend, work),
          new SsaCheckInserter(backend, work, context.boundsChecked),
          new SsaSimplifyInterceptors(compiler, constantSystem, work),
          dce = new SsaDeadCodeEliminator(compiler)];
      runPhases(graph, phases);
      if (!dce.eliminatedSideEffects) return;
      phases = <OptimizationPhase>[
          new SsaGlobalValueNumberer(compiler),
          new SsaCodeMotion(),
          new SsaValueRangeAnalyzer(compiler, constantSystem, work),
          new SsaInstructionSimplifier(constantSystem, backend, work),
          new SsaCheckInserter(backend, work, context.boundsChecked),
          new SsaSimplifyInterceptors(compiler, constantSystem, work),
          new SsaDeadCodeEliminator(compiler)];
      runPhases(graph, phases);
    });
  }
}

/**
 * If both inputs to known operations are available execute the operation at
 * compile-time.
 */
class SsaInstructionSimplifier extends HBaseVisitor
    implements OptimizationPhase {
  final String name = "SsaInstructionSimplifier";
  final JavaScriptBackend backend;
  final CodegenWorkItem work;
  final ConstantSystem constantSystem;
  HGraph graph;
  Compiler get compiler => backend.compiler;

  SsaInstructionSimplifier(this.constantSystem, this.backend, this.work);

  void visitGraph(HGraph visitee) {
    graph = visitee;
    visitDominatorTree(visitee);
  }

  visitBasicBlock(HBasicBlock block) {
    HInstruction instruction = block.first;
    while (instruction != null) {
      HInstruction next = instruction.next;
      HInstruction replacement = instruction.accept(this);
      if (replacement != instruction) {
        block.rewrite(instruction, replacement);

        // If we can replace [instruction] with [replacement], then
        // [replacement]'s type can be narrowed.
        TypeMask newType = replacement.instructionType.intersection(
            instruction.instructionType, compiler);
        replacement.instructionType = newType;

        // If the replacement instruction does not know its
        // source element, use the source element of the
        // instruction.
        if (replacement.sourceElement == null) {
          replacement.sourceElement = instruction.sourceElement;
        }
        if (replacement.sourcePosition == null) {
          replacement.sourcePosition = instruction.sourcePosition;
        }
        if (!replacement.isInBasicBlock()) {
          // The constant folding can return an instruction that is already
          // part of the graph (like an input), so we only add the replacement
          // if necessary.
          block.addAfter(instruction, replacement);
          // Visit the replacement as the next instruction in case it
          // can also be constant folded away.
          next = replacement;
        }
        block.remove(instruction);
      }
      instruction = next;
    }
  }

  HInstruction visitInstruction(HInstruction node) {
    return node;
  }

  HInstruction visitBoolify(HBoolify node) {
    List<HInstruction> inputs = node.inputs;
    assert(inputs.length == 1);
    HInstruction input = inputs[0];
    if (input.isBoolean(compiler)) return input;
    // All values that cannot be 'true' are boolified to false.
    TypeMask mask = input.instructionType;
    if (!mask.contains(backend.jsBoolClass, compiler)) {
      return graph.addConstantBool(false, compiler);
    }
    return node;
  }

  HInstruction visitNot(HNot node) {
    List<HInstruction> inputs = node.inputs;
    assert(inputs.length == 1);
    HInstruction input = inputs[0];
    if (input is HConstant) {
      HConstant constant = input;
      bool isTrue = constant.constant.isTrue();
      return graph.addConstantBool(!isTrue, compiler);
    } else if (input is HNot) {
      return input.inputs[0];
    }
    return node;
  }

  HInstruction visitInvokeUnary(HInvokeUnary node) {
    HInstruction folded =
        foldUnary(node.operation(constantSystem), node.operand);
    return folded != null ? folded : node;
  }

  HInstruction foldUnary(UnaryOperation operation, HInstruction operand) {
    if (operand is HConstant) {
      HConstant receiver = operand;
      Constant folded = operation.fold(receiver.constant);
      if (folded != null) return graph.addConstant(folded, compiler);
    }
    return null;
  }

  HInstruction tryOptimizeLengthInterceptedGetter(HInvokeDynamic node) {
    HInstruction actualReceiver = node.inputs[1];
    if (actualReceiver.isIndexablePrimitive(compiler)) {
      if (actualReceiver.isConstantString()) {
        HConstant constantInput = actualReceiver;
        StringConstant constant = constantInput.constant;
        return graph.addConstantInt(constant.length, compiler);
      } else if (actualReceiver.isConstantList()) {
        HConstant constantInput = actualReceiver;
        ListConstant constant = constantInput.constant;
        return graph.addConstantInt(constant.length, compiler);
      }
      Element element = backend.jsIndexableLength;
      var mask = actualReceiver.instructionType;
      bool isFixedLength = false;
      if (mask.isContainer && mask.length != null) {
        // A container on which we have inferred the length.
        isFixedLength = true;
      } else if (mask.containsOnly(backend.jsFixedArrayClass)
                 || mask.containsOnlyString(compiler)
                 || backend.isTypedArray(mask)) {
        isFixedLength = true;
      }
      HFieldGet result = new HFieldGet(
          element, actualReceiver, backend.intType,
          isAssignable: !isFixedLength);
      return result;
    } else if (actualReceiver.isConstantMap()) {
      HConstant constantInput = actualReceiver;
      MapConstant constant = constantInput.constant;
      return graph.addConstantInt(constant.length, compiler);
    }
    return null;
  }

  HInstruction handleInterceptedCall(HInvokeDynamic node) {
    // Try constant folding the instruction.
    Operation operation = node.specializer.operation(constantSystem);
    if (operation != null) {
      HInstruction instruction = node.inputs.length == 2
          ? foldUnary(operation, node.inputs[1])
          : foldBinary(operation, node.inputs[1], node.inputs[2]);
      if (instruction != null) return instruction;
    }

    // Try converting the instruction to a builtin instruction.
    HInstruction instruction =
        node.specializer.tryConvertToBuiltin(node, compiler);
    if (instruction != null) return instruction;

    Selector selector = node.selector;
    HInstruction input = node.inputs[1];

    if (selector.isCall() || selector.isOperator()) {
      Element target;
      if (input.isExtendableArray(compiler)) {
        if (selector.applies(backend.jsArrayRemoveLast, compiler)) {
          target = backend.jsArrayRemoveLast;
        } else if (selector.applies(backend.jsArrayAdd, compiler)) {
          // The codegen special cases array calls, but does not
          // inline argument type checks.
          if (!compiler.enableTypeAssertions) {
            target = backend.jsArrayAdd;
          }
        }
      } else if (input.isString(compiler)) {
        if (selector.applies(backend.jsStringSplit, compiler)) {
          HInstruction argument = node.inputs[2];
          if (argument.isString(compiler) && !argument.canBeNull()) {
            target = backend.jsStringSplit;
          }
        } else if (selector.applies(backend.jsStringOperatorAdd, compiler)) {
          // `operator+` is turned into a JavaScript '+' so we need to
          // make sure the receiver and the argument are not null.
          HInstruction argument = node.inputs[2];
          if (argument.isString(compiler)
              && !argument.canBeNull()
              && !input.canBeNull()) {
            target = backend.jsStringOperatorAdd;
          }
        } else if (selector.applies(backend.jsStringToString, compiler)
                   && !input.canBeNull()) {
          return input;
        }
      }
      if (target != null) {
        // TODO(ngeoffray): There is a strong dependency between codegen
        // and this optimization that the dynamic invoke does not need an
        // interceptor. We currently need to keep a
        // HInvokeDynamicMethod and not create a HForeign because
        // HForeign is too opaque for the SsaCheckInserter (that adds a
        // bounds check on removeLast). Once we start inlining, the
        // bounds check will become explicit, so we won't need this
        // optimization.
        HInvokeDynamicMethod result = new HInvokeDynamicMethod(
            node.selector, node.inputs.sublist(1), node.instructionType);
        result.element = target;
        return result;
      }
    } else if (selector.isGetter()) {
      if (selector.asUntyped.applies(backend.jsIndexableLength, compiler)) {
        HInstruction optimized = tryOptimizeLengthInterceptedGetter(node);
        if (optimized != null) return optimized;
      }
    }

    return node;
  }

  HInstruction visitInvokeDynamicMethod(HInvokeDynamicMethod node) {
    if (node.isInterceptedCall) {
      HInstruction folded = handleInterceptedCall(node);
      if (folded != node) return folded;
    }

    TypeMask receiverType = node.getDartReceiver(compiler).instructionType;
    Selector selector = new TypedSelector(receiverType, node.selector);
    Element element = compiler.world.locateSingleElement(selector);
    // TODO(ngeoffray): Also fold if it's a getter or variable.
    if (element != null
        && element.isFunction()
        // If we found out that the only target is a [:noSuchMethod:],
        // we just ignore it.
        && element.name == selector.name) {
      FunctionElement method = element;

      if (method.isNative()) {
        HInstruction folded = tryInlineNativeMethod(node, method);
        if (folded != null) return folded;
      } else {
        // TODO(ngeoffray): If the method has optional parameters,
        // we should pass the default values.
        FunctionSignature parameters = method.computeSignature(compiler);
        if (parameters.optionalParameterCount == 0
            || parameters.parameterCount == node.selector.argumentCount) {
          node.element = element;
        }
      }
    }
    return node;
  }

  HInstruction tryInlineNativeMethod(HInvokeDynamicMethod node,
                                     FunctionElement method) {
    // Enable direct calls to a native method only if we don't run in checked
    // mode, where the Dart version may have type annotations on parameters and
    // return type that it should check.
    // Also check that the parameters are not functions: it's the callee that
    // will translate them to JS functions.
    //
    // TODO(ngeoffray): There are some cases where we could still inline in
    // checked mode if we know the arguments have the right type. And we could
    // do the closure conversion as well as the return type annotation check.

    if (!node.isInterceptedCall) return null;

    // TODO(sra): Check for legacy methods with bodies in the native strings.
    //   foo() native 'return something';
    // They should not be used.

    FunctionSignature signature = method.computeSignature(compiler);
    if (signature.optionalParametersAreNamed) return null;

    // Return types on native methods don't need to be checked, since the
    // declaration has to be truthful.

    // The call site might omit optional arguments. The inlined code must
    // preserve the number of arguments, so check only the actual arguments.

    List<HInstruction> inputs = node.inputs.sublist(1);
    int inputPosition = 1;  // Skip receiver.
    bool canInline = true;
    signature.forEachParameter((Element element) {
      if (inputPosition < inputs.length && canInline) {
        HInstruction input = inputs[inputPosition++];
        DartType type = element.computeType(compiler).unalias(compiler);
        if (type is FunctionType) {
          canInline = false;
        }
        if (compiler.enableTypeAssertions) {
          // TODO(sra): Check if [input] is guaranteed to pass the parameter
          // type check.  Consider using a strengthened type check to avoid
          // passing `null` to primitive types since the native methods usually
          // have non-nullable primitive parameter types.
          canInline = false;
        }
      }
    });

    if (!canInline) return null;

    // Strengthen instruction type from annotations to help optimize
    // dependent instructions.
    native.NativeBehavior nativeBehavior =
        native.NativeBehavior.ofMethod(method, compiler);
    TypeMask returnType =
        TypeMaskFactory.fromNativeBehavior(nativeBehavior, compiler);
    HInvokeDynamicMethod result =
        new HInvokeDynamicMethod(node.selector, inputs, returnType);
    result.element = method;
    return result;
  }

  HInstruction visitBoundsCheck(HBoundsCheck node) {
    HInstruction index = node.index;
    if (index.isInteger(compiler)) return node;
    if (index.isConstant()) {
      HConstant constantInstruction = index;
      assert(!constantInstruction.constant.isInt());
      if (!constantSystem.isInt(constantInstruction.constant)) {
        // -0.0 is a double but will pass the runtime integer check.
        node.staticChecks = HBoundsCheck.ALWAYS_FALSE;
      }
    }
    return node;
  }

  HInstruction foldBinary(BinaryOperation operation,
                          HInstruction left,
                          HInstruction right) {
    if (left is HConstant && right is HConstant) {
      HConstant op1 = left;
      HConstant op2 = right;
      Constant folded = operation.fold(op1.constant, op2.constant);
      if (folded != null) return graph.addConstant(folded, compiler);
    }
    return null;
  }

  HInstruction visitInvokeBinary(HInvokeBinary node) {
    HInstruction left = node.left;
    HInstruction right = node.right;
    BinaryOperation operation = node.operation(constantSystem);
    HConstant folded = foldBinary(operation, left, right);
    if (folded != null) return folded;
    return node;
  }

  bool allUsersAreBoolifies(HInstruction instruction) {
    List<HInstruction> users = instruction.usedBy;
    int length = users.length;
    for (int i = 0; i < length; i++) {
      if (users[i] is! HBoolify) return false;
    }
    return true;
  }

  HInstruction visitRelational(HRelational node) {
    if (allUsersAreBoolifies(node)) {
      // TODO(ngeoffray): Call a boolified selector.
      // This node stays the same, but the Boolify node will go away.
    }
    // Note that we still have to call [super] to make sure that we end up
    // in the remaining optimizations.
    return super.visitRelational(node);
  }

  HInstruction handleIdentityCheck(HRelational node) {
    HInstruction left = node.left;
    HInstruction right = node.right;
    TypeMask leftType = left.instructionType;
    TypeMask rightType = right.instructionType;

    // Intersection of int and double return conflicting, so
    // we don't optimize on numbers to preserve the runtime semantics.
    if (!(left.isNumberOrNull(compiler) && right.isNumberOrNull(compiler))) {
      TypeMask intersection = leftType.intersection(rightType, compiler);
      if (intersection.isEmpty && !intersection.isNullable) {
        return graph.addConstantBool(false, compiler);
      }
    }

    if (left.isNull() && right.isNull()) {
      return graph.addConstantBool(true, compiler);
    }

    if (left.isConstantBoolean() && right.isBoolean(compiler)) {
      HConstant constant = left;
      if (constant.constant.isTrue()) {
        return right;
      } else {
        return new HNot(right, backend.boolType);
      }
    }

    if (right.isConstantBoolean() && left.isBoolean(compiler)) {
      HConstant constant = right;
      if (constant.constant.isTrue()) {
        return left;
      } else {
        return new HNot(left, backend.boolType);
      }
    }

    return null;
  }

  HInstruction visitIdentity(HIdentity node) {
    HInstruction newInstruction = handleIdentityCheck(node);
    return newInstruction == null ? super.visitIdentity(node) : newInstruction;
  }

  void simplifyCondition(HBasicBlock block,
                         HInstruction condition,
                         bool value) {
    condition.dominatedUsers(block.first).forEach((user) {
      HInstruction newCondition = graph.addConstantBool(value, compiler);
      user.changeUse(condition, newCondition);
    });
  }

  HInstruction visitIf(HIf node) {
    HInstruction condition = node.condition;
    if (condition.isConstant()) return node;
    bool isNegated = condition is HNot;

    if (isNegated) {
      condition = condition.inputs[0];
    } else {
      // It is possible for LICM to move a negated version of the
      // condition out of the loop where it used. We still want to
      // simplify the nested use of the condition in that case, so
      // we look for all dominating negated conditions and replace
      // nested uses of them with true or false.
      Iterable<HInstruction> dominating = condition.usedBy.where((user) =>
          user is HNot && user.dominates(node));
      dominating.forEach((hoisted) {
        simplifyCondition(node.thenBlock, hoisted, false);
        simplifyCondition(node.elseBlock, hoisted, true);
      });
    }
    simplifyCondition(node.thenBlock, condition, !isNegated);
    simplifyCondition(node.elseBlock, condition, isNegated);
    return node;
  }

  HInstruction visitIs(HIs node) {
    DartType type = node.typeExpression;
    Element element = type.element;

    if (!node.isRawCheck) {
      return node;
    } else if (element.isTypedef()) {
      return node;
    } else if (element == compiler.functionClass) {
      return node;
    }

    if (element == compiler.objectClass || type.treatAsDynamic) {
      return graph.addConstantBool(true, compiler);
    }

    HInstruction expression = node.expression;
    if (expression.isInteger(compiler)) {
      if (identical(element, compiler.intClass)
          || identical(element, compiler.numClass)
          || Elements.isNumberOrStringSupertype(element, compiler)) {
        return graph.addConstantBool(true, compiler);
      } else if (identical(element, compiler.doubleClass)) {
        // We let the JS semantics decide for that check. Currently
        // the code we emit will always return true.
        return node;
      } else {
        return graph.addConstantBool(false, compiler);
      }
    } else if (expression.isDouble(compiler)) {
      if (identical(element, compiler.doubleClass)
          || identical(element, compiler.numClass)
          || Elements.isNumberOrStringSupertype(element, compiler)) {
        return graph.addConstantBool(true, compiler);
      } else if (identical(element, compiler.intClass)) {
        // We let the JS semantics decide for that check. Currently
        // the code we emit will return true for a double that can be
        // represented as a 31-bit integer and for -0.0.
        return node;
      } else {
        return graph.addConstantBool(false, compiler);
      }
    } else if (expression.isNumber(compiler)) {
      if (identical(element, compiler.numClass)) {
        return graph.addConstantBool(true, compiler);
      } else {
        // We cannot just return false, because the expression may be of
        // type int or double.
      }
    } else if (expression.canBePrimitiveNumber(compiler)
               && identical(element, compiler.intClass)) {
      // We let the JS semantics decide for that check.
      return node;
    // We need the [:hasTypeArguments:] check because we don't have
    // the notion of generics in the backend. For example, [:this:] in
    // a class [:A<T>:], is currently always considered to have the
    // raw type.
    } else if (!RuntimeTypes.hasTypeArguments(type)) {
      TypeMask expressionMask = expression.instructionType;
      TypeMask typeMask = (element == compiler.nullClass)
          ? new TypeMask.subtype(element)
          : new TypeMask.nonNullSubtype(element);
      if (expressionMask.union(typeMask, compiler) == typeMask) {
        return graph.addConstantBool(true, compiler);
      } else if (expressionMask.intersection(typeMask, compiler).isEmpty) {
        return graph.addConstantBool(false, compiler);
      }
    }
    return node;
  }

  HInstruction visitTypeConversion(HTypeConversion node) {
    HInstruction value = node.inputs[0];
    DartType type = node.typeExpression;
    if (type != null) {
      if (!type.treatAsRaw || type.kind == TypeKind.TYPE_VARIABLE) {
        return node;
      }
      if (type.kind == TypeKind.FUNCTION) {
        // TODO(johnniwinther): Optimize function type conversions.
        return node;
      }
    }
    return removeIfCheckAlwaysSucceeds(node, node.checkedType);
  }

  HInstruction visitTypeKnown(HTypeKnown node) {
    return removeIfCheckAlwaysSucceeds(node, node.knownType);
  }

  HInstruction removeIfCheckAlwaysSucceeds(HCheck node, TypeMask checkedType) {
    if (checkedType.containsAll(compiler)) return node;
    HInstruction input = node.checkedInput;
    TypeMask inputType = input.instructionType;
    return inputType.isInMask(checkedType, compiler) ? input : node;
  }

  Element findConcreteFieldForDynamicAccess(HInstruction receiver,
                                            Selector selector) {
    TypeMask receiverType = receiver.instructionType;
    return compiler.world.locateSingleField(
        new TypedSelector(receiverType, selector));
  }

  HInstruction visitFieldGet(HFieldGet node) {
    var receiver = node.receiver;
    if (node.element == backend.jsIndexableLength) {
      JavaScriptItemCompilationContext context = work.compilationContext;
      if (context.allocatedFixedLists.contains(receiver)) {
        // TODO(ngeoffray): checking if the second input is an integer
        // should not be necessary but it currently makes it easier for
        // other optimizations to reason about a fixed length constructor
        // that we know takes an int.
        if (receiver.inputs[0].isInteger(compiler)) {
          return receiver.inputs[0];
        }
      } else if (receiver.isConstantList() || receiver.isConstantString()) {
        return graph.addConstantInt(receiver.constant.length, compiler);
      } else {
        var type = receiver.instructionType;
        if (type.isContainer && type.length != null) {
          HInstruction constant = graph.addConstantInt(type.length, compiler);
          if (type.isNullable) {
            // If the container can be null, we update all uses of the
            // length access to use the constant instead, but keep the
            // length access in the graph, to ensure we still have a
            // null check.
            node.block.rewrite(node, constant);
            return node;
          } else {
            return constant;
          }
        }
      }
    }
    return node;
  }

  HInstruction visitIndex(HIndex node) {
    if (node.receiver.isConstantList() && node.index.isConstantInteger()) {
      var instruction = node.receiver;
      List<Constant> entries = instruction.constant.entries;
      instruction = node.index;
      int index = instruction.constant.value;
      if (index >= 0 && index < entries.length) {
        return graph.addConstant(entries[index], compiler);
      }
    }
    return node;
  }

  HInstruction visitInvokeDynamicGetter(HInvokeDynamicGetter node) {
    if (node.isInterceptedCall) {
      HInstruction folded = handleInterceptedCall(node);
      if (folded != node) return folded;
    }
    HInstruction receiver = node.getDartReceiver(compiler);
    Element field = findConcreteFieldForDynamicAccess(receiver, node.selector);
    if (field == null) return node;
    return directFieldGet(receiver, field);
  }

  HInstruction directFieldGet(HInstruction receiver, Element field) {
    Modifiers modifiers = field.modifiers;
    bool isAssignable = !(modifiers.isFinal() || modifiers.isConst());
    if (!compiler.resolverWorld.hasInvokedSetter(field, compiler)) {
      // If no setter is ever used for this field it is only initialized in the
      // initializer list.
      isAssignable = false;
    }
    if (field.isNative()) {
      // Some native fields are views of data that may be changed by operations.
      // E.g. node.firstChild depends on parentNode.removeBefore(n1, n2).
      // TODO(sra): Refine the effect classification so that native effects are
      // distinct from ordinary Dart effects.
      isAssignable = true;
    }

    TypeMask type;
    if (field.getEnclosingClass().isNative()) {
      type = TypeMaskFactory.fromNativeBehavior(
          native.NativeBehavior.ofFieldLoad(field, compiler),
          compiler);
    } else {
      type = TypeMaskFactory.inferredTypeForElement(field, compiler);
    }

    return new HFieldGet(
        field, receiver, type, isAssignable: isAssignable);
  }

  HInstruction visitInvokeDynamicSetter(HInvokeDynamicSetter node) {
    if (node.isInterceptedCall) {
      HInstruction folded = handleInterceptedCall(node);
      if (folded != node) return folded;
    }

    HInstruction receiver = node.getDartReceiver(compiler);
    Element field = findConcreteFieldForDynamicAccess(receiver, node.selector);
    if (field == null || !field.isAssignable()) return node;
    // Use [:node.inputs.last:] in case the call follows the
    // interceptor calling convention, but is not a call on an
    // interceptor.
    HInstruction value = node.inputs.last;
    if (compiler.enableTypeAssertions) {
      DartType type = field.computeType(compiler);
      if (!type.treatAsRaw || type.kind == TypeKind.TYPE_VARIABLE) {
        // We cannot generate the correct type representation here, so don't
        // inline this access.
        return node;
      }
      HInstruction other = value.convertType(
          compiler,
          type,
          HTypeConversion.CHECKED_MODE_CHECK);
      if (other != value) {
        node.block.addBefore(node, other);
        value = other;
      }
    }
    return new HFieldSet(field, receiver, value);
  }

  HInstruction visitStringConcat(HStringConcat node) {
    // Simplify string concat:
    //
    //     "" + R                ->  R
    //     L + ""                ->  L
    //     "L" + "R"             ->  "LR"
    //     (prefix + "L") + "R"  ->  prefix + "LR"
    //
    StringConstant getString(HInstruction instruction) {
      if (!instruction.isConstantString()) return null;
      HConstant constant = instruction;
      return constant.constant;
    }

    StringConstant leftString = getString(node.left);
    if (leftString != null && leftString.value.length == 0) return node.right;

    StringConstant rightString = getString(node.right);
    if (rightString == null) return node;
    if (rightString.value.length == 0) return node.left;

    HInstruction prefix = null;
    if (leftString == null) {
      if (node.left is! HStringConcat) return node;
      HStringConcat leftConcat = node.left;
      // Don't undo CSE.
      if (leftConcat.usedBy.length != 1) return node;
      prefix = leftConcat.left;
      leftString = getString(leftConcat.right);
      if (leftString == null) return node;
    }

    HInstruction folded = graph.addConstant(
        constantSystem.createString(
            new DartString.concat(leftString.value, rightString.value),
            node.node),
        compiler);
    if (prefix == null) return folded;
    return new HStringConcat(prefix, folded, node.node, backend.stringType);
  }

  HInstruction visitStringify(HStringify node) {
    HInstruction input = node.inputs[0];
    if (input.isString(compiler) && !input.canBeNull()) return input;
    if (input.isConstant()) {
      HConstant constant = input;
      if (!constant.constant.isPrimitive()) return node;
      PrimitiveConstant primitive = constant.constant;
      return graph.addConstant(constantSystem.createString(
          primitive.toDartString(), node.node), compiler);
    }
    return node;
  }

  HInstruction visitOneShotInterceptor(HOneShotInterceptor node) {
    return handleInterceptedCall(node);
  }
}

class SsaCheckInserter extends HBaseVisitor implements OptimizationPhase {
  final Set<HInstruction> boundsChecked;
  final CodegenWorkItem work;
  final JavaScriptBackend backend;
  final String name = "SsaCheckInserter";
  HGraph graph;

  SsaCheckInserter(this.backend,
                   this.work,
                   this.boundsChecked);

  void visitGraph(HGraph graph) {
    this.graph = graph;
    visitDominatorTree(graph);
  }

  void visitBasicBlock(HBasicBlock block) {
    HInstruction instruction = block.first;
    while (instruction != null) {
      HInstruction next = instruction.next;
      instruction = instruction.accept(this);
      instruction = next;
    }
  }

  HBoundsCheck insertBoundsCheck(HInstruction indexNode,
                                 HInstruction array,
                                 HInstruction indexArgument) {
    Compiler compiler = backend.compiler;
    bool isAssignable =
        !array.isFixedArray(compiler) && !array.isString(compiler);
    HFieldGet length = new HFieldGet(
        backend.jsIndexableLength, array, backend.intType,
        isAssignable: isAssignable);
    indexNode.block.addBefore(indexNode, length);

    HBoundsCheck check = new HBoundsCheck(
        indexArgument, length, array, backend.intType);
    indexNode.block.addBefore(indexNode, check);
    // If the index input to the bounds check was not known to be an integer
    // then we replace its uses with the bounds check, which is known to be an
    // integer.  However, if the input was already an integer we don't do this
    // because putting in a check instruction might obscure the real nature of
    // the index eg. if it is a constant.  The range information from the
    // BoundsCheck instruction is attached to the input directly by
    // visitBoundsCheck in the SsaValueRangeAnalyzer.
    if (!indexArgument.isInteger(compiler)) {
      indexArgument.replaceAllUsersDominatedBy(indexNode, check);
    }
    boundsChecked.add(indexNode);
    return check;
  }

  void visitIndex(HIndex node) {
    if (boundsChecked.contains(node)) return;
    HInstruction index = node.index;
    index = insertBoundsCheck(node, node.receiver, index);
  }

  void visitIndexAssign(HIndexAssign node) {
    if (boundsChecked.contains(node)) return;
    HInstruction index = node.index;
    index = insertBoundsCheck(node, node.receiver, index);
  }

  void visitInvokeDynamicMethod(HInvokeDynamicMethod node) {
    Element element = node.element;
    if (node.isInterceptedCall) return;
    if (element != backend.jsArrayRemoveLast) return;
    if (boundsChecked.contains(node)) return;
    insertBoundsCheck(
        node, node.receiver, graph.addConstantInt(0, backend.compiler));
  }
}

class SsaDeadCodeEliminator extends HGraphVisitor implements OptimizationPhase {
  final String name = "SsaDeadCodeEliminator";

  final Compiler compiler;
  SsaLiveBlockAnalyzer analyzer;
  bool eliminatedSideEffects = false;
  SsaDeadCodeEliminator(this.compiler);

  HInstruction zapInstructionCache;
  HInstruction get zapInstruction {
    return (zapInstructionCache == null)
        ? zapInstructionCache = analyzer.graph.addConstantInt(0, compiler)
        : zapInstructionCache;
  }

  /// Returns whether the next throwing instruction that may have side
  /// effects after [instruction], throws [NoSuchMethodError] on the
  /// same receiver of [instruction].
  bool hasFollowingThrowingNSM(HInstruction instruction) {
    HInstruction receiver = instruction.getDartReceiver(compiler);
    HInstruction current = instruction.next;
    do {
      if ((current.getDartReceiver(compiler) == receiver)
          && current.canThrow()) {
        return true;
      }
      if (current.canThrow() || current.sideEffects.hasSideEffects()) {
        return false;
      }
      if (current.next == null && current is HGoto) {
        // We do not merge blocks in our SSA graph, so if this block
        // just jumps to a single predecessor, visit this predecessor.
        assert(current.block.successors.length == 1);
        current = current.block.successors[0].first;
      } else {
        current = current.next;
      }
    } while (current != null);
    return false;
  }

  bool isDeadCode(HInstruction instruction) {
    if (!instruction.usedBy.isEmpty) return false;
    if (instruction.sideEffects.hasSideEffects()) return false;
    if (instruction.canThrow()
        && instruction.onlyThrowsNSM()
        && hasFollowingThrowingNSM(instruction)) {
      return true;
    }
    return !instruction.canThrow()
           && instruction is !HParameterValue
           && instruction is !HLocalSet;
  }

  void visitGraph(HGraph graph) {
    analyzer = new SsaLiveBlockAnalyzer(graph);
    analyzer.analyze();
    visitPostDominatorTree(graph);
    cleanPhis(graph);
  }

  void visitBasicBlock(HBasicBlock block) {
    bool isDeadBlock = analyzer.isDeadBlock(block);
    block.isLive = !isDeadBlock;
    // Start from the last non-control flow instruction in the block.
    HInstruction instruction = block.last.previous;
    while (instruction != null) {
      var previous = instruction.previous;
      if (isDeadBlock) {
        eliminatedSideEffects = eliminatedSideEffects ||
            instruction.sideEffects.hasSideEffects();
        removeUsers(instruction);
        block.remove(instruction);
      } else if (isDeadCode(instruction)) {
        block.remove(instruction);
      }
      instruction = previous;
    }
  }

  void cleanPhis(HGraph graph) {
    L: for (HBasicBlock block in graph.blocks) {
      List<HBasicBlock> predecessors = block.predecessors;
      if (predecessors.length < 2) continue L;
      // Find the index of the single live predecessor if it exists.
      int indexOfLive = -1;
      for (int i = 0; i < predecessors.length; i++) {
        if (predecessors[i].isLive) {
          if (indexOfLive >= 0) continue L;
          indexOfLive = i;
        }
      }
      // Run through the phis of the block and replace them with their input
      // that comes from the only live predecessor if that dominates the phi.
      block.forEachPhi((HPhi phi) {
        HInstruction replacement = (indexOfLive >= 0)
            ? phi.inputs[indexOfLive] : zapInstruction;
        if (replacement.dominates(phi)) {
          block.rewrite(phi, replacement);
          block.removePhi(phi);
        }
      });
    }
  }

  void removeUsers(HInstruction instruction) {
    instruction.usedBy.forEach((user) {
      removeInput(user, instruction);
    });
    instruction.usedBy.clear();
  }

  void removeInput(HInstruction user, HInstruction input) {
    List<HInstruction> inputs = user.inputs;
    for (int i = 0, length = inputs.length; i < length; i++) {
      if (input == inputs[i]) {
        HInstruction zap = zapInstruction;
        inputs[i] = zap;
        zap.usedBy.add(user);
      }
    }
  }
}

class SsaLiveBlockAnalyzer extends HBaseVisitor {
  final HGraph graph;
  final Set<HBasicBlock> live = new Set<HBasicBlock>();
  final List<HBasicBlock> worklist = <HBasicBlock>[];
  SsaLiveBlockAnalyzer(this.graph);

  bool isDeadBlock(HBasicBlock block) => !live.contains(block);

  void analyze() {
    markBlockLive(graph.entry);
    while (!worklist.isEmpty) {
      HBasicBlock live = worklist.removeLast();
      live.last.accept(this);
    }
  }

  void markBlockLive(HBasicBlock block) {
    if (!live.contains(block)) {
      worklist.add(block);
      live.add(block);
    }
  }

  void visitControlFlow(HControlFlow instruction) {
    instruction.block.successors.forEach(markBlockLive);
  }

  void visitIf(HIf instruction) {
    HInstruction condition = instruction.condition;
    if (condition.isConstant()) {
      if (condition.isConstantTrue()) {
        markBlockLive(instruction.thenBlock);
      } else {
        markBlockLive(instruction.elseBlock);
      }
    } else {
      visitControlFlow(instruction);
    }
  }
}

class SsaDeadPhiEliminator implements OptimizationPhase {
  final String name = "SsaDeadPhiEliminator";

  void visitGraph(HGraph graph) {
    final List<HPhi> worklist = <HPhi>[];
    // A set to keep track of the live phis that we found.
    final Set<HPhi> livePhis = new Set<HPhi>();

    // Add to the worklist all live phis: phis referenced by non-phi
    // instructions.
    for (final block in graph.blocks) {
      block.forEachPhi((HPhi phi) {
        for (final user in phi.usedBy) {
          if (user is !HPhi) {
            worklist.add(phi);
            livePhis.add(phi);
            break;
          }
        }
      });
    }

    // Process the worklist by propagating liveness to phi inputs.
    while (!worklist.isEmpty) {
      HPhi phi = worklist.removeLast();
      for (final input in phi.inputs) {
        if (input is HPhi && !livePhis.contains(input)) {
          worklist.add(input);
          livePhis.add(input);
        }
      }
    }

    // Remove phis that are not live.
    // Traverse in reverse order to remove phis with no uses before the
    // phis that they might use.
    // NOTICE: Doesn't handle circular references, but we don't currently
    // create any.
    List<HBasicBlock> blocks = graph.blocks;
    for (int i = blocks.length - 1; i >= 0; i--) {
      HBasicBlock block = blocks[i];
      HPhi current = block.phis.first;
      HPhi next = null;
      while (current != null) {
        next = current.next;
        if (!livePhis.contains(current)
            // TODO(ahe): Not sure the following is correct.
            && current.usedBy.isEmpty) {
          block.removePhi(current);
        }
        current = next;
      }
    }
  }
}

class SsaRedundantPhiEliminator implements OptimizationPhase {
  final String name = "SsaRedundantPhiEliminator";

  void visitGraph(HGraph graph) {
    final List<HPhi> worklist = <HPhi>[];

    // Add all phis in the worklist.
    for (final block in graph.blocks) {
      block.forEachPhi((HPhi phi) => worklist.add(phi));
    }

    while (!worklist.isEmpty) {
      HPhi phi = worklist.removeLast();

      // If the phi has already been processed, continue.
      if (!phi.isInBasicBlock()) continue;

      // Find if the inputs of the phi are the same instruction.
      // The builder ensures that phi.inputs[0] cannot be the phi
      // itself.
      assert(!identical(phi.inputs[0], phi));
      HInstruction candidate = phi.inputs[0];
      for (int i = 1; i < phi.inputs.length; i++) {
        HInstruction input = phi.inputs[i];
        // If the input is the phi, the phi is still candidate for
        // elimination.
        if (!identical(input, candidate) && !identical(input, phi)) {
          candidate = null;
          break;
        }
      }

      // If the inputs are not the same, continue.
      if (candidate == null) continue;

      // Because we're updating the users of this phi, we may have new
      // phis candidate for elimination. Add phis that used this phi
      // to the worklist.
      for (final user in phi.usedBy) {
        if (user is HPhi) worklist.add(user);
      }
      phi.block.rewrite(phi, candidate);
      phi.block.removePhi(phi);
    }
  }
}

class GvnWorkItem {
  final HBasicBlock block;
  final ValueSet valueSet;
  GvnWorkItem(this.block, this.valueSet);
}

class SsaGlobalValueNumberer implements OptimizationPhase {
  final String name = "SsaGlobalValueNumberer";
  final Compiler compiler;
  final Set<int> visited;

  List<int> blockChangesFlags;
  List<int> loopChangesFlags;

  SsaGlobalValueNumberer(this.compiler) : visited = new Set<int>();

  void visitGraph(HGraph graph) {
    computeChangesFlags(graph);
    moveLoopInvariantCode(graph);
    List<GvnWorkItem> workQueue =
        <GvnWorkItem>[new GvnWorkItem(graph.entry, new ValueSet())];
    do {
      GvnWorkItem item = workQueue.removeLast();
      visitBasicBlock(item.block, item.valueSet, workQueue);
    } while (!workQueue.isEmpty);
  }

  void moveLoopInvariantCode(HGraph graph) {
    for (int i = graph.blocks.length - 1; i >= 0; i--) {
      HBasicBlock block = graph.blocks[i];
      if (block.isLoopHeader()) {
        int changesFlags = loopChangesFlags[block.id];
        HLoopInformation info = block.loopInformation;
        // Iterate over all blocks of this loop. Note that blocks in
        // inner loops are not visited here, but we know they
        // were visited before because we are iterating in post-order.
        // So instructions that are GVN'ed in an inner loop are in their
        // loop entry, and [info.blocks] contains this loop entry.
        moveLoopInvariantCodeFromBlock(block, block, changesFlags);
        for (HBasicBlock other in info.blocks) {
          moveLoopInvariantCodeFromBlock(other, block, changesFlags);
        }
      }
    }
  }

  void moveLoopInvariantCodeFromBlock(HBasicBlock block,
                                      HBasicBlock loopHeader,
                                      int changesFlags) {
    assert(block.parentLoopHeader == loopHeader || block == loopHeader);
    HBasicBlock preheader = loopHeader.predecessors[0];
    int dependsFlags = SideEffects.computeDependsOnFlags(changesFlags);
    HInstruction instruction = block.first;
    bool isLoopAlwaysTaken() {
      HInstruction instruction = loopHeader.last;
      assert(instruction is HGoto || instruction is HLoopBranch);
      return instruction is HGoto
          || instruction.inputs[0].isConstantTrue();
    }
    bool firstInstructionInLoop = block == loopHeader
        // Compensate for lack of code motion.
        || (blockChangesFlags[loopHeader.id] == 0
            && isLoopAlwaysTaken()
            && loopHeader.successors[0] == block);
    while (instruction != null) {
      HInstruction next = instruction.next;
      if (instruction.useGvn()
          && (!instruction.canThrow() || firstInstructionInLoop)
          && !instruction.sideEffects.dependsOn(dependsFlags)) {
        bool loopInvariantInputs = true;
        List<HInstruction> inputs = instruction.inputs;
        for (int i = 0, length = inputs.length; i < length; i++) {
          if (isInputDefinedAfterDominator(inputs[i], preheader)) {
            loopInvariantInputs = false;
            break;
          }
        }

        // If the inputs are loop invariant, we can move the
        // instruction from the current block to the pre-header block.
        if (loopInvariantInputs) {
          block.detach(instruction);
          preheader.moveAtExit(instruction);
        } else {
          firstInstructionInLoop = false;
        }
      }
      int oldChangesFlags = changesFlags;
      changesFlags |= instruction.sideEffects.getChangesFlags();
      if (oldChangesFlags != changesFlags) {
        dependsFlags = SideEffects.computeDependsOnFlags(changesFlags);
      }
      instruction = next;
    }
  }

  bool isInputDefinedAfterDominator(HInstruction input,
                                    HBasicBlock dominator) {
    return input.block.id > dominator.id;
  }

  void visitBasicBlock(
      HBasicBlock block, ValueSet values, List<GvnWorkItem> workQueue) {
    HInstruction instruction = block.first;
    if (block.isLoopHeader()) {
      int flags = loopChangesFlags[block.id];
      values.kill(flags);
    }
    while (instruction != null) {
      HInstruction next = instruction.next;
      int flags = instruction.sideEffects.getChangesFlags();
      assert(flags == 0 || !instruction.useGvn());
      values.kill(flags);
      if (instruction.useGvn()) {
        HInstruction other = values.lookup(instruction);
        if (other != null) {
          assert(other.gvnEquals(instruction) && instruction.gvnEquals(other));
          block.rewriteWithBetterUser(instruction, other);
          block.remove(instruction);
        } else {
          values.add(instruction);
        }
      }
      instruction = next;
    }

    List<HBasicBlock> dominatedBlocks = block.dominatedBlocks;
    for (int i = 0, length = dominatedBlocks.length; i < length; i++) {
      HBasicBlock dominated = dominatedBlocks[i];
      // No need to copy the value set for the last child.
      ValueSet successorValues = (i == length - 1) ? values : values.copy();
      // If we have no values in our set, we do not have to kill
      // anything. Also, if the range of block ids from the current
      // block to the dominated block is empty, there is no blocks on
      // any path from the current block to the dominated block so we
      // don't have to do anything either.
      assert(block.id < dominated.id);
      if (!successorValues.isEmpty && block.id + 1 < dominated.id) {
        visited.clear();
        List<HBasicBlock> workQueue = <HBasicBlock>[dominated];
        int changesFlags = 0;
        do {
          HBasicBlock current = workQueue.removeLast();
          changesFlags |=
              getChangesFlagsForDominatedBlock(block, current, workQueue);
        } while (!workQueue.isEmpty);
        successorValues.kill(changesFlags);
      }
      workQueue.add(new GvnWorkItem(dominated, successorValues));
    }
  }

  void computeChangesFlags(HGraph graph) {
    // Create the changes flags lists. Make sure to initialize the
    // loop changes flags list to zero so we can use bitwise or when
    // propagating loop changes upwards.
    final int length = graph.blocks.length;
    blockChangesFlags = new List<int>(length);
    loopChangesFlags = new List<int>(length);
    for (int i = 0; i < length; i++) loopChangesFlags[i] = 0;

    // Run through all the basic blocks in the graph and fill in the
    // changes flags lists.
    for (int i = length - 1; i >= 0; i--) {
      final HBasicBlock block = graph.blocks[i];
      final int id = block.id;

      // Compute block changes flags for the block.
      int changesFlags = 0;
      HInstruction instruction = block.first;
      while (instruction != null) {
        changesFlags |= instruction.sideEffects.getChangesFlags();
        instruction = instruction.next;
      }
      assert(blockChangesFlags[id] == null);
      blockChangesFlags[id] = changesFlags;

      // Loop headers are part of their loop, so update the loop
      // changes flags accordingly.
      if (block.isLoopHeader()) {
        loopChangesFlags[id] |= changesFlags;
      }

      // Propagate loop changes flags upwards.
      HBasicBlock parentLoopHeader = block.parentLoopHeader;
      if (parentLoopHeader != null) {
        loopChangesFlags[parentLoopHeader.id] |= (block.isLoopHeader())
            ? loopChangesFlags[id]
            : changesFlags;
      }
    }
  }

  int getChangesFlagsForDominatedBlock(HBasicBlock dominator,
                                       HBasicBlock dominated,
                                       List<HBasicBlock> workQueue) {
    int changesFlags = 0;
    List<HBasicBlock> predecessors = dominated.predecessors;
    for (int i = 0, length = predecessors.length; i < length; i++) {
      HBasicBlock block = predecessors[i];
      int id = block.id;
      // If the current predecessor block is on the path from the
      // dominator to the dominated, it must have an id that is in the
      // range from the dominator to the dominated.
      if (dominator.id < id && id < dominated.id && !visited.contains(id)) {
        visited.add(id);
        changesFlags |= blockChangesFlags[id];
        // Loop bodies might not be on the path from dominator to dominated,
        // but they can invalidate values.
        changesFlags |= loopChangesFlags[id];
        workQueue.add(block);
      }
    }
    return changesFlags;
  }
}

// This phase merges equivalent instructions on different paths into
// one instruction in a dominator block. It runs through the graph
// post dominator order and computes a ValueSet for each block of
// instructions that can be moved to a dominator block. These
// instructions are the ones that:
// 1) can be used for GVN, and
// 2) do not use definitions of their own block.
//
// A basic block looks at its sucessors and finds the intersection of
// these computed ValueSet. It moves all instructions of the
// intersection into its own list of instructions.
class SsaCodeMotion extends HBaseVisitor implements OptimizationPhase {
  final String name = "SsaCodeMotion";

  List<ValueSet> values;

  void visitGraph(HGraph graph) {
    values = new List<ValueSet>(graph.blocks.length);
    for (int i = 0; i < graph.blocks.length; i++) {
      values[graph.blocks[i].id] = new ValueSet();
    }
    visitPostDominatorTree(graph);
  }

  void visitBasicBlock(HBasicBlock block) {
    List<HBasicBlock> successors = block.successors;

    // Phase 1: get the ValueSet of all successors (if there are more than one),
    // compute the intersection and move the instructions of the intersection
    // into this block.
    if (successors.length > 1) {
      ValueSet instructions = values[successors[0].id];
      for (int i = 1; i < successors.length; i++) {
        ValueSet other = values[successors[i].id];
        instructions = instructions.intersection(other);
      }

      if (!instructions.isEmpty) {
        List<HInstruction> list = instructions.toList();
        for (HInstruction instruction in list) {
          // Move the instruction to the current block.
          instruction.block.detach(instruction);
          block.moveAtExit(instruction);
          // Go through all successors and rewrite their instruction
          // to the shared one.
          for (final successor in successors) {
            HInstruction toRewrite = values[successor.id].lookup(instruction);
            if (toRewrite != instruction) {
              successor.rewriteWithBetterUser(toRewrite, instruction);
              successor.remove(toRewrite);
            }
          }
        }
      }
    }

    // Don't try to merge instructions to a dominator if we have
    // multiple predecessors.
    if (block.predecessors.length != 1) return;

    // Phase 2: Go through all instructions of this block and find
    // which instructions can be moved to a dominator block.
    ValueSet set_ = values[block.id];
    HInstruction instruction = block.first;
    int flags = 0;
    while (instruction != null) {
      int dependsFlags = SideEffects.computeDependsOnFlags(flags);
      flags |= instruction.sideEffects.getChangesFlags();

      HInstruction current = instruction;
      instruction = instruction.next;

      // TODO(ngeoffray): this check is needed because we currently do
      // not have flags to express 'Gvn'able', but not movable.
      if (current is HCheck) continue;
      if (!current.useGvn()) continue;
      if (current.sideEffects.dependsOn(dependsFlags)) continue;

      bool canBeMoved = true;
      for (final HInstruction input in current.inputs) {
        if (input.block == block) {
          canBeMoved = false;
          break;
        }
      }
      if (!canBeMoved) continue;

      // This is safe because we are running after GVN.
      // TODO(ngeoffray): ensure GVN has been run.
      set_.add(current);
    }
  }
}

class SsaTypeConversionInserter extends HBaseVisitor
    implements OptimizationPhase {
  final String name = "SsaTypeconversionInserter";
  final Compiler compiler;

  SsaTypeConversionInserter(this.compiler);

  void visitGraph(HGraph graph) {
    visitDominatorTree(graph);
  }

  // Update users of [input] that are dominated by [:dominator.first:]
  // to use [newInput] instead.
  void changeUsesDominatedBy(HBasicBlock dominator,
                             HInstruction input,
                             TypeMask convertedType) {
    Setlet<HInstruction> dominatedUsers = input.dominatedUsers(dominator.first);
    if (dominatedUsers.isEmpty) return;

    HTypeKnown newInput = new HTypeKnown(convertedType, input);
    dominator.addBefore(dominator.first, newInput);
    dominatedUsers.forEach((HInstruction user) {
      user.changeUse(input, newInput);
    });
  }

  void visitIs(HIs instruction) {
    DartType type = instruction.typeExpression;
    Element element = type.element;
    if (!instruction.isRawCheck) {
      return;
    } else if (element.isTypedef()) {
      return;
    }

    List<HInstruction> ifUsers = <HInstruction>[];
    List<HInstruction> notIfUsers = <HInstruction>[];
    for (HInstruction user in instruction.usedBy) {
      if (user is HIf) {
        ifUsers.add(user);
      } else if (user is HNot) {
        for (HInstruction notUser in user.usedBy) {
          if (notUser is HIf) notIfUsers.add(notUser);
        }
      }
    }

    if (ifUsers.isEmpty && notIfUsers.isEmpty) return;

    TypeMask convertedType = new TypeMask.nonNullSubtype(element);
    HInstruction input = instruction.expression;
    for (HIf ifUser in ifUsers) {
      changeUsesDominatedBy(ifUser.thenBlock, input, convertedType);
      // TODO(ngeoffray): Also change uses for the else block on a HType
      // that knows it is not of a specific Type.
    }

    for (HIf ifUser in notIfUsers) {
      changeUsesDominatedBy(ifUser.elseBlock, input, convertedType);
      // TODO(ngeoffray): Also change uses for the then block on a HType
      // that knows it is not of a specific Type.
    }
  }
}
