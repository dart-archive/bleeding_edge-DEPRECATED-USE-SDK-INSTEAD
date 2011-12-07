// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test constant folding.

#library("compiler_helper");

#import("../../../leg/leg.dart", prefix: "leg");
#import("../../../leg/elements/elements.dart", prefix: "lego");
#import("parser_helper.dart");
#import("mock_compiler.dart");

class StringScript extends leg.Script {
  final String code;
  StringScript(this.code) : super(null);
  String get text() => code;
}

String compile(String code, [String entry = 'main']) {
  MockCompiler compiler = new MockCompiler();
  compiler.parseScript(code);
  lego.Element element = compiler.universe.find(buildSourceString(entry));
  if (element === null) return null;
  String generated = compiler.compileMethod(
      new leg.WorkElement.toCompile(element));
  return generated;
}

String compileClasses(List instantiatedClasses) {
  leg.Compiler compiler = new leg.Compiler(new StringScript(''));
  leg.Universe universe = compiler.universe;
  universe.instantiatedClasses.addAll(instantiatedClasses);
  compiler.emitter.assembleProgram();
  return compiler.assembledCode;
}
