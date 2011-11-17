// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test that parameters keep their names in the output.

#import("compiler_helper.dart");

final String FOO = @"""
void foo(var a, var b) {
}
""";


final String BAR = @"""
void bar(var eval, var $eval) {
}
""";


final String PARAMETER_AND_TEMP = @"""
void print(var a) {}
void bar(var t0, var b) {
  {
    var t0 = 2;
    if (b) {
      t0 = 4;
    } else {
      t0 = 3;
    }
    print(t0);
  }
  print(t0);
}
""";

main() {
  String generated = compile(FOO, 'foo');
  // TODO(ngeoffray): Use 'contains' when frog supports it.
  RegExp regexp = const RegExp("function foo\\(a, b\\) {");
  Expect.isTrue(regexp.hasMatch(generated));

  generated = compile(BAR, 'bar');
  regexp = const RegExp("function bar\\(\\\$eval, _\\\$eval\\) {");
  Expect.isTrue(regexp.hasMatch(generated));

  generated = compile(PARAMETER_AND_TEMP, 'bar');
  regexp = const RegExp("print\\(t0\\)");
  Expect.isTrue(regexp.hasMatch(generated));
  // Check that the second 't0' got another name.
  regexp = const RegExp("print\\(t0_0\\)");
  Expect.isTrue(regexp.hasMatch(generated));
}
