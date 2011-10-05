// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Tests basic classes and methods.
class ClassTest {
  ClassTest() {}

  static main() {
    var test = new ClassTest();
    test.testSuperCalls();
    test.testVirtualCalls();
    test.testStaticCalls();
    test.testInheritedField();
    test.testMemberRefInClosure();
    test.testFactory();
    test.testNamedConstructors();
    test.testDefaultImplementation();
    test.testFunctionParameter(function(int a) { return a;});
  }

  testFunctionParameter(int func(int a)) {
    assert(func(1) == 1);
  }

  testSuperCalls() {
    var sub = new Sub();
    assert(sub.methodX() == 43);
    assert(sub.methodK() == 84);
  }

  testVirtualCalls() {
    var sub = new Sub();
    assert(sub.method2() == 41);
    assert(sub.method3() == 41);
  }

  testStaticCalls() {
    var sub = new Sub();
    assert(Sub.method4() == -42);
    assert(sub.method5() == -41);
  }

  testInheritedField() {
    var sub  = new Sub();
    assert(sub.method6() == 42);
  }

  testMemberRefInClosure() {
    var sub = new Sub();
    assert(sub.closureRef() == 1, "expected 1");
    assert(sub.closureRef() == 2, "expected 2");
    // Make sure it is actually on the object, not the global 'this'.
    sub = new Sub();
    assert(sub.closureRef() == 1, "expected 1");
    assert(sub.closureRef() == 2, "expected 2");
  }

  testFactory() {
    var sup = new Sup.named();
    assert(sup.methodX() == 43);
    assert(sup.methodK() == 84);
  }

  testNamedConstructors() {
    var sup = new Sup.fromInt(4);
    assert(sup.methodX() == 4);
    assert(sup.methodK() == 0);
  }

  testDefaultImplementation() {
    var x = new Inter(4);
    assert(x.methodX() == 4);
    assert(x.methodK() == 8);

    x = new Inter.fromInt(4);
    assert(x.methodX() == 4);
    assert(x.methodK() == 0);

    x = new Inter.named();
    assert(x.methodX() == 43);
    assert(x.methodK() == 84);

    x = new Inter.factory();
    assert(x.methodX() == 43);
    assert(x.methodK() == 84);
  }
}

interface Inter default Sup {
  Inter.named();
  Inter.fromInt(int x);
  Inter(int x);
  Inter.factory();
  int methodX();
  int methodK();
  int x_;
}

class Sup implements Inter {
  int x_;
  int k_;

  factory Sup.named() {
    return new Sub();
  }

  factory Inter.factory() {
    return new Sub();
  }

  Sup.fromInt(int x) {
    x_ = x;
    k_ = 0;
  }

  int methodX() {
    return x_;
  }

  int methodK() {
    return k_;
  }

  Sup(int x) : x_(x) {
    k_ = x * 2;
  }

  int method2() {
    return x_ - 1;
  }
}

class Sub extends Sup {
  int y_;

  // Override
  int methodX() {
    return super.methodX() + 1;
  }

  int method3() {
    return method2();
  }

  static int method4() {
    return -42;
  }

  int method5() {
    return method4() + 1;
  }

  int method6() {
    return x_ + y_;
  }

  int closureRef() {
    var f = function() {y_ += 1; return y_;};
    return f();
  }

  Sub() : super(42) {
    y_ = 0;
  }
}
