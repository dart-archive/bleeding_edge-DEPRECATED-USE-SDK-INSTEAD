// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a conditional:

intFuture(v) {
  final c = new Completer<int>();
  final f = c.future;
  c.complete(v);
  return f;
}

bool get notAnalyzableCondition() {
  return true;
}

main() {
  var x;
  if (notAnalyzableCondition) {
    final f = intFuture(3);
    x = await f;
  } else {
    x = 3;
  }
  Expect.equals(3, x);
}

// This is roughly equivalent to:
// conditional() {
//   final _ret = new Completer();
//   var x;
//   _after_if() {
//     Expect.equals(x, 3);
//     _ret.complete(null);
//   }
//   if (notAnalyzableCondition) {
//     final f = intFuture(3);
//     f.then((_v) {
//       x = _v;
//       _after_if();
//     });
//     Futures.propagateError(f, _ret);
//   } else {
//     x = 3;
//     _after_if();
//   }
//   return _ret;
// }
