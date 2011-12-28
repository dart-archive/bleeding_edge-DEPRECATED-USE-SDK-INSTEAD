// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// A library used by await tests.
#library("await_test_helper.dart");

#import("../../../lib/node/node.dart");

/** Returns a future that completes with [value] after calling [setTimeout]. */
Future futureOf(value) {
  final c = new Completer();
  final f = c.future;
  setTimeout(() { c.complete(value); }, 0);
  return f;
}

Future errorOf(error) {
  final c = new Completer();
  final f = c.future;
  setTimeout(() { c.completeException(error); }, 0);
  return f;
}
