// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a nested try-catch block, throw happens before await, inner
// catch has priority.

#import("await_test_helper.dart");

class E1 implements Exception {
  const E1([msg=null]);
}

awaitAndThrow(Exception ex) {
  try {
    try {
      throw ex;
      final t = await futureOf(0);
    } catch (E1 e) {
      return 1;
    }
  } catch (E1 e) {
    return 2;
  }
}

main() {
  int t = await awaitAndThrow(const E1());
  Expect.equals(1, t);
}
