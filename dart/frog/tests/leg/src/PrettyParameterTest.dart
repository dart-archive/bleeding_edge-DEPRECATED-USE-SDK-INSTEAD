// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test that parameters keep their names in the output.

#import("compiler_helper.dart");

final String FOO = """
void foo(var a, var b) {
}
""";


final String BAR = """
void bar(var eval) {
}
""";

main() {
  String generated = compile(FOO, 'foo');
  // TODO(ngeoffray): Use 'contains' when frog supports it.
  RegExp regexp = const RegExp("function foo\\(a, b\\) {");
  Expect.isTrue(regexp.hasMatch(generated));

  generated = compile(BAR, 'bar');
  regexp = const RegExp("function bar\\(_eval\\) {");
  Expect.isTrue(regexp.hasMatch(generated));
}
