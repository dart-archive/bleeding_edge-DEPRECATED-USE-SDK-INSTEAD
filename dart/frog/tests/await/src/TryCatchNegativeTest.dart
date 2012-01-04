// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a try-catch block, no error occurs.

#import("await_test_helper.dart");

noerror() {
  try {
    final t = await futureOf(0);
    return t; // this returns 0
  } catch (e) {
    return -1;
  }
}

main() {
  int t = await noerror();
  Expect.notEquals(0, t); // this should fail
}
