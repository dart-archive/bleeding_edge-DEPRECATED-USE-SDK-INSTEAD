// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('core');

#import('coreimpl.dart');

#import('js_helper.dart'); // TODO(ahe): remove this import.

void print(var obj) {
  obj = obj.toString();
  var hasConsole = JS("bool", @"typeof console == 'object'");
  if (hasConsole) {
    JS("void", @"console.log($0)", obj);
  } else {
    JS("void", @"write($0)", obj);
    JS("void", @"write('\n')");
  }
}

/* Include when interfaces are implemented
interface Iterable<T> {
  Iterator<T> iterator();
}

interface Iterator<T> {
  bool hasNext();
  T next();
}
*/
class int {}
class double {}
class String {}
class bool {}
class num {}
class Object {
  String toString() {
    String name = JS('String', @'this.constructor.name');
    if (name === null) {
      name = JS('String', @'$0.match(/^\s*function\s*(\S*)\s*\(/)[1]',
                JS('String', @'this.constructor.toString()'));
    }
    return "Instance of '$name'";
  }

  void noSuchMethod(String name, List args) {
    throw new NoSuchMethodException(this, name, args);
  }
}

class NoSuchMethodException {
  Object receiver;
  String name;
  List args;
  NoSuchMethodException(Object this.receiver, String this.name, List this.args);
  String toString() {
    return "NoSuchMethodException - receiver: '$receiver'" +
        "function name: '$name' arguments: [$args]";
  }
}

class List<T> /* implements Iterable<T> */ {

  factory List([int length]) {
    if (length == null) return JS("Object", @"new Array()");
    if (!(length is int)) throw "Invalid argument";
    if (length < 0) throw "Negative size";
    return JS("Object", @"new Array($0)", length);
  }
}

class ListIterator<T> /* implements Iterator<T> */ {
  int i;
  List<T> list;
  ListIterator(List<T> this.list) : i = 0;
  bool hasNext() => i < JS("int", @"$0.length", list);
  T next() {
    var value = JS("Object", @"$0[$1]", list, i);
    i += 1;
    return value;
  }
}

class Expect {
  // TODO(ngeoffray): add optional message parameters to these
  // methods.
  static void equals(var expected, var actual) {
    if (expected == actual) return;
    _fail('Expect.equals(expected: <$expected>, actual:<$actual> fails.');
  }

  static void fail(String message) {
    _fail("Expect.fail('$message')");
  }

  static void _fail(String message) {
    throw message;
  }

  static void isTrue(var actual) {
    if (actual === true) return;
    _fail("Expect.isTrue($actual) fails.");
  }

  static void isFalse(var actual) {
    if (actual === false) return;
    _fail("Expect.isFalse($actual) fails.");
  }

  static void listEquals(List expected, List actual) {
    // We check on length at the end in order to provide better error
    // messages when an unexpected item is inserted in a list.
    if (expected.length != actual.length) {
      _fail('list not equals');
    }

    for (int i = 0; i < expected.length; i++) {
      if (expected[i] != actual[i]) {
        _fail('list not equals');
      }
    }
  }
}

class Stopwatch {
  double startMs;
  double elapsedMs;

  Stopwatch() {
    elapsedMs = 0.0;
  }

  void start() {
    if (startMs == null) {
      startMs = JS("num", @"Date.now()");
    }
  }

  void stop() {
    if (startMs == null) return;
    elapsedMs += JS("num", @"Date.now() - $0", startMs);
    startMs = null;
  }

  void reset() {
    elapsedMs = 0.0;
    if (startMs == null) return;
    startMs = JS("num", @"Date.now()");
  }

  int elapsed() {
    return elapsedInMs();
  }

  int elapsedInUs() {
    return elapsedInMs() * 1000;
  }

  int elapsedInMs() {
    if (startMs == null) {
      return JS("num", @"Math.floor($0)", elapsedMs);
    } else {
      return
        JS("num", @"Math.floor($0 + (Date.now() - $1))", elapsedMs, startMs);
    }
  }

  int frequency() {
    return 1000;
  }
}
