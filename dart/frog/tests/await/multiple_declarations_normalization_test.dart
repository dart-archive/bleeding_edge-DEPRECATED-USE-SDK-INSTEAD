// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// The next line is used to tell test.dart that this test is run by invoking
// awaitc.dart and passing this file as an argument (e.g. frog
// frog/await/awaitc.dart test.dart):
// VMOptions=frog/await/awaitc.dart

// await on multi-variable initialization.
#import("await_test_helper.dart");

main() {
  int x = 1, y = await futureOf(x + 1), z = y + 1;
  Expect.equals(3, z);
}
