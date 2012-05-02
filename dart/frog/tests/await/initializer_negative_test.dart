// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// The next line is used to tell test.dart that this test is run by invoking
// awaitc.dart and passing this file as an argument (e.g. frog
// frog/await/awaitc.dart test.dart):
// VMOptions=frog/await/awaitc.dart

// Illegal to use await in initializers.
#import("await_test_helper.dart");

final Future f = null;

class A {
  int x;
  A() : x = await f;
}

main() => Expect.equals(true, (new A() != null));
