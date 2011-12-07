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

main() {
  var a = [0];
  expectEquals(0, a[0]);

  a = [1, 2];
  expectEquals(1, a[0]);
  expectEquals(2, a[1]);

  a[0] = 42;
  expectEquals(42, a[0]);
  expectEquals(2, a[1]);

  a[1] = 43;
  expectEquals(42, a[0]);
  expectEquals(43, a[1]);

  a[1] += 2;
  expectEquals(45, a[1]);
  a[1] -= a[1];
  expectEquals(0, a[1]);
}
