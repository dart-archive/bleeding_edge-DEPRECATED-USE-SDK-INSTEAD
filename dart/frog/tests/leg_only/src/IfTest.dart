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

int if1() {
  if (true) {
    return 499;
  }
  return 3;
}

int if2() {
  if (true) {
    return 499;
  }
}

int if3() {
  if (false) {
    return 42;
  } else {
    if (true) {
      return 499;
    }
    unreachable();
  }
}

int if4() {
  if (true) {
    return 499;
  } else {
    return 42;
  }
}

void main() {
  expectEquals(499, if1());
  expectEquals(499, if2());
  expectEquals(499, if3());
  expectEquals(499, if4());
}
