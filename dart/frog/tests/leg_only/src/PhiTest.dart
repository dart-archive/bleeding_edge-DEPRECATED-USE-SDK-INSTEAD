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

void phi1() {
  var x = 42;
  if (true) {
    expectEquals(42, x);
    print(x);
  }
  expectEquals(42, x);
  print(x);
}

void phi2() {
  var x = 499;
  if (true) {
    expectEquals(499, x);
    x = 42;
  }
  expectEquals(42, x);
  print(x);
}

void phi3() {
  var x = 499;
  if (true) {
    expectEquals(499, x);
    x = 42;
  } else {
    unreachable();
    print(x);
  }
  expectEquals(42, x);
  print(x);
}

void phi4() {
  var x = 499;
  if (true) {
    expectEquals(499, x);
    print(x);
  } else {
    unreachable();
    x = 42;
  }
  expectEquals(499, x);
  print(x);
}

void phi5() {
  var x = 499;
  if (true) {
    if (true) {
      expectEquals(499, x);
      x = 42;
    }
  }
  expectEquals(42, x);
  print(x);
}

void phi6() {
  var x = 499;
  if (true) {
    if (true) {
      expectEquals(499, x);
      print(x);
    } else {
      x = 42;
      unreachable();
    }
  }
  expectEquals(499, x);
  print(x);
}

void phi7() {
  var x = 499;
  if (true) {
    x = 42;
    if (true) {
      expectEquals(42, x);
      x = 99;
    } else {
      x = 111;
      unreachable();
    }
  } else {
    unreachable();
    if (false) {
      x = 341;
    } else {
      x = 1024;
    }
  }
  expectEquals(99, x);
  print(x);
}

void phi8() {
  var x = 499;
  if (true) {
    x = 42;
    if (true) {
      expectEquals(42, x);
      x = 99;
    } else {
      unreachable();
      x = 111;
    }
  } else {
    unreachable();
    if (false) {
      x = 341;
    } else {
      x = 1024;
    }
  }
  if (true) {
    expectEquals(99, x);
    x = 12342;
    if (true) {
      x = 12399;
    } else {
      unreachable();
      x = 123111;
    }
  } else {
    unreachable();
    if (false) {
      x = 123341;
    } else {
      x = 1231024;
    }
  }
  expectEquals(12399, x);
  print(x);
}

void phi9() {
  var x = 499;
  if (true) {
    var y = 42;
    if (true) {
      y = 99;
    } else {
      unreachable();
      x = 111;
    }
    expectEquals(99, y);
    print(y);
  }
  expectEquals(499, x);
  print(x);
}

void main() {
  phi1();
  phi2();
  phi3();
  phi4();
  phi5();
  phi6();
  phi7();
  phi8();
  phi9();
}
