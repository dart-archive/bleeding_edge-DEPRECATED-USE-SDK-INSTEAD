// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Tests basic expressions. Does not attempt to validate the details of arithmetic, coercion, and
// so forth.
class ExpressionTest {

  ExpressionTest() {}

  int foo;

  static main() {
    var test = new ExpressionTest();
    test.testBinary();
    test.testUnary();
    test.testShifts();
    test.testBitwise();
    test.testIncrement();
    test.testMangling();
  }

  testBinary() {
    int x = 4, y = 2;
    assert(x + y == 6);
    assert(x - y == 2);
    assert(x * y == 8);
    assert(x / y == 2);
    assert(x % y == 0);
  }

  testUnary() {
    int x = 4, y = 2;
    bool t = true, f = false;
    assert(-x == -4);
    assert(~x == -5);
    assert(!t == f);
  }

  testShifts() {
    int x = 4, y = 2;
    assert(x >> 1 == y);
    assert(y << 1 == x);
  }

  testBitwise() {
    int x = 4, y = 2;
    assert((x | y) == 6);
    assert((x & y) == 0);
    assert((x ^ y) == 6);
  }

  operator [](int index) {
    return foo;
  }

  operator []=(int index, int value) {
    foo = value;
  }

  testIncrement() {
    int x = 4, a = x++;
    assert(a == 4);
    assert(x == 5);
    assert(++x == 6);
    assert(x++ == 6);
    assert(x == 7);
    assert(--x == 6);
    assert(x-- == 6);
    assert(x == 5);

    this.foo = 0;
    assert(this.foo++ == 0);
    assert(this.foo == 1);
    assert(++this.foo == 2);
    assert(this.foo == 2);
    assert(this.foo-- == 2);
    assert(this.foo == 1);
    assert(--this.foo == 0);
    assert(this.foo == 0);

    assert(this[0]++ == 0);
    assert(this[0] == 1);
    assert(++this[0] == 2);
    assert(this[0] == 2);
    assert(this[0]-- == 2);
    assert(this[0] == 1);
    assert(--this[0] == 0);
    assert(this[0] == 0);

    int $0 = 42, $1 = 87, $2 = 117;
    assert($0++ == 42);
    assert($0 == 43);
    assert(++$0 == 44);
    assert(($0 += $0) == 88);
    assert($1++ == 87);
    assert($1 == 88);
    assert(++$1 == 89);
    assert(($1 += $1) == 178);
    assert($2++ == 117);
    assert($2 == 118);
    assert(++$2 == 119);
  }

  void testMangling() {
    int $0 = 42, $1 = 87, $2 = 117;
    this[0] = 0;
    assert((this[0] += $0) == 42);
    assert((this[0] += $1) == 129);
    assert((this[0] += $2) == 246);
  }
}
