// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// The next line is used to tell test.dart that this test is run by invoking
// awaitc.dart and passing this file as an argument (e.g. frog
// frog/await/awaitc.dart test.dart):
// VMOptions=frog/await/awaitc.dart

// Await within a loop:

#import("await_test_helper.dart");

main() {
  var x = 0;
  while (x < 3) {
    final f = futureOf(1);
    final t = await f;
    x += t;
  }
  Expect.equals(x, 3);
}

// This is roughly equivalent to:
// main() {
//   final _ret = new Completer();
//   var x = 0;
//   _after_loop_1() {
//     Expect.equals(x, 3);
//     _ret.complete(null);
//   }
//   _loop_1() {
//     while (x < 3) {
//       final f = futureOf(1);
//       f.then((t) {
//         x += t;
//         _loop_1();
//       });
//       Futures.propagateError(f, _ret);
//       return;
//     }
//     _after_loop_1();
//   }();
//   return _ret;
// }
