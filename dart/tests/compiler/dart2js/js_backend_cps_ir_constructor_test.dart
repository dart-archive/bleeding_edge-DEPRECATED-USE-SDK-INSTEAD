// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// VMOptions=-DUSE_CPS_IR=true

// Tests of interceptors.

library constructor_test;

import 'js_backend_cps_ir.dart';

const List<TestEntry> tests = const [
  const TestEntry.forMethod('generative_constructor(Sub#)', """
class Base {
  var x;
  Base(this.x);
}
class Sub extends Base {
  var y;
  Sub(x, this.y) : super(x);
}
main() {
  print(new Sub(1, 2).x);
}""",
r"""
function(x, y) {
  return new V.Sub(y, x);
}"""),

  const TestEntry.forMethod('generative_constructor_body(Sub#)', """
class Base {
  var x;
  Base(this.x);
}
class Sub extends Base {
  var y;
  Sub(x, this.y) : super(x) {
    print(x);
  }
}
main() {
  print(new Sub(1, 2).x);
}""",
r"""
function(x, y) {
  P.print(x);
  return null;
}"""),

  const TestEntry.forMethod('generative_constructor(Sub#)', """
class Base0 {
  Base0() {
    print('Base0');
  }
}
class Base extends Base0 {
  var x;
  Base(this.x);
}
class Sub extends Base {
  var y;
  Sub(x, this.y) : super(x) {
    print(x);
  }
}
main() {
  print(new Sub(1, 2).x);
}""",
r"""
function(x, y) {
  var v0;
  v0 = new V.Sub(y, x);
  v0.Base0$0();
  v0.Sub$2(x, y);
  return v0;
}"""),

  const TestEntry.forMethod('generative_constructor(Sub#)', """
class Base0 {
  Base0() {
    print('Base0');
  }
}
class Base extends Base0 {
  var x;
  Base(x1) : x = (() => ++x1) {
    print(x1); // use boxed x1
  }
}
class Sub extends Base {
  var y;
  Sub(x, this.y) : super(x) {
    print(x);
  }
}
main() {
  print(new Sub(1, 2).x);
}""",
r"""
function(x, y) {
  var box_0, v0;
  box_0 = {};
  box_0.x1_0 = x;
  v0 = new V.Sub(y, new V.Base_closure(box_0));
  v0.Base0$0();
  v0.Base$1(box_0);
  v0.Sub$2(x, y);
  return v0;
}"""),

  const TestEntry.forMethod('generative_constructor(Sub#)', """
foo(x) {
  print(x);
}
class Base {
  var x1 = foo('x1');
  var x2;
  var x3 = foo('x3');
  Base() : x2 = foo('x2');
}
class Sub extends Base {
  var y1 = foo('y1');
  var y2;
  var y3;
  Sub() : y2 = foo('y2'), super(), y3 = foo('y3');
}
main() {
  new Sub();
}
""",
r"""
function() {
  var v0, v1, v2, v3, v4;
  v0 = V.foo("y1");
  v1 = V.foo("y2");
  v2 = V.foo("x1");
  v3 = V.foo("x3");
  v4 = V.foo("x2");
  return new V.Sub(v0, v1, V.foo("y3"), v2, v4, v3);
}"""),


  const TestEntry.forMethod('generative_constructor(Foo#)', """
class Bar {
  Bar(x, {y, z: 'z', w: '_', q}) {
    print(x);
    print(y);
    print(z);
    print(w);
    print(q);
  }
}
class Foo extends Bar {
  Foo() : super('x', y: 'y', w: 'w');
}
main() {
  new Foo();
}
""",
r"""
function() {
  var x, y, w, z, q, v0;
  x = "x";
  y = "y";
  w = "w";
  z = "z";
  q = null;
  v0 = new V.Foo();
  v0.Bar$5$q$w$y$z(x, q, w, y, z);
  return v0;
}"""),
];

void main() {
  runTests(tests);
}
