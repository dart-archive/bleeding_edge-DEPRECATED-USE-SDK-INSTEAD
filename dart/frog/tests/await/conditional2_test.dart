// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// The next line is used to tell test.dart that this test is run by invoking
// awaitc.dart and passing this file as an argument (e.g. frog
// frog/await/awaitc.dart test.dart):
// VMOptions=frog/await/awaitc.dart

// Await within a conditional - the branch non-taken.

#import("await_test_helper.dart");

main() {
  var x;
  if (x != null) {
    final f = futureOf(4);
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
//   if (x != null) {
//     final f = futureOf(4);
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
