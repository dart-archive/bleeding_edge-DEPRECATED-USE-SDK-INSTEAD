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
  A() {
  }
  int x;

  foo() {
    x = 42;
    expectEquals(42, x);
    x = 0;
    expectEquals(0, x);
  }
}

class B extends A {
}

main() {
  A a = new A();
  a.foo();
  expectEquals(0, a.x);
  a.x = 4;
  expectEquals(4, a.x);
  a.x += 1;
  expectEquals(5, a.x);

  B b = new B();
  b.foo();
  expectEquals(0, b.x);
}
