// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a try-catch block, the error occurs after the await.

#import("await_test_helper.dart");

errorAfter() {
  try {
    final t = await futureOf(3);
    throw "error";
    return t;
  } catch (e) {
    return 3;
  }
}

main() {
  int t = await errorAfter();
  Expect.equals(3, t);
}
