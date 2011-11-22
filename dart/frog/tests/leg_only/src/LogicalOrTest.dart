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

void or1() {
  var b = true;
  b = b || b;
  expectEquals(true, b);
}

void or2() {
  var b = false;
  b = b || b;
  expectEquals(false, b);
}

void or3() {
  var b = true;
  b = b || false;
  expectEquals(true, b);
}

void or4() {
  var b = true;
  b = b || true;
  expectEquals(true, b);
}

void or5() {
  if (true || false) {
  } else {
    unreachable();
  }
}

void or6() {
  var b = true;
  if (true || true) b = false;
  expectEquals(false, b);
}

void or7() {
  var b = false;
  if (true || false) {
    b = true;
  } else {
    b = false;
  }
  expectEquals(true, b);
}

void main() {
  or1();
  or2();
  or3();
  or4();
  or5();
  or6();
  or7();
}
