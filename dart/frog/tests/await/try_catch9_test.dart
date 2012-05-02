// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// The next line is used to tell test.dart that this test is run by invoking
// awaitc.dart and passing this file as an argument (e.g. frog
// frog/await/awaitc.dart test.dart):
// VMOptions=frog/await/awaitc.dart

// Await within a try-catch block with typed catches.

#import("await_test_helper.dart");

class E1 implements Exception {
  const E1([msg=null]);
}
class E2 implements Exception {
  const E2([msg=null]);
}

awaitAndThrow(Exception ex) {
  try {
    final t = await futureOf(3);
    throw ex;
    return t;
  } catch (E1 e) {
    return 1;
  } catch (E2 e) {
    return 2;
  }
}

main() {
  int t = await awaitAndThrow(const E1());
  Expect.equals(1, t);
  int t = await awaitAndThrow(const E2());
  Expect.equals(2, t);
}
