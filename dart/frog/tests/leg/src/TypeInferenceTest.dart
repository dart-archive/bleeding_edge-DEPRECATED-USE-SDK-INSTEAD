// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("compiler_helper.dart");

final String TEST_ONE = @"""
lt() {}
add() {}
guard$num(x) { return true; }
sum(param0, param1) {
  var sum = 0;
  for (var i = param0; i < param1; i += 1) sum = sum + i;
  return sum;
}
""";

main() {
  String generated = compile(TEST_ONE, 'sum');
  RegExp regexp = const RegExp("i = \\(i \\+ 1\\)");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp("sum = \\(sum \\+ i\\)");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp('guard\\\$num\\(param0\\)');
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp('guard\\\$num\\(param1\\)');
  Expect.isTrue(regexp.hasMatch(generated));
}
