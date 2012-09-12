// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class OperatorTest {
  static int i1, i2;

  OperatorTest() {}

  static main() {
    var op1 = new Operator(1);
    var op2 = new Operator(2);
    assert(3 == op1 + op2);
    assert(-1 == op1 - op2);
    assert(0.5 == op1 / op2);
    assert(0 == op1 ~/ op2);
    assert(2 == op1 * op2);
    assert(1 == op1 % op2);
    assert(!(op1 == op2));
    assert(op1 < op2);
    assert(!(op1 > op2));
    assert(op1 <= op2);
    assert(!(op1 >= op2));
    assert(3 == (op1 | op2));
    assert(3 == (op1 ^ op2));
    assert(0 == (op1 & op2));
    assert(4 == (op1 << op2));
    assert(0 == (op1 >> op2));
    assert(~1 == ~op1);
    assert(-1 == -op1);

    op1.value += op2.value;
    assert(op1.value == 3);

    op2.value += (op2.value += op2.value);
    assert(6 == op2.value);

    op2.value -= (op2.value -= op2.value);
    assert(6 == op2.value);

    op1.value = op2.value = 42;
    assert(op1.value == 42);
    assert(op2.value == 42);

    i1 = i2 = 42;
    assert(i1 == 42);
    assert(i2 == 42);
    i1 += 7;
    assert(i1 == 49);
    i1 += (i2 = 17);
    assert(i1 == 66);
    assert(i2 == 17);

    i1 += i2 += 3;
    assert(i1 == 86);
    assert(i2 == 20);
  }
}

class Operator {
  int value;

  Operator(int i) {
    value = i;
  }

  operator +(Operator other) {
    return value + other.value;
  }

  operator -(Operator other) {
    return value - other.value;
  }

  operator /(Operator other) {
    return value / other.value;
  }

  operator *(Operator other) {
    return value * other.value;
  }

  operator %(Operator other) {
    return value % other.value;
  }

  operator ==(Operator other) {
    return value == other.value;
  }

  operator <(Operator other) {
    return value < other.value;
  }

  operator >(Operator other) {
    return value > other.value;
  }

  operator <=(Operator other) {
    return value <= other.value;
  }

  operator >=(Operator other) {
    return value >= other.value;
  }

  operator |(Operator other) {
    return value | other.value;
  }

  operator ^(Operator other) {
    return value ^ other.value;
  }

  operator &(Operator other) {
    return value & other.value;
  }

  operator <<(Operator other) {
    return value << other.value;
  }

  operator >>(Operator other) {
    return value >> other.value;
  }

  operator ~/(Operator other) {
    return value ~/ other.value;
  }

  operator ~() {
    return ~value;
  }

  operator -() {
    return -value;
  }
}
