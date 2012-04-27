// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a conditional - return on one branch.

#import("await_test_helper.dart");

main() {
  var x;
  if (x != null) {
    final f = futureOf(4);
    x = await f;
  } else {
    x = 0;
    return; // assertion is skipped below. (This is a regression test for a bad
            // CPS transformation bug.)
  }
  Expect.equals(3, x);
}
