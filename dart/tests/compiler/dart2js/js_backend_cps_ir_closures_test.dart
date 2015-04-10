// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// VMOptions=-DUSE_CPS_IR=true

// Tests of closures.

library closures_test;

import 'js_backend_cps_ir.dart';

const List<TestEntry> tests = const [
  const TestEntry("""
main(x) {
  a() {
    return x;
  }
  x = x + '1';
  print(a());
}
""",
r"""
function(x) {
  var _box_0 = {}, a;
  _box_0._captured_x_0 = x;
  a = new V.main_a(_box_0);
  x = _box_0._captured_x_0;
  _box_0._captured_x_0 = J.getInterceptor$ns(x).$add(x, "1");
  P.print(a.call$0());
  return null;
}"""),

  const TestEntry("""
main(x) {
  a() {
    return x;
  }
  print(a());
}
""",
r"""
function(x) {
  P.print(new V.main_a(x).call$0());
  return null;
}"""),

  const TestEntry("""
main() {
  var x = 122;
  var a = () => x;
  x = x + 1;
  print(a());
}
""",
r"""
function() {
  var _box_0 = {}, a, x;
  _box_0._captured_x_0 = 122;
  a = new V.main_closure(_box_0);
  x = _box_0._captured_x_0;
  _box_0._captured_x_0 = J.getInterceptor$ns(x).$add(x, 1);
  P.print(a.call$0());
  return null;
}"""),

  const TestEntry("""
main() {
  var x = 122;
  var a = () {
    var y = x;
    return () => y;
  };
  x = x + 1;
  print(a()());
}
""",
r"""
function() {
  var _box_0 = {}, a, x;
  _box_0._captured_x_0 = 122;
  a = new V.main_closure(_box_0);
  x = _box_0._captured_x_0;
  _box_0._captured_x_0 = J.getInterceptor$ns(x).$add(x, 1);
  P.print(a.call$0().call$0());
  return null;
}"""),

  const TestEntry("""
main() {
  var a;
  for (var i=0; i<10; i++) {
    a = () => i;
  }
  print(a());
}
""",
r"""
function() {
  var a = null, i = 0;
  while (P.identical(J.getInterceptor$n(i).$lt(i, 10), true)) {
    a = new V.main_closure(i);
    i = J.getInterceptor$ns(i).$add(i, 1);
  }
  P.print(a.call$0());
  return null;
}"""),

  const TestEntry.forMethod('function(A#b)', """
class A {
  a() => 1;
  b() => () => a();
}
main() {
  print(new A().b()());
}
""",
r"""
function() {
  return new V.A_b_closure(this);
}"""),
];

void main() {
  runTests(tests);
}
