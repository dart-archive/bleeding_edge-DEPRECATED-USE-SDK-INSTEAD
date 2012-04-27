// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within the test of a conditional.

#import("await_test_helper.dart");

main() {
  if (!(await futureOf(false))) {
    Expect.fails("fails to make this a negative test");
  }
}
