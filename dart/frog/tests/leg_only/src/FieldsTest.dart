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
    x = 42;
  }
  int x;

  foo() {
    expectEquals(x, 42);
    x = 0;
    expectEquals(x, 0);
  }
}

main() {
  A a = new A();
  a.foo();
}
