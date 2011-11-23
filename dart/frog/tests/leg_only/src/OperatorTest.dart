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
  expectEquals(0, 0 + 0);
  expectEquals(m1, m1 + 0);
  expectEquals(0, m1 + 1);
  expectEquals(499, 400 + 99);
  expectEquals(1, 0 + one());
  expectEquals(1, one() + 0);
  expectEquals(2, one() + one());
}

void subTest() {
  var m1 = 0 - 1;
  expectEquals(0, 0 - 0);
  expectEquals(m1, 0 - 1);
  expectEquals(0, 1 - 1);
  expectEquals(400, 499 - 99);
  expectEquals(m1, 0 - one());
  expectEquals(1, one() - 0);
  expectEquals(0, one() - one());
}

void mulTest() {
  var m1 = 0 - 1;
  expectEquals(0, 0 * 0);
  expectEquals(m1, m1 * 1);
  expectEquals(1, 1 * 1);
  expectEquals(49401, 499 * 99);
  expectEquals(499, 499 * one());
  expectEquals(499, one() * 499);
  expectEquals(49401, four99() * 99);
}

void divTest() {
  var m1 = 0.0 - 1.0;
  var m2 = 0 - 2;
  expectEquals(1.0, 2 / 2);
  expectEquals(m1, m2 / 2);
  expectEquals(0.5, 1 / 2);
  expectEquals(499.0, 49401 / 99);

  expectEquals(1.0, two() / 2);
  expectEquals(1.0, 2 / two());
  expectEquals(m1, m2 / two());
  expectEquals(m1, two() / m2);
  expectEquals(0.5, 1 / two());
  expectEquals(0.5, one() / 2);
  expectEquals(499.0, four99times99() / 99);
}

void tdivTest() {
  var m1 = 0 - 1;
  var m2 = 0 - 2;
  expectEquals(1, 2 ~/ 2);
  expectEquals(m1, m2 ~/ 2);
  expectEquals(0, 1 ~/ 2);
  expectEquals(0, m1 ~/ 2);
  expectEquals(499, 49401 ~/ 99);
  expectEquals(499, 49402 ~/ 99);

  expectEquals(1, two() ~/ 2);
  expectEquals(1, 2 ~/ two());
  expectEquals(m1, m2 ~/ two());
  expectEquals(m1, two() ~/ m2);
  expectEquals(0, 1 ~/ two());
  expectEquals(0, one() ~/ 2);
  expectEquals(499, four99times99() ~/ 99);
  expectEquals(499, four99times99plus1() ~/ 99);
}

void modTest() {
  var m5 = 0 - 5;
  var m3 = 0 - 3;
  expectEquals(2, 5 % 3);
  expectEquals(0, 49401 % 99);
  expectEquals(1, 49402 % 99);
  expectEquals(1, m5 % 3);
  expectEquals(2, 5 % m3);

  expectEquals(2, five() % 3);
  expectEquals(2, 5 % three());
  expectEquals(0, four99times99() % 99);
  expectEquals(1, four99times99plus1() % 99);
  expectEquals(1, minus5() % 3);
  expectEquals(2, five() % m3);
}

void equalsTest() {
  // Equality of normal numbers is already well tested with "expectEquals".
  expectEquals(false, 1 == 2);
  expectEquals(false, 1 == "foo");
  expectEquals(false, 1 == true);
  expectEquals(false, 1 == false);
  expectEquals(false, false == "");
  expectEquals(false, false == 0);
  // expectEquals(false, falseValue == null);
  expectEquals(false, "" == false);
  expectEquals(false, 0 == false);
  // expectEquals(false, null == falseValue);

  var falseValue = false;
  if (one() == 2) falseValue = true;

  expectEquals(false, one() == 2);
  expectEquals(false, one() == "foo");
  expectEquals(false, one() == true);
  expectEquals(false, one() == false);
  expectEquals(false, falseValue == "");
  expectEquals(false, falseValue == 0);
  // expectEquals(false, falseValue == null);
  expectEquals(false, "" == falseValue);
  expectEquals(false, 0 == falseValue);
  // expectEquals(false, null == falseValue);
}

void lessTest() {
  var m1 = minus1();
  expectEquals(true, 1 < 2);
  expectEquals(false, 2 < 1);
  expectEquals(false, 1 < 1);

  expectEquals(true, 0 < 1);
  expectEquals(false, 1 < 0);
  expectEquals(false, 0 < 0);

  expectEquals(true, one() < 2);
  expectEquals(false, 2 < one());
  expectEquals(false, 1 < one());

  expectEquals(true, 0 < one());
  expectEquals(false, one() < 0);
  expectEquals(false, 0 < 0);

  expectEquals(true, m1 < 0);
  expectEquals(false, 0 < m1);
  expectEquals(false, m1 < m1);
}

void lessEqualTest() {
  var m1 = minus1();
  expectEquals(true, 1 <= 2);
  expectEquals(false, 2 <= 1);
  expectEquals(true, 1 <= 1);

  expectEquals(true, 0 <= 1);
  expectEquals(false, 1 <= 0);
  expectEquals(true, 0 <= 0);

  expectEquals(true, one() <= 2);
  expectEquals(false, 2 <= one());
  expectEquals(true, 1 <= one());

  expectEquals(true, 0 <= one());
  expectEquals(false, one() <= 0);
  expectEquals(true, 0 <= 0);

  expectEquals(true, m1 <= 0);
  expectEquals(false, 0 <= m1);
  expectEquals(true, m1 <= m1);
}

void greaterTest() {
  var m1 = minus1();
  expectEquals(false, 1 > 2);
  expectEquals(true, 2 > 1);
  expectEquals(false, 1 > 1);

  expectEquals(false, 0 > 1);
  expectEquals(true, 1 > 0);
  expectEquals(false, 0 > 0);

  expectEquals(false, one() > 2);
  expectEquals(true, 2 > one());
  expectEquals(false, 1 > one());

  expectEquals(false, 0 > one());
  expectEquals(true, one() > 0);
  expectEquals(false, 0 > 0);

  expectEquals(false, m1 > 0);
  expectEquals(true, 0 > m1);
  expectEquals(false, m1 > m1);
}

void greaterEqualTest() {
  var m1 = minus1();
  expectEquals(false, 1 >= 2);
  expectEquals(true, 2 >= 1);
  expectEquals(true, 1 >= 1);

  expectEquals(false, 0 >= 1);
  expectEquals(true, 1 >= 0);
  expectEquals(true, 0 >= 0);

  expectEquals(false, one() >= 2);
  expectEquals(true, 2 >= one());
  expectEquals(true, 1 >= one());

  expectEquals(false, 0 >= one());
  expectEquals(true, one() >= 0);
  expectEquals(true, 0 >= 0);

  expectEquals(false, m1 >= 0);
  expectEquals(true, 0 >= m1);
  expectEquals(true, m1 >= m1);
}

void main() {
  addTest();
  subTest();
  mulTest();
  divTest();
  tdivTest();
  modTest();
  equalsTest();
  lessTest();
  lessEqualTest();
  greaterTest();
  greaterEqualTest();
}
