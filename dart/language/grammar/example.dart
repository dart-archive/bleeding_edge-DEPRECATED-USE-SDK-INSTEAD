// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Object {
  var x;
  int foo() {
    return 42;
  }
  bar(int x, int y, z) { }
}

class Baz extends Kuk implements A, B, C {
  static final y = 87, z = 42;
  static final Foo moms = 42, kuks = 42;
  final Kuk hest;
  static var foo;
  var fisk = 2;
  final fiskHest = const Foo();

  /* Try a few
   * syntactic constructs. */
  void baz() {
    if (42) if (42) 42; else throw 42;
    switch (42) { case 42: return 42; default: break; }
    try { } catch (var e) { }
    int kongy(x,y) { return 42; }  // This is a comment.
    for (var i in e) {}
    for (var i in e.baz) {}
    for (var i in e[0]) {}
    for (final i in e) {}
    for (final Foo<int> i in e) {}
    for (Foo<int> i in e) {}
    for (i in e) {}
  }

  int hest(a) {
    for (var i = 0; i < a.length; i++) {
      a.b.c.f().g[i] += foo(i);
      int kuk = 42;
      (kuk);
      id(x) { return x; }
      int id(x) { return x; }
      var f = hest() { };
      var f = int horse() { };
      assert(x == 12);
    }
  }

  Baz(x, y, z) : super(x, y, z) {}
}

interface Foo extends D, E {
  bar();
}


// Test various basic forms of formal parameters.
interface MethodSignatureSyntax {
  a();
  b(x);
  c(int x);
  d(var x);
  e(final x);

  f(x, y);
  g(var x, y);
  h(final x, y);
  j(var x, var y);
  k(final x, final y);

  l(int x, y);
  m(int x, int y);
}


// Test more details on the formal parameter syntax.
class FormalParameterSyntax {
  a([x = 42]) { }
  b([int x = 42]) { }
  c(x, [y = 42]) { }
  d(x, [int y = 42]) { }
}


// Test various forms of function type syntax.
class FunctionTypeSyntax {
  Function a;
  static Function b;

  Function c() { }
  static Function d() { }

  e(Function f) { }
  static f(Function f) { }

  // Dart allows C++ style function types in formal
  // parameter lists.
  g(f()) { }
  h(void f()) { }
  j(f(x)) { }
  k(f(x, y)) { }
  l(int f(int x, int y)) { }
  m(int x, int f(x), int y) { }
}


// Test super calls.
class SuperCallSyntax {
  method() {
    super.foo();
    super.foo(1);
    super.foo(1, 2);

    super.foo().x;
    super.foo()[42];
    super.foo().x++;

    super.foo()();
    super.foo(1, 2)(3, 4);

    var v1 = super.foo();
    var v2 = super.foo(1);
    var v3 = super.foo(1, 2);

    var v4 = super.foo().x;
    var v5 = super.foo()[42];
    var v6 = super.foo().x++;

    var v7 = super.foo()();
    var v8 = super.foo(1, 2)(3, 4);
  }

  get field() {
    super.field;
    super.field = 42;
    super.field += 87;

    super['baz'];
    super['baz'] = 42;
    super['baz'] += 87;
  }
}


// Test generic types.
class Box<T> {
  T t;
  getT() { return t; }
  setT(T t) { this.t = t; }
}

class UseBox {
  Box<Box<Box<prefix.Fisk>>> boxIt(Box<Box<prefix.Fisk>> box) {
    return new Box<Box<Box<prefix.Fisk>>>(box);
  }
}

// Test shift operators.
class Shifting {
  operator >>>(other) {
    Box<Box<Box<prefix.Fisk>>> foo = null;
    return other >>> 1;
  }

  operator >>(other) {
    Box<Box<prefix.Fisk>> foo = null;
    return other >> 1;
  }
}

typedef void VoidCallback1(Event event);
typedef void VoidCallback2(Event event, int x);
typedef void VoidCallback3(Event event, int x, y);
typedef void VoidCallback4(Event event, int x, var y);

typedef Callback1(Event event);
typedef Callback2(Event event, int x);
typedef Callback3(Event event, int x, y);
typedef Callback4(Event event, int x, var y);

typedef int IntCallback1(Event event);
typedef int IntCallback2(Event event, int x);
typedef int IntCallback3(Event event, int x, y);
typedef int IntCallback4(Event event, int x, var y);

typedef Box<int> BoxCallback1(Event event);
typedef Box<int> BoxCallback2(Event event, int x);
typedef Box<int> BoxCallback3(Event event, int x, y);
typedef Box<int> BoxCallback4(Event event, int x, var y);

typedef Box<Box<int>> BoxBoxCallback1(Event event);
typedef Box<Box<int>> BoxBoxCallback2(Event event, int x);
typedef Box<Box<int>> BoxBoxCallback3(Event event, int x, y);
typedef Box<Box<int>> BoxBoxCallback4(Event event, int x, var y);

typedef void VoidCallbak1(Event event);
typedef void VoidCallbak2(Event event, int x);
typedef void VoidCallbak3(Event event, int x, y);
typedef void VoidCallbak4(Event event, int x, var y);

typedef void VoidCallbuk1<E>(E event);
typedef void VoidCallbuk2<E, I>(E event, I x);
typedef void VoidCallbuk3<E extends Event, I extends int>(E event, I x, y);
typedef void VoidCallbuk4<E extends Event<E>, I extends int>(E event, I x,
                                                               var y);

typedef Callbuk1<E>(E event);
typedef Callbuk2<E, I>(E event, I x);
typedef Callbuk3<E extends Event, I>(E event, I x, y);
typedef Callbuk4<E, I extends int>(E event, I x, var y);

typedef int IntCallbuk1<E>(E event);
typedef I IntCallbuk2<E extends Event, I extends int>(E event, I x);
typedef I IntCallbuk3<E, I>(E event, I x, y);
typedef I IntCallbuk4<E, I>(E event, I x, var y);

typedef Box<int> BoxCallbuk1<E>(E event);
typedef Box<I> BoxCallbuk2<E, I>(E event, I x);
typedef Box<I> BoxCallbuk3<E, I>(E event, I x, y);
typedef Box<I> BoxCallbuk4<E, I>(E event, I x, var y);

typedef Box<int> BoxBoxCallbuk1<E>(E event);
typedef Box<I> BoxBoxCallbuk2<E, I>(E event, I x);
typedef Box<I> BoxBoxCallbuk3<E, I>(E event, I x, y);
typedef Box<I> BoxBoxCallbuk4<E, I>(E event, I x, var y);

class NumberSyntax {
  f() {
    1; 12; 123;
    1.0; 12.0; 123.0;
    .1; .12; .123;
    1.0; 12.12; 123.123;

    1e1; 12e12; 123e123;
    1e+1; 12e+12; 123e+123;
    1e-1; 12e-12; 123e-123;

    1.0e1; 12.0e12; 123.0e123;
    1.0e+1; 12.0e+12; 123.0e+123;
    1.0e-1; 12.0e-12; 123.0e-123;

    .1e1; .12e12; .123e123;
    .1e+1; .12e+12; .123e+123;
    .1e-1; .12e-12; .123e-123;

    1.0e1; 12.12e12; 123.123e123;
    1.0e+1; 12.12e+12; 123.123e+123;
    1.0e-1; 12.12e-12; 123.123e-123;

    1.1234e+444;

    o.d();
  }
}

class ArrayLiteralSyntax {
  void f() {
    var a0 = [];
    var a1 = [12];
    var a2 = [null];
    var a3 = [f(),o2];
    var a4 = [(){return 42;},o2];
    var a5 = [()=>42,o2];
    var a6 = const <int> [ 12, 18 ];
    var a7 = <int> [ 12, 18 ];
  }
}

class MapLiteralSyntax {
  void f() {
    var o0 = {};
    var o1 = {"a":12};
    var o2 = {"a":null,};
    var o3 = {"a":f(),"b":o2};
    var o4 = {"a":(){return 42;},"b":o2,};
    var o4 = {"a":()=>42,"b":o2,};
    var o5 = {"if": 12};
    var o6 = {"foo bar":null, "while":17};
    var o7 = const <String,int> { "a": 12, "b": 18 };
    var o8 = <String,int> { "a": 12, "b": 18 };
  }
}

class CompileTimeConstructorSyntax {
  const CompileTimeConstructorSyntax();
  const CompileTimeConstructorSyntax() : super(1, 2, 3);
}

class AbstractMethodSyntax {
  abstract f0();
  abstract void f1();
  abstract int f2(x, y);
  abstract f3(int x, var y);

  abstract get x();
  abstract int get x();
  abstract set y(value);
  abstract void set y(value);

  abstract operator +(x);
  abstract int operator -(x);
}

class AssignableSyntax {
  test(a) {
    a[0] ? "a" : "b";
    return a[0] ? "a" : "b";
  }
}

class SetGetSyntax {
  get x() { }
  set x(v) { }
  int get y() { }
  void set y(v) { }

  static get x() { }
  static set x(v) { }
  static int get y() { }
  static void set y(v) { }
}

class SwitchSyntax {
  void foo() {
    switch (42) {
      case 42:
        var x = 0;
        break;
      case 87:
        throw 42;
        var x = 0;  // Dead code is allowed by the grammar
      L: default:
        var x = 0;
        return;
        var y = 0;  // Dead code is allowed by the grammar.
    }
  }
}

class ConstructorSyntax {
  ConstructorSyntax(x, y) : super(), this.x = x, this.y = y {}
  ConstructorSyntax.a(x, y) : x = x, super(), y = x {}
  ConstructorSyntax.b(x, y) : this.x = y, this.y = x, super() {}
}

class FieldParameterSyntax {
  FieldParameterSyntax(this.x){}
  FieldParameterSyntax.a(int this.x){}
  FieldParameterSyntax.b(var this.x, int y){}
  FieldParameterSyntax.b(int x, final this.y){}
}

class WithNamedArguments {
  void m1([int foo([int i])]) {}
  void m2([int foo([int i]), int bar([int i])]) {}
  void m3([int foo([int i, int i]), int bar([int i, int i])]) {}

  void test() {
    foo(x, n1:x);
    foo(x, y, n1:x, n2:x);
    foo(x, y, z, n1:x, n2:x, n3:x);
    foo(n1:x);
    foo(n1:x, n2:x);
    foo(n1:x, n2:x, n3:x);
  }
}

// Top level functions.
topLevelUntypedFunction() {}
void topLevelTypedFunction(int a) {}

// Top level variables.
final topLevelFinalUntypedVariable = 1;
final topLevelListFinalUntypedVariable = 1, b = 2, c = 3;
final int topLevelFinalTypedVariable = 1;
final int topLevelListFinalTypedVariable = 1, b = 2, c = 3;
int topLevelTypedVariable;
var topLevelUnTypedVariable;
int topLevelListTypedVariable, a, b;
var topLevelListUnTypedVariable, a, b;
var topLevelInitializedVariable = 2;
final topLevelInitializedVariable2 = const Foo();

// Top level setters
get topLevelGetter() {}
set topLevelSetter(a) {}
Foo<int> get topLevelGetter3() {}
void set topLevelSetter3(Foo<int> a) {}


class Operators {

  operator ~() { }
  operator negate() { }

  operator *(x) { }
  operator /(x) { }
  operator %(x) { }
  operator ~/(x) { }

  operator +(x) { }
  operator -(x) { }

  operator <<(x) { }
  operator >>(x) { }
  operator >>>(x) { }

  operator ==(x) { }
  operator <=(x) { }
  operator <(x) { }
  operator >=(x) { }
  operator >(x) { }

  operator &(x) { }
  operator ^(x) { }
  operator |(x) { }

  foo() {
    ~super;
    -super;

    super * 42;
    super / 42;
    super % 42;
    super ~/ 42;

    super + 42;
    super - 42;

    super << 42;
    super >> 42;
    super >>> 42;

    super == 42;
    super != 42;  // Expected to map to !(super == 42).
    super <= 42;
    super < 42;
    super >= 42;
    super > 42;

    super & 42;
    super ^ 42;
    super | 42;

    // BUG(4994724): Do we need to allow calling these?
    !super;
    super === 42;
    super !== 42;
  }

}


class Redirection {

  const Redirection() : this.foo();
  const Redirection.bar() : this.foo();

  Redirection() : this.foo();
  Redirection.baz() : this.foo();

}


class FunctionBody {

  // Even constructors can use the => syntax instead of a
  // body. Syntactically okay, but doesn't make much sense since
  // constructors aren't allowed to return anything.
  FunctionBody() : this.x = 99 => 42;

  foo() => 99;
  get x() => x;
  set y(x) => x + y;  // Setters should be void -- not enforced by syntax.
  operator +(x) => x + 42;

  int foo() => 99;
  int get x() => x;
  void set y(x) => x + y;
  int operator +(x) => x + 42;

  bar() {
    baz() => 87;
    int biz() => 87;

    var f = () => 42;
    var g = int _() => 87;
    var h = fugl() => 99;
  }

}


class NastyConstructor {

  // NOTE: These examples aren't pretty but they illustrate what's legal.
  A() : x = foo() => 42;
  A() : x = foo() { }
  A() : x = (foo() { }) { }
  A() : x = (foo() => 42) { }

  A() : x = ((foo()) { }) { }
  A() : x = ((foo()) => 42) { }
  A() : x = ((foo()) { }) => 87;
  A() : x = ((foo()) => 42) => 87;

  A() : x = bar((foo()) { }) { }
  A() : x = bar((foo()) => 42) { }
  A() : x = bar((foo()) { }) => 87;
  A() : x = bar((foo()) => 42) => 87;

  A() : x = [(foo()) { }] { }
  A() : x = [(foo()) => 42] { }
  A() : x = [(foo()) { }] => 87;
  A() : x = [(foo()) => 42] => 87;

  A() : x = {'x':(foo()) { }} { }
  A() : x = {'x':(foo()) => 42} { }
  A() : x = {'x':(foo()) { }} => 87;
  A() : x = {'x':(foo()) => 42} => 87;

  factory lib.Class<T, X>.name() { return null; }
  factory Class<T, X>.name() { return null; }
  factory lib.Class<T, X>() { return null; }
  factory Class<T, X>() { return null; }
}
