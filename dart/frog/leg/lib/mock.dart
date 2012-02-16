// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Mocks of things that Leg cannot read directly.

// TODO(ahe): Remove this file.

interface Exception default ExceptionImplementation {
  const Exception([var msg]);
}
class IndexOutOfRangeException implements Exception {}
class IllegalAccessException implements Exception {}
class ClosureArgumentMismatchException implements Exception {}
class ObjectNotClosureException implements Exception {}
class IllegalArgumentException implements Exception {}
class OutOfMemoryException implements Exception {}
class StackOverflowException implements Exception {}
class BadNumberFormatException implements Exception {}
class WrongArgumentCountException implements Exception {}
class NullPointerException implements Exception {}
class NoMoreElementsException implements Exception {}
class EmptyQueueException implements Exception {}
class UnsupportedOperationException implements Exception {}
class NotImplementedException implements Exception {}
class IllegalJSRegExpException implements Exception {}
class IntegerDivisionByZeroException implements Exception {}

class AssertionError {}
class TypeError extends AssertionError {}
class FallThroughError {}

// TODO(ahe): VM specfic exception?
class InternalError {}

// TODO(ahe): VM specfic exception?
class StaticResolutionException implements Exception {}

class NoSuchMethodException implements Exception {
  Object receiver;
  String name;
  List args;
  NoSuchMethodException(Object this.receiver, String this.name, List this.args);
  String toString() {
    return "NoSuchMethodException - receiver: '$receiver'" +
        "function name: '$name' arguments: [$args]";
  }
}

class List<E> implements Iterable<E> {
  factory List([int length]) => Primitives.newList(length);
  factory List.from(Iterable<E> other) {
    throw 'List.from is not implemented';
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

void assert(condition) {}
