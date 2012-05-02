// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// The next line is used to tell test.dart that this test is run by invoking
// awaitc.dart and passing this file as an argument (e.g. frog
// frog/await/awaitc.dart test.dart):
// VMOptions=frog/await/awaitc.dart

// await on boolean || expressions.
#import("await_test_helper.dart");

main() {
  // boolean value is correct:
  Expect.equals(true, await futureOf(true) || await futureOf(false));
  Expect.equals(false, await futureOf(false) || await futureOf(false));
  Expect.equals(true, await futureOf(false) || await futureOf(true));
  Expect.equals(true, await futureOf(true) || await futureOf(true));

  // short-circuit works, second example is important because it shows that not
  // only the error doesn't occur, but that the expression is not evaluated
  // altogether.
  Expect.equals(true, await futureOf(true) || await errorOf("ERROR1"));
  Expect.equals(true, await futureOf(true) || await () { throw "ERROR2"; }());
}
