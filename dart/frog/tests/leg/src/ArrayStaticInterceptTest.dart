// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("compiler_helper.dart");

final String TEST_ONE = @"""

builtin$add$1(x) => null;
builtin$removeLast$0() { }

foo() {
  var a = [];
  a.add(42);
  a.removeLast();
}
""";

main() {
  String generated = compile(TEST_ONE, 'foo');
  Expect.isTrue(generated.contains(@'.builtin$add$1('));
  Expect.isTrue(generated.contains(@'.builtin$removeLast$0('));
}
