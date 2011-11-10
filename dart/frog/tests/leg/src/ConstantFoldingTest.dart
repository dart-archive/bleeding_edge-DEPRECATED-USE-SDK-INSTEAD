// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test constant folding.

#import("../../../leg/leg.dart");

class StringScript extends Script {
  final String code;
  StringScript(this.code) : super(null);
  String get text() => code;
}

final String STRING_FOLDING = """
void print(var obj) {}

void main() {
  var a = 'hello';
  var b = 'world';
  print(a + b);
}
""";

final String NUMBER_FOLDING = """
void print(var obj) {}

void main() {
  var a = 4;
  var b = 3;
  print(a + b);
}
""";


void compileAndTest(String code, RegExp regexp) {
  Compiler compiler = new Compiler(new StringScript(code));
  compiler.scanner.scan(compiler.script);
  String generated = compiler.compileMethod(Compiler.MAIN);
  Expect.isTrue(regexp.hasMatch(generated));
}

main() {
  compileAndTest(
      STRING_FOLDING, const RegExp("print\\('hello' \\+ 'world'\\)"));
  compileAndTest(
      NUMBER_FOLDING, const RegExp("print\\(7\\)"));
}
