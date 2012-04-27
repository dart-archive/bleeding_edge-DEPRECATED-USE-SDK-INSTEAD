// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Leaving type unspecified in asynchornous function is ok.

#import("await_test_helper.dart");

f() {
  final y = await futureOf(3);
  return y;
}

main() {
  final x = await f();
  Expect.equals(3, x);
}
