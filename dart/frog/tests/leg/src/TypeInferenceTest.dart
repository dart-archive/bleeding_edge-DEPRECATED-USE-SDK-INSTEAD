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

String anyIdentifier = "[a-zA-Z][a-zA-Z0-9]*";

main() {
  String generated = compile(TEST_ONE, 'sum');
  RegExp regexp = new RegExp("i = \\($anyIdentifier \\+ 1\\)");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = new RegExp("sum = \\($anyIdentifier \\+ $anyIdentifier\\)");
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp('guard\\\$num\\(param0\\)');
  Expect.isTrue(regexp.hasMatch(generated));

  regexp = const RegExp('guard\\\$num\\(param1\\)');
  Expect.isTrue(regexp.hasMatch(generated));
}
