// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class WorkItem {
  final Element element;
  TreeElements resolutionTree;
  Function run;
  Map<int, BailoutInfo> bailouts = null;
  bool allowSpeculativeOptimization = true;

  WorkItem.toCompile(this.element) : resolutionTree = null {
    run = this.compile;
  }

  WorkItem.toCodegen(this.element, this.resolutionTree) {
    run = this.codegen;
  }

  WorkItem.bailoutVersion(this.element, this.resolutionTree, this.bailouts) {
    run = this.codegen;
  }

  bool isBailoutVersion() => bailouts != null;

  bool isAnalyzed() => resolutionTree != null;

  int hashCode() => element.hashCode();

  String compile(Compiler compiler) {
    return compiler.compile(this);
  }

  String codegen(Compiler compiler) {
    return compiler.codegen(this);
  }
}

class Compiler implements DiagnosticListener {
  Queue<WorkItem> worklist;
  Universe universe;
  String assembledCode;
  Namer namer;
  Types types;
  final String currentDirectory;

  final Tracer tracer;

  CompilerTask measuredTask;
  Element _currentElement;
  LibraryElement coreLibrary;
  LibraryElement coreImplLibrary;
  LibraryElement jsHelperLibrary;
  LibraryElement mainApp;
  ClassElement objectClass;

  Element get currentElement() => _currentElement;
  withCurrentElement(Element element, f()) {
    Element old = currentElement;
    _currentElement = element;
    try {
      return f();
    } finally {
      _currentElement = old;
    }
  }

  List<CompilerTask> tasks;
  ScannerTask scanner;
  DietParserTask dietParser;
  ParserTask parser;
  TreeValidatorTask validator;
  ResolverTask resolver;
  TypeCheckerTask checker;
  SsaBuilderTask builder;
  SsaOptimizerTask optimizer;
  SsaCodeGeneratorTask generator;
  CodeEmitterTask emitter;
  CompileTimeConstantHandler compileTimeConstantHandler;

  static final SourceString MAIN = const SourceString('main');
  static final SourceString NO_SUCH_METHOD = const SourceString('noSuchMethod');
  static final SourceString NO_SUCH_METHOD_EXCEPTION =
      const SourceString('NoSuchMethodException');
  static final SourceString OBJECT = const SourceString('Object');
  bool enabledNoSuchMethod = false;

  Compiler.withCurrentDirectory(String this.currentDirectory,
                                [this.tracer = const Tracer()])
      : types = new Types(),
        universe = new Universe(),
        worklist = new Queue<WorkItem>() {
    namer = new Namer(this);
    compileTimeConstantHandler = new CompileTimeConstantHandler(this);
    scanner = new ScannerTask(this);
    dietParser = new DietParserTask(this);
    parser = new ParserTask(this);
    validator = new TreeValidatorTask(this);
    resolver = new ResolverTask(this);
    checker = new TypeCheckerTask(this);
    builder = new SsaBuilderTask(this);
    optimizer = new SsaOptimizerTask(this);
    generator = new SsaCodeGeneratorTask(this);
    emitter = new CodeEmitterTask(this);
    tasks = [scanner, dietParser, parser, resolver, checker,
             builder, optimizer, generator,
             emitter, compileTimeConstantHandler];
  }

  void ensure(bool condition) {
    if (!condition) cancel('failed assertion in leg');
  }

  void unimplemented(String methodName,
                     [Node node, Token token, HInstruction instruction,
                      Element element]) {
    cancel("$methodName not implemented", node, token, instruction, element);
  }

  void internalError(String message,
                     [Node node, Token token, HInstruction instruction,
                      Element element]) {
    cancel("Internal Error: $message", node, token, instruction, element);
  }

  void cancel([String reason, Node node, Token token,
               HInstruction instruction, Element element]) {
    throw new CompilerCancelledException(reason);
  }

  void log(message) {
    // Do nothing.
  }

  void enqueue(WorkItem work) {
    worklist.add(work);
  }

  bool run(Script script) {
    try {
      runCompiler(script);
    } catch (CompilerCancelledException exception) {
      log(exception.toString());
      log('compilation failed');
      return false;
    }
    tracer.close();
    log('compilation succeeded');
    return true;
  }

  enableNoSuchMethod(Element element) {
    if (enabledNoSuchMethod) return;
    if (element.enclosingElement == objectClass) return;
    enabledNoSuchMethod = true;
    Set<Invocation> invocations = universe.invokedNames.putIfAbsent(
        NO_SUCH_METHOD, () => new Set<Invocation>());
    invocations.add(new Invocation(2));
  }

  abstract LibraryElement scanBuiltinLibrary(String filename);

  void scanBuiltinLibraries() {
    coreImplLibrary = scanBuiltinLibrary('coreimpl.dart');
    jsHelperLibrary = scanBuiltinLibrary('js_helper.dart');
    coreLibrary = scanBuiltinLibrary('core.dart');
    objectClass = coreLibrary.find(OBJECT);
    // Since coreLibrary import the libraries "coreimpl", and
    // "js_helper", coreLibrary is null when they are being built. So
    // we add the implicit import of coreLibrary now. This can be
    // cleaned up when we have proper support for "dart:core" and
    // don't need to access it through the field "coreLibrary".
    // TODO(ahe): Clean this up as described above.
    scanner.importLibrary(coreImplLibrary, coreLibrary, null);
    scanner.importLibrary(jsHelperLibrary, coreLibrary, null);
    addForeignFunctions(jsHelperLibrary);

    universe.libraries['dart:core'] = coreLibrary;
    universe.libraries['dart:coreimpl'] = coreImplLibrary;
  }

  /** Define the JS helper functions in the given library. */
  void addForeignFunctions(LibraryElement library) {
    library.define(new ForeignElement(
        const SourceString('JS'), library), this);
    library.define(new ForeignElement(
        const SourceString('UNINTERCEPTED'), library), this);
    library.define(new ForeignElement(
        const SourceString('JS_HAS_EQUALS'), library), this);
  }

  void enqueueInvokedInstanceMethods() {
    // TODO(floitsch): find a more efficient way of doing this.

    // Run through the classes and see if we need to compile methods.
    for (ClassElement classElement in universe.instantiatedClasses) {
      for (ClassElement currentClass = classElement;
           currentClass !== null;
           currentClass = currentClass.superclass) {
        // TODO(floitsch): we don't need to add members that have been
        // overwritten by subclasses.
        for (Element member in currentClass.members) {
          if (universe.generatedCode[member] !== null) continue;
          if (!member.isInstanceMember()) continue;
          if (member.kind == ElementKind.FUNCTION) {
            if (member.name == NO_SUCH_METHOD) enableNoSuchMethod(member);
            Set<Selector> selectors = universe.invokedNames[member.name];
            if (selectors != null) {
              for (Selector selector in selectors) {
                if (selector.applies(this, member)) {
                  addToWorklist(member);
                  break;
                }
              }
            }
            // If there is a property access with the same name as a method we
            // need to emit the method.
            if (universe.invokedGetters.contains(member.name)) {
              addToWorklist(member);
            }
          } else if (member.kind == ElementKind.GETTER) {
            if (universe.invokedGetters.contains(member.name)) {
              addToWorklist(member);
            }
            // A method invocation like in o.foo(x, y) might actually be an
            // invocation of the getter foo followed by an invocation of the
            // returned closure.
            Set<Selector> invokedSelectors = universe.invokedNames[member.name];
            // We don't know what selectors the returned closure accepts. If
            // the set contains any selector we have to assume that it matches.
            if (invokedSelectors !== null && !invokedSelectors.isEmpty()) {
              addToWorklist(member);
            }
          } else if (member.kind === ElementKind.SETTER) {
             if (universe.invokedSetters.contains(member.name)) {
              addToWorklist(member);
            }
          }

          // Make sure that the closure understands a call with the given
          // selector. For a method-invocation of the form o.foo(a: 499), we
          // need to make sure that closures can handle the optional argument if
          // there exists a field or getter 'foo'.
          if (member.kind === ElementKind.GETTER ||
              member.kind === ElementKind.FIELD) {
            Set<Selector> invokedSelectors = universe.invokedNames[member.name];
            if (invokedSelectors != null) {
              for (Selector selector in invokedSelectors) {
                registerDynamicInvocation(Namer.CLOSURE_INVOCATION_NAME,
                                          selector);
              }
            }
          }
        }
      }
    }
  }

  void runCompiler(Script script) {
    scanBuiltinLibraries();
    mainApp = new LibraryElement(script);
    native.checkNativeSupport(this, mainApp, null);

    Element element;
    withCurrentElement(mainApp, () {
        scanner.scan(currentElement);
        element = mainApp.find(MAIN);
        if (element === null) cancel('Could not find $MAIN');
      });
    worklist.add(new WorkItem.toCompile(element));
    do {
      while (!worklist.isEmpty()) {
        WorkItem work = worklist.removeLast();
        withCurrentElement(work.element, () => (work.run)(this));
      }
      enqueueInvokedInstanceMethods();
    } while (!worklist.isEmpty());
    emitter.assembleProgram();
  }

  TreeElements analyzeElement(Element element) {
    assert(parser !== null);
    Node tree = parser.parse(element);
    validator.validate(tree);
    TreeElements elements = resolver.resolve(element);
    checker.check(tree, elements);
    return elements;
  }

  TreeElements analyze(WorkItem work) {
    work.resolutionTree = analyzeElement(work.element);
    return work.resolutionTree;
  }

  String codegen(WorkItem work) {
    String code;
    if (work.element.kind == ElementKind.FIELD
        || work.element.kind == ElementKind.PARAMETER) {
      compileTimeConstantHandler.compileWorkItem(work);
      return null;
    } else {
      HGraph graph = builder.build(work);
      optimizer.optimize(work, graph);
      code = generator.generate(work, graph);
      universe.addGeneratedCode(work, code);
      return code;
    }
  }

  String compile(WorkItem work) {
    String code = universe.generatedCode[work.element];
    if (code !== null) return code;
    analyze(work);
    return codegen(work);
  }

  void addToWorklist(Element element) {
    if (element.kind === ElementKind.GENERATIVE_CONSTRUCTOR) {
      registerInstantiatedClass(element.enclosingElement);
    }
    worklist.add(new WorkItem.toCompile(element));
  }

  void registerStaticUse(Element element) {
    addToWorklist(element);
  }

  void registerGetOfStaticFunction(FunctionElement element) {
    registerStaticUse(element);
    universe.staticFunctionsNeedingGetter.add(element);
  }

  void registerDynamicInvocation(SourceString methodName, Selector selector) {
    assert(selector !== null);
    Set<Invocation> existing = universe.invokedNames[methodName];
    if (existing == null) {
      universe.invokedNames[methodName] = new Set.from(<Selector>[selector]);
    } else {
      existing.add(selector);
    }
  }

  void registerDynamicGetter(SourceString methodName) {
    universe.invokedGetters.add(methodName);
  }

  void registerDynamicSetter(SourceString methodName) {
    universe.invokedSetters.add(methodName);
  }

  void registerInstantiatedClass(ClassElement element) {
    universe.instantiatedClasses.add(element);
  }

  Type resolveType(ClassElement element) {
    return withCurrentElement(element, () => resolver.resolveType(element));
  }

  FunctionParameters resolveSignature(FunctionElement element) {
    return withCurrentElement(element,
                              () => resolver.resolveSignature(element));
  }

  Object compileVariable(VariableElement element) {
    return withCurrentElement(element, () {
        compile(new WorkItem.toCompile(element));
        return compileTimeConstantHandler.compileVariable(element);
      });
  }

  reportWarning(Node node, var message) {}
  reportError(Node node, var message) {}

  Script readScript(Uri uri, [ScriptTag node]) {
    unimplemented('Compiler.readScript');
  }

  String get legDirectory() {
    unimplemented('Compiler.legDirectory');
  }

  Element findHelper(SourceString name) => jsHelperLibrary.find(name);
}

class CompilerTask {
  final Compiler compiler;
  final Stopwatch watch;

  CompilerTask(this.compiler) : watch = new Stopwatch();

  String get name() => 'Unknown task';
  int get timing() => watch.elapsedInMs();

  measure(Function action) {
    // TODO(kasperl): Do we have to worry about exceptions here?
    CompilerTask previous = compiler.measuredTask;
    compiler.measuredTask = this;
    if (previous !== null) previous.watch.stop();
    watch.start();
    var result = action();
    watch.stop();
    if (previous !== null) previous.watch.start();
    compiler.measuredTask = previous;
    return result;
  }
}

class CompilerCancelledException implements Exception {
  final String reason;
  CompilerCancelledException(this.reason);

  String toString() {
    String banner = 'compiler cancelled';
    return (reason !== null) ? '$banner: $reason' : '$banner';
  }
}

interface Tracer default LTracer {
  const Tracer();
  final bool enabled;
  void traceGraph(String name, var graph);
  void close();
}

// TODO(ahe): Remove when the VM supports implicit interfaces.
class LTracer implements Tracer {
  const LTracer();
  final bool enabled = false;
  void traceGraph(String name, var graph) {
  }
  void close() {
  }
}
