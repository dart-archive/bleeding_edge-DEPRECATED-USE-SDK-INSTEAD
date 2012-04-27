// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a lambda function.

#import("await_test_helper.dart");

main() {
  bool called = false;
  final f2 = () {
    final f = futureOf(3);
    final x = await f;
    Expect.equals(3, x);
    called = true;
  }();
  Expect.equals(false, called);
  await f2;
  Expect.equals(true, called);
}

// This is roughly equivalent to:
// main() {
//   final _ret0 = new Completer();
//   final f2 = (function() {
//     final _ret1 = new Completer();
//     final f = futureOf(3);
//     f.then((x) {
//       Expect.equals(3, x);
//       called = true;
//       _ret.complete(null);
//     });
//     Futures.propagateError(f, _ret);
//     return _ret1.future;
//   })();
//  Expect.equals(false, called);
//  f2.then((v) {
//    Expect.equals(true, called);
//  });
//  return _ret0.future;
// }
