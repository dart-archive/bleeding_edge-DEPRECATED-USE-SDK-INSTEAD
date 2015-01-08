// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dart2js.js_emitter;

/// This class should morph into something that makes it easy to build
/// JavaScript representations of libraries, class-sides, and instance-sides.
/// Initially, it is just a placeholder for code that is moved from
/// [CodeEmitterTask].
class ContainerBuilder extends CodeEmitterHelper {
  bool needsSuperGetter(FunctionElement element) =>
    compiler.codegenWorld.methodsNeedingSuperGetter.contains(element);

  /**
   * Generate stubs to handle invocation of methods with optional
   * arguments.
   *
   * A method like [: foo([x]) :] may be invoked by the following
   * calls: [: foo(), foo(1), foo(x: 1) :]. See the sources of this
   * function for detailed examples.
   */
  void addParameterStub(FunctionElement member,
                        Selector selector,
                        AddStubFunction addStub,
                        Set<String> alreadyGenerated) {
    FunctionSignature parameters = member.functionSignature;
    int positionalArgumentCount = selector.positionalArgumentCount;
    if (positionalArgumentCount == parameters.parameterCount) {
      assert(selector.namedArgumentCount == 0);
      return;
    }
    if (parameters.optionalParametersAreNamed
        && selector.namedArgumentCount == parameters.optionalParameterCount) {
      // If the selector has the same number of named arguments as the element,
      // we don't need to add a stub. The call site will hit the method
      // directly.
      return;
    }
    JavaScriptConstantCompiler handler = backend.constants;
    List<String> names = selector.getOrderedNamedArguments();

    String invocationName = namer.invocationName(selector);
    if (alreadyGenerated.contains(invocationName)) return;
    alreadyGenerated.add(invocationName);

    bool isInterceptedMethod = backend.isInterceptedMethod(member);

    // If the method is intercepted, we need to also pass the actual receiver.
    int extraArgumentCount = isInterceptedMethod ? 1 : 0;
    // Use '$receiver' to avoid clashes with other parameter names. Using
    // '$receiver' works because [:namer.safeName:] used for getting parameter
    // names never returns a name beginning with a single '$'.
    String receiverArgumentName = r'$receiver';

    // The parameters that this stub takes.
    List<jsAst.Parameter> parametersBuffer =
        new List<jsAst.Parameter>(selector.argumentCount + extraArgumentCount);
    // The arguments that will be passed to the real method.
    List<jsAst.Expression> argumentsBuffer =
        new List<jsAst.Expression>(
            parameters.parameterCount + extraArgumentCount);

    int count = 0;
    if (isInterceptedMethod) {
      count++;
      parametersBuffer[0] = new jsAst.Parameter(receiverArgumentName);
      argumentsBuffer[0] = js('#', receiverArgumentName);
      emitter.interceptorEmitter.interceptorInvocationNames.add(invocationName);
    }

    int optionalParameterStart = positionalArgumentCount + extraArgumentCount;
    // Includes extra receiver argument when using interceptor convention
    int indexOfLastOptionalArgumentInParameters = optionalParameterStart - 1;

    int parameterIndex = 0;
    parameters.orderedForEachParameter((ParameterElement element) {
      String jsName = backend.namer.safeName(element.name);
      assert(jsName != receiverArgumentName);
      if (count < optionalParameterStart) {
        parametersBuffer[count] = new jsAst.Parameter(jsName);
        argumentsBuffer[count] = js('#', jsName);
      } else {
        int index = names.indexOf(element.name);
        if (index != -1) {
          indexOfLastOptionalArgumentInParameters = count;
          // The order of the named arguments is not the same as the
          // one in the real method (which is in Dart source order).
          argumentsBuffer[count] = js('#', jsName);
          parametersBuffer[optionalParameterStart + index] =
              new jsAst.Parameter(jsName);
        } else {
          ConstantExpression constant = handler.getConstantForVariable(element);
          if (constant == null) {
            argumentsBuffer[count] =
                emitter.constantReference(new NullConstantValue());
          } else {
            ConstantValue value = constant.value;
            if (!value.isNull) {
              // If the value is the null constant, we should not pass it
              // down to the native method.
              indexOfLastOptionalArgumentInParameters = count;
            }
            argumentsBuffer[count] = emitter.constantReference(value);
          }
        }
      }
      count++;
    });

    var body;  // List or jsAst.Statement.
    if (member.hasFixedBackendName) {
      body = emitter.nativeEmitter.generateParameterStubStatements(
          member, isInterceptedMethod, invocationName,
          parametersBuffer, argumentsBuffer,
          indexOfLastOptionalArgumentInParameters);
    } else if (member.isInstanceMember) {
      if (needsSuperGetter(member)) {
        ClassElement superClass = member.enclosingClass;
        String methodName = namer.getNameOfInstanceMember(member);
        // When redirecting, we must ensure that we don't end up in a subclass.
        // We thus can't just invoke `this.foo$1.call(filledInArguments)`.
        // Instead we need to call the statically resolved target.
        //   `<class>.prototype.bar$1.call(this, argument0, ...)`.
        body = js.statement(
            'return #.#.call(this, #);',
            [backend.emitter.prototypeAccess(superClass,
                                             hasBeenInstantiated: true),
             methodName,
             argumentsBuffer]);
      } else {
        body = js.statement(
            'return this.#(#);',
            [namer.getNameOfInstanceMember(member), argumentsBuffer]);
      }
    } else {
      body = js.statement('return #(#)',
          [emitter.staticFunctionAccess(member), argumentsBuffer]);
    }

    jsAst.Fun function = js('function(#) { #; }', [parametersBuffer, body]);

    addStub(selector, function);
  }

  void addParameterStubs(FunctionElement member, AddStubFunction defineStub,
                         [bool canTearOff = false]) {
    if (member.enclosingElement.isClosure) {
      ClosureClassElement cls = member.enclosingElement;
      if (cls.supertype.element == backend.boundClosureClass) {
        compiler.internalError(cls.methodElement, 'Bound closure1.');
      }
      if (cls.methodElement.isInstanceMember) {
        compiler.internalError(cls.methodElement, 'Bound closure2.');
      }
    }

    // We fill the lists depending on the selector. For example,
    // take method foo:
    //    foo(a, b, {c, d});
    //
    // We may have multiple ways of calling foo:
    // (1) foo(1, 2);
    // (2) foo(1, 2, c: 3);
    // (3) foo(1, 2, d: 4);
    // (4) foo(1, 2, c: 3, d: 4);
    // (5) foo(1, 2, d: 4, c: 3);
    //
    // What we generate at the call sites are:
    // (1) foo$2(1, 2);
    // (2) foo$3$c(1, 2, 3);
    // (3) foo$3$d(1, 2, 4);
    // (4) foo$4$c$d(1, 2, 3, 4);
    // (5) foo$4$c$d(1, 2, 3, 4);
    //
    // The stubs we generate are (expressed in Dart):
    // (1) foo$2(a, b) => foo$4$c$d(a, b, null, null)
    // (2) foo$3$c(a, b, c) => foo$4$c$d(a, b, c, null);
    // (3) foo$3$d(a, b, d) => foo$4$c$d(a, b, null, d);
    // (4) No stub generated, call is direct.
    // (5) No stub generated, call is direct.
    //
    // We need to pay attention if this stub is for a function that has been
    // invoked from a subclass. Then we cannot just redirect, since that
    // would invoke the methods of the subclass. We have to compile to:
    // (1) foo$2(a, b) => MyClass.foo$4$c$d.call(this, a, b, null, null)
    // (2) foo$3$c(a, b, c) => MyClass.foo$4$c$d(this, a, b, c, null);
    // (3) foo$3$d(a, b, d) => MyClass.foo$4$c$d(this, a, b, null, d);

    Set<Selector> selectors = member.isInstanceMember
        ? compiler.codegenWorld.invokedNames[member.name]
        : null; // No stubs needed for static methods.

    /// Returns all closure call selectors renamed to match this member.
    Set<Selector> callSelectorsAsNamed() {
      if (!canTearOff) return null;
      Set<Selector> callSelectors = compiler.codegenWorld.invokedNames[
          namer.closureInvocationSelectorName];
      if (callSelectors == null) return null;
      return callSelectors.map((Selector callSelector) {
        return new Selector.call(
            member.name, member.library,
            callSelector.argumentCount, callSelector.namedArguments);
      }).toSet();
    }
    if (selectors == null) {
      selectors = callSelectorsAsNamed();
      if (selectors == null) return;
    } else {
      Set<Selector> callSelectors = callSelectorsAsNamed();
      if (callSelectors != null) {
        selectors = selectors.union(callSelectors);
      }
    }
    Set<Selector> untypedSelectors = new Set<Selector>();
    if (selectors != null) {
      for (Selector selector in selectors) {
        if (!selector.appliesUnnamed(member, compiler.world)) continue;
        if (untypedSelectors.add(selector.asUntyped)) {
          // TODO(ahe): Is the last argument to [addParameterStub] needed?
          addParameterStub(member, selector, defineStub, new Set<String>());
        }
      }
    }
    if (canTearOff) {
      selectors = compiler.codegenWorld.invokedNames[
          namer.closureInvocationSelectorName];
      if (selectors != null) {
        for (Selector selector in selectors) {
          selector = new Selector.call(
              member.name, member.library,
              selector.argumentCount, selector.namedArguments);
          if (!selector.appliesUnnamed(member, compiler.world)) continue;
          if (untypedSelectors.add(selector)) {
            // TODO(ahe): Is the last argument to [addParameterStub] needed?
            addParameterStub(member, selector, defineStub, new Set<String>());
          }
        }
      }
    }
  }

  /**
   * Documentation wanted -- johnniwinther
   *
   * Invariant: [member] must be a declaration element.
   */
  void emitCallStubForGetter(Element member,
                             Set<Selector> selectors,
                             AddPropertyFunction addProperty) {
    assert(invariant(member, member.isDeclaration));
    LibraryElement memberLibrary = member.library;
    // If the method is intercepted, the stub gets the
    // receiver explicitely and we need to pass it to the getter call.
    bool isInterceptedMethod = backend.isInterceptedMethod(member);
    bool isInterceptorClass =
        backend.isInterceptorClass(member.enclosingClass);

    const String receiverArgumentName = r'$receiver';

    jsAst.Expression buildGetter() {
      jsAst.Expression receiver =
          js(isInterceptorClass ? receiverArgumentName : 'this');
      if (member.isGetter) {
        String getterName = namer.getterName(member);
        if (isInterceptedMethod) {
          return js('this.#(#)', [getterName, receiver]);
        }
        return js('#.#()', [receiver, getterName]);
      } else {
        String fieldName = namer.instanceFieldPropertyName(member);
        return js('#.#', [receiver, fieldName]);
      }
    }

    // Two selectors may match but differ only in type.  To avoid generating
    // identical stubs for each we track untyped selectors which already have
    // stubs.
    Set<Selector> generatedSelectors = new Set<Selector>();
    for (Selector selector in selectors) {
      if (selector.applies(member, compiler.world)) {
        selector = selector.asUntyped;
        if (generatedSelectors.contains(selector)) continue;
        generatedSelectors.add(selector);

        String invocationName = namer.invocationName(selector);
        Selector callSelector = new Selector.callClosureFrom(selector);
        String closureCallName = namer.invocationName(callSelector);

        List<jsAst.Parameter> parameters = <jsAst.Parameter>[];
        List<jsAst.Expression> arguments = <jsAst.Expression>[];
        if (isInterceptedMethod) {
          parameters.add(new jsAst.Parameter(receiverArgumentName));
        }

        for (int i = 0; i < selector.argumentCount; i++) {
          String name = 'arg$i';
          parameters.add(new jsAst.Parameter(name));
          arguments.add(js('#', name));
        }

        jsAst.Fun function = js(
            'function(#) { return #.#(#); }',
            [ parameters, buildGetter(), closureCallName, arguments]);

        compiler.dumpInfoTask.registerElementAst(member,
            addProperty(invocationName, function));
      }
    }
  }

  /**
   * Documentation wanted -- johnniwinther
   *
   * Invariant: [member] must be a declaration element.
   */
  void emitExtraAccessors(Element member, ClassBuilder builder) {
    assert(invariant(member, member.isDeclaration));
    if (member.isGetter || member.isField) {
      Set<Selector> selectors = compiler.codegenWorld.invokedNames[member.name];
      if (selectors != null && !selectors.isEmpty) {
        emitCallStubForGetter(member, selectors, builder.addProperty);
      }
    }
  }

  void addMember(Element member, ClassBuilder builder) {
    assert(invariant(member, member.isDeclaration));

    if (member.isField) {
      addMemberField(member, builder);
    } else if (member.isFunction ||
               member.isGenerativeConstructorBody ||
               member.isGenerativeConstructor ||
               member.isAccessor) {
      addMemberMethod(member, builder);
    } else {
      compiler.internalError(member,
          'Unexpected kind: "${member.kind}".');
    }
    if (member.isInstanceMember) emitExtraAccessors(member, builder);
  }

  void addMemberMethod(FunctionElement member, ClassBuilder builder) {
    MemberInfo info = analyzeMemberMethod(member);
    if (info != null) {
      addMemberMethodFromInfo(info, builder);
    }
  }

  MemberInfo analyzeMemberMethod(FunctionElement member) {
    if (member.isAbstract) return null;
    jsAst.Expression code = backend.generatedCode[member];
    if (code == null) return null;
    String name = namer.getNameOfMember(member);

    FunctionSignature parameters = member.functionSignature;
    bool needsStubs = !parameters.optionalParameters.isEmpty;
    bool canTearOff = false;
    bool isClosure = false;
    bool isNotApplyTarget = !member.isFunction ||
                            member.isConstructor ||
                            member.isAccessor;
    String tearOffName;


    final bool canBeReflected = backend.isAccessibleByReflection(member) ||
        // During incremental compilation, we have to assume that reflection
        // *might* get enabled.
        compiler.hasIncrementalSupport;

    if (isNotApplyTarget) {
      canTearOff = false;
    } else if (member.isInstanceMember) {
      if (member.enclosingClass.isClosure) {
        canTearOff = false;
        isClosure = true;
      } else {
        // Careful with operators.
        canTearOff =
            compiler.codegenWorld.hasInvokedGetter(member, compiler.world) ||
            (canBeReflected && !member.isOperator);
        assert(!needsSuperGetter(member) || canTearOff);
        tearOffName = namer.getterName(member);
      }
    } else {
      canTearOff =
          compiler.codegenWorld.staticFunctionsNeedingGetter.contains(member) ||
          canBeReflected;
      tearOffName = namer.getStaticClosureName(member);
    }
    final bool canBeApplied = compiler.enabledFunctionApply &&
                              compiler.world.getMightBePassedToApply(member);

    final bool hasSuperAlias = backend.isAliasedSuperMember(member);

    final bool needStructuredInfo =
        canTearOff || canBeReflected || canBeApplied || hasSuperAlias;


    if (canTearOff) {
      assert(invariant(member, !member.isGenerativeConstructor));
      assert(invariant(member, !member.isGenerativeConstructorBody));
      assert(invariant(member, !member.isConstructor));
    }

    return new MemberInfo(
        member,
        name,
        parameters,
        code,
        needsStubs: needsStubs,
        canTearOff: canTearOff,
        isClosure: isClosure,
        tearOffName: tearOffName,
        canBeReflected: canBeReflected,
        canBeApplied: canBeApplied,
        hasSuperAlias: hasSuperAlias,
        needStructuredInfo: needStructuredInfo
    );
  }

  void addMemberMethodFromInfo(MemberInfo info, ClassBuilder builder) {
    final FunctionElement member = info.member;
    String name = info.name;
    final FunctionSignature parameters = info.parameters;
    jsAst.Expression code = info.code;
    final bool needsStubs = info.needsStubs;
    final bool canTearOff = info.canTearOff;
    final bool isClosure = info.isClosure;
    final String tearOffName = info.tearOffName;
    final bool canBeReflected = info.canBeReflected;
    final bool canBeApplied = info.canBeApplied;
    final bool needStructuredInfo = info.needStructuredInfo;
    final bool hasSuperAlias = info.hasSuperAlias;

    emitter.interceptorEmitter.recordMangledNameOfMemberMethod(member, name);

    if (!needStructuredInfo) {
      compiler.dumpInfoTask.registerElementAst(member,
          builder.addProperty(name, code));
      if (needsStubs) {
        addParameterStubs(
            member,
            (Selector selector, jsAst.Fun function) {
              compiler.dumpInfoTask.registerElementAst(member,
                  builder.addProperty(namer.invocationName(selector),
                                      function));
            });
      }
      return;
    }
    emitter.needsStructuredMemberInfo = true;

    // This element is needed for reflection or needs additional stubs or has a
    // super alias. So we need to retain additional information.

    // The information is stored in an array with this format:
    //
    // 1.   The alias name for this function (optional).
    // 2.   The JS function for this member.
    // 3.   First stub.
    // 4.   Name of first stub.
    // ...
    // M.   Call name of this member.
    // M+1. Call name of first stub.
    // ...
    // N.   Getter name for tearOff.
    // N+1. (Required parameter count << 1) + (member.isAccessor ? 1 : 0).
    // N+2. (Optional parameter count << 1) +
    //                      (parameters.optionalParametersAreNamed ? 1 : 0).
    // N+3. Index to function type in constant pool.
    // N+4. First default argument.
    // ...
    // O.   First parameter name (if needed for reflection or Function.apply).
    // ...
    // P.   Unmangled name (if reflectable).
    // P+1. First metadata (if reflectable).
    // ...
    // TODO(ahe): Consider one of the parameter counts can be replaced by the
    // length property of the JavaScript function object.

    List<jsAst.Expression> expressions = <jsAst.Expression>[];

    // Create the optional aliasing entry if this method is called via super.
    if (backend.isAliasedSuperMember(member)) {
      expressions.add(new jsAst.LiteralString(
          '"${namer.getNameOfAliasedSuperMember(member)}"'));
    }

    expressions.add(code);

    final bool onlyNeedsSuperAlias =
        !(canTearOff || canBeReflected || canBeApplied || needsStubs);

    if (onlyNeedsSuperAlias) {
      jsAst.ArrayInitializer arrayInit =
            new jsAst.ArrayInitializer(expressions);
          compiler.dumpInfoTask.registerElementAst(member,
              builder.addProperty(name, arrayInit));
      return;
    }

    String callSelectorString = 'null';
    if (member.isFunction) {
      Selector callSelector = new Selector.fromElement(member).toCallSelector();
      callSelectorString = '"${namer.invocationName(callSelector)}"';
    }

    // On [requiredParameterCount], the lower bit is set if this method can be
    // called reflectively.
    int requiredParameterCount = parameters.requiredParameterCount << 1;
    if (member.isAccessor) requiredParameterCount++;

    int optionalParameterCount = parameters.optionalParameterCount << 1;
    if (parameters.optionalParametersAreNamed) optionalParameterCount++;

    // TODO(sra): Don't use LiteralString for non-strings.
    List tearOffInfo = [new jsAst.LiteralString(callSelectorString)];

    if (needsStubs || canTearOff) {
      addParameterStubs(member, (Selector selector, jsAst.Fun function) {
        expressions.add(function);
        if (member.isInstanceMember) {
          Set invokedSelectors =
              compiler.codegenWorld.invokedNames[member.name];
            expressions.add(js.string(namer.invocationName(selector)));
        } else {
          expressions.add(js('null'));
          // TOOD(ahe): Since we know when reading static data versus instance
          // data, we can eliminate this element.
        }
        Set<Selector> callSelectors = compiler.codegenWorld.invokedNames[
            namer.closureInvocationSelectorName];
        Selector callSelector = selector.toCallSelector();
        String callSelectorString = 'null';
        if (canTearOff && callSelectors != null &&
            callSelectors.contains(callSelector)) {
          callSelectorString = '"${namer.invocationName(callSelector)}"';
        }
        tearOffInfo.add(new jsAst.LiteralString(callSelectorString));
      }, canTearOff);
    }

    jsAst.Expression memberTypeExpression;
    if (canTearOff || canBeReflected) {
      DartType memberType;
      if (member.isGenerativeConstructorBody) {
        var body = member;
        memberType = body.constructor.type;
      } else {
        memberType = member.type;
      }
      if (memberType.containsTypeVariables) {
        jsAst.Expression thisAccess = js(r'this.$receiver');
        memberTypeExpression =
            backend.rti.getSignatureEncoding(memberType, thisAccess);
      } else {
        memberTypeExpression =
            js.number(emitter.metadataEmitter.reifyType(memberType));
      }
    } else {
      memberTypeExpression = js('null');
    }

    expressions
        ..addAll(tearOffInfo)
        ..add((tearOffName == null || member.isAccessor)
              ? js("null") : js.string(tearOffName))
        ..add(js.number(requiredParameterCount))
        ..add(js.number(optionalParameterCount))
        ..add(memberTypeExpression)
        ..addAll(emitter.metadataEmitter
            .reifyDefaultArguments(member).map(js.number));

    if (canBeReflected || canBeApplied) {
      parameters.forEachParameter((Element parameter) {
        expressions.add(
            js.number(emitter.metadataEmitter.reifyName(parameter.name)));
        if (backend.mustRetainMetadata) {
          Iterable<int> metadataIndices =
              parameter.metadata.map((MetadataAnnotation annotation) {
            ConstantValue constant =
                backend.constants.getConstantForMetadata(annotation).value;
            backend.constants.addCompileTimeConstantForEmission(constant);
            return emitter.metadataEmitter.reifyMetadata(annotation);
          });
          expressions.add(new jsAst.ArrayInitializer(
              metadataIndices.map(js.number).toList()));
        }
      });
    }
    if (canBeReflected) {
      jsAst.LiteralString reflectionName;
      if (member.isConstructor) {
        String reflectionNameString = emitter.getReflectionName(member, name);
        reflectionName =
            new jsAst.LiteralString(
                '"new ${Elements.reconstructConstructorName(member)}"');
      } else {
        reflectionName =
            js.string(namer.privateName(member.library, member.name));
      }
      expressions
          ..add(reflectionName)
          ..addAll(emitter.metadataEmitter
              .computeMetadata(member).map(js.number));
    } else if (isClosure && canBeApplied) {
      expressions.add(js.string(namer.privateName(member.library,
                                                  member.name)));
    }

    jsAst.ArrayInitializer arrayInit =
      new jsAst.ArrayInitializer(expressions.toList());
    compiler.dumpInfoTask.registerElementAst(member,
        builder.addProperty(name, arrayInit));
  }

  void addMemberField(VariableElement member, ClassBuilder builder) {
    // For now, do nothing.
  }
}

class MemberInfo {
  final FunctionElement member;

  final String name;

  final FunctionSignature parameters;

  final jsAst.Expression code;

  final bool needsStubs;

  final bool canTearOff;

  final bool isClosure;

  final String tearOffName;

  final bool canBeReflected;

  final bool canBeApplied;

  final bool needStructuredInfo;

  final bool hasSuperAlias;

  MemberInfo(
      this.member,
      this.name,
      this.parameters,
      this.code,
      {this.needsStubs,
       this.canTearOff,
       this.isClosure,
       this.tearOffName,
       this.canBeReflected,
       this.canBeApplied,
       this.hasSuperAlias,
       this.needStructuredInfo}) {
    assert(member != null);
    assert(name != null);
    assert(parameters != null);
    assert(code != null);
    assert(needsStubs != null);
    assert(canTearOff != null);
    assert(isClosure != null);
    assert(tearOffName != null || !canTearOff);
    assert(canBeReflected != null);
    assert(canBeApplied != null);
    assert(hasSuperAlias != null);
    assert(needStructuredInfo != null);
  }
}
