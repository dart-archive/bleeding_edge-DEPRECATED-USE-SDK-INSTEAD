// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a try-catch block:

intFuture(v) {
  final c = new Completer<int>();
  final f = c.future;
  c.complete(v);
  return f;
}

main() {
  try {
    final f = intFuture(1);
    final t = await f;
    return t;
  } catch (Ex1 ex) {
    return -1;
  }
}

// This is roughly equivalent to:
// main() {
//   final _ret = new Completer();
//   final f = intFuture(1);
//   f.then((t) {
//     _ret.complete(t);
//   });
//   f.addExceptionHandler((ex) {
//     if (ex is Ex1) {
//       _ret.complete(-1);
//       return true;
//     }
//     return false;
//   });
//   Futures.propagateError(f, _ret);
// }
