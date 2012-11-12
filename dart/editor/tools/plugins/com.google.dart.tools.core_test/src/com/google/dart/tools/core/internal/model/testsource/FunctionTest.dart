// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Tests function statements and expressions.

class Bug4089219 {
  int x;
  var f;

  Bug4089219(int i) : x(i) {
    f = function() { return x; };
  }
}

class Bug4342163 {
  final m;
  Bug4342163(int a) : m(function() {return a;}) {}
}

class StaticFunctionDef {
  static final int one = 1;
  static final fn1 = function() { return one; };
  static final fn2 = function() { return (function() { return one; })(); };
  static final fn3 = function() {
                         final local = 1;
                         return (function() { return local; })();
                     };
}

class Bug4473099 {
  static final Map m = {"1": function() { return null; }};
}

class A {
  var ma;
  A(a) {ma = a;}
}

class B1 extends A {
  final mfn;
  B1(int a) : super(a), mfn(function() {return a;}) {
  }
}

class B2 extends A {
 final mfn;
 B2(int a) : super(2), mfn(function() {return a;}) {
 }
}

class B3 extends A {
 final mfn;
 B3(int a) : super(function() {return a;}), mfn(function() {return a;}) {
 }
}

interface void Fisk();

class FunctionTest {

  FunctionTest() {}

  static void main() {
    var test = new FunctionTest();
    test.testRecursiveClosureRef();
    test.testForEach();
    test.testVarOrder1();
    test.testVarOrder2();
    test.testLexicalClosureRef1();
    test.testLexicalClosureRef2();
    test.testLexicalClosureRef3();
    test.testLexicalClosureRef4();
    test.testLexicalClosureRef5();
    test.testFunctionScopes();
    test.testDefaultParametersOrder();
    test.testParametersOrder();
    test.testRest1();
    test.testRest2();
    test.testFunctionDefaults1();
    test.testFunctionDefaults2();
    test.testEscapingFunctions();
    test.testThisBinding();
    test.testFnBindingInStatics();
    test.testFnBindingInInitLists();
    test.testSubclassConstructorScopeAlias();
  }

  void testSubclassConstructorScopeAlias() {
    var b1 = new B1(10);
    assert(10 == (b1.mfn)());
    assert(10 == b1.ma);

    var b2 = new B2(11);
    assert(11 == (b2.mfn)());
    assert(2 == b2.ma);

    var b3 = new B3(12);
    assert(12 == (b3.mfn)());
    assert(12 == (b3.ma)());
  }

  void testFnBindingInInitLists() {
    assert(1 == (new Bug4342163(1).m)());
  }

  void testFnBindingInStatics() {
    assert(1 == ((StaticFunctionDef.fn1)()));
    assert(1 == ((StaticFunctionDef.fn2)()));
    assert(1 == ((StaticFunctionDef.fn3)()));
    assert(null == (Bug4473099.m["1"])());
  }

  Fisk testReturnVoidFunction() {
    void f() {}
    Fisk x = f;
    return f;
  }

  void testVarOrder1() {
    var a = 0, b = a++, c = a++;

    assert(a == 2);
    assert(b == 0);
    assert(c == 1);
  }

  void testVarOrder2() {
    var a = 0;
    function f() {return a++;};
    var b = f(), c = f();

    assert(a == 2);
    assert(b == 0);
    assert(c == 1);
  }

  void testLexicalClosureRef1() {
    var a = 1;
    var f, g;
    {
      var b = 2;
      f = function() {return b - a;};
    }

    {
      var b = 3;
      g = function() {return b - a;};
    }
    assert(f() == 1);
    assert(g() == 2);
  }

  void testLexicalClosureRef2() {
    var a = 1;
    var f, g;
    {
      var b = 2;
      f = function() {return (function(){return b - a;})();};
    }

    {
      var b = 3;
      g = function() {return (function(){return b - a;})();};
    }
    assert(f() == 1);
    assert(g() == 2);
  }

  void testLexicalClosureRef3() {
    var a = new GrowableArray();
    for (int i = 0; i < 10; i++) {
      var x = i;
      a.add(function() {return x;});
    }

    var sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += (a[i])();
    }

    assert(sum == 45);
  }

  void testLexicalClosureRef5() {
    {
      var a;
      assert( a == null );
      a = 1;
      assert( a == 1 );
    }

    {
      var a;
      assert( a == null );
      a = 1;
      assert( a == 1 );
    }
  }

  // Make sure labels are preserved, and a second 'i' does influence the first.
  void testLexicalClosureRef4() {
    var a = new GrowableArray();
    x:for (int i = 0; i < 10; i++) {
      a.add(function() {return i;});
      continue x;
    }

    var sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += (a[i])();
    }

    assert(sum == 100);
  }

  int tempField;

  // Validate that a closure that calls the private name of a function (for
  // for recursion) calls the version of function with the bound names.
  void testRecursiveClosureRef() {
    tempField = 2;
    var x = 3;
    var g = function f(a) {
       tempField++;
       x++;
       if (a > 0) {
         f(--a);
       }
    };
    g(2);


    assert(tempField == 5);
    assert(x == 6);
  }

  void testForEach() {
    Array<int> vals = [1,2,3];
    int total = 0;
    vals.forEach(function(int v) {
      total += v;
    });
    assert(total == 6);
  }

  void testFunctionScopes() {
    // Function expression. 'recurse' is only defined within the function body.
    // FAILS:
    // var factorial0 = function recurse(int x) {
    //  return (x == 1) ? 1 : (x * recurse(x - 1));
    // };
    // TEMP:
    var factorial0;
    factorial0 = function recurse(int x) {
      return (x == 1) ? 1 : (x * factorial0(x - 1));
    };
    // END TEMP


    // Function statement. 'factorial1' is defined in the outer scope.
    int factorial1(int x) {
      return (x == 1) ? 1 : (x * factorial1(x - 1));
    }

    // This would fail to compile if 'recurse' were defined in the outer scope.
    // Which it shouldn't be.
    int recurse = 42;

    assert(6 == factorial0(3));
    assert(24 == factorial0(4));
  }

  void testDefaultParametersOrder() {
    function f(a = 1, b = 3) {
      return a - b;
    }
    assert(-2 == f());
  }

  void testParametersOrder() {
    function f(a, b) {
      return a - b;
    }
    assert(-2 == f(1,3));
  }

  void testRest1() {
    function f(... rest) {
      return rest[0] + rest[1];
    }
    int result = f(1, 2);
    assert(3 == result, "expected 3 got ${result}");
  }

  void testRest2() {
    function f(a, ... rest) {
      return rest[0] + rest[1];
    }
    int result = f(1, 2, 3);
    assert(5 == result, "expected 5 got ${result}");
  }

  void testFunctionDefaults1() {
    // TODO(jimhug): This return null shouldn't be necessary.
    function f() { return null; };
    (function(a = 10) { assert(a == 10); })();
    (function(a, b = 10) { assert(b == 10); })(1);
    (function(a = 10) { assert(a == null); })( f() );
  }

  void testFunctionDefaults2() {
    assert( helperFunctionDefaults2() == 10);
    assert( helperFunctionDefaults2(1) == 1);
  }

  void helperFunctionDefaults2(a = 10) {
    return (function(){return a;})();
  }

  void testEscapingFunctions() {
    function f() { return 42; }
    (function() { assert(f() == 42); })();
    var o = new Bug4089219(42);
    assert((o.f)() == 42);
  }

  void testThisBinding() {
    assert(this == function() { return this; }());
  }
}

interface void Foo<A, B>(A a, B b);

class Bar<A, B> {
  Foo<A, B> field;
  Bar(A a, B b) : field(function(A a1, B b2){}) {
    field(a, b);
  }
}

interface function UntypedFunction(arg);
interface function UntypedFunction2(arg);

class UseFunctionTypes {
  void test() {
    Function f = null;
    UntypedFunction uf = null;
    UntypedFunction2 uf2 = null;
    Foo foo = null;
    Foo<int, String> fooIntString = null;

    f = uf;
    f = uf2;
    f = foo;
    f = fooIntString;

    uf = f;
    uf2 = f;
    foo = f;
    fooIntString = f;

    foo = fooIntString;
    fooIntString = foo;

    uf = uf2;
    uf2 = uf;
  }
}
