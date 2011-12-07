// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Compiler implements Canceler, Logger {
  final Script script;
  Queue<Element> worklist;
  Universe universe;
  String assembledCode;

  CompilerTask measuredTask;

  List<CompilerTask> tasks;
  ScannerTask scanner;
  ParserTask parser;
  TreeValidatorTask validator;
  ResolverTask resolver;
  TypeCheckerTask checker;
  SsaBuilderTask builder;
  SsaOptimizerTask optimizer;
  SsaCodeGeneratorTask generator;
  CodeEmitterTask emitter;

  static final SourceString MAIN = const SourceString('main');

  Compiler(this.script) {
    universe = new Universe();
    worklist = new Queue<Element>();
    scanner = new ScannerTask(this);
    parser = new ParserTask(this);
    validator = new TreeValidatorTask(this);
    resolver = new ResolverTask(this);
    checker = new TypeCheckerTask(this);
    builder = new SsaBuilderTask(this);
    optimizer = new SsaOptimizerTask(this);
    generator = new SsaCodeGeneratorTask(this);
    emitter = new CodeEmitterTask(this);
    tasks = [scanner, parser, resolver, checker, builder, optimizer, generator,
             emitter];
  }

  void ensure(bool condition) {
    if (!condition) cancel('failed assertion in leg');
  }

  void unimplemented(String methodName) {
    cancel("$methodName not implemented");
  }

  void cancel([String reason, Node node, Token token,
               HInstruction instruction]) {
    throw new CompilerCancelledException(reason);
  }

  void log(message) {
    // Do nothing.
  }

  bool run() {
    try {
      runCompiler();
    } catch (CompilerCancelledException exception) {
      log(exception.toString());
      log('compilation failed');
      return false;
    }
    // TODO(floitsch): the following code can be removed once the HTracer
    // writes directly into a file.
    if (GENERATE_SSA_TRACE) {
      print("------------------");
      print(new HTracer.singleton());
      print("------------------");
    }
    log('compilation succeeded');
    return true;
  }

  void scanCoreLibrary() {
    String fileName = io.join([legDirectory, 'lib', 'core.dart']);
    scanner.scan(readScript(fileName));
    // Make our special function a foreign kind.
    Element element = new ForeignElement(const SourceString('JS'));
    universe.define(element);
  }

  void runCompiler() {
    scanCoreLibrary();
    scanner.scan(script);
    Element element = universe.find(MAIN);
    if (element === null) cancel('Could not find $MAIN');
    compileMethod(element);
    while (!worklist.isEmpty()) {
      compileMethod(worklist.removeLast());
    }
    emitter.assembleProgram();
  }

  String compileMethod(Element element) {
    String code = universe.generatedCode[element];
    if (code !== null) return code;
    Node tree = parser.parse(element);
    validator.validate(tree);
    TreeElements elements = resolver.resolve(element);
    checker.check(tree, elements);
    HGraph graph = builder.build(tree, elements);
    optimizer.optimize(graph);
    code = generator.generate(element, graph);
    universe.addGeneratedCode(element, code);
    return code;
  }

  Element resolveType(ClassElement element) {
    parser.parse(element);
    resolver.resolveType(element);
    return element;
  }

  Element resolveSignature(FunctionElement element) {
    parser.parse(element);
    resolver.resolveSignature(element);
    return element;
  }

  reportWarning(Node node, var message) {}
  reportError(Node node, var message) {}

  Script readScript(String filename) {
    unimplemented('Compiler.readScript');
  }

  String get legDirectory() {
    unimplemented('Compiler.legDirectory');
  }
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

class CompilerCancelledException {
  final String reason;
  CompilerCancelledException(this.reason);

  String toString() {
    String banner = 'compiler cancelled';
    return (reason !== null) ? '$banner: $reason' : '$banner';
  }
}
