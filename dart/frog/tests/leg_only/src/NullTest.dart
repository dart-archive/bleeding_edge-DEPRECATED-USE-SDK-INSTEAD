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

null1() {
  return;  // implicit null;
}

null2() {
  // Implicit null return.
}

null3() {
  var x;  // Implicit null value.
  return x;
}

void main() {
  expectEquals(null, null1());
  expectEquals(null, null2());
  expectEquals(null, null3());
}
