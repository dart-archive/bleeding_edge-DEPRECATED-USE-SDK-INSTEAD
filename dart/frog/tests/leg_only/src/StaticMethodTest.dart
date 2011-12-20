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
  static foo(res) {
    return res;
  }

  bar(res) {
    return foo(res);
  }

  bar2(res) {
    return A.foo(res);
  }
}

main() {
  A a = new A();
  expectEquals(42, a.bar(42));
  expectEquals(43, a.bar2(43));
}
