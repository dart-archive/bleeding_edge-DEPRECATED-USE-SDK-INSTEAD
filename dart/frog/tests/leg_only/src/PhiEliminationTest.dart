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

void bar() {
  var a = 0;
  var c = 0;

  if (a == 0) c = a++;
  else c = a--;

  expectEquals(1, a);
  expectEquals(0, c);

  if (a == 0) c = a++;
  else c = a--;

  expectEquals(0, a);
  expectEquals(1, c);
}

void foo() {
  var a = 0;
  var c = 0;

  if (a == 0) {
    c = a;
    a = a + 1;
  } else {
    c = a;
    a = a - 1;
  }

  expectEquals(1, a);
  expectEquals(0, c);

  if (a == 0) {
    c = a;
    a = a + 1;
  } else {
    c = a;
    a = a - 1;
  }

  expectEquals(0, a);
  expectEquals(1, c);
}

main() {
  foo();
  bar();
}
