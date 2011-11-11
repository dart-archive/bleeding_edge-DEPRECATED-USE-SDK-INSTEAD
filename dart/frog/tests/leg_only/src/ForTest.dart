// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

void expectEquals(expected, actual) {
  if (expected == actual) {
    // Nothing to do.
  } else {
    print("Actual not equal to expected");
    print(actual);
    print(expected);
    throw "expectEquals failed";
  }
}

void unreachable() {
  throw "Unreachable";
}

void for1() {
  var cond = true;
  var result = 0;
  for (var x = 0; cond; x = x + 1) {
    if (x == 10) cond = false;
    result = result + x;
  }
  expectEquals(55, result);
}

void for2() {
  var t = 0;
  for (var i = 0; i == 0; i = i + 1) {
    t = t + 10;
  }
  expectEquals(10, t);
}

void for3() {
  for (var i = 0; i == 1; i = i + 1) {
    unreachable();
  }
}

void for4() {
  var cond1 = true;
  var result = 0;
  for (var i = 0; cond1; i = i + 1) {
    if (i == 9) cond1 = false;
    var cond2 = true;
    for (var j = 0; cond2; j = j + 1) {
      if (j == 9) cond2 = false;
      result = result + 1;
    }
  }
  expectEquals(100, result);
}

void main() {
  for1();
  for2();
  for3();
  for4();
}
