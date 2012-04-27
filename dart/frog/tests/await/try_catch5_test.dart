// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a try-catch block, no error, but there is code after the
// try-catch block.

#import("await_test_helper.dart");

noerror() {
  var x = 0;
  try {
    x = await futureOf(2);
  } catch (e) {
    x = 3;
  }
  Expect.equals(2, x);
  return x;
}

main() {
  int t = await noerror();
  Expect.equals(2, t);
}
