// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// smoke test: Test that the test helper library works correctly (this and
// HelperNegativeTest work together to ensure that).

#import("await_test_helper.dart");
#import("../../../lib/node/node.dart");

main() {
  final f = futureOf(3);
  // futures don't complete immediatelly
  Expect.equals(false, f.hasValue);
  setTimeout(() {
    // but do complete later. HelperNegativeTest ensures this code was reached.
    Expect.equals(true, f.hasValue);
  }, 0);
}
