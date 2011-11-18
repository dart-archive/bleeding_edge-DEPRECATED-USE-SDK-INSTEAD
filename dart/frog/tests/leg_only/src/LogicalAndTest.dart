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

void and1() {
  var b = true;
  b = b && b;
  expectEquals(true, b);
}

void and2() {
  var b = false;
  b = b && b;
  expectEquals(false, b);
}

void and3() {
  var b = true;
  b = b && false;
  expectEquals(false, b);
}

void and4() {
  var b = true;
  b = b && true;
  expectEquals(true, b);
}

void and5() {
  if (true && false) unreachable();
}

void and6() {
  var b = true;
  if (true && true) b = false;
  expectEquals(false, b);
}

void and7() {
  var b = false;
  if (true && false) {
    b = false;
  } else {
    b = true;
  }
  expectEquals(true, b);
}

void main() {
  and1();
  and2();
  and3();
  and4();
  and5();
  and6();
  and7();
}
