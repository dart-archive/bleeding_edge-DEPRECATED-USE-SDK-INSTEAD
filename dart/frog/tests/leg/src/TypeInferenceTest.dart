// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("compiler_helper.dart");

final String TEST_ONE = @"""
sum(param0, param1) {
  var sum = 0;
  for (var i = param0; i < param1; i += 1) sum = sum + i;
  return sum;
}
""";

final String TEST_TWO = @"""
foo(int param0) {
  return -param0;
}
""";

final String TEST_THREE = @"""
foo(c) {
  for (int i = 0; i < 10; i++) print(c[i]);
}
""";

final String TEST_FOUR = @"""
foo(String c) {
  print(c[0]); // Force a type guard.
  while (true) print(c.length);
}
""";

final String TEST_FIVE = @"""
foo(a) {
  a[0] = 1;
  print(a[1]);
}
""";

final String TEST_SIX = @"""
main(a) {
  print(a[0]);
  while (true) {
    a[0] = a[1];
  }
}
""";

main() {
  String generated = compile(TEST_ONE, 'sum');
  RegExp regexp = new RegExp("i = \\(?$anyIdentifier \\+ \\(1\\)\\)?");
  // TODO(ngeoffray): Do live range analysis to make this test pass.
  Expect.isFalse(regexp.hasMatch(generated));

  regexp = new RegExp("sum = \\(?$anyIdentifier \\+ $anyIdentifier\\)?");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("typeof param0 !== 'number'");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("typeof param1 !== 'number'");
  Expect.isTrue(regexp.hasMatch(generated));

  generated = compile(TEST_TWO, 'foo');
  regexp = new RegExp(getNumberTypeCheck('param0'));
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp('-param0');
  Expect.isTrue(regexp.hasMatch(generated));

  generated = compile(TEST_THREE, 'foo');
  regexp = new RegExp("c[$anyIdentifier]");
  Expect.isTrue(regexp.hasMatch(generated));

  generated = compile(TEST_FOUR, 'foo');
  regexp = new RegExp("c.length");
  Expect.isTrue(regexp.hasMatch(generated));

  generated = compile(TEST_FIVE, 'foo');
  regexp = const RegExp('a.constructor !== Array');
  Expect.isTrue(regexp.hasMatch(generated));
  Expect.isTrue(!generated.contains('index'));
  Expect.isTrue(!generated.contains('indexSet'));

  generated = compile(TEST_FIVE, 'foo');
  regexp = const RegExp('a.constructor !== Array');
  Expect.isTrue(regexp.hasMatch(generated));
  Expect.isTrue(!generated.contains('index'));
  Expect.isTrue(!generated.contains('indexSet'));
}
