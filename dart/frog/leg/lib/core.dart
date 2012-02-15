// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('core');

#import('coreimpl.dart');

#import('js_helper.dart'); // TODO(ahe): remove this import.

#source('../../../corelib/src/bool.dart');
#source('../../../corelib/src/collection.dart');
#source('../../../corelib/src/comparable.dart');
#source('../../../corelib/src/date.dart');
#source('../../../corelib/src/double.dart');
#source('../../../corelib/src/duration.dart');
// #source('../../../corelib/src/exceptions.dart');
// #source('../../../corelib/src/expect.dart');
#source('../../../corelib/src/function.dart');
#source('../../../corelib/src/future.dart');
#source('../../../corelib/src/hashable.dart');
#source('../../../corelib/src/int.dart');
#source('../../../corelib/src/isolate.dart');
#source('../../../corelib/src/iterable.dart');
#source('../../../corelib/src/iterator.dart');
// #source('../../../corelib/src/list.dart');
#source('../../../corelib/src/map.dart');
#source('../../../corelib/src/math.dart');
#source('../../../corelib/src/num.dart');
#source('../../../corelib/src/options.dart');
#source('../../../corelib/src/pattern.dart');
#source('../../../corelib/src/promise.dart');
#source('../../../corelib/src/queue.dart');
#source('../../../corelib/src/regexp.dart');
#source('../../../corelib/src/set.dart');
// #source('../../../corelib/src/stopwatch.dart');
#source('../../../corelib/src/string.dart');
#source('../../../corelib/src/string_buffer.dart');
#source('../../../corelib/src/strings.dart');
#source('../../../corelib/src/time_zone.dart');

void print(var obj) => Primitives.printString(obj.toString());

class Object {
  String toString() => Primitives.objectToString(this);

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

class List<T> implements Iterable<T> {
  factory List([int length]) => Primitives.newList(length);
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
      startMs = Primitives.dateNow();
    }
  }

  void stop() {
    if (startMs == null) return;
    elapsedMs += Primitives.dateNow() - startMs;
    startMs = null;
  }

  void reset() {
    elapsedMs = 0.0;
    if (startMs == null) return;
    startMs = Primitives.dateNow();
  }

  int elapsed() {
    return elapsedInMs();
  }

  int elapsedInUs() {
    return elapsedInMs() * 1000;
  }

  int elapsedInMs() {
    if (startMs == null) {
      return Primitives.mathFloor(elapsedMs);
    } else {
      return Primitives.mathFloor(elapsedMs + (Primitives.dateNow() - startMs));
    }
  }

  int frequency() {
    return 1000;
  }
}
