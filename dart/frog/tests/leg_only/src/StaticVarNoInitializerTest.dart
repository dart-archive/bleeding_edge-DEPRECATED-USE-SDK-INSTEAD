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

void unreachable() {
  throw "unreachable";
}

int one;
int x;

void testOne() {
  expectEquals(1, one);
}

void testX(var expected) {
  expectEquals(expected, x);
}

void increaseX() {
  x = x + 1;
}

void main() {
  one = 1;
  x = 5;
  expectEquals(1, one);
  testOne();
  expectEquals(5, x);
  testX(5);
  x = x + 1;
  expectEquals(6, x);
  testX(6);
  increaseX();
  expectEquals(7, x);
  testX(7);
}
