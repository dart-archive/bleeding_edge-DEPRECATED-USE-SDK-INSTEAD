// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A test of simple runtime behavior on numbers, strings and arrays with
 * a focus on both correct behavior and runtime errors.
 *
 * This file is written to use minimal type declarations to match a
 * typical dynamic language coding style.
 */
class CoreRuntimeTypesTest {
  static main() {
    testBooleanOperators();
    testRationalOperators();
    testIntegerOperators();
    testOperatorErrors();
    testRationalMethods();
    testIntegerMethods();
    testStringOperators();
    testStringMethods();
    testArrayOperators();
    testArrayMethods();
    testMapOperators();
    testMapMethods();
    testLiterals();
    testDateMethods();
  }

  // TODO(jimhug): Move the next three methods into a shared utility class.
  static assertEquals(a, b) {
    if (a != b) {
      assertFail("${a} != ${b}");
    }
  }

  static assertFail(message) {
    // TODO(jimhug): Make this throw a real exception.
    //               Logger_println(message);
    assert(false, message);
  }

  static assertTypeError(void f()) {
    try {
      f();
    } catch (exception) {
      assertEquals(exception instanceof TypeError, true);
      return;
    }
    assertFail("no exception thrown");
  }

  static testBooleanOperators() {
    var x = true, y = false;
    assertEquals(x, true);
    assertEquals(y, false);
    assertEquals(x, !y);
    assertEquals(!x, y);
  }


  static testRationalOperators() {
    var x = 10, y = 20;
    assertEquals(x + y, 30);
    assertEquals(x - y, -10);
    assertEquals(x * y, 200);
    assertEquals(x / y, 0.5);
    assertEquals(x ~/ y, 0);
    assertEquals(x % y, 10);
  }

  static testIntegerOperators() {
    var x = 18, y = 17;
    assertEquals(x | y, 19);
    assertEquals(x & y, 16);
    assertEquals(x ^ y, 3);
    assertEquals(2 >> 1, 1);
    assertEquals(1 << 1, 2);
  }

  static testOperatorErrors() {
    var objs = [1, '2', [3], null, true, new Map()];
    for (var i=0; i < objs.length; i++) {
      for (var j=i+1; j < objs.length; j++) {
        testBinaryOperatorErrors(objs[i], objs[j]);
        testBinaryOperatorErrors(objs[j], objs[i]);
      }
      if (objs[i] != 1) {
        testUnaryOperatorErrors(objs[i]);
      }
    }
  }

  static testBinaryOperatorErrors(x, y) {
    // TODO(jimhug): Add this check once String.+ is removed
    //               assertTypeError(function() { x + y;});
    assertTypeError(function() { x - y;});
    assertTypeError(function() { x * y;});
    assertTypeError(function() { x / y;});
    assertTypeError(function() { x | y;});
    assertTypeError(function() { x ^ y;});
    assertTypeError(function() { x & y;});
    assertTypeError(function() { x << y;});
    assertTypeError(function() { x >> y;});
    assertTypeError(function() { x >>> y;});
    assertTypeError(function() { x ~/ y;});
    assertTypeError(function() { x % y;});

    testComparisonOperatorErrors(x, y);
  }

  static testComparisonOperatorErrors(x, y) {
    assertEquals(x == y, false);
    assertEquals(x != y, true);
    assertTypeError(function() { x < y; });
    assertTypeError(function() { x <= y; });
    assertTypeError(function() { x > y; });
    assertTypeError(function() { x >= y; });
  }

  static testUnaryOperatorErrors(x) {
    // TODO(jimhug): Add guard for instanceof Number when instanceof is working
    assertTypeError(function() { ~x;});
    assertTypeError(function() { -x;});
    // TODO(jimhug): Add check for !x as an error when x is not a bool
  }

  static testRationalMethods() {
    var x = 10.6;
    assertEquals(x.abs(), 10.6);
    assertEquals((-x).abs(), 10.6);
    assertEquals(x.round(), 11);
    assertEquals(x.floor(), 10);
    assertEquals(x.ceil(), 11);
  }

  // TODO(jimhug): Determine correct behavior for mixing ints and floats.
  static testIntegerMethods() {
    var y = 9;
    assertEquals(y.isEven, false);
    assertEquals(y.isOdd, true);
    assertEquals(y.toRadixString(2), '1001');
    assertEquals(y.toRadixString(3), '100');
    assertEquals(y.toRadixString(16), '9');
  }

  static testStringOperators() {
    var s = "abcdef";
    assertEquals(s, "abcdef");
    assertEquals(s.charCodeAt(0), 97);
    assertEquals(s[0], 'a');
    assertEquals(s.length, 6);
    assertTypeError(function() { s[null]; });
    assertTypeError(function() { s['hello']; });
    assertTypeError(function() { s[0] = 'x'; });
  }

  // TODO(jimhug): Fill out full set of string methods.
  static testStringMethods() {
    var s = "abcdef";
    assertEquals(s.isEmpty, false);
    assertEquals(s.startsWith("abc"), true);
    assertEquals(s.endsWith("def"), true);
    assertEquals(s.startsWith("aa"), false);
    assertEquals(s.endsWith("ff"), false);
    assertEquals(s.contains('cd'), true);
    assertEquals(s.contains('cd', 2), true);
    assertEquals(s.contains('cd', 3), false);
    assertEquals(s.indexOf('cd'), 2);
    assertEquals(s.indexOf('cd', 2), 2);
    assertEquals(s.indexOf('cd', 3), -1);

    assertTypeError(function() { s.startsWith(1); });
    assertTypeError(function() { s.endsWith(1); });
  }

  static testArrayOperators() {
    var a = [1,2,3,4];
    assertEquals(a[0], 1);
    assertTypeError(function() { a['0']; });
    a[0] = 42;
    assertEquals(a[0], 42);
    assertTypeError(function() { a['0'] = 99; });
    assertEquals(a.length, 4);
  }

  // TODO(jimhug): Fill out full set of array methods.
  static testArrayMethods() {
    var a = [1,2,3,4];
    assertEquals(a.isEmpty, false);
    assertEquals(a.length, 4);
    a.clear();
    assertEquals(a.isEmpty, false);
    assertEquals(a.length, 4);
    assertEquals(a.length, 4);
  }

  static testMapOperators() {
    var d = new Map();
    d['a'] = 1;
    d['b'] = 2;
    assertEquals(d['a'], 1);
    assertEquals(d['b'], 2);
    assertEquals(d['c'], null);
  }

  static testMapMethods() {
    var d = new Map();
    d['a'] = 1;
    d['b'] = 2;
    assertEquals(d.containsValue(2), true);
    assertEquals(d.containsValue(3), false);
    assertEquals(d.containsKey('a'), true);
    assertEquals(d.containsKey('c'), false);
    assertEquals(d.getKeys().length, 2);
    assertEquals(d.getValues().length, 2);

    assertEquals(d.remove('c'), null);
    assertEquals(d.remove('b'), 2);
    assertEquals(d.getKeys(), ['a']);
    assertEquals(d.getValues(), [1]);

    d['c'] = 3;
    d['f'] = 4;
    assertEquals(d.getKeys().length, 3);
    assertEquals(d.getValues().length, 3);
    assertEquals(d.getKeys(), ['a', 'c', 'f']);
    assertEquals(d.getValues(), [1, 3, 4]);

    var count = 0;
    d.forEach(function(key, value) {
      count++;
      assertEquals(value, d[key]);
    });
    assertEquals(count, 3);

    d = { 'a': 1, 'b': 2 };
    assertEquals(d.containsValue(2), true);
    assertEquals(d.containsValue(3), false);
    assertEquals(d.containsKey('a'), true);
    assertEquals(d.containsKey('c'), false);
    assertEquals(d.getKeys().length, 2);
    assertEquals(d.getValues().length, 2);

    d['g'] = null;
    assertEquals(d.containsKey('g'), true);
    assertEquals(d['g'], null);
  }

  static testDateMethods() {
    // TODO(jimhug): Switch to named constructors when available.
    // Pushing this into Jan 2nd to make the year independent of timezone.
    // TODO(jimhug): Pursue a better solution to TZ issues.
    var msec = 115201000;
    var d = new Date.fromMillisecondsSinceEpoch(msec, const TimeZone.utc());
    assertEquals(d.second, 1);
    assertEquals(d.year, 1970);

    d = new Date.now();
    assertEquals(d.year >= 2011, true);
  }

  static testLiterals() {
    true.toString();
    1d.toString();
    .5.toString();
    1.toString();
    if (false) {
      null.toString();
    }
    '${1}'.toString();
    ''.toString();
    ''.endsWith('');
  }
}
