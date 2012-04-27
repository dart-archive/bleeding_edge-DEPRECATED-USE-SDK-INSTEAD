// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await expressions used at the statement level (no assignment).
#import("await_test_helper.dart");

main() {
  final f = futureOf(3);
  Expect.equals(false, f.hasValue);
  await f;
  Expect.equals(true, f.hasValue);
  Expect.equals(3, f.value);
}

// This is roughly equivalent to:
// main() {
//   final _ret = new Completer();
//   final f = futureOf(3);
//   Expect.equals(false, f.hasValue);
//   f.then((_) {
//     Expect.equals(true, f.hasValue);
//     Expect.equals(3, f.value);
//     _ret.complete(null);
//   });
//   Futures.propagateError(f, _ret);
//   return _ret.future;
// }
