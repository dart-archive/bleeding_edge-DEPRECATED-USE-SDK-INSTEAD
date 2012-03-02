// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Test that Frog does not try to put JS get and set properties on a hidden
// native class.
//
// I.e If B was not a hidden native class, Frog could generate the following:
//
// Object.defineProperty(B.prototype, "field", {
//  get: B.prototype.get$field,
//  set: B.prototype.set$field
// });
//
// This is clearly not possible when the prototype of B is hidden.

class A native "*A" {
  int field;
}

class B extends A native "*B" {
  int get field() native 'return this.field*100';
  void set field(int x) { super.field = x; }
}


makeA() native;
makeB() native;

void setup1() native """
// Poison hidden native names 'A' and 'B' to prove the compiler didn't place
// anthing on the hidden native class.
A = null;
B = null;
""";

void setup2() native """
// This code is all inside 'setup' and so not accesible from the global scope.
function A(){}
function B(){}
makeA = function(){return new A};
makeB = function(){return new B};
""";

int inscrutable(int x) => x == 0 ? 0 : x | inscrutable(x & (x - 1));

main() {
  setup1();
  setup2();

  var things = [makeA(), makeB()];
  {
    var a = things[inscrutable(0)];
    var b = things[inscrutable(1)];

    a.field = 2;
    Expect.equals(2, a.field);

    b.field = 3;
    Expect.equals(300, b.field);
  }

  {
    A a = things[inscrutable(0)];
    B b = things[inscrutable(1)];

    a.field = 2;
    Expect.equals(2, a.field);

    b.field = 3;
    Expect.equals(300, b.field);
  }

  {
    A a = things[inscrutable(1)]; // Actually a B.

    a.field = 4;
    Expect.equals(400, a.field);
  }
}
