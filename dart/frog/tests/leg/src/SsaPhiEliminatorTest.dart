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

main() {
  String generated = compile(TEST_ONE, 'foo');
  RegExp regexp = const RegExp("a = 2");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("a = 3");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("print\\(a\\)");
  Expect.isTrue(regexp.hasMatch(generated));
}
