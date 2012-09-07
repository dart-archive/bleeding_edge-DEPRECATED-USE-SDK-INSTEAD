// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class A {
  A() { NamingTest.count++; }
  foo(a, b) {
    assert(a == 1);
    assert(b == 2);
  }
}

class MyException {
  MyException() {}
}

class debugger {
  static final int __PROTO__ = 5;

  int x;

  factory debugger.F() {
    return new debugger(1);
  }
  debugger(x) : x(x + 1) { }
  debugger.C(x) : x(x + 2) { }
  debugger.C$C(x) : x(x + 3) { }
  debugger.C$I(x) : x(x + 4) { }
}

class debugger$C {
  int x;

  factory debugger$C.F() {
    return new debugger$C(1);
  }
  debugger$C(x) : x(x + 5) { }
  debugger$C.C(x) : x(x + 6) { }
  debugger$C.C$C(x) : x(x + 7) { }
  debugger$C.C$I(x) : x(x + 8) { }
}

class debugger$C$C {
  int x;

  factory debugger$C$C.F() {
    return new debugger$C$C(1);
  }
  debugger$C$C(x) : x(x + 9) { }
  debugger$C$C.C(x) : x(x + 10) { }
  debugger$C$C.C$C(x) : x(x + 11) { }
  debugger$C$C.C$I(x) : x(x + 12) { }
}

class with extends debugger$C {
  int y;

  factory with.F() {
    return new with(1, 2);
  }
  with(x, y) : super(x), y(y + 1) { }
  with.I(x, y) : super.C(x), y(y + 2) { }
  with.C(x, y) : super.C$C(x), y(y + 3) { }
  with.I$C(x, y) : super.C$I(x), y(y + 4) { }
  with.C$C(x, y) : super(x), y(y + 5) { }
  with.C$C$C(x, y) : super.C(x), y(y + 6) { }
  with.$C$I(x, y) : super.C$C(x), y(y + 7) { }
  with.$$I$C(x, y) : super.C$I(x), y(y + 8) { }
  with.$(x, y) : super(x), y(y + 9) { }
  with.$$(x, y) : super.C(x), y(y + 10) { }
}

class with$I extends debugger$C {
  int y;

  factory with$I.F() {
    return new with$I(1, 2);
  }
  with$I(x, y) : super(x), y(y + 11) { }
  with$I.I(x, y) : super.C(x), y(y + 12) { }
  with$I.C(x, y) : super.C$C(x), y(y + 13) { }
  with$I.I$C(x, y) : super.C$I(x), y(y + 14) { }
  with$I.C$C(x, y) : super(x), y(y + 15) { }
  with$I.C$C$C(x, y) : super.C(x), y(y + 16) { }
  with$I.$C$I(x, y) : super.C$C(x), y(y + 17) { }
  with$I.$$I$C(x, y) : super.C$I(x), y(y + 18) { }
  with$I.$(x, y) : super(x), y(y + 19) { }
  with$I.$$(x, y) : super.C(x), y(y + 20) { }
}

class with$C extends debugger$C$C {
  int y;

  factory with$C.F() {
    return new with$C(1, 2);
  }
  with$C(x, y) : super(x), y(y + 21) { }
  with$C.I(x, y) : super.C(x), y(y + 22) { }
  with$C.C(x, y) : super.C$C(x), y(y + 23) { }
  with$C.I$C(x, y) : super.C$I(x), y(y + 24) { }
  with$C.C$C(x, y) : super(x), y(y + 25) { }
  with$C.C$C$C(x, y) : super.C(x), y(y + 26) { }
  with$C.$C$I(x, y) : super.C$C(x), y(y + 27) { }
  with$C.$$I$C(x, y) : super.C$I(x), y(y + 28) { }
  with$C.$(x, y) : super(x), y(y + 29) { }
  with$C.$$(x, y) : super.C(x), y(y + 30) { }
}

class with$I$C extends debugger$C$C {
  int y;

  factory with$I$C.F() {
    return new with$I$C(1, 2);
  }
  with$I$C(x, y) : super(x), y(y + 31) { }
  with$I$C.I(x, y) : super.C(x), y(y + 32) { }
  with$I$C.C(x, y) : super.C$C(x), y(y + 33) { }
  with$I$C.I$C(x, y) : super.C$I(x), y(y + 34) { }
  with$I$C.C$C(x, y) : super(x), y(y + 35) { }
  with$I$C.C$C$C(x, y) : super.C(x), y(y + 36) { }
  with$I$C.$C$I(x, y) : super.C$C(x), y(y + 37) { }
  with$I$C.$$I$C(x, y) : super.C$I(x), y(y + 38) { }
  with$I$C.$(x, y) : super(x), y(y + 39) { }
  with$I$C.$$(x, y) : super.C(x), y(y + 40) { }
}

class Tata {
  var prototype;

  Tata() : prototype(0) {}

  function __PROTO__$(...args) { return 12; }
}

class Toto extends Tata {
  var __PROTO__;

  Toto() : super(), __PROTO__(0) { }

  function prototype$() { return 10; }

  function titi() {
    assert(prototype == 0);
    assert(__PROTO__ == 0);
    prototype = 3;
    __PROTO__ = 5;
    assert(prototype == 3);
    assert(__PROTO__ == 5);
    assert(prototype$() == 10);
    assert(__PROTO__$() == 12);
    assert(this.__PROTO__$() == 12);
    assert(this.prototype$() == 10);
    assert(prototype$(...[]) == 10);
    assert(__PROTO__$(...[]) == 12);
    assert(this.__PROTO__$(...[]) == 12);
    assert(this.prototype$(...[]) == 10);
  }
}

class Bug4082360 {
  int x_;
  Bug4082360() {}

  int get x { return x_; }
  void set x(int value) { x_ = value; }

  void indirectSet(int value) { x = value; }

  static void test() {
    var bug = new Bug4082360();
    bug.indirectSet(42);
    assert(bug.x_ == 42);
    assert(bug.x == 42);
  }
}

class Hoisting {
  var f_;
  Hoisting.negate(var x) {
    f_ = function() { return x; };
  }

  operator negate() {
    var x = 3;
    return function() { return x + 1; };
  }

  negate(x) {
    return function() { return x + 2; };
  }

  operator[] (x) {
    return function() { return x + 3; };
  }

  static void test() {
    var h = new Hoisting.negate(1);
    assert(1 == (h.f_)());
    var f = -h;
    assert(4 == f());
    assert(6 == h.negate(4)());
    assert(7 == h[4]());
  }
}

// It is not possible to make sure that the backend uses the hardcoded names
// we are testing against. This test might therefore become rapidly out of date
class NamingTest {
  static int count;

  static testExceptionNaming() {
    // Exceptions use a hardcoded "e" as exception name. If the namer works
    // correctly then it will be renamed in case of clashes.
    var e = 3;
    var caught = false;
    try {
      throw new MyException();
    } catch (exc) {
      try {
        throw new MyException();
      } catch (exc2) {
        exc = 9;
      }
      assert(exc == 9);
      caught = true;
    }
    assert(caught == true);
    assert(e == 3);
  }

  static testTmpNaming() {
    // Tmp variables are hardcoded as tmp$0. The spread operator uses one to
    // store the 'this' argument.
    assert(count == 0);
    var tmp$0 = 1;
    var tmp$1 = 2;
    new A().foo(... [tmp$0, tmp$1]);
    assert(count == 1);
  }

  static testScopeNaming() {
    // Alias scopes use a hardcoded "dartc_scp$<depth>" as names.
    var dartc_scp$1 = 5;
    var foo = 8;
    var f = function() {
      var dartc_scp$1 = 15;
      return foo + dartc_scp$1;
    };
    assert(dartc_scp$1 == 5);
    assert(f() == 23);
  }

  static testGlobalMangling() {
    var x;
    x = new debugger(0);
    assert(x.x == 1);
    x = new debugger.C(0);
    assert(x.x == 2);
    x = new debugger.C$C(0);
    assert(x.x == 3);
    x = new debugger.C$I(0);
    assert(x.x == 4);
    x = new debugger$C(0);
    assert(x.x == 5);
    x = new debugger$C.C(0);
    assert(x.x == 6);
    x = new debugger$C.C$C(0);
    assert(x.x == 7);
    x = new debugger$C.C$I(0);
    assert(x.x == 8);
    x = new debugger$C$C(0);
    assert(x.x == 9);
    x = new debugger$C$C.C(0);
    assert(x.x == 10);
    x = new debugger$C$C.C$C(0);
    assert(x.x == 11);
    x = new debugger$C$C.C$I(0);
    assert(x.x == 12);
    x = new with(0, 0);
    assert(x.x == 5);
    assert(x.y == 1);
    x = new with.I(0, 0);
    assert(x.x == 6);
    assert(x.y == 2);
    x = new with.C(0, 0);
    assert(x.x == 7);
    assert(x.y == 3);
    x = new with.I$C(0, 0);
    assert(x.x == 8);
    assert(x.y == 4);
    x = new with.C$C(0, 0);
    assert(x.x == 5);
    assert(x.y == 5);
    x = new with.C$C$C(0, 0);
    assert(x.x == 6);
    assert(x.y == 6);
    x = new with.$C$I(0, 0);
    assert(x.x == 7);
    assert(x.y == 7);
    x = new with.$$I$C(0, 0);
    assert(x.x == 8);
    assert(x.y == 8);
    x = new with.$(0, 0);
    assert(x.x == 5);
    assert(x.y == 9);
    x = new with.$$(0, 0);
    assert(x.x == 6);
    assert(x.y == 10);
    x = new with$I(0, 0);
    assert(x.x == 5);
    assert(x.y == 11);
    x = new with$I.I(0, 0);
    assert(x.x == 6);
    assert(x.y == 12);
    x = new with$I.C(0, 0);
    assert(x.x == 7);
    assert(x.y == 13);
    x = new with$I.I$C(0, 0);
    assert(x.x == 8);
    assert(x.y == 14);
    x = new with$I.C$C(0, 0);
    assert(x.x == 5);
    assert(x.y == 15);
    x = new with$I.C$C$C(0, 0);
    assert(x.x == 6);
    assert(x.y == 16);
    x = new with$I.$C$I(0, 0);
    assert(x.x == 7);
    assert(x.y == 17);
    x = new with$I.$$I$C(0, 0);
    assert(x.x == 8);
    assert(x.y == 18);
    x = new with$I.$(0, 0);
    assert(x.x == 5);
    assert(x.y == 19);
    x = new with$I.$$(0, 0);
    assert(x.x == 6);
    assert(x.y == 20);
    x = new with$C(0, 0);
    assert(x.x == 9);
    assert(x.y == 21);
    x = new with$C.I(0, 0);
    assert(x.x == 10);
    assert(x.y == 22);
    x = new with$C.C(0, 0);
    assert(x.x == 11);
    assert(x.y == 23);
    x = new with$C.I$C(0, 0);
    assert(x.x == 12);
    assert(x.y == 24);
    x = new with$C.C$C(0, 0);
    assert(x.x == 9);
    assert(x.y == 25);
    x = new with$C.C$C$C(0, 0);
    assert(x.x == 10);
    assert(x.y == 26);
    x = new with$C.$C$I(0, 0);
    assert(x.x == 11);
    assert(x.y == 27);
    x = new with$C.$$I$C(0, 0);
    assert(x.x == 12);
    assert(x.y == 28);
    x = new with$C.$(0, 0);
    assert(x.x == 9);
    assert(x.y == 29);
    x = new with$C.$$(0, 0);
    assert(x.x == 10);
    assert(x.y == 30);
    x = new with$I$C(0, 0);
    assert(x.x == 9);
    assert(x.y == 31);
    x = new with$I$C.I(0, 0);
    assert(x.x == 10);
    assert(x.y == 32);
    x = new with$I$C.C(0, 0);
    assert(x.x == 11);
    assert(x.y == 33);
    x = new with$I$C.I$C(0, 0);
    assert(x.x == 12);
    assert(x.y == 34);
    x = new with$I$C.C$C(0, 0);
    assert(x.x == 9);
    assert(x.y == 35);
    x = new with$I$C.C$C$C(0, 0);
    assert(x.x == 10);
    assert(x.y == 36);
    x = new with$I$C.$C$I(0, 0);
    assert(x.x == 11);
    assert(x.y == 37);
    x = new with$I$C.$$I$C(0, 0);
    assert(x.x == 12);
    assert(x.y == 38);
    x = new with$I$C.$(0, 0);
    assert(x.x == 9);
    assert(x.y == 39);
    x = new with$I$C.$$(0, 0);
    assert(x.x == 10);
    assert(x.y == 40);
    var wasCaught = false;
    try {
      throw new with(0, 0);
    } catch(with e) {
      wasCaught = true;
      assert(e.x == 5);
    }
    assert(wasCaught);
  }

  static void testMemberMangling() {
    assert(debugger.__PROTO__ == 5);
    new Toto().titi();
  }

  static void testFactoryMangling() {
    var o = new debugger.F();
    assert(o.x == 2);
    o = new debugger$C.F();
    assert(o.x == 6);
    o = new debugger$C$C.F();
    assert(o.x == 10);
    o = new with.F();
    assert(o.x == 6);
    assert(o.y == 3);
    o = new with$I.F();
    assert(o.x == 6);
    assert(o.y == 13);
    o = new with$C.F();
    assert(o.x == 10);
    assert(o.y == 23);
    o = new with$I$C.F();
    assert(o.x == 10);
    assert(o.y == 33);
  }

  static testFunctionParameters() {
    function a(with) {
      return with;
    }

    function b(eval) {
      return eval;
    }

    function c(arguments) {
      return arguments;
    }

    assert(10 == a(10));
    assert(10 == b(10));
    assert(10 == c(10));
  }

  static void main(args) {
    count = 0;
    testExceptionNaming();
    testTmpNaming();
    testScopeNaming();
    testGlobalMangling();
    testMemberMangling();
    testFactoryMangling();
    testFunctionParameters();
    Bug4082360.test();
    Hoisting.test();
  }
}
