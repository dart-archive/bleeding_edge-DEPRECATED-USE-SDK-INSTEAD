// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Compiler implements Canceler, Logger {
  final Script script;
  Queue<SourceString> worklist;
  Universe universe;

  List<CompilerTask> tasks;
  ScannerTask scanner;
  ResolverTask resolver;
  TypeCheckerTask checker;
  SsaBuilderTask builder;
  SsaOptimizerTask optimizer;
  SsaCodeGeneratorTask generator;

  static final SourceString MAIN = const SourceString('main');

  Compiler(this.script) {
    universe = new Universe();
    worklist = new Queue<SourceString>.from([MAIN]);
    scanner = new ScannerTask(this);
    resolver = new ResolverTask(this);
    checker = new TypeCheckerTask(this);
    builder = new SsaBuilderTask(this);
    optimizer = new SsaOptimizerTask(this);
    generator = new SsaCodeGeneratorTask(this);
    tasks = [scanner, resolver, checker, builder, optimizer, generator];
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

  void runCompiler() {
    scanner.scan(script);
    while (!worklist.isEmpty()) {
      SourceString name = worklist.removeLast();
      Element element = universe.find(name);
      if (element === null) cancel('Could not find $name');
      Node tree = element.parseNode(this, this);
      Map<Node, Element> elements = resolver.resolve(tree);
      checker.check(tree, elements);
      HGraph graph = builder.build(tree);
      optimizer.optimize(graph);
      String code = generator.generate(tree, graph);
      universe.addGeneratedCode(element, code);
    }
  }

  String getGeneratedCode() {
    StringBuffer buffer = new StringBuffer();
    buffer.add(PRINT_SUPPORT);
    buffer.add(ADD_SUPPORT);
    List<String> codeBlocks = universe.generatedCode.getValues();
    for (int i = codeBlocks.length - 1; i >= 0; i--) {
      buffer.add(codeBlocks[i]);
    }
    buffer.add('main();\n');
    return buffer.toString();
  }

  abstract reportWarning(Node node, String message);
}

class CompilerTask {
  final Compiler compiler;
  final StopWatch watch;

  CompilerTask(this.compiler) : watch = new StopWatch();

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

// TODO(kasperl): These need to be read from a file. Soon.
final String PRINT_SUPPORT = """
var print = (typeof console == 'object')
    ? function(obj) { console.log(obj); }
    : function(obj) { write(obj); write('\\n'); };
""";

final String ADD_SUPPORT = """
function \$add(a, b) {
  return a + b;
}
""";
