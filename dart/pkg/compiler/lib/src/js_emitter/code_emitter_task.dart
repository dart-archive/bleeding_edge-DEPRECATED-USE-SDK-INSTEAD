// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dart2js.js_emitter;

const USE_NEW_EMITTER = const bool.fromEnvironment("dart2js.use.new.emitter");

/**
 * Generates the code for all used classes in the program. Static fields (even
 * in classes) are ignored, since they can be treated as non-class elements.
 *
 * The code for the containing (used) methods must exist in the [:universe:].
 */
class CodeEmitterTask extends CompilerTask {
  // TODO(floitsch): the code-emitter task should not need a namer.
  final Namer namer;
  final TypeTestRegistry typeTestRegistry;
  NativeEmitter nativeEmitter;
  MetadataCollector metadataCollector;
  OldEmitter oldEmitter;
  Emitter emitter;

  final Set<ClassElement> neededClasses = new Set<ClassElement>();
  final Map<OutputUnit, List<ClassElement>> outputClassLists =
      new Map<OutputUnit, List<ClassElement>>();
  final Map<OutputUnit, List<ConstantValue>> outputConstantLists =
      new Map<OutputUnit, List<ConstantValue>>();
  final Map<OutputUnit, List<Element>> outputStaticLists =
      new Map<OutputUnit, List<Element>>();
  final Map<OutputUnit, List<VariableElement>> outputStaticNonFinalFieldLists =
      new Map<OutputUnit, List<VariableElement>>();
  final Map<OutputUnit, Set<LibraryElement>> outputLibraryLists =
      new Map<OutputUnit, Set<LibraryElement>>();

  /// True, if the output contains a constant list.
  ///
  /// This flag is updated in [computeNeededConstants].
  bool outputContainsConstantList = false;

  final List<ClassElement> nativeClassesAndSubclasses = <ClassElement>[];

  /// Records if a type variable is read dynamically for type tests.
  final Set<TypeVariableElement> readTypeVariables =
      new Set<TypeVariableElement>();

  List<TypedefElement> typedefsNeededForReflection;

  JavaScriptBackend get backend => compiler.backend;

  CodeEmitterTask(Compiler compiler, Namer namer, bool generateSourceMap)
      : super(compiler),
        this.namer = namer,
        this.typeTestRegistry = new TypeTestRegistry(compiler) {
    nativeEmitter = new NativeEmitter(this);
    oldEmitter = new OldEmitter(compiler, namer, generateSourceMap, this);
    emitter = USE_NEW_EMITTER
        ? new new_js_emitter.Emitter(compiler, namer, nativeEmitter)
        : oldEmitter;
    metadataCollector = new MetadataCollector(compiler, emitter);
  }

  String get name => 'Code emitter';

  /// Returns the closure expression of a static function.
  jsAst.Expression isolateStaticClosureAccess(FunctionElement element) {
    return emitter.isolateStaticClosureAccess(element);
  }

  /// Returns the JS function that must be invoked to get the value of the
  /// lazily initialized static.
  jsAst.Expression isolateLazyInitializerAccess(FieldElement element) {
    return emitter.isolateLazyInitializerAccess(element);
  }

  /// Returns the JS code for accessing the embedded [global].
  jsAst.Expression generateEmbeddedGlobalAccess(String global) {
    return emitter.generateEmbeddedGlobalAccess(global);
  }

  /// Returns the JS code for accessing the given [constant].
  jsAst.Expression constantReference(ConstantValue constant) {
    return emitter.constantReference(constant);
  }

  jsAst.Expression staticFieldAccess(FieldElement e) {
    return emitter.staticFieldAccess(e);
  }

  /// Returns the JS function representing the given function.
  ///
  /// The function must be invoked and can not be used as closure.
  jsAst.Expression staticFunctionAccess(FunctionElement e) {
    return emitter.staticFunctionAccess(e);
  }

  /// Returns the JS constructor of the given element.
  ///
  /// The returned expression must only be used in a JS `new` expression.
  jsAst.Expression constructorAccess(ClassElement e) {
    return emitter.constructorAccess(e);
  }

  /// Returns the JS prototype of the given class [e].
  jsAst.Expression prototypeAccess(ClassElement e,
                                   {bool hasBeenInstantiated: false}) {
    return emitter.prototypeAccess(e, hasBeenInstantiated);
  }

  /// Returns the JS prototype of the given interceptor class [e].
  jsAst.Expression interceptorPrototypeAccess(ClassElement e) {
    return jsAst.js('#.prototype', interceptorClassAccess(e));
  }

  /// Returns the JS constructor of the given interceptor class [e].
  jsAst.Expression interceptorClassAccess(ClassElement e) {
    return emitter.interceptorClassAccess(e);
  }

  /// Returns the JS expression representing the type [e].
  ///
  /// The given type [e] might be a Typedef.
  jsAst.Expression typeAccess(Element e) {
    return emitter.typeAccess(e);
  }

  void registerReadTypeVariable(TypeVariableElement element) {
    readTypeVariables.add(element);
  }

  Set<ClassElement> computeInterceptorsReferencedFromConstants() {
    Set<ClassElement> classes = new Set<ClassElement>();
    JavaScriptConstantCompiler handler = backend.constants;
    List<ConstantValue> constants = handler.getConstantsForEmission();
    for (ConstantValue constant in constants) {
      if (constant is InterceptorConstantValue) {
        InterceptorConstantValue interceptorConstant = constant;
        classes.add(interceptorConstant.dispatchedType.element);
      }
    }
    return classes;
  }

  /**
   * Return a function that returns true if its argument is a class
   * that needs to be emitted.
   */
  Function computeClassFilter() {
    if (backend.isTreeShakingDisabled) return (ClassElement cls) => true;

    Set<ClassElement> unneededClasses = new Set<ClassElement>();
    // The [Bool] class is not marked as abstract, but has a factory
    // constructor that always throws. We never need to emit it.
    unneededClasses.add(compiler.boolClass);

    // Go over specialized interceptors and then constants to know which
    // interceptors are needed.
    Set<ClassElement> needed = new Set<ClassElement>();
    backend.specializedGetInterceptors.forEach(
        (_, Iterable<ClassElement> elements) {
          needed.addAll(elements);
        }
    );

    // Add interceptors referenced by constants.
    needed.addAll(computeInterceptorsReferencedFromConstants());

    // Add unneeded interceptors to the [unneededClasses] set.
    for (ClassElement interceptor in backend.interceptedClasses) {
      if (!needed.contains(interceptor)
          && interceptor != compiler.objectClass) {
        unneededClasses.add(interceptor);
      }
    }

    // These classes are just helpers for the backend's type system.
    unneededClasses.add(backend.jsMutableArrayClass);
    unneededClasses.add(backend.jsFixedArrayClass);
    unneededClasses.add(backend.jsExtendableArrayClass);
    unneededClasses.add(backend.jsUInt32Class);
    unneededClasses.add(backend.jsUInt31Class);
    unneededClasses.add(backend.jsPositiveIntClass);

    return (ClassElement cls) => !unneededClasses.contains(cls);
  }

  /**
   * Compute all the constants that must be emitted.
   */
  void computeNeededConstants() {
    // Make sure we retain all metadata of all elements. This could add new
    // constants to the handler.
    if (backend.mustRetainMetadata) {
      // TODO(floitsch): verify that we don't run through the same elements
      // multiple times.
      for (Element element in backend.generatedCode.keys) {
        if (backend.isAccessibleByReflection(element)) {
          bool shouldRetainMetadata = backend.retainMetadataOf(element);
          if (shouldRetainMetadata && element.isFunction) {
            FunctionElement function = element;
            function.functionSignature.forEachParameter(
                backend.retainMetadataOf);
          }
        }
      }
      for (ClassElement cls in neededClasses) {
        final onlyForRti = typeTestRegistry.rtiNeededClasses.contains(cls);
        if (!onlyForRti) {
          backend.retainMetadataOf(cls);
          oldEmitter.classEmitter.visitFields(cls, false,
              (Element member,
               String name,
               String accessorName,
               bool needsGetter,
               bool needsSetter,
               bool needsCheckedSetter) {
            bool needsAccessor = needsGetter || needsSetter;
            if (needsAccessor && backend.isAccessibleByReflection(member)) {
              backend.retainMetadataOf(member);
            }
          });
        }
      }
      typedefsNeededForReflection.forEach(backend.retainMetadataOf);
    }

    JavaScriptConstantCompiler handler = backend.constants;
    List<ConstantValue> constants = handler.getConstantsForEmission(
        compiler.hasIncrementalSupport ? null : emitter.compareConstants);
    for (ConstantValue constant in constants) {
      if (emitter.isConstantInlinedOrAlreadyEmitted(constant)) continue;

      if (constant.isList) outputContainsConstantList = true;

      OutputUnit constantUnit =
          compiler.deferredLoadTask.outputUnitForConstant(constant);
      if (constantUnit == null) {
        // The back-end introduces some constants, like "InterceptorConstant" or
        // some list constants. They are emitted in the main output-unit.
        // TODO(sigurdm): We should track those constants.
        constantUnit = compiler.deferredLoadTask.mainOutputUnit;
      }
      outputConstantLists.putIfAbsent(
          constantUnit, () => new List<ConstantValue>()).add(constant);
    }
  }

  /// Compute all the classes and typedefs that must be emitted.
  void computeNeededDeclarations() {
    // Compute needed typedefs.
    typedefsNeededForReflection = Elements.sortedByPosition(
        compiler.world.allTypedefs
            .where(backend.isAccessibleByReflection)
            .toList());

    // Compute needed classes.
    Set<ClassElement> instantiatedClasses =
        compiler.codegenWorld.directlyInstantiatedClasses
            .where(computeClassFilter()).toSet();

    void addClassWithSuperclasses(ClassElement cls) {
      neededClasses.add(cls);
      for (ClassElement superclass = cls.superclass;
          superclass != null;
          superclass = superclass.superclass) {
        neededClasses.add(superclass);
      }
    }

    void addClassesWithSuperclasses(Iterable<ClassElement> classes) {
      for (ClassElement cls in classes) {
        addClassWithSuperclasses(cls);
      }
    }

    // 1. We need to generate all classes that are instantiated.
    addClassesWithSuperclasses(instantiatedClasses);

    // 2. Add all classes used as mixins.
    Set<ClassElement> mixinClasses = neededClasses
        .where((ClassElement element) => element.isMixinApplication)
        .map(computeMixinClass)
        .toSet();
    neededClasses.addAll(mixinClasses);

    // 3. Find all classes needed for rti.
    // It is important that this is the penultimate step, at this point,
    // neededClasses must only contain classes that have been resolved and
    // codegen'd. The rtiNeededClasses may contain additional classes, but
    // these are thought to not have been instantiated, so we neeed to be able
    // to identify them later and make sure we only emit "empty shells" without
    // fields, etc.
    typeTestRegistry.computeRtiNeededClasses();

    // TODO(floitsch): either change the name, or get the rti-classes
    // differently.
    typeTestRegistry.rtiNeededClasses.removeAll(neededClasses);
    // rtiNeededClasses now contains only the "empty shells".
    neededClasses.addAll(typeTestRegistry.rtiNeededClasses);

    // TODO(18175, floitsch): remove once issue 18175 is fixed.
    if (neededClasses.contains(backend.jsIntClass)) {
      neededClasses.add(compiler.intClass);
    }
    if (neededClasses.contains(backend.jsDoubleClass)) {
      neededClasses.add(compiler.doubleClass);
    }
    if (neededClasses.contains(backend.jsNumberClass)) {
      neededClasses.add(compiler.numClass);
    }
    if (neededClasses.contains(backend.jsStringClass)) {
      neededClasses.add(compiler.stringClass);
    }
    if (neededClasses.contains(backend.jsBoolClass)) {
      neededClasses.add(compiler.boolClass);
    }
    if (neededClasses.contains(backend.jsArrayClass)) {
      neededClasses.add(compiler.listClass);
    }

    // 4. Finally, sort the classes.
    List<ClassElement> sortedClasses = Elements.sortedByPosition(neededClasses);

    for (ClassElement element in sortedClasses) {
      if (Elements.isNativeOrExtendsNative(element) &&
          !typeTestRegistry.rtiNeededClasses.contains(element)) {
        // For now, native classes and related classes cannot be deferred.
        nativeClassesAndSubclasses.add(element);
        assert(invariant(element,
                         !compiler.deferredLoadTask.isDeferred(element)));
        outputClassLists.putIfAbsent(compiler.deferredLoadTask.mainOutputUnit,
            () => new List<ClassElement>()).add(element);
      } else {
        outputClassLists.putIfAbsent(
            compiler.deferredLoadTask.outputUnitForElement(element),
            () => new List<ClassElement>())
            .add(element);
      }
    }
  }

  void computeNeededStatics() {
    bool isStaticFunction(Element element) =>
        !element.isInstanceMember && !element.isField;

    Iterable<Element> elements =
        backend.generatedCode.keys.where(isStaticFunction);

    for (Element element in Elements.sortedByPosition(elements)) {
      List<Element> list = outputStaticLists.putIfAbsent(
          compiler.deferredLoadTask.outputUnitForElement(element),
          () => new List<Element>());
      list.add(element);
    }
  }

  void computeNeededStaticNonFinalFields() {
    JavaScriptConstantCompiler handler = backend.constants;
    Iterable<VariableElement> staticNonFinalFields =
        handler.getStaticNonFinalFieldsForEmission();
    for (Element element in Elements.sortedByPosition(staticNonFinalFields)) {
      List<VariableElement> list = outputStaticNonFinalFieldLists.putIfAbsent(
            compiler.deferredLoadTask.outputUnitForElement(element),
            () => new List<VariableElement>());
      list.add(element);
    }
  }

  void computeNeededLibraries() {
    void addSurroundingLibraryToSet(Element element) {
      OutputUnit unit = compiler.deferredLoadTask.outputUnitForElement(element);
      LibraryElement library = element.library;
      outputLibraryLists.putIfAbsent(unit, () => new Set<LibraryElement>())
          .add(library);
    }

    backend.generatedCode.keys.forEach(addSurroundingLibraryToSet);
    neededClasses.forEach(addSurroundingLibraryToSet);
  }

  void computeAllNeededEntities() {
    // Compute the required type checks to know which classes need a
    // 'is$' method.
    typeTestRegistry.computeRequiredTypeChecks();

    computeNeededDeclarations();
    computeNeededConstants();
    computeNeededStatics();
    computeNeededStaticNonFinalFields();
    computeNeededLibraries();
  }

  int assembleProgram() {
    return measure(() {
      emitter.invalidateCaches();

      computeAllNeededEntities();

      ProgramBuilder programBuilder = new ProgramBuilder(compiler, namer, this);
      return emitter.emitProgram(programBuilder);
    });
  }
}

abstract class Emitter {
  /// Uses the [programBuilder] to generate a model of the program, emits
  /// the program, and returns the size of the generated output.
  int emitProgram(ProgramBuilder programBuilder);

  /// Returns the JS function that must be invoked to get the value of the
  /// lazily initialized static.
  jsAst.Expression isolateLazyInitializerAccess(FieldElement element);

  /// Returns the closure expression of a static function.
  jsAst.Expression isolateStaticClosureAccess(FunctionElement element);

  /// Returns the JS code for accessing the embedded [global].
  jsAst.Expression generateEmbeddedGlobalAccess(String global);

  /// Returns the JS code for accessing the given [constant].
  jsAst.Expression constantReference(ConstantValue constant);

  /// Returns the JS function representing the given function.
  ///
  /// The function must be invoked and can not be used as closure.
  jsAst.Expression staticFunctionAccess(FunctionElement element);

  jsAst.Expression staticFieldAccess(FieldElement element);

  /// Returns the JS constructor of the given element.
  ///
  /// The returned expression must only be used in a JS `new` expression.
  jsAst.Expression constructorAccess(ClassElement e);

  /// Returns the JS prototype of the given class [e].
  jsAst.Expression prototypeAccess(ClassElement e, bool hasBeenInstantiated);

  /// Returns the JS constructor of the given interceptor class [e].
  jsAst.Expression interceptorClassAccess(ClassElement e);

  /// Returns the JS expression representing the type [e].
  jsAst.Expression typeAccess(Element e);

  /// Returns the JS expression representing a function that returns 'null'
  jsAst.Expression generateFunctionThatReturnsNull();

  int compareConstants(ConstantValue a, ConstantValue b);
  bool isConstantInlinedOrAlreadyEmitted(ConstantValue constant);

  void invalidateCaches();
}
