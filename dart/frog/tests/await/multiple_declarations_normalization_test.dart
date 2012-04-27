// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// await on multi-variable initialization.
#import("await_test_helper.dart");

main() {
  int x = 1, y = await futureOf(x + 1), z = y + 1;
  Expect.equals(3, z);
}
