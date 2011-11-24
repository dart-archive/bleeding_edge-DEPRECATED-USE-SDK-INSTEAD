// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("compiler_helper.dart");

final String TEST_ONE = @"""
foo(a) {
  int c = foo(1);
  if (a) c = foo(2);
  return c;
}
""";

main() {
  String generated = compile(TEST_ONE, 'foo');
  RegExp regexp = const RegExp("var c = guard\\\$num\\(foo\\(1\\)\\);");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("c = guard\\\$num\\(foo\\(2\\)\\);");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("return c;");
  Expect.isTrue(regexp.hasMatch(generated));
}
