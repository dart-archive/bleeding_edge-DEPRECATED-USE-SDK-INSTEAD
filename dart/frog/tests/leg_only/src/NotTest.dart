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

void not1() {
  var x = !true;
  expectEquals(false, x);
}

void not2() {
  var x = true;
  var y = !x;
  expectEquals(false, y);
}

void not3() {
  var x = true;
  var y = !x;
  var z = !y;
  expectEquals(true, z);
}

void not4() {
  var x = true;
  if (!x) unreachable();
  expectEquals(true, x);
}

void main() {
  not1();
  not2();
  not3();
  not4();
}
