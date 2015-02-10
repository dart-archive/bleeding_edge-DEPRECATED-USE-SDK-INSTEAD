// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'package:analyzer/analyzer.dart';
import 'package:analyzer/file_system/file_system.dart';
import 'package:analyzer/file_system/memory_file_system.dart';
import 'package:analyzer/src/generated/element.dart';
import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/sdk.dart';
import 'package:analyzer/src/generated/source.dart';
import 'package:analyzer/src/generated/source_io.dart';
import 'package:analyzer2dart/src/identifier_semantics.dart';
import 'package:unittest/unittest.dart';

import 'mock_sdk.dart';

main() {
  test('Call function defined at top level', () {
    Helper helper = new Helper('''
g() {}

f() {
  g();
}
''');
    helper.checkStaticMethod('g()', null, 'g', true, isInvoke: true);
  });

  test('Call function defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.g();
}
''');
    helper.addFile('/lib.dart', '''
library lib;

g() {}
''');
    helper.checkStaticMethod('l.g()', null, 'g', true, isInvoke: true);
  });

  test('Call method defined statically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  static g() {}

  f() {
    g();
  }
}
''');
    helper.checkStaticMethod('g()', 'A', 'g', true, isInvoke: true);
  });

  test('Call method defined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {
  static g() {}
}
f() {
  A.g();
}
''');
    helper.checkStaticMethod('A.g()', 'A', 'g', true, isInvoke: true);
  });

  test(
      'Call method defined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.A.g();
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {
  static g() {}
}
''');
    helper.checkStaticMethod('l.A.g()', 'A', 'g', true, isInvoke: true);
  });

  test('Call method defined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  g() {}

  f() {
    g();
  }
}
''');
    helper.checkDynamic('g()', null, 'g', isInvoke: true);
  });

  test(
      'Call method defined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {
  g() {}
}
f(A a) {
  a.g();
}
''');
    helper.checkDynamic('a.g()', 'a', 'g', isInvoke: true);
  });

  test(
      'Call method defined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {
  g() {}
}
A h() => null;
f() {
  h().g();
}
''');
    helper.checkDynamic('h().g()', 'h()', 'g', isInvoke: true);
  });

  test(
      'Call method defined dynamically in class from outside class via dynamic var',
      () {
    Helper helper = new Helper('''
f(a) {
  a.g();
}
''');
    helper.checkDynamic('a.g()', 'a', 'g', isInvoke: true);
  });

  test(
      'Call method defined dynamically in class from outside class via dynamic expression',
      () {
    Helper helper = new Helper('''
h() => null;
f() {
  h().g();
}
''');
    helper.checkDynamic('h().g()', 'h()', 'g', isInvoke: true);
  });

  test('Call method defined locally', () {
    Helper helper = new Helper('''
f() {
  g() {}
  g();
}
''');
    helper.checkLocalFunction('g()', 'g', isInvoke: true);
  });

  test('Call method undefined at top level', () {
    Helper helper = new Helper('''
f() {
  g();
}
''');
    // Undefined top level invocations are treated as dynamic.
    // TODO(paulberry): not sure if this is a good idea.  In general, when such
    // a call appears inside an instance method, it is dynamic, because "this"
    // might be an instance of a derived class that implements g().  However,
    // in this case, we are not inside an instance method, so we know that the
    // target is undefined.
    helper.checkDynamic('g()', null, 'g', isInvoke: true);
  });

  test('Call method undefined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.g();
}
''');
    helper.addFile('/lib.dart', '''
library lib;
''');
    // Undefined top level invocations are treated as dynamic.
    // TODO(paulberry): not sure if this is a good idea, for similar reasons to
    // the case above.
    helper.checkDynamic('l.g()', null, 'g', isInvoke: true);
  });

  test('Call method undefined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {}

f() {
  A.g();
}
''');
    helper.checkStaticMethod('A.g()', 'A', 'g', false, isInvoke: true);
  });

  test(
      'Call method undefined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.A.g();
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {}
''');
    helper.checkStaticMethod('l.A.g()', 'A', 'g', false, isInvoke: true);
  });

  test('Call method undefined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  f() {
    g();
  }
}
''');
    helper.checkDynamic('g()', null, 'g', isInvoke: true);
  });

  test(
      'Call method undefined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {}

f(A a) {
  a.g();
}
''');
    helper.checkDynamic('a.g()', 'a', 'g', isInvoke: true);
  });

  test(
      'Call method undefined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {}

A h() => null;

f() {
  h().g();
}
''');
    helper.checkDynamic('h().g()', 'h()', 'g', isInvoke: true);
  });

  test('Call variable defined at top level', () {
    Helper helper = new Helper('''
var x;

f() {
  x();
}
''');
    helper.checkStaticField('x()', null, 'x', isInvoke: true);
  });

  test('Call variable defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.x();
}
''');
    helper.addFile('/lib.dart', '''
library lib;

var x;
''');
    helper.checkStaticField('l.x()', null, 'x', isInvoke: true);
  });

  test('Call field defined statically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  static var x;

  f() {
    return x();
  }
}
''');
    helper.checkStaticField('x()', 'A', 'x', isInvoke: true);
  });

  test('Call field defined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {
  static var x;
}

f() {
  return A.x();
}
''');
    helper.checkStaticField('A.x()', 'A', 'x', isInvoke: true);
  });

  test(
      'Call field defined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.A.x();
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {
  static var x;
}
''');
    helper.checkStaticField('l.A.x()', 'A', 'x', isInvoke: true);
  });

  test('Call field defined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  var x;

  f() {
    return x();
  }
}
''');
    helper.checkDynamic('x()', null, 'x', isInvoke: true);
  });

  test(
      'Call field defined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {
  var x;
}

f(A a) {
  return a.x();
}
''');
    helper.checkDynamic('a.x()', 'a', 'x', isInvoke: true);
  });

  test(
      'Call field defined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {
  var x;
}

A h() => null;

f() {
  return h().x();
}
''');
    helper.checkDynamic('h().x()', 'h()', 'x', isInvoke: true);
  });

  test(
      'Call field defined dynamically in class from outside class via dynamic var',
      () {
    Helper helper = new Helper('''
f(a) {
  return a.x();
}
''');
    helper.checkDynamic('a.x()', 'a', 'x', isInvoke: true);
  });

  test(
      'Call field defined dynamically in class from outside class via dynamic expression',
      () {
    Helper helper = new Helper('''
h() => null;

f() {
  return h().x();
}
''');
    helper.checkDynamic('h().x()', 'h()', 'x', isInvoke: true);
  });

  test('Call variable defined locally', () {
    Helper helper = new Helper('''
f() {
  var x;
  return x();
}
''');
    helper.checkLocalVariable('x()', 'x', isInvoke: true);
  });

  test('Call variable defined in parameter', () {
    Helper helper = new Helper('''
f(x) {
  return x();
}
''');
    helper.checkParameter('x()', 'x', isInvoke: true);
  });

  test('Call accessor defined at top level', () {
    Helper helper = new Helper('''
get x => null;

f() {
  return x();
}
''');
    helper.checkStaticProperty('x()', null, 'x', true, isInvoke: true);
  });

  test('Call accessor defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.x();
}
''');
    helper.addFile('/lib.dart', '''
library lib;

get x => null;
''');
    helper.checkStaticProperty('l.x()', null, 'x', true, isInvoke: true);
  });

  test('Call accessor defined statically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  static get x => null;

  f() {
    return x();
  }
}
''');
    helper.checkStaticProperty('x()', 'A', 'x', true, isInvoke: true);
  });

  test('Call accessor defined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {
  static get x => null;
}

f() {
  return A.x();
}
''');
    helper.checkStaticProperty('A.x()', 'A', 'x', true, isInvoke: true);
  });

  test(
      'Call accessor defined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.A.x();
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {
  static get x => null;
}
''');
    helper.checkStaticProperty('l.A.x()', 'A', 'x', true, isInvoke: true);
  });

  test('Call accessor defined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  get x => null;

  f() {
    return x();
  }
}
''');
    helper.checkDynamic('x()', null, 'x', isInvoke: true);
  });

  test(
      'Call accessor defined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {
  get x => null;
}

f(A a) {
  return a.x();
}
''');
    helper.checkDynamic('a.x()', 'a', 'x', isInvoke: true);
  });

  test(
      'Call accessor defined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {
  get x => null;
}

A h() => null;

f() {
  return h().x();
}
''');
    helper.checkDynamic('h().x()', 'h()', 'x', isInvoke: true);
  });

  test(
      'Call accessor defined dynamically in class from outside class via dynamic var',
      () {
    Helper helper = new Helper('''
f(a) {
  return a.x();
}
''');
    helper.checkDynamic('a.x()', 'a', 'x', isInvoke: true);
  });

  test(
      'Call accessor defined dynamically in class from outside class via dynamic expression',
      () {
    Helper helper = new Helper('''
h() => null;

f() {
  return h().x();
}
''');
    helper.checkDynamic('h().x()', 'h()', 'x', isInvoke: true);
  });

  test('Call class defined at top level', () {
    Helper helper = new Helper('''
class A {}

f() {
  A();
}
''');
    helper.checkTypeReference(
        'A()',
        'A',
        AccessKind.TOPLEVEL_TYPE,
        isInvoke: true);
  });

  test('Call class defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.A();
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {}
''');
    helper.checkTypeReference(
        'l.A()',
        'A',
        AccessKind.TOPLEVEL_TYPE,
        isInvoke: true);
  });

  test('Call dynamic type undefined at toplevel', () {
    Helper helper = new Helper('''
f() {
  dynamic();
}
''');
    // Since it is legal to define a toplevel function or a class member called
    // dynamic, "dynamic()" must be treated as a dynamic access to a function
    // called "dynamic".
    helper.checkDynamic('dynamic()', null, 'dynamic', isInvoke: true);
  });

  test('Call function typedef defined at top level', () {
    Helper helper = new Helper('''
typedef F();

f() {
  F();
}
''');
    helper.checkTypeReference(
        'F()',
        'F',
        AccessKind.TOPLEVEL_TYPE,
        isInvoke: true);
  });

  test('Call function typedef defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.F();
}
''');
    helper.addFile('/lib.dart', '''
library lib;

typedef F();
''');
    helper.checkTypeReference(
        'l.F()',
        'F',
        AccessKind.TOPLEVEL_TYPE,
        isInvoke: true);
  });

  test('Call mixin application defined at top level', () {
    Helper helper = new Helper('''
class A {}
class B {}
class C = A with B;

f() {
  C();
}
''');
    helper.checkTypeReference(
        'C()',
        'C',
        AccessKind.TOPLEVEL_TYPE,
        isInvoke: true);
  });

  test('Call mixin application defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.C();
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {}
class B {}
class C = A with B;
''');
    helper.checkTypeReference(
        'l.C()',
        'C',
        AccessKind.TOPLEVEL_TYPE,
        isInvoke: true);
  });

  test('Call type parameter of enclosing class', () {
    Helper helper = new Helper('''
class A<T, U> {
  f() {
    U();
  }
}
''');
    helper.checkTypeReference(
        'U()',
        'U',
        AccessKind.TYPE_PARAMETER,
        isInvoke: true);
  });

  test('Get function defined at top level', () {
    Helper helper = new Helper('''
g() {}

f() {
  return g;
}
''');
    helper.checkStaticMethod('g', null, 'g', true, isRead: true);
  });

  test('Get function defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.g;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

g() {}
''');
    helper.checkStaticMethod('l.g', null, 'g', true, isRead: true);
  });

  test('Get method defined statically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  static g() {}

  f() {
    return g;
  }
}
''');
    helper.checkStaticMethod('g', 'A', 'g', true, isRead: true);
  });

  test('Get method defined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {
  static g() {}
}
f() {
  return A.g;
}
''');
    helper.checkStaticMethod('A.g', 'A', 'g', true, isRead: true);
  });

  test(
      'Get method defined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.A.g;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {
  static g() {}
}
''');
    helper.checkStaticMethod('l.A.g', 'A', 'g', true, isRead: true);
  });

  test('Get method defined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  g() {}

  f() {
    return g;
  }
}
''');
    helper.checkDynamic('g', null, 'g', isRead: true);
  });

  test(
      'Get method defined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {
  g() {}
}
f(A a) {
  return a.g;
}
''');
    helper.checkDynamic('a.g', 'a', 'g', isRead: true);
  });

  test(
      'Get method defined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {
  g() {}
}
A h() => null;
f() {
  return h().g;
}
''');
    helper.checkDynamic('h().g', 'h()', 'g', isRead: true);
  });

  test('Get method defined locally', () {
    Helper helper = new Helper('''
f() {
  g() {}
  return g;
}
''');
    helper.checkLocalFunction('g', 'g', isRead: true);
  });

  test('Get variable defined at top level', () {
    Helper helper = new Helper('''
var x;

f() {
  return x;
}
''');
    helper.checkStaticField('x', null, 'x', isRead: true);
  });

  test('Get variable defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.x;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

var x;
''');
    helper.checkStaticField('l.x', null, 'x', isRead: true);
  });

  test('Get field defined statically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  static var x;

  f() {
    return x;
  }
}
''');
    helper.checkStaticField('x', 'A', 'x', isRead: true);
  });

  test('Get field defined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {
  static var x;
}

f() {
  return A.x;
}
''');
    helper.checkStaticField('A.x', 'A', 'x', isRead: true);
  });

  test(
      'Get field defined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.A.x;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {
  static var x;
}
''');
    helper.checkStaticField('l.A.x', 'A', 'x', isRead: true);
  });

  test('Get field defined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  var x;

  f() {
    return x;
  }
}
''');
    helper.checkDynamic('x', null, 'x', isRead: true);
  });

  test(
      'Get field defined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {
  var x;
}

f(A a) {
  return a.x;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isRead: true);
  });

  test(
      'Get field defined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {
  var x;
}

A h() => null;

f() {
  return h().x;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isRead: true);
  });

  test(
      'Get field defined dynamically in class from outside class via dynamic var',
      () {
    Helper helper = new Helper('''
f(a) {
  return a.x;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isRead: true);
  });

  test(
      'Get field defined dynamically in class from outside class via dynamic expression',
      () {
    Helper helper = new Helper('''
h() => null;

f() {
  return h().x;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isRead: true);
  });

  test('Get variable defined locally', () {
    Helper helper = new Helper('''
f() {
  var x;
  return x;
}
''');
    helper.checkLocalVariable('x', 'x', isRead: true);
  });

  test('Get variable defined in parameter', () {
    Helper helper = new Helper('''
f(x) {
  return x;
}
''');
    helper.checkParameter('x', 'x', isRead: true);
  });

  test('Get accessor defined at top level', () {
    Helper helper = new Helper('''
get x => null;

f() {
  return x;
}
''');
    helper.checkStaticProperty('x', null, 'x', true, isRead: true);
  });

  test('Get accessor defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.x;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

get x => null;
''');
    helper.checkStaticProperty('l.x', null, 'x', true, isRead: true);
  });

  test('Get accessor defined statically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  static get x => null;

  f() {
    return x;
  }
}
''');
    helper.checkStaticProperty('x', 'A', 'x', true, isRead: true);
  });

  test('Get accessor defined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {
  static get x => null;
}

f() {
  return A.x;
}
''');
    helper.checkStaticProperty('A.x', 'A', 'x', true, isRead: true);
  });

  test(
      'Get accessor defined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.A.x;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {
  static get x => null;
}
''');
    helper.checkStaticProperty('l.A.x', 'A', 'x', true, isRead: true);
  });

  test('Get accessor defined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  get x => null;

  f() {
    return x;
  }
}
''');
    helper.checkDynamic('x', null, 'x', isRead: true);
  });

  test(
      'Get accessor defined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {
  get x => null;
}

f(A a) {
  return a.x;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isRead: true);
  });

  test(
      'Get accessor defined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {
  get x => null;
}

A h() => null;

f() {
  return h().x;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isRead: true);
  });

  test(
      'Get accessor defined dynamically in class from outside class via dynamic var',
      () {
    Helper helper = new Helper('''
f(a) {
  return a.x;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isRead: true);
  });

  test(
      'Get accessor defined dynamically in class from outside class via dynamic expression',
      () {
    Helper helper = new Helper('''
h() => null;

f() {
  return h().x;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isRead: true);
  });

  test('Get accessor undefined at top level', () {
    Helper helper = new Helper('''
f() {
  return x;
}
''');
    // Undefined top level property accesses are treated as dynamic.
    // TODO(paulberry): not sure if this is a good idea.  In general, when such
    // an access appears inside an instance method, it is dynamic, because
    // "this" might be an instance of a derived class that implements x.
    // However, in this case, we are not inside an instance method, so we know
    // that the target is undefined.
    helper.checkDynamic('x', null, 'x', isRead: true);
  });

  test('Get accessor undefined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.x;
}
''');
    helper.addFile('/lib.dart', '''
library lib;
''');
    // Undefined top level property accesses are treated as dynamic.
    // TODO(paulberry): not sure if this is a good idea, for similar reasons to
    // the case above.
    helper.checkDynamic('l.x', null, 'x', isRead: true);
  });

  test('Get accessor undefined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {}

f() {
  return A.x;
}
''');
    helper.checkStaticProperty('A.x', 'A', 'x', false, isRead: true);
  });

  test(
      'Get accessor undefined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  return l.A.x;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {}
''');
    helper.checkStaticProperty('l.A.x', 'A', 'x', false, isRead: true);
  });

  test('Get accessor undefined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  f() {
    return x;
  }
}
''');
    helper.checkDynamic('x', null, 'x', isRead: true);
  });

  test(
      'Get accessor undefined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {}

f(A a) {
  return a.x;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isRead: true);
  });

  test(
      'Get accessor undefined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {}

A h() => null;

f() {
  return h().x;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isRead: true);
  });

  test('Get class defined at top level', () {
    Helper helper = new Helper('''
class A {}
var t = A;
''');
    helper.checkTypeReference('A', 'A', AccessKind.TOPLEVEL_TYPE, isRead: true);
  });

  test('Get class defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

var t = l.A;
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {}
''');
    helper.checkTypeReference(
        'l.A',
        'A',
        AccessKind.TOPLEVEL_TYPE,
        isRead: true);
  });

  test('Get dynamic type', () {
    Helper helper = new Helper('''
var t = dynamic;
''');
    helper.checkTypeReference(
        'dynamic',
        'dynamic',
        AccessKind.TOPLEVEL_TYPE,
        isRead: true);
  });

  test('Get function typedef defined at top level', () {
    Helper helper = new Helper('''
typedef F();
var t = F;
''');
    helper.checkTypeReference('F', 'F', AccessKind.TOPLEVEL_TYPE, isRead: true);
  });

  test('Get function typedef defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

var t = l.F;
''');
    helper.addFile('/lib.dart', '''
library lib;

typedef F();
''');
    helper.checkTypeReference(
        'l.F',
        'F',
        AccessKind.TOPLEVEL_TYPE,
        isRead: true);
  });

  test('Get mixin application defined at top level', () {
    Helper helper = new Helper('''
class A {}
class B {}
class C = A with B;
var t = C;
''');
    helper.checkTypeReference('C', 'C', AccessKind.TOPLEVEL_TYPE, isRead: true);
  });

  test('Get mixin application defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

var t = l.C;
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {}
class B {}
class C = A with B;
''');
    helper.checkTypeReference(
        'l.C',
        'C',
        AccessKind.TOPLEVEL_TYPE,
        isRead: true);
  });

  test('Get type parameter of enclosing class', () {
    Helper helper = new Helper('''
class A<T, U> {
  f() {
    var t = U;
  }
}
''');
    helper.checkTypeReference(
        'U',
        'U',
        AccessKind.TYPE_PARAMETER,
        isRead: true);
  });

  test('Set variable defined at top level', () {
    Helper helper = new Helper('''
var x;

f() {
  x = 1;
}
''');
    helper.checkStaticField('x', null, 'x', isWrite: true);
  });

  test('Set variable defined at top level in foreach loop', () {
    Helper helper = new Helper('''
var x;

f() {
  for (x in []) {}
}
''');
    helper.checkStaticField('x', null, 'x', isWrite: true);
  });

  test('Set variable defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.x = 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

var x;
''');
    helper.checkStaticField('l.x', null, 'x', isWrite: true);
  });

  test('Set field defined statically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  static var x;

  f() {
    x = 1;
  }
}
''');
    helper.checkStaticField('x', 'A', 'x', isWrite: true);
  });

  test(
      'Set field defined statically in class from inside class in foreach' + ' loop',
      () {
    Helper helper = new Helper('''
class A {
  static var x;

  f() {
    for (x in []) {}
  }
}
''');
    helper.checkStaticField('x', 'A', 'x', isWrite: true);
  });

  test('Set field defined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {
  static var x;
}

f() {
  A.x = 1;
}
''');
    helper.checkStaticField('A.x', 'A', 'x', isWrite: true);
  });

  test(
      'Set field defined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.A.x = 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {
  static var x;
}
''');
    helper.checkStaticField('l.A.x', 'A', 'x', isWrite: true);
  });

  test('Set field defined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  var x;

  f() {
    x = 1;
  }
}
''');
    helper.checkDynamic('x', null, 'x', isWrite: true);
  });

  test(
      'Set field defined dynamically in class from inside class in foreach' + ' loop',
      () {
    Helper helper = new Helper('''
class A {
  var x;

  f() {
    for (x in []) {}
  }
}
''');
    helper.checkDynamic('x', null, 'x', isWrite: true);
  });

  test(
      'Set field defined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {
  var x;
}

f(A a) {
  a.x = 1;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isWrite: true);
  });

  test(
      'Set field defined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {
  var x;
}

A h() => null;

f() {
  h().x = 1;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isWrite: true);
  });

  test('Set variable defined locally', () {
    Helper helper = new Helper('''
f() {
  var x;
  x = 1;
}
''');
    helper.checkLocalVariable('x', 'x', isWrite: true);
  });

  test('Set variable defined locally in foreach loop', () {
    Helper helper = new Helper('''
f() {
  var x;
  for (x in []) {}
}
''');
    helper.checkLocalVariable('x', 'x', isWrite: true);
  });

  test('Set variable defined in parameter', () {
    Helper helper = new Helper('''
f(x) {
  x = 1;
}
''');
    helper.checkParameter('x', 'x', isWrite: true);
  });

  test('Set variable defined in parameter in foreach loop', () {
    Helper helper = new Helper('''
f(x) {
  for (x in []) {}
}
''');
    helper.checkParameter('x', 'x', isWrite: true);
  });

  test('Set accessor defined at top level', () {
    Helper helper = new Helper('''
set x(value) {};

f() {
  x = 1;
}
''');
    helper.checkStaticProperty('x', null, 'x', true, isWrite: true);
  });

  test('Set accessor defined at top level in foreach loop', () {
    Helper helper = new Helper('''
set x(value) {};

f() {
  for (x in []) {}
}
''');
    helper.checkStaticProperty('x', null, 'x', true, isWrite: true);
  });

  test('Set accessor defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.x = 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

set x(value) {};
''');
    helper.checkStaticProperty('l.x', null, 'x', true, isWrite: true);
  });

  test('Set accessor defined statically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  static set x(value) {}

  f() {
    x = 1;
  }
}
''');
    helper.checkStaticProperty('x', 'A', 'x', true, isWrite: true);
  });

  test(
      'Set accessor defined statically in class from inside class in' +
          ' foreach loop',
      () {
    Helper helper = new Helper('''
class A {
  static set x(value) {}

  f() {
    for (x in []) {}
  }
}
''');
    helper.checkStaticProperty('x', 'A', 'x', true, isWrite: true);
  });

  test('Set accessor defined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {
  static set x(value) {}
}

f() {
  A.x = 1;
}
''');
    helper.checkStaticProperty('A.x', 'A', 'x', true, isWrite: true);
  });

  test(
      'Set accessor defined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.A.x = 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {
  static set x(value) {}
}
''');
    helper.checkStaticProperty('l.A.x', 'A', 'x', true, isWrite: true);
  });

  test('Set accessor defined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  set x(value) {}

  f() {
    x = 1;
  }
}
''');
    helper.checkDynamic('x', null, 'x', isWrite: true);
  });

  test(
      'Set accessor defined dynamically in class from inside class in' +
          ' foreach loop',
      () {
    Helper helper = new Helper('''
class A {
  set x(value) {}

  f() {
    for (x in []) {}
  }
}
''');
    helper.checkDynamic('x', null, 'x', isWrite: true);
  });

  test(
      'Set accessor defined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {
  set x(value) {}
}

f(A a) {
  a.x = 1;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isWrite: true);
  });

  test(
      'Set accessor defined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {
  set x(value) {}
}

A h() => null;

f() {
  h().x = 1;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isWrite: true);
  });

  test(
      'Set accessor defined dynamically in class from outside class via dynamic var',
      () {
    Helper helper = new Helper('''
f(a) {
  a.x = 1;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isWrite: true);
  });

  test(
      'Set accessor defined dynamically in class from outside class via dynamic expression',
      () {
    Helper helper = new Helper('''
h() => null;

f() {
  h().x = 1;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isWrite: true);
  });

  test('Set accessor undefined at top level', () {
    Helper helper = new Helper('''
f() {
  x = 1;
}
''');
    helper.checkDynamic('x', null, 'x', isWrite: true);
  });

  test('Set accessor undefined at top level in foreach loop', () {
    Helper helper = new Helper('''
f() {
  for (x in []) {}
}
''');
    helper.checkDynamic('x', null, 'x', isWrite: true);
  });

  test('Set accessor undefined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.x = 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;
''');
    helper.checkDynamic('l.x', null, 'x', isWrite: true);
  });

  test('Set accessor undefined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {}

f() {
  A.x = 1;
}
''');
    helper.checkStaticProperty('A.x', 'A', 'x', false, isWrite: true);
  });

  test(
      'Set accessor undefined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.A.x = 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {}
''');
    helper.checkStaticProperty('l.A.x', 'A', 'x', false, isWrite: true);
  });

  test('Set accessor undefined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  f() {
    x = 1;
  }
}
''');
    helper.checkDynamic('x', null, 'x', isWrite: true);
  });

  test(
      'Set accessor undefined dynamically in class from inside class in' +
          ' foreach loop',
      () {
    Helper helper = new Helper('''
class A {
  f() {
    for (x in []) {}
  }
}
''');
    helper.checkDynamic('x', null, 'x', isWrite: true);
  });

  test(
      'Set accessor undefined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {}

f(A a) {
  a.x = 1;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isWrite: true);
  });

  test(
      'Set accessor undefined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {}

A h() => null;

f() {
  h().x = 1;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isWrite: true);
  });

  test('RMW variable defined at top level', () {
    Helper helper = new Helper('''
var x;

f() {
  x += 1;
}
''');
    helper.checkStaticField('x', null, 'x', isRead: true, isWrite: true);
  });

  test('RMW variable defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.x += 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

var x;
''');
    helper.checkStaticField('l.x', null, 'x', isRead: true, isWrite: true);
  });

  test('RMW field defined statically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  static var x;

  f() {
    x += 1;
  }
}
''');
    helper.checkStaticField('x', 'A', 'x', isRead: true, isWrite: true);
  });

  test('RMW field defined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {
  static var x;
}

f() {
  A.x += 1;
}
''');
    helper.checkStaticField('A.x', 'A', 'x', isRead: true, isWrite: true);
  });

  test(
      'RMW field defined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.A.x += 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {
  static var x;
}
''');
    helper.checkStaticField('l.A.x', 'A', 'x', isRead: true, isWrite: true);
  });

  test('RMW field defined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  var x;

  f() {
    x += 1;
  }
}
''');
    helper.checkDynamic('x', null, 'x', isRead: true, isWrite: true);
  });

  test(
      'RMW field defined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {
  var x;
}

f(A a) {
  a.x += 1;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isRead: true, isWrite: true);
  });

  test(
      'RMW field defined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {
  var x;
}

A h() => null;

f() {
  h().x += 1;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isRead: true, isWrite: true);
  });

  test('RMW variable defined locally', () {
    Helper helper = new Helper('''
f() {
  var x;
  x += 1;
}
''');
    helper.checkLocalVariable('x', 'x', isRead: true, isWrite: true);
  });

  test('RMW variable defined in parameter', () {
    Helper helper = new Helper('''
f(x) {
  x += 1;
}
''');
    helper.checkParameter('x', 'x', isRead: true, isWrite: true);
  });

  test('RMW accessor defined at top level', () {
    Helper helper = new Helper('''
set x(value) {};

f() {
  x += 1;
}
''');
    helper.checkStaticProperty(
        'x',
        null,
        'x',
        true,
        isRead: true,
        isWrite: true);
  });

  test('RMW accessor defined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.x += 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

set x(value) {};
''');
    helper.checkStaticProperty(
        'l.x',
        null,
        'x',
        true,
        isRead: true,
        isWrite: true);
  });

  test('RMW accessor defined statically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  static set x(value) {}

  f() {
    x += 1;
  }
}
''');
    helper.checkStaticProperty(
        'x',
        'A',
        'x',
        true,
        isRead: true,
        isWrite: true);
  });

  test('RMW accessor defined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {
  static set x(value) {}
}

f() {
  A.x += 1;
}
''');
    helper.checkStaticProperty(
        'A.x',
        'A',
        'x',
        true,
        isRead: true,
        isWrite: true);
  });

  test(
      'RMW accessor defined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.A.x += 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {
  static set x(value) {}
}
''');
    helper.checkStaticProperty(
        'l.A.x',
        'A',
        'x',
        true,
        isRead: true,
        isWrite: true);
  });

  test('RMW accessor defined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  set x(value) {}

  f() {
    x += 1;
  }
}
''');
    helper.checkDynamic('x', null, 'x', isRead: true, isWrite: true);
  });

  test(
      'RMW accessor defined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {
  set x(value) {}
}

f(A a) {
  a.x += 1;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isRead: true, isWrite: true);
  });

  test(
      'RMW accessor defined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {
  set x(value) {}
}

A h() => null;

f() {
  h().x += 1;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isRead: true, isWrite: true);
  });

  test(
      'RMW accessor defined dynamically in class from outside class via dynamic var',
      () {
    Helper helper = new Helper('''
f(a) {
  a.x += 1;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isRead: true, isWrite: true);
  });

  test(
      'RMW accessor defined dynamically in class from outside class via dynamic expression',
      () {
    Helper helper = new Helper('''
h() => null;

f() {
  h().x += 1;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isRead: true, isWrite: true);
  });

  test('RMW accessor undefined at top level', () {
    Helper helper = new Helper('''
f() {
  x += 1;
}
''');
    helper.checkDynamic('x', null, 'x', isRead: true, isWrite: true);
  });

  test('RMW accessor undefined at top level via prefix', () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.x += 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;
''');
    helper.checkDynamic('l.x', null, 'x', isRead: true, isWrite: true);
  });

  test('RMW accessor undefined statically in class from outside class', () {
    Helper helper = new Helper('''
class A {}

f() {
  A.x += 1;
}
''');
    helper.checkStaticProperty(
        'A.x',
        'A',
        'x',
        false,
        isRead: true,
        isWrite: true);
  });

  test(
      'RMW accessor undefined statically in class from outside class via prefix',
      () {
    Helper helper = new Helper('''
import 'lib.dart' as l;

f() {
  l.A.x += 1;
}
''');
    helper.addFile('/lib.dart', '''
library lib;

class A {}
''');
    helper.checkStaticProperty(
        'l.A.x',
        'A',
        'x',
        false,
        isRead: true,
        isWrite: true);
  });

  test('RMW accessor undefined dynamically in class from inside class', () {
    Helper helper = new Helper('''
class A {
  f() {
    x += 1;
  }
}
''');
    helper.checkDynamic('x', null, 'x', isRead: true, isWrite: true);
  });

  test(
      'RMW accessor undefined dynamically in class from outside class via typed var',
      () {
    Helper helper = new Helper('''
class A {}

f(A a) {
  a.x += 1;
}
''');
    helper.checkDynamic('a.x', 'a', 'x', isRead: true, isWrite: true);
  });

  test(
      'RMW accessor undefined dynamically in class from outside class via typed expression',
      () {
    Helper helper = new Helper('''
class A {}

A h() => null;

f() {
  h().x += 1;
}
''');
    helper.checkDynamic('h().x', 'h()', 'x', isRead: true, isWrite: true);
  });
}

typedef void AccessHandler(Expression node, AccessSemantics semantics);

class Helper {
  final MemoryResourceProvider provider = new MemoryResourceProvider();
  Source rootSource;
  AnalysisContext context;

  Helper(String rootContents) {
    DartSdk sdk = new MockSdk();
    String rootFile = '/root.dart';
    File file = provider.newFile(rootFile, rootContents);
    rootSource = file.createSource();
    context = AnalysisEngine.instance.createAnalysisContext();
    // Set up the source factory.
    List<UriResolver> uriResolvers = [
        new ResourceUriResolver(provider),
        new DartUriResolver(sdk)];
    context.sourceFactory = new SourceFactory(uriResolvers);
    // add the Source
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(rootSource);
    context.applyChanges(changeSet);
  }

  LibraryElement get libraryElement {
    return context.computeLibraryElement(rootSource);
  }

  void addFile(String path, String contents) {
    provider.newFile(path, contents);
  }

  /**
   * Verify that the node represented by [expectedSource] is classified as
   * a dynamic method invocation.
   */
  void checkDynamic(String expectedSource, String expectedTarget,
      String expectedName, {bool isRead: false, bool isWrite: false, bool isInvoke:
      false}) {
    TestVisitor visitor = new TestVisitor();
    int count = 0;
    visitor.onAccess = (AstNode node, AccessSemantics semantics) {
      count++;
      expect(node.toSource(), equals(expectedSource));
      expect(semantics.kind, equals(AccessKind.DYNAMIC));
      if (expectedTarget == null) {
        expect(semantics.target, isNull);
      } else {
        expect(semantics.target.toSource(), equals(expectedTarget));
      }
      expect(semantics.identifier.name, equals(expectedName));
      expect(semantics.element, isNull);
      expect(semantics.classElement, isNull);
      expect(semantics.isRead, equals(isRead));
      expect(semantics.isWrite, equals(isWrite));
      expect(semantics.isInvoke, equals(isInvoke));
    };
    libraryElement.unit.accept(visitor);
    expect(count, equals(1));
  }

  /**
   * Verify that the node represented by [expectedSource] is classified as
   * a local function invocation.
   */
  void checkLocalFunction(String expectedSource, String expectedName,
      {bool isRead: false, bool isWrite: false, bool isInvoke: false}) {
    TestVisitor visitor = new TestVisitor();
    int count = 0;
    visitor.onAccess = (AstNode node, AccessSemantics semantics) {
      count++;
      expect(node.toSource(), equals(expectedSource));
      expect(semantics.kind, equals(AccessKind.LOCAL_FUNCTION));
      expect(semantics.identifier.name, equals(expectedName));
      expect(semantics.element.displayName, equals(expectedName));
      expect(semantics.classElement, isNull);
      expect(semantics.target, isNull);
      expect(semantics.isRead, equals(isRead));
      expect(semantics.isWrite, equals(isWrite));
      expect(semantics.isInvoke, equals(isInvoke));
    };
    libraryElement.unit.accept(visitor);
    expect(count, equals(1));
  }

  /**
   * Verify that the node represented by [expectedSource] is classified as
   * a local variable access.
   */
  void checkLocalVariable(String expectedSource, String expectedName,
      {bool isRead: false, bool isWrite: false, bool isInvoke: false}) {
    TestVisitor visitor = new TestVisitor();
    int count = 0;
    visitor.onAccess = (AstNode node, AccessSemantics semantics) {
      count++;
      expect(node.toSource(), equals(expectedSource));
      expect(semantics.kind, equals(AccessKind.LOCAL_VARIABLE));
      expect(semantics.element.name, equals(expectedName));
      expect(semantics.classElement, isNull);
      expect(semantics.target, isNull);
      expect(semantics.isRead, equals(isRead));
      expect(semantics.isWrite, equals(isWrite));
      expect(semantics.isInvoke, equals(isInvoke));
    };
    libraryElement.unit.accept(visitor);
    expect(count, equals(1));
  }

  /**
   * Verify that the node represented by [expectedSource] is classified as a
   * parameter access.
   */
  void checkParameter(String expectedSource, String expectedName, {bool isRead:
      false, bool isWrite: false, bool isInvoke: false}) {
    TestVisitor visitor = new TestVisitor();
    int count = 0;
    visitor.onAccess = (AstNode node, AccessSemantics semantics) {
      count++;
      expect(node.toSource(), equals(expectedSource));
      expect(semantics.kind, equals(AccessKind.PARAMETER));
      expect(semantics.element.name, equals(expectedName));
      expect(semantics.classElement, isNull);
      expect(semantics.target, isNull);
      expect(semantics.isRead, equals(isRead));
      expect(semantics.isWrite, equals(isWrite));
      expect(semantics.isInvoke, equals(isInvoke));
    };
    libraryElement.unit.accept(visitor);
    expect(count, equals(1));
  }

  /**
   * Verify that the node represented by [expectedSource] is classified as
   * a static field element reference.
   */
  void checkStaticField(String expectedSource, String expectedClass,
      String expectedName, {bool isRead: false, bool isWrite: false, bool isInvoke:
      false}) {
    TestVisitor visitor = new TestVisitor();
    int count = 0;
    visitor.onAccess = (Expression node, AccessSemantics semantics) {
      count++;
      expect(node.toSource(), equals(expectedSource));
      expect(semantics.kind, equals(AccessKind.STATIC_FIELD));
      expect(semantics.identifier.name, equals(expectedName));
      expect(semantics.element.displayName, equals(expectedName));
      if (expectedClass == null) {
        expect(semantics.classElement, isNull);
      } else {
        expect(semantics.classElement, isNotNull);
        expect(semantics.classElement.displayName, equals(expectedClass));
      }
      expect(semantics.target, isNull);
      expect(semantics.isRead, equals(isRead));
      expect(semantics.isWrite, equals(isWrite));
      expect(semantics.isInvoke, equals(isInvoke));
    };
    libraryElement.unit.accept(visitor);
    expect(count, equals(1));
  }

  /**
   * Verify that the node represented by [expectedSource] is classified as
   * a static method.
   */
  void checkStaticMethod(String expectedSource, String expectedClass,
      String expectedName, bool defined, {bool isRead: false, bool isWrite: false,
      bool isInvoke: false}) {
    TestVisitor visitor = new TestVisitor();
    int count = 0;
    visitor.onAccess = (AstNode node, AccessSemantics semantics) {
      count++;
      expect(node.toSource(), equals(expectedSource));
      expect(semantics.kind, equals(AccessKind.STATIC_METHOD));
      expect(semantics.identifier.name, equals(expectedName));
      if (expectedClass == null) {
        expect(semantics.classElement, isNull);
        if (defined) {
          expect(semantics.element, new isInstanceOf<FunctionElement>());
        }
      } else {
        expect(semantics.classElement, isNotNull);
        expect(semantics.classElement.displayName, equals(expectedClass));
        if (defined) {
          expect(semantics.element, new isInstanceOf<MethodElement>());
        }
      }
      if (defined) {
        expect(semantics.element.displayName, equals(expectedName));
      } else {
        expect(semantics.element, isNull);
      }
      expect(semantics.target, isNull);
      expect(semantics.isRead, equals(isRead));
      expect(semantics.isWrite, equals(isWrite));
      expect(semantics.isInvoke, equals(isInvoke));
    };
    libraryElement.unit.accept(visitor);
    expect(count, equals(1));
  }

  /**
   * Verify that the node represented by [expectedSource] is classified as
   * a static property access.
   */
  void checkStaticProperty(String expectedSource, String expectedClass,
      String expectedName, bool defined, {bool isRead: false, bool isWrite: false,
      bool isInvoke: false}) {
    TestVisitor visitor = new TestVisitor();
    int count = 0;
    visitor.onAccess = (Expression node, AccessSemantics semantics) {
      count++;
      expect(node.toSource(), equals(expectedSource));
      expect(semantics.kind, equals(AccessKind.STATIC_PROPERTY));
      expect(semantics.identifier.name, equals(expectedName));
      if (expectedClass == null) {
        expect(semantics.classElement, isNull);
      } else {
        expect(semantics.classElement, isNotNull);
        expect(semantics.classElement.displayName, equals(expectedClass));
      }
      if (defined) {
        expect(semantics.element.displayName, equals(expectedName));
      } else {
        expect(semantics.element, isNull);
      }
      expect(semantics.target, isNull);
      expect(semantics.isRead, equals(isRead));
      expect(semantics.isWrite, equals(isWrite));
      expect(semantics.isInvoke, equals(isInvoke));
    };
    libraryElement.unit.accept(visitor);
    expect(count, equals(1));
  }

  /**
   * Verify that the node represented by [expectedSource] is classified as a
   * reference to a toplevel class or a type parameter.
   */
  void checkTypeReference(String expectedSource, String expectedName,
      AccessKind expectedKind, {bool isRead: false, bool isInvoke: false}) {
    TestVisitor visitor = new TestVisitor();
    int count = 0;
    visitor.onAccess = (AstNode node, AccessSemantics semantics) {
      count++;
      expect(node.toSource(), equals(expectedSource));
      expect(semantics.kind, equals(expectedKind));
      expect(semantics.element.name, equals(expectedName));
      expect(semantics.classElement, isNull);
      expect(semantics.target, isNull);
      expect(semantics.isRead, equals(isRead));
      expect(semantics.isWrite, isFalse);
      expect(semantics.isInvoke, equals(isInvoke));
    };
    libraryElement.unit.accept(visitor);
    expect(count, equals(1));
  }
}

/**
 * Visitor class used to run the tests.
 */
class TestVisitor extends RecursiveAstVisitor {
  AccessHandler onAccess;

  @override
  visitMethodInvocation(MethodInvocation node) {
    onAccess(node, node.accept(ACCESS_SEMANTICS_VISITOR));
  }

  @override
  visitPrefixedIdentifier(PrefixedIdentifier node) {
    onAccess(node, node.accept(ACCESS_SEMANTICS_VISITOR));
  }

  @override
  visitPropertyAccess(PropertyAccess node) {
    onAccess(node, node.accept(ACCESS_SEMANTICS_VISITOR));
  }

  @override
  visitSimpleIdentifier(SimpleIdentifier node) {
    AccessSemantics semantics = node.accept(ACCESS_SEMANTICS_VISITOR);
    if (semantics != null) {
      onAccess(node, semantics);
    }
  }
}
