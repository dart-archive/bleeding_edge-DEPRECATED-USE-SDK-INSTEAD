// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

void expectEquals(var expected, var actual) {
  if (expected == actual) {
  } else {
    print("Actual does not match expected");
    throw actual;
  }
}

class A {
  foo() { return 499; }
  bar(x) { return x + 499; }
  baz() { return 54; }
}

class B {
  foo() { return 42; }
  bar(x) { return x + 42; }
}

class C extends A {
  foo() { return 99; }
  bar(x) { return x + 99; }
}

void main() {
  var a = new A();
  expectEquals(499, a.foo());
  expectEquals(500, a.bar(1));
  var b = new B();
  expectEquals(42, b.foo());
  expectEquals(43, b.bar(1));
  var c = new C();
  expectEquals(99, c.foo());
  expectEquals(100, c.bar(1));

  expectEquals(54, a.baz());
  expectEquals(54, c.baz());
}
