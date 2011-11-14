// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Compiler implements Canceler, Logger {
  final Script script;
  Queue<Element> worklist;
  Universe universe;

  List<CompilerTask> tasks;
  ScannerTask scanner;
  ParserTask parser;
  ResolverTask resolver;
  TypeCheckerTask checker;
  SsaBuilderTask builder;
  SsaOptimizerTask optimizer;
  SsaCodeGeneratorTask generator;

  static final SourceString MAIN = const SourceString('main');

  Compiler(this.script) {
    universe = new Universe();
    worklist = new Queue<SourceString>();
    scanner = new ScannerTask(this);
    parser = new ParserTask(this);
    resolver = new ResolverTask(this);
    checker = new TypeCheckerTask(this);
    builder = new SsaBuilderTask(this);
    optimizer = new SsaOptimizerTask(this);
    generator = new SsaCodeGeneratorTask(this);
    tasks = [scanner, parser, resolver, checker, builder, optimizer, generator];
  }

  void ensure(bool condition) {
    if (!condition) cancel('failed assertion in leg');
  }

  void unimplemented(String methodName) {
    cancel("$methodName not implemented");
  }

  void cancel([String reason]) {
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
    String fileName = io.join([frog.options.libDir, '..',
                               'leg', 'lib', 'core.dart']);
    frog.SourceFile file = io.readSync(fileName);
    scanner.scan(new Script(file));
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
  }

  String compileMethod(Element element) {
    Node tree = parser.parse(element);
    Map<Node, Element> elements = resolver.resolve(tree);
    checker.check(tree, elements);
    HGraph graph = builder.build(tree, elements);
    optimizer.optimize(graph);
    String code = generator.generate(tree, graph);
    universe.addGeneratedCode(element, code);
    return code;
  }

  Element resolveType(ClassElement element) {
    resolver.resolveType(parser.parse(element));
  }

  String getGeneratedCode() {
    StringBuffer buffer = new StringBuffer();
    List<String> codeBlocks = universe.generatedCode.getValues();
    for (int i = codeBlocks.length - 1; i >= 0; i--) {
      buffer.add(codeBlocks[i]);
    }
    buffer.add('main();\n');
    return buffer.toString();
  }

  reportWarning(Node node, String message) {}
}

class CompilerTask {
  final Compiler compiler;
  final Stopwatch watch;

  CompilerTask(this.compiler) : watch = new Stopwatch();

  String get name() => 'Unknown task';
  int get timing() => watch.elapsedInMs();

  measure(Function action) {
    // TODO(kasperl): Do we have to worry about exceptions here?
    watch.start();
    var result = action();
    watch.stop();
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
