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

int zero() { return 0; }
int one() { return 1; }
int minus1() { return 0 - 1; }
int two() { return 2; }
int three() { return 3; }
int five() { return 5; }
int minus5() { return 0 - 5; }
int ninetyNine() { return 99; }
int four99() { return 499; }
int four99times99() { return 499 * 99; }
int four99times99plus1() { return 499 * 99 + 1; }

void addTest() {
  var m1 = 0 - 1;
  var x = 0;
  x += 0;
  expectEquals(0, x);
  x += one();
  expectEquals(1, x);
  x += m1;
  expectEquals(0, x);
  x += 499;
  expectEquals(499, x);
}

void subTest() {
  var m1 = 0 - 1;
  var x = 0;
  x -= 0;
  expectEquals(0, x);
  x -= one();
  expectEquals(m1, x);
  x -= m1;
  expectEquals(0, x);
  x = 499;
  x -= one();
  x -= 98;
  expectEquals(400, x);
}

void mulTest() {
  var m1 = 0 - 1;
  var x = 0;
  x *= 0;
  expectEquals(0, x);
  x = one();
  x *= 1;
  expectEquals(1, x);
  x *= four99();
  expectEquals(499, x);
  x *= m1;
  expectEquals(0 - 499, x);
}

void divTest() {
  var m1 = 0.0 - 1.0;
  var m2 = 0 - 2;
  var x = two();
  x /= 2;
  expectEquals(1.0, x);
  x /= 2;
  expectEquals(0.5, x);
  x = four99times99();
  x /= 99;
  expectEquals(499.0, x);
}

void tdivTest() {
  var x = 3;
  x ~/= two();
  expectEquals(1, x);
  x = 49402;
  x ~/= ninetyNine();
  expectEquals(499, x);
}

void modTest() {
  var x = five();
  x %= 3;
  expectEquals(2, x);
  x = 49402;
  x %= ninetyNine();
  expectEquals(1, x);
}

void shlTest() {
  var x = five();
  x <<= 2;
  expectEquals(20, x);
  x <<= 1;
  expectEquals(40, x);
}

void shrTest() {
  var x = four99();
  x >>= 1;
  expectEquals(249, x);
  x >>= 2;
  expectEquals(62, x);
}

void andTest() {
  var x = five();
  x &= 3;
  expectEquals(1, x);
  x &= 10;
  expectEquals(0, x);
  x = four99();
  x &= 63;
  expectEquals(51, x);
}

void orTest() {
  var x = five();
  x |= 2;
  expectEquals(7, x);
  x |= 7;
  expectEquals(7, x);
  x |= 10;
  expectEquals(15, x);
  x |= 499;
  expectEquals(511, x);
}

void xorTest() {
  var x = five();
  x ^= 2;
  expectEquals(7, x);
  x ^= 7;
  expectEquals(0, x);
  x ^= 10;
  expectEquals(10, x);
  x ^= 499;
  expectEquals(505, x);
}

void main() {
  addTest();
  subTest();
  mulTest();
  divTest();
  tdivTest();
  modTest();
  shlTest();
  shrTest();
  andTest();
  orTest();
  xorTest();
}
