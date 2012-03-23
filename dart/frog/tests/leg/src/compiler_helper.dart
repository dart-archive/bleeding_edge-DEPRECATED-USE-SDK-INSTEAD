// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test constant folding.

#library("compiler_helper");

#import('../../../../lib/uri/uri.dart');
#import("../../../leg/leg.dart", prefix: "leg");
#import("../../../leg/elements/elements.dart", prefix: "lego");
#import("../../../leg/ssa/ssa.dart", prefix: "ssa");
#import("parser_helper.dart");
#import("mock_compiler.dart");

String compile(String code, [String entry = 'main']) {
  MockCompiler compiler = new MockCompiler();
  compiler.parseScript(code);
  lego.Element element = compiler.mainApp.find(buildSourceString(entry));
  if (element === null) return null;
  String generated = compiler.compile(new leg.WorkItem.toCompile(element));
  return generated;
}

String compileAll(String code) {
  leg.Compiler compiler = new MockCompiler();
  Uri uri = new Uri(scheme: 'source');
  compiler.sources[uri.toString()] = code;
  compiler.runCompiler(uri);
  return compiler.assembledCode;
}

class HGraphPair {
  ssa.HGraph optimized;
  ssa.HGraph unoptimized;
  HGraphPair(this.optimized, this.unoptimized);
}

ssa.HGraph getGraph(MockCompiler compiler, leg.WorkItem work) {
  compiler.analyze(work);
  ssa.HGraph graph = compiler.builder.build(work);
  compiler.optimizer.optimize(work, graph);
  // Also run the code generator to get the unoptimized version in the
  // queue.
  compiler.generator.generate(work, graph);
  return graph;
}

HGraphPair getGraphs(String code, String entry) {
  MockCompiler compiler = new MockCompiler();
  compiler.parseScript(code);
  lego.Element element = compiler.mainApp.find(buildSourceString(entry));
  if (element === null) return null;
  leg.WorkItem work = new leg.WorkItem.toCompile(element);
  ssa.HGraph optimized = getGraph(compiler, work);
  ssa.HGraph unoptimized = null;
  work = compiler.lastBailoutWork;
  if (work != null && work.element == element) {
    unoptimized = getGraph(compiler, work);
  }
  return new HGraphPair(optimized, unoptimized);
}

String anyIdentifier = "[a-zA-Z][a-zA-Z0-9]*";

String getIntTypeCheck(String variable) {
  return "\\($variable !== \\($variable \\| 0\\)\\)";
}

String getNumberTypeCheck(String variable) {
  return "\\(typeof $variable !== 'number'\\)";
}

bool checkNumberOfMatches(Iterator it, int nb) {
  for (int i = 0; i < nb; i++) {
    Expect.isTrue(it.hasNext());
    it.next();
  }
  Expect.isFalse(it.hasNext());
}

void compileAndMatch(String code, String entry, RegExp regexp) {
  String generated = compile(code, entry);
  Expect.isTrue(regexp.hasMatch(generated),
                '"$generated" does not match /$regexp/');
}

void compileAndDoNotMatch(String code, String entry, RegExp regexp) {
  String generated = compile(code, entry);
  Expect.isFalse(regexp.hasMatch(generated),
                 '"$generated" has a match in /$regexp/');
}
