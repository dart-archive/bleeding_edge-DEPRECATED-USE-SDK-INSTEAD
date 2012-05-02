// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// The next line is used to tell test.dart that this test is run by invoking
// awaitc.dart and passing this file as an argument (e.g. frog
// frog/await/awaitc.dart test.dart):
// VMOptions=frog/await/awaitc.dart

// Await within a try-catch block, error before the await, but there is code
// after the try-catch block.

#import("await_test_helper.dart");

error() {
  var x = 0;
  try {
    throw "error";
    x = await futureOf(2);
  } catch (e) {
    x = 3;
  }
  Expect.equals(3, x);
  return x;
}

main() {
  int t = await error();
  Expect.equals(3, t);
}
