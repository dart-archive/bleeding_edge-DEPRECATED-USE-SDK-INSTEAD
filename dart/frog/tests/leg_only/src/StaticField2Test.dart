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
  static int b;
}

main() {
  A.b = 42;
  expectEquals(42, A.b);
}
