// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Testing Bigints with and without intrinsics.
// VMOptions=
// VMOptions=--no_intrinsify

library big_integer_test;
import "package:expect/expect.dart";

foo() => 1234567890123456789;
bar() => 12345678901234567890;

testSmiOverflow() {
  var a = 1073741823;
  var b = 1073741822;
  Expect.equals(2147483645, a + b);
  a = -1000000000;
  b =  1000000001;
  Expect.equals(-2000000001, a - b);
  Expect.equals(-1000000001000000000, a * b);
}

testBigintAdd() {
  // Bigint and Smi.
  var a = 12345678901234567890;
  var b = 2;
  Expect.equals(12345678901234567892, a + b);
  Expect.equals(12345678901234567892, b + a);
  // Bigint and Bigint.
  a = 10000000000000000001;
  Expect.equals(20000000000000000002, a + a);
  // Bigint and double.
  a = 100000000000000000000.0;
  b = 200000000000000000000;
  Expect.isTrue((a + b) is double);
  Expect.equals(300000000000000000000.0, a + b);
  Expect.isTrue((b + a) is double);
  Expect.equals(300000000000000000000.0, b + a);
}

testBigintSub() {
  // Bigint and Smi.
  var a = 12345678901234567890;
  var b = 2;
  Expect.equals(12345678901234567888, a - b);
  Expect.equals(-12345678901234567888, b - a);
  // Bigint and Bigint.
  a = 10000000000000000001;
  Expect.equals(20000000000000000002, a + a);
  // Bigint and double.
  a = 100000000000000000000.0;
  b = 200000000000000000000;
  Expect.isTrue((a + b) is double);
  Expect.equals(-100000000000000000000.0, a - b);
  Expect.isTrue((b + a) is double);
  Expect.equals(100000000000000000000.0, b - a);
  Expect.equals(-1, 0xF00000000 - 0xF00000001);
}

testBigintMul() {
  // Bigint and Smi.
  var a = 12345678901234567890;
  var b = 10;
  Expect.equals(123456789012345678900, a * b);
  Expect.equals(123456789012345678900, b * a);
  // Bigint and Bigint.
  a = 12345678901234567890;
  b = 10000000000000000;
  Expect.equals(123456789012345678900000000000000000, a * b);
  // Bigint and double.
  a = 2.0;
  b = 200000000000000000000;
  Expect.isTrue((a * b) is double);
  Expect.equals(400000000000000000000.0, a * b);
  Expect.isTrue((b * a) is double);
  Expect.equals(400000000000000000000.0, b * a);
}

testBigintTruncDiv() {
  var a = 12345678901234567890;
  var b = 10;
  // Bigint and Smi.
  Expect.equals(1234567890123456789, a ~/ b);
  Expect.equals(0, b ~/ a);
  Expect.equals(123456789, 123456789012345678 ~/ 1000000000);
  // Bigint and Bigint.
  a = 12345678901234567890;
  b = 10000000000000000;
  Expect.equals(1234, a ~/ b);
  // Bigint and double.
  a = 100000000000000000000.0;
  b = 200000000000000000000;
  Expect.equals(0, a ~/ b);
  Expect.equals(2, b ~/ a);
}

testBigintDiv() {
  // Bigint and Smi.
  Expect.equals(1234567890123456789.1, 12345678901234567891 / 10);
  Expect.equals(0.000000001234, 1234 / 1000000000000);
  Expect.equals(12345678901234000000.0, 123456789012340000000 / 10);
  // Bigint and Bigint.
  var a = 12345670000000000000;
  var b = 10000000000000000;
  Expect.equals(1234.567, a / b);
  // Bigint and double.
  a = 400000000000000000000.0;
  b = 200000000000000000000;
  Expect.equals(2.0, a / b);
  Expect.equals(0.5, b / a);
}

testBigintModulo() {
  // Bigint and Smi.
  var a = 1000000000005;
  var b = 10;
  Expect.equals(5, a % b);
  Expect.equals(10, b % a);
  // Bigint & Bigint
  a = 10000000000000000001;
  b = 10000000000000000000;
  Expect.equals(1, a % b);
  Expect.equals(10000000000000000000, b % a);
  // Bigint & double.
  a = 10000000100000000.0;
  b = 10000000000000000;
  Expect.equals(100000000.0, a % b);
  Expect.equals(10000000000000000.0, b % a);
  // Transitioning from Mint to Bigint.
  var iStart = 4611686018427387900;
  var prevX = -23 % iStart;
  for (int i = iStart + 1; i < iStart + 10; i++) {
    var x = -23 % i;
    Expect.equals(1, x - prevX);
    Expect.isTrue(x > 0);
    prevX = x;
  }
}

testBigintModPow() {
  var x, e, m;
  x = 1234567890;
  e = 1000000001;
  m = 19;
  Expect.equals(11, x.modPow(e, m));
  x = 1234567890;
  e = 19;
  m = 1000000001;
  Expect.equals(122998977, x.modPow(e, m));
  x = 19;
  e = 1234567890;
  m = 1000000001;
  Expect.equals(619059596, x.modPow(e, m));
  x = 19;
  e = 1000000001;
  m = 1234567890;
  Expect.equals(84910879, x.modPow(e, m));
  x = 1000000001;
  e = 19;
  m = 1234567890;
  Expect.equals(872984351, x.modPow(e, m));
  x = 1000000001;
  e = 1234567890;
  m = 19;
  Expect.equals(0, x.modPow(e, m));
  x = 12345678901234567890;
  e = 10000000000000000001;
  m = 19;
  Expect.equals(2, x.modPow(e, m));
  x = 12345678901234567890;
  e = 19;
  m = 10000000000000000001;
  Expect.equals(3239137215315834625, x.modPow(e, m));
  x = 19;
  e = 12345678901234567890;
  m = 10000000000000000001;
  Expect.equals(4544207837373941034, x.modPow(e, m));
  x = 19;
  e = 10000000000000000001;
  m = 12345678901234567890;
  Expect.equals(11135411705397624859, x.modPow(e, m));
  x = 10000000000000000001;
  e = 19;
  m = 12345678901234567890;
  Expect.equals(2034013733189773841, x.modPow(e, m));
  x = 10000000000000000001;
  e = 12345678901234567890;
  m = 19;
  Expect.equals(1, x.modPow(e, m));
  x = 12345678901234567890;
  e = 19;
  m = 10000000000000000001;
  Expect.equals(3239137215315834625, x.modPow(e, m));
  x = 12345678901234567890;
  e = 10000000000000000001;
  m = 19;
  Expect.equals(2, x.modPow(e, m));
  x = 123456789012345678901234567890;
  e = 123456789012345678901234567891;
  m = 123456789012345678901234567899;
  Expect.equals(116401406051033429924651549616, x.modPow(e, m));
  x = 123456789012345678901234567890;
  e = 123456789012345678901234567899;
  m = 123456789012345678901234567891;
  Expect.equals(123456789012345678901234567890, x.modPow(e, m));
  x = 123456789012345678901234567899;
  e = 123456789012345678901234567890;
  m = 123456789012345678901234567891;
  Expect.equals(35088523091000351053091545070, x.modPow(e, m));
  x = 123456789012345678901234567899;
  e = 123456789012345678901234567891;
  m = 123456789012345678901234567890;
  Expect.equals(18310047270234132455316941949, x.modPow(e, m));
  x = 123456789012345678901234567891;
  e = 123456789012345678901234567899;
  m = 123456789012345678901234567890;
  Expect.equals(1, x.modPow(e, m));
  x = 123456789012345678901234567891;
  e = 123456789012345678901234567890;
  m = 123456789012345678901234567899;
  Expect.equals(40128068573873018143207285483, x.modPow(e, m));
}

testBigintNegate() {
  var a = 0xF000000000000000F;
  var b = ~a;  // negate.
  Expect.equals(-0xF0000000000000010, b);
  Expect.equals(0, a & b);
  Expect.equals(-1, a | b);
}

testShiftAmount() {
  Expect.equals(0, 12 >> 111111111111111111111111111111);
  Expect.equals(-1, -12 >> 111111111111111111111111111111);
  bool exceptionCaught = false;
  try {
    var a = 1 << 1111111111111111111111111111;
  } on OutOfMemoryError catch (e) {
    exceptionCaught = true;
  }
  Expect.equals(true, exceptionCaught);
}

main() {
  Expect.equals(1234567890123456789, foo());
  Expect.equals(12345678901234567890, bar());
  testSmiOverflow();
  testBigintAdd();
  testBigintSub();
  testBigintMul();
  testBigintTruncDiv();
  testBigintDiv();
  testBigintModulo();
  testBigintModPow();
  testBigintNegate();
  testShiftAmount();
  Expect.equals(12345678901234567890, (12345678901234567890).abs());
  Expect.equals(12345678901234567890, (-12345678901234567890).abs());
  var a = 10000000000000000000;
  var b = 10000000000000000001;
  Expect.equals(false, a.hashCode == b.hashCode);
  Expect.equals(true, a.hashCode == (b - 1).hashCode);
}
