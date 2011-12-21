// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

void expectEquals(var expected, var actual) {
  if (expected != actual) {
    print("Actual does not match expected");
    throw actual;
  }
}

class A {
  static int a;
  A();
  static foo() {
    // Make sure 'A' is not resolved to the constructor.
    return A.a;
  }
}

main() {
  A.a = 42;
  expectEquals(42, A.foo());
}
