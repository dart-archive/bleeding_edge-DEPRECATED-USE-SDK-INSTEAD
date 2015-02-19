// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dart_backend;

// TODO(ahe): This class is simply wrong.  This backend should use
// elements when it can, not AST nodes.  Perhaps a [Map<Element,
// TreeElements>] is what is needed.
class ElementAst {
  final Node ast;
  final TreeElements treeElements;

  ElementAst(this.ast, this.treeElements);
}

class DartBackend extends Backend {
  final List<CompilerTask> tasks;
  final bool stripAsserts;

  // TODO(zarah) Maybe change this to a command-line option.
  // Right now, it is set by the tests.
  bool useMirrorHelperLibrary = false;

  /// Updated to a [MirrorRenamerImpl] instance if the [useMirrorHelperLibrary]
  /// field is set and mirror are needed.
  MirrorRenamer mirrorRenamer = const MirrorRenamer();

  final DartOutputter outputter;

  // Used in test.
  PlaceholderRenamer get placeholderRenamer => outputter.renamer;
  Map<ClassNode, List<Node>> get memberNodes => outputter.output.memberNodes;

  ConstantSystem get constantSystem {
    return constantCompilerTask.constantCompiler.constantSystem;
  }

  BackendConstantEnvironment get constants => constantCompilerTask;

  DartConstantTask constantCompilerTask;

  DartResolutionCallbacks resolutionCallbacks;

  final Set<ClassElement> usedTypeLiterals = new Set<ClassElement>();

  /// The set of visible platform classes that are implemented by instantiated
  /// user classes.
  final Set<ClassElement> _userImplementedPlatformClasses =
      new Set<ClassElement>();

  bool get canHandleCompilationFailed => false;

  /**
   * Tells whether it is safe to remove type declarations from variables,
   * functions parameters. It becomes not safe if:
   * 1) TypeError is used somewhere in the code,
   * 2) The code has typedefs in right hand side of IS checks,
   * 3) The code has classes which extend typedefs, have type arguments typedefs
   *    or type variable bounds typedefs.
   * These restrictions can be less strict.
   */
  bool isSafeToRemoveTypeDeclarations(
      Map<ClassElement, Iterable<Element>> classMembers) {
    ClassElement typeErrorElement = compiler.coreLibrary.find('TypeError');
    if (classMembers.containsKey(typeErrorElement) ||
        compiler.resolverWorld.isChecks.any(
            (DartType type) => type.element == typeErrorElement)) {
      return false;
    }
    Set<DartType> processedTypes = new Set<DartType>();
    List<DartType> workQueue = new List<DartType>();
    workQueue.addAll(
        classMembers.keys.map((classElement) => classElement.thisType));
    workQueue.addAll(compiler.resolverWorld.isChecks);

    while (!workQueue.isEmpty) {
      DartType type = workQueue.removeLast();
      if (processedTypes.contains(type)) continue;
      processedTypes.add(type);
      if (type is FunctionType) return false;
      if (type is TypedefType) return false;
      if (type is InterfaceType) {
        InterfaceType interfaceType = type;
        // Check all type arguments.
        interfaceType.typeArguments.forEach(workQueue.add);
        ClassElement element = type.element;
        // Check all supertypes.
        if (element.allSupertypes != null) {
          element.allSupertypes.forEach(workQueue.add);
        }
      }
    }
    return true;
  }

  DartBackend(Compiler compiler, List<String> strips, {bool multiFile})
      : tasks = <CompilerTask>[],
        stripAsserts = strips.indexOf('asserts') != -1,
        constantCompilerTask = new DartConstantTask(compiler),
        outputter = new DartOutputter(
            compiler, compiler.outputProvider,
            forceStripTypes: strips.indexOf('types') != -1,
            multiFile: multiFile,
            enableMinification: compiler.enableMinification),
        super(compiler) {
    resolutionCallbacks = new DartResolutionCallbacks(this);
  }

  bool classNeedsRti(ClassElement cls) => false;
  bool methodNeedsRti(FunctionElement function) => false;

  void enqueueHelpers(ResolutionEnqueuer world, Registry registry) {
    // Right now resolver doesn't always resolve interfaces needed
    // for literals, so force them. TODO(antonm): fix in the resolver.
    final LITERAL_TYPE_NAMES = const [
      'Map', 'List', 'num', 'int', 'double', 'bool'
    ];
    final coreLibrary = compiler.coreLibrary;
    for (final name in LITERAL_TYPE_NAMES) {
      ClassElement classElement = coreLibrary.findLocal(name);
      classElement.ensureResolved(compiler);
    }
    // Enqueue the methods that the VM might invoke on user objects because
    // we don't trust the resolution to always get these included.
    world.registerInvocation(new Selector.call("toString", null, 0));
    world.registerInvokedGetter(new Selector.getter("hashCode", null));
    world.registerInvocation(new Selector.binaryOperator("=="));
    world.registerInvocation(new Selector.call("compareTo", null, 1));
  }

  void codegen(CodegenWorkItem work) { }

  /// Create an [ElementAst] from the CPS IR.
  static ElementAst createElementAst(
       ElementAstCreationContext context,
       Element element,
       cps_ir.ExecutableDefinition cpsDefinition) {
    // Transformations on the CPS IR.
    context.traceCompilation(element.name);

    // TODO(karlklose): enable type propagation for dart2dart when constant
    // types are correctly marked as instantiated (Issue 21880).
    new TypePropagator(context.dartTypes,
                       context.constantSystem,
                       new UnitTypeSystem(),
                       context.internalError)
        .rewrite(cpsDefinition);
    context.traceGraph("Sparse constant propagation", cpsDefinition);
    new RedundantPhiEliminator().rewrite(cpsDefinition);
    context.traceGraph("Redundant phi elimination", cpsDefinition);
    new ShrinkingReducer().rewrite(cpsDefinition);
    context.traceGraph("Shrinking reductions", cpsDefinition);

    // Do not rewrite the IR after variable allocation.  Allocation
    // makes decisions based on an approximation of IR variable live
    // ranges that can be invalidated by transforming the IR.
    new cps_ir.RegisterAllocator().visit(cpsDefinition);

    tree_builder.Builder builder =
        new tree_builder.Builder(context.internalError);
    tree_ir.ExecutableDefinition treeDefinition = builder.build(cpsDefinition);
    assert(treeDefinition != null);
    context.traceGraph('Tree builder', treeDefinition);

    // Transformations on the Tree IR.
    new StatementRewriter().rewrite(treeDefinition);
    context.traceGraph('Statement rewriter', treeDefinition);
    new CopyPropagator().rewrite(treeDefinition);
    context.traceGraph('Copy propagation', treeDefinition);
    new LoopRewriter().rewrite(treeDefinition);
    context.traceGraph('Loop rewriter', treeDefinition);
    new LogicalRewriter().rewrite(treeDefinition);
    context.traceGraph('Logical rewriter', treeDefinition);
    new backend_ast_emitter.UnshadowParameters().unshadow(treeDefinition);
    context.traceGraph('Unshadow parameters', treeDefinition);

    TreeElementMapping treeElements = new TreeElementMapping(element);
    backend_ast.ExecutableDefinition backendAst =
        backend_ast_emitter.emit(treeDefinition);
    Node frontend_ast = backend2frontend.emit(treeElements, backendAst);
    return new ElementAst(frontend_ast, treeElements);

  }

  /**
   * Tells whether we should output given element. Corelib classes like
   * Object should not be in the resulting code.
   */
  @override
  bool shouldOutput(Element element) {
    return (!element.library.isPlatformLibrary &&
            !element.isSynthesized &&
            element is! AbstractFieldElement)
        || mirrorRenamer.isMirrorHelperLibrary(element.library);
  }

  int assembleProgram() {
    ElementAstCreationContext context =
        new _ElementAstCreationContext(compiler, constantSystem);

    ElementAst computeElementAst(AstElement element) {
      if (!compiler.irBuilder.hasIr(element)) {
        return new ElementAst(element.resolvedAst.node,
                              element.resolvedAst.elements);
      } else {
        cps_ir.ExecutableDefinition definition =
            compiler.irBuilder.getIr(element);
        return createElementAst(context, element, definition);
      }
    }

    // TODO(johnniwinther): Remove the need for this method.
    void postProcessElementAst(
        AstElement element, ElementAst elementAst,
        newTypedefElementCallback,
        newClassElementCallback) {
      ReferencedElementCollector collector =
          new ReferencedElementCollector(compiler,
                                         element,
                                         elementAst,
                                         newTypedefElementCallback,
                                         newClassElementCallback);
      collector.collect();
    }

    int totalSize = outputter.assembleProgram(
        libraries: compiler.libraryLoader.libraries,
        instantiatedClasses: compiler.resolverWorld.directlyInstantiatedClasses,
        resolvedElements: compiler.enqueuer.resolution.resolvedElements,
        usedTypeLiterals: usedTypeLiterals,
        postProcessElementAst: postProcessElementAst,
        computeElementAst: computeElementAst,
        shouldOutput: shouldOutput,
        isSafeToRemoveTypeDeclarations: isSafeToRemoveTypeDeclarations,
        sortElements: Elements.sortedByPosition,
        mirrorRenamer: mirrorRenamer,
        mainFunction: compiler.mainFunction,
        outputUri: compiler.outputUri);

    // Output verbose info about size ratio of resulting bundle to all
    // referenced non-platform sources.
    logResultBundleSizeInfo(
        outputter.libraryInfo.userLibraries,
        outputter.elementInfo.topLevelElements,
        totalSize);

    return totalSize;
  }

  void logResultBundleSizeInfo(Iterable<LibraryElement> userLibraries,
                               Iterable<Element> topLevelElements,
                               int totalOutputSize) {
    // Sum total size of scripts in each referenced library.
    int nonPlatformSize = 0;
    for (LibraryElement lib in userLibraries) {
      for (CompilationUnitElement compilationUnit in lib.compilationUnits) {
        nonPlatformSize += compilationUnit.script.file.length;
      }
    }
    int percentage = totalOutputSize * 100 ~/ nonPlatformSize;
    log('Total used non-platform files size: ${nonPlatformSize} bytes, '
        'Output total size: $totalOutputSize bytes (${percentage}%)');
  }

  log(String message) => compiler.log('[DartBackend] $message');

  @override
  Future onLibrariesLoaded(LoadedLibraries loadedLibraries) {
    // All platform classes must be resolved to ensure that their member names
    // are preserved.
    loadedLibraries.forEachLibrary((LibraryElement library) {
      if (library.isPlatformLibrary) {
        library.forEachLocalMember((Element element) {
          if (element.isClass) {
            ClassElement classElement = element;
            classElement.ensureResolved(compiler);
          }
        });
      }
    });
    if (useMirrorHelperLibrary &&
        loadedLibraries.containsLibrary(Compiler.DART_MIRRORS)) {
      return compiler.libraryLoader.loadLibrary(
          compiler.translateResolvedUri(
              loadedLibraries.getLibrary(Compiler.DART_MIRRORS),
              MirrorRenamerImpl.DART_MIRROR_HELPER, null)).
          then((LibraryElement library) {
        mirrorRenamer = new MirrorRenamerImpl(compiler, this, library);
      });
    }
    return new Future.value();
  }

  @override
  void registerStaticUse(Element element, Enqueuer enqueuer) {
    if (element == compiler.mirrorSystemGetNameFunction) {
      FunctionElement getNameFunction = mirrorRenamer.getNameFunction;
      if (getNameFunction != null) {
        enqueuer.addToWorkList(getNameFunction);
      }
    }
  }

  @override
  void registerInstantiatedType(InterfaceType type, Registry registry) {
    // Without patching, dart2dart has no way of performing sound tree-shaking
    // in face external functions. Therefore we employ another scheme:
    //
    // Based on the assumption that the platform code only relies on the
    // interfaces of it's own classes, we can approximate the semantics of
    // external functions by eagerly registering dynamic invocation of instance
    // members defined the platform interfaces.
    //
    // Since we only need to generate code for non-platform classes we can
    // restrict this registration to platform interfaces implemented by
    // instantiated non-platform classes.
    //
    // Consider for instance this program:
    //
    //     import 'dart:math' show Random;
    //
    //     class MyRandom implements Random {
    //       int nextInt() => 0;
    //     }
    //
    //     main() {
    //       print([0, 1, 2].shuffle(new MyRandom()));
    //     }
    //
    // Here `MyRandom` is a subtype if `Random` defined in 'dart:math'. By the
    // assumption, all methods defined `Random` are potentially called, and
    // therefore, though there are no visible call sites from the user node,
    // dynamic invocation of for instance `nextInt` should be registered. In
    // this case, `nextInt` is actually called by the standard implementation of
    // `shuffle`.

    ClassElement cls = type.element;
    if (!cls.library.isPlatformLibrary) {
      for (Link<DartType> link = cls.allSupertypes;
           !link.isEmpty;
           link = link.tail) {
        InterfaceType supertype = link.head;
        ClassElement superclass = supertype.element;
        LibraryElement library = superclass.library;
        if (library.isPlatformLibrary) {
          if (_userImplementedPlatformClasses.add(superclass)) {
            // Register selectors for all instance methods since these might
            // be called on user classes from within the platform
            // implementation.
            superclass.forEachLocalMember((Element element) {
              if (element.isConstructor || element.isStatic) return;

              FunctionElement function = element.asFunctionElement();
              if (function != null) {
                function.computeSignature(compiler);
              }
              Selector selector = new Selector.fromElement(element);
              if (selector.isGetter) {
                registry.registerDynamicGetter(selector);
              } else if (selector.isSetter) {
                registry.registerDynamicSetter(selector);
              } else {
                registry.registerDynamicInvocation(selector);
              }
            });
          }
        }
      }
    }

  }
}

class DartResolutionCallbacks extends ResolutionCallbacks {
  final DartBackend backend;

  DartResolutionCallbacks(this.backend);

  void onTypeLiteral(DartType type, Registry registry) {
    if (type.isInterfaceType) {
      backend.usedTypeLiterals.add(type.element);
    }
  }
}

class EmitterUnparser extends Unparser {
  final Map<Node, String> renames;

  EmitterUnparser(this.renames, {bool minify, bool stripTypes})
      : super(minify: minify, stripTypes: stripTypes);

  visit(Node node) {
    if (node != null && renames.containsKey(node)) {
      write(renames[node]);
    } else {
      super.visit(node);
    }
  }

  unparseSendReceiver(Send node, {bool spacesNeeded: false}) {
    // TODO(smok): Remove ugly hack for library prefices.
    if (node.receiver != null && renames[node.receiver] == '') return;
    super.unparseSendReceiver(node, spacesNeeded: spacesNeeded);
  }

  unparseFunctionName(Node name) {
    if (name != null && renames.containsKey(name)) {
      write(renames[name]);
    } else {
      super.unparseFunctionName(name);
    }
  }
}


/**
 * Some elements are not recorded by resolver now,
 * for example, typedefs or classes which are only
 * used in signatures, as/is operators or in super clauses
 * (just to name a few).  Retraverse AST to pick those up.
 */
class ReferencedElementCollector extends Visitor {
  final Compiler compiler;
  final Element element;
  final ElementAst elementAst;
  final newTypedefElementCallback;
  final newClassElementCallback;

  ReferencedElementCollector(this.compiler,
                             this.element,
                             this.elementAst,
                             this.newTypedefElementCallback,
                             this.newClassElementCallback);

  visitNode(Node node) {
    node.visitChildren(this);
  }

  visitTypeAnnotation(TypeAnnotation typeAnnotation) {
    TreeElements treeElements = elementAst.treeElements;
    final DartType type = treeElements.getType(typeAnnotation);
    assert(invariant(typeAnnotation, type != null,
        message: "Missing type for type annotation: $treeElements."));
    if (type.isTypedef) newTypedefElementCallback(type.element);
    if (type.isInterfaceType) newClassElementCallback(type.element);
    typeAnnotation.visitChildren(this);
  }

  void collect() {
    compiler.withCurrentElement(element, () {
      elementAst.ast.accept(this);
    });
  }
}

Comparator compareBy(f) => (x, y) => f(x).compareTo(f(y));

List sorted(Iterable l, comparison) {
  final result = new List.from(l);
  result.sort(comparison);
  return result;
}

compareElements(e0, e1) {
  int result = compareBy((e) => e.library.canonicalUri.toString())(e0, e1);
  if (result != 0) return result;
  return compareBy((e) => e.position.charOffset)(e0, e1);
}

/// [ConstantCompilerTask] for compilation of constants for the Dart backend.
///
/// Since this task needs no distinction between frontend and backend constants
/// it also serves as the [BackendConstantEnvironment].
class DartConstantTask extends ConstantCompilerTask
    implements BackendConstantEnvironment {
  final DartConstantCompiler constantCompiler;

  DartConstantTask(Compiler compiler)
    : this.constantCompiler = new DartConstantCompiler(compiler),
      super(compiler);

  String get name => 'ConstantHandler';

  ConstantExpression getConstantForVariable(VariableElement element) {
    return constantCompiler.getConstantForVariable(element);
  }

  ConstantExpression getConstantForNode(Node node, TreeElements elements) {
    return constantCompiler.getConstantForNode(node, elements);
  }

  ConstantExpression getConstantForMetadata(MetadataAnnotation metadata) {
    return metadata.constant;
  }

  ConstantExpression compileConstant(VariableElement element) {
    return measure(() {
      return constantCompiler.compileConstant(element);
    });
  }

  void compileVariable(VariableElement element) {
    measure(() {
      constantCompiler.compileVariable(element);
    });
  }

  ConstantExpression compileNode(Node node, TreeElements elements) {
    return measure(() {
      return constantCompiler.compileNodeWithDefinitions(node, elements);
    });
  }

  ConstantExpression compileMetadata(MetadataAnnotation metadata,
                           Node node,
                           TreeElements elements) {
    return measure(() {
      return constantCompiler.compileMetadata(metadata, node, elements);
    });
  }
}

abstract class ElementAstCreationContext {
  DartTypes get dartTypes;
  ConstantSystem get constantSystem;
  InternalErrorFunction get internalError;

  void traceCompilation(String name);
  void traceGraph(String title, var irObject);
}

class _ElementAstCreationContext implements ElementAstCreationContext {
  final Compiler compiler;
  final ConstantSystem constantSystem;

  _ElementAstCreationContext(this.compiler, this.constantSystem);

  void traceCompilation(String name) {
    compiler.tracer.traceCompilation(name, null);
  }

  void traceGraph(String title, var irObject) {
    compiler.tracer.traceGraph(title, irObject);
  }

  DartTypes get dartTypes => compiler.types;

  InternalErrorFunction get internalError => compiler.internalError;
}
