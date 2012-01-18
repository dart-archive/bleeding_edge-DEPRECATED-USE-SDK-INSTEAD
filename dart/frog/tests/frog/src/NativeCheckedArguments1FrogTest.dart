// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Test that type checks occur on native methods.

class A native "*A" {
  int foo(int x) native;
  int cmp(A other) native;
}

class B native "*B" {
  String foo(String x) native;
  int cmp(B other) native;
}

A makeA() native { return new A(); }
B makeB() native { return new B(); }

void setup() native """
function A() {}
A.prototype.foo = function (x) { return x + 1; };
A.prototype.cmp = function (x) { return 0; };

function B() {}
B.prototype.foo = function (x) { return x + 'ha!'; };
B.prototype.cmp = function (x) { return 0; };

makeA = function(){return new A;};
makeB = function(){return new B;};
""";

expectThrows(action()) {
  bool threw = false;
  try {
    action();
  } catch (var e) {
    threw = true;
  }
  Expect.isTrue(threw);
}

main() {
  setup();

  var things = [makeA(), makeB()];
  var a = things[0];
  var b = things[1];

  Expect.equals(124, a.foo(123));
  expectThrows(() => a.foo('xxx'));

  Expect.equals('helloha!', b.foo('hello'));
  expectThrows(() => b.foo(123));

  Expect.equals(0, a.cmp(a));
  expectThrows(() => a.cmp(b));
  expectThrows(() => a.cmp(5));

  Expect.equals(0, b.cmp(b));
  expectThrows(() => b.cmp(a));
  expectThrows(() => b.cmp(5));
}
