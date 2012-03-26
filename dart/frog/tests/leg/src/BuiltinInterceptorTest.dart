// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("compiler_helper.dart");

final String TEST_ONE = @"""
foo(String a) {
  // index into the parameter to make sure we'll get a type guard.
  print(a[0]);
  return a.length;
}
""";

final String TEST_TWO = @"""
foo() {
  return "foo".length;
}
""";

final String TEST_THREE = @"""
foo() {
  return @"foo".length;
}
""";

main() {
  String generated = compile(TEST_ONE, 'foo');
  Expect.isTrue(generated.contains("return a.length;"));

  generated = compile(TEST_TWO, 'foo');
  Expect.isTrue(generated.contains("return 3;"));

  generated = compile(TEST_THREE, 'foo');
  Expect.isTrue(generated.contains("return 3;"));
}
