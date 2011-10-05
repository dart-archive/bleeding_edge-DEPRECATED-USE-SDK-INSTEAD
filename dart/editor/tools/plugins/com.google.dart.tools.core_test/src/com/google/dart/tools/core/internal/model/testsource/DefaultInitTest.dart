// Tests static and instance fields initialization.
class DefaultInitTest {
  static main() {
    assert(A.a == 0);
    assert(A.b == 2);
    assert(A.c == null);

    A a1 = new A(42);
    assert(a1.d == 42);
    assert(a1.e == null);

    A a2 = new A.named(43);
    assert(a2.d == null);
    assert(a2.e == 43);

    assert(B.instance.x == 42);
    assert(C.instance.z == 3);
  }
}

class A {
  static final int a = 0;
  static final int b = 2;
  static int c;
  int d;
  int e;

  A(int val) {
    d = val;
  }

  A.named(int val) {
    e = val;
  }
}

// The following tests cover cases described in b/4101270

class B {
  static final B instance = const B();
  // by putting this field after the static initializer above, the JS code gen
  // was calling the constructor before the setter of this property was defined.
  final int x;
  const B() : x(41 + 1);
}

class C {
  // forward reference to another class
  static final D instance = const D();
  C() {}
}

class D {
  const D(): z(3);
  final int z;
}
