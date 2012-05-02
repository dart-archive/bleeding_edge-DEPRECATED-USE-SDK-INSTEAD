// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// The next line is used to tell test.dart that this test is run by invoking
// awaitc.dart and passing this file as an argument (e.g. frog
// frog/await/awaitc.dart test.dart):
// VMOptions=frog/await/awaitc.dart

// Await in doubly-nested functions.

#import("await_test_helper.dart");

bool called = false;

main() {
  f1() {
    f2() {
      final x = await futureOf(3);
      return x + 1;
    }
    final x = await f2();
    return x + 10;
  }
  final x = await f1();
  Expect.equals(14, x);
}
