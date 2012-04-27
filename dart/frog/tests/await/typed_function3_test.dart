// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Type added to asynchornous function is correct.

#import("await_test_helper.dart");

Future<int> f() { // correct return type
  final y = await futureOf(3);
  return y;
}

main() {
  final x = await f();
  Expect.equals(3, x);
}
