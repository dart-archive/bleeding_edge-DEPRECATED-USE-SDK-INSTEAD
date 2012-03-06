// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

switcher(val) {
  var x = 0;
  switch (val) {
  case 1:
    x = 100;
    break;
  case 2:
    x = 200;
    break;
  case 3:
    x = 300;
    break;
  default:
    return 400;
    break; // Intentional dead code (regression test for crash).
  }
  return x;
}

main() {
  Expect.equals(100, switcher(1));
  Expect.equals(200, switcher(2));
  Expect.equals(300, switcher(3));
  Expect.equals(400, switcher(4));
}
