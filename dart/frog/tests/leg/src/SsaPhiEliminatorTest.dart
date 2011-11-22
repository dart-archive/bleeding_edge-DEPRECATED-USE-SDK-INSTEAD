// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test that parameters keep their names in the output.

#import("compiler_helper.dart");

final String TEST_ONE = @"""
void print() {}
void foo(bar) {
  var a = 1;
  if (bar) {
    a = 2;
  } else {
    a = 3;
  }
  print(a);
}
""";

final String TEST_TWO = @"""
void print() {}
void eq() {}
void add() {}

void main() {
  var t = 0;
  for (var i = 0; i == 0; i = i + 1) {
    t = t + 10;
  }
  print(t);
}
""";

final String TEST_THREE = @"""
bool lt() {}
foo(b, c) {
  var val = 42;
  if (b) {
    if (c) {
      val = 43;
    }
  }
  return val;
}
""";

main() {
  String generated = compile(TEST_ONE, 'foo');
  RegExp regexp = const RegExp("a = 2");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("a = 3");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("print\\(a\\)");
  Expect.isTrue(regexp.hasMatch(generated));

  generated = compile(TEST_TWO, 'main');
  regexp = const RegExp("t = \\(t +");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("i = \\(i +");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("print\\(t\\)");
  Expect.isTrue(regexp.hasMatch(generated));

  generated = compile(TEST_THREE, 'foo');
  // Check that we don't have additional 'val'.
  regexp = const RegExp("val_");
  Expect.isTrue(!regexp.hasMatch(generated));

  // Check that we don't have 'val = val'.
  regexp = const RegExp("val = val");
  Expect.isTrue(!regexp.hasMatch(generated));

  regexp = const RegExp("val = 43");
  Expect.isTrue(regexp.hasMatch(generated));
  regexp = const RegExp("return val");
  Expect.isTrue(regexp.hasMatch(generated));
}
