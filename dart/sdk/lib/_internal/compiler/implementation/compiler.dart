// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dart2js;

/**
 * If true, print a warning for each method that was resolved, but not
 * compiled.
 */
const bool REPORT_EXCESS_RESOLUTION = false;

/**
 * If true, dump the inferred types after compilation.
 */
const bool DUMP_INFERRED_TYPES = false;

/**
 * Contains backend-specific data that is used throughout the compilation of
 * one work item.
 */
class ItemCompilationContext {
}

abstract class WorkItem {
  final ItemCompilationContext compilationContext;
  /**
   * Documentation wanted -- johnniwinther
   *
   * Invariant: [element] must be a declaration element.
   */
  final Element element;
  TreeElements resolutionTree;

  WorkItem(this.element, this.compilationContext) {
    assert(invariant(element, element.isDeclaration));
  }

  void run(Compiler compiler, Enqueuer world);
}

/// [WorkItem] used exclusively by the [ResolutionEnqueuer].
class ResolutionWorkItem extends WorkItem {
  ResolutionWorkItem(Element element,
                     ItemCompilationContext compilationContext)
      : super(element, compilationContext);

  void run(Compiler compiler, ResolutionEnqueuer world) {
    resolutionTree = compiler.analyze(this, world);
  }

  bool isAnalyzed() => resolutionTree != null;
}

/// [WorkItem] used exclusively by the [CodegenEnqueuer].
class CodegenWorkItem extends WorkItem {
  CodegenWorkItem(Element element,
                  ItemCompilationContext compilationContext)
      : super(element, compilationContext);

  void run(Compiler compiler, CodegenEnqueuer world) {
    if (world.isProcessed(element)) return;
    resolutionTree =
        compiler.enqueuer.resolution.getCachedElements(element);
    assert(invariant(element, resolutionTree != null,
        message: 'Resolution tree is null for $element in codegen work item'));
    compiler.codegen(this, world);
  }
}

typedef void DeferredAction();

class DeferredTask {
  final Element element;
  final DeferredAction action;

  DeferredTask(this.element, this.action);
}

abstract class Backend {
  final Compiler compiler;
  final ConstantSystem constantSystem;

  Backend(this.compiler,
          [ConstantSystem constantSystem = DART_CONSTANT_SYSTEM])
      : this.constantSystem = constantSystem;

  void initializeHelperClasses() {}

  void enqueueAllTopLevelFunctions(LibraryElement lib, Enqueuer world) {
    lib.forEachExport((Element e) {
      if (e.isFunction()) world.addToWorkList(e);
    });
  }

  void enqueueHelpers(ResolutionEnqueuer world, TreeElements elements);
  void codegen(CodegenWorkItem work);

  // The backend determines the native resolution enqueuer, with a no-op
  // default, so tools like dart2dart can ignore the native classes.
  native.NativeEnqueuer nativeResolutionEnqueuer(world) {
    return new native.NativeEnqueuer();
  }
  native.NativeEnqueuer nativeCodegenEnqueuer(world) {
    return new native.NativeEnqueuer();
  }

  void assembleProgram();
  List<CompilerTask> get tasks;

  void onResolutionComplete() {}

  // TODO(ahe,karlklose): rename this?
  void dumpInferredTypes() {}

  ItemCompilationContext createItemCompilationContext() {
    return new ItemCompilationContext();
  }

  bool classNeedsRti(ClassElement cls);
  bool methodNeedsRti(FunctionElement function);


  /// Called during codegen when [constant] has been used.
  void registerCompileTimeConstant(Constant constant, TreeElements elements) {}

  /// Called during post-processing when [constant] has been evaluated.
  void registerMetadataConstant(Constant constant, TreeElements elements) {}

  /// Called during resolution to notify to the backend that a class is
  /// being instantiated.
  void registerInstantiatedClass(ClassElement cls,
                                 Enqueuer enqueuer,
                                 TreeElements elements) {}

  /// Called during resolution to notify to the backend that the
  /// program uses string interpolation.
  void registerStringInterpolation(TreeElements elements) {}

  /// Called during resolution to notify to the backend that the
  /// program has a catch statement.
  void registerCatchStatement(Enqueuer enqueuer,
                              TreeElements elements) {}

  /// Called during resolution to notify to the backend that the
  /// program explicitly throws an exception.
  void registerThrowExpression(TreeElements elements) {}

  /// Called during resolution to notify to the backend that the
  /// program has a global variable with a lazy initializer.
  void registerLazyField(TreeElements elements) {}

  /// Called during resolution to notify to the backend that the
  /// program uses a type variable as an expression.
  void registerTypeVariableExpression(TreeElements elements) {}

  /// Called during resolution to notify to the backend that the
  /// program uses a type literal.
  void registerTypeLiteral(Element element,
                           Enqueuer enqueuer,
                           TreeElements elements) {}

  /// Called during resolution to notify to the backend that the
  /// program has a catch statement with a stack trace.
  void registerStackTraceInCatch(TreeElements elements) {}

  /// Register an is check to the backend.
  void registerIsCheck(DartType type,
                       Enqueuer enqueuer,
                       TreeElements elements) {}

  /// Register an as check to the backend.
  void registerAsCheck(DartType type,
                       Enqueuer enqueuer,
                       TreeElements elements) {}

  /// Register a runtime type variable bound tests between [typeArgument] and
  /// [bound].
  void registerTypeVariableBoundsSubtypeCheck(DartType typeArgument,
                                              DartType bound) {}

  /// Registers that a type variable bounds check might occur at runtime.
  void registerTypeVariableBoundCheck(TreeElements elements) {}

  /// Register that the application may throw a [NoSuchMethodError].
  void registerThrowNoSuchMethod(TreeElements elements) {}

  /// Register that the application may throw a [RuntimeError].
  void registerThrowRuntimeError(TreeElements elements) {}

  /// Register that the application may throw an
  /// [AbstractClassInstantiationError].
  void registerAbstractClassInstantiation(TreeElements elements) {}

  /// Register that the application may throw a [FallThroughError].
  void registerFallThroughError(TreeElements elements) {}

  /// Register that a super call will end up calling
  /// [: super.noSuchMethod :].
  void registerSuperNoSuchMethod(TreeElements elements) {}

  /// Register that the application creates a constant map.
  void registerConstantMap(TreeElements elements) {}

  /**
   * Call this to register that an instantiated generic class has a call
   * method.
   */
  void registerGenericCallMethod(Element callMethod,
                                 Enqueuer enqueuer,
                                 TreeElements elements) {}
  /**
   * Call this to register that a getter exists for a function on an
   * instantiated generic class.
   */
  void registerGenericClosure(Element closure,
                              Enqueuer enqueuer,
                              TreeElements elements) {}
  /**
   * Call this to register that the [:runtimeType:] property has been accessed.
   */
  void registerRuntimeType(Enqueuer enqueuer, TreeElements elements) {}

  /**
   * Call this method to enable [noSuchMethod] handling in the
   * backend.
   */
  void enableNoSuchMethod(Enqueuer enqueuer) {
    enqueuer.registerInvocation(compiler.noSuchMethodSelector);
  }

  void registerRequiredType(DartType type, Element enclosingElement) {}
  void registerClassUsingVariableExpression(ClassElement cls) {}

  void registerConstSymbol(String name, TreeElements elements) {}
  void registerNewSymbol(TreeElements elements) {}
  /// Called when resolving the `Symbol` constructor.
  void registerSymbolConstructor(TreeElements elements) {}

  bool isNullImplementation(ClassElement cls) {
    return cls == compiler.nullClass;
  }
  ClassElement get intImplementation => compiler.intClass;
  ClassElement get doubleImplementation => compiler.doubleClass;
  ClassElement get numImplementation => compiler.numClass;
  ClassElement get stringImplementation => compiler.stringClass;
  ClassElement get listImplementation => compiler.listClass;
  ClassElement get growableListImplementation => compiler.listClass;
  ClassElement get fixedListImplementation => compiler.listClass;
  ClassElement get constListImplementation => compiler.listClass;
  ClassElement get mapImplementation => compiler.mapClass;
  ClassElement get constMapImplementation => compiler.mapClass;
  ClassElement get functionImplementation => compiler.functionClass;
  ClassElement get typeImplementation => compiler.typeClass;
  ClassElement get boolImplementation => compiler.boolClass;
  ClassElement get nullImplementation => compiler.nullClass;

  ClassElement defaultSuperclass(ClassElement element) => compiler.objectClass;

  bool isDefaultNoSuchMethodImplementation(Element element) {
    assert(element.name == Compiler.NO_SUCH_METHOD);
    ClassElement classElement = element.getEnclosingClass();
    return classElement == compiler.objectClass;
  }

  void registerStaticUse(Element element, Enqueuer enqueuer) {}

  Future onLibraryLoaded(LibraryElement library, Uri uri) {
    return new Future.value();
  }

  /// Called by [MirrorUsageAnalyzerTask] after it has merged all @MirrorsUsed
  /// annotations. The arguments corresponds to the unions of the corresponding
  /// fields of the annotations.
  void registerMirrorUsage(Set<String> symbols,
                           Set<Element> targets,
                           Set<Element> metaTargets) {}

  /// Returns true if this element should be retained for reflection even if it
  /// would normally be tree-shaken away.
  bool isNeededForReflection(Element element) => false;

  /// Returns true if global optimizations such as type inferencing
  /// can apply to this element. One category of elements that do not
  /// apply is runtime helpers that the backend calls, but the
  /// optimizations don't see those calls.
  bool canBeUsedForGlobalOptimizations(Element element) => true;

  /// Called when [enqueuer]'s queue is empty, but before it is closed.
  /// This is used, for example, by the JS backend to enqueue additional
  /// elements needed for reflection.
  void onQueueEmpty(Enqueuer enqueuer) {}
}

/**
 * Key class used in [TokenMap] in which the hash code for a token is based
 * on the [charOffset].
 */
class TokenKey {
  final Token token;
  TokenKey(this.token);
  int get hashCode => token.charOffset;
  operator==(other) => other is TokenKey && token == other.token;
}

/// Map of tokens and the first associated comment.
/*
 * This implementation was chosen among several candidates for its space/time
 * efficiency by empirical tests of running dartdoc on dartdoc itself. Time
 * measurements for the use of [Compiler.commentMap]:
 *
 * 1) Using [TokenKey] as key (this class): ~80 msec
 * 2) Using [TokenKey] as key + storing a separate map in each script: ~120 msec
 * 3) Using [Token] as key in a [Map]: ~38000 msec
 * 4) Storing comments is new field in [Token]: ~20 msec
 *    (Abandoned due to the increased memory usage)
 * 5) Storing comments in an [Expando]: ~14000 msec
 * 6) Storing token/comments pairs in a linked list: ~5400 msec
 */
class TokenMap {
  Map<TokenKey,Token> comments = new Map<TokenKey,Token>();

  Token operator[] (Token key) {
    if (key == null) return null;
    return comments[new TokenKey(key)];
  }

  void operator[]= (Token key, Token value) {
    if (key == null) return;
    comments[new TokenKey(key)] = value;
  }
}

abstract class Compiler implements DiagnosticListener {
  final Map<String, LibraryElement> libraries =
    new Map<String, LibraryElement>();
  final Stopwatch totalCompileTime = new Stopwatch();
  int nextFreeClassId = 0;
  World world;
  String assembledCode;
  Types types;

  /**
   * Map from token to the first preceeding comment token.
   */
  final TokenMap commentMap = new TokenMap();

  /**
   * Records global dependencies, that is, dependencies that don't
   * correspond to a particular element.
   *
   * We should get rid of this and ensure that all dependencies are
   * associated with a particular element.
   */
  final TreeElements globalDependencies = new TreeElementMapping(null);

  final bool enableMinification;
  final bool enableTypeAssertions;
  final bool enableUserAssertions;
  final bool trustTypeAnnotations;
  final bool enableConcreteTypeInference;
  final bool disableTypeInferenceFlag;

  /**
   * The maximum size of a concrete type before it widens to dynamic during
   * concrete type inference.
   */
  final int maxConcreteTypeSize;
  final bool analyzeAllFlag;
  final bool analyzeOnly;
  /**
   * If true, skip analysis of method bodies and field initializers. Implies
   * [analyzeOnly].
   */
  final bool analyzeSignaturesOnly;
  final bool enableNativeLiveTypeAnalysis;

  /**
   * If [:true:], comment tokens are collected in [commentMap] during scanning.
   */
  final bool preserveComments;

  /**
   * Is the compiler in verbose mode.
   */
  final bool verbose;

  /**
   * URI of the main source map if the compiler is generating source
   * maps.
   */
  final Uri sourceMapUri;

  /**
   * The name to use for the global JS object in JS output.  Default
   * value is "$".
   */
  final String globalJsName;

  /// Emit terse diagnostics without howToFix.
  final bool terseDiagnostics;

  final api.CompilerOutputProvider outputProvider;

  bool disableInlining = false;

  List<Uri> librariesToAnalyzeWhenRun;

  final Tracer tracer;

  CompilerTask measuredTask;
  Element _currentElement;
  LibraryElement coreLibrary;
  LibraryElement isolateLibrary;
  LibraryElement isolateHelperLibrary;
  LibraryElement jsHelperLibrary;
  LibraryElement interceptorsLibrary;
  LibraryElement foreignLibrary;

  LibraryElement mainApp;
  FunctionElement mainFunction;

  /// Initialized when dart:mirrors is loaded.
  LibraryElement mirrorsLibrary;

  /// Initialized when dart:typed_data is loaded.
  LibraryElement typedDataLibrary;

  ClassElement objectClass;
  ClassElement closureClass;
  ClassElement boundClosureClass;
  ClassElement dynamicClass;
  ClassElement boolClass;
  ClassElement numClass;
  ClassElement intClass;
  ClassElement doubleClass;
  ClassElement stringClass;
  ClassElement functionClass;
  ClassElement nullClass;
  ClassElement listClass;
  ClassElement typeClass;
  ClassElement mapClass;
  ClassElement symbolClass;
  ClassElement stackTraceClass;
  ClassElement typedDataClass;

  // Initialized after symbolClass has been resolved.
  FunctionElement symbolConstructor;

  // Initialized when dart:mirrors is loaded.
  ClassElement mirrorSystemClass;

  // Initialized when dart:mirrors is loaded.
  ClassElement mirrorsUsedClass;

  // Initialized after mirrorSystemClass has been resolved.
  FunctionElement mirrorSystemGetNameFunction;

  // Initialized when dart:_collection-dev is loaded.
  ClassElement symbolImplementationClass;

  // Initialized when symbolImplementationClass has been resolved.
  FunctionElement symbolValidatedConstructor;

  // Initialized when mirrorsUsedClass has been resolved.
  FunctionElement mirrorsUsedConstructor;

  // Initialized when dart:mirrors is loaded.
  ClassElement deferredLibraryClass;

  ClassElement jsInvocationMirrorClass;
  /// Document class from dart:mirrors.
  ClassElement documentClass;
  Element assertMethod;
  Element identicalFunction;
  Element functionApplyMethod;
  Element invokeOnMethod;
  Element intEnvironment;
  Element boolEnvironment;
  Element stringEnvironment;

  fromEnvironment(String name) => null;

  Element get currentElement => _currentElement;

  String tryToString(object) {
    try {
      return object.toString();
    } catch (_) {
      return '<exception in toString()>';
    }
  }

  /**
   * Perform an operation, [f], returning the return value from [f].  If an
   * error occurs then report it as having occurred during compilation of
   * [element].  Can be nested.
   */
  withCurrentElement(Element element, f()) {
    Element old = currentElement;
    _currentElement = element;
    try {
      return f();
    } on SpannableAssertionFailure catch (ex, s) {
      if (!hasCrashed) {
        String message = (ex.message != null) ? tryToString(ex.message)
                                              : tryToString(ex);
        SourceSpan span = spanFromSpannable(ex.node);
        reportError(ex.node, MessageKind.GENERIC, {'text': message});
        pleaseReportCrash(s, 'The compiler crashed: $message.');
      }
      hasCrashed = true;
      throw new CompilerCancelledException('The compiler crashed.');
    } on CompilerCancelledException catch (ex) {
      rethrow;
    } on StackOverflowError catch (ex) {
      // We cannot report anything useful in this case, because we
      // do not have enough stack space.
      rethrow;
    } catch (ex, s) {
      if (hasCrashed) rethrow;
      String message = 'The compiler crashed: ${tryToString(ex)}.';
      try {
        unhandledExceptionOnElement(element, s, message);
      } catch (doubleFault) {
        // Ignoring exceptions in exception handling.
      }
      throw new CompilerCancelledException(message);
    } finally {
      _currentElement = old;
    }
  }

  List<CompilerTask> tasks;
  ScannerTask scanner;
  DietParserTask dietParser;
  ParserTask parser;
  PatchParserTask patchParser;
  LibraryLoader libraryLoader;
  TreeValidatorTask validator;
  ResolverTask resolver;
  closureMapping.ClosureTask closureToClassMapper;
  TypeCheckerTask checker;
  ti.TypesTask typesTask;
  Backend backend;
  ConstantHandler constantHandler;
  EnqueueTask enqueuer;
  DeferredLoadTask deferredLoadTask;
  MirrorUsageAnalyzerTask mirrorUsageAnalyzerTask;
  String buildId;

  static const String MAIN = 'main';
  static const String CALL_OPERATOR_NAME = 'call';
  static const String NO_SUCH_METHOD = 'noSuchMethod';
  static const int NO_SUCH_METHOD_ARG_COUNT = 1;
  static const String CREATE_INVOCATION_MIRROR =
      'createInvocationMirror';

  // TODO(ahe): Rename this field and move this logic to backend, similar to how
  // we disable tree-shaking when seeing disableTreeShaking in js_mirrors.dart.
  static const String INVOKE_ON =
      '_getCachedInvocation';

  static const String RUNTIME_TYPE = 'runtimeType';
  static const String START_ROOT_ISOLATE =
      'startRootIsolate';

  static const String UNDETERMINED_BUILD_ID =
      "build number could not be determined";

  final Selector iteratorSelector =
      new Selector.getter('iterator', null);
  final Selector currentSelector =
      new Selector.getter('current', null);
  final Selector moveNextSelector =
      new Selector.call('moveNext', null, 0);
  final Selector noSuchMethodSelector = new Selector.call(
      Compiler.NO_SUCH_METHOD, null, Compiler.NO_SUCH_METHOD_ARG_COUNT);
  final Selector symbolValidatedConstructorSelector = new Selector.call(
      'validated', null, 1);
  final Selector fromEnvironmentSelector = new Selector.callConstructor(
      'fromEnvironment', null, 2);

  bool enabledNoSuchMethod = false;
  bool enabledRuntimeType = false;
  bool enabledFunctionApply = false;
  bool enabledInvokeOn = false;

  Stopwatch progress = new Stopwatch()..start();

  static const int PHASE_SCANNING = 0;
  static const int PHASE_RESOLVING = 1;
  static const int PHASE_DONE_RESOLVING = 2;
  static const int PHASE_COMPILING = 3;
  int phase;

  bool compilationFailed = false;

  bool hasCrashed = false;

  /// Set by the backend if real reflection is detected in use of dart:mirrors.
  bool disableTypeInferenceForMirrors = false;

  Compiler({this.tracer: const Tracer(),
            this.enableTypeAssertions: false,
            this.enableUserAssertions: false,
            this.trustTypeAnnotations: false,
            this.enableConcreteTypeInference: false,
            this.disableTypeInferenceFlag: false,
            this.maxConcreteTypeSize: 5,
            this.enableMinification: false,
            this.enableNativeLiveTypeAnalysis: false,
            bool emitJavaScript: true,
            bool generateSourceMap: true,
            this.analyzeAllFlag: false,
            bool analyzeOnly: false,
            bool analyzeSignaturesOnly: false,
            this.preserveComments: false,
            this.verbose: false,
            this.sourceMapUri: null,
            this.buildId: UNDETERMINED_BUILD_ID,
            this.globalJsName: r'$',
            this.terseDiagnostics: false,
            outputProvider,
            List<String> strips: const []})
      : this.analyzeOnly = analyzeOnly || analyzeSignaturesOnly,
        this.analyzeSignaturesOnly = analyzeSignaturesOnly,
        this.outputProvider = (outputProvider == null)
            ? NullSink.outputProvider
            : outputProvider {
    world = new World(this);

    closureMapping.ClosureNamer closureNamer;
    if (emitJavaScript) {
      js_backend.JavaScriptBackend jsBackend =
          new js_backend.JavaScriptBackend(this, generateSourceMap);
      closureNamer = jsBackend.namer;
      backend = jsBackend;
    } else {
      closureNamer = new closureMapping.ClosureNamer();
      backend = new dart_backend.DartBackend(this, strips);
    }

    // No-op in production mode.
    validator = new TreeValidatorTask(this);

    tasks = [
      libraryLoader = new LibraryLoaderTask(this),
      scanner = new ScannerTask(this),
      dietParser = new DietParserTask(this),
      parser = new ParserTask(this),
      patchParser = new PatchParserTask(this),
      resolver = new ResolverTask(this),
      closureToClassMapper = new closureMapping.ClosureTask(this, closureNamer),
      checker = new TypeCheckerTask(this),
      typesTask = new ti.TypesTask(this),
      constantHandler = new ConstantHandler(this, backend.constantSystem),
      deferredLoadTask = new DeferredLoadTask(this),
      mirrorUsageAnalyzerTask = new MirrorUsageAnalyzerTask(this),
      enqueuer = new EnqueueTask(this)];

    tasks.addAll(backend.tasks);
  }

  Universe get resolverWorld => enqueuer.resolution.universe;
  Universe get codegenWorld => enqueuer.codegen.universe;

  bool get hasBuildId => buildId != UNDETERMINED_BUILD_ID;

  bool get analyzeAll => analyzeAllFlag || compileAll;

  bool get compileAll => false;

  bool get disableTypeInference => disableTypeInferenceFlag;

  int getNextFreeClassId() => nextFreeClassId++;

  void ensure(bool condition) {
    if (!condition) cancel('failed assertion in leg');
  }

  void unimplemented(String methodName,
                     {Node node, Token token, HInstruction instruction,
                      Element element}) {
    internalError("$methodName not implemented",
                  node: node, token: token,
                  instruction: instruction, element: element);
  }

  void internalError(String message,
                     {Node node, Token token, HInstruction instruction,
                      Element element}) {
    cancel('Internal Error: $message',
           node: node, token: token,
           instruction: instruction, element: element);
  }

  void internalErrorOnElement(Element element, String message) {
    internalError(message, element: element);
  }

  void unhandledExceptionOnElement(Element element,
                                   StackTrace stackTrace,
                                   String message) {
    if (hasCrashed) return;
    hasCrashed = true;
    reportDiagnostic(spanFromElement(element),
                     MessageKind.COMPILER_CRASHED.error().toString(),
                     api.Diagnostic.CRASH);
    pleaseReportCrash(stackTrace, message);
  }

  void pleaseReportCrash(StackTrace stackTrace, String message) {
    print(MessageKind.PLEASE_REPORT_THE_CRASH.message({'buildId': buildId}));
    if (message != null) {
      print(message);
    }
    if (stackTrace != null) {
      print(stackTrace);
    }
  }

  void cancel(String reason, {Node node, Token token,
               HInstruction instruction, Element element}) {
    assembledCode = null; // Compilation failed. Make sure that we
                          // don't return a bogus result.
    Spannable spannable = null;
    if (node != null) {
      spannable = node;
    } else if (token != null) {
      spannable = token;
    } else if (instruction != null) {
      spannable = instruction;
    } else if (element != null) {
      spannable = element;
    } else {
      throw 'No error location for error: $reason';
    }
    reportError(spannable, MessageKind.GENERIC, {'text': reason});
    throw new CompilerCancelledException(reason);
  }

  SourceSpan spanFromSpannable(Spannable node, [Uri uri]) {
    if (node == null) return null;
    if (node == CURRENT_ELEMENT_SPANNABLE) {
      node = currentElement;
    }
    if (node is SourceSpan) {
      return node;
    } else if (node is Node) {
      return spanFromNode(node, uri);
    } else if (node is Token) {
      return spanFromTokens(node, node, uri);
    } else if (node is HInstruction) {
      return spanFromHInstruction(node);
    } else if (node is Element) {
      return spanFromElement(node);
    } else if (node is MetadataAnnotation) {
      MetadataAnnotation annotation = node;
      uri = annotation.annotatedElement.getCompilationUnit().script.uri;
      return spanFromTokens(annotation.beginToken, annotation.endToken, uri);
    } else {
      throw 'No error location.';
    }
  }

  void log(message) {
    reportDiagnostic(null, message, api.Diagnostic.VERBOSE_INFO);
  }

  Future<bool> run(Uri uri) {
    totalCompileTime.start();

    return new Future.sync(() => runCompiler(uri)).catchError((error, trace) {
      if (error is CompilerCancelledException) {
        log('Error: $error');
        return false;
      }

      try {
        if (!hasCrashed) {
          hasCrashed = true;
          reportDiagnostic(new SourceSpan(uri, 0, 0),
                           MessageKind.COMPILER_CRASHED.error().toString(),
                           api.Diagnostic.CRASH);
          String message = 'The compiler crashed.';
          pleaseReportCrash(trace, message);
        }
      } catch (doubleFault) {
        // Ignoring exceptions in exception handling.
      }
      throw error;
    }).whenComplete(() {
      tracer.close();
      totalCompileTime.stop();
    }).then((_) {
      return !compilationFailed;
    });
  }

  bool hasIsolateSupport() => isolateLibrary != null;

  /**
   * This method is called before [library] import and export scopes have been
   * set up.
   */
  Future onLibraryLoaded(LibraryElement library, Uri uri) {
    if (dynamicClass != null) {
      // When loading the built-in libraries, dynamicClass is null. We
      // take advantage of this as core imports js_helper and sees [dynamic]
      // this way.
      withCurrentElement(dynamicClass, () {
        library.addToScope(dynamicClass, this);
      });
    }
    if (uri == new Uri(scheme: 'dart', path: 'mirrors')) {
      mirrorsLibrary = library;
      mirrorSystemClass =
          findRequiredElement(library, 'MirrorSystem');
      mirrorsUsedClass =
          findRequiredElement(library, 'MirrorsUsed');
    } else if (uri == new Uri(scheme: 'dart', path: 'typed_data')) {
      typedDataLibrary = library;
      typedDataClass =
          findRequiredElement(library, 'TypedData');
    } else if (uri == new Uri(scheme: 'dart', path: '_collection-dev')) {
      symbolImplementationClass =
          findRequiredElement(library, 'Symbol');
    } else if (uri == new Uri(scheme: 'dart', path: 'async')) {
      deferredLibraryClass =
          findRequiredElement(library, 'DeferredLibrary');
    } else if (isolateHelperLibrary == null
	       && (uri == new Uri(scheme: 'dart', path: '_isolate_helper'))) {
      isolateHelperLibrary = library;
    } else if (foreignLibrary == null
	       && (uri == new Uri(scheme: 'dart', path: '_foreign_helper'))) {
      foreignLibrary = library;
    }
    return backend.onLibraryLoaded(library, uri);
  }

  Element findRequiredElement(LibraryElement library, String name) {
    var element = library.find(name);
    if (element == null) {
      internalErrorOnElement(
          library,
          'The library "${library.canonicalUri}" does not contain required '
          'element: $name');
      }
    return element;
  }

  void onClassResolved(ClassElement cls) {
    if (mirrorSystemClass == cls) {
      mirrorSystemGetNameFunction =
        cls.lookupLocalMember('getName');
    } else if (symbolClass == cls) {
      symbolConstructor = cls.constructors.head;
    } else if (symbolImplementationClass == cls) {
      symbolValidatedConstructor = symbolImplementationClass.lookupConstructor(
          symbolValidatedConstructorSelector);
    } else if (mirrorsUsedClass == cls) {
      mirrorsUsedConstructor = cls.constructors.head;
    } else if (intClass == cls) {
      intEnvironment = intClass.lookupConstructor(fromEnvironmentSelector);
    } else if (stringClass == cls) {
      stringEnvironment =
          stringClass.lookupConstructor(fromEnvironmentSelector);
    } else if (boolClass == cls) {
      boolEnvironment = boolClass.lookupConstructor(fromEnvironmentSelector);
    }
  }

  Future<LibraryElement> scanBuiltinLibrary(String filename);

  void initializeSpecialClasses() {
    final List missingCoreClasses = [];
    ClassElement lookupCoreClass(String name) {
      ClassElement result = coreLibrary.find(name);
      if (result == null) {
        missingCoreClasses.add(name);
      }
      return result;
    }
    objectClass = lookupCoreClass('Object');
    boolClass = lookupCoreClass('bool');
    numClass = lookupCoreClass('num');
    intClass = lookupCoreClass('int');
    doubleClass = lookupCoreClass('double');
    stringClass = lookupCoreClass('String');
    functionClass = lookupCoreClass('Function');
    listClass = lookupCoreClass('List');
    typeClass = lookupCoreClass('Type');
    mapClass = lookupCoreClass('Map');
    nullClass = lookupCoreClass('Null');
    stackTraceClass = lookupCoreClass('StackTrace');
    if (!missingCoreClasses.isEmpty) {
      internalErrorOnElement(coreLibrary,
          'dart:core library does not contain required classes: '
          '$missingCoreClasses');
    }

    // The Symbol class may not exist during unit testing.
    // TODO(ahe): It is possible that we have to require the presence
    // of Symbol as we change how we implement noSuchMethod.
    symbolClass = lookupCoreClass('Symbol');

    final List missingHelperClasses = [];
    ClassElement lookupHelperClass(String name) {
      ClassElement result = jsHelperLibrary.find(name);
      if (result == null) {
        missingHelperClasses.add(name);
      }
      return result;
    }
    jsInvocationMirrorClass = lookupHelperClass('JSInvocationMirror');
    boundClosureClass = lookupHelperClass('BoundClosure');
    closureClass = lookupHelperClass('Closure');
    dynamicClass = lookupHelperClass('Dynamic_');
    if (!missingHelperClasses.isEmpty) {
      internalErrorOnElement(jsHelperLibrary,
          'dart:_js_helper library does not contain required classes: '
          '$missingHelperClasses');
    }

    if (types == null) {
      types = new Types(this, dynamicClass);
    }
    backend.initializeHelperClasses();

    dynamicClass.ensureResolved(this);
  }

  Element _unnamedListConstructor;
  Element get unnamedListConstructor {
    if (_unnamedListConstructor != null) return _unnamedListConstructor;
    Selector callConstructor = new Selector.callConstructor(
        "", listClass.getLibrary());
    return _unnamedListConstructor =
        listClass.lookupConstructor(callConstructor);
  }

  Element _filledListConstructor;
  Element get filledListConstructor {
    if (_filledListConstructor != null) return _filledListConstructor;
    Selector callConstructor = new Selector.callConstructor(
        "filled", listClass.getLibrary());
    return _filledListConstructor =
        listClass.lookupConstructor(callConstructor);
  }

  Future scanBuiltinLibraries() {
    return scanBuiltinLibrary('_js_helper').then((LibraryElement library) {
      jsHelperLibrary = library;
      return scanBuiltinLibrary('_interceptors');
    }).then((LibraryElement library) {
      interceptorsLibrary = library;

      assertMethod = jsHelperLibrary.find('assertHelper');
      identicalFunction = coreLibrary.find('identical');

      initializeSpecialClasses();

      functionClass.ensureResolved(this);
      functionApplyMethod =
          functionClass.lookupLocalMember('apply');
      jsInvocationMirrorClass.ensureResolved(this);
      invokeOnMethod = jsInvocationMirrorClass.lookupLocalMember(INVOKE_ON);

      if (preserveComments) {
        var uri = new Uri(scheme: 'dart', path: 'mirrors');
        return libraryLoader.loadLibrary(uri, null, uri).then(
            (LibraryElement libraryElement) {
          documentClass = libraryElement.find('Comment');
        });
      }
    });
  }

  void importHelperLibrary(LibraryElement library) {
    if (jsHelperLibrary != null) {
      libraryLoader.importLibrary(library, jsHelperLibrary, null);
    }
  }

  /**
   * Get an [Uri] pointing to a patch for the dart: library with
   * the given path. Returns null if there is no patch.
   */
  Uri resolvePatchUri(String dartLibraryPath);

  Future runCompiler(Uri uri) {
    // TODO(ahe): This prevents memory leaks when invoking the compiler
    // multiple times. Implement a better mechanism where we can store
    // such caches in the compiler and get access to them through a
    // suitably maintained static reference to the current compiler.
    StringToken.canonicalizedSubstrings.clear();
    Selector.canonicalizedValues.clear();
    TypedSelector.canonicalizedValues.clear();

    assert(uri != null || analyzeOnly);
    return scanBuiltinLibraries().then((_) {
      if (librariesToAnalyzeWhenRun != null) {
        return Future.forEach(librariesToAnalyzeWhenRun, (libraryUri) {
          log('analyzing $libraryUri ($buildId)');
          return libraryLoader.loadLibrary(libraryUri, null, libraryUri);
        });
      }
    }).then((_) {
      if (uri != null) {
        if (analyzeOnly) {
          log('analyzing $uri ($buildId)');
        } else {
          log('compiling $uri ($buildId)');
        }
        return libraryLoader.loadLibrary(uri, null, uri)
            .then((LibraryElement library) {
          mainApp = library;
        });
      }
    }).then((_) {
      compileLoadedLibraries();
    });
  }

  /// Performs the compilation when all libraries have been loaded.
  void compileLoadedLibraries() {
    Element main = null;
    if (mainApp != null) {
      main = mainApp.findExported(MAIN);
      if (main == null) {
        if (!analyzeOnly) {
          // Allow analyze only of libraries with no main.
          reportFatalError(
              mainApp,
              MessageKind.GENERIC,
              {'text': "Error: Could not find '$MAIN'."});
        } else if (!analyzeAll) {
          reportFatalError(
              mainApp,
              MessageKind.GENERIC,
              {'text': "Error: Could not find '$MAIN'. "
              "No source will be analyzed. "
              "Use '--analyze-all' to analyze all code in the library."});
        }
      } else {
        if (main.isErroneous()) {
          reportFatalError(
              main,
              MessageKind.GENERIC,
              {'text': "Error: Cannot determine which '$MAIN' to use."});
        } else if (!main.isFunction()) {
          reportFatalError(
              main,
              MessageKind.GENERIC,
              {'text': "Error: '$MAIN' is not a function."});
        }
        mainFunction = main;
        FunctionSignature parameters = mainFunction.computeSignature(this);
        if (parameters.parameterCount > 2) {
          int index = 0;
          parameters.forEachParameter((Element parameter) {
            if (index++ < 2) return;
            reportError(
                parameter,
                MessageKind.GENERIC,
                {'text':
                  "Error: '$MAIN' cannot have more than two parameters."});
          });
        }
      }

      mirrorUsageAnalyzerTask.analyzeUsage(mainApp);

      // In order to see if a library is deferred, we must compute the
      // compile-time constants that are metadata.  This means adding
      // something to the resolution queue.  So we cannot wait with
      // this until after the resolution queue is processed.
      // TODO(ahe): Clean this up, for example, by not enqueueing
      // classes only used for metadata.
      deferredLoadTask.findDeferredLibraries(mainApp);
    }

    log('Resolving...');
    phase = PHASE_RESOLVING;
    if (analyzeAll) {
      libraries.forEach(
          (_, lib) => fullyEnqueueLibrary(lib, enqueuer.resolution));
    }
    // Elements required by enqueueHelpers are global dependencies
    // that are not pulled in by a particular element.
    backend.enqueueHelpers(enqueuer.resolution, globalDependencies);
    resolveLibraryMetadata();
    processQueue(enqueuer.resolution, main);
    enqueuer.resolution.logSummary(log);

    if (compilationFailed) return;
    if (analyzeOnly) return;
    assert(main != null);
    phase = PHASE_DONE_RESOLVING;

    // TODO(ahe): Remove this line. Eventually, enqueuer.resolution
    // should know this.
    world.populate();
    // Compute whole-program-knowledge that the backend needs. (This might
    // require the information computed in [world.populate].)
    backend.onResolutionComplete();

    deferredLoadTask.onResolutionComplete(main);

    log('Inferring types...');
    typesTask.onResolutionComplete(main);

    log('Compiling...');
    phase = PHASE_COMPILING;
    // TODO(johnniwinther): Move these to [CodegenEnqueuer].
    if (hasIsolateSupport()) {
      enqueuer.codegen.addToWorkList(
          isolateHelperLibrary.find(Compiler.START_ROOT_ISOLATE));
      enqueuer.codegen.registerGetOfStaticFunction(main);
    }
    if (enabledNoSuchMethod) {
      backend.enableNoSuchMethod(enqueuer.codegen);
    }
    if (compileAll) {
      libraries.forEach((_, lib) => fullyEnqueueLibrary(lib,
          enqueuer.codegen));
    }
    processQueue(enqueuer.codegen, main);
    enqueuer.codegen.logSummary(log);

    if (compilationFailed) return;

    backend.assembleProgram();

    checkQueues();

    if (compilationFailed) {
      assembledCode = null; // Signals failure.
    }
  }

  void fullyEnqueueLibrary(LibraryElement library, Enqueuer world) {
    void enqueueAll(Element element) {
      fullyEnqueueTopLevelElement(element, world);
    }
    library.implementation.forEachLocalMember(enqueueAll);
  }

  void fullyEnqueueTopLevelElement(Element element, Enqueuer world) {
    if (element.isClass()) {
      ClassElement cls = element;
      cls.ensureResolved(this);
      cls.forEachLocalMember(enqueuer.resolution.addToWorkList);
      world.registerInstantiatedClass(element, globalDependencies);
    } else {
      world.addToWorkList(element);
    }
  }

  // Resolves metadata on library elements.  This is necessary in order to
  // resolve metadata classes referenced only from metadata on library tags.
  // TODO(ahe): Figure out how to do this lazily.
  void resolveLibraryMetadata() {
    for (LibraryElement library in libraries.values) {
      if (library.metadata != null) {
        for (MetadataAnnotation metadata in library.metadata) {
          metadata.ensureResolved(this);
        }
      }
    }
  }

  void processQueue(Enqueuer world, Element main) {
    world.nativeEnqueuer.processNativeClasses(libraries.values);
    if (main != null) {
      FunctionElement mainMethod = main;
      if (mainMethod.computeSignature(this).parameterCount != 0) {
        // TODO(ngeoffray, floitsch): we should also ensure that the
        // class IsolateMessage is instantiated. Currently, just enabling
        // isolate support works.
        world.enableIsolateSupport(main.getLibrary());
        world.registerInstantiatedClass(listClass, globalDependencies);
        world.registerInstantiatedClass(stringClass, globalDependencies);
      }
      world.addToWorkList(main);
    }
    progress.reset();
    world.forEach((WorkItem work) {
      withCurrentElement(work.element, () => work.run(this, world));
    });
    world.queueIsClosed = true;
    if (compilationFailed) return;
    assert(world.checkNoEnqueuedInvokedInstanceMethods());
    if (DUMP_INFERRED_TYPES && phase == PHASE_COMPILING) {
      backend.dumpInferredTypes();
    }
  }

  /**
   * Perform various checks of the queues. This includes checking that
   * the queues are empty (nothing was added after we stopped
   * processing the queues). Also compute the number of methods that
   * were resolved, but not compiled (aka excess resolution).
   */
  checkQueues() {
    for (Enqueuer world in [enqueuer.resolution, enqueuer.codegen]) {
      world.forEach((WorkItem work) {
        internalErrorOnElement(work.element, "Work list is not empty.");
      });
    }
    if (!REPORT_EXCESS_RESOLUTION) return;
    var resolved = new Set.from(enqueuer.resolution.resolvedElements.keys);
    for (Element e in enqueuer.codegen.generatedCode.keys) {
      resolved.remove(e);
    }
    for (Element e in new Set.from(resolved)) {
      if (e.isClass() ||
          e.isField() ||
          e.isTypeVariable() ||
          e.isTypedef() ||
          identical(e.kind, ElementKind.ABSTRACT_FIELD)) {
        resolved.remove(e);
      }
      if (identical(e.kind, ElementKind.GENERATIVE_CONSTRUCTOR)) {
        ClassElement enclosingClass = e.getEnclosingClass();
        resolved.remove(e);

      }
      if (identical(e.getLibrary(), jsHelperLibrary)) {
        resolved.remove(e);
      }
      if (identical(e.getLibrary(), interceptorsLibrary)) {
        resolved.remove(e);
      }
    }
    log('Excess resolution work: ${resolved.length}.');
    for (Element e in resolved) {
      SourceSpan span = spanFromElement(e);
      reportDiagnostic(span, 'Warning: $e resolved but not compiled.',
                       api.Diagnostic.WARNING);
    }
  }

  TreeElements analyzeElement(Element element) {
    assert(invariant(element,
           element.impliesType() ||
           element.isField() ||
           element.isFunction() ||
           element.isGenerativeConstructor() ||
           element.isGetter() ||
           element.isSetter(),
           message: 'Unexpected element kind: ${element.kind}'));
    assert(invariant(element, element.isDeclaration));
    ResolutionEnqueuer world = enqueuer.resolution;
    TreeElements elements = world.getCachedElements(element);
    if (elements != null) return elements;
    assert(parser != null);
    Node tree = parser.parse(element);
    assert(invariant(element, !element.isSynthesized || tree == null));
    if (tree != null) validator.validate(tree);
    elements = resolver.resolve(element);
    if (tree != null && elements != null && !analyzeSignaturesOnly) {
      // Only analyze nodes with a corresponding [TreeElements].
      checker.check(elements);
    }
    world.resolvedElements[element] = elements;
    return elements;
  }

  TreeElements analyze(ResolutionWorkItem work, ResolutionEnqueuer world) {
    assert(invariant(work.element, identical(world, enqueuer.resolution)));
    assert(invariant(work.element, !work.isAnalyzed(),
        message: 'Element ${work.element} has already been analyzed'));
    if (progress.elapsedMilliseconds > 500) {
      // TODO(ahe): Add structured diagnostics to the compiler API and
      // use it to separate this from the --verbose option.
      if (phase == PHASE_RESOLVING) {
        log('Resolved ${enqueuer.resolution.resolvedElements.length} '
            'elements.');
        progress.reset();
      }
    }
    Element element = work.element;
    TreeElements result = world.getCachedElements(element);
    if (result != null) return result;
    result = analyzeElement(element);
    return result;
  }

  void codegen(CodegenWorkItem work, CodegenEnqueuer world) {
    assert(invariant(work.element, identical(world, enqueuer.codegen)));
    if (progress.elapsedMilliseconds > 500) {
      // TODO(ahe): Add structured diagnostics to the compiler API and
      // use it to separate this from the --verbose option.
      log('Compiled ${enqueuer.codegen.generatedCode.length} methods.');
      progress.reset();
    }
    backend.codegen(work);
  }

  DartType resolveTypeAnnotation(Element element,
                                 TypeAnnotation annotation) {
    return resolver.resolveTypeAnnotation(element, annotation);
  }

  DartType resolveReturnType(Element element,
                             TypeAnnotation annotation) {
    return resolver.resolveReturnType(element, annotation);
  }

  FunctionSignature resolveSignature(FunctionElement element) {
    return withCurrentElement(element,
                              () => resolver.resolveSignature(element));
  }

  FunctionSignature resolveFunctionExpression(Element element,
                                              FunctionExpression node) {
    return withCurrentElement(element,
        () => resolver.resolveFunctionExpression(element, node));
  }

  void resolveTypedef(TypedefElement element) {
    withCurrentElement(element,
                       () => resolver.resolve(element));
  }

  FunctionType computeFunctionType(Element element,
                                   FunctionSignature signature) {
    return withCurrentElement(element,
        () => resolver.computeFunctionType(element, signature));
  }

  reportWarning(Spannable node, var message) {
    if (message is TypeWarning) {
      // TODO(ahe): Don't supress these warning when the type checker
      // is more complete.
      if (message.message.kind == MessageKind.MISSING_RETURN) return;
      if (message.message.kind == MessageKind.MAYBE_MISSING_RETURN) return;
    }
    SourceSpan span = spanFromSpannable(node);
    reportDiagnostic(span, '$message', api.Diagnostic.WARNING);
  }

  void reportError(Spannable node,
                   MessageKind errorCode,
                   [Map arguments = const {}]) {
    reportMessage(spanFromSpannable(node),
                  errorCode.error(arguments, terseDiagnostics),
                  api.Diagnostic.ERROR);
  }

  void reportFatalError(Spannable node, MessageKind errorCode,
                        [Map arguments = const {}]) {
    reportError(node, errorCode, arguments);
    // TODO(ahe): Make this only abort the current method.
    throw new CompilerCancelledException(
        'Error: Cannot continue due to previous error.');
  }

  // TODO(ahe): Rename to reportWarning when that method has been removed.
  void reportWarningCode(Spannable node, MessageKind errorCode,
                         [Map arguments = const {}]) {
    reportMessage(spanFromSpannable(node),
                  errorCode.error(arguments, terseDiagnostics),
                  api.Diagnostic.WARNING);
  }

  void reportInfo(Spannable node, MessageKind errorCode,
                  [Map arguments = const {}]) {
    reportMessage(spanFromSpannable(node),
                  errorCode.error(arguments, terseDiagnostics),
                  api.Diagnostic.INFO);
  }

  void reportHint(Spannable node, MessageKind errorCode,
                  [Map arguments = const {}]) {
    reportMessage(spanFromSpannable(node),
                  errorCode.error(arguments, terseDiagnostics),
                  api.Diagnostic.HINT);
  }

  /// For debugging only, print a message with a source location.
  void reportHere(Spannable node, String debugMessage) {
    reportInfo(node, MessageKind.GENERIC, {'text': 'HERE: $debugMessage'});
  }

  void reportInternalError(Spannable node, String message) {
    reportError(
        node, MessageKind.GENERIC, {'text': 'Internal Error: $message'});
  }

  void reportMessage(SourceSpan span, Diagnostic message, api.Diagnostic kind) {
    // TODO(ahe): The names Diagnostic and api.Diagnostic are in
    // conflict. Fix it.
    reportDiagnostic(span, "$message", kind);
  }

  void reportDiagnostic(SourceSpan span, String message, api.Diagnostic kind);

  SourceSpan spanFromTokens(Token begin, Token end, [Uri uri]) {
    if (begin == null || end == null) {
      // TODO(ahe): We can almost always do better. Often it is only
      // end that is null. Otherwise, we probably know the current
      // URI.
      throw 'Cannot find tokens to produce error message.';
    }
    if (uri == null && currentElement != null) {
      uri = currentElement.getCompilationUnit().script.uri;
    }
    return SourceSpan.withCharacterOffsets(begin, end,
      (beginOffset, endOffset) => new SourceSpan(uri, beginOffset, endOffset));
  }

  SourceSpan spanFromNode(Node node, [Uri uri]) {
    return spanFromTokens(node.getBeginToken(), node.getEndToken(), uri);
  }

  SourceSpan spanFromElement(Element element) {
    if (Elements.isErroneousElement(element)) {
      element = element.enclosingElement;
    }
    if (element.position() == null &&
        !element.isLibrary() &&
        !element.isCompilationUnit()) {
      // Sometimes, the backend fakes up elements that have no
      // position. So we use the enclosing element instead. It is
      // not a good error location, but cancel really is "internal
      // error" or "not implemented yet", so the vicinity is good
      // enough for now.
      element = element.enclosingElement;
      // TODO(ahe): I plan to overhaul this infrastructure anyways.
    }
    if (element == null) {
      element = currentElement;
    }
    Token position = element.position();
    Uri uri = element.getCompilationUnit().script.uri;
    return (position == null)
        ? new SourceSpan(uri, 0, 0)
        : spanFromTokens(position, position, uri);
  }

  SourceSpan spanFromHInstruction(HInstruction instruction) {
    Element element = instruction.sourceElement;
    if (element == null) element = currentElement;
    var position = instruction.sourcePosition;
    if (position == null) return spanFromElement(element);
    Token token = position.token;
    if (token == null) return spanFromElement(element);
    Uri uri = element.getCompilationUnit().script.uri;
    return spanFromTokens(token, token, uri);
  }

  /**
   * Translates the [resolvedUri] into a readable URI.
   *
   * The [importingLibrary] holds the library importing [resolvedUri] or
   * [:null:] if [resolvedUri] is loaded as the main library. The
   * [importingLibrary] is used to grant access to internal libraries from
   * platform libraries and patch libraries.
   *
   * If the [resolvedUri] is not accessible from [importingLibrary], this method
   * is responsible for reporting errors.
   *
   * See [LibraryLoader] for terminology on URIs.
   */
  Uri translateResolvedUri(LibraryElement importingLibrary,
                           Uri resolvedUri, Node node) {
    unimplemented('Compiler.translateResolvedUri');
  }

  /**
   * Reads the script specified by the [readableUri].
   *
   * See [LibraryLoader] for terminology on URIs.
   */
  Future<Script> readScript(Uri readableUri, [Element element, Node node]) {
    unimplemented('Compiler.readScript');
  }

  String get legDirectory {
    unimplemented('Compiler.legDirectory');
  }

  // TODO(karlklose): split into findHelperFunction and findHelperClass and
  // add a check that the element has the expected kind.
  Element findHelper(String name)
      => jsHelperLibrary.findLocal(name);
  Element findInterceptor(String name)
      => interceptorsLibrary.findLocal(name);

  Element lookupElementIn(ScopeContainerElement container, String name) {
    Element element = container.localLookup(name);
    if (element == null) {
      throw 'Could not find $name in $container';
    }
    return element;
  }

  bool get isMockCompilation => false;

  Token processAndStripComments(Token currentToken) {
    Token firstToken = currentToken;
    Token prevToken;
    while (currentToken.kind != EOF_TOKEN) {
      if (identical(currentToken.kind, COMMENT_TOKEN)) {
        Token firstCommentToken = currentToken;
        while (identical(currentToken.kind, COMMENT_TOKEN)) {
          currentToken = currentToken.next;
        }
        commentMap[currentToken] = firstCommentToken;
        if (prevToken == null) {
          firstToken = currentToken;
        } else {
          prevToken.next = currentToken;
        }
      }
      prevToken = currentToken;
      currentToken = currentToken.next;
    }
    return firstToken;
  }
}

class CompilerTask {
  final Compiler compiler;
  final Stopwatch watch;

  CompilerTask(Compiler compiler)
      : this.compiler = compiler,
        watch = (compiler.verbose) ? new Stopwatch() : null;

  String get name => 'Unknown task';
  int get timing => (watch != null) ? watch.elapsedMilliseconds : 0;

  measure(action()) {
    if (watch == null) return action();
    CompilerTask previous = compiler.measuredTask;
    if (identical(this, previous)) return action();
    compiler.measuredTask = this;
    if (previous != null) previous.watch.stop();
    watch.start();
    try {
      return action();
    } finally {
      watch.stop();
      if (previous != null) previous.watch.start();
      compiler.measuredTask = previous;
    }
  }

  measureElement(Element element, action()) {
    compiler.withCurrentElement(element, () => measure(action));
  }
}

class CompilerCancelledException implements Exception {
  final String reason;
  CompilerCancelledException(this.reason);

  String toString() {
    String banner = 'compiler cancelled';
    return (reason != null) ? '$banner: $reason' : '$banner';
  }
}

class Tracer {
  final bool enabled = false;

  const Tracer();

  void traceCompilation(String methodName,
                        ItemCompilationContext context,
                        Compiler compiler) {
  }

  void traceGraph(String name, var graph) {
  }

  void close() {
  }
}

class SourceSpan implements Spannable {
  final Uri uri;
  final int begin;
  final int end;

  const SourceSpan(this.uri, this.begin, this.end);

  static withCharacterOffsets(Token begin, Token end,
                     f(int beginOffset, int endOffset)) {
    final beginOffset = begin.charOffset;
    final endOffset = end.charOffset + end.charCount;

    // [begin] and [end] might be the same for the same empty token. This
    // happens for instance when scanning '$$'.
    assert(endOffset >= beginOffset);
    return f(beginOffset, endOffset);
  }

  String toString() => 'SourceSpan($uri, $begin, $end)';
}

/**
 * Throws a [SpannableAssertionFailure] if [condition] is
 * [:false:]. [condition] must be either a [:bool:] or a no-arg
 * function returning a [:bool:].
 *
 * Use this method to provide better information for assertion by calling
 * [invariant] as the argument to an [:assert:] statement:
 *
 *     assert(invariant(position, isValid));
 *
 * [spannable] must be non-null and will be used to provide positional
 * information in the generated error message.
 */
bool invariant(Spannable spannable, var condition, {var message: null}) {
  // TODO(johnniwinther): Use [spannable] and [message] to provide better
  // information on assertion errors.
  if (spannable == null) {
    throw new SpannableAssertionFailure(CURRENT_ELEMENT_SPANNABLE,
        "Spannable was null for invariant. Use CURRENT_ELEMENT_SPANNABLE.");
  }
  if (condition is Function){
    condition = condition();
  }
  if (!condition) {
    if (message is Function) {
      message = message();
    }
    throw new SpannableAssertionFailure(spannable, message);
  }
  return true;
}

/**
 * Global
 */
bool isPrivateName(String s) => !s.isEmpty && s.codeUnitAt(0) == $_;

/// A sink that drains into /dev/null.
class NullSink implements EventSink<String> {
  final String name;

  NullSink(this.name);

  add(String value) {}

  void addError(Object error, [StackTrace stackTrace]) {}

  void close() {}

  toString() => name;

  /// Convenience method for getting an [api.CompilerOutputProvider].
  static NullSink outputProvider(String name, String extension) {
    return new NullSink('$name.$extension');
  }
}
