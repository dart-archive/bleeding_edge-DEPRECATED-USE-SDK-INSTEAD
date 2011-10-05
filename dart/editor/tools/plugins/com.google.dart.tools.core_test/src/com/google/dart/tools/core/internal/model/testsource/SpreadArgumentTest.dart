// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class A {
  A() {}
  bar(a, b, c) {
    assert(a == 10);
    assert(b == 11);
    assert(c == 12);
    return 0;
  }
}

class SpreadArgumentTest extends A {
  SpreadArgumentTest() : super() {}

  int bar(a, b, c) {
    assert(a == 0);
    assert(b == 1);
    assert(c == 2);
    return 1;
  }

  void superBar(mode) {
    if (mode == 0) {
      return super.bar(10, 11, ... [12]);
    } else {
      return super.bar(...[10, 11, 12]);
    }
  }

  static void main(args) {
    var test = new SpreadArgumentTest();
    var a = [1, 2];
    var b = [0, 1, 2];
    var returnValue = test.bar(0, ...a);
    assert(returnValue == 1);
    returnValue = test.bar(... b);
    assert(returnValue == 1);
    returnValue = test.superBar(0);
    assert(returnValue == 0);
    returnValue = test.superBar(1);
    assert(returnValue == 0);
  }
}
