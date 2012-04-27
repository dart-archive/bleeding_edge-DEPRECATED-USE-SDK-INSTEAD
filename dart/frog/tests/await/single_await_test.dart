// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Simple test that transforms the await keyword used at the top-level of a
// function body.

#import("await_test_helper.dart");

main() {
  final f = futureOf(3);
  final x = await f;
  Expect.equals(3, x);
}

// This is roughly equivalent to:
// main() {
//   final _ret = new Completer();
//   final f = futureOf(3);
//   f.then((x) {
//     Expect.equals(3, x);
//     _ret.complete(null);
//   });
//   Futures.propagateError(f, _ret);
//   return _ret.future;
// }
