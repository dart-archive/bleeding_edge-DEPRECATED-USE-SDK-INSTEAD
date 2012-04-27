// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Linear await with the minimal required normalization in the code.
#import("await_test_helper.dart");

main() {
  final x = await futureOf(3);
  Expect.equals(3, x);
}
