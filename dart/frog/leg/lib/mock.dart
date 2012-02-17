// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Mocks of things that Leg cannot read directly.

// TODO(ahe): Remove this file.

class AssertionError {}
class TypeError extends AssertionError {}
class FallThroughError {}

// TODO(ahe): VM specfic exception?
class InternalError {}

// TODO(ahe): VM specfic exception?
class StaticResolutionException implements Exception {}

class List<E> implements Iterable<E> {
  factory List([int length]) => Primitives.newList(length);
  factory List.from(Iterable<E> other) {
    throw 'List.from is not implemented';
  }
}

void assert(condition) {}
