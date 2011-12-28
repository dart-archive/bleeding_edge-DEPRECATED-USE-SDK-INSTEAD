// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a nested try-catch block, errors on await, catch chain is
// followed.

#import("await_test_helper.dart");

class E1 implements Exception {
  const E1([msg=null]);
}
class E2 implements Exception {
  const E2([msg=null]);
}

awaitAndThrow(Exception ex) {
  try {
    try {
      final f = errorOf(ex);
      final t = await f;
      return 0;
    } catch (E1 e) {
      return 1;
    }
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
