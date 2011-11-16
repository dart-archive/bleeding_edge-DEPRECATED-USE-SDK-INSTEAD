// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test constant folding.

#import("compiler_helper.dart");

final String STRING_FOLDING = """
void add(var a, var b) {}
void print(var obj) {}

void main() {
  var a = 'hello';
  var b = 'world';
  print(a + b);
}
""";

final String NUMBER_FOLDING = """
void add(var a, var b) {}
void print(var obj) {}

void main() {
  var a = 4;
  var b = 3;
  print(a + b);
}
""";


void compileAndTest(String code, String entry, RegExp regexp) {
  String generated = compile(code, entry);
  Expect.isTrue(regexp.hasMatch(generated));
}

main() {
  compileAndTest(
      STRING_FOLDING, 'main', const RegExp("print\\('hello' \\+ 'world'\\)"));
  compileAndTest(
      NUMBER_FOLDING, 'main', const RegExp("print\\(7\\)"));
}
