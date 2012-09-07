// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class GettersSettersTest {

  static int foo;

  static main() {
    A a = new A();
    a.x = 2;
    assert(2 == a.x);
    assert(2 == a.x_);

    // Test inheritance.
    a = new B();
    a.x = 4;
    assert(4 == a.x);
    assert(4 == a.x_);

    // Test overriding.
    C c = new C();
    c.x = 8;
    assert(8 == c.x);
    assert(0 == c.x_);
    assert(8 == c.y_);

    // Test keyed getters and setters.
    a.x_ = 0;
    assert(2 == a[2]);
    a[2] = 4;
    assert(6 == a[0]);

    // Test assignment operators.
    a.x_ = 0;
    a[2] += 8;
    assert(12 == a[0]);

    // Test calling a function that internally uses getters.
    assert(a.isXPositive());

    // Test static fields.
    foo = 42;
    assert(foo == 42);
    A.foo = 43;
    assert(A.foo == 43);

    new D().test();

    OverrideField of = new OverrideField();
    assert(of.getX_() == 27);

    ReferenceField rf = new ReferenceField();
    rf.x_ = 1;
    assert(rf.getIt() == 1);
    rf.setIt(2);
    assert(rf.x_ == 2);
  }
}

class A {
  // TODO(fabiofmv): consider removing once http://b/4254120 is fixed.
  A() { }
  int x_;
  static int foo;

  static get bar {
    return foo;
  }

  static set bar(newValue) {
    foo = newValue;
  }

  A() {}

  int get x {
    return x_;
  }

  void set x(int value) {
    x_ = value;
  }

  bool isXPositive() {
    return x > 0;
  }

  int operator [](int index) {
    return x_ + index;
  }

  void operator []=(int index, int value) {
    x_ = index + value;
  }

  int getX_() {
    return x_;
  }
}

class B extends A {
  B() : super() {}
}

class C extends A {
  int y_;

  C() : super() {
    this.x_ = 0;
  }

  int get x {
    return y_;
  }

  void set x(int value) {
    y_ = value;
  }
}

class D extends A {
  D() : super() {}

  var x_;

  set x(new_x) {
    x_ = new_x;
  }

  test() {
    x = 87;
    assert(x_ == 87);
    x = 42;
    assert(x_ == 42);

    foo = 0;
    assert(bar == 0);
    bar = 1;
    assert(foo == 1);
    var tmp = foo;
    foo += 3;
    assert(bar == 4);
    bar += 5;
    assert(foo == 9);
  }
}

class OverrideField extends A {
  OverrideField() : super() {}

  int get x_ {
    return 27;
  }
}

class ReferenceField extends A {
  ReferenceField() : super() {}

  setIt(a) {
     super.x_ = a;
  }

  int getIt() {
     return super.x_;
  }
}
